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
 * 1. `InstallScriptsTest` 是 ThingsBoard Application 中验证 `InstallScripts` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
