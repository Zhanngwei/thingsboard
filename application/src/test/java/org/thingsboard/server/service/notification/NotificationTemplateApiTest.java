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
 * 1. `NotificationTemplateApiTest` 是 ThingsBoard Application 中验证 `NotificationTemplateApi` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractNotificationApiTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
