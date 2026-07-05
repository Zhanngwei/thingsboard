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
package org.thingsboard.server.common.transport.limits;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;

import java.net.InetSocketAddress;

/**
 * 中文说明：
 * 1. 类目的：`TransportRateLimitService` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
public interface TransportRateLimitService {

    /**
     * 功能：校验`Limits`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `dataPoints`：待处理数据。
     * 返回：判断结果。
     */
    EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `update`：`update` 参数。
     * 返回：无。
     */
    void update(TenantProfileUpdateResult update);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void update(TenantId tenantId);

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void remove(TenantId tenantId);

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    void remove(DeviceId deviceId);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `transportEnabled`：`transportEnabled` 参数。
     * 返回：无。
     */
    void update(TenantId tenantId, boolean transportEnabled);

    /**
     * 功能：校验`Address`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：判断结果。
     */
    boolean checkAddress(InetSocketAddress address);

    /**
     * 功能：处理`on Auth Success`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    void onAuthSuccess(InetSocketAddress address);

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    void onAuthFailure(InetSocketAddress address);

    /**
     * 功能：执行 `invalidateRateLimitsIpTable` 对应的处理。
     * 参数：
     * - `sessionInactivityTimeout`：会话对象。
     * 返回：无。
     */
    void invalidateRateLimitsIpTable(long sessionInactivityTimeout);

}

/*
 * 本类总结：
 * 1. 核心职责：`TransportRateLimitService` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
