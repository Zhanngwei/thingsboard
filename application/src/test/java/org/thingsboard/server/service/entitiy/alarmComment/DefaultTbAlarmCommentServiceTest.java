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
package org.thingsboard.server.service.entitiy.alarmComment;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.entitiy.alarm.DefaultTbAlarmCommentService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `DefaultTbAlarmCommentServiceTest` 是 ThingsBoard Application 中验证 `DefaultTbAlarmCommentService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmCommentService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmCommentServiceTest {

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
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected AlarmService alarmService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @MockBean
    protected AlarmCommentService alarmCommentService;
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
    @SpyBean
    DefaultTbAlarmCommentService service;

    /**
     * 功能：验证`Save`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSave() throws ThingsboardException {
        var alarm = new Alarm();
        var alarmComment = new AlarmComment();
        when(alarmCommentService.createOrUpdateAlarmComment(Mockito.any(), eq(alarmComment))).thenReturn(alarmComment);
        service.saveAlarmComment(alarm, alarmComment, new User());

        verify(notificationEntityService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ADDED_COMMENT), any(), any());
    }

    /**
     * 功能：验证`Delete`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDelete() throws ThingsboardException {
        var alarmId = new AlarmId(UUID.randomUUID());
        var alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setUserId(new UserId(UUID.randomUUID()));
        alarmComment.setType(AlarmCommentType.OTHER);

        when(alarmCommentService.saveAlarmComment(Mockito.any(), eq(alarmComment))).thenReturn(alarmComment);
        service.deleteAlarmComment(new Alarm(alarmId), alarmComment, new User());

        verify(notificationEntityService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.DELETED_COMMENT), any(), any());
    }

    /**
     * 功能：验证Actor 系统相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldNotDeleteSystemComment() {
        var alarmId = new AlarmId(UUID.randomUUID());
        var alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setType(AlarmCommentType.SYSTEM);

        assertThatThrownBy(() -> service.deleteAlarmComment(new Alarm(alarmId), alarmComment, new User()))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("System comment could not be deleted");
    }
}