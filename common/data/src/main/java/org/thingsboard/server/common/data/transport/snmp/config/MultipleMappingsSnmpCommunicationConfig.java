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
package org.thingsboard.server.common.data.transport.snmp.config;

import lombok.Data;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;

import java.util.List;

/**
 * 中文说明：
 * 1. `MultipleMappingsSnmpCommunicationConfig` 是 ThingsBoard Common Data 中描述 SNMP 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `SnmpCommunicationConfig`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public abstract class MultipleMappingsSnmpCommunicationConfig implements SnmpCommunicationConfig {
    /**
     * `mappings`列表，用于保存一组待处理对象。
     */
    protected List<SnmpMapping> mappings;

    /**
     * 功能：判断`Valid`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isValid() {
        return mappings != null && !mappings.isEmpty() && mappings.stream().allMatch(mapping -> mapping != null && mapping.isValid());
    }

    /**
     * 功能：获取`All Mappings`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<SnmpMapping> getAllMappings() {
        return mappings;
    }
}
