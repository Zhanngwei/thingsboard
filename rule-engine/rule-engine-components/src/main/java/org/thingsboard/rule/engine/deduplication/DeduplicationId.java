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
package org.thingsboard.rule.engine.deduplication;

/**
 * 中文说明：`DeduplicationId` 是去重标识枚举，用于限定按配置聚合、去重、延迟和输出消息时可选择的固定值。
 * 调用边界：本枚举本身不直接涉及数据库、缓存、Rule Engine、Actor、MQTT 或事务，只作为配置或流程判断的类型值。
 */
public enum DeduplicationId {

    /**
     * 客户常量，用于统一引用固定值。
     */
    ORIGINATOR, TENANT, CUSTOMER

    /*
     * 本类总结：`DeduplicationId` 负责按配置聚合、去重、延迟和输出消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
