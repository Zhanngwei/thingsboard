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
package org.thingsboard.server.common.stats;

import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. 类目的：`TbApiUsageReportClient` 是ThingsBoard Common 模块中的统计指标契约类型，用于定义运行时统计项、计数器和持久化消息的数据结构。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Actor、Queue、Application 统计服务、监控和日志系统。
 * 4. 生命周期：由运行期采样、周期持久化或测试流程创建和消费。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / DTO。
 */
public interface TbApiUsageReportClient {

    /**
     * 功能：执行 `report` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value);

    /**
     * 功能：执行 `report` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `key`：键。
     * 返回：无。
     */
    void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbApiUsageReportClient` 在 ThingsBoard Common 模块 中承担统计指标契约类型职责，核心目的是定义运行时统计项、计数器和持久化消息的数据结构。
 * 2. 核心流程：采集运行时指标后聚合为统计消息并交给持久化或监控流程。
 * 3. 关键依赖：主要依赖或协作对象包括Actor、Queue、Application 统计服务、监控和日志系统。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
