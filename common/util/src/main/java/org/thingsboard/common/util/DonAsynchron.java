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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `DonAsynchron` 是 ThingsBoard Common 中围绕 `Don Asynchron` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class DonAsynchron {

    /**
     * 功能：执行 `withCallback` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure) {
        withCallback(future, onSuccess, onFailure, null);
    }

    /**
     * 功能：执行 `withCallback` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure, Executor executor) {
        FutureCallback<T> callback = new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                try {
                    onSuccess.accept(result);
                } catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onFailure.accept(t);
            }
        };
        if (executor != null) {
            Futures.addCallback(future, callback, executor);
        } else {
            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

    /**
     * 功能：执行 `submit` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * - `executor`：`executor` 参数。
     * 返回：匹配的数据集合。
     */
    public static <T> ListenableFuture<T> submit(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure, Executor executor) {
        return submit(task, onSuccess, onFailure, executor, null);
    }

    /**
     * 功能：执行 `submit` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * - `executor`：`executor` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    public static <T> ListenableFuture<T> submit(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure, Executor executor, Executor callbackExecutor) {
        ListenableFuture<T> future = Futures.submit(task, executor);
        withCallback(future, onSuccess, onFailure, callbackExecutor);
        return future;
    }

}
