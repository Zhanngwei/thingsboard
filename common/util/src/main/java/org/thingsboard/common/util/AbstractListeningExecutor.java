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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;

/**
 * Created by igor on 4/13/18.
 */
/**
 * 中文说明：
 * 1. `AbstractListeningExecutor` 是 ThingsBoard Common 中处理 `Listening Executor` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ListeningExecutor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public abstract class AbstractListeningExecutor implements ListeningExecutor {

    /**
     * 服务列表，用于保存一组待处理对象。
     */
    private ListeningExecutorService service;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(getThreadPollSize(), getClass()));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        return service.submit(task);
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<?> executeAsync(Runnable task) {
        return service.submit(task);
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * 返回：无。
     */
    @Override
    public void execute(Runnable command) {
        service.execute(command);
    }

    /**
     * 功能：执行 `executor` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public ListeningExecutorService executor() {
        return service;
    }

    /**
     * 功能：获取`Thread Poll Size`。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract int getThreadPollSize();

}
