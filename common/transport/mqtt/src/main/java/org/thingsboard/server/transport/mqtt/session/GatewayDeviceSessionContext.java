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
package org.thingsboard.server.transport.mqtt.session;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by nickAS21 on 26.12.22
 */
/**
 * 中文说明：
 * 1. `GatewayDeviceSessionContext` 是 ThingsBoard Common Transport 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AbstractGatewayDeviceSessionContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class GatewayDeviceSessionContext extends AbstractGatewayDeviceSessionContext<GatewaySessionHandler> {

    /**
     * 功能：创建 `GatewayDeviceSessionContext` 实例，并初始化必要字段。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `deviceInfo`：设备信息或设备标识。
     * - `deviceProfile`：设备信息或设备标识。
     * - `mqttQoSMap`：键值映射。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public GatewayDeviceSessionContext(GatewaySessionHandler parent,
                                       TransportDeviceInfo deviceInfo,
                                       DeviceProfile deviceProfile,
                                       ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap,
                                       TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

}
