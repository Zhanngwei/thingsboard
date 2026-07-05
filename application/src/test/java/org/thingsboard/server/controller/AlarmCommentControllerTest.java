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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AlarmCommentControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@Slf4j
@ContextConfiguration(classes = {AlarmCommentControllerTest.Config.class})
@DaoSqlTest
public class AlarmCommentControllerTest extends AbstractControllerTest {

    /**
     * 客户对象，用于描述当前业务场景。
     */
    protected Device customerDevice;
    protected Alarm alarm;

    /**
     * 中文说明：
     * 1. 类目的：`Config` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
     * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 MVC Controller / Facade。
     */
    static class Config {
        /**
         * 功能：执行 `alarmDao` 对应的处理。
         * 参数：
         * - `alarmDao`：`alarmDao` 参数。
         * 返回：处理结果。
         */
        @Bean
        @Primary
        public AlarmDao alarmDao(AlarmDao alarmDao) {
            return Mockito.mock(AlarmDao.class, AdditionalAnswers.delegatesTo(alarmDao));
        }
    }

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("Type");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("test alarm type")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        resetTokens();
    }

    /**
     * 功能：执行 `teardown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void teardown() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);
        loginSysAdmin();
        deleteDifferentTenant();
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAlarmCommentViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateAlarmCommentViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());
        Assert.assertEquals(AlarmCommentType.OTHER, createdComment.getType());

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateAlarmCommentViaCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.UPDATED_COMMENT, 1, updatedAlarmComment);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED_COMMENT, 1, updatedAlarmComment);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(equalTo(msgErrorNoFound("Alarm", alarm.getId().toString()))));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    @Test
    public void testDeleteAlarmСommentViaCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(JacksonUtil.newObjectNode().put("text", String.format("User %s deleted his comment",
                        CUSTOMER_USER_EMAIL)))
                .build();
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.DELETED_COMMENT, 1, expectedAlarmComment);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(JacksonUtil.newObjectNode().put("text", String.format("User %s deleted his comment",
                        TENANT_ADMIN_EMAIL)))
                .build();
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, expectedAlarmComment);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAlarmCommentsViaCustomerUser() throws Exception {
        loginCustomerUser();

        List<AlarmComment> createdAlarmComments = new LinkedList<>();

        final int size = 10;
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.randomAlphanumeric(10))
            );
        }

        var response = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size,
                new TypeReference<PageData<AlarmCommentInfo>>() {}
        );
        var foundAlarmCommentInfos = response.getData();
        Assert.assertNotNull("Found pageData is null", foundAlarmCommentInfos);
        Assert.assertNotEquals(
                "Expected alarms are not found!",
                0, foundAlarmCommentInfos.size()
        );

        boolean allMatch = createdAlarmComments.stream()
                .allMatch(alarmComment -> foundAlarmCommentInfos.stream()
                        .map(AlarmCommentInfo::getComment)
                        .anyMatch(comment -> alarmComment.getComment().equals(comment))
                );
        Assert.assertTrue("Created alarm comment doesn't match any found!", allMatch);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAlarmsViaDifferentCustomerUser() throws Exception {
        loginCustomerUser();

        final int size = 10;
        List<AlarmComment> createdAlarmComments = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.randomAlphanumeric(10))
            );
        }

        loginDifferentCustomer();
        doGet("/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAlarmCommentsViaPublicCustomer() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setName("Test Public Device");
        device.setLabel("Label");
        device.setCustomerId(customerId);
        device = doPost("/api/device", device, Device.class);
        device = doPost("/api/customer/public/device/" + device.getUuidId(), Device.class);

        String publicId = device.getCustomerId().toString();

        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("Test")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        Mockito.reset(tbClusterService, auditLogService);
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        PageData<AlarmCommentInfo> pageData = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment" + "?page=0&pageSize=1", new TypeReference<PageData<AlarmCommentInfo>>() {}
        );

        Assert.assertNotNull("Found pageData is null", pageData);
        Assert.assertNotEquals("Expected alarms are not found!", 0, pageData.getTotalElements());

        AlarmCommentInfo alarmCommentInfo = pageData.getData().get(0);
        boolean equals = alarmComment.getId().equals(alarmCommentInfo.getId()) && alarmComment.getComment().equals(alarmCommentInfo.getComment());
        Assert.assertTrue("Created alarm doesn't match the found one!", equals);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `text`：`text` 参数。
     * 返回：处理结果。
     */
    private AlarmComment createAlarmComment(AlarmId alarmId, String text) {
        AlarmComment alarmComment = AlarmComment.builder()
                .comment(JacksonUtil.newObjectNode().set("text", new TextNode(text)))
                .build();

        return saveAlarmComment(alarmId, alarmComment);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    private AlarmComment createAlarmComment(AlarmId alarmId) {
        return createAlarmComment(alarmId, "Please take a look");
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    private AlarmComment saveAlarmComment(AlarmId alarmId, AlarmComment alarmComment) {
        alarmComment = doPost("/api/alarm/" + alarmId + "/comment", alarmComment, AlarmComment.class);
        Assert.assertNotNull(alarmComment);

        return alarmComment;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmCommentControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
