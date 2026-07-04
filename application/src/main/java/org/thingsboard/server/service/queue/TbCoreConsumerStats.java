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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`TbCoreConsumerStats` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
public class TbCoreConsumerStats {
    /**
     * 字段说明：
     * 1. 保存 `TOTAL_MSGS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String SESSION_EVENTS = "sessionEvents";
    /**
     * 字段说明：
     * 1. 保存 `GET_ATTRIBUTE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String GET_ATTRIBUTE = "getAttr";
    public static final String ATTRIBUTE_SUBSCRIBES = "subToAttr";
    /**
     * 字段说明：
     * 1. 保存 `RPC_SUBSCRIBES` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String RPC_SUBSCRIBES = "subToRpc";
    public static final String TO_DEVICE_RPC_CALL_RESPONSES = "toDevRpc";
    /**
     * 字段说明：
     * 1. 保存 `SUBSCRIPTION_INFO` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String SUBSCRIPTION_INFO = "subInfo";
    public static final String DEVICE_CLAIMS = "claimDevice";
    /**
     * 字段说明：
     * 1. 保存 `DEVICE_STATES` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String DEVICE_STATES = "deviceState";
    public static final String SUBSCRIPTION_MSGS = "subMsgs";
    /**
     * 字段说明：
     * 1. 保存 `EDGE_NOTIFICATIONS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String EDGE_NOTIFICATIONS = "edgeNfs";
    public static final String DEVICE_CONNECTS = "deviceConnect";
    /**
     * 字段说明：
     * 1. 保存 `DEVICE_ACTIVITIES` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String DEVICE_ACTIVITIES = "deviceActivity";
    public static final String DEVICE_DISCONNECTS = "deviceDisconnect";
    /**
     * 字段说明：
     * 1. 保存 `DEVICE_INACTIVITIES` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String DEVICE_INACTIVITIES = "deviceInactivity";

    /**
     * 字段说明：
     * 1. 保存 `TO_CORE_NF_OTHER` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_CORE_NF_OTHER = "coreNfOther"; // normally, there is no messages when codebase is fine
    public static final String TO_CORE_NF_COMPONENT_LIFECYCLE = "coreNfCompLfcl";
    /**
     * 字段说明：
     * 1. 保存 `TO_CORE_NF_DEVICE_RPC_RESPONSE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_CORE_NF_DEVICE_RPC_RESPONSE = "coreNfDevRpcRsp";
    public static final String TO_CORE_NF_EDGE_EVENT_UPDATE = "coreNfEdgeUpd";
    /**
     * 字段说明：
     * 1. 保存 `TO_CORE_NF_EDGE_SYNC_REQUEST` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_CORE_NF_EDGE_SYNC_REQUEST = "coreNfEdgeSyncReq";
    public static final String TO_CORE_NF_EDGE_SYNC_RESPONSE = "coreNfEdgeSyncResp";
    /**
     * 字段说明：
     * 1. 保存 `TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR = "coreNfNfRlProc";
    public static final String TO_CORE_NF_QUEUE_UPDATE = "coreNfQueueUpd";
    /**
     * 字段说明：
     * 1. 保存 `TO_CORE_NF_QUEUE_DELETE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_CORE_NF_QUEUE_DELETE = "coreNfQueueDel";
    public static final String TO_CORE_NF_SUBSCRIPTION_SERVICE = "coreNfSubSvc";
    /**
     * 字段说明：
     * 1. 保存 `TO_CORE_NF_SUBSCRIPTION_MANAGER` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_CORE_NF_SUBSCRIPTION_MANAGER = "coreNfSubMgr";
    public static final String TO_CORE_NF_VC_RESPONSE = "coreNfVCRsp";

    /**
     * 字段说明：
     * 1. 保存 `totalCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter totalCounter;
    private final StatsCounter sessionEventCounter;
    /**
     * 字段说明：
     * 1. 保存 `getAttributesCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter getAttributesCounter;
    private final StatsCounter subscribeToAttributesCounter;
    /**
     * 字段说明：
     * 1. 保存 `subscribeToRPCCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter subscribeToRPCCounter;
    private final StatsCounter toDeviceRPCCallResponseCounter;
    /**
     * 字段说明：
     * 1. 保存 `subscriptionInfoCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter subscriptionInfoCounter;
    private final StatsCounter claimDeviceCounter;
    /**
     * 字段说明：
     * 1. 保存 `deviceStateCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter deviceStateCounter;
    private final StatsCounter subscriptionMsgCounter;
    /**
     * 字段说明：
     * 1. 保存 `edgeNotificationsCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter edgeNotificationsCounter;
    private final StatsCounter deviceConnectsCounter;
    /**
     * 字段说明：
     * 1. 保存 `deviceActivitiesCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter deviceActivitiesCounter;
    private final StatsCounter deviceDisconnectsCounter;
    /**
     * 字段说明：
     * 1. 保存 `deviceInactivitiesCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter deviceInactivitiesCounter;

    /**
     * 字段说明：
     * 1. 保存 `toCoreNfOtherCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter toCoreNfOtherCounter;
    private final StatsCounter toCoreNfComponentLifecycleCounter;
    /**
     * 字段说明：
     * 1. 保存 `toCoreNfDeviceRpcResponseCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter toCoreNfDeviceRpcResponseCounter;
    private final StatsCounter toCoreNfEdgeEventUpdateCounter;
    /**
     * 字段说明：
     * 1. 保存 `toCoreNfEdgeSyncRequestCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter toCoreNfEdgeSyncRequestCounter;
    private final StatsCounter toCoreNfEdgeSyncResponseCounter;
    /**
     * 字段说明：
     * 1. 保存 `toCoreNfNotificationRuleProcessorCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter toCoreNfNotificationRuleProcessorCounter;
    private final StatsCounter toCoreNfQueueUpdateCounter;
    /**
     * 字段说明：
     * 1. 保存 `toCoreNfQueueDeleteCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter toCoreNfQueueDeleteCounter;
    private final StatsCounter toCoreNfSubscriptionServiceCounter;
    /**
     * 字段说明：
     * 1. 保存 `toCoreNfSubscriptionManagerCounter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final StatsCounter toCoreNfSubscriptionManagerCounter;
    private final StatsCounter toCoreNfVersionControlResponseCounter;

    private final List<StatsCounter> counters = new ArrayList<>(24);

    /**
     * 方法说明：
     * 1. 职责：执行 `TbCoreConsumerStats` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbCoreConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.CORE.getName();

        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.sessionEventCounter = register(statsFactory.createStatsCounter(statsKey, SESSION_EVENTS));
        this.getAttributesCounter = register(statsFactory.createStatsCounter(statsKey, GET_ATTRIBUTE));
        this.subscribeToAttributesCounter = register(statsFactory.createStatsCounter(statsKey, ATTRIBUTE_SUBSCRIBES));
        this.subscribeToRPCCounter = register(statsFactory.createStatsCounter(statsKey, RPC_SUBSCRIBES));
        this.toDeviceRPCCallResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_DEVICE_RPC_CALL_RESPONSES));
        this.subscriptionInfoCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_INFO));
        this.claimDeviceCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CLAIMS));
        this.deviceStateCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_STATES));
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        this.subscriptionMsgCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_MSGS));
        this.edgeNotificationsCounter = register(statsFactory.createStatsCounter(statsKey, EDGE_NOTIFICATIONS));
        this.deviceConnectsCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CONNECTS));
        this.deviceActivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_ACTIVITIES));
        this.deviceDisconnectsCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_DISCONNECTS));
        this.deviceInactivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_INACTIVITIES));

        // Core notification counters
        this.toCoreNfOtherCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_OTHER));
        this.toCoreNfComponentLifecycleCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_COMPONENT_LIFECYCLE));
        this.toCoreNfDeviceRpcResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_DEVICE_RPC_RESPONSE));
        this.toCoreNfEdgeEventUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_EVENT_UPDATE));
        this.toCoreNfEdgeSyncRequestCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_REQUEST));
        this.toCoreNfEdgeSyncResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_RESPONSE));
        this.toCoreNfNotificationRuleProcessorCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR));
        this.toCoreNfQueueUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_QUEUE_UPDATE));
        this.toCoreNfQueueDeleteCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_QUEUE_DELETE));
        this.toCoreNfSubscriptionServiceCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_SUBSCRIPTION_SERVICE));
        this.toCoreNfSubscriptionManagerCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_SUBSCRIPTION_MANAGER));
        this.toCoreNfVersionControlResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_VC_RESPONSE));

    }

    /**
     * 方法说明：
     * 1. 职责：执行 `register` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private StatsCounter register(StatsCounter counter){
        counters.add(counter);
        return counter;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.TransportToDeviceActorMsg msg) {
        totalCounter.increment();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasSessionEvent()) {
            sessionEventCounter.increment();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasGetAttributes()) {
            getAttributesCounter.increment();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasSubscribeToAttributes()) {
            subscribeToAttributesCounter.increment();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasSubscribeToRPC()) {
            subscribeToRPCCounter.increment();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasToDeviceRPCCallResponse()) {
            toDeviceRPCCallResponseCounter.increment();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasSubscriptionInfo()) {
            subscriptionInfoCounter.increment();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasClaimDevice()) {
            claimDeviceCounter.increment();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.DeviceStateServiceMsgProto msg) {
        totalCounter.increment();
        deviceStateCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.EdgeNotificationMsgProto msg) {
        totalCounter.increment();
        edgeNotificationsCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.DeviceConnectProto msg) {
        totalCounter.increment();
        deviceConnectsCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.DeviceActivityProto msg) {
        totalCounter.increment();
        deviceActivitiesCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.DeviceDisconnectProto msg) {
        totalCounter.increment();
        deviceDisconnectsCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.DeviceInactivityProto msg) {
        totalCounter.increment();
        deviceInactivitiesCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.SubscriptionMgrMsgProto msg) {
        totalCounter.increment();
        subscriptionMsgCounter.increment();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `log` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void log(TransportProtos.ToCoreNotificationMsg msg) {
        totalCounter.increment();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.hasToLocalSubscriptionServiceMsg()) {
            toCoreNfSubscriptionServiceCounter.increment();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (msg.hasFromDeviceRpcResponse()) {
            toCoreNfDeviceRpcResponseCounter.increment();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (msg.hasComponentLifecycle()) {
            toCoreNfComponentLifecycleCounter.increment();
        } else if (!msg.getComponentLifecycleMsg().isEmpty()) {
            toCoreNfComponentLifecycleCounter.increment();
        } else if (msg.hasEdgeEventUpdate()) {
            toCoreNfEdgeEventUpdateCounter.increment();
        } else if (!msg.getEdgeEventUpdateMsg().isEmpty()) {
            toCoreNfEdgeEventUpdateCounter.increment();
        } else if (msg.hasToEdgeSyncRequest()) {
            toCoreNfEdgeSyncRequestCounter.increment();
        } else if (!msg.getToEdgeSyncRequestMsg().isEmpty()) {
            toCoreNfEdgeSyncRequestCounter.increment();
        } else if (msg.hasFromEdgeSyncResponse()) {
            toCoreNfEdgeSyncResponseCounter.increment();
        } else if (!msg.getFromEdgeSyncResponseMsg().isEmpty()) {
            toCoreNfEdgeSyncResponseCounter.increment();
        } else if (msg.getQueueUpdateMsgsCount() > 0) {
            toCoreNfQueueUpdateCounter.increment();
        } else if (msg.getQueueDeleteMsgsCount() > 0) {
            toCoreNfQueueDeleteCounter.increment();
        } else if (msg.hasVcResponseMsg()) {
            toCoreNfVersionControlResponseCounter.increment();
        } else if (msg.hasToSubscriptionMgrMsg()) {
            toCoreNfSubscriptionManagerCounter.increment();
        } else if (msg.hasNotificationRuleProcessorMsg()) {
            toCoreNfNotificationRuleProcessorCounter.increment();
        } else {
            toCoreNfOtherCounter.increment();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `printStats` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            log.info("Core Stats: {}", stats);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reset` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbCoreConsumerStats` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
