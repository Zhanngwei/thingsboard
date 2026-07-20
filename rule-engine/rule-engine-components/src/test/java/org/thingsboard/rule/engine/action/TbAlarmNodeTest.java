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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * `TbAlarmNodeTest` 测试类，用于验证 `TbAlarmNode` 相关行为。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbAlarmNodeTest {

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbAbstractAlarmNode node;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctx;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Mock
    private RuleEngineAlarmService alarmService;

    /**
     * `detailsJs` 字段，保存当前对象的对应属性。
     */
    @Mock
    private ScriptEngine detailsJs;

    /**
     * `successCaptor` 字段，保存当前对象的对应属性。
     */
    @Captor
    private ArgumentCaptor<Runnable> successCaptor;
    /**
     * 失败信息，表示当前对象的对应属性。
     */
    @Captor
    private ArgumentCaptor<Consumer<Throwable>> failureCaptor;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    private ListeningExecutor dbExecutor;

    /**
     * `originator` 字段，保存当前对象的对应属性。
     */
    private final EntityId originator = new DeviceId(Uuids.timeBased());
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private final EntityId alarmOriginator = new AlarmId(Uuids.timeBased());
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final TbMsgMetaData metaData = new TbMsgMetaData();
    /**
     * JSON，表示当前对象的对应属性。
     */
    private final String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
        dbExecutor = new TestDbCallbackExecutor();
    }

    /**
     * 功能：执行 `newAlarmCanBeCreated` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void newAlarmCanBeCreated() {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：构建`Details Throws Exception`。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void buildDetailsThrowsException() {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFailedFuture(new NotImplementedException("message")));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);

        node.onMsg(ctx, msg);

        verifyError(msg, "message", NotImplementedException.class);

        verify(ctx).createScriptEngine(ScriptLanguage.JS, "DETAILS");
        verify(ctx).getAlarmService();
        verify(ctx, times(2)).getDbCallbackExecutor();
        verify(ctx).logJsEvalRequest();
        verify(ctx).getTenantId();
        verify(alarmService).findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType");

        verifyNoMoreInteractions(ctx, alarmService);
    }

    /**
     * 功能：执行 `ifAlarmClearedCreateNew` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void ifAlarmClearedCreateNew() {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm clearedAlarm = Alarm.builder().cleared(true).acknowledged(true).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(clearedAlarm);

        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());


        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：执行 `alarmCanBeUpdated` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void alarmCanBeUpdated() {
        initWithCreateAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(activeAlarm);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.CRITICAL)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .endTs(activeAlarm.getEndTs())
                .build();
        when(alarmService.updateAlarm(any(AlarmUpdateRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .modified(true)
                        .old(new Alarm(activeAlarm))
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());
        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Updated"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_EXISTING_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertTrue(activeAlarm.getEndTs() >= oldEndDate);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：执行 `alarmCanBeCleared` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void alarmCanBeCleared() {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .cleared(true)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(activeAlarm);
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：执行 `alarmCanBeClearedWithAlarmOriginator` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void alarmCanBeClearedWithAlarmOriginator() throws ScriptException, IOException {
        initWithClearAlarmScript();
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, alarmOriginator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        long oldEndDate = System.currentTimeMillis();
        AlarmId id = new AlarmId(alarmOriginator.getId());
        Alarm activeAlarm = Alarm.builder().type("SomeType").tenantId(tenantId).originator(originator).severity(AlarmSeverity.WARNING).endTs(oldEndDate).build();
        activeAlarm.setId(id);

        Alarm expectedAlarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(originator)
                .cleared(true)
                .severity(AlarmSeverity.WARNING)
                .propagate(false)
                .type("SomeType")
                .details(null)
                .endTs(oldEndDate)
                .build();
        expectedAlarm.setId(id);

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findAlarmById(tenantId, id)).thenReturn(activeAlarm);
        when(alarmService.clearAlarm(eq(activeAlarm.getTenantId()), eq(activeAlarm.getId()), anyLong(), nullable(JsonNode.class)))
                .thenReturn(AlarmApiCallResult.builder()
                        .successful(true)
                        .cleared(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Cleared"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(alarmOriginator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_CLEARED_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAlarmWithDynamicSeverityFromMessageBody() throws Exception {
        TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setSeverity("$[alarmSeverity]");
        config.setAlarmType("SomeType");
        config.setScriptLang(ScriptLanguage.JS);
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        String rawJson = "{\"alarmSeverity\": \"WARNING\", \"passed\": 5}";
        metaData.putValue("key", "value");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("value", metadataCaptor.getValue().getValue("key"));
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAlarmWithDynamicSeverityFromMetadata() throws Exception {
        TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
        config.setPropagate(true);
        config.setScriptLang(ScriptLanguage.JS);
        config.setSeverity("${alarmSeverity}");
        config.setAlarmType("SomeType");
        config.setAlarmDetailsBuildJs("DETAILS");
        config.setDynamicSeverity(true);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAlarmService()).thenReturn(alarmService);
        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbCreateAlarmNode();
        node.init(ctx, nodeConfiguration);

        metaData.putValue("alarmSeverity", "WARNING");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        long ts = msg.getTs();
        Alarm expectedAlarm = Alarm.builder()
                .startTs(ts)
                .endTs(ts)
                .tenantId(tenantId)
                .originator(originator)
                .severity(AlarmSeverity.WARNING)
                .propagate(true)
                .type("SomeType")
                .details(null)
                .build();

        when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
        when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType")).thenReturn(null);
        when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                AlarmApiCallResult.builder()
                        .successful(true)
                        .created(true)
                        .alarm(new AlarmInfo(expectedAlarm))
                        .build());

        node.onMsg(ctx, msg);

        verify(ctx).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
        successCaptor.getValue().run();
        verify(ctx).tellNext(any(), eq("Created"));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
        assertNotSame(metaData, metadataCaptor.getValue());

        Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
        assertEquals(expectedAlarm, actualAlarm);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAlarmsWithPropagationToTenantWithDynamicTypes() throws Exception {
        for (int i = 0; i < 10; i++) {
            var config = new TbCreateAlarmNodeConfiguration();
            config.setPropagateToTenant(true);
            config.setSeverity(AlarmSeverity.CRITICAL.name());
            config.setAlarmType("SomeType" + i);
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            config.setDynamicSeverity(true);
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);

            metaData.putValue("key", "value");
            TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
            long ts = msg.getTs();
            Alarm expectedAlarm = Alarm.builder()
                    .startTs(ts)
                    .endTs(ts)
                    .tenantId(tenantId)
                    .originator(originator)
                    .severity(AlarmSeverity.CRITICAL)
                    .propagateToTenant(true)
                    .type("SomeType" + i)
                    .details(null)
                    .build();

            when(detailsJs.executeJsonAsync(msg)).thenReturn(Futures.immediateFuture(null));
            when(alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, "SomeType" + i)).thenReturn(null);
            when(alarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class))).thenReturn(
                    AlarmApiCallResult.builder()
                            .successful(true)
                            .created(true)
                            .alarm(new AlarmInfo(expectedAlarm))
                            .build());
            node.onMsg(ctx, msg);

            verify(ctx, atMost(10)).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
            successCaptor.getValue().run();
            verify(ctx, atMost(10)).tellNext(any(), eq("Created"));

            ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            ArgumentCaptor<TbMsgType> typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
            ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
            ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
            ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
            verify(ctx, atMost(10)).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

            assertEquals(TbMsgType.ALARM, typeCaptor.getValue());
            assertEquals(originator, originatorCaptor.getValue());
            assertEquals("value", metadataCaptor.getValue().getValue("key"));
            assertEquals(Boolean.TRUE.toString(), metadataCaptor.getValue().getValue(DataConstants.IS_NEW_ALARM));
            assertNotSame(metaData, metadataCaptor.getValue());

            Alarm actualAlarm = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), Alarm.class);
            assertEquals(expectedAlarm, actualAlarm);
        }
    }

    /**
     * 功能：初始化或启动告警。
     * 参数：无。
     * 返回：无。
     */
    private void initWithCreateAlarmScript() {
        try {
            TbCreateAlarmNodeConfiguration config = new TbCreateAlarmNodeConfiguration();
            config.setPropagate(true);
            config.setSeverity(AlarmSeverity.CRITICAL.name());
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbCreateAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 功能：初始化或启动告警。
     * 参数：无。
     * 返回：无。
     */
    private void initWithClearAlarmScript() {
        try {
            TbClearAlarmNodeConfiguration config = new TbClearAlarmNodeConfiguration();
            config.setAlarmType("SomeType");
            config.setScriptLang(ScriptLanguage.JS);
            config.setAlarmDetailsBuildJs("DETAILS");
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

            when(ctx.createScriptEngine(ScriptLanguage.JS, "DETAILS")).thenReturn(detailsJs);

            when(ctx.getTenantId()).thenReturn(tenantId);
            when(ctx.getAlarmService()).thenReturn(alarmService);
            when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

            node = new TbClearAlarmNode();
            node.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 功能：校验错误信息。
     * 参数：
     * - `msg`：待处理消息。
     * - `message`：待处理消息。
     * - `expectedClass`：`expectedClass` 参数。
     * 返回：无。
     */
    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }

}
