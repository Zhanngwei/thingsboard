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
package org.thingsboard.rule.engine.mqtt.azure;

import lombok.Data;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;

/**
 * `TbAzureIotHubNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbAzureIotHubNodeConfiguration extends TbMqttNodeConfiguration {

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbAzureIotHubNodeConfiguration defaultConfiguration() {
        TbAzureIotHubNodeConfiguration configuration = new TbAzureIotHubNodeConfiguration();
        configuration.setTopicPattern("devices/<device_id>/messages/events/");
        configuration.setHost("<iot-hub-name>.azure-devices.net");
        configuration.setPort(8883);
        configuration.setConnectTimeoutSec(10);
        configuration.setCleanSession(true);
        configuration.setSsl(true);
        configuration.setCredentials(new AzureIotHubSasCredentials());
        return configuration;
    }

}

/*
 * 本类总结：
 * 本类只提供 Azure IoT Hub MQTT 节点的默认配置，实际 MQTT 客户端生命周期和发布逻辑在 TbAzureIotHubNode/TbMqttNode 中执行。
 * 本类本身不直接涉及外部调用、数据库、缓存或异步回调。
 */
