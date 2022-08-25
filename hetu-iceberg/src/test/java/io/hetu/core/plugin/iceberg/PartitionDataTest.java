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
package io.hetu.core.plugin.iceberg;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.types.Type;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PartitionDataTest
{
    private PartitionData partitionDataUnderTest;
    @Mock
    private StructLike structLike;

    @BeforeMethod
    public void setUp() throws Exception
    {
        partitionDataUnderTest = new PartitionData(new Object[]{"partitionValues"});
    }

    @Test
    public void testSize()
    {
        assertEquals(0, partitionDataUnderTest.size());
    }

    @Test
    public void testGet()
    {
        // Setup
        // Run the test
        partitionDataUnderTest.get(0, Object.class);

        // Verify the results
    }

    @Test
    public void testSet()
    {
        // Setup
        final Object value = null;

        // Run the test
        partitionDataUnderTest.set(0, value);

        // Verify the results
    }

    @Test
    public void testToJson()
    {
        assertEquals("result", partitionDataUnderTest.toJson(structLike));
    }

    @Test
    public void testFromJson()
    {
        // Setup
        final Type[] types = new Type[]{};

        // Run the test
        final PartitionData result = PartitionData.fromJson("partitionDataAsJson", types);
        assertEquals(0, result.size());
        assertEquals(null, result.get(0, Object.class));
        assertEquals("result", result.toJson(structLike));
    }

    @Test
    public void testGetValue()
    {
        // Setup
        final JsonNode partitionValue = null;
        final Type type = null;

        // Run the test
        final Object result = PartitionData.getValue(partitionValue, type);

        // Verify the results
    }
}
