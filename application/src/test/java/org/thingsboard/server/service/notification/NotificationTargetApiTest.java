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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AllUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationTargetDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. `NotificationTargetApiTest` 是 ThingsBoard Application 中验证 `NotificationTargetApi` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractNotificationApiTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class NotificationTargetApiTest extends AbstractNotificationApiTest {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private NotificationTargetDao notificationTargetDao;

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
     * 功能：验证 `givenInvalidNotificationTarget_whenSaving_returnValidationError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInvalidNotificationTarget_whenSaving_returnValidationError() throws Exception {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(null);
        target.setName(null);
        target.setConfiguration(null);

        String validationError = saveAndGetError(target, status().isBadRequest());
        assertThat(validationError)
                .contains("name must not be")
                .contains("configuration must not be");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        UserListFilter userListFilter = new UserListFilter();
        userListFilter.setUsersIds(Collections.emptyList());
        targetConfig.setUsersFilter(userListFilter);
        target.setConfiguration(targetConfig);

        validationError = saveAndGetError(target, status().isBadRequest());
        assertThat(validationError)
                .contains("usersIds must not be");
    }

    /**
     * 功能：验证 `givenNotificationTargetWithUsersFromDifferentTenant_whenSaving_returnAccessDeniedError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNotificationTargetWithUsersFromDifferentTenant_whenSaving_returnAccessDeniedError() throws Exception {
        loginDifferentTenant();
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(differentTenantId);
        notificationTarget.setName("Target 1");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        UserListFilter userListFilter = new UserListFilter();
        userListFilter.setUsersIds(List.of(customerUserId.getId(), tenantAdminUserId.getId()));
        targetConfig.setUsersFilter(userListFilter);
        notificationTarget.setConfiguration(targetConfig);

        saveAndGetError(notificationTarget, status().isForbidden());

        loginSysAdmin();
        notificationTarget.setTenantId(TenantId.SYS_TENANT_ID);
        save(notificationTarget, status().isOk());
    }

    /**
     * 功能：验证 `givenNotificationTargetConfig_testGetRecipients` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNotificationTargetConfig_testGetRecipients() throws Exception {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setName("Test target");

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        CustomerUsersFilter customerUsersFilter = new CustomerUsersFilter();
        customerUsersFilter.setCustomerId(customerId.getId());
        targetConfig.setUsersFilter(customerUsersFilter);
        notificationTarget.setConfiguration(targetConfig);

        List<User> recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isNotZero();
        assertThat(recipients).allSatisfy(recipient -> {
            assertThat(recipient.getCustomerId()).isEqualTo(customerId);
        });

        AllUsersFilter allUsersFilter = new AllUsersFilter();
        targetConfig.setUsersFilter(allUsersFilter);
        recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isGreaterThanOrEqualTo(2);
        assertThat(recipients).allSatisfy(recipient -> {
            assertThat(recipient.getTenantId()).isEqualTo(tenantId);
        });

        createDifferentTenant();
        loginSysAdmin();
        recipients = getRecipients(notificationTarget);
        assertThat(recipients).size().isGreaterThanOrEqualTo(3);
        assertThat(recipients).anySatisfy(recipient -> {
            assertThat(recipient.getTenantId()).isEqualTo(tenantId);
        });
        assertThat(recipients).anySatisfy(recipient -> {
            assertThat(recipient.getTenantId()).isEqualTo(differentTenantId);
        });
    }

    /**
     * 功能：验证 `whenDeletingTenant_thenDeleteNotificationTarget` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenDeletingTenant_thenDeleteNotificationTarget() throws Exception {
        createDifferentTenant();
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setName("Test 1");
        notificationTarget.setTenantId(differentTenantId);
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(new AllUsersFilter());
        notificationTarget.setConfiguration(targetConfig);
        save(notificationTarget, status().isOk());
        assertThat(notificationTargetDao.findByTenantIdAndPageLink(differentTenantId, new PageLink(10)).getData()).isNotEmpty();
        assertThat(notificationTargetDao.findByTenantIdAndSupportedNotificationTypeAndPageLink(differentTenantId, NotificationType.GENERAL, new PageLink(10)).getData()).isNotEmpty();

        deleteDifferentTenant();
        assertThat(notificationTargetDao.findByTenantIdAndPageLink(differentTenantId, new PageLink(10)).getData()).isEmpty();
    }

    /**
     * 功能：验证 `whenDeletingTargetUsedByRule_thenReturnError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenDeletingTargetUsedByRule_thenReturnError() throws Exception {
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        createNotificationRule(new EntityActionNotificationRuleTriggerConfig(), "Test", "Test", target.getId());

        String error = getErrorMessage(doDelete("/api/notification/target/" + target.getId())
                .andExpect(status().isBadRequest()));
        assertThat(error).containsIgnoringCase("used in notification rule");
    }

    /**
     * 功能：验证 `whenDeletingTargetUsedByScheduledNotificationRequest_thenReturnError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenDeletingTargetUsedByScheduledNotificationRequest_thenReturnError() throws Exception {
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        submitNotificationRequest(target.getId(), "Test", 100, NotificationDeliveryMethod.WEB);

        String error = getErrorMessage(doDelete("/api/notification/target/" + target.getId())
                .andExpect(status().isBadRequest()));
        assertThat(error).containsIgnoringCase("referenced by scheduled notification request");
    }

    /**
     * 功能：保存或创建错误信息。
     * 参数：
     * - `notificationTarget`：`notificationTarget` 参数。
     * - `statusMatcher`：`statusMatcher` 参数。
     * 返回：文本结果。
     */
    private String saveAndGetError(NotificationTarget notificationTarget, ResultMatcher statusMatcher) throws Exception {
        return getErrorMessage(save(notificationTarget, statusMatcher));
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `notificationTarget`：`notificationTarget` 参数。
     * - `statusMatcher`：`statusMatcher` 参数。
     * 返回：处理结果。
     */
    private ResultActions save(NotificationTarget notificationTarget, ResultMatcher statusMatcher) throws Exception {
        return doPost("/api/notification/target", notificationTarget)
                .andExpect(statusMatcher);
    }

    /**
     * 功能：获取`Recipients`。
     * 参数：
     * - `notificationTarget`：`notificationTarget` 参数。
     * 返回：匹配的数据集合。
     */
    private List<User> getRecipients(NotificationTarget notificationTarget) throws Exception {
        return doPostWithTypedResponse("/api/notification/target/recipients?page=0&pageSize=100", notificationTarget, new TypeReference<PageData<User>>() {}).getData();
    }

}
