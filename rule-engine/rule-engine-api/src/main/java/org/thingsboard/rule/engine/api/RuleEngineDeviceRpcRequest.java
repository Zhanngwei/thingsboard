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
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId;
    /**
     * 请求ID，用于定位对应业务对象。
     */
    private final int requestId;
    /**
     * 请求ID，用于定位对应业务对象。
     */
    private final UUID requestUUID;
    /**
     * 服务ID，用于定位对应业务对象。
     */
    private final String originServiceId;
    /**
     * 是否满足`oneway`条件。
     */
    private final boolean oneway;
    /**
     * 是否满足`persisted`条件。
     */
    private final boolean persisted;
    /**
     * `method` 字段，保存当前对象的对应属性。
     */
    private final String method;
    /**
     * `body` 字段，保存当前对象的对应属性。
     */
    private final String body;
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    private final long expirationTime;
    /**
     * 是否满足`restApiCall`条件。
     */
    private final boolean restApiCall;
    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    private final String additionalInfo;
    /**
     * `retries` 字段，保存当前对象的对应属性。
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
