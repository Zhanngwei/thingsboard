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

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.importing.csv.AbstractBulkImportService;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@TbCoreComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DeviceBulkImportService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DeviceBulkImportService extends AbstractBulkImportService<Device> {
    /**
     * 字段说明：
     * 1. 保存 `deviceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final DeviceService deviceService;
    protected final TbDeviceService tbDeviceService;
    /**
     * 字段说明：
     * 1. 保存 `deviceCredentialsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final DeviceCredentialsService deviceCredentialsService;
    protected final DeviceProfileService deviceProfileService;

    private final Lock findOrCreateDeviceProfileLock = new ReentrantLock();

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setEntityFields` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setEntityFields(Device device, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(device);
        fields.forEach((columnType, value) -> {
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (columnType) {
                case NAME:
                    device.setName(value);
                    break;
                case TYPE:
                    device.setType(value);
                    break;
                case LABEL:
                    device.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case IS_GATEWAY:
                    additionalInfo.set("gateway", BooleanNode.valueOf(Boolean.parseBoolean(value)));
                    break;
            }
            device.setAdditionalInfo(additionalInfo);
        });
        setUpDeviceConfiguration(device, fields);
    }

    @Override
    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `saveEntity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Device saveEntity(SecurityUser user, Device device, Map<BulkImportColumnType, String> fields) {
        DeviceCredentials deviceCredentials;
        try {
            deviceCredentials = createDeviceCredentials(device.getTenantId(), device.getId(), fields);
            deviceCredentialsService.formatCredentials(deviceCredentials);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            throw new DeviceCredentialsValidationException("Invalid device credentials: " + e.getMessage());
        }

        DeviceProfile deviceProfile;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.LWM2M_CREDENTIALS) {
            deviceProfile = setUpLwM2mDeviceProfile(device.getTenantId(), device);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (StringUtils.isNotEmpty(device.getType())) {
            deviceProfile = deviceProfileService.findOrCreateDeviceProfile(device.getTenantId(), device.getType());
        } else {
            deviceProfile = deviceProfileService.findDefaultDeviceProfile(device.getTenantId());
        }
        device.setDeviceProfileId(deviceProfile.getId());

        return tbDeviceService.saveDeviceWithCredentials(device, deviceCredentials, user);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findOrCreateEntity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Device findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(deviceService.findDeviceByTenantIdAndName(tenantId, name))
                .orElseGet(Device::new);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setOwners` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setOwners(Device entity, SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpDeviceConfiguration` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setUpDeviceConfiguration(Device device, Map<BulkImportColumnType, String> fields) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (fields.containsKey(BulkImportColumnType.SNMP_HOST)) {
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            SnmpDeviceTransportConfiguration transportConfiguration = new SnmpDeviceTransportConfiguration();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            transportConfiguration.setHost(fields.get(BulkImportColumnType.SNMP_HOST));
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            transportConfiguration.setPort(Optional.ofNullable(fields.get(BulkImportColumnType.SNMP_PORT))
                    .map(Integer::parseInt).orElse(161));
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            transportConfiguration.setProtocolVersion(Optional.ofNullable(fields.get(BulkImportColumnType.SNMP_VERSION))
                    .map(version -> SnmpProtocolVersion.valueOf(version.toUpperCase())).orElse(SnmpProtocolVersion.V2C));
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            transportConfiguration.setCommunity(fields.getOrDefault(BulkImportColumnType.SNMP_COMMUNITY_STRING, "public"));

            DeviceData deviceData = new DeviceData();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            deviceData.setTransportConfiguration(transportConfiguration);
            device.setDeviceData(deviceData);
        }
    }

    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `createDeviceCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceId deviceId, Map<BulkImportColumnType, String> fields) {
        DeviceCredentials credentials = new DeviceCredentials();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (fields.containsKey(BulkImportColumnType.LWM2M_CLIENT_ENDPOINT)) {
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            setUpLwm2mCredentials(fields, credentials);
        } else if (fields.containsKey(BulkImportColumnType.X509)) {
            credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
            setUpX509CertificateCredentials(fields, credentials);
        } else if (CollectionUtils.containsAny(fields.keySet(), EnumSet.of(BulkImportColumnType.MQTT_CLIENT_ID, BulkImportColumnType.MQTT_USER_NAME, BulkImportColumnType.MQTT_PASSWORD))) {
            credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
            setUpBasicMqttCredentials(fields, credentials);
        } else if (deviceId != null && !fields.containsKey(BulkImportColumnType.ACCESS_TOKEN)) {
            credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        } else  {
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            setUpAccessTokenCredentials(fields, credentials);
        }
        return credentials;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpAccessTokenCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setUpAccessTokenCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) {
        credentials.setCredentialsId(Optional.ofNullable(fields.get(BulkImportColumnType.ACCESS_TOKEN))
                .orElseGet(() -> StringUtils.randomAlphanumeric(20)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpBasicMqttCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setUpBasicMqttCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) {
        BasicMqttCredentials basicMqttCredentials = new BasicMqttCredentials();
        basicMqttCredentials.setClientId(fields.get(BulkImportColumnType.MQTT_CLIENT_ID));
        basicMqttCredentials.setUserName(fields.get(BulkImportColumnType.MQTT_USER_NAME));
        basicMqttCredentials.setPassword(fields.get(BulkImportColumnType.MQTT_PASSWORD));
        credentials.setCredentialsValue(JacksonUtil.toString(basicMqttCredentials));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpX509CertificateCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setUpX509CertificateCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) {
        credentials.setCredentialsValue(fields.get(BulkImportColumnType.X509));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpLwm2mCredentials` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setUpLwm2mCredentials(Map<BulkImportColumnType, String> fields, DeviceCredentials credentials) throws com.fasterxml.jackson.core.JsonProcessingException {
        ObjectNode lwm2mCredentials = JacksonUtil.newObjectNode();

        Set.of(BulkImportColumnType.LWM2M_CLIENT_SECURITY_CONFIG_MODE, BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE,
                        BulkImportColumnType.LWM2M_SERVER_SECURITY_MODE).stream()
                .map(fields::get)
                .filter(Objects::nonNull)
                .forEach(securityMode -> {
                    try {
                        LwM2MSecurityMode.valueOf(securityMode.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new DeviceCredentialsValidationException("Unknown LwM2M security mode: " + securityMode + ", (the mode should be: NO_SEC, PSK, RPK, X509)!");
                    }
                });

        ObjectNode client = JacksonUtil.newObjectNode();
        setValues(client, fields, Set.of(BulkImportColumnType.LWM2M_CLIENT_SECURITY_CONFIG_MODE,
                BulkImportColumnType.LWM2M_CLIENT_ENDPOINT, BulkImportColumnType.LWM2M_CLIENT_IDENTITY,
                BulkImportColumnType.LWM2M_CLIENT_KEY, BulkImportColumnType.LWM2M_CLIENT_CERT));
        LwM2MClientCredential lwM2MClientCredential = JacksonUtil.treeToValue(client, LwM2MClientCredential.class);
        // so that only fields needed for specific type of lwM2MClientCredentials were saved in json
        lwm2mCredentials.set("client", JacksonUtil.valueToTree(lwM2MClientCredential));

        ObjectNode bootstrapServer = JacksonUtil.newObjectNode();
        setValues(bootstrapServer, fields, Set.of(BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE,
                BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_PUBLIC_KEY_OR_ID, BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECRET_KEY));

        ObjectNode lwm2mServer = JacksonUtil.newObjectNode();
        setValues(lwm2mServer, fields, Set.of(BulkImportColumnType.LWM2M_SERVER_SECURITY_MODE,
                BulkImportColumnType.LWM2M_SERVER_CLIENT_PUBLIC_KEY_OR_ID, BulkImportColumnType.LWM2M_SERVER_CLIENT_SECRET_KEY));

        ObjectNode bootstrap = JacksonUtil.newObjectNode();
        bootstrap.set("bootstrapServer", bootstrapServer);
        bootstrap.set("lwm2mServer", lwm2mServer);
        lwm2mCredentials.set("bootstrap", bootstrap);

        credentials.setCredentialsValue(lwm2mCredentials.toString());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpLwM2mDeviceProfile` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private DeviceProfile setUpLwM2mDeviceProfile(TenantId tenantId, Device device) {
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileByName(tenantId, device.getType());
        if (deviceProfile != null) {
            if (deviceProfile.getTransportType() != DeviceTransportType.LWM2M) {
                deviceProfile.setTransportType(DeviceTransportType.LWM2M);
                deviceProfile.getProfileData().setTransportConfiguration(new Lwm2mDeviceProfileTransportConfiguration());
                deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
            }
        } else {
            findOrCreateDeviceProfileLock.lock();
            try {
                deviceProfile = deviceProfileService.findDeviceProfileByName(tenantId, device.getType());
                if (deviceProfile == null) {
                    deviceProfile = new DeviceProfile();
                    deviceProfile.setTenantId(tenantId);
                    deviceProfile.setType(DeviceProfileType.DEFAULT);
                    deviceProfile.setName(device.getType());
                    deviceProfile.setTransportType(DeviceTransportType.LWM2M);
                    deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);

                    Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
                    transportConfiguration.setBootstrap(Collections.emptyList());
                    transportConfiguration.setClientLwM2mSettings(new OtherConfiguration(1, 1, 1, PowerMode.DRX, null, null, null, null, null));
                    transportConfiguration.setObserveAttr(new TelemetryMappingConfiguration(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap()));

                    DeviceProfileData deviceProfileData = new DeviceProfileData();
                    DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
                    DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
                    deviceProfileData.setConfiguration(configuration);
                    deviceProfileData.setTransportConfiguration(transportConfiguration);
                    deviceProfileData.setProvisionConfiguration(provisionConfiguration);
                    deviceProfile.setProfileData(deviceProfileData);

                    deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
                }
            } finally {
                findOrCreateDeviceProfileLock.unlock();
            }
        }
        return deviceProfile;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setValues` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setValues(ObjectNode objectNode, Map<BulkImportColumnType, String> data, Collection<BulkImportColumnType> columns) {
        for (BulkImportColumnType column : columns) {
            String value = StringUtils.defaultString(data.get(column), column.getDefaultValue());
            if (value != null && column.getKey() != null) {
                objectNode.set(column.getKey(), new TextNode(value));
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getEntityType` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceBulkImportService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
