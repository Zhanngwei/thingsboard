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
package org.thingsboard.monitoring.config.transport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.CoapTransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.HttpTransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.Lwm2mTransportHealthChecker;
import org.thingsboard.monitoring.service.transport.impl.MqttTransportHealthChecker;

/**
 * 中文说明：
 * 1. `TransportType` 是 ThingsBoard Monitoring 中定义传输层固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
@AllArgsConstructor
@Getter
public enum TransportType {

    MQTT("MQTT", MqttTransportHealthChecker.class),
    COAP("CoAP",CoapTransportHealthChecker.class),
    HTTP("HTTP", HttpTransportHealthChecker.class),
    LWM2M("LwM2M", Lwm2mTransportHealthChecker.class);

    /**
     * 名称，用于标识或展示当前对象。
     */
    private final String name;
    private final Class<? extends TransportHealthChecker<?>> serviceClass;

}
