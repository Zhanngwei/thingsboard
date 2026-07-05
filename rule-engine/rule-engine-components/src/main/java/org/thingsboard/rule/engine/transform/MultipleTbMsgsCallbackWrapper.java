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
 * 中文说明：`MultipleTbMsgsCallbackWrapper` 是多消息Msgs回调包装器辅助类，用于转换消息体、元数据、发起实体或拆分/包装规则链消息。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
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
    /*
     * 本类总结：`MultipleTbMsgsCallbackWrapper` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}

