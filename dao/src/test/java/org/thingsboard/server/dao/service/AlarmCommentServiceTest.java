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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.user.UserService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.alarm.AlarmCommentType.OTHER;

/**
 * 中文说明：
 * 1. `AlarmCommentServiceTest` 是 ThingsBoard DAO 中验证 `AlarmCommentService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class AlarmCommentServiceTest extends AbstractServiceTest {

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    AlarmService alarmService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    AlarmCommentService alarmCommentService;
    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    UserService userService;

    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String TEST_ALARM = "TEST_ALARM";
    private Alarm alarm;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private User user;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
        alarm = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(new AssetId(Uuids.timeBased()))
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(System.currentTimeMillis()).build()).getAlarm();

        user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenant@thingsboard.org");
        user.setFirstName("John");
        user.setLastName("Brown");
        user = userService.saveUser(TenantId.SYS_TENANT_ID, user);
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        alarmService.delAlarm(tenantId, alarm.getId());
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAndFetchAlarmComment() throws ExecutionException, InterruptedException {
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(user.getId())
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        Assert.assertNotNull(createdComment);
        Assert.assertNotNull(createdComment.getId());

        Assert.assertEquals(alarm.getId(), createdComment.getAlarmId());
        Assert.assertEquals(user.getId(), createdComment.getUserId());
        Assert.assertEquals(OTHER, createdComment.getType());
        Assert.assertTrue(createdComment.getCreatedTime() > 0);

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertEquals(createdComment, fetched);

        PageData<AlarmCommentInfo> alarmComments = alarmCommentService.findAlarmComments(tenantId, alarm.getId(), new PageLink(10, 0));
        Assert.assertNotNull(alarmComments.getData());
        Assert.assertEquals(1, alarmComments.getData().size());
        Assert.assertEquals(createdComment, new AlarmComment(alarmComments.getData().get(0)));
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateAlarmComment() throws ExecutionException, InterruptedException {
        UserId userId = new UserId(UUID.randomUUID());
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(userId)
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        Assert.assertNotNull(createdComment);
        Assert.assertNotNull(createdComment.getId());

        //update comment
        String newComment = "new comment";
        createdComment.setComment(JacksonUtil.newObjectNode().put("text", newComment));
        AlarmComment updatedComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, createdComment);

        Assert.assertEquals(alarm.getId(), updatedComment.getAlarmId());
        Assert.assertEquals(userId, updatedComment.getUserId());
        Assert.assertEquals(OTHER, updatedComment.getType());
        Assert.assertTrue(updatedComment.getCreatedTime() > 0);
        Assert.assertEquals(newComment, updatedComment.getComment().get("text").asText());
        Assert.assertEquals("true", updatedComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedComment.getComment().get("editedOn").asText());

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertEquals(updatedComment, fetched);

        PageData<AlarmCommentInfo> alarmComments = alarmCommentService.findAlarmComments(tenantId, alarm.getId(), new PageLink(10, 0));
        Assert.assertNotNull(alarmComments.getData());
        Assert.assertEquals(1, alarmComments.getData().size());
        Assert.assertEquals(updatedComment, new AlarmComment(alarmComments.getData().get(0)));
    }

    /**
     * 功能：验证告警相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAlarmComment() throws ExecutionException, InterruptedException {
        UserId userId = new UserId(UUID.randomUUID());
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(userId)
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        createdComment.setType(AlarmCommentType.SYSTEM);
        createdComment.setUserId(null);
        alarmCommentService.saveAlarmComment(tenantId, createdComment);

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertNull(fetched.getUserId());
        Assert.assertEquals(AlarmCommentType.SYSTEM, fetched.getType());
    }
}
