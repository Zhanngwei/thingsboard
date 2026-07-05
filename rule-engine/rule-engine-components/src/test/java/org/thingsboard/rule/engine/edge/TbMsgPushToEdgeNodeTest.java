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
 * `TbMsgPushToEdgeNodeTest` 测试类，用于验证 `TbMsgPushToEdgeNode` 相关行为。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPushToEdgeNodeTest {

    /**
     * `MISC_EVENTS`常量，用于统一引用固定值。
     */
    private static final List<TbMsgType> MISC_EVENTS = List.of(TbMsgType.CONNECT_EVENT, TbMsgType.DISCONNECT_EVENT,
            /**
             * 消息，用于区分不同处理分支。
             */
            TbMsgType.ACTIVITY_EVENT, TbMsgType.INACTIVITY_EVENT);

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    TbMsgPushToEdgeNode node;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctx;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Mock
    private EdgeService edgeService;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Mock
    private EdgeEventService edgeEventService;
    /**
     * 回调列表，用于保存一组待处理对象。
     */
    @Mock
    private ListeningExecutor dbCallbackExecutor;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws TbNodeException {
        node = new TbMsgPushToEdgeNode();
        TbMsgPushToEdgeNodeConfiguration config = new TbMsgPushToEdgeNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    /**
     * 功能：执行 `ackMsgInCaseNoEdgeRelated` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void ackMsgInCaseNoEdgeRelated() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, deviceId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(new PageData<>());

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, TbMsgMetaData.EMPTY,
                TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, null, null);

        node.onMsg(ctx, msg);

        verify(ctx).ack(msg);
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAttributeUpdateMsg_userEntity() {
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
     * 功能：验证`Misc Events Processed As Attributes Updated`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testMiscEventsProcessedAsAttributesUpdated() {
        for (var event : MISC_EVENTS) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue(DataConstants.SCOPE, DataConstants.SERVER_SCOPE);
            testEvent(event, metaData, EdgeEventActionType.ATTRIBUTES_UPDATED, "kv");
        }
    }

    /**
     * 功能：验证时序数据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testMiscEventsProcessedAsTimeseriesUpdated() {
        for (var event : MISC_EVENTS) {
            testEvent(event, TbMsgMetaData.EMPTY, EdgeEventActionType.TIMESERIES_UPDATED, "data");
        }
    }

    /**
     * 功能：验证事件相关场景。
     * 参数：
     * - `event`：`event` 参数。
     * - `metaData`：待处理数据。
     * - `expectedType`：类型。
     * - `dataKey`：待处理数据。
     * 返回：无。
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
