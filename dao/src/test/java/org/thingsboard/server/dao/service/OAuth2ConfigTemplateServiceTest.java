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
 * 1. 类目的：`OAuth2ConfigTemplateServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2ConfigTemplateServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
