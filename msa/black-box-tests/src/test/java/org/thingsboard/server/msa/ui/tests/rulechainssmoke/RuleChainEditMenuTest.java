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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. `RuleChainEditMenuTest` 是 ThingsBoard Microservices 中验证 `RuleChainEditMenu` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Edit rule chain")
public class RuleChainEditMenuTest extends AbstractRuleChainTest {

    /**
     * 功能：执行 `changeName` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String newRuleChainName = "Changed" + getRandomNumber();
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.setHeaderName();
        String nameBefore = ruleChainsPage.getHeaderName();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(newRuleChainName);
        ruleChainsPage.doneBtnEditView().click();
        ruleChainName = newRuleChainName;
        ruleChainsPage.setHeaderName();
        String nameAfter = ruleChainsPage.getHeaderName();

        assertThat(nameAfter).as("The name has changed").isNotEqualTo(nameBefore);
        assertThat(nameAfter).as("The name has changed correctly").isEqualTo(newRuleChainName);
    }

    /**
     * 功能：删除或清理名称。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu("");

        assertIsDisable(ruleChainsPage.doneBtnEditViewVisible());
    }

    /**
     * 功能：保存或创建`Only With Space`。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space")
    public void saveOnlyWithSpace() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(" ");
        ruleChainsPage.doneBtnEditView().click();

        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_RULE_CHAIN_MESSAGE);
    }

    /**
     * 功能：执行 `editDescription` 对应的处理。
     * 参数：
     * - `description`：`description` 参数。
     * - `newDescription`：`newDescription` 参数。
     * - `finalDescription`：`finalDescription` 参数。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName, description));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.descriptionEntityView().sendKeys(newDescription);
        ruleChainsPage.doneBtnEditView().click();
        ruleChainsPage.setDescription();

        assertThat(ruleChainsPage.getDescription()).as("The description changed correctly").isEqualTo(finalDescription);
    }

    /**
     * 功能：执行 `debugMode` 对应的处理。
     * 参数：
     * - `debugMode`：`debugMode` 参数。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "enable")
    @Description("Enable debug mode/Disable debug mode")
    public void debugMode(boolean debugMode) {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName, debugMode));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();

        if (debugMode) {
            assertThat(ruleChainsPage.debugCheckboxView().getAttribute("class").contains("selected"))
                    .as("Debug mode is enable").isFalse();
        } else {
            assertThat(ruleChainsPage.debugCheckboxView().getAttribute("class").contains("selected"))
                    .as("Debug mode is enable").isTrue();
        }
    }
}
