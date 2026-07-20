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
package org.thingsboard.rule.engine.transform;

import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `MultipleTbMsgsCallbackWrapper` 是 ThingsBoard Rule Engine Components 中承载 `Multiple Tb Msgs` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbMsgCallbackWrapper`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class MultipleTbMsgsCallbackWrapper implements TbMsgCallbackWrapper {

    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    private final AtomicInteger tbMsgsCallbackCount;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    private final TbMsgCallback callback;

    /**
     * 功能：创建 `MultipleTbMsgsCallbackWrapper` 实例，并初始化必要字段。
     * 参数：
     * - `tbMsgsCallbackCount`：处理完成后的回调。
     * - `callback`：处理完成后的回调。
     * 返回：新创建的对象实例。
     */
    public MultipleTbMsgsCallbackWrapper(int tbMsgsCallbackCount, TbMsgCallback callback) {
        this.tbMsgsCallbackCount = new AtomicInteger(tbMsgsCallbackCount);
        this.callback = callback;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onSuccess() {
        if (tbMsgsCallbackCount.decrementAndGet() <= 0) {
            callback.onSuccess();
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

