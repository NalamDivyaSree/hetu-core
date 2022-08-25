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
package io.prestosql.spi.relation;

import io.prestosql.spi.block.Block;
import io.prestosql.spi.type.Type;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ConstantExpressionTest
{
    @Mock
    private Type mockType;

    private ConstantExpression constantExpressionUnderTest;

    @BeforeMethod
    public void setUp() throws Exception
    {
        initMocks(this);
        constantExpressionUnderTest = new ConstantExpression("value", mockType);
    }

    @Test
    public void testGetValueBlock()
    {
        // Setup
        // Run the test
        final Block result = constantExpressionUnderTest.getValueBlock();

        // Verify the results
    }

    @Test
    public void testIsNull() throws Exception
    {
        assertTrue(constantExpressionUnderTest.isNull());
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("result", constantExpressionUnderTest.toString());
    }

    @Test
    public void testHashCode() throws Exception
    {
        assertEquals(0, constantExpressionUnderTest.hashCode());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertTrue(constantExpressionUnderTest.equals("obj"));
    }

//    @Test
//    public void testAccept() throws Exception
//    {
//        // Setup
//        final RowExpressionVisitor<R, C> visitor = null;
//        final C context = null;
//
//        // Run the test
//        final R result = constantExpressionUnderTest.accept(visitor, context);
//
//        // Verify the results
//    }
//
//    @Test
//    public void testCreateConstantExpression() throws Exception
//    {
//        // Setup
//        final Block valueBlock = null;
//        final Type type = null;
//
//        // Run the test
//        final ConstantExpression result = ConstantExpression.createConstantExpression(valueBlock, type);
//        assertEquals(null, result.getValueBlock());
//        assertEquals("value", result.getValue());
//        assertTrue(result.isNull());
//        assertEquals(null, result.getType());
//        assertEquals("result", result.toString());
//        assertEquals(0, result.hashCode());
//        assertTrue(result.equals("obj"));
//        final RowExpressionVisitor<R, C> visitor = null;
//        final C context = null;
//        assertEquals(null, result.accept(visitor, context));
//        assertTrue(result.absEquals("o"));
//    }
}
