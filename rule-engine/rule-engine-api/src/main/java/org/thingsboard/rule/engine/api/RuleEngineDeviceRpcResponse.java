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
import org.thingsboard.server.common.data.rpc.RpcError;

import java.util.Optional;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：封装设备 RPC 调用完成后的响应或错误信息。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的设备 RPC 返回数据模型。
 * 3. 协作对象：与 {@link RuleEngineRpcService}、RPC 请求节点、设备传输层和回调消费者协作。
 * 4. 生命周期：由 RPC 服务实现或传输层在收到响应、超时或失败时创建，随一次回调消费结束。
 * 5. 设计原因：响应和错误是互斥的可选结果，用 DTO 统一传递可减少回调接口复杂度。
 * 6. 设计模式：Builder，用于清晰构造成功或失败响应。
 * 7. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor、数据库；响应来源可能来自 MQTT/其它传输协议，直接服务 Rule Engine RPC 流程。
 */
@Data
@Builder
public final class RuleEngineDeviceRpcResponse {

    /**
     * 中文说明：响应所属设备标识，来源于请求上下文或传输会话；生命周期用于把响应关联回原 RPC 请求。
     */
    private final DeviceId deviceId;
    /**
     * 中文说明：响应对应的请求 ID，来源于原始 RPC 请求；生命周期用于会话内请求/响应匹配。
     */
    private final int requestId;
    /**
     * 中文说明：设备返回的成功响应体，来源于设备传输层；生命周期随回调消费结束。
     * 设计为 Optional 是为了明确表达可能没有成功响应。
     */
    private final Optional<String> response;
    /**
     * 中文说明：RPC 错误信息，来源于超时、设备离线、传输失败或服务端处理失败；生命周期随回调消费结束。
     * 设计为 Optional 是为了与成功响应互斥表达，避免使用 null 传递错误语义。
     */
    private final Optional<RpcError> error;

}

/*
 * 本类总结：
 * 1. 核心职责：承载设备 RPC 的成功响应或错误结果。
 * 2. 核心流程：传输层或 RPC 服务创建响应对象，Rule Engine 回调消费者据此选择成功或失败路由。
 * 3. 关键依赖：RuleEngineRpcService、RpcError、DeviceId 和 RPC 回调消费者。
 * 4. 学习重点：Rule Engine 的 RPC 返回值通过 Optional 区分成功响应和错误，避免隐式 null 语义。
 */
