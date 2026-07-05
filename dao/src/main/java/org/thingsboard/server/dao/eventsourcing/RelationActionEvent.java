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
package org.thingsboard.server.dao.eventsourcing;

import lombok.Data;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;

/**
 * 中文说明：
 * 1. 类目的：`RelationActionEvent` 是 ThingsBoard DAO 模块 中的审计与事件持久化类型，用于记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 生命周期：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Observer / Event Sourcing / Repository。
 */
@Data
public class RelationActionEvent {
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final EntityRelation relation;
    /**
     * 类型，用于区分不同处理分支。
     */
    private final ActionType actionType;
}

/*
 * 本类总结：
 * 1. 核心职责：`RelationActionEvent` 在 ThingsBoard DAO 模块 中承担审计与事件持久化类型职责，核心目的是记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 核心流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
 * 3. 关键依赖：主要依赖或协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
