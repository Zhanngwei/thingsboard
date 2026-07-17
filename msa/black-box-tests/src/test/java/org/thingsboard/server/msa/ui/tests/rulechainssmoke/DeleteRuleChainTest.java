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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. `DeleteRuleChainTest` 是 ThingsBoard Microservices 中验证 `DeleteRuleChain` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Delete rule chain")
public class DeleteRuleChainTest extends AbstractRuleChainTest {

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the rule chain by clicking on the trash icon in the right side of rule chain")
    public void removeRuleChainByRightSideBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove rule chain by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRuleChain() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromRuleChainView() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ENTITY_NAME).click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainFromView(ruleChainName);
        jsClick(ruleChainsPage.refreshBtn());

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by clicking on the trash icon in the right side of rule chain")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        assertIsDisable(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME));
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove root rule chain by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromRootRuleChainView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ROOT_RULE_CHAIN_NAME).click();

        assertThat(ruleChainsPage.deleteBtnInRootRuleChainIsNotDisplayed())
                .as("Delete btn isn't displayed in details tab").isTrue();
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the rule chain with device profile by clicking on the trash icon in the right side of rule chain")
    public void removeProfileRuleChainByRightSideBtn() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteBtn(deletedRuleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(deletedRuleChain));
        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText())
                .as("Text of warning message").isEqualTo(DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain with device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedProfileRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected("Thermostat");
        ruleChainsPage.refreshBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(deletedRuleChain));
        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText())
                .as("Text of warning message").isEqualTo(DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain with device profile by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromProfileRuleChainView() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(deletedRuleChain).click();
        jsClick(ruleChainsPage.deleteBtnFromView());
        ruleChainsPage.warningPopUpYesBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(deletedRuleChain));
        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText())
                .as("Text of warning message").isEqualTo(DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove the rule chain by clicking on the trash icon in the right side of rule chain without refresh")
    public void removeRuleChainByRightSideBtnWithoutRefresh() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }
}
