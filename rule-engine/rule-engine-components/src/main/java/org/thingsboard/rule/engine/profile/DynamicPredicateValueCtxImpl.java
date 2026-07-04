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

@Slf4j
/**
 * 中文说明：`DynamicPredicateValueCtxImpl` 是动态谓词值上下文实现辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class DynamicPredicateValueCtxImpl implements DynamicPredicateValueCtx {
    /**
     * 字段说明：保存 `tenantId`，表示租户上下文，供本类方法在规则节点处理流程中使用。
     */
    private final TenantId tenantId;
    /**
     * 字段说明：保存 `customerId`，表示客户名称、客户标识或客户缓存，供本类方法在规则节点处理流程中使用。
     */
    private CustomerId customerId;
    /**
     * 字段说明：保存 `deviceId`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final DeviceId deviceId;
    /**
     * 字段说明：保存 Rule Engine 上下文引用；本字段本身不直接代表数据库、MQTT 或事务资源。
     */
    private final TbContext ctx;

    /**
     * 方法说明：构造 `DynamicPredicateValueCtxImpl` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    public DynamicPredicateValueCtxImpl(TenantId tenantId, DeviceId deviceId, TbContext ctx) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.ctx = ctx;
        resetCustomer();
    }

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `DynamicPredicateValueCtxImpl` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public EntityKeyValue getTenantValue(String key) {
        return getValue(tenantId, key);
    }

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `DynamicPredicateValueCtxImpl` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public EntityKeyValue getCustomerValue(String key) {
        return customerId == null || customerId.isNullUid() ? null : getValue(customerId, key);
    }

    @Override
    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `DynamicPredicateValueCtxImpl` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `DeviceService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void resetCustomer() {
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        Device device = ctx.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.customerId = device.getCustomerId();
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `DynamicPredicateValueCtxImpl` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AttributesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private EntityKeyValue getValue(EntityId entityId, String key) {
        try {
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
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
