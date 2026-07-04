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

/**
 * 中文说明：`TbMsgCallbackWrapper` 是消息回调包装器接口，用于抽象转换消息体、元数据、发起实体或拆分/包装规则链消息中的可替换行为。
 * 调用边界：接口本身不直接涉及数据库、缓存、Rule Engine、Actor、MQTT 或事务；具体实现或调用链可能涉及。
 */
public interface TbMsgCallbackWrapper {

    /**
     * 方法说明：处理异步调用成功回调并继续规则链投递。
     * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
     */
    void onSuccess();

    /**
     * 方法说明：处理异步调用失败回调并转入失败关系。
     * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
     */
    void onFailure(Throwable t);
    /*
     * 本类总结：`TbMsgCallbackWrapper` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
