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
import org.thingsboard.server.common.data.CoapDeviceType;

/**
 * 中文说明：
 * 1. `DefaultCoapDeviceTypeConfiguration` 是 ThingsBoard Common Data 中描述设备行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `CoapDeviceTypeConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class DefaultCoapDeviceTypeConfiguration implements CoapDeviceTypeConfiguration {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -4287100699186773773L;

    /**
     * 消息载荷，保存当前对象的配置选项。
     */
    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CoapDeviceType getCoapDeviceType() {
        return CoapDeviceType.DEFAULT;
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    public TransportPayloadTypeConfiguration getTransportPayloadTypeConfiguration() {
        if (transportPayloadTypeConfiguration != null) {
            return transportPayloadTypeConfiguration;
        } else {
            return new JsonTransportPayloadConfiguration();
        }
    }

}
