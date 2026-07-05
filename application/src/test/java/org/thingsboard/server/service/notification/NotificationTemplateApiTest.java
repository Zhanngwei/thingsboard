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
package org.thingsboard.server.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`NotificationTemplateApiTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@DaoSqlTest
public class NotificationTemplateApiTest extends AbstractNotificationApiTest {

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    /**
     * 功能：验证 `givenInvalidNotificationTemplate_whenSaving_returnValidationError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInvalidNotificationTemplate_whenSaving_returnValidationError() throws Exception {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName(null);
        notificationTemplate.setNotificationType(null);
        notificationTemplate.setConfiguration(null);

        String validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .contains("name must not be")
                .contains("notificationType must not be")
                .contains("configuration must not be");

        NotificationTemplateConfig config = new NotificationTemplateConfig();
        notificationTemplate.setConfiguration(config);
        EmailDeliveryMethodNotificationTemplate emailTemplate = new EmailDeliveryMethodNotificationTemplate();
        emailTemplate.setEnabled(true);
        emailTemplate.setBody(null);
        emailTemplate.setSubject(null);
        config.setDeliveryMethodsTemplates(Map.of(
                NotificationDeliveryMethod.EMAIL, emailTemplate
        ));
        notificationTemplate.setName("<script/>");

        validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .contains("subject must not be")
                .contains("body must not be")
                .contains("name is malformed");
    }

    /**
     * 功能：验证`Templates Search`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testTemplatesSearch() throws Exception {
        NotificationTemplate alarmNotificationTemplate = createNotificationTemplate(NotificationType.ALARM, "Alarm", "Alarm", NotificationDeliveryMethod.WEB);
        NotificationTemplate generalNotificationTemplate = createNotificationTemplate(NotificationType.GENERAL, "General", "General", NotificationDeliveryMethod.WEB);
        NotificationTemplate entityActionNotificationTemplate = createNotificationTemplate(NotificationType.ENTITY_ACTION, "Entity action", "Entity action", NotificationDeliveryMethod.WEB);

        assertThat(findTemplates(NotificationType.ALARM)).extracting(IdBased::getId)
                .containsOnly(alarmNotificationTemplate.getId());
        assertThat(findTemplates(NotificationType.ENTITY_ACTION)).extracting(IdBased::getId)
                .containsOnly(entityActionNotificationTemplate.getId());
        assertThat(findTemplates(NotificationType.GENERAL)).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId());

        assertThat(findTemplates(NotificationType.GENERAL, NotificationType.ALARM)).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId(), alarmNotificationTemplate.getId());
        assertThat(findTemplates(NotificationType.GENERAL, NotificationType.ENTITY_ACTION)).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId(), entityActionNotificationTemplate.getId());

        assertThat(findTemplates()).extracting(IdBased::getId)
                .containsOnly(generalNotificationTemplate.getId(), alarmNotificationTemplate.getId(), entityActionNotificationTemplate.getId());
    }

    /**
     * 功能：保存或创建错误信息。
     * 参数：
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * - `statusMatcher`：`statusMatcher` 参数。
     * 返回：文本结果。
     */
    private String saveAndGetError(NotificationTemplate notificationTemplate, ResultMatcher statusMatcher) throws Exception {
        return getErrorMessage(save(notificationTemplate, statusMatcher));
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * - `statusMatcher`：`statusMatcher` 参数。
     * 返回：处理结果。
     */
    private ResultActions save(NotificationTemplate notificationTemplate, ResultMatcher statusMatcher) throws Exception {
        return doPost("/api/notification/template", notificationTemplate)
                .andExpect(statusMatcher);
    }

    /**
     * 功能：获取`Templates`。
     * 参数：
     * - `notificationTypes`：类型。
     * 返回：匹配的数据集合。
     */
    private List<NotificationTemplate> findTemplates(NotificationType... notificationTypes) throws Exception {
        PageLink pageLink = new PageLink(100, 0);
        return doGetTypedWithPageLink("/api/notification/templates?notificationTypes=" + StringUtils.join(notificationTypes, ",") + "&",
                new TypeReference<PageData<NotificationTemplate>>() {}, pageLink).getData();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationTemplateApiTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
