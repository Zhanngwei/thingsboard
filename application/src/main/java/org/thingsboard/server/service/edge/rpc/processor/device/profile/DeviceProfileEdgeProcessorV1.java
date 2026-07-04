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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`DeviceProfileEdgeProcessorV1` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class DeviceProfileEdgeProcessorV1 extends DeviceProfileEdgeProcessor {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `dataDecodingEncodingService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DataDecodingEncodingService dataDecodingEncodingService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `constructDeviceProfileFromUpdateMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected DeviceProfile constructDeviceProfileFromUpdateMsg(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setCreatedTime(Uuids.unixTimestamp(deviceProfileId.getId()));
        deviceProfile.setName(deviceProfileUpdateMsg.getName());
        deviceProfile.setDefault(deviceProfileUpdateMsg.getDefault());
        deviceProfile.setDescription(deviceProfileUpdateMsg.hasDescription() ? deviceProfileUpdateMsg.getDescription() : null);
        deviceProfile.setType(DeviceProfileType.valueOf(deviceProfileUpdateMsg.getType()));
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        deviceProfile.setTransportType(deviceProfileUpdateMsg.hasTransportType()
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                ? DeviceTransportType.valueOf(deviceProfileUpdateMsg.getTransportType()) : DeviceTransportType.DEFAULT);
        deviceProfile.setImage(deviceProfileUpdateMsg.hasImage()
                ? new String(deviceProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
        deviceProfile.setProvisionType(deviceProfileUpdateMsg.hasProvisionType()
                ? DeviceProfileProvisionType.valueOf(deviceProfileUpdateMsg.getProvisionType()) : DeviceProfileProvisionType.DISABLED);
        deviceProfile.setProvisionDeviceKey(deviceProfileUpdateMsg.hasProvisionDeviceKey()
                ? deviceProfileUpdateMsg.getProvisionDeviceKey() : null);
        deviceProfile.setDefaultQueueName(deviceProfileUpdateMsg.getDefaultQueueName());

        Optional<DeviceProfileData> profileDataOpt =
                dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());
        deviceProfile.setProfileData(profileDataOpt.orElse(null));

        String defaultQueueName = StringUtils.isNotBlank(deviceProfileUpdateMsg.getDefaultQueueName())
                ? deviceProfileUpdateMsg.getDefaultQueueName() : null;
        deviceProfile.setDefaultQueueName(defaultQueueName);

        UUID firmwareUUID = safeGetUUID(deviceProfileUpdateMsg.getFirmwareIdMSB(), deviceProfileUpdateMsg.getFirmwareIdLSB());
        deviceProfile.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

        UUID softwareUUID = safeGetUUID(deviceProfileUpdateMsg.getSoftwareIdMSB(), deviceProfileUpdateMsg.getSoftwareIdLSB());
        deviceProfile.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);
        return deviceProfile;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setDefaultRuleChainId` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId) {
        deviceProfile.setDefaultRuleChainId(ruleChainId);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setDefaultEdgeRuleChainId` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultEdgeRuleChainUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB(), deviceProfileUpdateMsg.getDefaultRuleChainIdLSB());
        deviceProfile.setDefaultEdgeRuleChainId(defaultEdgeRuleChainUUID != null ? new RuleChainId(defaultEdgeRuleChainUUID) : ruleChainId);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setDefaultDashboardId` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultDashboardUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultDashboardIdMSB(), deviceProfileUpdateMsg.getDefaultDashboardIdLSB());
        deviceProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : dashboardId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProfileEdgeProcessorV1` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
