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
 * 1. 类目的：`AlarmCommentServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`AlarmCommentServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
