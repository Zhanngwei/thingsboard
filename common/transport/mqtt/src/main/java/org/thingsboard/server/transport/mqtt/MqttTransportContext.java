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
package org.thingsboard.server.transport.mqtt;

import io.netty.handler.ssl.SslHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 04.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`MqttTransportContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.mqtt.enabled}'=='true')")
public class MqttTransportContext extends TransportContext {

    /**
     * 处理器，用于按场景创建或提供目标对象。
     */
    @Getter
    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    /**
     * JSON，表示当前对象的对应属性。
     */
    @Getter
    @Autowired
    private JsonMqttAdaptor jsonMqttAdaptor;

    /**
     * Protobuf，表示当前对象的对应属性。
     */
    @Getter
    @Autowired
    private ProtoMqttAdaptor protoMqttAdaptor;

    /**
     * 消息载荷，保存消息正文内容。
     */
    @Getter
    @Value("${transport.mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    /**
     * 是否满足客户端条件。
     */
    @Getter
    @Value("${transport.mqtt.ssl.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Getter
    @Setter
    private SslHandler sslHandler;

    /**
     * 设备，承载当前步骤需要处理的内容。
     */
    @Getter
    @Value("${transport.mqtt.msg_queue_size_per_device_limit:100}")
    private int messageQueueSizePerDeviceLimit;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.mqtt.timeout:10000}")
    private long timeout;

    /**
     * 是否启用`proxy`。
     */
    @Getter
    @Value("${transport.mqtt.proxy_enabled:false}")
    private boolean proxyEnabled;

    private final AtomicInteger connectionsCounter = new AtomicInteger();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init();
        transportService.createGaugeStats("openConnections", connectionsCounter);
    }

    /**
     * 功能：执行 `channelRegistered` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void channelRegistered() {
        connectionsCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `channelUnregistered` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void channelUnregistered() {
        connectionsCounter.decrementAndGet();
    }

    /**
     * 功能：校验`Address`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：判断结果。
     */
    public boolean checkAddress(InetSocketAddress address) {
        return rateLimitService.checkAddress(address);
    }

    /**
     * 功能：处理`on Auth Success`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    public void onAuthSuccess(InetSocketAddress address) {
        rateLimitService.onAuthSuccess(address);
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    public void onAuthFailure(InetSocketAddress address) {
        rateLimitService.onAuthFailure(address);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MqttTransportContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
