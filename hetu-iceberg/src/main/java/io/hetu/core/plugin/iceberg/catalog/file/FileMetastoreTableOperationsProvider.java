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
package io.hetu.core.plugin.iceberg.catalog.file;

import io.hetu.core.plugin.iceberg.FileIoProvider;
import io.hetu.core.plugin.iceberg.catalog.IcebergTableOperations;
import io.hetu.core.plugin.iceberg.catalog.IcebergTableOperationsProvider;
import io.hetu.core.plugin.iceberg.catalog.TrinoCatalog;
import io.hetu.core.plugin.iceberg.catalog.hms.TrinoHiveCatalog;
import io.prestosql.plugin.hive.HdfsEnvironment.HdfsContext;
import io.prestosql.spi.connector.ConnectorSession;

import javax.inject.Inject;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class FileMetastoreTableOperationsProvider
        implements IcebergTableOperationsProvider
{
    private final FileIoProvider fileIoProvider;

    @Inject
    public FileMetastoreTableOperationsProvider(FileIoProvider fileIoProvider)
    {
        this.fileIoProvider = requireNonNull(fileIoProvider, "fileIoProvider is null");
    }

    @Override
    public IcebergTableOperations createTableOperations(
            TrinoCatalog catalog,
            ConnectorSession session,
            String database,
            String table,
            Optional<String> owner,
            Optional<String> location)
    {
        return new FileMetastoreTableOperations(
                fileIoProvider.createFileIo(new HdfsContext(session.getIdentity()), session.getQueryId()),
                ((TrinoHiveCatalog) catalog).getMetastore(),
                session,
                database,
                table,
                owner,
                location);
    }
}
