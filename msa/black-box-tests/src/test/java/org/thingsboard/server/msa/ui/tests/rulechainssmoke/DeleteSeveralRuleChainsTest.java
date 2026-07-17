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

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. `DeleteSeveralRuleChainsTest` 是 ThingsBoard Microservices 中验证 `DeleteSeveralRuleChains` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Delete several rule chains")
public class DeleteSeveralRuleChainsTest extends AbstractRuleChainTest {

    /**
     * 功能：执行 `canDeleteSeveralRuleChainsByTopBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Remove several rule chains by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void canDeleteSeveralRuleChainsByTopBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteSelected(2);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }

    /**
     * 功能：执行 `selectAllRuleChain` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Remove several rule chains by mark all the rule chains on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void selectAllRuleChain() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();
        jsClick(ruleChainsPage.deleteSelectedBtn());
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        assertIsDisable(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME));
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by mark all the rule chains on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    /**
     * 功能：删除或清理`Several Rule Chains By Top Btn Without Refresh`。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 30, groups = "smoke")
    @Description("Remove several rule chains by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top without refresh")
    public void deleteSeveralRuleChainsByTopBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteSelected(2);

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }
}
