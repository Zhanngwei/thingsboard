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

import lombok.NonNull;
import lombok.ToString;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. `ThingsBoardForkJoinWorkerThreadFactory` 是 ThingsBoard Common 中创建或提供 `Things Board Fork` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `ForkJoinWorkerThreadFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@ToString
public class ThingsBoardForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    /**
     * 名称，用于展示或标识当前对象。
     */
    private final String namePrefix;
    private final AtomicLong threadNumber = new AtomicLong(1);

    /**
     * 功能：创建 `ThingsBoardForkJoinWorkerThreadFactory` 实例，并初始化必要字段。
     * 参数：
     * - `namePrefix`：名称。
     * 返回：新创建的对象实例。
     */
    public ThingsBoardForkJoinWorkerThreadFactory(@NonNull String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * 功能：执行 `newThread` 对应的处理。
     * 参数：
     * - `pool`：`pool` 参数。
     * 返回：处理结果。
     */
    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setContextClassLoader(this.getClass().getClassLoader());
        thread.setName(namePrefix +"-"+thread.getPoolIndex()+"-"+threadNumber.getAndIncrement());
        return thread;
    }
}
