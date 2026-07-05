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
     * `V1`常量，用于统一引用固定值。
     */
    private static final String V1 = "v1";
    private static final String API = "api";
    /**
     * 遥测常量，用于统一引用固定值。
     */
    private static final String TELEMETRY = "telemetry";
    private static final String ATTRIBUTES = "attributes";
    /**
     * RPC常量，用于统一引用固定值。
     */
    private static final String RPC = "rpc";
    private static final String CLAIM = "claim";
    /**
     * `PROVISION`常量，用于统一引用固定值。
     */
    private static final String PROVISION = "provision";
    private static final String GET_ATTRIBUTES_URI_QUERY = "clientKeys=attribute1,attribute2&sharedKeys=shared1,shared2";

    private static final Random RANDOM = new Random();

    /**
     * 传输层，表示当前对象的对应属性。
     */
    private static CoapTransportResource coapTransportResource;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeAll
    static void setUp() {

        var ctxMock = mock(CoapTransportContext.class);
        var coapServerServiceMock = mock(CoapServerService.class);
        var transportServiceMock = mock(TransportService.class);
        var clientContextMock = mock(CoapClientContext.class);
        var schedulerComponentMock = mock(SchedulerComponent.class);

        when(ctxMock.getTransportService()).thenReturn(transportServiceMock);
        when(ctxMock.getClientContext()).thenReturn(clientContextMock);
        when(ctxMock.getSessionReportTimeout()).thenReturn(1L);
        when(ctxMock.getScheduler()).thenReturn(schedulerComponentMock);

        coapTransportResource = new CoapTransportResource(ctxMock, coapServerServiceMock, V1);
    }

    /**
     * 功能：验证 `givenRequest_whenGetFeatureType_thenReturnedExpectedFeatureType` 描述的测试场景。
     * 参数：
     * - `request`：请求对象。
     * - `expectedFeatureType`：类型。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideRequestAndFeatureType")
    void givenRequest_whenGetFeatureType_thenReturnedExpectedFeatureType(Request request, FeatureType expectedFeatureType) {
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(expectedFeatureType, featureTypeOptional.get(), "Feature type is invalid");
    }

    /**
     * 功能：执行 `provideRequestAndFeatureType` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
     * 功能：执行 `toAccessTokenRequest` 对应的处理。
     * 参数：
     * - `method`：`method` 参数。
     * - `featureType`：类型。
     * 返回：处理结果。
     */
    private static Request toAccessTokenRequest(CoAP.Code method, String featureType) {
        return getAccessTokenRequest(method, featureType, null, null);
    }

    /**
     * 功能：执行 `toGetAttributesAccessTokenRequest` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Request toGetAttributesAccessTokenRequest() {
        return getAccessTokenRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    /**
     * 功能：执行 `toRpcResponseAccessTokenRequest` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Request toRpcResponseAccessTokenRequest() {
        return getAccessTokenRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, RANDOM.nextInt(100), null);
    }

    /**
     * 功能：执行 `toCertificateRequest` 对应的处理。
     * 参数：
     * - `method`：`method` 参数。
     * - `featureType`：类型。
     * 返回：处理结果。
     */
    private static Request toCertificateRequest(CoAP.Code method, String featureType) {
        return getCertificateRequest(method, featureType, null, null);
    }

    /**
     * 功能：执行 `toGetAttributesCertificateRequest` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Request toGetAttributesCertificateRequest() {
        return getCertificateRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    /**
     * 功能：执行 `toRpcResponseCertificateRequest` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Request toRpcResponseCertificateRequest() {
        return getCertificateRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, RANDOM.nextInt(100), null);
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `method`：`method` 参数。
     * - `featureType`：类型。
     * - `requestId`：请求ID。
     * - `uriQuery`：`uriQuery` 参数。
     * 返回：处理结果。
     */
    private static Request getAccessTokenRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        return getRequest(method, featureType, false, requestId, uriQuery);
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `method`：`method` 参数。
     * - `featureType`：类型。
     * - `requestId`：请求ID。
     * - `uriQuery`：`uriQuery` 参数。
     * 返回：处理结果。
     */
    private static Request getCertificateRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        return getRequest(method, featureType, true, requestId, uriQuery);
    }

    /**
     * 功能：执行 `toProvisionRequest` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Request toProvisionRequest() {
        return getRequest(CoAP.Code.POST, PROVISION, true, null, null);
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `method`：`method` 参数。
     * - `featureType`：类型。
     * - `dtls`：`dtls` 参数。
     * - `requestId`：请求ID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private static Request getRequest(CoAP.Code method, String featureType, boolean dtls, Integer requestId, String uriQuery) {
        var request = new Request(method);
        var options = new OptionSet();
        options.addUriPath(API);
        options.addUriPath(V1);
        if (!dtls) {
            options.addUriPath(StringUtils.randomAlphanumeric(20));
        }
        options.addUriPath(featureType);
        if (requestId != null) {
            options.addUriPath(String.valueOf(requestId));
        }
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
