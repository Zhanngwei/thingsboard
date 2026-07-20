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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

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
 * 中文说明：
 * 1. `DefaultTbRuleEngineRpcService` 是 ThingsBoard Application 中负责 RPC 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbRuleEngineDeviceRpcService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineRpcService implements TbRuleEngineDeviceRpcService {

    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;
    private final TbClusterService clusterService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final RpcService rpcService;

    private final ConcurrentMap<UUID, Consumer<FromDeviceRpcResponse>> toDeviceRpcRequests = new ConcurrentHashMap<>();

    /**
     * RPC，提供当前类调用的业务操作。
     */
    private Optional<TbCoreDeviceRpcService> tbCoreRpcService;
    private ScheduledExecutorService scheduler;
    /**
     * 服务ID，用于定位对应业务对象。
     */
    private String serviceId;

    /**
     * 功能：创建 `DefaultTbRuleEngineRpcService` 实例，并初始化必要字段。
     * 参数：
     * - `partitionService`：服务对象。
     * - `clusterService`：服务对象。
     * - `serviceInfoProvider`：服务对象。
     * - `rpcService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTbRuleEngineRpcService(PartitionService partitionService,
                                         TbClusterService clusterService,
                                         TbServiceInfoProvider serviceInfoProvider,
                                         RpcService rpcService) {
        this.partitionService = partitionService;
        this.clusterService = clusterService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.rpcService = rpcService;
    }

    /**
     * 功能：更新RPC。
     * 参数：
     * - `tbCoreRpcService`：服务对象。
     * 返回：无。
     */
    @Autowired(required = false)
    public void setTbCoreRpcService(Optional<TbCoreDeviceRpcService> tbCoreRpcService) {
        this.tbCoreRpcService = tbCoreRpcService;
    }

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("rule-engine-rpc-scheduler"));
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
     * 功能：发送或提交设备。
     * 参数：
     * - `serviceId`：服务ID。
     * - `sessionId`：会话ID。
     * - `requestId`：请求ID。
     * - `body`：`body` 参数。
     * 返回：无。
     */
    @Override
    public void sendRpcReplyToDevice(String serviceId, UUID sessionId, int requestId, String body) {
        if (serviceId == null || serviceId.isEmpty()){
            log.trace("sendRpcReplyToDevice: skipping message without serviceId [{}], sessionId[{}], requestId[{}], body[{}]", serviceId, sessionId, requestId, body);
            return;
        }
        TransportProtos.ToServerRpcResponseMsg responseMsg = TransportProtos.ToServerRpcResponseMsg.newBuilder()
                .setRequestId(requestId)
                .setPayload(body).build();
        TransportProtos.ToTransportMsg msg = TransportProtos.ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToServerResponse(responseMsg)
                .build();
        clusterService.pushNotificationToTransport(serviceId, msg, null);
    }

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `src`：`src` 参数。
     * - `consumer`：`consumer` 参数。
     * 返回：无。
     */
    @Override
    public void sendRpcRequestToDevice(RuleEngineDeviceRpcRequest src, Consumer<RuleEngineDeviceRpcResponse> consumer) {
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(src.getRequestUUID(), src.getTenantId(), src.getDeviceId(),
                src.isOneway(), src.getExpirationTime(), new ToDeviceRpcRequestBody(src.getMethod(), src.getBody()), src.isPersisted(), src.getRetries(), src.getAdditionalInfo());
        forwardRpcRequestToDeviceActor(request, response -> {
            if (src.isRestApiCall()) {
                sendRpcResponseToTbCore(src.getOriginServiceId(), response);
            }
            consumer.accept(RuleEngineDeviceRpcResponse.builder()
                    .deviceId(src.getDeviceId())
                    .requestId(src.getRequestId())
                    .error(response.getError())
                    .response(response.getResponse())
                    .build());
        });
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public Rpc findRpcById(TenantId tenantId, RpcId id) {
        return rpcService.findById(tenantId, id);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void processRpcResponseFromDevice(FromDeviceRpcResponse response) {
        log.trace("[{}] Received response to server-side RPC request from Core RPC Service", response.getId());
        UUID requestId = response.getId();
        Consumer<FromDeviceRpcResponse> consumer = toDeviceRpcRequests.remove(requestId);
        if (consumer != null) {
            scheduler.submit(() -> consumer.accept(response));
        } else {
            log.trace("[{}] Unknown or stale rpc response received [{}]", requestId, response);
        }
    }

    /**
     * 功能：执行 `forwardRpcRequestToDeviceActor` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `responseConsumer`：响应对象。
     * 返回：无。
     */
    private void forwardRpcRequestToDeviceActor(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer) {
        log.trace("[{}][{}] Processing local rpc call to device actor [{}]", request.getTenantId(), request.getId(), request.getDeviceId());
        UUID requestId = request.getId();
        toDeviceRpcRequests.put(requestId, responseConsumer);
        sendRpcRequestToDevice(request);
        scheduleTimeout(request, requestId);
    }

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void sendRpcRequestToDevice(ToDeviceRpcRequest msg) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, msg.getTenantId(), msg.getDeviceId());
        ToDeviceRpcRequestActorMsg rpcMsg = new ToDeviceRpcRequestActorMsg(serviceId, msg);
        if (tpi.isMyPartition()) {
            log.trace("[{}] Forwarding msg {} to device actor!", msg.getDeviceId(), msg);
            if (tbCoreRpcService.isPresent()) {
                tbCoreRpcService.get().forwardRpcRequestToDeviceActor(rpcMsg);
            } else {
                log.warn("Failed to find tbCoreRpcService for local service. Possible duplication of serviceIds.");
            }
        } else {
            log.trace("[{}] Forwarding msg {} to queue actor!", msg.getDeviceId(), msg);
            clusterService.pushMsgToCore(rpcMsg, null);
        }
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `originServiceId`：服务ID。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void sendRpcResponseToTbCore(String originServiceId, FromDeviceRpcResponse response) {
        if (serviceId.equals(originServiceId)) {
            if (tbCoreRpcService.isPresent()) {
                tbCoreRpcService.get().processRpcResponseFromRuleEngine(response);
            } else {
                log.warn("Failed to find tbCoreRpcService for local service. Possible duplication of serviceIds.");
            }
        } else {
            clusterService.pushNotificationToCore(originServiceId, response, null);
        }
    }

    /**
     * 功能：执行 `scheduleTimeout` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `requestId`：请求ID。
     * 返回：无。
     */
    private void scheduleTimeout(ToDeviceRpcRequest request, UUID requestId) {
        long timeout = Math.max(0, request.getExpirationTime() - System.currentTimeMillis()) + TimeUnit.SECONDS.toMillis(1);
        log.trace("[{}] processing the request: [{}]", this.hashCode(), requestId);
        scheduler.schedule(() -> {
            log.trace("[{}] timeout the request: [{}]", this.hashCode(), requestId);
            Consumer<FromDeviceRpcResponse> consumer = toDeviceRpcRequests.remove(requestId);
            if (consumer != null) {
                scheduler.submit(() -> consumer.accept(new FromDeviceRpcResponse(requestId, null, RpcError.TIMEOUT)));
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }
}
