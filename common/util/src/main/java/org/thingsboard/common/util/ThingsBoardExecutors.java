/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
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
package org.thingsboard.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * 中文说明：
 * 1. `ThingsBoardExecutors` 是 ThingsBoard Common 中围绕 `Things Board Executors` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class ThingsBoardExecutors {

    /**
     * Method forked from ExecutorService to provide thread poll name
     *
     * Creates a thread pool that maintains enough threads to support
     * the given parallelism level, and may use multiple queues to
     * reduce contention. The parallelism level corresponds to the
     * maximum number of threads actively engaged in, or available to
     * engage in, task processing. The actual number of threads may
     * grow and shrink dynamically. A work-stealing pool makes no
     * guarantees about the order in which submitted tasks are
     * executed.
     *
     * @param parallelism the targeted parallelism level
     * @param namePrefix used to define thread name
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code parallelism <= 0}
     * @since 1.8
     */
    /**
     * 功能：执行 `newWorkStealingPool` 对应的处理。
     * 参数：
     * - `parallelism`：`parallelism` 参数。
     * - `namePrefix`：名称。
     * 返回：处理结果。
     */
    public static ExecutorService newWorkStealingPool(int parallelism, String namePrefix) {
        return new ForkJoinPool(parallelism,
                new ThingsBoardForkJoinWorkerThreadFactory(namePrefix),
                null, true);
    }

    /**
     * 功能：执行 `newWorkStealingPool` 对应的处理。
     * 参数：
     * - `parallelism`：`parallelism` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    public static ExecutorService newWorkStealingPool(int parallelism, Class clazz) {
        return newWorkStealingPool(parallelism, clazz.getSimpleName());
    }

}
