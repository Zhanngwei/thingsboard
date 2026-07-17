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
 * 1. `DefaultActorService` 是 ThingsBoard Application 中负责 `Actor` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbApplicationEventListener`、`ActorService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
