/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.orc.impl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.common.io.DiskRange;
import org.apache.hadoop.hive.ql.util.JavaDataModel;
import org.apache.hadoop.io.Text;
import org.apache.orc.ColumnStatistics;
import org.apache.orc.CompressionCodec;
import org.apache.orc.CompressionKind;
import org.apache.orc.FileFormatException;
import org.apache.orc.FileMetadata;
import org.apache.orc.OrcFile;
import org.apache.orc.OrcProto;
import org.apache.orc.OrcUtils;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.apache.orc.StripeInformation;
import org.apache.orc.StripeStatistics;
import org.apache.orc.TypeDescription;
import org.apache.orc.UnknownFormatException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReaderImpl
        implements Reader
{
    private static final io.prestosql.hive.$internal.org.slf4j.Logger LOG = io.prestosql.hive.$internal.org.slf4j.LoggerFactory.getLogger(ReaderImpl.class);

    private static final int DIRECTORY_SIZE_GUESS = 16 * 1024;

    protected final FileSystem fileSystem;
    private final long maxLength;
    protected final Path path;
    protected final org.apache.orc.CompressionKind compressionKind;
    protected FSDataInputStream file;
    protected int bufferSize;
    protected OrcProto.Metadata metadata;
    private List<OrcProto.StripeStatistics> stripeStats;
    private final int metadataSize;
    protected final List<OrcProto.Type> types;
    private TypeDescription schema;
    private final List<OrcProto.UserMetadataItem> userMetadata;
    private final List<OrcProto.ColumnStatistics> fileStats;
    private final List<StripeInformation> stripes;
    protected final int rowIndexStride;
    private final long contentLength;
    private final long numberOfRows;

    private long deserializedSize = -1;
    protected final Configuration conf;
    protected final boolean useUTCTimestamp;
    private final List<Integer> versionList;
    private final OrcFile.WriterVersion writerVersion;

    protected OrcTail tail;

    @Override
    public void close() throws IOException
    {
        if (file == null) {
            return;
        }
        file.close();
    }

    public static class StripeInformationImpl
            implements StripeInformation
    {
        private final OrcProto.StripeInformation stripe;

        public StripeInformationImpl(OrcProto.StripeInformation stripe)
        {
            this.stripe = stripe;
        }

        @Override
        public long getOffset()
        {
            return stripe.getOffset();
        }

        @Override
        public long getLength()
        {
            return stripe.getDataLength() + getIndexLength() + getFooterLength();
        }

        @Override
        public long getDataLength()
        {
            return stripe.getDataLength();
        }

        @Override
        public long getFooterLength()
        {
            return stripe.getFooterLength();
        }

        @Override
        public long getIndexLength()
        {
            return stripe.getIndexLength();
        }

        @Override
        public long getNumberOfRows()
        {
            return stripe.getNumberOfRows();
        }

        @Override
        public String toString()
        {
            return "offset: " + getOffset() + " data: " + getDataLength() +
                    " rows: " + getNumberOfRows() + " tail: " + getFooterLength() +
                    " index: " + getIndexLength();
        }
    }

    @Override
    public long getNumberOfRows()
    {
        return numberOfRows;
    }

    @Override
    public List<String> getMetadataKeys()
    {
        List<String> result = new ArrayList<>();
        for (OrcProto.UserMetadataItem item : userMetadata) {
            result.add(item.getName());
        }
        return result;
    }

    @Override
    public ByteBuffer getMetadataValue(String key)
    {
        for (OrcProto.UserMetadataItem item : userMetadata) {
            if (item.hasName() && item.getName().equals(key)) {
                return item.getValue().asReadOnlyByteBuffer();
            }
        }
        throw new IllegalArgumentException("Can't find user metadata " + key);
    }

    public boolean hasMetadataValue(String key)
    {
        for (OrcProto.UserMetadataItem item : userMetadata) {
            if (item.hasName() && item.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public org.apache.orc.CompressionKind getCompressionKind()
    {
        return compressionKind;
    }

    @Override
    public int getCompressionSize()
    {
        return bufferSize;
    }

    @Override
    public List<StripeInformation> getStripes()
    {
        return stripes;
    }

    @Override
    public long getContentLength()
    {
        return contentLength;
    }

    @Override
    public List<OrcProto.Type> getTypes()
    {
        return types;
    }

    public static OrcFile.Version getFileVersion(List<Integer> versionList)
    {
        if (versionList == null || versionList.isEmpty()) {
            return OrcFile.Version.V_0_11;
        }
        for (OrcFile.Version version : OrcFile.Version.values()) {
            if (version.getMajor() == versionList.get(0) &&
                    version.getMinor() == versionList.get(1)) {
                return version;
            }
        }
        return OrcFile.Version.FUTURE;
    }

    @Override
    public OrcFile.Version getFileVersion()
    {
        return getFileVersion(versionList);
    }

    @Override
    public OrcFile.WriterVersion getWriterVersion()
    {
        return writerVersion;
    }

    @Override
    public OrcProto.FileTail getFileTail()
    {
        return tail.getFileTail();
    }

    @Override
    public int getRowIndexStride()
    {
        return rowIndexStride;
    }

    @Override
    public ColumnStatistics[] getStatistics()
    {
        return deserializeStats(schema, fileStats);
    }

    public static ColumnStatistics[] deserializeStats(
            TypeDescription schema,
            List<OrcProto.ColumnStatistics> fileStats)
    {
        ColumnStatistics[] result = new ColumnStatistics[fileStats.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = ColumnStatisticsImpl.deserialize(schema, fileStats.get(i));
        }
        return result;
    }

    @Override
    public TypeDescription getSchema()
    {
        return schema;
    }

    /**
     * Ensure this is an ORC file to prevent users from trying to read text
     * files or RC files as ORC files.
     * @param in the file being read
     * @param path the filename for error messages
     * @param psLen the postscript length
     * @param buffer the tail of the file
     */
    protected static void ensureOrcFooter(FSDataInputStream in,
                                          Path path,
                                          int psLen,
                                          ByteBuffer buffer) throws IOException
    {
        int magicLength = OrcFile.MAGIC.length();
        int fullLength = magicLength + 1;
        if (psLen < fullLength || buffer.remaining() < fullLength) {
            throw new FileFormatException("Malformed ORC file " + path +
                    ". Invalid postscript length " + psLen);
        }
        int offset = buffer.arrayOffset() + buffer.position() + buffer.limit() - fullLength;
        byte[] array = buffer.array();
        // now look for the magic string at the end of the postscript.
        if (!Text.decode(array, offset, magicLength).equals(OrcFile.MAGIC)) {
            // If it isn't there, this may be the 0.11.0 version of ORC.
            // Read the first 3 bytes of the file to check for the header
            byte[] header = new byte[magicLength];
            in.readFully(0, header, 0, magicLength);
            // if it isn't there, this isn't an ORC file
            if (!Text.decode(header, 0, magicLength).equals(OrcFile.MAGIC)) {
                throw new FileFormatException("Malformed ORC file " + path +
                        ". Invalid postscript.");
            }
        }
    }

    /**
     * Ensure this is an ORC file to prevent users from trying to read text
     * files or RC files as ORC files.
     * @param psLen the postscript length
     * @param buffer the tail of the file
     */
    protected static void ensureOrcFooter(ByteBuffer buffer, int psLen) throws IOException
    {
        int magicLength = OrcFile.MAGIC.length();
        int fullLength = magicLength + 1;
        if (psLen < fullLength || buffer.remaining() < fullLength) {
            throw new FileFormatException("Malformed ORC file. Invalid postscript length " + psLen);
        }

        int offset = buffer.arrayOffset() + buffer.position() + buffer.limit() - fullLength;
        byte[] array = buffer.array();
        // now look for the magic string at the end of the postscript.
        if (!Text.decode(array, offset, magicLength).equals(OrcFile.MAGIC)) {
            // if it isn't there, this may be 0.11.0 version of the ORC file.
            // Read the first 3 bytes from the buffer to check for the header
            if (!Text.decode(buffer.array(), 0, magicLength).equals(OrcFile.MAGIC)) {
                throw new FileFormatException("Malformed ORC file. Invalid postscript length " + psLen);
            }
        }
    }

    /**
     * Build a version string out of an array.
     * @param version the version number as a list
     * @return the human readable form of the version string
     */
    private static String versionString(List<Integer> version)
    {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < version.size(); ++i) {
            if (i != 0) {
                buffer.append('.');
            }
            buffer.append(version.get(i));
        }
        return buffer.toString();
    }

    /**
     * Check to see if this ORC file is from a future version and if so,
     * warn the user that we may not be able to read all of the column encodings.
     * @param path the data source path for error messages
     * @param postscript the parsed postscript
     */
    protected static void checkOrcVersion(Path path,
                                          OrcProto.PostScript postscript
    ) throws IOException
    {
        List<Integer> version = postscript.getVersionList();
        if (getFileVersion(version) == OrcFile.Version.FUTURE) {
            throw new UnknownFormatException(path, versionString(version),
                    postscript);
        }
    }

    /**
     * Constructor that let's the user specify additional options.
     * @param path pathname for file
     * @param options options for reading
     */
    public ReaderImpl(Path path, OrcFile.ReaderOptions options) throws IOException
    {
        FileSystem fs = options.getFilesystem();
        if (fs == null) {
            fs = path.getFileSystem(options.getConfiguration());
        }
        this.fileSystem = fs;
        this.path = path;
        this.conf = options.getConfiguration();
        this.maxLength = options.getMaxLength();
        this.useUTCTimestamp = options.getUseUTCTimestamp();
        FileMetadata fileMetadata = options.getFileMetadata();
        if (fileMetadata != null) {
            this.compressionKind = fileMetadata.getCompressionKind();
            this.bufferSize = fileMetadata.getCompressionBufferSize();
            this.metadataSize = fileMetadata.getMetadataSize();
            this.stripeStats = fileMetadata.getStripeStats();
            this.versionList = fileMetadata.getVersionList();
            OrcFile.WriterImplementation writer =
                    OrcFile.WriterImplementation.from(fileMetadata.getWriterImplementation());
            this.writerVersion =
                    OrcFile.WriterVersion.from(writer, fileMetadata.getWriterVersionNum());
            this.types = fileMetadata.getTypes();
            this.rowIndexStride = fileMetadata.getRowIndexStride();
            this.contentLength = fileMetadata.getContentLength();
            this.numberOfRows = fileMetadata.getNumberOfRows();
            this.fileStats = fileMetadata.getFileStats();
            this.stripes = fileMetadata.getStripes();
            this.userMetadata = null; // not cached and not needed here
        }
        else {
            OrcTail orcTail = options.getOrcTail();
            if (orcTail == null) {
                tail = extractFileTail(fs, path, options.getMaxLength());
                options.orcTail(tail);
            }
            else {
                checkOrcVersion(path, orcTail.getPostScript());
                tail = orcTail;
            }
            this.compressionKind = tail.getCompressionKind();
            this.bufferSize = tail.getCompressionBufferSize();
            this.metadataSize = tail.getMetadataSize();
            this.versionList = tail.getPostScript().getVersionList();
            this.types = tail.getFooter().getTypesList();
            this.rowIndexStride = tail.getFooter().getRowIndexStride();
            this.contentLength = tail.getFooter().getContentLength();
            this.numberOfRows = tail.getFooter().getNumberOfRows();
            this.userMetadata = tail.getFooter().getMetadataList();
            this.fileStats = tail.getFooter().getStatisticsList();
            this.writerVersion = tail.getWriterVersion();
            this.stripes = tail.getStripes();
            this.stripeStats = tail.getStripeStatisticsProto();
        }
        OrcUtils.isValidTypeTree(this.types, 0);
        this.schema = OrcUtils.convertTypeFromProtobuf(this.types, 0);
    }

    /**
     * Get the WriterVersion based on the ORC file postscript.
     * @param writerVersion the integer writer version
     * @return the version of the software that produced the file
     */
    public static OrcFile.WriterVersion getWriterVersion(int writerVersion)
    {
        for (OrcFile.WriterVersion version : OrcFile.WriterVersion.values()) {
            if (version.getId() == writerVersion) {
                return version;
            }
        }
        return OrcFile.WriterVersion.FUTURE;
    }

    static List<DiskRange> singleton(DiskRange item)
    {
        List<DiskRange> result = new ArrayList<>();
        result.add(item);
        return result;
    }

    private static OrcProto.Footer extractFooter(ByteBuffer bb, int footerAbsPos,
                                                 int footerSize, CompressionCodec codec, int bufferSize) throws IOException
    {
        bb.position(footerAbsPos);
        bb.limit(footerAbsPos + footerSize);
        return OrcProto.Footer.parseFrom(InStream.createCodedInputStream("footer",
                singleton(new BufferChunk(bb, 0)), footerSize, codec, bufferSize));
    }

    public static OrcProto.Metadata extractMetadata(ByteBuffer bb, int metadataAbsPos,
                                                    int metadataSize, CompressionCodec codec, int bufferSize) throws IOException
    {
        bb.position(metadataAbsPos);
        bb.limit(metadataAbsPos + metadataSize);
        return OrcProto.Metadata.parseFrom(InStream.createCodedInputStream("metadata",
                singleton(new BufferChunk(bb, 0)), metadataSize, codec, bufferSize));
    }

    private static OrcProto.PostScript extractPostScript(ByteBuffer bb, Path path,
                                                         int psLen, int psAbsOffset) throws IOException
    {
        // TODO: when PB is upgraded to 2.6, newInstance(ByteBuffer) method should be used here.
        io.prestosql.hive.$internal.com.google.protobuf.CodedInputStream in = io.prestosql.hive.$internal.com.google.protobuf.CodedInputStream.newInstance(
                bb.array(), bb.arrayOffset() + psAbsOffset, psLen);
        OrcProto.PostScript ps = OrcProto.PostScript.parseFrom(in);
        checkOrcVersion(path, ps);

        // Check compression codec.
        switch (ps.getCompression()) {
            case NONE:
            case ZLIB:
            case SNAPPY:
            case LZO:
            case LZ4:
            case ZSTD:
                break;
            default:
                throw new IllegalArgumentException("Unknown compression");
        }
        return ps;
    }

    public static OrcTail extractFileTail(ByteBuffer buffer)
            throws IOException
    {
        return extractFileTail(buffer, -1, -1);
    }

    public static OrcTail extractFileTail(ByteBuffer buffer, long fileLength, long modificationTime)
            throws IOException
    {
        int readSize = buffer.limit();
        int psLen = buffer.get(readSize - 1) & 0xff;
        int psOffset = readSize - 1 - psLen;
        ensureOrcFooter(buffer, psLen);
        byte[] psBuffer = new byte[psLen];
        System.arraycopy(buffer.array(), psOffset, psBuffer, 0, psLen);
        OrcProto.PostScript ps = OrcProto.PostScript.parseFrom(psBuffer);
        int footerSize = (int) ps.getFooterLength();
        CompressionKind kind = CompressionKind.valueOf(ps.getCompression().name());
        OrcProto.FileTail.Builder fileTailBuilder;
        CompressionCodec codec = OrcCodecPool.getCodec(kind);
        try {
            OrcProto.Footer footer = extractFooter(buffer,
                    (int) (buffer.position() + ps.getMetadataLength()),
                    footerSize, codec, (int) ps.getCompressionBlockSize());
            fileTailBuilder = OrcProto.FileTail.newBuilder()
                    .setPostscriptLength(psLen)
                    .setPostscript(ps)
                    .setFooter(footer)
                    .setFileLength(fileLength);
        }
        finally {
            OrcCodecPool.returnCodec(kind, codec);
        }
        // clear does not clear the contents but sets position to 0 and limit = capacity
        buffer.clear();
        return new OrcTail(fileTailBuilder.build(), buffer.slice(), modificationTime);
    }

    /**
     * Build a virtual OrcTail for empty files.
     * @return a new OrcTail
     */
    OrcTail buildEmptyTail()
    {
        OrcProto.PostScript.Builder postscript = OrcProto.PostScript.newBuilder();
        OrcFile.Version version = OrcFile.Version.CURRENT;
        postscript.setMagic(OrcFile.MAGIC)
                .setCompression(OrcProto.CompressionKind.NONE)
                .setFooterLength(0)
                .addVersion(version.getMajor())
                .addVersion(version.getMinor())
                .setMetadataLength(0)
                .setWriterVersion(OrcFile.CURRENT_WRITER.getId());

        // Use a struct with no fields
        OrcProto.Type.Builder struct = OrcProto.Type.newBuilder();
        struct.setKind(OrcProto.Type.Kind.STRUCT);

        OrcProto.Footer.Builder footer = OrcProto.Footer.newBuilder();
        footer.setHeaderLength(0)
                .setContentLength(0)
                .addTypes(struct)
                .setNumberOfRows(0)
                .setRowIndexStride(0);

        OrcProto.FileTail.Builder result = OrcProto.FileTail.newBuilder();
        result.setFooter(footer);
        result.setPostscript(postscript);
        result.setFileLength(0);
        result.setPostscriptLength(0);
        return new OrcTail(result.build(), null);
    }

    protected OrcTail extractFileTail(FileSystem fs, Path path,
                                      long maxFileLength) throws IOException
    {
        ByteBuffer buffer;
        OrcProto.PostScript ps;
        OrcProto.FileTail.Builder fileTailBuilder = OrcProto.FileTail.newBuilder();
        long modificationTime;
        try (FSDataInputStream paper = fs.open(path)) {
            // figure out the size of the file using the option or filesystem
            long size;
            if (maxFileLength == Long.MAX_VALUE) {
                FileStatus fileStatus = fs.getFileStatus(path);
                size = fileStatus.getLen();
                modificationTime = fileStatus.getModificationTime();
            }
            else {
                size = maxFileLength;
                modificationTime = -1;
            }
            if (size == 0) {
                // Hive often creates empty files (including ORC) and has an
                // optimization to create a 0 byte file as an empty ORC file.
                return buildEmptyTail();
            }
            else if (size <= OrcFile.MAGIC.length()) {
                // Anything smaller than MAGIC header cannot be valid (valid ORC files
                // are actually around 40 bytes, this is more conservative)
                throw new FileFormatException("Not a valid ORC file " + path
                        + " (maxFileLength= " + maxFileLength + ")");
            }
            fileTailBuilder.setFileLength(size);

            //read last bytes into buffer to get PostScript
            int readSize = (int) Math.min(size, DIRECTORY_SIZE_GUESS);
            buffer = ByteBuffer.allocate(readSize);
            paper.readFully((size - readSize),
                    buffer.array(), buffer.arrayOffset(), readSize);
            buffer.position(0);

            //read the PostScript
            //get length of PostScript
            int psLen = buffer.get(readSize - 1) & 0xff;
            ensureOrcFooter(paper, path, psLen, buffer);
            int psOffset = readSize - 1 - psLen;
            ps = extractPostScript(buffer, path, psLen, psOffset);
            bufferSize = (int) ps.getCompressionBlockSize();
            CompressionKind kind = CompressionKind.valueOf(ps.getCompression().name());
            fileTailBuilder.setPostscriptLength(psLen).setPostscript(ps);

            int footerSize = (int) ps.getFooterLength();
            int psMetadataLength = (int) ps.getMetadataLength();

            //check if extra bytes need to be read
            int extra = Math.max(0, psLen + 1 + footerSize + psMetadataLength - readSize);
            int tailSize = 1 + psLen + footerSize + psMetadataLength;
            if (extra > 0) {
                //more bytes need to be read, seek back to the right place and read extra bytes
                ByteBuffer extraBuf = ByteBuffer.allocate(extra + readSize);
                paper.readFully((size - readSize - extra), extraBuf.array(),
                        extraBuf.arrayOffset() + extraBuf.position(), extra);
                extraBuf.position(extra);
                //append with already read bytes
                extraBuf.put(buffer);
                buffer = extraBuf;
                buffer.position(0);
                buffer.limit(tailSize);
                readSize += extra;
                psOffset = readSize - 1 - psLen;
            }
            else {
                //footer is already in the bytes in buffer, just adjust position, length
                buffer.position(psOffset - footerSize - psMetadataLength);
                buffer.limit(buffer.position() + tailSize);
            }

            buffer.mark();
            int footerOffset = psOffset - footerSize;
            buffer.position(footerOffset);
            ByteBuffer footerBuffer = buffer.slice();
            buffer.reset();
            OrcProto.Footer footer;
            CompressionCodec codec = OrcCodecPool.getCodec(kind);
            try {
                footer = extractFooter(footerBuffer, 0, footerSize, codec, bufferSize);
            }
            finally {
                OrcCodecPool.returnCodec(kind, codec);
            }
            fileTailBuilder.setFooter(footer);
        }

        ByteBuffer serializedTail = ByteBuffer.allocate(buffer.remaining());
        serializedTail.put(buffer.slice());
        serializedTail.rewind();
        return new OrcTail(fileTailBuilder.build(), serializedTail, modificationTime);
    }

    private static void read(FSDataInputStream file, BufferChunk chunks) throws IOException
    {
        BufferChunk bufferChunk = chunks;
        while (bufferChunk != null) {
            if (!bufferChunk.hasData()) {
                int len = bufferChunk.getLength();
                ByteBuffer bb = ByteBuffer.allocate(len);
                file.readFully(bufferChunk.getOffset(), bb.array(), bb.arrayOffset(), len);
                bufferChunk.setChunk(bb);
            }
            bufferChunk = (BufferChunk) bufferChunk.next;
        }
    }

    @Override
    public ByteBuffer getSerializedFileFooter()
    {
        return tail.getSerializedTail();
    }

    @Override
    public Options options()
    {
        return new Options(conf);
    }

    @Override
    public RecordReader rows() throws IOException
    {
        return rows(options());
    }

    @Override
    public RecordReader rows(Options options) throws IOException
    {
        LOG.info("Reading ORC rows from " + path + " with " + options);
        return new RecordReaderImpl(this, options);
    }

    @Override
    public long getRawDataSize()
    {
        // if the deserializedSize is not computed, then compute it, else
        // return the already computed size. since we are reading from the footer
        // we don't have to compute deserialized size repeatedly
        if (deserializedSize == -1) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < fileStats.size(); ++i) {
                indices.add(i);
            }
            deserializedSize = getRawDataSizeFromColIndices(indices);
        }
        return deserializedSize;
    }

    @Override
    public long getRawDataSizeFromColIndices(List<Integer> colIndices)
    {
        return getRawDataSizeFromColIndices(colIndices, types, fileStats);
    }

    public static long getRawDataSizeFromColIndices(
            List<Integer> colIndices, List<OrcProto.Type> types,
            List<OrcProto.ColumnStatistics> stats)
    {
        long result = 0;
        for (int colIdx : colIndices) {
            result += getRawDataSizeOfColumn(colIdx, types, stats);
        }
        return result;
    }

    private static long getRawDataSizeOfColumn(int colIdx, List<OrcProto.Type> types,
                                               List<OrcProto.ColumnStatistics> stats)
    {
        OrcProto.ColumnStatistics colStat = stats.get(colIdx);
        long numVals = colStat.getNumberOfValues();
        OrcProto.Type type = types.get(colIdx);

        switch (type.getKind()) {
            case BINARY:
                // old orc format doesn't support binary statistics. checking for binary
                // statistics is not required as protocol buffers takes care of it.
                return colStat.getBinaryStatistics().getSum();
            case STRING:
            case CHAR:
            case VARCHAR:
                // old orc format doesn't support sum for string statistics. checking for
                // existence is not required as protocol buffers takes care of it.

                // ORC strings are deserialized to java strings. so use java data model's
                // string size
                numVals = numVals == 0 ? 1 : numVals;
                int avgStrLen = (int) (colStat.getStringStatistics().getSum() / numVals);
                return numVals * JavaDataModel.get().lengthForStringOfLength(avgStrLen);
            case TIMESTAMP:
                return numVals * JavaDataModel.get().lengthOfTimestamp();
            case DATE:
                return numVals * JavaDataModel.get().lengthOfDate();
            case DECIMAL:
                return numVals * JavaDataModel.get().lengthOfDecimal();
            case DOUBLE:
            case LONG:
                return numVals * JavaDataModel.get().primitive2();
            case FLOAT:
            case INT:
            case SHORT:
            case BOOLEAN:
            case BYTE:
                return numVals * JavaDataModel.get().primitive1();
            default:
                LOG.debug("Unknown primitive category: " + type.getKind());
                break;
        }

        return 0;
    }

    @Override
    public long getRawDataSizeOfColumns(List<String> colNames)
    {
        List<Integer> colIndices = getColumnIndicesFromNames(colNames);
        return getRawDataSizeFromColIndices(colIndices);
    }

    private List<Integer> getColumnIndicesFromNames(List<String> colNames)
    {
        // top level struct
        OrcProto.Type type = types.get(0);
        List<Integer> colIndices = new ArrayList<>();
        List<String> fieldNames = type.getFieldNamesList();
        int fieldIdx;
        for (String colName : colNames) {
            if (fieldNames.contains(colName)) {
                fieldIdx = fieldNames.indexOf(colName);
            }
            else {
                StringBuilder s = new StringBuilder("Cannot find field for: ");
                s.append(colName);
                s.append(" in ");
                for (String fn : fieldNames) {
                    s.append(fn);
                    s.append(", ");
                }
                LOG.warn(s.toString());
                continue;
            }

            // a single field may span multiple columns. find start and end column
            // index for the requested field
            int idxStart = type.getSubtypes(fieldIdx);

            int idxEnd;

            // if the specified is the last field and then end index will be last
            // column index
            if (fieldIdx + 1 > fieldNames.size() - 1) {
                idxEnd = getLastIdx() + 1;
            }
            else {
                idxEnd = type.getSubtypes(fieldIdx + 1);
            }

            // if start index and end index are same then the field is a primitive
            // field else complex field (like map, list, struct, union)
            if (idxStart == idxEnd) {
                // simple field
                colIndices.add(idxStart);
            }
            else {
                // complex fields spans multiple columns
                for (int i = idxStart; i < idxEnd; i++) {
                    colIndices.add(i);
                }
            }
        }
        return colIndices;
    }

    private int getLastIdx()
    {
        Set<Integer> indices = new HashSet<>();
        for (OrcProto.Type type : types) {
            indices.addAll(type.getSubtypesList());
        }
        return Collections.max(indices);
    }

    @Override
    public List<OrcProto.StripeStatistics> getOrcProtoStripeStatistics()
    {
        return stripeStats;
    }

    @Override
    public List<OrcProto.ColumnStatistics> getOrcProtoFileStatistics()
    {
        return fileStats;
    }

    @Override
    public List<StripeStatistics> getStripeStatistics() throws IOException
    {
        if (metadata == null) {
            CompressionCodec codec = OrcCodecPool.getCodec(compressionKind);
            try {
                metadata = extractMetadata(tail.getSerializedTail(), 0, metadataSize, codec, bufferSize);
            }
            finally {
                OrcCodecPool.returnCodec(compressionKind, codec);
            }
        }
        if (stripeStats == null) {
            stripeStats = metadata.getStripeStatsList();
        }
        List<StripeStatistics> result = new ArrayList<>();
        for (OrcProto.StripeStatistics ss : stripeStats) {
            result.add(new StripeStatistics(ss.getColStatsList()));
        }
        return result;
    }

    public List<OrcProto.UserMetadataItem> getOrcProtoUserMetadata()
    {
        return userMetadata;
    }

    @Override
    public List<Integer> getVersionList()
    {
        return versionList;
    }

    @Override
    public int getMetadataSize()
    {
        return metadataSize;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ORC Reader(");
        buffer.append(path);
        if (maxLength != -1) {
            buffer.append(", ");
            buffer.append(maxLength);
        }
        buffer.append(")");
        return buffer.toString();
    }
}
