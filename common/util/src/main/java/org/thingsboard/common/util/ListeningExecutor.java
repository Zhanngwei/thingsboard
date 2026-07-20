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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * 中文说明：
 * 1. `ListeningExecutor` 是 ThingsBoard Common 中定义 `Listening Executor` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Executor`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ListeningExecutor extends Executor {

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    <T> ListenableFuture<T> executeAsync(Callable<T> task);

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<?> executeAsync(Runnable task) {
        return executeAsync(() -> {
            task.run();
            return null;
        });
    }

    /**
     * 功能：执行 `submit` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    default <T> ListenableFuture<T> submit(Callable<T> task) {
        return executeAsync(task);
    }

    /**
     * 功能：执行 `submit` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<?> submit(Runnable task) {
        return executeAsync(task);
    }

}
