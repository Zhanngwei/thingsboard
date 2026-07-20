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
package org.thingsboard.server.common.data.event;

import lombok.Getter;

/**
 * 中文说明：
 * 1. `EventType` 是 ThingsBoard Common Data 中定义事件固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum EventType {
    ERROR("error_event", "ERROR"),
    LC_EVENT("lc_event", "LC_EVENT"),
    STATS("stats_event", "STATS"),
    DEBUG_RULE_NODE("rule_node_debug_event", "DEBUG_RULE_NODE", true),
    DEBUG_RULE_CHAIN("rule_chain_debug_event", "DEBUG_RULE_CHAIN", true);

    /**
     * `table` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final String table;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @Getter
    private final String oldName;
    /**
     * 是否开启调试模式。
     */
    @Getter
    private final boolean debug;

    EventType(String table, String oldName) {
        this(table, oldName, false);
    }

    EventType(String table, String oldName, boolean debug) {
        this.table = table;
        this.oldName = oldName;
        this.debug = debug;
    }
}