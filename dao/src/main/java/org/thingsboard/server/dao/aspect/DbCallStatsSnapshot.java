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
package org.thingsboard.server.dao.aspect;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`DbCallStatsSnapshot` 是 ThingsBoard DAO 模块 中的数据库调用统计切面类型，用于拦截 DAO 方法调用并统计数据库访问耗时、调用次数和快照指标。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring AOP、DAO 实现、Stats 模块、日志和运维监控流程。
 * 4. 生命周期：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Aspect / Observer / Decorator。
 */
@Data
@Builder
public class DbCallStatsSnapshot {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final int totalSuccess;
    /**
     * 失败信息，表示当前对象的对应属性。
     */
    private final int totalFailure;
    private final long totalTiming;
    /**
     * `methodStats`映射关系，用于按键查找对应值。
     */
    private final Map<String, MethodCallStatsSnapshot> methodStats;

    /**
     * 功能：获取`Total Calls`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getTotalCalls() {
        return totalSuccess + totalFailure;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DbCallStatsSnapshot` 在 ThingsBoard DAO 模块 中承担数据库调用统计切面类型职责，核心目的是拦截 DAO 方法调用并统计数据库访问耗时、调用次数和快照指标。
 * 2. 核心流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
 * 3. 关键依赖：主要依赖或协作对象包括Spring AOP、DAO 实现、Stats 模块、日志和运维监控流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
