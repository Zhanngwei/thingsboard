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
package org.thingsboard.server.rules.flow;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.event.EventService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractRuleEngineFlowIntegrationTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
public abstract class AbstractRuleEngineFlowIntegrationTest extends AbstractRuleEngineControllerTest {

    /**
     * 字段说明：
     * 1. 保存 `savedTenant` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected Tenant savedTenant;
    protected User tenantAdmin;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `actorSystem` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected ActorSystemContext actorSystem;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected AttributesService attributesService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `eventService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected EventService eventService;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeTest` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void beforeTest() throws Exception {

        EventService spyEventService = spy(eventService);

        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        Mockito.doAnswer((Answer<ListenableFuture<Void>>) invocation -> {
            Object[] args = invocation.getArguments();
            Event event = (Event) args[0];
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            ListenableFuture<Void> future = eventService.saveAsync(event);
            try {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                future.get();
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {}
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            return future;
        }).when(spyEventService).saveAsync(Mockito.any(Event.class));

        ReflectionTestUtils.setField(actorSystem, "eventService", spyEventService);

        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        ruleChainService.deleteRuleChainsByTenantId(savedTenant.getId());

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `afterTest` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void afterTest() throws Exception {
        loginSysAdmin();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testRuleChainWithTwoRules` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testRuleChainWithTwoRules() throws Exception {
        // Creating Rule Chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Simple Rule Chain");
        ruleChain.setTenantId(savedTenant.getId());
        ruleChain.setRoot(true);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);
        Assert.assertNull(ruleChain.getFirstRuleNodeId());

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode1.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setFetchTo(TbMsgSource.METADATA);
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setFetchTo(TbMsgSource.METADATA);
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));


        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        metaData = saveRuleChainMetaData(metaData);
        Assert.assertNotNull(metaData);

        ruleChain = getRuleChain(ruleChain.getId());
        Assert.assertNotNull(ruleChain.getFirstRuleNodeId());

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey1", "serverAttributeValue1"), System.currentTimeMillis()))).get();
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey2", "serverAttributeValue2"), System.currentTimeMillis()))).get();

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        Mockito.when(tbMsgCallback.isMsgValid()).thenReturn(true);
        TbMsg tbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, device.getId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT, tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);
        // Pushing Message to the system
        actorSystem.tell(qMsg);
        Mockito.verify(tbMsgCallback, Mockito.timeout(10000)).onSuccess();

        PageData<EventInfo> eventsPage = getDebugEvents(savedTenant.getId(), ruleChain.getFirstRuleNodeId(), 1000);
        List<EventInfo> events = eventsPage.getData().stream().filter(filterByPostTelemetryEventType()).collect(Collectors.toList());
        Assert.assertEquals(2, events.size());

        EventInfo inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(ruleChain.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        EventInfo outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(ruleChain.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());

        RuleChain finalRuleChain = ruleChain;
        RuleNode lastRuleNode = metaData.getNodes().stream().filter(node -> !node.getId().equals(finalRuleChain.getFirstRuleNodeId())).findFirst().get();

        eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), 1000);
        events = eventsPage.getData().stream().filter(filterByPostTelemetryEventType()).collect(Collectors.toList());

        Assert.assertEquals(2, events.size());

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());
        Assert.assertEquals("serverAttributeValue2", getMetadata(outEvent).get("ss_serverAttributeKey2").asText());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTwoRuleChainsWithTwoRules` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTwoRuleChainsWithTwoRules() throws Exception {
        // Creating Rule Chain
        RuleChain rootRuleChain = new RuleChain();
        rootRuleChain.setName("Root Rule Chain");
        rootRuleChain.setTenantId(savedTenant.getId());
        rootRuleChain.setRoot(true);
        rootRuleChain.setDebugMode(true);
        rootRuleChain = saveRuleChain(rootRuleChain);
        Assert.assertNull(rootRuleChain.getFirstRuleNodeId());

        // Creating Rule Chain
        RuleChain secondaryRuleChain = new RuleChain();
        secondaryRuleChain.setName("Secondary Rule Chain");
        secondaryRuleChain.setTenantId(savedTenant.getId());
        secondaryRuleChain.setRoot(false);
        secondaryRuleChain.setDebugMode(true);
        secondaryRuleChain = saveRuleChain(secondaryRuleChain);
        Assert.assertNull(secondaryRuleChain.getFirstRuleNodeId());

        RuleChainMetaData rootMetaData = new RuleChainMetaData();
        rootMetaData.setRuleChainId(rootRuleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode1.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setFetchTo(TbMsgSource.METADATA);
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode12 = new RuleNode();
        ruleNode12.setName("Simple Rule Node 1");
        ruleNode12.setType(org.thingsboard.rule.engine.flow.TbRuleChainInputNode.class.getName());
        ruleNode12.setDebugMode(true);
        TbRuleChainInputNodeConfiguration configuration12 = new TbRuleChainInputNodeConfiguration();
        configuration12.setRuleChainId(secondaryRuleChain.getId().getId().toString());
        ruleNode12.setConfiguration(JacksonUtil.valueToTree(configuration12));

        rootMetaData.setNodes(Arrays.asList(ruleNode1, ruleNode12));
        rootMetaData.setFirstNodeIndex(0);
        NodeConnectionInfo connection = new NodeConnectionInfo();
        connection.setFromIndex(0);
        connection.setToIndex(1);
        connection.setType(TbNodeConnectionType.SUCCESS);
        rootMetaData.setConnections(Collections.singletonList(connection));
        rootMetaData = saveRuleChainMetaData(rootMetaData);
        Assert.assertNotNull(rootMetaData);

        rootRuleChain = getRuleChain(rootRuleChain.getId());
        Assert.assertNotNull(rootRuleChain.getFirstRuleNodeId());

        RuleChainMetaData secondaryMetaData = new RuleChainMetaData();
        secondaryMetaData.setRuleChainId(secondaryRuleChain.getId());

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setFetchTo(TbMsgSource.METADATA);
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        secondaryMetaData.setNodes(Collections.singletonList(ruleNode2));
        secondaryMetaData.setFirstNodeIndex(0);
        secondaryMetaData = saveRuleChainMetaData(secondaryMetaData);
        Assert.assertNotNull(secondaryMetaData);

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey1", "serverAttributeValue1"), System.currentTimeMillis()))).get();
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey2", "serverAttributeValue2"), System.currentTimeMillis()))).get();

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        Mockito.when(tbMsgCallback.isMsgValid()).thenReturn(true);
        TbMsg tbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, device.getId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT, tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);
        // Pushing Message to the system
        actorSystem.tell(qMsg);

        Mockito.verify(tbMsgCallback, Mockito.timeout(10000)).onSuccess();

        PageData<EventInfo> eventsPage = getDebugEvents(savedTenant.getId(), rootRuleChain.getFirstRuleNodeId(), 1000);
        List<EventInfo> events = eventsPage.getData().stream().filter(filterByPostTelemetryEventType()).collect(Collectors.toList());

        Assert.assertEquals(2, events.size());

        EventInfo inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(rootRuleChain.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        EventInfo outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(rootRuleChain.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());

        RuleChain finalRuleChain = rootRuleChain;
        RuleNode lastRuleNode = secondaryMetaData.getNodes().stream().filter(node -> !node.getId().equals(finalRuleChain.getFirstRuleNodeId())).findFirst().get();

        eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), 1000);
        events = eventsPage.getData().stream().filter(filterByPostTelemetryEventType()).collect(Collectors.toList());


        Assert.assertEquals(2, events.size());

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());
        Assert.assertEquals("serverAttributeValue2", getMetadata(outEvent).get("ss_serverAttributeKey2").asText());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractRuleEngineFlowIntegrationTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
