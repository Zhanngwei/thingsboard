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
package org.thingsboard.server.service.ttl.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.rpc.RpcDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`RpcCleanUpService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class RpcCleanUpService {
    @Value("${sql.ttl.rpc.enabled}")
    /**
     * 字段说明：
     * 1. 保存 `ttlTaskExecutionEnabled` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean ttlTaskExecutionEnabled;

    /**
     * 字段说明：
     * 1. 保存 `tenantService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TenantService tenantService;
    private final PartitionService partitionService;
    /**
     * 字段说明：
     * 1. 保存 `tenantProfileCache` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbTenantProfileCache tenantProfileCache;
    private final RpcDao rpcDao;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.rpc.checking_interval})}", fixedDelayString = "${sql.ttl.rpc.checking_interval}")
    /**
     * 方法说明：
     * 1. 职责：执行 `cleanUp` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void cleanUp() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ttlTaskExecutionEnabled) {
            PageLink tenantsBatchRequest = new PageLink(10_000, 0);
            PageData<TenantId> tenantsIds;
            do {
                tenantsIds = tenantService.findTenantsIds(tenantsBatchRequest);
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (TenantId tenantId : tenantsIds.getData()) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
                        continue;
                    }

                    // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
                    Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getRpcTtlDays() == 0) {
                        continue;
                    }

                    long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getRpcTtlDays());
                    long expirationTime = System.currentTimeMillis() - ttl;

                    // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
                    int totalRemoved = rpcDao.deleteOutdatedRpcByTenantId(tenantId, expirationTime);

                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (totalRemoved > 0) {
                        log.info("Removed {} outdated rpc(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
                    }
                }

                tenantsBatchRequest = tenantsBatchRequest.nextPageLink();
            } while (tenantsIds.hasNext());
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RpcCleanUpService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
