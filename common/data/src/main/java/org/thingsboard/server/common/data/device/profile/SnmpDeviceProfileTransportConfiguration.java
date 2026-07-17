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
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;

import java.util.List;

/**
 * 中文说明：
 * 1. `SnmpDeviceProfileTransportConfiguration` 是 ThingsBoard Common Data 中描述设备配置行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `DeviceProfileTransportConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class SnmpDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private Integer timeoutMs;
    private Integer retries;
    /**
     * `communicationConfigs`列表，用于保存一组待处理对象。
     */
    private List<SnmpCommunicationConfig> communicationConfigs;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("SNMP transport configuration is not valid");
        }
    }

    /**
     * 功能：判断`Valid`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    private boolean isValid() {
        return timeoutMs != null && timeoutMs >= 0 && retries != null && retries >= 0
                && communicationConfigs != null
                && communicationConfigs.stream().allMatch(config -> config != null && config.isValid());
    }

}
