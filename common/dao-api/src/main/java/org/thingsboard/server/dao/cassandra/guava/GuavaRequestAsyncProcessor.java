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
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.session.DefaultSession;
import com.datastax.oss.driver.internal.core.session.RequestProcessor;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.CompletionStage;

/**
 * Wraps a {@link RequestProcessor} that returns {@link CompletionStage}s and converts them to a
 * {@link ListenableFuture}s.
 *
 * @param <T> The type of request
 * @param <U> The type of responses enclosed in the future response.
 */
/**
 * 中文说明：
 * 1. `GuavaRequestAsyncProcessor` 是 ThingsBoard Common 中处理请求的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `Request`、`RequestProcessor`、`ListenableFuture`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public class GuavaRequestAsyncProcessor<T extends Request, U>
        implements RequestProcessor<T, ListenableFuture<U>> {

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final RequestProcessor<T, CompletionStage<U>> subProcessor;

    /**
     * 类型，用于区分不同处理分支。
     */
    private final GenericType resultType;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final Class<?> requestClass;

    GuavaRequestAsyncProcessor(
            RequestProcessor<T, CompletionStage<U>> subProcessor,
            Class<?> requestClass,
            GenericType resultType) {
        this.subProcessor = subProcessor;
        this.requestClass = requestClass;
        this.resultType = resultType;
    }

    /**
     * 功能：执行 `canProcess` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `resultType`：类型。
     * 返回：判断结果。
     */
    @Override
    public boolean canProcess(Request request, GenericType resultType) {
        return requestClass.isInstance(request) && resultType.equals(this.resultType);
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `session`：会话对象。
     * - `context`：处理上下文。
     * - `sessionLogPrefix`：会话对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<U> process(
            T request, DefaultSession session, InternalDriverContext context, String sessionLogPrefix) {
        SettableFuture<U> future = SettableFuture.create();
        subProcessor
                .process(request, session, context, sessionLogPrefix)
                .whenComplete(
                        (r, ex) -> {
                            if (ex != null) {
                                future.setException(ex);
                            } else {
                                future.set(r);
                            }
                        });
        return future;
    }

    /**
     * 功能：执行 `newFailure` 对应的处理。
     * 参数：
     * - `error`：错误信息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<U> newFailure(RuntimeException error) {
        return Futures.immediateFailedFuture(error);
    }
}
