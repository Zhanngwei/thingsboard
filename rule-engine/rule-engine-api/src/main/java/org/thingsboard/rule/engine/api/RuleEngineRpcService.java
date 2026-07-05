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
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine 发送设备 RPC、回复设备 RPC 和查询持久化 RPC 的服务边界。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的 RPC 子系统。
 * 3. 协作对象：与 RPC 规则节点、设备传输会话、持久化 RPC 存储和回调消费者协作。
 * 4. 生命周期：由 Spring 实现类长期存在，节点处理 RPC 消息时通过 {@link TbContext} 获取并调用。
 * 5. 设计原因：RPC 需要跨规则引擎、传输层和存储层，接口可避免节点直接耦合设备会话管理细节。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现可能通过传输协议下发、访问 RPC 数据库并触发 Rule Engine 回调。
 */
public interface RuleEngineRpcService {

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `serviceId`：服务ID。
     * - `sessionId`：会话ID。
     * - `requestId`：请求ID。
     * - `body`：`body` 参数。
     * 返回：无。
     */
    void sendRpcReplyToDevice(String serviceId, UUID sessionId, int requestId, String body);

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `request`：请求对象。
     * - `consumer`：`consumer` 参数。
     * 返回：无。
     */
    void sendRpcRequestToDevice(RuleEngineDeviceRpcRequest request, Consumer<RuleEngineDeviceRpcResponse> consumer);

    /**
     * 功能：获取RPC。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    Rpc findRpcById(TenantId tenantId, RpcId id);
}

/*
 * 本类总结：
 * 1. 核心职责：为 Rule Engine 提供设备 RPC 发送、回复和查询入口。
 * 2. 核心流程：规则节点构建请求或回复，RuleEngineRpcService 实现负责传输下发、回调和持久化查询。
 * 3. 关键依赖：RuleEngineDeviceRpcRequest、RuleEngineDeviceRpcResponse、Rpc、设备传输会话和 RPC 存储。
 * 4. 学习重点：RPC API 是规则引擎与设备传输层之间的重要解耦边界。
 */
