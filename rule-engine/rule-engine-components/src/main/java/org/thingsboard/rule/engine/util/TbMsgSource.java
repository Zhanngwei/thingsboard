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
package org.thingsboard.rule.engine.util;

/**
 * 指示规则节点从消息数据体还是元数据中读取值的枚举。
 * 本枚举本身不读写消息，只作为调用方选择数据来源的配置值。
 */
public enum TbMsgSource {

    /**
     * 从消息数据体读取。
     */
    DATA,
    /**
     * 从消息元数据读取。
     */
    METADATA

}

/*
 * 本类总结：
 * 本枚举提供消息字段来源的两个取值；实际 Rule Engine 消息解析由使用该枚举的节点或工具方法完成。
 */
