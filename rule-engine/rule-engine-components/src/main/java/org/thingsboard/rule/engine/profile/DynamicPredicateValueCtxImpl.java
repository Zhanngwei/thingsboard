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
package org.thingsboard.rule.engine.profile;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * 中文说明：`DynamicPredicateValueCtxImpl` 是动态谓词值上下文实现辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
@Slf4j
public class DynamicPredicateValueCtxImpl implements DynamicPredicateValueCtx {
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    private CustomerId customerId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbContext ctx;

    /**
     * 功能：创建 `DynamicPredicateValueCtxImpl` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `ctx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public DynamicPredicateValueCtxImpl(TenantId tenantId, DeviceId deviceId, TbContext ctx) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.ctx = ctx;
        resetCustomer();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public EntityKeyValue getTenantValue(String key) {
        return getValue(tenantId, key);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public EntityKeyValue getCustomerValue(String key) {
        return customerId == null || customerId.isNullUid() ? null : getValue(customerId, key);
    }

    /**
     * 功能：执行 `resetCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void resetCustomer() {
        Device device = ctx.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.customerId = device.getCustomerId();
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    private EntityKeyValue getValue(EntityId entityId, String key) {
        try {
            Optional<AttributeKvEntry> entry = ctx.getAttributesService().find(tenantId, entityId, DataConstants.SERVER_SCOPE, key).get();
            if (entry.isPresent()) {
                return DeviceState.toEntityValue(entry.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to get attribute by key: {} for {}: [{}]", key, entityId.getEntityType(), entityId.getId());
        }
        return null;
    }
    /*
     * 本类总结：`DynamicPredicateValueCtxImpl` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
