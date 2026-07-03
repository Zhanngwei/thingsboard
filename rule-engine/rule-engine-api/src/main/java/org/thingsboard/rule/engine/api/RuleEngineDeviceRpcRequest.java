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

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：封装 Rule Engine 向设备发起服务端 RPC 请求所需的全部上下文。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的设备 RPC 数据模型。
 * 3. 协作对象：与 {@link RuleEngineRpcService}、RPC 规则节点、设备会话层和持久化 RPC 流程协作。
 * 4. 生命周期：由 RPC 节点或 REST 调用流程创建，随一次 RPC 请求存在，响应或超时后结束。
 * 5. 设计原因：RPC 请求参数跨 Rule Engine、传输会话和持久化层传递，使用不可变 Builder DTO 可避免参数错位。
 * 6. 设计模式：Builder，用于构建包含多个可选字段的命令对象。
 * 7. 技术关联：本类本身不直接操作事务、缓存、MQTT、Actor、数据库；可能被实现层用于 MQTT/传输下发、RPC 持久化和 Rule Engine 回调。
 */
@Data
@Builder
public final class RuleEngineDeviceRpcRequest {

    /**
     * 中文说明：租户标识，来源于规则消息或 REST 调用上下文；生命周期与单次 RPC 请求一致，用于租户隔离。
     */
    private final TenantId tenantId;
    /**
     * 中文说明：目标设备标识，来源于消息发起者、节点配置或 REST 参数；生命周期与单次 RPC 请求一致。
     */
    private final DeviceId deviceId;
    /**
     * 中文说明：RPC 请求数字 ID，来源于调用方生成的会话内请求编号；生命周期用于匹配设备响应。
     */
    private final int requestId;
    /**
     * 中文说明：RPC 请求 UUID，来源于调用方生成的全局请求标识；生命周期用于跨服务追踪和持久化关联。
     */
    private final UUID requestUUID;
    /**
     * 中文说明：发起请求的服务实例 ID，来源于当前 ThingsBoard 服务节点；生命周期用于响应路由回原服务。
     */
    private final String originServiceId;
    /**
     * 中文说明：是否单向 RPC，来源于节点配置或调用参数；生命周期决定是否等待设备响应。
     */
    private final boolean oneway;
    /**
     * 中文说明：是否持久化 RPC，来源于调用参数；生命周期决定实现层是否写入 RPC 存储。
     */
    private final boolean persisted;
    /**
     * 中文说明：设备端方法名，来源于节点配置、消息数据或 REST 参数；生命周期与单次 RPC 请求一致。
     */
    private final String method;
    /**
     * 中文说明：RPC 请求体，来源于消息数据、节点模板或 REST 参数；生命周期与单次请求一致。
     */
    private final String body;
    /**
     * 中文说明：请求过期时间戳，来源于超时配置计算结果；生命周期用于传输层和持久化层判断请求是否超时。
     */
    private final long expirationTime;
    /**
     * 中文说明：标记请求是否来自 REST API 调用，来源于调用入口；生命周期用于区分回调和审计语义。
     */
    private final boolean restApiCall;
    /**
     * 中文说明：附加信息 JSON 字符串，来源于调用方扩展参数；生命周期用于持久化或响应处理时保留额外上下文。
     */
    private final String additionalInfo;
    /**
     * 中文说明：重试次数配置，来源于节点或 RPC 调用参数；生命周期用于实现层决定失败后的重试策略。
     */
    private final Integer retries;
}

/*
 * 本类总结：
 * 1. 核心职责：作为 Rule Engine 发起设备 RPC 的不可变请求命令对象。
 * 2. 核心流程：RPC 节点构建请求，RuleEngineRpcService 发送到设备传输层并等待响应或超时。
 * 3. 关键依赖：RuleEngineRpcService、DeviceId、TenantId、设备传输会话和持久化 RPC 实现。
 * 4. 学习重点：RPC 请求需要同时支持集群路由、单向/双向模式、持久化和 REST 调用来源。
 */
