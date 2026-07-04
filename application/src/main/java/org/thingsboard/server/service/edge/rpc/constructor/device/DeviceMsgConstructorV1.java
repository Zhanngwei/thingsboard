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
package org.thingsboard.server.service.edge.rpc.constructor.device;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;

@Component
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`DeviceMsgConstructorV1` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class DeviceMsgConstructorV1 extends BaseDeviceMsgConstructor {

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

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `imageService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ImageService imageService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `constructDeviceUpdatedMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits())
                .setName(device.getName())
                .setType(device.getType());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getLabel() != null) {
            builder.setLabel(device.getLabel());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getCustomerId() != null) {
            builder.setCustomerIdMSB(device.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(device.getCustomerId().getId().getLeastSignificantBits());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits());
            builder.setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(device.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(device.getFirmwareId().getId().getLeastSignificantBits());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getSoftwareId() != null) {
            builder.setSoftwareIdMSB(device.getSoftwareId().getId().getMostSignificantBits())
                    .setSoftwareIdLSB(device.getSoftwareId().getId().getLeastSignificantBits());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getDeviceData() != null) {
            builder.setDeviceDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(device.getDeviceData())));
        }
        return builder.build();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `constructDeviceCredentialsUpdatedMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        DeviceCredentialsUpdateMsg.Builder builder = DeviceCredentialsUpdateMsg.newBuilder()
                .setDeviceIdMSB(deviceCredentials.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceCredentials.getDeviceId().getId().getLeastSignificantBits());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceCredentials.getCredentialsType() != null) {
            builder.setCredentialsType(deviceCredentials.getCredentialsType().name())
                    .setCredentialsId(deviceCredentials.getCredentialsId());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceCredentials.getCredentialsValue() != null) {
            builder.setCredentialsValue(deviceCredentials.getCredentialsValue());
        }
        return builder.build();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `constructDeviceProfileUpdatedMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile) {
        deviceProfile = JacksonUtil.clone(deviceProfile);
        imageService.inlineImageForEdge(deviceProfile);
        DeviceProfileUpdateMsg.Builder builder = DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits())
                .setName(deviceProfile.getName())
                .setDefault(deviceProfile.isDefault())
                .setType(deviceProfile.getType().name())
                .setProfileDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile.getProfileData())));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceProfile.getDescription() != null) {
            builder.setDescription(deviceProfile.getDescription());
        }
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        if (deviceProfile.getTransportType() != null) {
            builder.setTransportType(deviceProfile.getTransportType().name());
        }
        if (deviceProfile.getProvisionType() != null) {
            builder.setProvisionType(deviceProfile.getProvisionType().name());
        }
        if (deviceProfile.getProvisionDeviceKey() != null) {
            builder.setProvisionDeviceKey(deviceProfile.getProvisionDeviceKey());
        }
        if (deviceProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(deviceProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (deviceProfile.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(deviceProfile.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(deviceProfile.getFirmwareId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getSoftwareId() != null) {
            builder.setSoftwareIdMSB(deviceProfile.getSoftwareId().getId().getMostSignificantBits())
                    .setSoftwareIdLSB(deviceProfile.getSoftwareId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultEdgeRuleChainId() != null) {
            builder.setDefaultRuleChainIdMSB(deviceProfile.getDefaultEdgeRuleChainId().getId().getMostSignificantBits())
                    .setDefaultRuleChainIdLSB(deviceProfile.getDefaultEdgeRuleChainId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultDashboardId() != null) {
            builder.setDefaultDashboardIdMSB(deviceProfile.getDefaultDashboardId().getId().getMostSignificantBits())
                    .setDefaultDashboardIdLSB(deviceProfile.getDefaultDashboardId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceMsgConstructorV1` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
