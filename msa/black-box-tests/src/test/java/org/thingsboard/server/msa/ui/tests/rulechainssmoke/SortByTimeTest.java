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

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. `SortByTimeTest` 是 ThingsBoard Microservices 中验证 `SortByTime` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRuleChainTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Sort rule chain by time")
public class SortByTimeTest extends AbstractRuleChainTest {

    /**
     * 功能：执行 `sortByTimeDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Sort rule chain 'DOWN'")
    public void sortByTimeDown() {
        ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(0).getText();

        assertThat(firstListElement).as("Last in list is last created").isEqualTo(lastCreated);
        assertIsDisplayed(ruleChainsPage.createdTimeEntity(ruleChainName, lastCreated));
    }

    /**
     * 功能：执行 `sortByTimeUp` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Sort rule chain 'UP'")
    public void sortByTimeUp() {
        ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByTimeBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(ruleChainsPage.createdTime().size() - 1).getText();

        assertThat(firstListElement).as("First in list is last created").isEqualTo(lastCreated);
        assertIsDisplayed(ruleChainsPage.createdTimeEntity(ruleChainName, lastCreated));
    }
}
