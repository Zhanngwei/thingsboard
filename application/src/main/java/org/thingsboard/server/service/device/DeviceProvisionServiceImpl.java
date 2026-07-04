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
package org.thingsboard.server.service.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`DeviceProvisionServiceImpl` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DeviceProvisionServiceImpl implements DeviceProvisionService {

    /**
     * 字段说明：
     * 1. 保存 `ruleEngineMsgProducer` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;

    /**
     * 字段说明：
     * 1. 保存 `DEVICE_PROVISION_STATE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String DEVICE_PROVISION_STATE = "provisionState";
    private static final String PROVISIONED_STATE = "provisioned";

    /**
     * 字段说明：
     * 1. 保存 `clusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbClusterService clusterService;
    private final DeviceProfileService deviceProfileService;
    /**
     * 字段说明：
     * 1. 保存 `deviceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    /**
     * 字段说明：
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final AttributesService attributesService;
    private final AuditLogService auditLogService;
    /**
     * 字段说明：
     * 1. 保存 `partitionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final PartitionService partitionService;

    /**
     * 方法说明：
     * 1. 职责：执行 `DeviceProvisionServiceImpl` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProvisionServiceImpl(TbQueueProducerProvider producerProvider, TbClusterService clusterService, DeviceProfileService deviceProfileService, DeviceService deviceService, DeviceCredentialsService deviceCredentialsService, AttributesService attributesService, AuditLogService auditLogService, PartitionService partitionService) {
        ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();
        this.clusterService = clusterService;
        this.deviceProfileService = deviceProfileService;
        this.deviceService = deviceService;
        this.deviceCredentialsService = deviceCredentialsService;
        this.attributesService = attributesService;
        this.auditLogService = auditLogService;
        this.partitionService = partitionService;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `provisionDeviceViaX509Chain` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ProvisionResponse provisionDeviceViaX509Chain(DeviceProfile targetProfile, ProvisionRequest provisionRequest) throws ProvisionFailedException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (targetProfile == null) {
            throw new ProvisionFailedException("Device profile is not specified!");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(targetProfile.getProfileData().getProvisionConfiguration().getType())) {
            throw new ProvisionFailedException("Device profile provision strategy is not X509_CERTIFICATE_CHAIN!");
        }
        X509CertificateChainProvisionConfiguration configuration = (X509CertificateChainProvisionConfiguration) targetProfile.getProfileData().getProvisionConfiguration();
        String certificateValue = provisionRequest.getCredentialsData().getX509CertHash();
        String certificateRegEx = configuration.getCertificateRegExPattern();
        String commonName = getCNFromX509Certificate(targetProfile, certificateValue);
        String deviceName = extractDeviceNameFromCNByRegEx(targetProfile, commonName, certificateRegEx);
        provisionRequest.setDeviceName(deviceName);
        Device targetDevice = deviceService.findDeviceByTenantIdAndName(targetProfile.getTenantId(), provisionRequest.getDeviceName());
        X509CertificateChainProvisionConfiguration x509Configuration = (X509CertificateChainProvisionConfiguration) targetProfile.getProfileData().getProvisionConfiguration();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (targetDevice != null && targetDevice.getDeviceProfileId().equals(targetProfile.getId())) {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(targetDevice.getTenantId(), targetDevice.getId());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (DeviceCredentialsType.X509_CERTIFICATE.equals(deviceCredentials.getCredentialsType())) {
                String updatedDeviceCertificateValue = provisionRequest.getCredentialsData().getX509CertHash();
                deviceCredentials = updateDeviceCredentials(targetDevice.getTenantId(), deviceCredentials,
                        updatedDeviceCertificateValue, DeviceCredentialsType.X509_CERTIFICATE);
            }
            return new ProvisionResponse(deviceCredentials, ProvisionResponseStatus.SUCCESS);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (x509Configuration.isAllowCreateNewDevicesByX509Certificate()) {
            return createDevice(provisionRequest, targetProfile);
        } else {
            log.warn("[{}][{}] Device with name {} doesn't exist and cannot be created due incorrect configuration for X509CertificateChainProvisionConfiguration",
                    targetProfile.getTenantId(), targetProfile.getId(), provisionRequest.getDeviceName());
            throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `provisionDevice` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) {
        String provisionRequestKey = provisionRequest.getCredentials().getProvisionDeviceKey();
        String provisionRequestSecret = provisionRequest.getCredentials().getProvisionDeviceSecret();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!StringUtils.isEmpty(provisionRequest.getDeviceName())) {
            provisionRequest.setDeviceName(provisionRequest.getDeviceName().trim());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (StringUtils.isEmpty(provisionRequest.getDeviceName())) {
                log.warn("Provision request contains empty device name!");
                throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
            }
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isEmpty(provisionRequestKey) || StringUtils.isEmpty(provisionRequestSecret)) {
            throw new ProvisionFailedException(ProvisionResponseStatus.NOT_FOUND.name());
        }

        DeviceProfile targetProfile = deviceProfileService.findDeviceProfileByProvisionDeviceKey(provisionRequestKey);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (targetProfile == null || targetProfile.getProfileData().getProvisionConfiguration() == null ||
                targetProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret() == null) {
            throw new ProvisionFailedException(ProvisionResponseStatus.NOT_FOUND.name());
        }

        Device targetDevice = deviceService.findDeviceByTenantIdAndName(targetProfile.getTenantId(), provisionRequest.getDeviceName());

        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (targetProfile.getProvisionType()) {
            case ALLOW_CREATE_NEW_DEVICES:
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (targetProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret().equals(provisionRequestSecret)) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (targetDevice != null) {
                        log.warn("[{}] The device is present and could not be provisioned once more!", targetDevice.getName());
                        notify(targetDevice, provisionRequest, TbMsgType.PROVISION_FAILURE, false);
                        throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
                    } else {
                        return createDevice(provisionRequest, targetProfile);
                    }
                }
                break;
            case CHECK_PRE_PROVISIONED_DEVICES:
                if (targetProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret().equals(provisionRequestSecret)) {
                    if (targetDevice != null && targetDevice.getDeviceProfileId().equals(targetProfile.getId())) {
                        return processProvision(targetDevice, provisionRequest);
                    } else {
                        log.warn("[{}] Failed to find pre provisioned device!", provisionRequest.getDeviceName());
                        throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
                    }
                }
                break;
            case X509_CERTIFICATE_CHAIN:
                throw new ProvisionFailedException("Invalid provision strategy type!");
        }
        throw new ProvisionFailedException(ProvisionResponseStatus.NOT_FOUND.name());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processProvision` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ProvisionResponse processProvision(Device device, ProvisionRequest provisionRequest) {
        try {
            Optional<AttributeKvEntry> provisionState = attributesService.find(device.getTenantId(), device.getId(),
                    DataConstants.SERVER_SCOPE, DEVICE_PROVISION_STATE).get();
            if (provisionState != null && provisionState.isPresent() && !provisionState.get().getValueAsString().equals(PROVISIONED_STATE)) {
                notify(device, provisionRequest, TbMsgType.PROVISION_FAILURE, false);
                throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
            } else {
                saveProvisionStateAttribute(device).get();
                notify(device, provisionRequest, TbMsgType.PROVISION_SUCCESS, true);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
        }
        return new ProvisionResponse(deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()), ProvisionResponseStatus.SUCCESS);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDevice` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ProvisionResponse createDevice(ProvisionRequest provisionRequest, DeviceProfile profile) {
        return processCreateDevice(provisionRequest, profile);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `notify` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void notify(Device device, ProvisionRequest provisionRequest, TbMsgType type, boolean success) {
        pushProvisionEventToRuleEngine(provisionRequest, device, type);
        logAction(device.getTenantId(), device.getCustomerId(), device, success, provisionRequest);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processCreateDevice` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ProvisionResponse processCreateDevice(ProvisionRequest provisionRequest, DeviceProfile profile) {
        try {
            if (StringUtils.isEmpty(provisionRequest.getDeviceName())) {
                String newDeviceName = StringUtils.randomAlphanumeric(20);
                log.info("Device name not found in provision request. Generated name is: {}", newDeviceName);
                provisionRequest.setDeviceName(newDeviceName);
            }
            Device savedDevice = deviceService.saveDevice(provisionRequest, profile);
            clusterService.onDeviceUpdated(savedDevice, null);
            saveProvisionStateAttribute(savedDevice).get();
            pushDeviceCreatedEventToRuleEngine(savedDevice);
            notify(savedDevice, provisionRequest, TbMsgType.PROVISION_SUCCESS, true);

            return new ProvisionResponse(getDeviceCredentials(savedDevice), ProvisionResponseStatus.SUCCESS);
        } catch (Exception e) {
            log.warn("[{}] Error during device creation from provision request: [{}]", provisionRequest.getDeviceName(), provisionRequest, e);
            Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
            if (device != null) {
                notify(device, provisionRequest, TbMsgType.PROVISION_FAILURE, false);
            }
            throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateDeviceCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials, String certificateValue,
                                                      DeviceCredentialsType credentialsType) {
        log.trace("Updating device credentials [{}] with certificate value [{}]", deviceCredentials, certificateValue);
        deviceCredentials.setCredentialsValue(certificateValue);
        deviceCredentials.setCredentialsType(credentialsType);
        return deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveProvisionStateAttribute` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<List<String>> saveProvisionStateAttribute(Device device) {
        return attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(DEVICE_PROVISION_STATE, PROVISIONED_STATE),
                        System.currentTimeMillis())));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private DeviceCredentials getDeviceCredentials(Device device) {
        return deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushProvisionEventToRuleEngine` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushProvisionEventToRuleEngine(ProvisionRequest request, Device device, TbMsgType type) {
        try {
            JsonNode entityNode = JacksonUtil.valueToTree(request);
            TbMsg msg = TbMsg.newMsg(type, device.getId(), device.getCustomerId(), createTbMsgMetaData(device), JacksonUtil.toString(entityNode));
            sendToRuleEngine(device.getTenantId(), msg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), type, e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushDeviceCreatedEventToRuleEngine` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(device);
            TbMsg msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, device.getId(), device.getCustomerId(), createTbMsgMetaData(device), JacksonUtil.OBJECT_MAPPER.writeValueAsString(entityNode));
            sendToRuleEngine(device.getTenantId(), msg, null);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendToRuleEngine` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, tbMsg.getOriginator());
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTbMsgMetaData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbMsgMetaData createTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("tenantId", device.getTenantId().toString());
        return metaData;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logAction` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logAction(TenantId tenantId, CustomerId customerId, Device device, boolean success, ProvisionRequest provisionRequest) {
        ActionType actionType = success ? ActionType.PROVISION_SUCCESS : ActionType.PROVISION_FAILURE;
        auditLogService.logEntityAction(tenantId, customerId, new UserId(UserId.NULL_UUID), device.getName(), device.getId(), device, actionType, null, provisionRequest);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCNFromX509Certificate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getCNFromX509Certificate(DeviceProfile profile, String x509Value) {
        try {
            return SslUtil.parseCommonName(SslUtil.readCertFile(x509Value));
        } catch (Exception e) {
            log.trace("[{}][{}] Failed to parse CN from X509 certificate {}", profile.getTenantId(), profile.getId(), x509Value);
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractDeviceNameFromCNByRegEx` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String extractDeviceNameFromCNByRegEx(DeviceProfile profile, String commonName, String regex) throws ProvisionFailedException {
        try {
            log.trace("Extract device name from CN [{}] by regex pattern [{}]", commonName, regex);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(commonName);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {}
        log.trace("[{}][{}] Failed to match device name using [{}] from CN: [{}]", profile.getTenantId(), profile.getId(), regex, commonName);
        throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProvisionServiceImpl` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
