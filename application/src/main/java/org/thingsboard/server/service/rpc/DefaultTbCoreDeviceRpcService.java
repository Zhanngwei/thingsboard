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
package org.thingsboard.server.service.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. `DefaultTbCoreDeviceRpcService` 是 ThingsBoard Application 中负责设备的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbCoreDeviceRpcService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@TbCoreComponent
public class DefaultTbCoreDeviceRpcService implements TbCoreDeviceRpcService {

    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final DeviceService deviceService;
    private final TbClusterService clusterService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final ActorSystemContext actorContext;

    private final ConcurrentMap<UUID, Consumer<FromDeviceRpcResponse>> localToRuleEngineRpcRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ToDeviceRpcRequestActorMsg> localToDeviceRpcRequests = new ConcurrentHashMap<>();

    /**
     * 规则引擎，提供当前类调用的业务操作。
     */
    private Optional<TbRuleEngineDeviceRpcService> tbRuleEngineRpcService;
    private ScheduledExecutorService scheduler;
    /**
     * 服务ID，用于定位对应业务对象。
     */
    private String serviceId;

    /**
     * 功能：创建 `DefaultTbCoreDeviceRpcService` 实例，并初始化必要字段。
     * 参数：
     * - `deviceService`：设备信息或设备标识。
     * - `clusterService`：服务对象。
     * - `serviceInfoProvider`：服务对象。
     * - `actorContext`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public DefaultTbCoreDeviceRpcService(DeviceService deviceService, TbClusterService clusterService, TbServiceInfoProvider serviceInfoProvider,
                                         ActorSystemContext actorContext) {
        this.deviceService = deviceService;
        this.clusterService = clusterService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.actorContext = actorContext;
    }

    /**
     * 功能：更新规则引擎。
     * 参数：
     * - `tbRuleEngineRpcService`：服务对象。
     * 返回：无。
     */
    @Autowired(required = false)
    public void setTbRuleEngineRpcService(Optional<TbRuleEngineDeviceRpcService> tbRuleEngineRpcService) {
        this.tbRuleEngineRpcService = tbRuleEngineRpcService;
    }

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-core-rpc-scheduler"));
        serviceId = serviceInfoProvider.getServiceId();
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdownExecutor() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `request`：请求对象。
     * - `responseConsumer`：响应对象。
     * - `currentUser`：`currentUser` 参数。
     * 返回：无。
     */
    @Override
    public void processRestApiRpcRequest(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer, SecurityUser currentUser) {
        log.trace("[{}][{}] Processing REST API call to rule engine [{}]", request.getTenantId(), request.getId(), request.getDeviceId());
        UUID requestId = request.getId();
        localToRuleEngineRpcRequests.put(requestId, responseConsumer);
        sendRpcRequestToRuleEngine(request, currentUser);
        scheduleToRuleEngineTimeout(request, requestId);
    }

    /**
     * 功能：处理规则引擎。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void processRpcResponseFromRuleEngine(FromDeviceRpcResponse response) {
        log.trace("[{}] Received response to server-side RPC request from rule engine: [{}]", response.getId(), response);
        UUID requestId = response.getId();
        Consumer<FromDeviceRpcResponse> consumer = localToRuleEngineRpcRequests.remove(requestId);
        if (consumer != null) {
            consumer.accept(response);
        } else {
            log.trace("[{}] Unknown or stale rpc response received [{}]", requestId, response);
        }
    }

    /**
     * 功能：执行 `forwardRpcRequestToDeviceActor` 对应的处理。
     * 参数：
     * - `rpcMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void forwardRpcRequestToDeviceActor(ToDeviceRpcRequestActorMsg rpcMsg) {
        ToDeviceRpcRequest request = rpcMsg.getMsg();
        log.trace("[{}][{}] Processing local rpc call to device actor [{}]", request.getTenantId(), request.getId(), request.getDeviceId());
        UUID requestId = request.getId();
        localToDeviceRpcRequests.put(requestId, rpcMsg);
        actorContext.tellWithHighPriority(rpcMsg);
        scheduleToDeviceTimeout(request, requestId);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void processRpcResponseFromDeviceActor(FromDeviceRpcResponse response) {
        log.trace("[{}] Received response to server-side RPC request from device actor.", response.getId());
        UUID requestId = response.getId();
        ToDeviceRpcRequestActorMsg request = localToDeviceRpcRequests.remove(requestId);
        if (request != null) {
            sendRpcResponseToTbRuleEngine(request.getServiceId(), response);
        } else {
            log.trace("[{}] Unknown or stale rpc response received [{}]", requestId, response);
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `removeRpcMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void processRemoveRpc(RemoveRpcActorMsg removeRpcMsg) {
        log.trace("[{}][{}] Processing remove RPC [{}]", removeRpcMsg.getTenantId(), removeRpcMsg.getRequestId(), removeRpcMsg.getDeviceId());
        actorContext.tellWithHighPriority(removeRpcMsg);
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `originServiceId`：服务ID。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void sendRpcResponseToTbRuleEngine(String originServiceId, FromDeviceRpcResponse response) {
        if (serviceId.equals(originServiceId)) {
            if (tbRuleEngineRpcService.isPresent()) {
                tbRuleEngineRpcService.get().processRpcResponseFromDevice(response);
            } else {
                log.warn("Failed to find tbCoreRpcService for local service. Possible duplication of serviceIds.");
            }
        } else {
            clusterService.pushNotificationToRuleEngine(originServiceId, response, null);
        }
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `msg`：待处理消息。
     * - `currentUser`：`currentUser` 参数。
     * 返回：无。
     */
    private void sendRpcRequestToRuleEngine(ToDeviceRpcRequest msg, SecurityUser currentUser) {
        ObjectNode entityNode = JacksonUtil.newObjectNode();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("requestUUID", msg.getId().toString());
        metaData.putValue("originServiceId", serviceId);
        metaData.putValue("expirationTime", Long.toString(msg.getExpirationTime()));
        metaData.putValue("oneway", Boolean.toString(msg.isOneway()));
        metaData.putValue(DataConstants.PERSISTENT, Boolean.toString(msg.isPersisted()));

        if (msg.getRetries() != null) {
            metaData.putValue(DataConstants.RETRIES, msg.getRetries().toString());
        }


        Device device = deviceService.findDeviceById(msg.getTenantId(), msg.getDeviceId());
        if (device != null) {
            metaData.putValue("deviceName", device.getName());
            metaData.putValue("deviceType", device.getType());
        }

        entityNode.put("method", msg.getBody().getMethod());
        entityNode.put("params", msg.getBody().getParams());

        entityNode.put(DataConstants.ADDITIONAL_INFO, msg.getAdditionalInfo());

        try {
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, msg.getDeviceId(), Optional.ofNullable(currentUser).map(User::getCustomerId).orElse(null), metaData, TbMsgDataType.JSON, JacksonUtil.toString(entityNode));
            clusterService.pushMsgToRuleEngine(msg.getTenantId(), msg.getDeviceId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `scheduleToRuleEngineTimeout` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `requestId`：请求ID。
     * 返回：无。
     */
    private void scheduleToRuleEngineTimeout(ToDeviceRpcRequest request, UUID requestId) {
        long timeout = Math.max(0, request.getExpirationTime() - System.currentTimeMillis()) + TimeUnit.SECONDS.toMillis(1);
        log.trace("[{}] processing to rule engine request.", requestId);
        scheduler.schedule(() -> {
            log.trace("[{}] timeout for processing to rule engine request.", requestId);
            Consumer<FromDeviceRpcResponse> consumer = localToRuleEngineRpcRequests.remove(requestId);
            if (consumer != null) {
                consumer.accept(new FromDeviceRpcResponse(requestId, null, RpcError.TIMEOUT));
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 功能：执行 `scheduleToDeviceTimeout` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `requestId`：请求ID。
     * 返回：无。
     */
    private void scheduleToDeviceTimeout(ToDeviceRpcRequest request, UUID requestId) {
        long timeout = Math.max(0, request.getExpirationTime() - System.currentTimeMillis()) + TimeUnit.SECONDS.toMillis(1);
        log.trace("[{}] processing to device request.", requestId);
        scheduler.schedule(() -> {
            log.trace("[{}] timeout for to device request.", requestId);
            localToDeviceRpcRequests.remove(requestId);
        }, timeout, TimeUnit.MILLISECONDS);
    }

}
