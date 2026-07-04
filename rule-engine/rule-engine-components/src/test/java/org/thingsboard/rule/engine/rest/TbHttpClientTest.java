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
package org.thingsboard.rule.engine.rest;


import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.AsyncRestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * 测试目标：验证 {@code TbHttpClientTest} 覆盖的 REST 调用组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbHttpClient}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class TbHttpClientTest {

    /** 可变 fixture 字段：{@code eventLoop} 保存 {@code EventLoopGroup} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    EventLoopGroup eventLoop;
    /** 可变 fixture 字段：{@code client} 保存 {@code TbHttpClient} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbHttpClient client;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void setUp() throws Exception {
        client = mock(TbHttpClient.class);
        when(client.getSharedOrCreateEventLoopGroup(any())).thenCallRealMethod();
    }

    /**
     * 生命周期方法：{@code tearDown} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (eventLoop != null) {
            eventLoop.shutdownGracefully();
        }
    }

    /**
     * 测试方法：覆盖 {@code givenSharedEventLoop_whenGetEventLoop_ThenReturnShared} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenSharedEventLoop_whenGetEventLoop_ThenReturnShared() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        eventLoop = mock(EventLoopGroup.class);
        assertThat(client.getSharedOrCreateEventLoopGroup(eventLoop), is(eventLoop));
    }

    /**
     * 测试方法：覆盖 {@code givenNull_whenGetEventLoop_ThenReturnShared} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenNull_whenGetEventLoop_ThenReturnShared() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        eventLoop = client.getSharedOrCreateEventLoopGroup(null);
        assertThat(eventLoop, instanceOf(NioEventLoopGroup.class));
    }

    /**
     * 测试方法：覆盖 {@code testBuildSimpleUri} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testBuildSimpleUri() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Mockito.when(client.buildEncodedUri(any())).thenCallRealMethod();
        String url = "http://localhost:8080/";
        URI uri = client.buildEncodedUri(url);
        Assertions.assertEquals(url, uri.toString());
    }

    /**
     * 测试方法：覆盖 {@code testBuildUriWithoutProtocol} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testBuildUriWithoutProtocol() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Mockito.when(client.buildEncodedUri(any())).thenCallRealMethod();
        String url = "localhost:8080/";
        assertThatThrownBy(() -> client.buildEncodedUri(url));
    }

    /**
     * 测试方法：覆盖 {@code testBuildInvalidUri} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testBuildInvalidUri() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Mockito.when(client.buildEncodedUri(any())).thenCallRealMethod();
        String url = "aaa";
        assertThatThrownBy(() -> client.buildEncodedUri(url));
    }

    /**
     * 测试方法：覆盖 {@code testBuildUriWithSpecialSymbols} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testBuildUriWithSpecialSymbols() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Mockito.when(client.buildEncodedUri(any())).thenCallRealMethod();
        String url = "http://192.168.1.1/data?d={\"a\": 12}";
        String expected = "http://192.168.1.1/data?d=%7B%22a%22:%2012%7D";
        URI uri = client.buildEncodedUri(url);
        Assertions.assertEquals(expected, uri.toString());
    }

    /**
     * 测试方法：覆盖 {@code testProcessMessageWithJsonInUrlVariable} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testProcessMessageWithJsonInUrlVariable() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        String host = "localhost";
        String path = "/api";
        String paramKey = "data";
        String paramVal = "[{\"test\":\"test\"}]";
        String successResponseBody = "SUCCESS";

        var server = setUpDummyServer(host, path, paramKey, paramVal, successResponseBody);

        String endpointUrl = String.format(
                "http://%s:%d%s?%s=%s",
                host, server.getPort(), path, paramKey, paramVal
        );
        String method = "GET";


        var config = new TbRestApiCallNodeConfiguration()
                .defaultConfiguration();
        config.setRequestMethod(method);
        config.setRestEndpointUrlPattern(endpointUrl);
        config.setUseSimpleClientHttpFactory(true);

        var asyncRestTemplate = new AsyncRestTemplate();

        var httpClient = new TbHttpClient(config, eventLoop);
        httpClient.setHttpClient(asyncRestTemplate);

        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, new DeviceId(EntityId.NULL_UUID), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        var successMsg = TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, msg.getOriginator(),
                msg.getMetaData(), msg.getData()
        );

        var ctx = mock(TbContext.class);
        when(ctx.transformMsg(
                eq(msg),
                eq(msg.getMetaData()),
                eq(msg.getData())
        )).thenReturn(successMsg);

        var capturedData = ArgumentCaptor.forClass(String.class);

        when(ctx.transformMsg(
                eq(msg),
                any(),
                capturedData.capture()
        )).thenReturn(successMsg);

        CountDownLatch latch = new CountDownLatch(1);

        httpClient.processMessage(ctx, msg,
                m -> {
                    ctx.tellSuccess(msg);
                    latch.countDown();
                },
                (m, t) -> {
                    ctx.tellFailure(m, t);
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);

        verify(ctx, times(1)).tellSuccess(any());
        verify(ctx, times(0)).tellFailure(any(), any());
        Assertions.assertEquals(successResponseBody, capturedData.getValue());
    }

    /**
     * 辅助方法：{@code setUpDummyServer} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private ClientAndServer setUpDummyServer(String host, String path, String paramKey, String paramVal, String successResponseBody) {
        var server = startClientAndServer(host, 1080);
        createGetMethodExpectations(server, path, paramKey, paramVal, successResponseBody);
        return server;
    }

    /**
     * 辅助方法：{@code createGetMethodExpectations} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void createGetMethodExpectations(ClientAndServer server, String path, String paramKey, String paramVal, String successResponseBody) {
        server.when(
                request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter(paramKey, paramVal)
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(successResponseBody)
        );
    }

    /**
     * 测试方法：覆盖 {@code testHeadersToMetaData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testHeadersToMetaData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Map<String, List<String>> headers = new LinkedMultiValueMap<>();
        headers.put("Content-Type", List.of("binary"));
        headers.put("Set-Cookie", List.of("sap-context=sap-client=075; path=/", "sap-token=sap-client=075; path=/"));

        TbMsgMetaData metaData = new TbMsgMetaData();

        willCallRealMethod().given(client).headersToMetaData(any(), any());

        client.headersToMetaData(headers, metaData::putValue);

        Map<String, String> data = metaData.getData();

        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals(data.get("Content-Type"), "binary");
        Assertions.assertEquals(data.get("Set-Cookie"), "[\"sap-context=sap-client=075; path=/\",\"sap-token=sap-client=075; path=/\"]");
    }

    @ParameterizedTest
    @ValueSource(strings = { "false", "\"", "\"\"", "\"This is a string with double quotes\"", "Path: /home/developer/test.txt",
            "First line\nSecond line\n\nFourth line", "Before\rAfter", "Tab\tSeparated\tValues", "Test\bbackspace", "[]",
            "[1, 2, 3]", "{\"key\": \"value\"}", "{\n\"temperature\": 25.5,\n\"humidity\": 50.2\n\"}", "Expression: (a + b) * c",
            "世界", "Україна", "\u1F1FA\u1F1E6", "🇺🇦"})
    /**
     * 辅助方法：{@code testParseJsonStringToPlainText} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    public void testParseJsonStringToPlainText(String original) {
        Mockito.when(client.parseJsonStringToPlainText(anyString())).thenCallRealMethod();

        String serialized = JacksonUtil.toString(original);
        Assertions.assertNotNull(serialized);
        Assertions.assertEquals(original, client.parseJsonStringToPlainText(serialized));
    }
}
/*
 * 本类总结：{@code TbHttpClientTest} 为 {@code TbHttpClient} 的 REST 调用组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
