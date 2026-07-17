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
 * 1. `Rpc` 是 ThingsBoard Common Data 中承载 RPC 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasTenantId`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
