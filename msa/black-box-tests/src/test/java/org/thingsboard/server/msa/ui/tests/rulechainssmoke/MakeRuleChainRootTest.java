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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `MakeRuleChainRootTest` 是 ThingsBoard Microservices 中验证 `MakeRuleChainRoot` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Make rule chain root")
public class MakeRuleChainRootTest extends AbstractRuleChainTest {

    /**
     * 功能：执行 `makeRoot` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void makeRoot() {
        setRootRuleChain("Root Rule Chain");
    }

    /**
     * 功能：执行 `makeRuleChainRootByRightCornerBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Make rule chain root by clicking on the 'Make rule chain root' icon in the right corner")
    public void makeRuleChainRootByRightCornerBtn() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.makeRootBtn(ruleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();

        assertIsDisplayed(ruleChainsPage.rootCheckBoxEnable(ruleChain));
    }

    /**
     * 功能：执行 `makeRuleChainRootFromView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Make rule chain root by clicking on the 'Make rule chain root' button in the entity view")
    public void makeRuleChainRootFromView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.detailsBtn(ruleChain).click();
        ruleChainsPage.makeRootFromViewBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        assertIsDisplayed(ruleChainsPage.rootCheckBoxEnable(ruleChain));
    }

    /**
     * 功能：执行 `multiplyRoot` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 30, groups = "smoke")
    @Description("Make multiple root rule chains (only one rule chain can be root)")
    public void multiplyRoot() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.detailsBtn(ruleChain).click();
        jsClick(ruleChainsPage.makeRootFromViewBtn());
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        assertThat(ruleChainsPage.rootCheckBoxesEnable()).as("Enable only 1 root checkbox").hasSize(1);
    }
}
