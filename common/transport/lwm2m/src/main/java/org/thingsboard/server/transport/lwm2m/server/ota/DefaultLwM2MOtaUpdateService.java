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
package org.thingsboard.server.transport.lwm2m.server.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareDeliveryMethod;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MFirmwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MSoftwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MClientOtaInfoStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.ota.OtaPackageKey.STATE;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getAttributeKey;
import static org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult.UPDATE_SUCCESSFULLY;
import static org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult.NOT_ENOUGH_STORAGE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultLwM2MOtaUpdateService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class DefaultLwM2MOtaUpdateService extends LwM2MExecutorAwareService implements LwM2MOtaUpdateService {

    public static final String FIRMWARE_VERSION = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION);
    public static final String FIRMWARE_TITLE = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE);
    public static final String FIRMWARE_TAG = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TAG);
    public static final String FIRMWARE_URL = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.URL);
    public static final String SOFTWARE_VERSION = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION);
    public static final String SOFTWARE_TITLE = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE);
    public static final String SOFTWARE_TAG = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TAG);
    public static final String SOFTWARE_URL = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.URL);

    /**
     * 字段说明：
     * 1. 保存 `FIRMWARE_UPDATE_COAP_RESOURCE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String FIRMWARE_UPDATE_COAP_RESOURCE = "tbfw";
    public static final String SOFTWARE_UPDATE_COAP_RESOURCE = "tbsw";
    /**
     * 字段说明：
     * 1. 保存 `FW_PACKAGE_5_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String FW_PACKAGE_5_ID = "/5/0/0";
    private static final String FW_PACKAGE_19_ID = "/19/0/0";
    /**
     * 字段说明：
     * 1. 保存 `FW_URL_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String FW_URL_ID = "/5/0/1";
    private static final String FW_EXECUTE_ID = "/5/0/2";
    /**
     * 字段说明：
     * 1. 保存 `FW_STATE_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String FW_STATE_ID = "/5/0/3";
    public static final String FW_RESULT_ID = "/5/0/5";
    /**
     * 字段说明：
     * 1. 保存 `FW_NAME_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String FW_NAME_ID = "/5/0/6";
    public static final String FW_VER_ID = "/5/0/7";
    /**
     * Quectel@Hi15RM1-HLB_V1.0@BC68JAR01A10,V150R100C20B300SP7,V150R100C20B300SP7@8
     * Revision:BC68JAR01A10
     */
    /**
     * 字段说明：
     * 1. 保存 `FW_3_VER_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String FW_3_VER_ID = "/3/0/3";
    public static final String FW_DELIVERY_METHOD = "/5/0/9";

    /**
     * 字段说明：
     * 1. 保存 `SW_3_VER_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SW_3_VER_ID = "/3/0/19";

    /**
     * 字段说明：
     * 1. 保存 `SW_NAME_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SW_NAME_ID = "/9/0/0";
    public static final String SW_VER_ID = "/9/0/1";
    /**
     * 字段说明：
     * 1. 保存 `SW_PACKAGE_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SW_PACKAGE_ID = "/9/0/2";
    public static final String SW_PACKAGE_URI_ID = "/9/0/3";
    /**
     * 字段说明：
     * 1. 保存 `SW_INSTALL_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SW_INSTALL_ID = "/9/0/4";
    public static final String SW_STATE_ID = "/9/0/7";
    /**
     * 字段说明：
     * 1. 保存 `SW_RESULT_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SW_RESULT_ID = "/9/0/9";
    public static final String SW_UN_INSTALL_ID = "/9/0/6";

    private final Map<String, LwM2MClientFwOtaInfo> fwStates = new ConcurrentHashMap<>();
    private final Map<String, LwM2MClientSwOtaInfo> swStates = new ConcurrentHashMap<>();

    /**
     * 字段说明：
     * 1. 保存 `transportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TransportService transportService;
    private final LwM2mClientContext clientContext;
    /**
     * 字段说明：
     * 1. 保存 `config` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MTransportServerConfig config;
    private final LwM2mUplinkMsgHandler uplinkHandler;
    /**
     * 字段说明：
     * 1. 保存 `downlinkHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    private final OtaPackageDataCache otaPackageDataCache;
    /**
     * 字段说明：
     * 1. 保存 `logService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MTelemetryLogService logService;
    private final LwM2mTransportServerHelper helper;
    /**
     * 字段说明：
     * 1. 保存 `otaInfoStore` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbLwM2MClientOtaInfoStore otaInfoStore;

    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2MAttributesService attributesService;

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        super.init();
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `destroy` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void destroy() {
        log.trace("Destroying {}", getClass().getSimpleName());
        super.destroy();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getExecutorSize` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected int getExecutorSize() {
        return config.getOtaPoolSize();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getExecutorName` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String getExecutorName() {
        return "LwM2M OTA";
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init(LwM2mClient client) {
        //TODO: add locks by client fwInfo.
        //TODO: check that the client supports FW and SW by checking the supported objects in the model.
        List<String> attributesToFetch = new ArrayList<>();
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (fwInfo.isSupported()) {
            attributesToFetch.add(FIRMWARE_TITLE);
            attributesToFetch.add(FIRMWARE_VERSION);
            attributesToFetch.add(FIRMWARE_TAG);
            attributesToFetch.add(FIRMWARE_URL);
        }

        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (swInfo.isSupported()) {
            attributesToFetch.add(SOFTWARE_TITLE);
            attributesToFetch.add(SOFTWARE_VERSION);
            attributesToFetch.add(SOFTWARE_TAG);
            attributesToFetch.add(SOFTWARE_URL);
        }

        var clientSettings = clientContext.getProfile(client.getProfileId()).getClientLwM2mSettings();
        initFwStrategy(client, clientSettings);
        initSwStrategy(client, clientSettings);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!attributesToFetch.isEmpty()) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            var future = attributesService.getSharedAttributes(client, attributesToFetch);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            DonAsynchron.withCallback(future, attrs -> {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (fwInfo.isSupported()) {
                    Optional<String> newFwTitle = getAttributeValue(attrs, FIRMWARE_TITLE);
                    Optional<String> newFwVersion = getAttributeValue(attrs, FIRMWARE_VERSION);
                    Optional<String> newFwTag = getAttributeValue(attrs, FIRMWARE_TAG);
                    Optional<String> newFwUrl = getAttributeValue(attrs, FIRMWARE_URL);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (newFwTitle.isPresent() && newFwVersion.isPresent() && !isOtaDownloading(client) && !UPDATING.equals(fwInfo.status)) {
                        onTargetFirmwareUpdate(client, newFwTitle.get(), newFwVersion.get(), newFwUrl, newFwTag);
                    }
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (swInfo.isSupported()) {
                    Optional<String> newSwTitle = getAttributeValue(attrs, SOFTWARE_TITLE);
                    Optional<String> newSwVersion = getAttributeValue(attrs, SOFTWARE_VERSION);
                    Optional<String> newSwTag = getAttributeValue(attrs, SOFTWARE_TAG);
                    Optional<String> newSwUrl = getAttributeValue(attrs, SOFTWARE_URL);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (newSwTitle.isPresent() && newSwVersion.isPresent()) {
                        onTargetSoftwareUpdate(client, newSwTitle.get(), newSwVersion.get(), newSwUrl, newSwTag);
                    }
                }
            }, throwable -> {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (fwInfo.isSupported()) {
                    update(fwInfo);
                }
            }, executor);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `forceFirmwareUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void forceFirmwareUpdate(LwM2mClient client) {
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setRetryAttempts(0);
        fwInfo.setFailedPackageId(null);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onTargetFirmwareUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onTargetFirmwareUpdate(LwM2mClient client, String newFirmwareTitle, String newFirmwareVersion, Optional<String> newFirmwareUrl, Optional<String> newFirmwareTag) {
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.updateTarget(newFirmwareTitle, newFirmwareVersion, newFirmwareUrl, newFirmwareTag);
        update(fwInfo);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentFirmwareNameUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentFirmwareNameUpdate(LwM2mClient client, String name) {
        log.debug("[{}] Current fw name: {}", client.getEndpoint(), name);
        getOrInitFwInfo(client).setCurrentName(name);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentSoftwareNameUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentSoftwareNameUpdate(LwM2mClient client, String name) {
        log.debug("[{}] Current sw name: {}", client.getEndpoint(), name);
        getOrInitSwInfo(client).setCurrentName(name);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onFirmwareStrategyUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onFirmwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration) {
        log.debug("[{}] Current fw strategy: {}", client.getEndpoint(), configuration.getFwUpdateStrategy());
        startFirmwareUpdateIfNeeded(client, initFwStrategy(client, configuration));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initFwStrategy` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MClientFwOtaInfo initFwStrategy(LwM2mClient client, OtherConfiguration configuration) {
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setStrategy(LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(configuration.getFwUpdateStrategy()));
        fwInfo.setBaseUrl(configuration.getFwUpdateResource());
        return fwInfo;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentSoftwareStrategyUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentSoftwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration) {
        log.debug("[{}] Current sw strategy: {}", client.getEndpoint(), configuration.getSwUpdateStrategy());
        startSoftwareUpdateIfNeeded(client, initSwStrategy(client, configuration));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initSwStrategy` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MClientSwOtaInfo initSwStrategy(LwM2mClient client, OtherConfiguration configuration) {
        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        swInfo.setStrategy(LwM2MSoftwareUpdateStrategy.fromStrategySwByCode(configuration.getSwUpdateStrategy()));
        swInfo.setBaseUrl(configuration.getSwUpdateResource());
        return swInfo;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentFirmwareVersion3Update` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentFirmwareVersion3Update(LwM2mClient client, String version) {
        log.debug("[{}] Current fw version(3): {}", client.getEndpoint(), version);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion3(version);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentFirmwareVersionUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentFirmwareVersionUpdate(LwM2mClient client, String version) {
        log.debug("[{}] Current fw version(5): {}", client.getEndpoint(), version);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion(version);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentFirmwareStateUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentFirmwareStateUpdate(LwM2mClient client, Long stateCode) {
        log.debug("[{}] Current fw state: {}", client.getEndpoint(), stateCode);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        FirmwareUpdateState state = FirmwareUpdateState.fromStateFwByCode(stateCode.intValue());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (FirmwareUpdateState.DOWNLOADED.equals(state)) {
            executeFwUpdate(client);
        }
        fwInfo.setUpdateState(state);
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(state);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (FirmwareUpdateState.IDLE.equals(state) && DOWNLOADING.equals(fwInfo.getStatus())) {
            fwInfo.setFailedPackageId(fwInfo.getTargetPackageId());
            status = Optional.of(FAILED);
        }

        status.ifPresent(otaStatus -> {
            fwInfo.setStatus(otaStatus);
            sendStateUpdateToTelemetry(client, fwInfo,
                    otaStatus, "Firmware Update State: " + state.name());
        });
        update(fwInfo);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentFirmwareResultUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentFirmwareResultUpdate(LwM2mClient client, Long code) {
        log.debug("[{}] Current fw result: {}", client.getEndpoint(), code);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        FirmwareUpdateResult result = FirmwareUpdateResult.fromUpdateResultFwByCode(code.intValue());
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(result);

        if (FirmwareUpdateResult.INITIAL.equals(result) && OtaPackageUpdateStatus.UPDATING.equals(fwInfo.getStatus())) {
            status = Optional.of(UPDATED);
            fwInfo.setRetryAttempts(0);
            fwInfo.setFailedPackageId(null);
        }

        status.ifPresent(otaStatus -> {
                    fwInfo.setStatus(otaStatus);
                    sendStateUpdateToTelemetry(client, fwInfo,
                            otaStatus, "Firmware Update Result: " + result.name());
                }
        );

        if (result.isAgain() && fwInfo.getRetryAttempts() <= 2) {
            fwInfo.setRetryAttempts(fwInfo.getRetryAttempts() + 1);
            startFirmwareUpdateIfNeeded(client, fwInfo);
        } else {
            fwInfo.update(result);
        }
        update(fwInfo);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentFirmwareDeliveryMethodUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient client, Long value) {
        log.debug("[{}] Current fw delivery method: {}", client.getEndpoint(), value);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setDeliveryMethod(value.intValue());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentSoftwareVersion3Update` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentSoftwareVersion3Update(LwM2mClient client, String version) {
        log.debug("[{}] Current sw version(3): {}", client.getEndpoint(), version);
        getOrInitSwInfo(client).setCurrentVersion3(version);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentSoftwareVersionUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentSoftwareVersionUpdate(LwM2mClient client, String version) {
        log.debug("[{}] Current sw version(9): {}", client.getEndpoint(), version);
        getOrInitSwInfo(client).setCurrentVersion(version);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentSoftwareStateUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentSoftwareStateUpdate(LwM2mClient client, Long stateCode) {
        log.debug("[{}] Current sw state: {}", client.getEndpoint(), stateCode);
        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        SoftwareUpdateState state = SoftwareUpdateState.fromUpdateStateSwByCode(stateCode.intValue());
        if (SoftwareUpdateState.INITIAL.equals(state)) {
            startSoftwareUpdateIfNeeded(client, swInfo);
        } else if (SoftwareUpdateState.DELIVERED.equals(state)) {
            executeSwInstall(client);
        }
        swInfo.setUpdateState(state);
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(state);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, swInfo,
                otaStatus, "Firmware Update State: " + state.name()));
        update(swInfo);
    }


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCurrentSoftwareResultUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCurrentSoftwareResultUpdate(LwM2mClient client, Long code) {
        log.debug("[{}] Current sw result: {}", client.getEndpoint(), code);
        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        SoftwareUpdateResult result = SoftwareUpdateResult.fromUpdateResultSwByCode(code.intValue());
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(result);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, swInfo,
                otaStatus, "Software Update Result: " + result.name()));
        if (result.isAgain() && swInfo.getRetryAttempts() <= 2) {
            swInfo.setRetryAttempts(swInfo.getRetryAttempts() + 1);
            startSoftwareUpdateIfNeeded(client, swInfo);
        } else {
            swInfo.update(result);
        }
        update(swInfo);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onTargetSoftwareUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onTargetSoftwareUpdate(LwM2mClient client, String newSoftwareTitle, String newSoftwareVersion, Optional<String> newSoftwareUrl, Optional<String> newSoftwareTag) {
        LwM2MClientSwOtaInfo fwInfo = getOrInitSwInfo(client);
        fwInfo.updateTarget(newSoftwareTitle, newSoftwareVersion, newSoftwareUrl, newSoftwareTag);
        update(fwInfo);
        startSoftwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `isOtaDownloading` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean isOtaDownloading(LwM2mClient client) {
        String endpoint = client.getEndpoint();
        LwM2MClientFwOtaInfo fwInfo = fwStates.get(endpoint);
        LwM2MClientSwOtaInfo swInfo = swStates.get(endpoint);

        if (fwInfo != null && (DOWNLOADING.equals(fwInfo.getStatus()))) {
            return true;
        }
        if (swInfo != null && (DOWNLOADING.equals(swInfo.getStatus()))) {
            return true;
        }

        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startFirmwareUpdateIfNeeded` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void startFirmwareUpdateIfNeeded(LwM2mClient client, LwM2MClientFwOtaInfo fwInfo) {
        try {
            if (!fwInfo.isSupported() && fwInfo.isAssigned()) {
                log.debug("[{}] Fw update is not supported: {}", client.getEndpoint(), fwInfo);
                sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Client does not support firmware update or profile misconfiguration!");
            } else if (fwInfo.isUpdateRequired()) {
                if (StringUtils.isNotEmpty(fwInfo.getTargetUrl())) {
                    log.debug("[{}] Starting update to [{}{}][] using URL: {}", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion(), fwInfo.getTargetUrl());
                    startUpdateUsingUrl(client, FW_URL_ID, fwInfo.getTargetUrl());
                } else {
                    log.debug("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion());
                    startUpdateUsingBinary(client, fwInfo);
                }
            } else if (fwInfo.getResult() != null && fwInfo.getResult().getCode() > UPDATE_SUCCESSFULLY.getCode()) {
                log.trace("[{}] Previous update failed. [{}]", client.getEndpoint(), fwInfo);
                logService.log(client, "Previous update firmware failed. Result: " + fwInfo.getResult().name());
            }
        } catch (Exception e) {
            log.error("[{}] failed to update client: {}", client.getEndpoint(), fwInfo, e);
            sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startSoftwareUpdateIfNeeded` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void startSoftwareUpdateIfNeeded(LwM2mClient client, LwM2MClientSwOtaInfo swInfo) {
        try {
            if (!swInfo.isSupported() && swInfo.isAssigned()) {
                log.debug("[{}] Sw update is not supported: {}", client.getEndpoint(), swInfo);
                sendStateUpdateToTelemetry(client, swInfo, OtaPackageUpdateStatus.FAILED, "Client does not support software update or profile misconfiguration!");
            } else if (swInfo.isUpdateRequired()) {
                if (SoftwareUpdateState.INSTALLED.equals(swInfo.getUpdateState())) {
                    log.debug("[{}] Attempt to restore the update state: {}", client.getEndpoint(), swInfo.getUpdateState());
                    executeSwUninstallForUpdate(client);
                } else {
                    if (StringUtils.isNotEmpty(swInfo.getTargetUrl())) {
                        log.debug("[{}] Starting update to [{}{}] using URL: {}", client.getEndpoint(), swInfo.getTargetName(), swInfo.getTargetVersion(), swInfo.getTargetUrl());
                        startUpdateUsingUrl(client, SW_PACKAGE_URI_ID, swInfo.getTargetUrl());
                    } else {
                        log.debug("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), swInfo.getTargetName(), swInfo.getTargetVersion());
                        startUpdateUsingBinary(client, swInfo);
                    }
                }
            } else if (swInfo.getResult() != null && swInfo.getResult().getCode() >= NOT_ENOUGH_STORAGE.getCode()) {
                log.trace("[{}] Previous update failed. [{}]", client.getEndpoint(), swInfo);
                logService.log(client, "Previous update software failed. Result: " + swInfo.getResult().name());
            }
        } catch (Exception e) {
            log.info("[{}] failed to update client: {}", client.getEndpoint(), swInfo, e);
            sendStateUpdateToTelemetry(client, swInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startUpdateUsingBinary` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void startUpdateUsingBinary(LwM2mClient client, LwM2MClientSwOtaInfo swInfo) {
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), swInfo.getType().name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateSoftwareUsingBinary(response, swInfo, client));
                    }

                    @Override
                    public void onError(Throwable e) {
                        logService.log(client, "Failed to process software update: " + e.getMessage());
                    }
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startUpdateUsingUrl` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void startUpdateUsingUrl(LwM2mClient client, String id, String url) {
        String targetIdVer = convertObjectIdToVersionedId(id, client.getRegistration());
        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(targetIdVer).value(url).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, targetIdVer));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startUpdateUsingBinary` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void startUpdateUsingBinary(LwM2mClient client, LwM2MClientFwOtaInfo fwInfo) {
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), fwInfo.getType().name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateFirmwareUsingBinary(response, fwInfo, client));
                    }

                    @Override
                    public void onError(Throwable e) {
                        logService.log(client, "Failed to process firmware update: " + e.getMessage());
                    }
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doUpdateFirmwareUsingBinary` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void doUpdateFirmwareUsingBinary(TransportProtos.GetOtaPackageResponseMsg response, LwM2MClientFwOtaInfo info, LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MFirmwareUpdateStrategy strategy;
            if (info.getDeliveryMethod() == null || info.getDeliveryMethod() == FirmwareDeliveryMethod.BOTH.code) {
                strategy = info.getStrategy();
            } else {
                strategy = info.getDeliveryMethod() == FirmwareDeliveryMethod.PULL.code ? LwM2MFirmwareUpdateStrategy.OBJ_5_TEMP_URL : LwM2MFirmwareUpdateStrategy.OBJ_5_BINARY;
            }
            switch (strategy) {
                case OBJ_5_BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(FW_PACKAGE_5_ID, client.getRegistration()), otaPackageId);
                    break;
                case OBJ_19_BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(FW_PACKAGE_19_ID, client.getRegistration()), otaPackageId);
                    break;
                case OBJ_5_TEMP_URL:
                    startUpdateUsingUrl(client, FW_URL_ID, info.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RESOURCE + "/" + otaPackageId.toString());
                    break;
                default:
                    sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doUpdateSoftwareUsingBinary` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void doUpdateSoftwareUsingBinary(TransportProtos.GetOtaPackageResponseMsg response, LwM2MClientSwOtaInfo info, LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MSoftwareUpdateStrategy strategy = info.getStrategy();
            switch (strategy) {
                case BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(SW_PACKAGE_ID, client.getRegistration()), otaPackageId);
                    break;
                case TEMP_URL:
                    startUpdateUsingUrl(client, SW_PACKAGE_URI_ID, info.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RESOURCE + "/" + otaPackageId.toString());
                    break;
                default:
                    sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startUpdateUsingBinary` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void startUpdateUsingBinary(LwM2mClient client, String versionedId, UUID otaPackageId) {
        byte[] firmwareChunk = otaPackageDataCache.get(otaPackageId.toString(), 0, 0);
        TbLwM2MWriteReplaceRequest writeRequest = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId)
                .value(firmwareChunk).contentFormat(ContentFormat.OPAQUE)
                .timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendWriteReplaceRequest(client, writeRequest, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createOtaPackageRequestMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TransportProtos.GetOtaPackageRequestMsg createOtaPackageRequestMsg(TransportProtos.SessionInfoProto sessionInfo, String nameFwSW) {
        return TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                .setType(nameFwSW)
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `executeFwUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void executeFwUpdate(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(FW_EXECUTE_ID).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, FW_EXECUTE_ID));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `executeSwInstall` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void executeSwInstall(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(SW_INSTALL_ID).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, SW_INSTALL_ID));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `executeSwUninstallForUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void executeSwUninstallForUpdate(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(SW_UN_INSTALL_ID).params("1").timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, SW_INSTALL_ID));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getAttributeValue` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Optional<String> getAttributeValue(List<TransportProtos.TsKvProto> attrs, String keyName) {
        for (TransportProtos.TsKvProto attr : attrs) {
            if (keyName.equals(attr.getKv().getKey())) {
                if (attr.getKv().getType().equals(TransportProtos.KeyValueType.STRING_V)) {
                    return Optional.of(attr.getKv().getStringV());
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getOrInitFwInfo` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MClientFwOtaInfo getOrInitFwInfo(LwM2mClient client) {
        return this.fwStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            LwM2MClientFwOtaInfo info = otaInfoStore.getFw(endpoint);
            if (info == null) {
                var profile = clientContext.getProfile(client.getProfileId());
                info = new LwM2MClientFwOtaInfo(endpoint, profile.getClientLwM2mSettings().getFwUpdateResource(),
                        LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(profile.getClientLwM2mSettings().getFwUpdateStrategy()));
                update(info);
            }
            return info;
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getOrInitSwInfo` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MClientSwOtaInfo getOrInitSwInfo(LwM2mClient client) {
        return this.swStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            LwM2MClientSwOtaInfo info = otaInfoStore.getSw(endpoint);
            if (info == null) {
                var profile = clientContext.getProfile(client.getProfileId());
                info = new LwM2MClientSwOtaInfo(endpoint, profile.getClientLwM2mSettings().getSwUpdateResource(),
                        LwM2MSoftwareUpdateStrategy.fromStrategySwByCode(profile.getClientLwM2mSettings().getSwUpdateStrategy()));
                update(info);
            }
            return info;
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `update` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void update(LwM2MClientFwOtaInfo info) {
        otaInfoStore.putFw(info);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `update` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void update(LwM2MClientSwOtaInfo info) {
        otaInfoStore.putSw(info);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendStateUpdateToTelemetry` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void sendStateUpdateToTelemetry(LwM2mClient client, LwM2MClientOtaInfo<?, ?, ?> fwInfo, OtaPackageUpdateStatus status, String log) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(getAttributeKey(fwInfo.getType(), STATE));
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(status.name());
        result.add(kvProto.build());
        kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY);
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(log);
        result.add(kvProto.build());
        helper.sendParametersOnThingsboardTelemetry(result, client.getSession(), client.getKeyTsLatestMap());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toOtaPackageUpdateStatus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(FirmwareUpdateResult fwUpdateResult) {
        switch (fwUpdateResult) {
            case INITIAL:
                return Optional.empty();
            case UPDATE_SUCCESSFULLY:
                return Optional.of(UPDATED);
            case NOT_ENOUGH:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case INTEGRITY_CHECK_FAILURE:
            case UNSUPPORTED_TYPE:
            case INVALID_URI:
            case UPDATE_FAILED:
            case UNSUPPORTED_PROTOCOL:
                return Optional.of(FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", fwUpdateResult.name());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toOtaPackageUpdateStatus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(FirmwareUpdateState firmwareUpdateState) {
        switch (firmwareUpdateState) {
            case IDLE:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(DOWNLOADED);
            case UPDATING:
                return Optional.of(UPDATING);
            default:
                throw new CodecException("Invalid value stateFw %d for FirmwareUpdateStatus.", firmwareUpdateState);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toOtaPackageUpdateStatus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(SoftwareUpdateState swUpdateState) {
        switch (swUpdateState) {
            case INITIAL:
                return Optional.empty();
            case DOWNLOAD_STARTED:
                return Optional.of(DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(DOWNLOADING);
            case DELIVERED:
                return Optional.of(DOWNLOADED);
            case INSTALLED:
                return Optional.empty();
            default:
                throw new CodecException("Invalid value stateSw %d for SoftwareUpdateState.", swUpdateState);
        }
    }

    /**
     * FirmwareUpdateStatus {
     * DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `toOtaPackageUpdateStatus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(SoftwareUpdateResult softwareUpdateResult) {
        switch (softwareUpdateResult) {
            case INITIAL:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(DOWNLOADING);
            case SUCCESSFULLY_INSTALLED:
                return Optional.of(UPDATED);
            case SUCCESSFULLY_DOWNLOADED_VERIFIED:
                return Optional.of(VERIFIED);
            case NOT_ENOUGH_STORAGE:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case PACKAGE_CHECK_FAILURE:
            case UNSUPPORTED_PACKAGE_TYPE:
            case INVALID_URI:
            case UPDATE_ERROR:
            case INSTALL_FAILURE:
            case UN_INSTALL_FAILURE:
                return Optional.of(FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", softwareUpdateResult.name());
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultLwM2MOtaUpdateService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
