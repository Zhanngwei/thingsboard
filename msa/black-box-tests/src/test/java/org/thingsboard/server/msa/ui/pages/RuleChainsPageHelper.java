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
package org.thingsboard.server.msa.ui.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * 中文说明：
 * 1. `RuleChainsPageHelper` 是 ThingsBoard Microservices 中处理 `Rule Chains Page` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `RuleChainsPageElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class RuleChainsPageHelper extends RuleChainsPageElements {
    /**
     * 功能：创建 `RuleChainsPageHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleChainsPageHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 功能：执行 `openCreateRuleChainView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openCreateRuleChainView() {
        plusBtn().click();
        createRuleChainBtn().click();
    }

    /**
     * 功能：执行 `openImportRuleChainView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openImportRuleChainView() {
        plusBtn().click();
        importRuleChainBtn().click();
    }

    /**
     * 功能：获取数量。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getRandomNumberFromRuleChainsCount() {
        Random random = new Random();
        return random.nextInt(notRootRuleChainsNames().size());
    }

    /**
     * 规则链，用于标识或展示当前对象。
     */
    private String ruleChainName;
    private String description;

    /**
     * 功能：更新规则链。
     * 参数：无。
     * 返回：无。
     */
    public void setRuleChainNameWithoutRoot() {
        this.ruleChainName = notRootRuleChainsNames().get(getRandomNumberFromRuleChainsCount()).getText();
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void setRuleChainNameWithoutRoot(int number) {
        this.ruleChainName = notRootRuleChainsNames().get(number).getText();
    }

    /**
     * 功能：更新描述信息。
     * 参数：无。
     * 返回：无。
     */
    public void setDescription() {
        scrollToElement(descriptionEntityView());
        this.description = descriptionEntityView().getAttribute("value");
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void setRuleChainName(int number) {
        this.ruleChainName = allNames().get(number).getText();
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getRuleChainName() {
        return this.ruleChainName;
    }

    /**
     * 功能：获取描述信息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：文本结果。
     */
    public String deleteRuleChainFromView(String ruleChainName) {
        String s = "";
        if (deleteBtnFromView() != null) {
            deleteBtnFromView().click();
            warningPopUpYesBtn().click();
            if (elementIsNotPresent(getWarningMessage())) {
                return getEntity(ruleChainName);
            }
        } else {
            for (int i = 0; i < notRootRuleChainsNames().size(); i++) {
                notRootRuleChainsNames().get(i).click();
                if (deleteBtnFromView() != null) {
                    deleteBtnFromView().click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        s = notRootRuleChainsNames().get(i).getText();
                        break;
                    }
                }
            }
        }
        return s;
    }

    /**
     * 功能：执行 `assertCheckBoxIsNotDisplayed` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：无。
     */
    public void assertCheckBoxIsNotDisplayed(String entityName) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//mat-checkbox)[2]")));
        Assert.assertFalse(driver.findElement(By.xpath(getCheckbox(entityName))).isDisplayed());
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean deleteBtnInRootRuleChainIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getDeleteRuleChainFromViewBtn())));
    }

    /**
     * 功能：执行 `assertRuleChainsIsNotPresent` 对应的处理。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：判断结果。
     */
    public boolean assertRuleChainsIsNotPresent(String ruleChainName) {
        return elementsIsNotPresent(getEntity(ruleChainName));
    }

    /**
     * 功能：执行 `sortByNameDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void sortByNameDown() {
        doubleClick(sortByNameBtn());
    }

    /**
     * `sort`列表，用于保存一组待处理对象。
     */
    ArrayList<String> sort;

    /**
     * 功能：更新`Sort`。
     * 参数：无。
     * 返回：无。
     */
    public void setSort() {
        ArrayList<String> createdTime = new ArrayList<>();
        createdTime().forEach(x -> createdTime.add(x.getText()));
        Collections.sort(createdTime);
        sort = createdTime;
    }

    /**
     * 功能：获取`Sort`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public ArrayList<String> getSort() {
        return sort;
    }
}
