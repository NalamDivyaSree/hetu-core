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

import io.prestosql.spi.type.Type;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class VariableReferenceExpressionTest
{
    @Mock
    private Type mockType;

    private VariableReferenceExpression variableReferenceExpressionUnderTest;

    @BeforeMethod
    public void setUp() throws Exception
    {
        initMocks(this);
        variableReferenceExpressionUnderTest = new VariableReferenceExpression("name", mockType);
    }

    @Test
    public void testHashCode() throws Exception
    {
        assertEquals(0, variableReferenceExpressionUnderTest.hashCode());
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("name", variableReferenceExpressionUnderTest.toString());
    }

//    @Test
//    public void testAccept() throws Exception
//    {
//        // Setup
//        final RowExpressionVisitor<R, C> visitor = null;
//        final C context = null;
//
//        // Run the test
//        final R result = variableReferenceExpressionUnderTest.accept(visitor, context);
//
//        // Verify the results
//    }

    @Test
    public void testEquals() throws Exception
    {
        assertTrue(variableReferenceExpressionUnderTest.equals("obj"));
    }
}
