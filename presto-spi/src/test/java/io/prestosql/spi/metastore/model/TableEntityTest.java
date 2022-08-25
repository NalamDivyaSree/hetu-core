/*
 * Copyright (C) 2018-2020. Huawei Technologies Co., Ltd. All rights reserved.
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
package io.prestosql.spi.metastore.model;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TableEntityTest
{
    private TableEntity tableEntityUnderTest;

    @BeforeMethod
    public void setUp() throws Exception
    {
        tableEntityUnderTest = new TableEntity("catalogName", "databaseName", "name", "owner", "type",
                "viewOriginalText", 0L, "comment", new HashMap<>(),
                Arrays.asList(new ColumnEntity("name", "type", "comment", new HashMap<>())));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertTrue(tableEntityUnderTest.equals("o"));
    }

    @Test
    public void testHashCode() throws Exception
    {
        assertEquals(0, tableEntityUnderTest.hashCode());
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("result", tableEntityUnderTest.toString());
    }

    @Test
    public void testBuilder1() throws Exception
    {
        // Setup
        // Run the test
        final TableEntity.Builder result = TableEntity.builder();

        // Verify the results
    }

    @Test
    public void testBuilder2() throws Exception
    {
        // Setup
        final TableEntity table = new TableEntity("catalogName", "databaseName", "name", "owner", "type",
                "viewOriginalText", 0L, "comment", new HashMap<>(),
                Arrays.asList(new ColumnEntity("name", "type", "comment", new HashMap<>())));

        // Run the test
        final TableEntity.Builder result = TableEntity.builder(table);

        // Verify the results
    }
}
