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
     * 字段说明：保存 `tbMsgsCallbackCount`，表示当前规则链消息，供本类方法在规则节点处理流程中使用。
     */
    private final AtomicInteger tbMsgsCallbackCount;
    /**
     * 字段说明：保存 `callback`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final TbMsgCallback callback;

    /**
     * 方法说明：构造 `MultipleTbMsgsCallbackWrapper` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    public MultipleTbMsgsCallbackWrapper(int tbMsgsCallbackCount, TbMsgCallback callback) {
        this.tbMsgsCallbackCount = new AtomicInteger(tbMsgsCallbackCount);
        this.callback = callback;
    }

    @Override
    /**
     * 方法说明：处理异步调用成功回调并继续规则链投递。
     * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
     */
    public void onSuccess() {
        if (tbMsgsCallbackCount.decrementAndGet() <= 0) {
            callback.onSuccess();
        }
    }

    @Override
    /**
     * 方法说明：处理异步调用失败回调并转入失败关系。
     * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
     */
    public void onFailure(Throwable t) {
        callback.onFailure(new RuleEngineException(t.getMessage(), t));
    }
    /*
     * 本类总结：`MultipleTbMsgsCallbackWrapper` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}

