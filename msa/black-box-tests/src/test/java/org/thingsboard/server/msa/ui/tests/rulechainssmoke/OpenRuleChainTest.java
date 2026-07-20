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
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `OpenRuleChainTest` 是 ThingsBoard Microservices 中验证 `OpenRuleChain` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Open rule chain")
public class OpenRuleChainTest extends AbstractRuleChainTest {

    /**
     * 功能：执行 `openRuleChainByRightCornerBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Open the rule chain by clicking on its name")
    public void openRuleChainByRightCornerBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        RuleChain ruleChain = getRuleChainByName(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(ruleChainName).click();
        openRuleChainPage.setHeadName();

        assertThat(urlContains(ruleChain.getUuidId().toString())).as("URL contains rule chain's ID").isTrue();
        assertIsDisplayed(openRuleChainPage.headRuleChainName());
        assertIsDisplayed(openRuleChainPage.inputNode());
        assertThat(openRuleChainPage.getHeadName()).as("Head of opened rule chain page text").isEqualTo(ruleChainName);
    }

    /**
     * 功能：执行 `openRuleChainByViewBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Open the rule chain by clicking on the 'Open rule chain' button in the entity view")
    public void openRuleChainByViewBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        RuleChain ruleChain = getRuleChainByName(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.openRuleChainFromViewBtn().click();
        openRuleChainPage.setHeadName();

        assertThat(ruleChain).as("Rule chain created").isNotNull();
        assertThat(urlContains(ruleChain.getUuidId().toString())).as("URL contains rule chain's ID").isTrue();
        assertIsDisplayed(openRuleChainPage.headRuleChainName());
        assertIsDisplayed(openRuleChainPage.inputNode());
        assertThat(openRuleChainPage.getHeadName()).as("Head of opened rule chain page text").isEqualTo(ruleChainName);
    }
}
