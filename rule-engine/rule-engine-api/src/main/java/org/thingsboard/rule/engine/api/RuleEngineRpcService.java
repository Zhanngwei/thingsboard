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
     * 中文说明：
     * 1. 方法职责：把服务端生成的 RPC 回复发送给设备会话。
     * 2. 输入参数：serviceId 是目标服务标识，sessionId 是设备会话，requestId 是请求编号，body 是回复内容。
     * 3. 返回值：无；发送失败由实现内部处理或记录。
     * 4. 调用时机：规则节点处理完来自设备的 RPC 请求后调用。
     * 5. 调用方：RPC Reply 规则节点。
     * 6. 使用流程：属于 Rule Engine 到设备传输层的 RPC 回复流程。
     * 7. 线程安全：接口无状态，实现需保证会话查找和发送并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务或数据库；实现可能通过 MQTT/其它传输协议和集群通信发送，直接服务 Rule Engine。
     */
    void sendRpcReplyToDevice(String serviceId, UUID sessionId, int requestId, String body);

    /**
     * 中文说明：
     * 1. 方法职责：向设备发送服务端 RPC 请求并注册响应消费者。
     * 2. 输入参数：request 是 RPC 请求上下文，consumer 是响应或错误回调。
     * 3. 返回值：无；结果通过 consumer 异步返回。
     * 4. 调用时机：RPC Request 规则节点或 REST API 发起设备 RPC 时调用。
     * 5. 调用方：RPC Request 规则节点、REST RPC 入口。
     * 6. 使用流程：属于 Rule Engine 设备 RPC 请求流程。
     * 7. 线程安全：实现需保证请求注册、超时和回调并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务；实现可能访问持久化 RPC、缓存会话并通过 MQTT/传输层或 Actor/集群通信发送。
     */
    void sendRpcRequestToDevice(RuleEngineDeviceRpcRequest request, Consumer<RuleEngineDeviceRpcResponse> consumer);

    /**
     * 中文说明：
     * 1. 方法职责：按租户和 RPC ID 查询持久化 RPC 记录。
     * 2. 输入参数：tenantId 是租户边界，id 是 RPC 记录标识。
     * 3. 返回值：匹配的 Rpc 记录，未找到时由实现决定返回 null 或抛出异常。
     * 4. 调用时机：规则节点或 API 需要读取持久化 RPC 状态时调用。
     * 5. 调用方：RPC 相关规则节点、REST 查询流程。
     * 6. 使用流程：属于 RPC 状态查询流程。
     * 7. 线程安全：接口无状态，实现需保证 DAO 访问并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor；实现通常访问数据库，可能使用缓存，服务 Rule Engine。
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
