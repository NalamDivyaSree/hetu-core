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
package io.prestosql.execution.scheduler;

import io.prestosql.execution.RemoteTask;
import io.prestosql.sql.planner.plan.PlanFragmentId;

//TODO(SURYA): implement current interface for UT class: TestingTaskLifecycleListener
public interface TaskLifecycleListener
{
    void taskCreated(PlanFragmentId fragmentId, RemoteTask task);

    void noMoreTasks(PlanFragmentId fragmentId);

    TaskLifecycleListener NO_OP = new TaskLifecycleListener()
    {
        @Override
        public void taskCreated(PlanFragmentId fragmentId, RemoteTask task)
        {
        }

        @Override
        public void noMoreTasks(PlanFragmentId fragmentId)
        {
        }
    };
}
