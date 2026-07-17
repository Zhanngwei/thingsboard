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
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `CreateRuleChainTest` 是 ThingsBoard Microservices 中验证 `CreateRuleChain` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Create rule chain")
public class CreateRuleChainTest extends AbstractRuleChainTest {

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Add rule chain after specifying the name (text/numbers /special characters)")
    public void createRuleChain() {
        ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().click();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Add rule chain after specifying the name and description (text/numbers /special characters)")
    public void createRuleChainWithDescription() {
        ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.descriptionAddEntityView().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.setHeaderName();

        assertThat(ruleChainsPage.getHeaderName()).as("Header of rule chain details tab").isEqualTo(ruleChainName);
        assertThat(ruleChainsPage.descriptionEntityView().getAttribute("value"))
                .as("Description in rule chain details tab").isEqualTo(ruleChainName);
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Add rule chain without the name")
    public void createRuleChainWithoutName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();

        assertIsDisable(ruleChainsPage.addBtnV());
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Create rule chain only with spase in name")
    public void createRuleChainWithOnlySpace() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(" ");
        ruleChainsPage.addBtnC().click();

        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_RULE_CHAIN_MESSAGE);
        assertIsDisplayed(ruleChainsPage.addEntityView());
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Create a rule chain with the same name")
    public void createRuleChainWithSameName() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        assertThat(ruleChainsPage.entities(ruleChainName).size() > 1).
                as("More than 1 rule chains have been created").isTrue();
        ruleChainsPage.entities(ruleChainName).forEach(this::assertIsDisplayed);
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 30, groups = "smoke")
    @Description("Add rule chain after specifying the name (text/numbers /special characters) without refresh")
    public void createRuleChainWithoutRefresh() {
        ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    /**
     * 功能：执行 `documentation` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 40, groups = "smoke")
    @Description("Go to rule chain documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/rule-chains/";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.detailsBtn(ruleChainsPage.getRuleChainName()).click();
        ruleChainsPage.goToHelpPage();

        assertThat(urlContains(urlPath)).as("Redirected URL contains " + urlPath).isTrue();
    }
}
