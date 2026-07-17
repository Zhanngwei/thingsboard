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
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. `DefaultSmsServiceTest` 是 ThingsBoard Application 中验证 `DefaultSmsService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=1",
})
public class DefaultSmsServiceTest extends AbstractControllerTest {
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @SpyBean
    private DefaultSmsService defaultSmsService;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AdminSettingsService adminSettingsService;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private TenantProfile tenantProfile;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws Exception {
        loginSysAdmin();
        prepareSmsSystemSetting();
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() throws Exception {
        saveTenantProfileWitConfiguration(tenantProfile, new DefaultTenantProfileConfiguration());
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "sms");
        resetTokens();
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testLimitSmsMessagingByTenantProfileSettings() throws Exception {
        tenantProfile = getDefaultTenantProfile();

        DefaultTenantProfileConfiguration config = createTenantProfileConfigurationWithSmsLimits(10, true);
        saveTenantProfileWitConfiguration(tenantProfile, config);

        for (int i = 0; i < 10; i++) {
            doReturn(1).when(defaultSmsService).sendSms(any(), any());
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }

        //wait 1 sec so that api usage state is updated
        TimeUnit.SECONDS.sleep(1);
        assertThrows(RuntimeException.class, () -> {
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }, "SMS sending is disabled due to API limits!");
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testLimitSmsMessagingIfSmsDisabled() throws Exception {
        tenantProfile = getDefaultTenantProfile();

        DefaultTenantProfileConfiguration config = createTenantProfileConfigurationWithSmsLimits(0, false);
        saveTenantProfileWitConfiguration(tenantProfile, config);

        TimeUnit.SECONDS.sleep(1);
        assertThrows(RuntimeException.class, () -> {
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }, "SMS sending is disabled due to API limits!");

        //enable sms messaging
        DefaultTenantProfileConfiguration config2 = createTenantProfileConfigurationWithSmsLimits(0, true);
        saveTenantProfileWitConfiguration(tenantProfile, config2);
        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < 10; i++) {
            doReturn(1).when(defaultSmsService).sendSms(any(), any());
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    private TenantProfile getDefaultTenantProfile() throws Exception {

        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        List<TenantProfile> tenantProfiles = new ArrayList<>(pageData.getData());

        Optional<TenantProfile> optionalDefaultProfile = tenantProfiles.stream().filter(TenantProfile::isDefault).reduce((a, b) -> null);
        Assert.assertTrue(optionalDefaultProfile.isPresent());

        return optionalDefaultProfile.get();
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `maxSms`：`maxSms` 参数。
     * - `smsEnabled`：`smsEnabled` 参数。
     * 返回：处理结果。
     */
    private DefaultTenantProfileConfiguration createTenantProfileConfigurationWithSmsLimits(Integer maxSms, Boolean smsEnabled) {
        DefaultTenantProfileConfiguration.DefaultTenantProfileConfigurationBuilder builder = DefaultTenantProfileConfiguration.builder();
        builder.maxSms(maxSms);
        builder.smsEnabled(smsEnabled);
        return builder.build();

    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `tenantProfileConfiguration`：租户信息或租户标识。
     * 返回：无。
     */
    private void saveTenantProfileWitConfiguration(TenantProfile tenantProfile, TenantProfileConfiguration tenantProfileConfiguration) {
        TenantProfileData tenantProfileData = tenantProfile.getProfileData();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
    }

    /**
     * 功能：执行 `prepareSmsSystemSetting` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void prepareSmsSystemSetting() throws Exception {
        if (doGet("/api/admin/settings/sms").andReturn().getResponse().getStatus() == 404) {
            AdminSettings adminSettings = new AdminSettings();
            ObjectNode value = JacksonUtil.newObjectNode();
            value.put("numberFrom", "+12543223870");
            value.put("accountSid", "testAcc");
            value.put("accountToken", "testToken");
            value.put("type", "TWILIO");
            adminSettings.setKey("sms");
            adminSettings.setJsonValue(value);

            doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());
        }
    }
}