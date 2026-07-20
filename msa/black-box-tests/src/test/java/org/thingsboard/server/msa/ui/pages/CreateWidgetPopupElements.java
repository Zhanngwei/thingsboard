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
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

/**
 * 中文说明：
 * 1. `CreateWidgetPopupElements` 是 ThingsBoard Microservices 中围绕部件提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractBasePage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class CreateWidgetPopupElements extends AbstractBasePage {
    /**
     * 功能：创建 `CreateWidgetPopupElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public CreateWidgetPopupElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ENTITY_ALIAS = "//input[@formcontrolname='entityAlias']";
    private static final String CREATE_NEW_ALIAS_BTN = "//a[text() = 'Create a new one!']/parent::span";
    /**
     * 类型常量，用于统一引用固定值。
     */
    private static final String FILTER_TYPE_FIELD = "//div[contains(@class,'tb-entity-filter')]//mat-select//span";
    private static final String TYPE_FIELD = "//mat-select[@formcontrolname='entityType']//span";
    /**
     * `OPTION_FROM_DROPDOWN`常量，用于统一引用固定值。
     */
    private static final String OPTION_FROM_DROPDOWN = "//span[text() = ' %s ']";
    private static final String ENTITY_FIELD = "//input[@formcontrolname='entity']";
    /**
     * `ADD_ALIAS_BTN`常量，用于统一引用固定值。
     */
    private static final String ADD_ALIAS_BTN = "//tb-entity-alias-dialog//span[text() = ' Add ']/parent::button";
    private static final String ADD_WIDGET_BTN = "//tb-add-widget-dialog//span[text() = ' Add ']/parent::button";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ENTITY_FROM_DROPDOWN = "//b[text() = '%s']";

    /**
     * 功能：执行 `entityAlias` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityAlias() {
        return waitUntilElementToBeClickable(ENTITY_ALIAS);
    }

    /**
     * 功能：保存或创建`New Alias Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement createNewAliasBtn() {
        return waitUntilElementToBeClickable(CREATE_NEW_ALIAS_BTN);
    }

    /**
     * 功能：执行 `filterTypeFiled` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement filterTypeFiled() {
        return waitUntilElementToBeClickable(FILTER_TYPE_FIELD);
    }

    /**
     * 功能：执行 `typeFiled` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement typeFiled() {
        return waitUntilElementToBeClickable(TYPE_FIELD);
    }

    /**
     * 功能：执行 `optionFromDropdown` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public WebElement optionFromDropdown(String type) {
        return waitUntilElementToBeClickable(String.format(OPTION_FROM_DROPDOWN, type));
    }

    /**
     * 功能：执行 `entityFiled` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityFiled() {
        return waitUntilElementToBeClickable(ENTITY_FIELD);
    }

    /**
     * 功能：保存或创建`Alias Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAliasBtn() {
        return waitUntilElementToBeClickable(ADD_ALIAS_BTN);
    }

    /**
     * 功能：保存或创建部件。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addWidgetBtn() {
        return waitUntilElementToBeClickable(ADD_WIDGET_BTN);
    }

    /**
     * 功能：执行 `entityFromDropdown` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement entityFromDropdown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ENTITY_FROM_DROPDOWN, entityName));
    }
}
