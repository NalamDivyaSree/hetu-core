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
package io.prestosql.spi.type;

import io.prestosql.spi.block.Block;
import io.prestosql.spi.block.BlockBuilder;
import io.prestosql.spi.block.BlockBuilderStatus;
import io.prestosql.spi.block.MapBlock;
import io.prestosql.spi.block.MapBlockBuilder;
import io.prestosql.spi.block.SingleMapBlock;
import io.prestosql.spi.connector.ConnectorSession;
import io.prestosql.spi.function.IcebergInvocationConvention;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.prestosql.spi.function.IcebergInvocationConvention.InvocationArgumentConvention.BLOCK_POSITION;
import static io.prestosql.spi.function.IcebergInvocationConvention.InvocationArgumentConvention.NEVER_NULL;
import static io.prestosql.spi.function.IcebergInvocationConvention.InvocationReturnConvention.FAIL_ON_NULL;
import static io.prestosql.spi.function.IcebergInvocationConvention.InvocationReturnConvention.NULLABLE_RETURN;
import static io.prestosql.spi.function.IcebergInvocationConvention.simpleConvention;
import static io.prestosql.spi.type.TypeUtils.checkElementNotNull;
import static io.prestosql.spi.type.TypeUtils.hashPosition;
import static java.lang.String.format;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class MapType<K extends Type, V extends Type>
        extends AbstractType
{
    private final K keyType;
    private final V valueType;
    private static final String MAP_NULL_ELEMENT_MSG = "MAP comparison not supported for null value elements";
    private static final int EXPECTED_BYTES_PER_ENTRY = 32;
    private static final IcebergInvocationConvention HASH_CODE_CONVENTION = simpleConvention(FAIL_ON_NULL, NEVER_NULL);

    private final MethodHandle keyNativeHashCode;
    private final MethodHandle keyBlockHashCode;
    private final MethodHandle keyBlockNativeEquals;
    private final MethodHandle keyBlockEquals;

    private final MethodHandle keyBlockNativeEqual;
    private final MethodHandle keyBlockEqual;

    public MapType(
            K keyType,
            V valueType,
            MethodHandle keyBlockNativeEquals,
            MethodHandle keyBlockEquals,
            MethodHandle keyNativeHashCode,
            MethodHandle keyBlockHashCode)
    {
        super(new TypeSignature(StandardTypes.MAP,
                        TypeSignatureParameter.of(keyType.getTypeSignature()),
                        TypeSignatureParameter.of(valueType.getTypeSignature())),
                Block.class);
        if (!keyType.isComparable()) {
            throw new IllegalArgumentException(format("key type must be comparable, got %s", keyType));
        }
        this.keyType = keyType;
        this.valueType = valueType;
        requireNonNull(keyBlockNativeEquals, "keyBlockNativeEquals is null");
        requireNonNull(keyNativeHashCode, "keyNativeHashCode is null");
        requireNonNull(keyBlockHashCode, "keyBlockHashCode is null");
        this.keyBlockNativeEquals = keyBlockNativeEquals;
        this.keyNativeHashCode = keyNativeHashCode;
        this.keyBlockHashCode = keyBlockHashCode;
        this.keyBlockEquals = keyBlockEquals;
        this.keyBlockNativeEqual = null;
        this.keyBlockEqual = null;
    }

    public MapType(K keyType, V valueType, TypeOperators typeOperators)
    {
        super(
                new TypeSignature(
                        StandardTypes.MAP,
                        TypeSignatureParameter.typeParameter(keyType.getTypeSignature()),
                        TypeSignatureParameter.typeParameter(valueType.getTypeSignature())),
                Block.class);
        if (!keyType.isComparable()) {
            throw new IllegalArgumentException(format("key type must be comparable, got %s", keyType));
        }
        this.keyType = keyType;
        this.valueType = valueType;
        this.keyBlockNativeEquals = null;
        this.keyBlockEquals = null;

        keyBlockNativeEqual = typeOperators.getEqualOperator(keyType, simpleConvention(NULLABLE_RETURN, BLOCK_POSITION, NEVER_NULL))
                .asType(methodType(Boolean.class, Block.class, int.class, keyType.getJavaType().isPrimitive() ? keyType.getJavaType() : Object.class));
        keyBlockEqual = typeOperators.getEqualOperator(keyType, simpleConvention(NULLABLE_RETURN, BLOCK_POSITION, BLOCK_POSITION));
        keyNativeHashCode = typeOperators.getHashCodeOperator(keyType, HASH_CODE_CONVENTION)
                .asType(methodType(long.class, keyType.getJavaType().isPrimitive() ? keyType.getJavaType() : Object.class));
        keyBlockHashCode = typeOperators.getHashCodeOperator(keyType, simpleConvention(FAIL_ON_NULL, BLOCK_POSITION));
    }

    @Override
    public BlockBuilder createBlockBuilder(BlockBuilderStatus blockBuilderStatus, int expectedEntries, int expectedBytesPerEntry)
    {
        return new MapBlockBuilder(this, blockBuilderStatus, expectedEntries);
    }

    @Override
    public BlockBuilder createBlockBuilder(BlockBuilderStatus blockBuilderStatus, int expectedEntries)
    {
        return createBlockBuilder(blockBuilderStatus, expectedEntries, EXPECTED_BYTES_PER_ENTRY);
    }

    public Type getKeyType()
    {
        return keyType;
    }

    public Type getValueType()
    {
        return valueType;
    }

    @Override
    public boolean isComparable()
    {
        return valueType.isComparable();
    }

    @Override
    public long hash(Block block, int position)
    {
        Block mapBlock = getObject(block, position);
        long result = 0;

        for (int i = 0; i < mapBlock.getPositionCount(); i += 2) {
            result += hashPosition(keyType, mapBlock, i) ^ hashPosition(valueType, mapBlock, i + 1);
        }
        return result;
    }

    @Override
    public <T> boolean equalTo(Block<T> leftBlock, int leftPosition, Block<T> rightBlock, int rightPosition)
    {
        Block leftMapBlock = leftBlock.getObject(leftPosition, Block.class);
        Block rightMapBlock = rightBlock.getObject(rightPosition, Block.class);

        if (leftMapBlock.getPositionCount() != rightMapBlock.getPositionCount()) {
            return false;
        }

        Map<KeyWrapper, Integer> wrappedLeftMap = new HashMap<>();
        for (int position = 0; position < leftMapBlock.getPositionCount(); position += 2) {
            wrappedLeftMap.put(new KeyWrapper(keyType, leftMapBlock, position), position + 1);
        }

        for (int position = 0; position < rightMapBlock.getPositionCount(); position += 2) {
            KeyWrapper key = new KeyWrapper(keyType, rightMapBlock, position);
            Integer leftValuePosition = wrappedLeftMap.get(key);
            if (leftValuePosition == null) {
                return false;
            }
            int rightValuePosition = position + 1;
            checkElementNotNull(leftMapBlock.isNull(leftValuePosition), MAP_NULL_ELEMENT_MSG);
            checkElementNotNull(rightMapBlock.isNull(rightValuePosition), MAP_NULL_ELEMENT_MSG);

            if (!valueType.equalTo(leftMapBlock, leftValuePosition, rightMapBlock, rightValuePosition)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <T> int compareTo(Block<T> leftBlock, int leftPosition, Block<T> rightBlock, int rightPosition)
    {
        throw new UnsupportedOperationException();
    }

    private static final class KeyWrapper
    {
        private final Type type;
        private final Block block;
        private final int position;

        public KeyWrapper(Type type, Block block, int position)
        {
            this.type = type;
            this.block = block;
            this.position = position;
        }

        public Block getBlock()
        {
            return this.block;
        }

        public int getPosition()
        {
            return this.position;
        }

        @Override
        public int hashCode()
        {
            return Long.hashCode(type.hash(block, position));
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }
            KeyWrapper other = (KeyWrapper) obj;
            return type.equalTo(this.block, this.position, other.getBlock(), other.getPosition());
        }
    }

    @Override
    public <T> Object getObjectValue(ConnectorSession session, Block<T> block, int position)
    {
        if (block.isNull(position)) {
            return null;
        }

        Block singleMapBlock = block.getObject(position, Block.class);
        if (!(singleMapBlock instanceof SingleMapBlock)) {
            throw new UnsupportedOperationException("Map is encoded with legacy block representation");
        }
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < singleMapBlock.getPositionCount(); i += 2) {
            map.put(keyType.getObjectValue(session, singleMapBlock, i), valueType.getObjectValue(session, singleMapBlock, i + 1));
        }

        return Collections.unmodifiableMap(map);
    }

    @Override
    public void appendTo(Block block, int position, BlockBuilder blockBuilder)
    {
        if (block.isNull(position)) {
            blockBuilder.appendNull();
        }
        else {
            block.writePositionTo(position, blockBuilder);
        }
    }

    @Override
    public <T> Block getObject(Block<T> block, int position)
    {
        return block.getObject(position, Block.class);
    }

    @Override
    public void writeObject(BlockBuilder blockBuilder, Object value)
    {
        if (!(value instanceof SingleMapBlock)) {
            throw new IllegalArgumentException("Maps must be represented with SingleMapBlock");
        }
        blockBuilder.appendStructure((Block) value);
    }

    @Override
    public List<Type> getTypeParameters()
    {
        return asList(getKeyType(), getValueType());
    }

    @Override
    public String getDisplayName()
    {
        return "map(" + keyType.getDisplayName() + ", " + valueType.getDisplayName() + ")";
    }

    public Block createBlockFromKeyValue(Optional<boolean[]> mapIsNull, int[] offsets, Block keyBlock, Block valueBlock)
    {
        return MapBlock.fromKeyValueBlock(
                mapIsNull,
                offsets,
                keyBlock,
                valueBlock,
                this);
    }

    /**
     * Internal use by this package and io.prestosql.spi.block only.
     */
    public MethodHandle getKeyNativeHashCode()
    {
        return keyNativeHashCode;
    }

    /**
     * Internal use by this package and io.prestosql.spi.block only.
     */
    public MethodHandle getKeyBlockHashCode()
    {
        return keyBlockHashCode;
    }

    /**
     * Internal use by this package and io.prestosql.spi.block only.
     */
    public MethodHandle getKeyBlockNativeEquals()
    {
        return keyBlockNativeEquals;
    }

    /**
     * Internal use by this package and io.prestosql.spi.block only.
     */
    public MethodHandle getKeyBlockEquals()
    {
        return keyBlockEquals;
    }
}
