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
package org.thingsboard.server.service.edge.rpc.processor.device.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

/**
 * 中文说明：
 * 1. 类目的：`BaseDeviceProfileProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class BaseDeviceProfileProcessor extends BaseEdgeProcessor {

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected Pair<Boolean, Boolean> saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        boolean created = false;
        boolean deviceProfileNameUpdated = false;
        deviceCreationLock.lock();
        try {
            DeviceProfile deviceProfile = constructDeviceProfileFromUpdateMsg(tenantId, deviceProfileId, deviceProfileUpdateMsg);
            if (deviceProfile == null) {
                throw new RuntimeException("[{" + tenantId + "}] deviceProfileUpdateMsg {" + deviceProfileUpdateMsg + "} cannot be converted to device profile");
            }
            DeviceProfile deviceProfileById = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
            if (deviceProfileById == null) {
                created = true;
                deviceProfile.setId(null);
                deviceProfile.setDefault(false);
            } else {
                deviceProfile.setId(deviceProfileId);
                deviceProfile.setDefault(deviceProfileById.isDefault());
            }
            String deviceProfileName = deviceProfile.getName();
            DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfileName);
            if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                deviceProfileName = deviceProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Device profile with name {} already exists. Renaming device profile name to {}",
                        tenantId, deviceProfile.getName(), deviceProfileName);
                deviceProfileNameUpdated = true;
            }
            deviceProfile.setName(deviceProfileName);

            RuleChainId ruleChainId = deviceProfile.getDefaultRuleChainId();
            setDefaultRuleChainId(tenantId, deviceProfile, created ? null : deviceProfileById.getDefaultRuleChainId());
            setDefaultEdgeRuleChainId(deviceProfile, ruleChainId, deviceProfileUpdateMsg);
            setDefaultDashboardId(tenantId, created ? null : deviceProfileById.getDefaultDashboardId(), deviceProfile, deviceProfileUpdateMsg);

            deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
            if (created) {
                deviceProfile.setId(deviceProfileId);
            }
            deviceProfileService.saveDeviceProfile(deviceProfile, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process device profile update msg [{}]", tenantId, deviceProfileUpdateMsg, e);
            throw e;
        } finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceProfileNameUpdated);
    }

    /**
     * 功能：执行 `constructDeviceProfileFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected abstract DeviceProfile constructDeviceProfileFromUpdateMsg(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    protected abstract void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `ruleChainId`：规则链ID。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    protected abstract void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    /**
     * 功能：更新仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    protected abstract void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseDeviceProfileProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
