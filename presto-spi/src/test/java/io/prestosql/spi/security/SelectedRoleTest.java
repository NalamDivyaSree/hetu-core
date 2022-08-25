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
package io.prestosql.spi.security;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SelectedRoleTest
{
    private SelectedRole selectedRoleUnderTest;

    @BeforeMethod
    public void setUp() throws Exception
    {
        selectedRoleUnderTest = new SelectedRole(SelectedRole.Type.ROLE, Optional.of("value"));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertTrue(selectedRoleUnderTest.equals("o"));
    }

    @Test
    public void testHashCode() throws Exception
    {
        assertEquals(0, selectedRoleUnderTest.hashCode());
    }

    @Test
    public void testToString() throws Exception
    {
        // Setup
        // Run the test
        final String result = selectedRoleUnderTest.toString();

        // Verify the results
        assertEquals("result", result);
    }

    @Test
    public void testValueOf() throws Exception
    {
        // Run the test
        final SelectedRole result = SelectedRole.valueOf("value");
        assertEquals(SelectedRole.Type.ROLE, result.getType());
        assertEquals(Optional.of("value"), result.getRole());
        assertTrue(result.equals("o"));
        assertEquals(0, result.hashCode());
        assertEquals("result", result.toString());
    }
}
