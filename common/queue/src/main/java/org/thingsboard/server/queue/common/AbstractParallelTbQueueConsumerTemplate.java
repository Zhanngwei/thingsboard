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
package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `AbstractParallelTbQueueConsumerTemplate` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public abstract class AbstractParallelTbQueueConsumerTemplate<R, T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<R, T> {

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    protected ListeningExecutorService consumerExecutor;

    /**
     * 功能：创建 `AbstractParallelTbQueueConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public AbstractParallelTbQueueConsumerTemplate(String topic) {
        super(topic);
    }

    /**
     * 功能：初始化或启动执行器。
     * 参数：
     * - `threadPoolSize`：`threadPoolSize` 参数。
     * 返回：无。
     */
    protected void initNewExecutor(int threadPoolSize) {
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            try {
                consumerExecutor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.trace("Interrupted while waiting for consumer executor to stop");
            }
        }
        consumerExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threadPoolSize, ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    protected void shutdownExecutor() {
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }

}
