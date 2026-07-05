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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractSubscriptionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public abstract class AbstractSubscriptionService extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService clusterService;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Autowired
    protected PartitionService partitionService;
    /**
     * 订阅，提供当前类调用的业务操作。
     */
    @Autowired
    protected Optional<SubscriptionManagerService> subscriptionManagerService;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    protected ExecutorService wsCallBackExecutor;

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getExecutorPrefix();

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getExecutorPrefix() + "-service-ws-callback"));
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdownExecutor() {
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `partitionChangeEvent`：分区标识或分区信息。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());
        }
    }

    /**
     * 功能：执行 `forwardToSubscriptionManagerService` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `toSubscriptionManagerService`：服务对象。
     * - `toCore`：`toCore` 参数。
     * 返回：无。
     */
    protected void forwardToSubscriptionManagerService(TenantId tenantId, EntityId entityId,
                                                       Consumer<SubscriptionManagerService> toSubscriptionManagerService,
                                                       Supplier<TransportProtos.ToCoreMsg> toCore) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                toSubscriptionManagerService.accept(subscriptionManagerService.get());
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = toCore.get();
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `saveFuture`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    protected <T> void addWsCallback(ListenableFuture<T> saveFuture, Consumer<T> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, wsCallBackExecutor);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractSubscriptionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
