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
package org.thingsboard.rule.engine.edge;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * 测试目标：验证 {@code TbMsgPushToEdgeNodeTest} 覆盖的 边缘同步节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbMsgPushToEdgeNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPushToEdgeNodeTest {

    /** 测试常量字段：{@code MISC_EVENTS} 保存 {@code List<TbMsgType>} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final List<TbMsgType> MISC_EVENTS = List.of(TbMsgType.CONNECT_EVENT, TbMsgType.DISCONNECT_EVENT,
            /** 可变 fixture 字段：{@code TbMsgType} 保存 {@code TbMsgType.ACTIVITY_EVENT,} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
            TbMsgType.ACTIVITY_EVENT, TbMsgType.INACTIVITY_EVENT);

    /** 可变 fixture 字段：{@code node} 保存 {@code TbMsgPushToEdgeNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbMsgPushToEdgeNode node;

    /** 固定 fixture 字段：{@code tenantId} 保存 {@code TenantId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    /** 固定 fixture 字段：{@code deviceId} 保存 {@code DeviceId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctx;

    /** Mock 依赖字段：{@code edgeService} 保存 {@code EdgeService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EdgeService edgeService;
    /** Mock 依赖字段：{@code edgeEventService} 保存 {@code EdgeEventService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EdgeEventService edgeEventService;
    /** Mock 依赖字段：{@code dbCallbackExecutor} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private ListeningExecutor dbCallbackExecutor;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Before
    public void setUp() throws TbNodeException {
        node = new TbMsgPushToEdgeNode();
        TbMsgPushToEdgeNodeConfiguration config = new TbMsgPushToEdgeNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    /**
     * 测试方法：覆盖 {@code ackMsgInCaseNoEdgeRelated} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void ackMsgInCaseNoEdgeRelated() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, deviceId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(new PageData<>());

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, TbMsgMetaData.EMPTY,
                TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, null, null);

        node.onMsg(ctx, msg);

        verify(ctx).ack(msg);
    }

    /**
     * 测试方法：覆盖 {@code testAttributeUpdateMsg_userEntity} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testAttributeUpdateMsg_userEntity() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

        UserId userId = new UserId(UUID.randomUUID());
        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        PageData<EdgeId> edgePageData = new PageData<>(List.of(edgeId), 1, 1, false);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, userId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(edgePageData);

        TbMsg msg = TbMsg.newMsg(TbMsgType.ATTRIBUTES_UPDATED, userId, TbMsgMetaData.EMPTY,
                TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
    }

    /**
     * 测试方法：覆盖 {@code testMiscEventsProcessedAsAttributesUpdated} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testMiscEventsProcessedAsAttributesUpdated() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (var event : MISC_EVENTS) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue(DataConstants.SCOPE, DataConstants.SERVER_SCOPE);
            testEvent(event, metaData, EdgeEventActionType.ATTRIBUTES_UPDATED, "kv");
        }
    }

    /**
     * 测试方法：覆盖 {@code testMiscEventsProcessedAsTimeseriesUpdated} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testMiscEventsProcessedAsTimeseriesUpdated() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        for (var event : MISC_EVENTS) {
            testEvent(event, TbMsgMetaData.EMPTY, EdgeEventActionType.TIMESERIES_UPDATED, "data");
        }
    }

    /**
     * 辅助方法：{@code testEvent} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void testEvent(TbMsgType event, TbMsgMetaData metaData, EdgeEventActionType expectedType, String dataKey) {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

        TbMsg msg = TbMsg.newMsg(event, new EdgeId(UUID.randomUUID()), metaData,
                TbMsgDataType.JSON, "{\"lastConnectTs\":1}", null, null);

        node.onMsg(ctx, msg);

        ArgumentMatcher<EdgeEvent> eventArgumentMatcher = edgeEvent ->
                edgeEvent.getAction().equals(expectedType)
                        && edgeEvent.getBody().get(dataKey).get("lastConnectTs").asInt() == 1;
        verify(edgeEventService).saveAsync(Mockito.argThat(eventArgumentMatcher));

        Mockito.reset(ctx, edgeEventService);
    }
}
/*
 * 本类总结：{@code TbMsgPushToEdgeNodeTest} 为 {@code TbMsgPushToEdgeNode} 的 边缘同步节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
