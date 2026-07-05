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
package org.thingsboard.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.TbActorSystemSettings;
import org.thingsboard.server.actors.app.AppActor;
import org.thingsboard.server.actors.app.AppInitMsg;
import org.thingsboard.server.actors.stats.StatsActor;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultActorService` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Service
@Slf4j
public class DefaultActorService extends TbApplicationEventListener<PartitionChangeEvent> implements ActorService {

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String APP_DISPATCHER_NAME = "app-dispatcher";
    public static final String TENANT_DISPATCHER_NAME = "tenant-dispatcher";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_DISPATCHER_NAME = "device-dispatcher";
    public static final String RULE_DISPATCHER_NAME = "rule-dispatcher";

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private ActorSystemContext actorContext;

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    private TbActorSystem system;

    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    private TbActorRef appActor;

    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    @Value("${actors.system.throughput:5}")
    private int actorThroughput;

    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    @Value("${actors.system.max_actor_init_attempts:10}")
    private int maxActorInitAttempts;

    /**
     * 线程池大小，用于安排延迟任务或周期任务。
     */
    @Value("${actors.system.scheduler_pool_size:1}")
    private int schedulerPoolSize;

    /**
     * `appDispatcherSize` 字段，保存当前对象的对应属性。
     */
    @Value("${actors.system.app_dispatcher_pool_size:1}")
    private int appDispatcherSize;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Value("${actors.system.tenant_dispatcher_pool_size:2}")
    private int tenantDispatcherSize;

    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Value("${actors.system.device_dispatcher_pool_size:4}")
    private int deviceDispatcherSize;

    /**
     * `ruleDispatcherSize` 字段，保存当前对象的对应属性。
     */
    @Value("${actors.system.rule_dispatcher_pool_size:8}")
    private int ruleDispatcherSize;

    /**
     * 功能：初始化或启动Actor 系统。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initActorSystem() {
        log.info("Initializing actor system.");
        actorContext.setActorService(this);
        TbActorSystemSettings settings = new TbActorSystemSettings(actorThroughput, schedulerPoolSize, maxActorInitAttempts);
        system = new DefaultTbActorSystem(settings);

        system.createDispatcher(APP_DISPATCHER_NAME, initDispatcherExecutor(APP_DISPATCHER_NAME, appDispatcherSize));
        system.createDispatcher(TENANT_DISPATCHER_NAME, initDispatcherExecutor(TENANT_DISPATCHER_NAME, tenantDispatcherSize));
        system.createDispatcher(DEVICE_DISPATCHER_NAME, initDispatcherExecutor(DEVICE_DISPATCHER_NAME, deviceDispatcherSize));
        system.createDispatcher(RULE_DISPATCHER_NAME, initDispatcherExecutor(RULE_DISPATCHER_NAME, ruleDispatcherSize));

        actorContext.setActorSystem(system);

        appActor = system.createRootActor(APP_DISPATCHER_NAME, new AppActor.ActorCreator(actorContext));
        actorContext.setAppActor(appActor);

        TbActorRef statsActor = system.createRootActor(TENANT_DISPATCHER_NAME, new StatsActor.ActorCreator(actorContext, "StatsActor"));
        actorContext.setStatsActor(statsActor);

        log.info("Actor system initialized.");
    }

    /**
     * 功能：初始化或启动执行器。
     * 参数：
     * - `dispatcherName`：名称。
     * - `poolSize`：`poolSize` 参数。
     * 返回：处理结果。
     */
    private ExecutorService initDispatcherExecutor(String dispatcherName, int poolSize) {
        if (poolSize == 0) {
            int cores = Runtime.getRuntime().availableProcessors();
            poolSize = Math.max(1, cores / 2);
        }
        if (poolSize == 1) {
            return Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(dispatcherName));
        } else {
            return ThingsBoardExecutors.newWorkStealingPool(poolSize, dispatcherName);
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `applicationReadyEvent`：`applicationReadyEvent` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.ACTOR_SYSTEM)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Sending application init message to actor system");
        appActor.tellWithHighPriority(new AppInitMsg());
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        log.info("Received partition change event.");
        appActor.tellWithHighPriority(new PartitionChangeMsg(event.getServiceType()));
    }

    /**
     * 功能：执行 `filterTbApplicationEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return event.getServiceType() == ServiceType.TB_RULE_ENGINE || event.getServiceType() == ServiceType.TB_CORE;
    }

    /**
     * 功能：停止或关闭Actor 系统。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void stopActorSystem() {
        if (system != null) {
            log.info("Stopping actor system.");
            system.stop();
            log.info("Actor system stopped.");
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultActorService` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
