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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceConnectivityConfiguration;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.common.data.DataConstants.DEFAULT_DEVICE_TYPE;

@Service
@Profile("install")
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultSystemDataLoaderService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    /**
     * 字段说明：
     * 1. 保存 `CUSTOMER_CRED` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String CUSTOMER_CRED = "customer";
    public static final String ACTIVITY_STATE = "active";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `installScripts` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private InstallScripts installScripts;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `passwordEncoder` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `userService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private UserService userService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `adminSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AdminSettingsService adminSettingsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `widgetTypeService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private WidgetTypeService widgetTypeService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `widgetsBundleService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TenantService tenantService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantProfileService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TenantProfileService tenantProfileService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `customerService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private CustomerService customerService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DeviceService deviceService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceProfileService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DeviceProfileService deviceProfileService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AttributesService attributesService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceCredentialsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `ruleChainService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RuleChainService ruleChainService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TimeseriesService tsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `connectivityConfiguration` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DeviceConnectivityConfiguration connectivityConfiguration;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `persistActivityToTelemetry` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean persistActivityToTelemetry;

    @Lazy
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `queueService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private QueueService queueService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `jwtSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private JwtSettingsService jwtSettingsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `notificationSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private NotificationSettingsService notificationSettingsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `notificationTargetService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private NotificationTargetService notificationTargetService;

    @Bean
    /**
     * 方法说明：
     * 1. 职责：执行 `passwordEncoder` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 字段说明：
     * 1. 保存 `tsCallBackExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `initExecutor` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("sys-loader-ts-callback"));
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `shutdownExecutor` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void shutdownExecutor() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createSysAdmin` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createSysAdmin() {
        createUser(Authority.SYS_ADMIN, null, null, "sysadmin@thingsboard.org", "sysadmin");
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createDefaultTenantProfiles` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createDefaultTenantProfiles() throws Exception {
        tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);

        TenantProfileData isolatedRuleEngineTenantProfileData = new TenantProfileData();
        isolatedRuleEngineTenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());

        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);

        isolatedRuleEngineTenantProfileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));

        TenantProfile isolatedTbRuleEngineProfile = new TenantProfile();
        isolatedTbRuleEngineProfile.setDefault(false);
        isolatedTbRuleEngineProfile.setName("Isolated TB Rule Engine");
        isolatedTbRuleEngineProfile.setDescription("Isolated TB Rule Engine tenant profile");
        isolatedTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbRuleEngineProfile.setProfileData(isolatedRuleEngineTenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbRuleEngineProfile);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createAdminSettings` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setTenantId(TenantId.SYS_TENANT_ID);
        generalSettings.setKey("general");
        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        node.put("prohibitDifferentUrl", false);
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, generalSettings);

        AdminSettings mailSettings = new AdminSettings();
        mailSettings.setTenantId(TenantId.SYS_TENANT_ID);
        mailSettings.setKey("mail");
        node = JacksonUtil.newObjectNode();
        node.put("mailFrom", "ThingsBoard <sysadmin@localhost.localdomain>");
        node.put("smtpProtocol", "smtp");
        node.put("smtpHost", "localhost");
        node.put("smtpPort", "25");
        node.put("timeout", "10000");
        node.put("enableTls", false);
        node.put("username", "");
        node.put("password", "");
        node.put("tlsVersion", "TLSv1.2");//NOSONAR, key used to identify password field (not password value itself)
        node.put("enableProxy", false);
        node.put("showChangePassword", false);
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);

        AdminSettings connectivitySettings = new AdminSettings();
        connectivitySettings.setTenantId(TenantId.SYS_TENANT_ID);
        connectivitySettings.setKey("connectivity");
        connectivitySettings.setJsonValue(JacksonUtil.valueToTree(connectivityConfiguration.getConnectivity()));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, connectivitySettings);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createRandomJwtSettings` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createRandomJwtSettings() throws Exception {
        jwtSettingsService.createRandomJwtSettings();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createOAuth2Templates` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createOAuth2Templates() throws Exception {
        installScripts.createOAuth2Templates();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `loadDemoData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void loadDemoData() throws Exception {
        Tenant demoTenant = new Tenant();
        demoTenant.setRegion("Global");
        demoTenant.setTitle("Tenant");
        demoTenant = tenantService.saveTenant(demoTenant);
        installScripts.loadDemoRuleChains(demoTenant.getId());
        createUser(Authority.TENANT_ADMIN, demoTenant.getId(), null, "tenant@thingsboard.org", "tenant");

        Customer customerA = new Customer();
        customerA.setTenantId(demoTenant.getId());
        customerA.setTitle("Customer A");
        customerA = customerService.saveCustomer(customerA);
        Customer customerB = new Customer();
        customerB.setTenantId(demoTenant.getId());
        customerB.setTitle("Customer B");
        customerB = customerService.saveCustomer(customerB);
        Customer customerC = new Customer();
        customerC.setTenantId(demoTenant.getId());
        customerC.setTitle("Customer C");
        customerC = customerService.saveCustomer(customerC);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customer@thingsboard.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customerA@thingsboard.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerB.getId(), "customerB@thingsboard.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerC.getId(), "customerC@thingsboard.org", CUSTOMER_CRED);

        DeviceProfile defaultDeviceProfile = this.deviceProfileService.findOrCreateDeviceProfile(demoTenant.getId(), DEFAULT_DEVICE_TYPE);

        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A1", "A1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A2", "A2_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A3", "A3_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerB.getId(), defaultDeviceProfile.getId(), "Test Device B1", "B1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerC.getId(), defaultDeviceProfile.getId(), "Test Device C1", "C1_TEST_TOKEN", null);

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "DHT11 Demo Device", "DHT11_DEMO_TOKEN", "Demo device that is used in sample " +
                "applications that upload data from DHT11 temperature and humidity sensor");

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "Raspberry Pi Demo Device", "RASPBERRY_PI_DEMO_TOKEN", "Demo device that is used in " +
                "Raspberry Pi GPIO control sample application");

        DeviceProfile thermostatDeviceProfile = new DeviceProfile();
        thermostatDeviceProfile.setTenantId(demoTenant.getId());
        thermostatDeviceProfile.setDefault(false);
        thermostatDeviceProfile.setName("thermostat");
        thermostatDeviceProfile.setType(DeviceProfileType.DEFAULT);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        thermostatDeviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        thermostatDeviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        thermostatDeviceProfile.setDescription("Thermostat device profile");
        thermostatDeviceProfile.setDefaultRuleChainId(ruleChainService.findTenantRuleChainsByType(
                demoTenant.getId(), RuleChainType.CORE, new PageLink(1, 0, "Thermostat")).getData().get(0).getId());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        thermostatDeviceProfile.setProfileData(deviceProfileData);

        DeviceProfileAlarm highTemperature = new DeviceProfileAlarm();
        highTemperature.setId("highTemperatureAlarmID");
        highTemperature.setAlarmType("High Temperature");
        AlarmRule temperatureRule = new AlarmRule();
        AlarmCondition temperatureCondition = new AlarmCondition();
        temperatureCondition.setSpec(new SimpleAlarmConditionSpec());

        AlarmConditionFilter temperatureAlarmFlagAttributeFilter = new AlarmConditionFilter();
        temperatureAlarmFlagAttributeFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperatureAlarmFlag"));
        temperatureAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate temperatureAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        temperatureAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        temperatureAlarmFlagAttributePredicate.setValue(new FilterPredicateValue<>(Boolean.TRUE));
        temperatureAlarmFlagAttributeFilter.setPredicate(temperatureAlarmFlagAttributePredicate);

        AlarmConditionFilter temperatureTimeseriesFilter = new AlarmConditionFilter();
        temperatureTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperatureTimeseriesFilterPredicate = new NumericFilterPredicate();
        temperatureTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        FilterPredicateValue<Double> temperatureTimeseriesPredicateValue =
                new FilterPredicateValue<>(25.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "temperatureAlarmThreshold"));
        temperatureTimeseriesFilterPredicate.setValue(temperatureTimeseriesPredicateValue);
        temperatureTimeseriesFilter.setPredicate(temperatureTimeseriesFilterPredicate);
        temperatureCondition.setCondition(Arrays.asList(temperatureAlarmFlagAttributeFilter, temperatureTimeseriesFilter));
        temperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        temperatureRule.setCondition(temperatureCondition);
        highTemperature.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.MAJOR, temperatureRule)));

        AlarmRule clearTemperatureRule = new AlarmRule();
        AlarmCondition clearTemperatureCondition = new AlarmCondition();
        clearTemperatureCondition.setSpec(new SimpleAlarmConditionSpec());

        AlarmConditionFilter clearTemperatureTimeseriesFilter = new AlarmConditionFilter();
        clearTemperatureTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        clearTemperatureTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate clearTemperatureTimeseriesFilterPredicate = new NumericFilterPredicate();
        clearTemperatureTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS_OR_EQUAL);
        FilterPredicateValue<Double> clearTemperatureTimeseriesPredicateValue =
                new FilterPredicateValue<>(25.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "temperatureAlarmThreshold"));

        clearTemperatureTimeseriesFilterPredicate.setValue(clearTemperatureTimeseriesPredicateValue);
        clearTemperatureTimeseriesFilter.setPredicate(clearTemperatureTimeseriesFilterPredicate);
        clearTemperatureCondition.setCondition(Collections.singletonList(clearTemperatureTimeseriesFilter));
        clearTemperatureRule.setCondition(clearTemperatureCondition);
        clearTemperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        highTemperature.setClearRule(clearTemperatureRule);

        DeviceProfileAlarm lowHumidity = new DeviceProfileAlarm();
        lowHumidity.setId("lowHumidityAlarmID");
        lowHumidity.setAlarmType("Low Humidity");
        AlarmRule humidityRule = new AlarmRule();
        AlarmCondition humidityCondition = new AlarmCondition();
        humidityCondition.setSpec(new SimpleAlarmConditionSpec());

        AlarmConditionFilter humidityAlarmFlagAttributeFilter = new AlarmConditionFilter();
        humidityAlarmFlagAttributeFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "humidityAlarmFlag"));
        humidityAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate humidityAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        humidityAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        humidityAlarmFlagAttributePredicate.setValue(new FilterPredicateValue<>(Boolean.TRUE));
        humidityAlarmFlagAttributeFilter.setPredicate(humidityAlarmFlagAttributePredicate);

        AlarmConditionFilter humidityTimeseriesFilter = new AlarmConditionFilter();
        humidityTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "humidity"));
        humidityTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate humidityTimeseriesFilterPredicate = new NumericFilterPredicate();
        humidityTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        FilterPredicateValue<Double> humidityTimeseriesPredicateValue =
                new FilterPredicateValue<>(60.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "humidityAlarmThreshold"));
        humidityTimeseriesFilterPredicate.setValue(humidityTimeseriesPredicateValue);
        humidityTimeseriesFilter.setPredicate(humidityTimeseriesFilterPredicate);
        humidityCondition.setCondition(Arrays.asList(humidityAlarmFlagAttributeFilter, humidityTimeseriesFilter));

        humidityRule.setCondition(humidityCondition);
        humidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidity.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.MINOR, humidityRule)));

        AlarmRule clearHumidityRule = new AlarmRule();
        AlarmCondition clearHumidityCondition = new AlarmCondition();
        clearHumidityCondition.setSpec(new SimpleAlarmConditionSpec());

        AlarmConditionFilter clearHumidityTimeseriesFilter = new AlarmConditionFilter();
        clearHumidityTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "humidity"));
        clearHumidityTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate clearHumidityTimeseriesFilterPredicate = new NumericFilterPredicate();
        clearHumidityTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL);
        FilterPredicateValue<Double> clearHumidityTimeseriesPredicateValue =
                new FilterPredicateValue<>(60.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "humidityAlarmThreshold"));

        clearHumidityTimeseriesFilterPredicate.setValue(clearHumidityTimeseriesPredicateValue);
        clearHumidityTimeseriesFilter.setPredicate(clearHumidityTimeseriesFilterPredicate);
        clearHumidityCondition.setCondition(Collections.singletonList(clearHumidityTimeseriesFilter));
        clearHumidityRule.setCondition(clearHumidityCondition);
        clearHumidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidity.setClearRule(clearHumidityRule);

        deviceProfileData.setAlarms(Arrays.asList(highTemperature, lowHumidity));

        DeviceProfile savedThermostatDeviceProfile = deviceProfileService.saveDeviceProfile(thermostatDeviceProfile);

        DeviceId t1Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T1", "T1_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();
        DeviceId t2Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T2", "T2_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();

        attributesService.save(demoTenant.getId(), t1Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.3948)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -122.1503)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 20)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 50))));

        attributesService.save(demoTenant.getId(), t2Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.493801)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -121.948769)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 25)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 30))));

        installScripts.loadDashboards(demoTenant.getId(), null);
        installScripts.createDefaultTenantDashboards(demoTenant.getId(), null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `loadSystemWidgets` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void loadSystemWidgets() throws Exception {
        installScripts.loadSystemWidgets();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createUser` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private User createUser(Authority authority,
                            TenantId tenantId,
                            CustomerId customerId,
                            String email,
                            String password) {
        User user = new User();
        user.setAuthority(authority);
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user = userService.saveUser(tenantId, user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
        userCredentials.setPassword(passwordEncoder.encode(password));
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
        return user;
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
    private Device createDevice(TenantId tenantId,
                                CustomerId customerId,
                                DeviceProfileId deviceProfileId,
                                String name,
                                String accessToken,
                                String description) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setDeviceProfileId(deviceProfileId);
        device.setName(name);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (description != null) {
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        save(device.getId(), ACTIVITY_STATE, false);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(TenantId.SYS_TENANT_ID, deviceCredentials);
        return device;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void save(DeviceId deviceId, String key, boolean value) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (persistActivityToTelemetry) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            ListenableFuture<Integer> saveFuture = tsService.save(
                    TenantId.SYS_TENANT_ID,
                    deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))), 0L);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            ListenableFuture<List<String>> saveFuture = attributesService.save(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE,
                    Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                    , System.currentTimeMillis())));
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`TelemetrySaveCallback` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        /**
         * 字段说明：
         * 1. 保存 `deviceId` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final DeviceId deviceId;
        private final String key;
        /**
         * 字段说明：
         * 1. 保存 `value` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onSuccess` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onFailure` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addTsCallback` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <S> void addTsCallback(ListenableFuture<S> saveFuture, final FutureCallback<S> callback) {
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createQueues` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createQueues() {
        Queue mainQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.MAIN_QUEUE_NAME);
        if (mainQueue == null) {
            mainQueue = new Queue();
            mainQueue.setTenantId(TenantId.SYS_TENANT_ID);
            mainQueue.setName(DataConstants.MAIN_QUEUE_NAME);
            mainQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
            mainQueue.setPollInterval(25);
            mainQueue.setPartitions(10);
            mainQueue.setConsumerPerPartition(true);
            mainQueue.setPackProcessingTimeout(2000);
            SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
            mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
            mainQueueSubmitStrategy.setBatchSize(1000);
            mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
            ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
            mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
            mainQueueProcessingStrategy.setRetries(3);
            mainQueueProcessingStrategy.setFailurePercentage(0);
            mainQueueProcessingStrategy.setPauseBetweenRetries(3);
            mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
            mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
            queueService.saveQueue(mainQueue);
        }

        Queue highPriorityQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.HP_QUEUE_NAME);
        if (highPriorityQueue == null) {
            highPriorityQueue = new Queue();
            highPriorityQueue.setTenantId(TenantId.SYS_TENANT_ID);
            highPriorityQueue.setName(DataConstants.HP_QUEUE_NAME);
            highPriorityQueue.setTopic(DataConstants.HP_QUEUE_TOPIC);
            highPriorityQueue.setPollInterval(25);
            highPriorityQueue.setPartitions(10);
            highPriorityQueue.setConsumerPerPartition(true);
            highPriorityQueue.setPackProcessingTimeout(2000);
            SubmitStrategy highPriorityQueueSubmitStrategy = new SubmitStrategy();
            highPriorityQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
            highPriorityQueueSubmitStrategy.setBatchSize(100);
            highPriorityQueue.setSubmitStrategy(highPriorityQueueSubmitStrategy);
            ProcessingStrategy highPriorityQueueProcessingStrategy = new ProcessingStrategy();
            highPriorityQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
            highPriorityQueueProcessingStrategy.setRetries(0);
            highPriorityQueueProcessingStrategy.setFailurePercentage(0);
            highPriorityQueueProcessingStrategy.setPauseBetweenRetries(5);
            highPriorityQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
            highPriorityQueue.setProcessingStrategy(highPriorityQueueProcessingStrategy);
            queueService.saveQueue(highPriorityQueue);
        }

        Queue sequentialByOriginatorQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.SQ_QUEUE_NAME);
        if (sequentialByOriginatorQueue == null) {
            sequentialByOriginatorQueue = new Queue();
            sequentialByOriginatorQueue.setTenantId(TenantId.SYS_TENANT_ID);
            sequentialByOriginatorQueue.setName(DataConstants.SQ_QUEUE_NAME);
            sequentialByOriginatorQueue.setTopic(DataConstants.SQ_QUEUE_TOPIC);
            sequentialByOriginatorQueue.setPollInterval(25);
            sequentialByOriginatorQueue.setPartitions(10);
            sequentialByOriginatorQueue.setPackProcessingTimeout(2000);
            sequentialByOriginatorQueue.setConsumerPerPartition(true);
            SubmitStrategy sequentialByOriginatorQueueSubmitStrategy = new SubmitStrategy();
            sequentialByOriginatorQueueSubmitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
            sequentialByOriginatorQueueSubmitStrategy.setBatchSize(100);
            sequentialByOriginatorQueue.setSubmitStrategy(sequentialByOriginatorQueueSubmitStrategy);
            ProcessingStrategy sequentialByOriginatorQueueProcessingStrategy = new ProcessingStrategy();
            sequentialByOriginatorQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
            sequentialByOriginatorQueueProcessingStrategy.setRetries(3);
            sequentialByOriginatorQueueProcessingStrategy.setFailurePercentage(0);
            sequentialByOriginatorQueueProcessingStrategy.setPauseBetweenRetries(5);
            sequentialByOriginatorQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
            sequentialByOriginatorQueue.setProcessingStrategy(sequentialByOriginatorQueueProcessingStrategy);
            queueService.saveQueue(sequentialByOriginatorQueue);
        }
    }

    @Override
    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `createDefaultNotificationConfigs` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createDefaultNotificationConfigs() {
        log.info("Creating default notification configs for system admin");
        if (notificationTargetService.countNotificationTargetsByTenantId(TenantId.SYS_TENANT_ID) == 0) {
            notificationSettingsService.createDefaultNotificationConfigs(TenantId.SYS_TENANT_ID);
        }
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 500);
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
        log.info("Creating default notification configs for all tenants");
        AtomicInteger count = new AtomicInteger();
        for (TenantId tenantId : tenants) {
            executor.submit(() -> {
                if (notificationTargetService.countNotificationTargetsByTenantId(tenantId) == 0) {
                    notificationSettingsService.createDefaultNotificationConfigs(tenantId);
                    int n = count.incrementAndGet();
                    if (n % 500 == 0) {
                        log.info("{} tenants processed", n);
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `updateDefaultNotificationConfigs` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void updateDefaultNotificationConfigs() {
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 500);
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
        log.info("Updating default edge failure notification configs for all tenants");
        AtomicInteger count = new AtomicInteger();
        for (TenantId tenantId : tenants) {
            executor.submit(() -> {
                notificationSettingsService.updateDefaultNotificationConfigs(tenantId);
                int n = count.incrementAndGet();
                if (n % 500 == 0) {
                    log.info("{} tenants processed", n);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        notificationSettingsService.updateDefaultNotificationConfigs(TenantId.SYS_TENANT_ID);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultSystemDataLoaderService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
