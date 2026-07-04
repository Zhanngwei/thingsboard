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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * 测试目标：验证 {@code TbRestApiCallNodeTest} 覆盖的 REST 调用组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbRestApiCallNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbRestApiCallNodeTest {
	
    /** 可变 fixture 字段：{@code restNode} 保存 {@code TbRestApiCallNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbRestApiCallNode restNode;

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctx;

    /** 可变 fixture 字段：{@code originator} 保存 {@code EntityId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private EntityId originator = new DeviceId(Uuids.timeBased());
    /** 可变 fixture 字段：{@code metaData} 保存 {@code TbMsgMetaData} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsgMetaData metaData = new TbMsgMetaData();

    /** 可变 fixture 字段：{@code ruleChainId} 保存 {@code RuleChainId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    /** 可变 fixture 字段：{@code ruleNodeId} 保存 {@code RuleNodeId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

	/** 可变 fixture 字段：{@code server} 保存 {@code HttpServer} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
	private HttpServer server;

    /**
     * 辅助方法：{@code setupServer} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    public void setupServer(String pattern, HttpRequestHandler handler) throws IOException {
        SocketConfig config  = SocketConfig.custom().setSoReuseAddress(true).setTcpNoDelay(true).build();
    	server = ServerBootstrap.bootstrap()
                .setSocketConfig(config)
    			.registerHandler(pattern, handler)
    			.create();
        server.start();
    }
    
    /**
     * 辅助方法：{@code initWithConfig} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void initWithConfig(TbRestApiCallNodeConfiguration config) {
        try {
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
            restNode = new TbRestApiCallNode();
            restNode.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 生命周期方法：{@code teardown} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @After
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }
    
    /**
     * 测试方法：覆盖 {@code deleteRequestWithoutBody} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void deleteRequestWithoutBody() throws IOException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
    	setupServer("*", new HttpRequestHandler() {
			
			@Override
			public void handle(HttpRequest request, HttpResponse response, HttpContext context)
					throws HttpException, IOException {
                try {
                    assertEquals("Request path matches", request.getRequestLine().getUri(), path);
                    assertFalse("Content-Type not included", request.containsHeader("Content-Type"));
                    assertTrue("Custom header included", request.containsHeader("Foo"));
                    assertEquals("Custom header value", "Bar", request.getFirstHeader("Foo").getValue());
                    response.setStatusCode(200);
                    new Thread(new Runnable() {
                        /** 实现方法：{@code run} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch ( Exception e ) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
            }
		});

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(true);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue("Server handled request", latch.await(10, TimeUnit.SECONDS));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    /**
     * 测试方法：覆盖 {@code deleteRequestWithBody} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void deleteRequestWithBody() throws IOException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", new HttpRequestHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    assertEquals("Request path matches", path, request.getRequestLine().getUri());
                    assertTrue("Content-Type included", request.containsHeader("Content-Type"));
                    assertEquals("Content-Type value", "text/plain;charset=ISO-8859-1",
                            request.getFirstHeader("Content-Type").getValue());
                    assertTrue("Content-Length included", request.containsHeader("Content-Length"));
                    assertEquals("Content-Length value", "2",
                            request.getFirstHeader("Content-Length").getValue());
                    assertTrue("Custom header included", request.containsHeader("Foo"));
                    assertEquals("Custom header value", "Bar", request.getFirstHeader("Foo").getValue());
                    response.setStatusCode(200);
                    new Thread(new Runnable() {
                        /** 实现方法：{@code run} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch ( Exception e ) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        });

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(false);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue("Server handled request", latch.await(10, TimeUnit.SECONDS));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());
        
        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    /**
     * 测试方法：覆盖 {@code givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var defaultConfig = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        var node = new TbRestApiCallNode();
        String oldConfig = "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\":\"POST\"," +
                "\"useSimpleClientHttpFactory\":false,\"ignoreRequestBody\":false,\"enableProxy\":false," +
                "\"useSystemProxyProperties\":false,\"proxyScheme\":null,\"proxyHost\":null,\"proxyPort\":0," +
                "\"proxyUser\":null,\"proxyPassword\":null,\"readTimeoutMs\":0,\"maxParallelRequestsCount\":0," +
                "\"headers\":{\"Content-Type\":\"application/json\"},\"useRedisQueueForMsgPersistence\":false," +
                "\"trimQueue\":null,\"maxQueueSize\":null,\"credentials\":{\"type\":\"anonymous\"},\"trimDoubleQuotes\":true}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertTrue(JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()).isParseToPlainText());
    }

}
/*
 * 本类总结：{@code TbRestApiCallNodeTest} 为 {@code TbRestApiCallNode} 的 REST 调用组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
