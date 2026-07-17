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

import java.util.List;

/**
 * 中文说明：
 * 1. `RuleChainsPageElements` 是 ThingsBoard Microservices 中围绕 `Rule Chains Page` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `OtherPageElementsHelper`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class RuleChainsPageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `RuleChainsPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleChainsPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * `MAKE_ROOT_BTN`常量，用于统一引用固定值。
     */
    private static final String MAKE_ROOT_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'flag')]/ancestor::button";
    private static final String ROOT = ENTITY + "/../..//mat-icon[text() = 'check_box']";
    /**
     * `ROOT_DISABLE`常量，用于统一引用固定值。
     */
    private static final String ROOT_DISABLE = ENTITY + "/../..//mat-icon[text() = 'check_box_outline_blank']";
    private static final String CREATED_TIME = ENTITY + "/../..//mat-cell/span[contains(text(),'%s')]";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    private static final String CREATE_RULE_CHAIN_BTN = "//span[contains(text(),'Create new rule chain')]";
    private static final String CREATE_RULE_CHAIN_NAME_FIELD = "//form[@class='ng-untouched ng-pristine ng-invalid']//input[@formcontrolname='name']";
    /**
     * `RULE_CHAINS_NAMES_WITHOUT_ROOT`常量，用于统一引用固定值。
     */
    private static final String RULE_CHAINS_NAMES_WITHOUT_ROOT = "//mat-icon[contains(text(),'check_box_outline_blank')]/../../../mat-cell[contains(@class,'name')]/span";
    private static final String DELETE_RULE_CHAIN_FROM_VIEW_BTN = "//span[contains(text(),' Delete')]";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    private static final String IMPORT_RULE_CHAIN_BTN = "//span[contains(text(),'Import rule chain')]";
    private static final String OPEN_RULE_CHAIN_FROM_VIEW = "//span[contains(text(),'Open rule chain')]";
    /**
     * `MAKE_ROOT_FROM_VIEW`常量，用于统一引用固定值。
     */
    private static final String MAKE_ROOT_FROM_VIEW = "//span[contains(text(),' Make rule chain root ')]/parent::button";
    private static final String ROOT_ACTIVE_CHECKBOXES = "//mat-icon[text() = 'check_box']";
    /**
     * `ALL_NAMES`常量，用于统一引用固定值。
     */
    private static final String ALL_NAMES = "//mat-icon[contains(text(),'check')]/../../../mat-cell[contains(@class,'name')]/span";

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeleteRuleChainFromViewBtn() {
        return DELETE_RULE_CHAIN_FROM_VIEW_BTN;
    }

    /**
     * 功能：执行 `makeRootBtn` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement makeRootBtn(String entityName) {
        return waitUntilElementToBeClickable(String.format(MAKE_ROOT_BTN, entityName));
    }

    /**
     * 功能：执行 `rootCheckBoxesEnable` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> rootCheckBoxesEnable() {
        return waitUntilVisibilityOfElementsLocated(ROOT_ACTIVE_CHECKBOXES);
    }

    /**
     * 功能：执行 `rootCheckBoxEnable` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement rootCheckBoxEnable(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ROOT, entityName));
    }

    /**
     * 功能：执行 `rootCheckBoxDisable` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement rootCheckBoxDisable(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ROOT_DISABLE, entityName));
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement createRuleChainBtn() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_BTN);
    }

    /**
     * 功能：执行 `importRuleChainBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement importRuleChainBtn() {
        return waitUntilElementToBeClickable(IMPORT_RULE_CHAIN_BTN);
    }

    /**
     * 功能：执行 `nameField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement nameField() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_NAME_FIELD);
    }

    /**
     * 功能：执行 `notRootRuleChainsNames` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> notRootRuleChainsNames() {
        return waitUntilVisibilityOfElementsLocated(RULE_CHAINS_NAMES_WITHOUT_ROOT);
    }

    /**
     * 功能：删除或清理`Btn From View`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deleteBtnFromView() {
        return waitUntilElementToBeClickable(DELETE_RULE_CHAIN_FROM_VIEW_BTN);
    }

    /**
     * 功能：执行 `openRuleChainFromViewBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement openRuleChainFromViewBtn() {
        return waitUntilElementToBeClickable(OPEN_RULE_CHAIN_FROM_VIEW);
    }

    /**
     * 功能：执行 `entities` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> entities(String name) {
        return waitUntilVisibilityOfElementsLocated(String.format(ENTITY, name));
    }

    /**
     * 功能：执行 `makeRootFromViewBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement makeRootFromViewBtn() {
        return waitUntilElementToBeClickable(MAKE_ROOT_FROM_VIEW);
    }

    /**
     * 功能：执行 `allNames` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> allNames() {
        return waitUntilVisibilityOfElementsLocated(ALL_NAMES);
    }

    /**
     * 功能：执行 `createdTimeEntity` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `time`：`time` 参数。
     * 返回：处理结果。
     */
    public WebElement createdTimeEntity(String name, String time) {
        return waitUntilElementToBeClickable(String.format(CREATED_TIME, name, time));
    }
}
