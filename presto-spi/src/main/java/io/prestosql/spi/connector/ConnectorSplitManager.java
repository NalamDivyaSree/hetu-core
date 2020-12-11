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
package io.prestosql.spi.connector;

import io.prestosql.spi.dynamicfilter.DynamicFilter;
import io.prestosql.spi.predicate.TupleDomain;
import io.prestosql.spi.resourcegroups.QueryType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public interface ConnectorSplitManager
{
    @Deprecated
    default ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorTableLayoutHandle layout,
            SplitSchedulingStrategy splitSchedulingStrategy)
    {
        throw new UnsupportedOperationException();
    }

    default ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorTableHandle table,
            SplitSchedulingStrategy splitSchedulingStrategy)
    {
        throw new UnsupportedOperationException();
    }

    default ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorTableHandle table,
            SplitSchedulingStrategy splitSchedulingStrategy,
            Supplier<Set<DynamicFilter>> dynamicFilterSupplier,
            Optional<QueryType> queryType,
            Map<String, Object> queryInfo,
            Set<TupleDomain<ColumnMetadata>> userDefinedCachePredicates,
            boolean partOfReuse)
    {
        return getSplits(transaction, session, table, splitSchedulingStrategy);
    }

    enum SplitSchedulingStrategy
    {
        UNGROUPED_SCHEDULING,
        GROUPED_SCHEDULING,
    }
}
