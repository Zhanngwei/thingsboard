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
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. `CreateRuleChainImportTest` 是 ThingsBoard Microservices 中验证 `CreateRuleChainImport` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Import rule chain")
public class CreateRuleChainImportTest extends AbstractRuleChainTest {

    private final String absolutePathToFileImportRuleChain = getClass().getClassLoader().getResource(IMPORT_RULE_CHAIN_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();

    /**
     * 功能：执行 `importRuleChain` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Drop json file")
    public void importRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);

        assertIsDisplayed(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME));
    }

    /**
     * 功能：执行 `importRuleChainAndDeleteFile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Drop json file and delete it")
    public void importRuleChainAndDeleteFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.clearImportFileBtn().click();

        assertIsDisplayed(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        ruleChainsPage.assertEntityIsNotPresent(IMPORT_RULE_CHAIN_FILE_NAME);
    }

    /**
     * 功能：执行 `importTxtFile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Import txt file")
    public void importTxtFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        assertIsDisplayed(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
    }

    /**
     * 功能：执行 `importRuleChainAndSave` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 30, groups = "smoke")
    @Description("Import rule chain")
    public void importRuleChainAndSave() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        WebElement doneBtn = openRuleChainPage.doneBtn();
        doneBtn.click();
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        sideBarMenuView.ruleChainsBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    /**
     * 功能：执行 `importRuleChainAndSaveWithSameName` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 40, groups = "smoke")
    @Description("Import rule chain with same name")
    public void importRuleChainAndSaveWithSameName() {
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        WebElement doneBtn = openRuleChainPage.doneBtn();
        doneBtn.click();
        sideBarMenuView.ruleChainsBtn().click();

        assertThat(ruleChainsPage.entities(ruleChainName).size() > 1).
                as("More than 1 rule chains have been created").isTrue();
        ruleChainsPage.entities(ruleChainName).forEach(this::assertIsDisplayed);
    }
}
