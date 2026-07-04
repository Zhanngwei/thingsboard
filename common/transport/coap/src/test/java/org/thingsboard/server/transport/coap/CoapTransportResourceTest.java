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

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.coap.client.CoapClientContext;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`CoapTransportResourceTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
class CoapTransportResourceTest {

    /**
     * 字段说明：
     * 1. 保存 `V1` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String V1 = "v1";
    private static final String API = "api";
    /**
     * 字段说明：
     * 1. 保存 `TELEMETRY` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String TELEMETRY = "telemetry";
    private static final String ATTRIBUTES = "attributes";
    /**
     * 字段说明：
     * 1. 保存 `RPC` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String RPC = "rpc";
    private static final String CLAIM = "claim";
    /**
     * 字段说明：
     * 1. 保存 `PROVISION` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String PROVISION = "provision";
    private static final String GET_ATTRIBUTES_URI_QUERY = "clientKeys=attribute1,attribute2&sharedKeys=shared1,shared2";

    private static final Random RANDOM = new Random();

    /**
     * 字段说明：
     * 1. 保存 `coapTransportResource` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static CoapTransportResource coapTransportResource;

    @BeforeAll
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    static void setUp() {

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        var ctxMock = mock(CoapTransportContext.class);
        var coapServerServiceMock = mock(CoapServerService.class);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        var transportServiceMock = mock(TransportService.class);
        var clientContextMock = mock(CoapClientContext.class);
        var schedulerComponentMock = mock(SchedulerComponent.class);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        when(ctxMock.getTransportService()).thenReturn(transportServiceMock);
        when(ctxMock.getClientContext()).thenReturn(clientContextMock);
        when(ctxMock.getSessionReportTimeout()).thenReturn(1L);
        when(ctxMock.getScheduler()).thenReturn(schedulerComponentMock);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        coapTransportResource = new CoapTransportResource(ctxMock, coapServerServiceMock, V1);
    }

    @ParameterizedTest
    @MethodSource("provideRequestAndFeatureType")
    /**
     * 方法说明：
     * 1. 职责：执行 `givenRequest_whenGetFeatureType_thenReturnedExpectedFeatureType` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenRequest_whenGetFeatureType_thenReturnedExpectedFeatureType(Request request, FeatureType expectedFeatureType) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(expectedFeatureType, featureTypeOptional.get(), "Feature type is invalid");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `provideRequestAndFeatureType` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    static Stream<Arguments> provideRequestAndFeatureType() {
        return Stream.of(
                // accessToken based tests
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, TELEMETRY), FeatureType.TELEMETRY),
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toGetAttributesAccessTokenRequest(), FeatureType.ATTRIBUTES),
                Arguments.of(toAccessTokenRequest(CoAP.Code.GET, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toAccessTokenRequest(CoAP.Code.GET, RPC), FeatureType.RPC),
                Arguments.of(toRpcResponseAccessTokenRequest(), FeatureType.RPC),
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, RPC), FeatureType.RPC),
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, CLAIM), FeatureType.CLAIM),
                // certificate based tests
                Arguments.of(toCertificateRequest(CoAP.Code.POST, TELEMETRY), FeatureType.TELEMETRY),
                Arguments.of(toCertificateRequest(CoAP.Code.POST, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toGetAttributesCertificateRequest(), FeatureType.ATTRIBUTES),
                Arguments.of(toCertificateRequest(CoAP.Code.GET, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toCertificateRequest(CoAP.Code.GET, RPC), FeatureType.RPC),
                Arguments.of(toRpcResponseCertificateRequest(), FeatureType.RPC),
                Arguments.of(toCertificateRequest(CoAP.Code.POST, RPC), FeatureType.RPC),
                Arguments.of(toCertificateRequest(CoAP.Code.POST, CLAIM), FeatureType.CLAIM),
                // provision request
                Arguments.of(toProvisionRequest(), FeatureType.PROVISION)
        );
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toAccessTokenRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toAccessTokenRequest(CoAP.Code method, String featureType) {
        return getAccessTokenRequest(method, featureType, null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toGetAttributesAccessTokenRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toGetAttributesAccessTokenRequest() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return getAccessTokenRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toRpcResponseAccessTokenRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toRpcResponseAccessTokenRequest() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return getAccessTokenRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, RANDOM.nextInt(100), null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toCertificateRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toCertificateRequest(CoAP.Code method, String featureType) {
        return getCertificateRequest(method, featureType, null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toGetAttributesCertificateRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toGetAttributesCertificateRequest() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return getCertificateRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toRpcResponseCertificateRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toRpcResponseCertificateRequest() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return getCertificateRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, RANDOM.nextInt(100), null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getAccessTokenRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request getAccessTokenRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        return getRequest(method, featureType, false, requestId, uriQuery);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCertificateRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request getCertificateRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        return getRequest(method, featureType, true, requestId, uriQuery);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toProvisionRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request toProvisionRequest() {
        return getRequest(CoAP.Code.POST, PROVISION, true, null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Request getRequest(CoAP.Code method, String featureType, boolean dtls, Integer requestId, String uriQuery) {
        var request = new Request(method);
        var options = new OptionSet();
        options.addUriPath(API);
        options.addUriPath(V1);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!dtls) {
            options.addUriPath(StringUtils.randomAlphanumeric(20));
        }
        options.addUriPath(featureType);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (requestId != null) {
            options.addUriPath(String.valueOf(requestId));
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (uriQuery != null) {
            options.setUriQuery(uriQuery);
        }
        request.setOptions(options);
        return request;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CoapTransportResourceTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
