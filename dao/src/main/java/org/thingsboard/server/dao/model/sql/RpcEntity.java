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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.RPC_ADDITIONAL_INFO;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_DEVICE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_EXPIRATION_TIME;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_REQUEST;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_RESPONSE;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_STATUS;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_TENANT_ID_COLUMN;

/**
 * 中文说明：
 * 1. `RpcEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = RPC_TABLE_NAME)
public class RpcEntity extends BaseSqlEntity<Rpc> implements BaseEntity<Rpc> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = RPC_TENANT_ID_COLUMN)
    private UUID tenantId;

    /**
     * 设备ID，用于定位对应业务对象。
     */
    @Column(name = RPC_DEVICE_ID)
    private UUID deviceId;

    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    @Column(name = RPC_EXPIRATION_TIME)
    private long expirationTime;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Type(type = "json")
    @Column(name = RPC_REQUEST)
    private JsonNode request;

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Type(type = "json")
    @Column(name = RPC_RESPONSE)
    private JsonNode response;

    /**
     * 状态，表示当前对象所处状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = RPC_STATUS)
    private RpcStatus status;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = RPC_ADDITIONAL_INFO)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `RpcEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RpcEntity() {
        super();
    }

    /**
     * 功能：创建 `RpcEntity` 实例，并初始化必要字段。
     * 参数：
     * - `rpc`：`rpc` 参数。
     * 返回：新创建的对象实例。
     */
    public RpcEntity(Rpc rpc) {
        this.setUuid(rpc.getUuidId());
        this.createdTime = rpc.getCreatedTime();
        this.tenantId = rpc.getTenantId().getId();
        this.deviceId = rpc.getDeviceId().getId();
        this.expirationTime = rpc.getExpirationTime();
        this.request = rpc.getRequest();
        this.response = rpc.getResponse();
        this.status = rpc.getStatus();
        this.additionalInfo = rpc.getAdditionalInfo();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Rpc toData() {
        Rpc rpc = new Rpc(new RpcId(id));
        rpc.setCreatedTime(createdTime);
        rpc.setTenantId(TenantId.fromUUID(tenantId));
        rpc.setDeviceId(new DeviceId(deviceId));
        rpc.setExpirationTime(expirationTime);
        rpc.setRequest(request);
        rpc.setResponse(response);
        rpc.setStatus(status);
        rpc.setAdditionalInfo(additionalInfo);
        return rpc;
    }
}
