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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. `SortByNameTest` 是 ThingsBoard Microservices 中验证 `SortByName` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Sort rule chain by name")
public class SortByNameTest extends AbstractRuleChainTest {

    /**
     * 功能：执行 `specialCharacterUp` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort rule chain 'UP'")
    public void specialCharacterUp(String name) {
        ruleChainName = name;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);

        assertThat(ruleChainsPage.getRuleChainName()).as("First rule chain after sort").isEqualTo(ruleChainName);
    }

    /**
     * 功能：执行 `allSortUp` 对应的处理。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * - `ruleChainSymbol`：`ruleChainSymbol` 参数。
     * - `ruleChainNumber`：`ruleChainNumber` 参数。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Sort rule chain by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort rule chain 'UP'")
    public void allSortUp(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainSymbol));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainNumber));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        deleteRuleChainByName(ruleChain);
        deleteRuleChainByName(ruleChainNumber);
        deleteRuleChainByName(ruleChainSymbol);

        assertThat(firstRuleChain).as("First rule chain with symbol in name").isEqualTo(ruleChainSymbol);
        assertThat(secondRuleChain).as("Second rule chain with number in name").isEqualTo(ruleChainNumber);
        assertThat(thirdRuleChain).as("Third rule chain with number in name").isEqualTo(ruleChain);
    }

    /**
     * 功能：执行 `specialCharacterDown` 对应的处理。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Sort rule chain by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort rule chain 'DOWN'")
    public void specialCharacterDown(String ruleChainName) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(ruleChainsPage.allNames().size() - 1);

        assertThat(ruleChainsPage.getRuleChainName()).as("Last rule chain after sort").isEqualTo(ruleChainName);
    }

    /**
     * 功能：执行 `allSortDown` 对应的处理。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * - `ruleChainSymbol`：`ruleChainSymbol` 参数。
     * - `ruleChainNumber`：`ruleChainNumber` 参数。
     * 返回：无。
     */
    @Epic("Rule chains smoke tests")
    @Feature("Sort rule chain by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort rule chain 'DOWN'")
    public void allSortDown(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainSymbol));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainNumber));

        sideBarMenuView.ruleChainsBtn().click();
        int lastIndex = ruleChainsPage.allNames().size() - 1;
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(lastIndex);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        deleteRuleChainByName(ruleChain);
        deleteRuleChainByName(ruleChainNumber);
        deleteRuleChainByName(ruleChainSymbol);

        assertThat(firstRuleChain).as("First from the end rule chain with symbol in name").isEqualTo(ruleChainSymbol);
        assertThat(secondRuleChain).as("Second from the end rule chain with number in name").isEqualTo(ruleChainNumber);
        assertThat(thirdRuleChain).as("Third rule from the end chain with number in name").isEqualTo(ruleChain);
    }
}
