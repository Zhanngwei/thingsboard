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
package org.thingsboard.server.common.data.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. 类目的：`Rpc` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
public class Rpc extends BaseData<RpcId> implements HasTenantId {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Device Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private DeviceId deviceId;
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    @ApiModelProperty(position = 5, value = "Expiration time of the request.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private long expirationTime;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @ApiModelProperty(position = 6, value = "The request body that will be used to send message to device.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private JsonNode request;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @ApiModelProperty(position = 7, value = "The response from the device.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private JsonNode response;
    /**
     * 状态，表示当前对象所处状态。
     */
    @ApiModelProperty(position = 8, value = "The current status of the RPC call.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RpcStatus status;
    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 9, value = "Additional info used in the rule engine to process the updates to the RPC state.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `Rpc` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Rpc() {
        super();
    }

    /**
     * 功能：创建 `Rpc` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Rpc(RpcId id) {
        super(id);
    }

    /**
     * 功能：创建 `Rpc` 实例，并初始化必要字段。
     * 参数：
     * - `rpc`：`rpc` 参数。
     * 返回：新创建的对象实例。
     */
    public Rpc(Rpc rpc) {
        super(rpc);
        this.tenantId = rpc.getTenantId();
        this.deviceId = rpc.getDeviceId();
        this.expirationTime = rpc.getExpirationTime();
        this.request = rpc.getRequest();
        this.response = rpc.getResponse();
        this.status = rpc.getStatus();
        this.additionalInfo = rpc.getAdditionalInfo();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the rpc Id. Referencing non-existing rpc Id will cause error.")
    @Override
    public RpcId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the rpc creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`Rpc` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
