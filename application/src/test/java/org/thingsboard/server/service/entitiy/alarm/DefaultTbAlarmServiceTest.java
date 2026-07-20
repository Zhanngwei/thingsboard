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
package org.thingsboard.server.service.entitiy.alarm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `DefaultTbAlarmServiceTest` 是 ThingsBoard Application 中验证 `DefaultTbAlarmService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmServiceTest {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @MockBean
    protected DbCallbackExecutorService dbExecutor;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbNotificationEntityService notificationEntityService;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @MockBean
    protected EdgeService edgeService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected AlarmService alarmService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbAlarmCommentService alarmCommentService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected AlarmSubscriptionService alarmSubscriptionService;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    @MockBean
    protected CustomerService customerService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbClusterService tbClusterService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    private EntitiesVersionControlService vcService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @SpyBean
    DefaultTbAlarmService service;

    /**
     * 功能：验证`Save`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSave() throws ThingsboardException {
        var alarm = new AlarmInfo();
        when(alarmSubscriptionService.createAlarm(any())).thenReturn(AlarmApiCallResult.builder()
                .successful(true)
                .modified(true)
                .alarm(alarm)
                .build());
        service.save(alarm, new User());

        verify(notificationEntityService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ADDED), any());
        verify(alarmSubscriptionService, times(1)).createAlarm(any());
    }

    /**
     * 功能：验证`Ack`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAck() throws ThingsboardException {
        var alarm = new Alarm();
        when(alarmSubscriptionService.acknowledgeAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).alarm(new AlarmInfo()).build());
        service.ack(alarm, new User(new UserId(UUID.randomUUID())));

        verify(alarmCommentService, times(1)).saveAlarmComment(any(), any(), any());
        verify(notificationEntityService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ALARM_ACK), any());
        verify(alarmSubscriptionService, times(1)).acknowledgeAlarm(any(), any(), anyLong());
    }

    /**
     * 功能：验证`Clear`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClear() throws ThingsboardException {
        var alarm = new Alarm();
        alarm.setAcknowledged(true);
        when(alarmSubscriptionService.clearAlarm(any(), any(), anyLong(), any()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).cleared(true).alarm(new AlarmInfo()).build());
        service.clear(alarm, new User(new UserId(UUID.randomUUID())));

        verify(alarmCommentService, times(1)).saveAlarmComment(any(), any(), any());
        verify(notificationEntityService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ALARM_CLEAR), any());
        verify(alarmSubscriptionService, times(1)).clearAlarm(any(), any(), anyLong(), any());
    }

    /**
     * 功能：验证`Delete`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDelete() {
        service.delete(new Alarm(), new User());

        verify(notificationEntityService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ALARM_DELETE), any());
        verify(alarmSubscriptionService, times(1)).deleteAlarm(any(), any());
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUnassignAlarm() throws ThingsboardException {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setId(new AlarmId(UUID.randomUUID()));
        when(alarmSubscriptionService.unassignAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).alarm(alarm).build());

        User user = new User();
        user.setEmail("testEmail@gmail.com");
        user.setId(new UserId(UUID.randomUUID()));
        service.unassign(new Alarm(), 0L, user);

        ObjectNode commentNode = JacksonUtil.newObjectNode();
        commentNode.put("subtype", "ASSIGN");
        commentNode.put("text", "Alarm was unassigned by user " + user.getTitle());
        commentNode.put("userId", user.getId().getId().toString());
        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(commentNode)
                .build();

        verify(alarmCommentService, times(1))
                .saveAlarmComment(eq(alarm), eq(expectedAlarmComment), eq(user));
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUnassignDeletedUserAlarms() throws ThingsboardException {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setId(new AlarmId(UUID.randomUUID()));

        when(alarmService.findAlarmIdsByAssigneeId(any(), any(), any()))
                .thenReturn(new PageData<>(List.of(alarm.getId()), 0, 1, false))
                .thenReturn(new PageData<>(Collections.EMPTY_LIST, 0, 0, false));
        when(alarmSubscriptionService.unassignAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).alarm(alarm).build());

        User user = new User();
        user.setEmail("testEmail@gmail.com");
        user.setId(new UserId(UUID.randomUUID()));
        service.unassignDeletedUserAlarms(new TenantId(UUID.randomUUID()), user, System.currentTimeMillis());

        ObjectNode commentNode = JacksonUtil.newObjectNode();
        commentNode.put("subtype", "ASSIGN");
        commentNode.put("text", String.format("Alarm was unassigned because user %s - was deleted", user.getTitle()));
        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(commentNode)
                .build();

        verify(alarmCommentService, times(1))
                .saveAlarmComment(eq(alarm), eq(expectedAlarmComment), eq(null));
    }


}
