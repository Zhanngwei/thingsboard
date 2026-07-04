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
package org.thingsboard.server.dao.service.validator;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.RPKLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.X509LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
/**
 * 中文说明：
 * 1. 类目的：`DeviceProfileDataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public class DeviceProfileDataValidator extends AbstractHasOtaPackageValidator<DeviceProfile> {

    /**
     * 字段说明：
     * 1. 保存 `ATTRIBUTES_PROTO_SCHEMA` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    private static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    /**
     * 字段说明：
     * 1. 保存 `RPC_REQUEST_PROTO_SCHEMA` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    private static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    /**
     * 字段说明：
     * 1. 保存 `EXCEPTION_PREFIX` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String EXCEPTION_PREFIX = "[Transport Configuration]";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceProfileDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DeviceProfileDao deviceProfileDao;
    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `deviceProfileService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DeviceProfileService deviceProfileService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DeviceDao deviceDao;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private TenantService tenantService;
    @Lazy
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `queueService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private QueueService queueService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `ruleChainService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private RuleChainService ruleChainService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `dashboardService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DashboardService dashboardService;

    @Value("${security.java_cacerts.path:}")
    /**
     * 字段说明：
     * 1. 保存 `javaCacertsPath` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private String javaCacertsPath;

    @Value("${security.java_cacerts.password:}")
    /**
     * 字段说明：
     * 1. 保存 `javaCacertsPassword` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private String javaCacertsPassword;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `validateDataImpl` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    protected void validateDataImpl(TenantId tenantId, DeviceProfile deviceProfile) {
        validateString("Device profile name", deviceProfile.getName());
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceProfile.getType() == null) {
            throw new DataValidationException("Device profile type should be specified!");
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceProfile.getTransportType() == null) {
            throw new DataValidationException("Device profile transport type should be specified!");
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceProfile.getTenantId() == null) {
            throw new DataValidationException("Device profile should be assigned to tenant!");
        } else {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (!tenantService.tenantExists(deviceProfile.getTenantId())) {
                throw new DataValidationException("Device profile is referencing to non-existent tenant!");
            }
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceProfile.isDefault()) {
            DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (defaultDeviceProfile != null && !defaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                throw new DataValidationException("Another default device profile is present in scope of current tenant!");
            }
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (StringUtils.isNotEmpty(deviceProfile.getDefaultQueueName())) {
            Queue queue = queueService.findQueueByTenantIdAndName(tenantId, deviceProfile.getDefaultQueueName());
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (queue == null) {
                throw new DataValidationException("Device profile is referencing to non-existent queue!");
            }
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceProfile.getProvisionType() == null) {
            deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceProfile.getProvisionDeviceKey() != null && DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(deviceProfile.getProvisionType())) {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (isDeviceProfileCertificateInJavaCacerts(deviceProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret())) {
                throw new DataValidationException("Device profile certificate cannot be well known root CA!");
            }
        }
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        transportConfiguration.validate();
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (mqttTransportConfiguration.getTransportPayloadTypeConfiguration() instanceof ProtoTransportPayloadConfiguration) {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                        (ProtoTransportPayloadConfiguration) mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
                validateProtoSchemas(protoTransportPayloadConfiguration);
                validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
            }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        } else if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
            if (coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
                if (transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration) {
                    ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                    validateProtoSchemas(protoTransportPayloadConfiguration);
                    validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                    validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
                }
            }
        } else if (transportConfiguration instanceof Lwm2mDeviceProfileTransportConfiguration) {
            List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations = ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).getBootstrap();
            if (lwM2MBootstrapServersConfigurations != null) {
                validateLwm2mServersConfigOfBootstrapForClient(lwM2MBootstrapServersConfigurations,
                        ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).isBootstrapServerUpdateEnable());
                for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
                    validateLwm2mServersCredentialOfBootstrapForClient(bootstrapServerCredential);
                }
            }
        }

        List<DeviceProfileAlarm> profileAlarms = deviceProfile.getProfileData().getAlarms();

        if (!CollectionUtils.isEmpty(profileAlarms)) {
            Set<String> alarmTypes = new HashSet<>();
            for (DeviceProfileAlarm alarm : profileAlarms) {
                String alarmType = alarm.getAlarmType();
                if (StringUtils.isEmpty(alarmType)) {
                    throw new DataValidationException("Alarm rule type should be specified!");
                }
                if (!alarmTypes.add(alarmType)) {
                    throw new DataValidationException(String.format("Can't create device profile with the same alarm rule types: \"%s\"!", alarmType));
                }
            }
        }

        if (deviceProfile.getDefaultRuleChainId() != null) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, deviceProfile.getDefaultRuleChainId());
            if (ruleChain == null) {
                throw new DataValidationException("Can't assign non-existent rule chain!");
            }
            if (!ruleChain.getTenantId().equals(deviceProfile.getTenantId())) {
                throw new DataValidationException("Can't assign rule chain from different tenant!");
            }
        }

        if (deviceProfile.getDefaultDashboardId() != null) {
            DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, deviceProfile.getDefaultDashboardId());
            if (dashboard == null) {
                throw new DataValidationException("Can't assign non-existent dashboard!");
            }
            if (!dashboard.getTenantId().equals(deviceProfile.getTenantId())) {
                throw new DataValidationException("Can't assign dashboard from different tenant!");
            }
        }

        validateOtaPackage(tenantId, deviceProfile, deviceProfile.getId());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `validateUpdate` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    protected DeviceProfile validateUpdate(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfile old = deviceProfileDao.findById(deviceProfile.getTenantId(), deviceProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing device profile!");
        }
        boolean profileTypeChanged = !old.getType().equals(deviceProfile.getType());
        boolean transportTypeChanged = !old.getTransportType().equals(deviceProfile.getTransportType());
        if (profileTypeChanged || transportTypeChanged) {
            Long profileDeviceCount = deviceDao.countDevicesByDeviceProfileId(deviceProfile.getTenantId(), deviceProfile.getId().getId());
            if (profileDeviceCount > 0) {
                String message = null;
                if (profileTypeChanged) {
                    message = "Can't change device profile type because devices referenced it!";
                } else if (transportTypeChanged) {
                    message = "Can't change device profile transport type because devices referenced it!";
                }
                throw new DataValidationException(message);
            }
        }
        if (deviceProfile.getProvisionDeviceKey() != null && DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(deviceProfile.getProvisionType())) {
            if (isDeviceProfileCertificateInJavaCacerts(deviceProfile.getProvisionDeviceKey())) {
                throw new DataValidationException("Device profile certificate cannot be well known root CA!");
            }
        }
        return old;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateProtoSchemas` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateProtoSchemas(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        try {
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceAttributesProtoSchema(), ATTRIBUTES_PROTO_SCHEMA, EXCEPTION_PREFIX);
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema(), TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX);
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema(), RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX);
            DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcResponseProtoSchema(), RPC_RESPONSE_PROTO_SCHEMA, EXCEPTION_PREFIX);
        } catch (Exception exception) {
            throw new DataValidationException(exception.getMessage());
        }
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `validateTelemetryDynamicMessageFields` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateTelemetryDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        String deviceTelemetryProtoSchema = protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema();
        Descriptors.Descriptor telemetryDynamicMessageDescriptor = protoTransportPayloadTypeConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema);
        if (telemetryDynamicMessageDescriptor == null) {
            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get telemetryDynamicMessageDescriptor!");
        } else {
            List<Descriptors.FieldDescriptor> fields = telemetryDynamicMessageDescriptor.getFields();
            if (CollectionUtils.isEmpty(fields)) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " " + telemetryDynamicMessageDescriptor.getName() + " fields is empty!");
            } else if (fields.size() == 2) {
                Descriptors.FieldDescriptor tsFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("ts");
                Descriptors.FieldDescriptor valuesFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("values");
                if (tsFieldDescriptor != null && valuesFieldDescriptor != null) {
                    if (!Descriptors.FieldDescriptor.Type.MESSAGE.equals(valuesFieldDescriptor.getType())) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'values' has invalid data type. Only message type is supported!");
                    }
                    if (!Descriptors.FieldDescriptor.Type.INT64.equals(tsFieldDescriptor.getType())) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'ts' has invalid data type. Only int64 type is supported!");
                    }
                    if (!tsFieldDescriptor.hasOptionalKeyword()) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'ts' has invalid label. Field 'ts' should have optional keyword!");
                    }
                }
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateRpcRequestDynamicMessageFields` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateRpcRequestDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        DynamicMessage.Builder rpcRequestDynamicMessageBuilder = protoTransportPayloadTypeConfiguration.getRpcRequestDynamicMessageBuilder(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema());
        Descriptors.Descriptor rpcRequestDynamicMessageDescriptor = rpcRequestDynamicMessageBuilder.getDescriptorForType();
        if (rpcRequestDynamicMessageDescriptor == null) {
            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get rpcRequestDynamicMessageDescriptor!");
        } else {
            if (CollectionUtils.isEmpty(rpcRequestDynamicMessageDescriptor.getFields()) || rpcRequestDynamicMessageDescriptor.getFields().size() != 3) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " " + rpcRequestDynamicMessageDescriptor.getName() + " message should always contains 3 fields: method, requestId and params!");
            }
            Descriptors.FieldDescriptor methodFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("method");
            if (methodFieldDescriptor == null) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: method!");
            } else {
                if (!Descriptors.FieldDescriptor.Type.STRING.equals(methodFieldDescriptor.getType())) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'method' has invalid data type. Only string type is supported!");
                }
                if (methodFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'method' has invalid label!");
                }
            }
            Descriptors.FieldDescriptor requestIdFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("requestId");
            if (requestIdFieldDescriptor == null) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: requestId!");
            } else {
                if (!Descriptors.FieldDescriptor.Type.INT32.equals(requestIdFieldDescriptor.getType())) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'requestId' has invalid data type. Only int32 type is supported!");
                }
                if (requestIdFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'requestId' has invalid label!");
                }
            }
            Descriptors.FieldDescriptor paramsFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("params");
            if (paramsFieldDescriptor == null) {
                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: params!");
            } else {
                if (paramsFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'params' has invalid label!");
                }
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateLwm2mServersConfigOfBootstrapForClient` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateLwm2mServersConfigOfBootstrapForClient(List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations, boolean isBootstrapServerUpdateEnable) {
        Set<String> uris = new HashSet<>();
        Set<Integer> shortServerIds = new HashSet<>();
        for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
            AbstractLwM2MBootstrapServerCredential serverConfig = (AbstractLwM2MBootstrapServerCredential) bootstrapServerCredential;
            if (!isBootstrapServerUpdateEnable && serverConfig.isBootstrapServerIs()) {
                throw new DeviceCredentialsValidationException("Bootstrap config must not include \"Bootstrap Server\". \"Include Bootstrap Server updates\" is " + isBootstrapServerUpdateEnable + ".");
            }
            String server = serverConfig.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server" + " shortServerId: " + serverConfig.getShortServerId() + ":";
            if (serverConfig.getShortServerId() < 1 || serverConfig.getShortServerId() > 65534) {
                throw new DeviceCredentialsValidationException(server + " ShortServerId must not be less than 1 and more than 65534!");
            }
            if (!shortServerIds.add(serverConfig.getShortServerId())) {
                throw new DeviceCredentialsValidationException(server + " \"Short server Id\" value = " + serverConfig.getShortServerId() + ". This value must be a unique value for all servers!");
            }
            String uri = serverConfig.getHost() + ":" + serverConfig.getPort();
            if (!uris.add(uri)) {
                throw new DeviceCredentialsValidationException(server + " \"Host + port\" value = " + uri + ". This value must be a unique value for all servers!");
            }
            int port;
            if (LwM2MSecurityMode.NO_SEC.equals(serverConfig.getSecurityMode())) {
                port = serverConfig.isBootstrapServerIs() ? 5687 : 5685;
            } else {
                port = serverConfig.isBootstrapServerIs() ? 5688 : 5686;
            }
            if (serverConfig.getPort() == null || serverConfig.getPort() != port) {
                throw new DeviceCredentialsValidationException(server + " \"Port\" value = " + serverConfig.getPort() + ". This value for security " + serverConfig.getSecurityMode().name() + " must be " + port + "!");
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateLwm2mServersCredentialOfBootstrapForClient` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateLwm2mServersCredentialOfBootstrapForClient(LwM2MBootstrapServerCredential bootstrapServerConfig) {
        String server;
        switch (bootstrapServerConfig.getSecurityMode()) {
            case NO_SEC:
            case PSK:
                break;
            case RPK:
                RPKLwM2MBootstrapServerCredential rpkServerCredentials = (RPKLwM2MBootstrapServerCredential) bootstrapServerConfig;
                server = rpkServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                if (StringUtils.isEmpty(rpkServerCredentials.getServerPublicKey())) {
                    throw new DeviceCredentialsValidationException(server + " RPK public key must be specified!");
                }
                try {
                    String pubkRpkSever = EncryptionUtil.pubkTrimNewLines(rpkServerCredentials.getServerPublicKey());
                    rpkServerCredentials.setServerPublicKey(pubkRpkSever);
                    SecurityUtil.publicKey.decode(rpkServerCredentials.getDecodedCServerPublicKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " RPK public key must be in standard [RFC7250] and then encoded to Base64 format!");
                }
                break;
            case X509:
                X509LwM2MBootstrapServerCredential x509ServerCredentials = (X509LwM2MBootstrapServerCredential) bootstrapServerConfig;
                server = x509ServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                if (StringUtils.isEmpty(x509ServerCredentials.getServerPublicKey())) {
                    throw new DeviceCredentialsValidationException(server + " X509 certificate must be specified!");
                }

                try {
                    String certServer = EncryptionUtil.certTrimNewLines(x509ServerCredentials.getServerPublicKey());
                    x509ServerCredentials.setServerPublicKey(certServer);
                    SecurityUtil.certificate.decode(x509ServerCredentials.getDecodedCServerPublicKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!");
                }
                break;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isDeviceProfileCertificateInJavaCacerts` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private boolean isDeviceProfileCertificateInJavaCacerts(String deviceProfileX509Secret) {
        try {
            FileInputStream is = new FileInputStream(javaCacertsPath);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, javaCacertsPassword.toCharArray());

            PKIXParameters params = new PKIXParameters(keystore);
            for (TrustAnchor ta : params.getTrustAnchors()) {
                X509Certificate cert = ta.getTrustedCert();
                if (getCertificateString(cert).equals(deviceProfileX509Secret)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.trace("Failed to validate certificate due to: ", e);
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCertificateString` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private String getCertificateString(X509Certificate cert) throws CertificateEncodingException {
        return EncryptionUtil.certTrimNewLines(Base64Utils.encodeToString(cert.getEncoded()));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProfileDataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
