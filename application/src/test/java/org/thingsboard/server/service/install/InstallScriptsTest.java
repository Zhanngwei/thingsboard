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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.install.update.ImagesUpdater;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;

/**
 * 中文说明：
 * 1. 类目的：`InstallScriptsTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@SpringBootTest(classes = {InstallScripts.class, RuleChainDataValidator.class})
class InstallScriptsTest {

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @MockBean
    RuleChainService ruleChainService;
    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @MockBean
    DashboardService dashboardService;
    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @MockBean
    WidgetTypeService widgetTypeService;
    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @MockBean
    WidgetsBundleService widgetsBundleService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    OAuth2ConfigTemplateService oAuth2TemplateService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    ResourceService resourceService;
    /**
     * `imagesUpdater` 字段，保存当前对象的对应属性。
     */
    @MockBean
    ImagesUpdater imagesUpdater;
    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @SpyBean
    InstallScripts installScripts;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @MockBean
    TenantService tenantService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    ApiLimitService apiLimitService;
    /**
     * 规则链对象，用于描述当前业务场景。
     */
    @SpyBean
    RuleChainDataValidator ruleChainValidator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        willReturn(true).given(apiLimitService).checkEntitiesLimit(any(), any());
    }

    /**
     * 功能：验证`Default Rule Chains Templates`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testDefaultRuleChainsTemplates() throws IOException {
        Path dir = installScripts.getTenantRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    /**
     * 功能：验证边缘节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testDefaultEdgeRuleChainsTemplates() throws IOException {
        Path dir = installScripts.getEdgeRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testDeviceProfileDefaultRuleChainTemplate() {
        validateRuleChainTemplate(installScripts.getDeviceProfileDefaultRuleChainTemplateFilePath());
    }

    /**
     * 功能：校验规则链。
     * 参数：
     * - `templateFilePath`：文件或资源路径。
     * 返回：无。
     */
    private void validateRuleChainTemplate(Path templateFilePath) {
        log.warn("validateRuleChainTemplate {}", templateFilePath);
        JsonNode ruleChainJson = JacksonUtil.toJsonNode(templateFilePath.toFile());

        RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
        ruleChain.setTenantId(tenantId);
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        ruleChain.setId(new RuleChainId(UUID.randomUUID()));

        RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        List<Throwable> throwables = RuleChainDataValidator.validateMetaData(ruleChainMetaData);

        assertThat(throwables).as("templateFilePath " + templateFilePath)
                .containsExactlyInAnyOrderElementsOf(Collections.emptyList());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`InstallScriptsTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
