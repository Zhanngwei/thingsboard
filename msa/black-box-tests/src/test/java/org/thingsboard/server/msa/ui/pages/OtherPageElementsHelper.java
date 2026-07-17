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

import org.openqa.selenium.WebDriver;

/**
 * 中文说明：
 * 1. `OtherPageElementsHelper` 是 ThingsBoard Microservices 中处理 `Other Page Elements` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `OtherPageElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class OtherPageElementsHelper extends OtherPageElements {
    /**
     * 功能：创建 `OtherPageElementsHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public OtherPageElementsHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String headerName;

    /**
     * 功能：更新名称。
     * 参数：无。
     * 返回：无。
     */
    public void setHeaderName() {
        this.headerName = headerNameView().getText();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * 功能：执行 `assertEntityIsNotPresent` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：判断结果。
     */
    public boolean assertEntityIsNotPresent(String entityName) {
        return elementIsNotPresent(getEntity(entityName));
    }

    /**
     * 功能：执行 `goToHelpPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void goToHelpPage() {
        helpBtn().click();
        goToNextTab(2);
    }

    /**
     * 功能：执行 `clickOnCheckBoxes` 对应的处理。
     * 参数：
     * - `count`：`count` 参数。
     * 返回：无。
     */
    public void clickOnCheckBoxes(int count) {
        for (int i = 0; i < count; i++) {
            checkBoxes().get(i).click();
        }
    }

    /**
     * 功能：执行 `changeNameEditMenu` 对应的处理。
     * 参数：
     * - `keysToSend`：键。
     * 返回：无。
     */
    public void changeNameEditMenu(CharSequence keysToSend) {
        nameFieldEditMenu().click();
        nameFieldEditMenu().clear();
        nameFieldEditMenu().sendKeys(keysToSend);
    }

    /**
     * 功能：执行 `changeDescription` 对应的处理。
     * 参数：
     * - `newDescription`：`newDescription` 参数。
     * 返回：无。
     */
    public void changeDescription(String newDescription) {
        descriptionEntityView().click();
        descriptionEntityView().clear();
        descriptionEntityView().sendKeys(newDescription);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：文本结果。
     */
    public String deleteRuleChainTrash(String entityName) {
        deleteBtn(entityName).click();
        warningPopUpYesBtn().click();
        return entityName;
    }

    /**
     * 功能：删除或清理`Selected`。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：文本结果。
     */
    public String deleteSelected(String entityName) {
        checkBox(entityName).click();
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
        return entityName;
    }

    /**
     * 功能：删除或清理`Selected`。
     * 参数：
     * - `countOfCheckBoxes`：`countOfCheckBoxes` 参数。
     * 返回：无。
     */
    public void deleteSelected(int countOfCheckBoxes) {
        clickOnCheckBoxes(countOfCheckBoxes);
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `namePath`：名称。
     * 返回：无。
     */
    public void searchEntity(String namePath) {
        searchBtn().click();
        searchField().sendKeys(namePath);
        sleep(0.5);
    }
}
