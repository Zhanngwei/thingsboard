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
package org.thingsboard.server.service.gateway_device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultGatewayNotificationsService` 是 ThingsBoard Application 中负责 `Gateway Notifications` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `GatewayNotificationsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGatewayNotificationsService implements GatewayNotificationsService {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private final static String DEVICE_RENAMED_METHOD_NAME = "gateway_device_renamed";
    private final static String DEVICE_DELETED_METHOD_NAME = "gateway_device_deleted";
    private final static Long rpcTimeout = TimeUnit.DAYS.toMillis(1);
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private TbCoreDeviceRpcService deviceRpcService;

    /**
     * 功能：处理设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceUpdated(Device device, Device oldDevice) {
        Optional<DeviceId> gatewayDeviceId = getGatewayDeviceIdFromAdditionalInfoInDevice(device);
        if (gatewayDeviceId.isPresent()) {
            ObjectNode renamedDeviceNode = JacksonUtil.newObjectNode();
            renamedDeviceNode.put(oldDevice.getName(), device.getName());
            ToDeviceRpcRequest rpcRequest = formDeviceToGatewayRPCRequest(device.getTenantId(), gatewayDeviceId.get(), renamedDeviceNode, DEVICE_RENAMED_METHOD_NAME);
            deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> {
                log.trace("Device renamed RPC with id: [{}] processed to gateway device with id: [{}], old device name: [{}], new device name: [{}]",
                        rpcRequest.getId(), gatewayDeviceId, oldDevice.getName(), device.getName());
            }, null);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceDeleted(Device device) {
        Optional<DeviceId> gatewayDeviceId = getGatewayDeviceIdFromAdditionalInfoInDevice(device);
        if (gatewayDeviceId.isPresent()) {
            TextNode deletedDeviceNode = new TextNode(device.getName());
            ToDeviceRpcRequest rpcRequest = formDeviceToGatewayRPCRequest(device.getTenantId(), gatewayDeviceId.get(), deletedDeviceNode, DEVICE_DELETED_METHOD_NAME);
            deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> {
                log.trace("Device deleted RPC with id: [{}] processed to gateway device with id: [{}], deleted device name: [{}]",
                        rpcRequest.getId(), gatewayDeviceId, device.getName());
            }, null);
        }
    }

    /**
     * 功能：执行 `formDeviceToGatewayRPCRequest` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `gatewayDeviceId`：设备IDID。
     * - `deviceDataNode`：设备信息或设备标识。
     * - `method`：`method` 参数。
     * 返回：处理结果。
     */
    private ToDeviceRpcRequest formDeviceToGatewayRPCRequest(TenantId tenantId, DeviceId gatewayDeviceId, JsonNode deviceDataNode, String method) {
        ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(method, JacksonUtil.toString(deviceDataNode));
        long expTime = System.currentTimeMillis() + rpcTimeout;
        UUID rpcRequestUUID = UUID.randomUUID();
        return new ToDeviceRpcRequest(rpcRequestUUID,
                tenantId,
                gatewayDeviceId,
                true,
                expTime,
                body,
                true,
                3,
                null
        );
    }

    /**
     * 功能：获取扩展信息。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：可能存在的结果。
     */
    private Optional<DeviceId> getGatewayDeviceIdFromAdditionalInfoInDevice(Device device) {
        JsonNode deviceAdditionalInfo = device.getAdditionalInfo();
        if (deviceAdditionalInfo != null && deviceAdditionalInfo.has(DataConstants.LAST_CONNECTED_GATEWAY)) {
            try {
                JsonNode lastConnectedGatewayIdNode = deviceAdditionalInfo.get(DataConstants.LAST_CONNECTED_GATEWAY);
                return Optional.of(new DeviceId(UUID.fromString(lastConnectedGatewayIdNode.asText())));
            } catch (RuntimeException e) {
                log.debug("[{}] Failed to decode connected gateway: {}", device.getId(), deviceAdditionalInfo);
            }
        }
        return Optional.empty();
    }
}
