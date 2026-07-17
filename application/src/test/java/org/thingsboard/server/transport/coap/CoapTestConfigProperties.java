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
package org.thingsboard.server.transport.coap;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;

/**
 * 中文说明：
 * 1. `CoapTestConfigProperties` 是 ThingsBoard Application 中描述 CoAP 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@Builder
public class CoapTestConfigProperties {

    /**
     * 设备，用于标识或展示当前对象。
     */
    String deviceName;

    /**
     * 设备，用于区分不同处理分支。
     */
    CoapDeviceType coapDeviceType;

    /**
     * 消息载荷，用于区分不同处理分支。
     */
    TransportPayloadType transportPayloadType;

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    String telemetryTopicFilter;
    String attributesTopicFilter;

    /**
     * 遥测，表示当前对象的对应属性。
     */
    String telemetryProtoSchema;
    String attributesProtoSchema;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    String rpcResponseProtoSchema;
    String rpcRequestProtoSchema;

    /**
     * 类型，用于区分不同处理分支。
     */
    DeviceProfileProvisionType provisionType;
    String provisionKey;
    /**
     * 密钥，用于认证或安全校验。
     */
    String provisionSecret;

}
