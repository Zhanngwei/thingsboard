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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;

import java.util.Arrays;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `OAuth2ConfigTemplateServiceTest` 是 ThingsBoard DAO 中验证 `OAuth2ConfigTemplateService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class OAuth2ConfigTemplateServiceTest extends AbstractServiceTest {

    /**
     * 配置，提供当前类调用的业务操作。
     */
    @Autowired
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    /**
     * 功能：执行 `beforeRun` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeRun() throws Exception {
        Assert.assertTrue(oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().isEmpty());
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() throws Exception {
        oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().forEach(clientRegistrationTemplate -> {
            oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(clientRegistrationTemplate.getId());
        });

        Assert.assertTrue(oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().isEmpty());
    }


    /**
     * 功能：验证提供者相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDuplicateProviderId() {
        OAuth2ClientRegistrationTemplate first = validClientRegistrationTemplate("providerId");
        OAuth2ClientRegistrationTemplate second = validClientRegistrationTemplate("providerId");
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(first);
        Assertions.assertThrows(DataValidationException.class, () -> {
            oAuth2ConfigTemplateService.saveClientRegistrationTemplate(second);
        });
    }

    /**
     * 功能：验证`Create New Template`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateNewTemplate() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = validClientRegistrationTemplate(UUID.randomUUID().toString());
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);

        Assert.assertNotNull(savedClientRegistrationTemplate);
        Assert.assertNotNull(savedClientRegistrationTemplate.getId());
        clientRegistrationTemplate.setId(savedClientRegistrationTemplate.getId());
        clientRegistrationTemplate.setCreatedTime(savedClientRegistrationTemplate.getCreatedTime());
        Assert.assertEquals(clientRegistrationTemplate, savedClientRegistrationTemplate);
    }

    /**
     * 功能：验证`Find Template`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTemplate() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = validClientRegistrationTemplate(UUID.randomUUID().toString());
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);

        OAuth2ClientRegistrationTemplate foundClientRegistrationTemplate = oAuth2ConfigTemplateService.findClientRegistrationTemplateById(savedClientRegistrationTemplate.getId());
        Assert.assertEquals(savedClientRegistrationTemplate, foundClientRegistrationTemplate);
    }

    /**
     * 功能：验证`Find All`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAll() {
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));

        Assert.assertEquals(2, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
    }

    /**
     * 功能：验证`Delete Template`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteTemplate() {
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));
        OAuth2ClientRegistrationTemplate saved = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(UUID.randomUUID().toString()));

        Assert.assertEquals(3, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
        Assert.assertNotNull(oAuth2ConfigTemplateService.findClientRegistrationTemplateById(saved.getId()));

        oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(saved.getId());

        Assert.assertEquals(2, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
        Assert.assertNull(oAuth2ConfigTemplateService.findClientRegistrationTemplateById(saved.getId()));
    }

    /**
     * 功能：执行 `validClientRegistrationTemplate` 对应的处理。
     * 参数：
     * - `providerId`：提供者ID。
     * 返回：处理结果。
     */
    private OAuth2ClientRegistrationTemplate validClientRegistrationTemplate(String providerId) {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = new OAuth2ClientRegistrationTemplate();
        clientRegistrationTemplate.setProviderId(providerId);
        clientRegistrationTemplate.setAdditionalInfo(JacksonUtil.newObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        clientRegistrationTemplate.setMapperConfig(OAuth2MapperConfig.builder()
                .type(MapperType.BASIC)
                .basic(OAuth2BasicMapperConfig.builder()
                        .firstNameAttributeKey("firstName")
                        .lastNameAttributeKey("lastName")
                        .emailAttributeKey("email")
                        .tenantNamePattern("tenant")
                        .defaultDashboardName("Test")
                        .alwaysFullScreen(true)
                        .build()
                )
                .build());
        clientRegistrationTemplate.setAuthorizationUri("authorizationUri");
        clientRegistrationTemplate.setAccessTokenUri("tokenUri");
        clientRegistrationTemplate.setScope(Arrays.asList("scope1", "scope2"));
        clientRegistrationTemplate.setUserInfoUri("userInfoUri");
        clientRegistrationTemplate.setUserNameAttributeName("userNameAttributeName");
        clientRegistrationTemplate.setJwkSetUri("jwkSetUri");
        clientRegistrationTemplate.setClientAuthenticationMethod("clientAuthenticationMethod");
        clientRegistrationTemplate.setComment("comment");
        clientRegistrationTemplate.setLoginButtonIcon("icon");
        clientRegistrationTemplate.setLoginButtonLabel("label");
        clientRegistrationTemplate.setHelpLink("helpLink");
        return clientRegistrationTemplate;
    }
}
