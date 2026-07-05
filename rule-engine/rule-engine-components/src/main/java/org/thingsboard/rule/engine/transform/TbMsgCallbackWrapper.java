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
     * 功能：处理`on Success`。
     * 参数：无。
     * 返回：无。
     */
    void onSuccess();

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    void onFailure(Throwable t);
    /*
     * 本类总结：`TbMsgCallbackWrapper` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
