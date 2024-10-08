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
package io.prestosql.execution;

import com.google.common.collect.ImmutableSet;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import io.prestosql.operator.BlockedReason;

import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.Set;

import static io.airlift.units.DataSize.Unit.BYTE;
import static io.airlift.units.DataSize.succinctBytes;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class BasicStageStats
{
    public static final BasicStageStats EMPTY_STAGE_STATS = new BasicStageStats(
            false,

            0,

            0,
            0,
            0,
            0,

            new DataSize(0, BYTE),
            0,

            new DataSize(0, BYTE),
            0,

            new DataSize(0, BYTE),
            0,

            0,
            0,
            new DataSize(0, BYTE),
            new DataSize(0, BYTE),

            new Duration(0, MILLISECONDS),
            new Duration(0, MILLISECONDS),
            new Duration(0, MILLISECONDS),
            new Duration(0, MILLISECONDS),

            false,
            ImmutableSet.of(),

            OptionalDouble.empty());

    private final boolean isScheduled;
    private final int failedTasks;
    private final int totalDrivers;
    private final int queuedDrivers;
    private final int runningDrivers;
    private final int completedDrivers;
    private final DataSize physicalInputDataSize;
    private final long physicalInputPositions;
    private final DataSize internalNetworkInputDataSize;
    private final long internalNetworkInputPositions;
    private final DataSize rawInputDataSize;
    private final long rawInputPositions;
    private final long cumulativeUserMemory;
    private final long failedCumulativeUserMemory;
    private final DataSize userMemoryReservation;
    private final DataSize totalMemoryReservation;
    private final Duration totalCpuTime;
    private final Duration failedCpuTime;
    private final Duration totalScheduledTime;
    private final Duration failedScheduledTime;
    private final boolean fullyBlocked;
    private final Set<BlockedReason> blockedReasons;
    private final OptionalDouble progressPercentage;

    public BasicStageStats(
            boolean isScheduled,

            int failedTasks,

            int totalDrivers,
            int queuedDrivers,
            int runningDrivers,
            int completedDrivers,

            DataSize physicalInputDataSize,
            long physicalInputPositions,

            DataSize internalNetworkInputDataSize,
            long internalNetworkInputPositions,

            DataSize rawInputDataSize,
            long rawInputPositions,

            long cumulativeUserMemory,
            long failedCumulativeUserMemory,
            DataSize userMemoryReservation,
            DataSize totalMemoryReservation,

            Duration totalCpuTime,
            Duration failedCpuTime,
            Duration totalScheduledTime,
            Duration failedScheduledTime,

            boolean fullyBlocked,
            Set<BlockedReason> blockedReasons,

            OptionalDouble progressPercentage)
    {
        this.isScheduled = isScheduled;
        this.failedTasks = failedTasks;
        this.totalDrivers = totalDrivers;
        this.queuedDrivers = queuedDrivers;
        this.runningDrivers = runningDrivers;
        this.completedDrivers = completedDrivers;
        this.physicalInputDataSize = requireNonNull(physicalInputDataSize, "physicalInputDataSize is null");
        this.physicalInputPositions = physicalInputPositions;
        this.internalNetworkInputDataSize = requireNonNull(internalNetworkInputDataSize, "internalNetworkInputDataSize is null");
        this.internalNetworkInputPositions = internalNetworkInputPositions;
        this.rawInputDataSize = requireNonNull(rawInputDataSize, "rawInputDataSize is null");
        this.rawInputPositions = rawInputPositions;
        this.cumulativeUserMemory = cumulativeUserMemory;
        this.failedCumulativeUserMemory = failedCumulativeUserMemory;
        this.userMemoryReservation = requireNonNull(userMemoryReservation, "userMemoryReservation is null");
        this.totalMemoryReservation = requireNonNull(totalMemoryReservation, "totalMemoryReservation is null");
        this.totalCpuTime = requireNonNull(totalCpuTime, "totalCpuTime is null");
        this.failedCpuTime = requireNonNull(failedCpuTime, "failedCpuTime is null");
        this.totalScheduledTime = requireNonNull(totalScheduledTime, "totalScheduledTime is null");
        this.failedScheduledTime = requireNonNull(failedScheduledTime, "failedScheduledTime is null");
        this.fullyBlocked = fullyBlocked;
        this.blockedReasons = ImmutableSet.copyOf(requireNonNull(blockedReasons, "blockedReasons is null"));
        this.progressPercentage = requireNonNull(progressPercentage, "progressPercentage is null");
    }

    public boolean isScheduled()
    {
        return isScheduled;
    }

    public int getFailedTasks()
    {
        return failedTasks;
    }

    public int getTotalDrivers()
    {
        return totalDrivers;
    }

    public int getQueuedDrivers()
    {
        return queuedDrivers;
    }

    public int getRunningDrivers()
    {
        return runningDrivers;
    }

    public int getCompletedDrivers()
    {
        return completedDrivers;
    }

    public DataSize getPhysicalInputDataSize()
    {
        return physicalInputDataSize;
    }

    public long getPhysicalInputPositions()
    {
        return physicalInputPositions;
    }

    public DataSize getInternalNetworkInputDataSize()
    {
        return internalNetworkInputDataSize;
    }

    public long getInternalNetworkInputPositions()
    {
        return internalNetworkInputPositions;
    }

    public DataSize getRawInputDataSize()
    {
        return rawInputDataSize;
    }

    public long getRawInputPositions()
    {
        return rawInputPositions;
    }

    public long getCumulativeUserMemory()
    {
        return cumulativeUserMemory;
    }

    public long getFailedCumulativeUserMemory()
    {
        return failedCumulativeUserMemory;
    }

    public DataSize getUserMemoryReservation()
    {
        return userMemoryReservation;
    }

    public DataSize getTotalMemoryReservation()
    {
        return totalMemoryReservation;
    }

    public Duration getTotalCpuTime()
    {
        return totalCpuTime;
    }

    public Duration getFailedCpuTime()
    {
        return failedCpuTime;
    }

    public Duration getTotalScheduledTime()
    {
        return totalScheduledTime;
    }

    public Duration getFailedScheduledTime()
    {
        return failedScheduledTime;
    }

    public boolean isFullyBlocked()
    {
        return fullyBlocked;
    }

    public Set<BlockedReason> getBlockedReasons()
    {
        return blockedReasons;
    }

    public OptionalDouble getProgressPercentage()
    {
        return progressPercentage;
    }

    public static BasicStageStats aggregateBasicStageStats(Iterable<BasicStageStats> stages)
    {
        int localFailedTasks = 0;

        int localTotalDrivers = 0;
        int localQueuedDrivers = 0;
        int localRunningDrivers = 0;
        int localCompletedDrivers = 0;

        long localCumulativeUserMemory = 0;
        long failedLocalCumulativeUserMemory = 0;
        long localUserMemoryReservation = 0;
        long localTotalMemoryReservation = 0;

        long totalScheduledTimeMillis = 0;
        long failedScheduledTimeMillis = 0;
        long localTotalCpuTime = 0;
        long localFailedCpuTime = 0;

        long localPhysicalInputDataSize = 0;
        long localPhysicalInputPositions = 0;

        long localInternalNetworkInputDataSize = 0;
        long localInternalNetworkInputPositions = 0;

        long localRawInputDataSize = 0;
        long localRawInputPositions = 0;

        boolean localScheduled = true;

        boolean localFullyBlocked = true;
        Set<BlockedReason> localBlockedReasons = new HashSet<>();

        for (BasicStageStats stageStats : stages) {
            localFailedTasks += stageStats.getFailedTasks();

            localTotalDrivers += stageStats.getTotalDrivers();
            localQueuedDrivers += stageStats.getQueuedDrivers();
            localRunningDrivers += stageStats.getRunningDrivers();
            localCompletedDrivers += stageStats.getCompletedDrivers();

            localCumulativeUserMemory += stageStats.getCumulativeUserMemory();
            failedLocalCumulativeUserMemory += stageStats.getFailedCumulativeUserMemory();
            localUserMemoryReservation += stageStats.getUserMemoryReservation().toBytes();
            localTotalMemoryReservation += stageStats.getTotalMemoryReservation().toBytes();

            totalScheduledTimeMillis += stageStats.getTotalScheduledTime().roundTo(MILLISECONDS);
            failedScheduledTimeMillis += stageStats.getFailedScheduledTime().roundTo(MILLISECONDS);
            localTotalCpuTime += stageStats.getTotalCpuTime().roundTo(MILLISECONDS);
            localFailedCpuTime += stageStats.getFailedCpuTime().roundTo(MILLISECONDS);

            localScheduled &= stageStats.isScheduled();

            localFullyBlocked &= stageStats.isFullyBlocked();
            localBlockedReasons.addAll(stageStats.getBlockedReasons());

            localPhysicalInputDataSize += stageStats.getPhysicalInputDataSize().toBytes();
            localPhysicalInputPositions += stageStats.getPhysicalInputPositions();

            localInternalNetworkInputDataSize += stageStats.getInternalNetworkInputDataSize().toBytes();
            localInternalNetworkInputPositions += stageStats.getInternalNetworkInputPositions();

            localRawInputDataSize += stageStats.getRawInputDataSize().toBytes();
            localRawInputPositions += stageStats.getRawInputPositions();
        }

        OptionalDouble localProgressPercentage = OptionalDouble.empty();
        if (localScheduled && localTotalDrivers != 0) {
            localProgressPercentage = OptionalDouble.of(min(100, (localCompletedDrivers * 100.0) / localTotalDrivers));
        }

        return new BasicStageStats(
                localScheduled,

                localFailedTasks,

                localTotalDrivers,
                localQueuedDrivers,
                localRunningDrivers,
                localCompletedDrivers,

                succinctBytes(localPhysicalInputDataSize),
                localPhysicalInputPositions,

                succinctBytes(localInternalNetworkInputDataSize),
                localInternalNetworkInputPositions,

                succinctBytes(localRawInputDataSize),
                localRawInputPositions,

                localCumulativeUserMemory,
                failedLocalCumulativeUserMemory,
                succinctBytes(localUserMemoryReservation),
                succinctBytes(localTotalMemoryReservation),

                new Duration(localTotalCpuTime, MILLISECONDS).convertToMostSuccinctTimeUnit(),
                new Duration(localFailedCpuTime, MILLISECONDS).convertToMostSuccinctTimeUnit(),
                new Duration(totalScheduledTimeMillis, MILLISECONDS).convertToMostSuccinctTimeUnit(),
                new Duration(failedScheduledTimeMillis, MILLISECONDS).convertToMostSuccinctTimeUnit(),

                localFullyBlocked,
                localBlockedReasons,

                localProgressPercentage);
    }
}
