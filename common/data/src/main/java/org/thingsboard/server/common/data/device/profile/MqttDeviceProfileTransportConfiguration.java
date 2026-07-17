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

import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：
 * 1. `MqttDeviceProfileTransportConfiguration` 是 ThingsBoard Common Data 中描述设备配置行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `DeviceProfileTransportConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class MqttDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    /**
     * 设备，用于匹配或发送对应主题的数据。
     */
    @NoXss
    private String deviceTelemetryTopic = MqttTopics.DEVICE_TELEMETRY_TOPIC;
    /**
     * 设备，用于匹配或发送对应主题的数据。
     */
    @NoXss
    private String deviceAttributesTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
    /**
     * 设备，用于匹配或发送对应主题的数据。
     */
    @NoXss
    private String deviceAttributesSubscribeTopic = MqttTopics.DEVICE_ATTRIBUTES_TOPIC;

    /**
     * 消息载荷，保存当前对象的配置选项。
     */
    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
    private boolean sparkplug;
    /**
     * Sparkplug集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<String> sparkplugAttributesMetricNames;
    private boolean sendAckOnValidationException;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MQTT;
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    public TransportPayloadTypeConfiguration getTransportPayloadTypeConfiguration() {
        return Objects.requireNonNullElseGet(transportPayloadTypeConfiguration, JsonTransportPayloadConfiguration::new);
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeviceTelemetryTopic() {
        return StringUtils.notBlankOrDefault(deviceTelemetryTopic, MqttTopics.DEVICE_TELEMETRY_TOPIC);
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeviceAttributesTopic() {
        return StringUtils.notBlankOrDefault(deviceAttributesTopic, MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeviceAttributesSubscribeTopic() {
        return StringUtils.notBlankOrDefault(deviceAttributesSubscribeTopic, MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
    }

}
