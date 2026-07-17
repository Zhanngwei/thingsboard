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

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

/**
 * 中文说明：
 * 1. `CreateWidgetPopupHelper` 是 ThingsBoard Microservices 中处理部件通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `CreateWidgetPopupElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class CreateWidgetPopupHelper extends CreateWidgetPopupElements {
    /**
     * 功能：创建 `CreateWidgetPopupHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public CreateWidgetPopupHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 功能：执行 `goToCreateEntityAliasPopup` 对应的处理。
     * 参数：
     * - `aliasName`：名称。
     * 返回：无。
     */
    public void goToCreateEntityAliasPopup(String aliasName) {
        entityAlias().sendKeys(aliasName + RandomStringUtils.randomAlphanumeric(7));
        createNewAliasBtn().click();
    }

    /**
     * 功能：执行 `selectFilterType` 对应的处理。
     * 参数：
     * - `filterType`：类型。
     * 返回：无。
     */
    public void selectFilterType(String filterType) {
        filterTypeFiled().click();
        optionFromDropdown(filterType).click();
    }

    /**
     * 功能：执行 `selectType` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：无。
     */
    public void selectType(String entityType) {
        typeFiled().click();
        optionFromDropdown(entityType).click();
    }

    /**
     * 功能：执行 `selectEntity` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：无。
     */
    public void selectEntity(String entityName) {
        entityFiled().sendKeys(entityName);
        entityFromDropdown(entityName);
        entityFiled().sendKeys(Keys.ARROW_DOWN);
        entityFiled().sendKeys(Keys.ENTER);
    }
}
