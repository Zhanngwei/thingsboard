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
package org.thingsboard.server.transport.coap;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapDtlsSessionInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.thingsboard.server.transport.coap.callback.CoapNoOpCallback;
import org.thingsboard.server.transport.coap.callback.CoapOkCallback;
import org.thingsboard.server.transport.coap.callback.GetAttributesSyncSessionCallback;
import org.thingsboard.server.transport.coap.callback.ToServerRpcSyncSessionCallback;
import org.thingsboard.server.transport.coap.client.CoapClientContext;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.californium.elements.DtlsEndpointContext.KEY_SESSION_ID;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`CoapTransportResource` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class CoapTransportResource extends AbstractCoapTransportResource {
    /**
     * 字段说明：
     * 1. 保存 `ACCESS_TOKEN_POSITION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int ACCESS_TOKEN_POSITION = 3;
    private static final int FEATURE_TYPE_POSITION = 4;
    /**
     * 字段说明：
     * 1. 保存 `REQUEST_ID_POSITION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int REQUEST_ID_POSITION = 5;

    /**
     * 字段说明：
     * 1. 保存 `FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST = 3;
    private static final int REQUEST_ID_POSITION_CERTIFICATE_REQUEST = 4;

    /**
     * 字段说明：
     * 1. 保存 `dtlsSessionsMap` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> dtlsSessionsMap;
    private final long timeout;
    /**
     * 字段说明：
     * 1. 保存 `piggybackTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final long piggybackTimeout;
    private final CoapClientContext clients;

    /**
     * 方法说明：
     * 1. 职责：执行 `CoapTransportResource` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public CoapTransportResource(CoapTransportContext ctx, CoapServerService coapServerService, String name) {
        super(ctx, name);
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
        this.dtlsSessionsMap = coapServerService.getDtlsSessionsMap();
        this.timeout = coapServerService.getTimeout();
        this.piggybackTimeout = coapServerService.getPiggybackTimeout();
        this.clients = ctx.getClientContext();
        long sessionReportTimeout = ctx.getSessionReportTimeout();
        ctx.getScheduler().scheduleAtFixedRate(clients::reportActivity, new Random().nextInt((int) sessionReportTimeout), sessionReportTimeout, TimeUnit.MILLISECONDS);
    }

    /*
     * Overwritten method from CoapResource to be able to manage our own observe notification counters.
     */
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `checkObserveRelation` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void checkObserveRelation(Exchange exchange, Response response) {
        String token = getTokenFromRequest(exchange.getRequest());
        final ObserveRelation relation = exchange.getRelation();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (relation == null || relation.isCanceled()) {
            return; // because request did not try to establish a relation
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (response.getCode().isSuccess()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!relation.isEstablished()) {
                relation.setEstablished();
                addObserveRelation(relation);
            }
            AtomicInteger state = clients.getNotificationCounterByToken(token);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (state != null) {
                response.getOptions().setObserve(state.getAndIncrement());
            } else {
                response.getOptions().removeObserve();
            }
        } // ObserveLayer takes care of the else case
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processHandleGet` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processHandleGet(CoapExchange exchange) {
        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (featureType.isEmpty()) {
            log.trace("Missing feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (featureType.get() == FeatureType.TELEMETRY) {
            log.trace("Can't fetch/subscribe to timeseries updates");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (exchange.getRequestOptions().hasObserve()) {
            processExchangeGetRequest(exchange, featureType.get());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (featureType.get() == FeatureType.ATTRIBUTES) {
            processRequest(exchange, CoapSessionMsgType.GET_ATTRIBUTES_REQUEST);
        } else {
            log.trace("Invalid feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processExchangeGetRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processExchangeGetRequest(CoapExchange exchange, FeatureType featureType) {
        boolean unsubscribe = exchange.getRequestOptions().getObserve() == 1;
        CoapSessionMsgType coapSessionMsgType;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (featureType == FeatureType.RPC) {
            coapSessionMsgType = unsubscribe ? CoapSessionMsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST : CoapSessionMsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST;
        } else {
            coapSessionMsgType = unsubscribe ? CoapSessionMsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST : CoapSessionMsgType.SUBSCRIBE_ATTRIBUTES_REQUEST;
        }
        processRequest(exchange, coapSessionMsgType);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processHandlePost` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processHandlePost(CoapExchange exchange) {
        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (featureType.isEmpty()) {
            log.trace("Missing feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else {
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (featureType.get()) {
                case ATTRIBUTES:
                    processRequest(exchange, CoapSessionMsgType.POST_ATTRIBUTES_REQUEST);
                    break;
                case TELEMETRY:
                    processRequest(exchange, CoapSessionMsgType.POST_TELEMETRY_REQUEST);
                    break;
                case RPC:
                    Optional<Integer> requestId = getRequestId(exchange.advanced().getRequest());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (requestId.isPresent()) {
                        processRequest(exchange, CoapSessionMsgType.TO_DEVICE_RPC_RESPONSE);
                    } else {
                        processRequest(exchange, CoapSessionMsgType.TO_SERVER_RPC_REQUEST);
                    }
                    break;
                case CLAIM:
                    processRequest(exchange, CoapSessionMsgType.CLAIM_REQUEST);
                    break;
                case PROVISION:
                    processProvision(exchange);
                    break;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processProvision` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processProvision(CoapExchange exchange) {
        deferAccept(exchange);
        try {
            UUID sessionId = UUID.randomUUID();
            log.trace("[{}] Processing provision publish msg [{}]!", sessionId, exchange.advanced().getRequest());
            TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg;
            TransportPayloadType payloadType;
            try {
                provisionRequestMsg = transportContext.getJsonCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                payloadType = TransportPayloadType.JSON;
            } catch (Exception e) {
                if (e instanceof JsonParseException || (e.getCause() != null && e.getCause() instanceof JsonParseException)) {
                    provisionRequestMsg = transportContext.getProtoCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                    payloadType = TransportPayloadType.PROTOBUF;
                } else {
                    throw new AdaptorException(e);
                }
            }
            transportService.process(provisionRequestMsg, new DeviceProvisionCallback(exchange, payloadType));
        } catch (AdaptorException e) {
            log.trace("Failed to decode message: ", e);
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processRequest(CoapExchange exchange, CoapSessionMsgType type) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        deferAccept(exchange);
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();

        var dtlsSessionId = request.getSourceContext().get(KEY_SESSION_ID);
        if (dtlsSessionsMap != null && dtlsSessionId != null && !dtlsSessionId.isEmpty()) {
            TbCoapDtlsSessionInfo tbCoapDtlsSessionInfo = dtlsSessionsMap
                    .computeIfPresent(request.getSourceContext().getPeerAddress(), (dtlsSessionIdStr, dtlsSessionInfo) -> {
                        dtlsSessionInfo.setLastActivityTime(System.currentTimeMillis());
                        return dtlsSessionInfo;
                    });
            if (tbCoapDtlsSessionInfo != null) {
                processRequest(exchange, type, request, tbCoapDtlsSessionInfo.getMsg(), tbCoapDtlsSessionInfo.getDeviceProfile());
            } else {
                processAccessTokenRequest(exchange, type, request);
            }
        } else {
            processAccessTokenRequest(exchange, type, request);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processAccessTokenRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processAccessTokenRequest(CoapExchange exchange, CoapSessionMsgType type, Request request) {
        Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (credentials.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new CoapDeviceAuthCallback(exchange, (deviceCredentials, deviceProfile) -> processRequest(exchange, type, request, deviceCredentials, deviceProfile)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processRequest(CoapExchange exchange, CoapSessionMsgType type, Request request, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) {
        TbCoapClientState clientState = null;
        try {
            clientState = clients.getOrCreateClient(type, deviceCredentials, deviceProfile);
            clients.awake(clientState);
            switch (type) {
                case POST_ATTRIBUTES_REQUEST:
                    handlePostAttributesRequest(clientState, exchange, request);
                    break;
                case POST_TELEMETRY_REQUEST:
                    handlePostTelemetryRequest(clientState, exchange, request);
                    break;
                case CLAIM_REQUEST:
                    handleClaimRequest(clientState, exchange, request);
                    break;
                case SUBSCRIBE_ATTRIBUTES_REQUEST:
                    handleAttributeSubscribeRequest(clientState, exchange, request);
                    break;
                case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                    handleAttributeUnsubscribeRequest(clientState, exchange, request);
                    break;
                case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                    handleRpcSubscribeRequest(clientState, exchange, request);
                    break;
                case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                    handleRpcUnsubscribeRequest(clientState, exchange, request);
                    break;
                case TO_DEVICE_RPC_RESPONSE:
                    handleToDeviceRpcResponse(clientState, exchange, request);
                    break;
                case TO_SERVER_RPC_REQUEST:
                    handleToServerRpcRequest(clientState, exchange, request);
                    break;
                case GET_ATTRIBUTES_REQUEST:
                    handleGetAttributesRequest(clientState, exchange, request);
                    break;
            }
        } catch (AdaptorException e) {
            if (clientState != null) {
                log.trace("[{}] Failed to decode message: ", clientState.getDeviceId(), e);
            }
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handlePostAttributesRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handlePostAttributesRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo, clientState.getAdaptor().convertToPostAttributes(sessionId, request,
                clientState.getConfiguration().getAttributesMsgDescriptor()),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handlePostTelemetryRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handlePostTelemetryRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo, clientState.getAdaptor().convertToPostTelemetry(sessionId, request,
                clientState.getConfiguration().getTelemetryMsgDescriptor()),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleClaimRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleClaimRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToClaimDevice(sessionId, request, sessionInfo),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAttributeSubscribeRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleAttributeSubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        String attrSubToken = getTokenFromRequest(request);
        if (!clients.registerAttributeObservation(clientState, attrSubToken, exchange)) {
            log.warn("[{}] Received duplicate attribute subscribe request for token: {}", clientState.getDeviceId(), attrSubToken);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAttributeUnsubscribeRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleAttributeUnsubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        clients.deregisterAttributeObservation(clientState, getTokenFromRequest(request), exchange);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleRpcUnsubscribeRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleRpcUnsubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        clients.deregisterRpcObservation(clientState, getTokenFromRequest(request), exchange);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleToDeviceRpcResponse` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleToDeviceRpcResponse(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto session = clientState.getSession();
        if (session == null) {
            session = clients.getNewSyncSession(clientState);
        }
        UUID sessionId = toSessionId(session);
        transportService.process(session,
                clientState.getAdaptor().convertToDeviceRpcResponse(sessionId, request, clientState.getConfiguration().getRpcResponseMsgDescriptor()),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleRpcSubscribeRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleRpcSubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        String rpcSubToken = getTokenFromRequest(request);
        if (!clients.registerRpcObservation(clientState, rpcSubToken, exchange)) {
            log.warn("[{}] Received duplicate rpc subscribe request.", rpcSubToken);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleGetAttributesRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleGetAttributesRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.registerSyncSession(sessionInfo, new GetAttributesSyncSessionCallback(clientState, exchange, request), timeout);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToGetAttributes(sessionId, request),
                new CoapNoOpCallback(exchange));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleToServerRpcRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleToServerRpcRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.registerSyncSession(sessionInfo, new ToServerRpcSyncSessionCallback(clientState, exchange, request), timeout);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToServerRpcRequest(sessionId, request),
                new CoapNoOpCallback(exchange));
    }

    /**
     * Send an empty ACK if we are unable to send the full response within the timeout.
     * If the full response is transmitted before the timeout this will not do anything.
     * If this is triggered the full response will be sent in a separate CON/NON message.
     * Essentially this allows the use of piggybacked responses.
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `deferAccept` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void deferAccept(CoapExchange exchange) {
        if (piggybackTimeout > 0) {
            transportContext.getScheduler().schedule(exchange::accept, piggybackTimeout, TimeUnit.MILLISECONDS);
        } else {
            exchange.accept();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toSessionId` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private UUID toSessionId(TransportProtos.SessionInfoProto sessionInfoProto) {
        return new UUID(sessionInfoProto.getSessionIdMSB(), sessionInfoProto.getSessionIdLSB());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTokenFromRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getTokenFromRequest(Request request) {
        return (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getAddress().getHostAddress() : "null")
                + ":" + (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getPort() : -1) + ":" + request.getTokenString();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `decodeCredentials` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() > ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getFeatureType` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Optional<FeatureType> getFeatureType(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            int size = uriPath.size();
            if (size >= FEATURE_TYPE_POSITION) {
                if (size == FEATURE_TYPE_POSITION && StringUtils.isNumeric(uriPath.get(size - 1))) {
                    return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 2).toUpperCase()));
                }
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 1).toUpperCase()));
            } else if (size == FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST) {
                if (uriPath.contains(DataConstants.PROVISION)) {
                    return Optional.of(FeatureType.valueOf(DataConstants.PROVISION.toUpperCase()));
                }
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST - 1).toUpperCase()));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRequestId` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Integer> getRequestId(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            if (uriPath.size() >= REQUEST_ID_POSITION) {
                return Optional.of(Integer.valueOf(uriPath.get(REQUEST_ID_POSITION - 1)));
            } else {
                return Optional.of(Integer.valueOf(uriPath.get(REQUEST_ID_POSITION_CERTIFICATE_REQUEST - 1)));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getChild` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Resource getChild(String name) {
        return this;
    }

    /**
     * 中文说明：
     * 1. 类目的：`DeviceProvisionCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class DeviceProvisionCallback implements TransportServiceCallback<TransportProtos.ProvisionDeviceResponseMsg> {
        /**
         * 字段说明：
         * 1. 保存 `exchange` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final CoapExchange exchange;
        private final TransportPayloadType payloadType;

        DeviceProvisionCallback(CoapExchange exchange, TransportPayloadType payloadType) {
            this.exchange = exchange;
            this.payloadType = payloadType;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onSuccess` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onSuccess(TransportProtos.ProvisionDeviceResponseMsg msg) {
            CoAP.ResponseCode responseCode = CoAP.ResponseCode.CREATED;
            if (!msg.getStatus().equals(TransportProtos.ResponseStatus.SUCCESS)) {
                responseCode = CoAP.ResponseCode.BAD_REQUEST;
            }
            if (payloadType.equals(TransportPayloadType.JSON)) {
                exchange.respond(responseCode, JsonConverter.toJson(msg).toString());
            } else {
                exchange.respond(responseCode, msg.toByteArray());
            }
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onError` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`CoapResourceObserver` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    public class CoapResourceObserver implements ResourceObserver {

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `changedName` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void changedName(String old) {
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `changedPath` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void changedPath(String old) {
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `addedChild` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void addedChild(Resource child) {
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `removedChild` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void removedChild(Resource child) {
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `addedObserveRelation` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void addedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String token = getTokenFromRequest(request);
            clients.registerObserveRelation(token, relation);
            log.trace("Added Observe relation for token: {}", token);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `removedObserveRelation` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void removedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String token = getTokenFromRequest(request);
            clients.deregisterObserveRelation(token);
            log.trace("Relation removed for token: {}", token);
        }
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`CoapTransportResource` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
