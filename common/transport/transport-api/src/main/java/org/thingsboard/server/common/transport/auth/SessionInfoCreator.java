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
package org.thingsboard.server.common.transport.auth;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `SessionInfoCreator` 是 ThingsBoard Common Transport 中创建或提供会话对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Slf4j
public class SessionInfoCreator {

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `context`：处理上下文。
     * - `sessionId`：会话ID。
     * 返回：处理结果。
     */
    public static TransportProtos.SessionInfoProto create(ValidateDeviceCredentialsResponse msg, TransportContext context, UUID sessionId) {
        return getSessionInfoProto(msg, context.getNodeId(), sessionId);
    }

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `nodeId`：节点实例ID。
     * - `sessionId`：会话ID。
     * 返回：处理结果。
     */
    public static TransportProtos.SessionInfoProto create(ValidateDeviceCredentialsResponse msg, String nodeId, UUID sessionId) {
        return getSessionInfoProto(msg, nodeId, sessionId);
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `msg`：待处理消息。
     * - `nodeId`：节点实例ID。
     * - `sessionId`：会话ID。
     * 返回：处理结果。
     */
    private static TransportProtos.SessionInfoProto getSessionInfoProto(ValidateDeviceCredentialsResponse msg, String nodeId, UUID sessionId) {
        return TransportProtos.SessionInfoProto.newBuilder().setNodeId(nodeId)
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits())
                .build();
    }

}
