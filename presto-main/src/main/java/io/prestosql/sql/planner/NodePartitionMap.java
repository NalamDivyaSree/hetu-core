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
package io.prestosql.sql.planner;

import com.google.common.collect.ImmutableList;
import io.prestosql.execution.scheduler.BucketNodeMap;
import io.prestosql.execution.scheduler.FixedBucketNodeMap;
import io.prestosql.metadata.InternalNode;
import io.prestosql.metadata.Split;

import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

// When the probe side of join is bucketed but builder side is not,
// bucket to partition mapping has to be populated to builder side remote fragment.
// NodePartitionMap is required in this case and cannot be replaced by BucketNodeMap.
//
//      Join
//      /  \
//   Scan  Remote
//
public class NodePartitionMap
{
    private final List<InternalNode> partitionToNode;
    private final int[] bucketToPartition;
    private final ToIntFunction<Split> splitToBucket;

    public NodePartitionMap(List<InternalNode> partitionToNode, ToIntFunction<Split> splitToBucket)
    {
        this(partitionToNode, IntStream.range(0, partitionToNode.size()).toArray(), splitToBucket);
    }

    public NodePartitionMap(List<InternalNode> partitionToNode, int[] bucketToPartition, ToIntFunction<Split> splitToBucket)
    {
        this.bucketToPartition = requireNonNull(bucketToPartition, "bucketToPartition is null");
        this.partitionToNode = ImmutableList.copyOf(requireNonNull(partitionToNode, "partitionToNode is null"));
        this.splitToBucket = requireNonNull(splitToBucket, "splitToBucket is null");
    }

    public List<InternalNode> getPartitionToNode()
    {
        return partitionToNode;
    }

    public int[] getBucketToPartition()
    {
        return bucketToPartition;
    }

    public InternalNode getNode(Split split)
    {
        int bucket = splitToBucket.applyAsInt(split);
        int partition = bucketToPartition[bucket];
        return requireNonNull(partitionToNode.get(partition));
    }

    public BucketNodeMap asBucketNodeMap()
    {
        ImmutableList.Builder<InternalNode> bucketToNode = ImmutableList.builder();
        for (int partition : bucketToPartition) {
            bucketToNode.add(partitionToNode.get(partition));
        }
        return new FixedBucketNodeMap(splitToBucket, bucketToNode.build());
    }
}
