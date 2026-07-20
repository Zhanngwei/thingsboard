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

import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `MultipleTbQueueCallbackWrapper` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueCallback`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class MultipleTbQueueCallbackWrapper implements TbQueueCallback {

    /**
     * 队列，用于接收异步处理完成后的结果。
     */
    private final AtomicInteger tbQueueCallbackCount;
    private final TbQueueCallback callback;

    /**
     * 功能：创建 `MultipleTbQueueCallbackWrapper` 实例，并初始化必要字段。
     * 参数：
     * - `tbQueueCallbackCount`：处理完成后的回调。
     * - `callback`：处理完成后的回调。
     * 返回：新创建的对象实例。
     */
    public MultipleTbQueueCallbackWrapper(int tbQueueCallbackCount, TbQueueCallback callback) {
        this.tbQueueCallbackCount = new AtomicInteger(tbQueueCallbackCount);
        this.callback = callback;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `metadata`：待处理数据。
     * 返回：无。
     */
    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        if (tbQueueCallbackCount.decrementAndGet() <= 0) {
            callback.onSuccess(metadata);
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable t) {
        callback.onFailure(new RuleEngineException(t.getMessage(), t));
    }
}
