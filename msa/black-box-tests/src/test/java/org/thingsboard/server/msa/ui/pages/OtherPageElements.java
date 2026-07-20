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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

import java.util.List;

/**
 * 中文说明：
 * 1. `OtherPageElements` 是 ThingsBoard Microservices 中围绕 `Other Page Elements` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractBasePage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class OtherPageElements extends AbstractBasePage {
    /**
     * 功能：创建 `OtherPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public OtherPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 实体常量，用于统一引用固定值。
     */
    protected static final String ENTITY = "//mat-row//span[contains(text(),'%s')]";
    protected static final String DELETE_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'delete')]/ancestor::button";
    /**
     * `DETAILS_BTN`常量，用于统一引用固定值。
     */
    protected static final String DETAILS_BTN = ENTITY + "/../..//mat-icon[contains(text(),'edit')]/../..";
    private static final String ENTITY_COUNT = "//div[@class='mat-paginator-range-label']";
    /**
     * `WARNING_DELETE_POPUP_YES`常量，用于统一引用固定值。
     */
    private static final String WARNING_DELETE_POPUP_YES = "//tb-confirm-dialog//button[2]";
    private static final String WARNING_DELETE_POPUP_TITLE = "//tb-confirm-dialog/h2";
    /**
     * `REFRESH_BTN`常量，用于统一引用固定值。
     */
    private static final String REFRESH_BTN = "//mat-icon[contains(text(),'refresh')]/parent::button";
    private static final String HELP_BTN = "//mat-icon[contains(text(),'help')]/ancestor::button";
    /**
     * `CHECKBOX`常量，用于统一引用固定值。
     */
    private static final String CHECKBOX = "//mat-row//span[contains(text(),'%s')]/../..//mat-checkbox";
    private static final String CHECKBOXES = "//tbody//mat-checkbox";
    /**
     * `DELETE_SELECTED_BTN`常量，用于统一引用固定值。
     */
    private static final String DELETE_SELECTED_BTN = "//div[@class='mat-toolbar-tools']//mat-icon[contains(text(),'delete')]/parent::button";
    private static final String DELETE_BTNS = "//mat-icon[contains(text(),' delete')]/../..";
    /**
     * `MARKS_CHECKBOX`常量，用于统一引用固定值。
     */
    private static final String MARKS_CHECKBOX = "//mat-row[contains (@class,'mat-selected')]//mat-checkbox[contains(@class, 'checked')]";
    private static final String SELECT_ALL_CHECKBOX = "//thead//mat-checkbox";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ALL_ENTITY = "//mat-row[@class='mat-mdc-row mdc-data-table__row cdk-row mat-row-select ng-star-inserted']";
    private static final String EDIT_PENCIL_BTN = "//tb-details-panel//mat-icon[contains(text(),'edit')]/ancestor::button";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String NAME_FIELD_EDIT_VIEW = "//input[@formcontrolname='name']";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    /**
     * `DONE_BTN_EDIT_VIEW`常量，用于统一引用固定值。
     */
    private static final String DONE_BTN_EDIT_VIEW = "//mat-icon[contains(text(),'done')]/ancestor::button";
    private static final String DESCRIPTION_ENTITY_VIEW = "//textarea";
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    private static final String DESCRIPTION_ADD_ENTITY_VIEW = "//tb-add-entity-dialog//textarea";
    private static final String DEBUG_CHECKBOX_EDIT = "//mat-checkbox[@formcontrolname='debugMode']";
    /**
     * `DEBUG_CHECKBOX_VIEW`常量，用于统一引用固定值。
     */
    private static final String DEBUG_CHECKBOX_VIEW = "//mat-checkbox[@formcontrolname='debugMode']//input";
    private static final String CLOSE_ENTITY_VIEW_BTN = "//header//mat-icon[contains(text(),'close')]/parent::button";
    /**
     * `SEARCH_BTN`常量，用于统一引用固定值。
     */
    private static final String SEARCH_BTN = "//mat-toolbar//mat-icon[contains(text(),'search')]/ancestor::button[contains(@class,'ng-star')]";
    private static final String SORT_BY_NAME_BTN = "//div[contains(text(),'Name')]";
    /**
     * `SORT_BY_TITLE_BTN`常量，用于统一引用固定值。
     */
    private static final String SORT_BY_TITLE_BTN = "//div[contains(text(),'Title')]";
    private static final String SORT_BY_TIME_BTN = "//div[contains(text(),'Created time')]/..";
    /**
     * 创建时间常量，用于统一引用固定值。
     */
    private static final String CREATED_TIME = "//tbody[@role='rowgroup']//mat-cell[2]/span";
    private static final String PLUS_BTN = "//mat-icon[contains(text(),'add')]/ancestor::button";
    /**
     * `CREATE_VIEW_ADD_BTN`常量，用于统一引用固定值。
     */
    private static final String CREATE_VIEW_ADD_BTN = "//span[contains(text(),'Add')]/..";
    private static final String WARNING_MESSAGE = "//tb-snack-bar-component/div/div";
    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String ERROR_MESSAGE = "//mat-error";
    private static final String ENTITY_VIEW_TITLE = "//div[@class='tb-details-title']//span";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String LIST_OF_ENTITY = "//div[@role='listbox']/mat-option";
    private static final String ENTITY_FROM_LIST = "//div[@role='listbox']/mat-option//span[contains(text(),'%s')]";
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    protected static final String ADD_ENTITY_VIEW = "//tb-add-entity-dialog";
    protected static final String STATE_CONTROLLER = "//tb-entity-state-controller";
    /**
     * 字段名常量，用于统一引用固定值。
     */
    private static final String SEARCH_FIELD = "//input[contains (@placeholder,'Search')]";
    private static final String BROWSE_FILE = "//input[@class='file-input']";
    /**
     * 文件常量，用于统一引用固定值。
     */
    private static final String IMPORT_BROWSE_FILE = "//mat-dialog-container//span[contains(text(),'Import')]/..";
    private static final String IMPORTING_FILE = "//div[contains(text(),'%s')]";
    /**
     * 文件常量，用于统一引用固定值。
     */
    private static final String CLEAR_IMPORT_FILE_BTN = "//div[@class='tb-file-clear-container']//button";

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：文本结果。
     */
    public String getEntity(String entityName) {
        return String.format(ENTITY, entityName);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getWarningMessage() {
        return WARNING_MESSAGE;
    }

    /**
     * 功能：获取`Delete Btns`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeleteBtns() {
        return DELETE_BTNS;
    }

    /**
     * 功能：获取`Checkbox`。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：文本结果。
     */
    public String getCheckbox(String entityName) {
        return String.format(CHECKBOX, entityName);
    }

    /**
     * 功能：获取`Checkboxes`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCheckboxes() {
        return String.format(CHECKBOXES);
    }

    /**
     * 功能：执行 `warningPopUpYesBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement warningPopUpYesBtn() {
        return waitUntilElementToBeClickable(WARNING_DELETE_POPUP_YES);
    }

    /**
     * 功能：执行 `warningPopUpTitle` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement warningPopUpTitle() {
        return waitUntilElementToBeClickable(WARNING_DELETE_POPUP_TITLE);
    }

    /**
     * 功能：执行 `entityCount` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityCount() {
        return waitUntilVisibilityOfElementLocated(ENTITY_COUNT);
    }

    /**
     * 功能：更新`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement refreshBtn() {
        return waitUntilElementToBeClickable(REFRESH_BTN);
    }

    /**
     * 功能：执行 `helpBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement helpBtn() {
        return waitUntilElementToBeClickable(HELP_BTN);
    }

    /**
     * 功能：校验`Box`。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：判断结果。
     */
    public WebElement checkBox(String entityName) {
        return waitUntilElementToBeClickable(String.format(CHECKBOX, entityName));
    }

    /**
     * 功能：执行 `presentCheckBox` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public WebElement presentCheckBox(String name) {
        return waitUntilPresenceOfElementLocated(getCheckbox(name));
    }

    /**
     * 功能：删除或清理`Selected Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deleteSelectedBtn() {
        return waitUntilElementToBeClickable(DELETE_SELECTED_BTN);
    }

    /**
     * 功能：执行 `selectAllCheckBox` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement selectAllCheckBox() {
        return waitUntilElementToBeClickable(SELECT_ALL_CHECKBOX);
    }

    /**
     * 功能：执行 `editPencilBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement editPencilBtn() {
        waitUntilVisibilityOfElementsLocated(EDIT_PENCIL_BTN);
        return waitUntilElementToBeClickable(EDIT_PENCIL_BTN);
    }

    /**
     * 功能：执行 `nameFieldEditMenu` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement nameFieldEditMenu() {
        return waitUntilElementToBeClickable(NAME_FIELD_EDIT_VIEW);
    }

    /**
     * 功能：执行 `headerNameView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement headerNameView() {
        return waitUntilVisibilityOfElementLocated(HEADER_NAME_VIEW);
    }

    /**
     * 功能：执行 `doneBtnEditView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement doneBtnEditView() {
        return waitUntilElementToBeClickable(DONE_BTN_EDIT_VIEW);
    }

    /**
     * 功能：执行 `descriptionEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement descriptionEntityView() {
        return waitUntilVisibilityOfElementLocated(DESCRIPTION_ENTITY_VIEW);
    }

    /**
     * 功能：执行 `descriptionAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement descriptionAddEntityView() {
        return waitUntilVisibilityOfElementLocated(DESCRIPTION_ADD_ENTITY_VIEW);
    }

    /**
     * 功能：执行 `debugCheckboxEdit` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement debugCheckboxEdit() {
        return waitUntilElementToBeClickable(DEBUG_CHECKBOX_EDIT);
    }

    /**
     * 功能：执行 `debugCheckboxView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement debugCheckboxView() {
        return waitUntilPresenceOfElementLocated(DEBUG_CHECKBOX_VIEW);
    }

    /**
     * 功能：停止或关闭实体视图。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement closeEntityViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_ENTITY_VIEW_BTN);
    }

    /**
     * 功能：获取`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement searchBtn() {
        return waitUntilElementToBeClickable(SEARCH_BTN);
    }

    /**
     * 功能：删除或清理`Btns`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> deleteBtns() {
        return waitUntilVisibilityOfElementsLocated(DELETE_BTNS);
    }

    /**
     * 功能：校验`Boxes`。
     * 参数：无。
     * 返回：判断结果。
     */
    public List<WebElement> checkBoxes() {
        return waitUntilElementsToBeClickable(CHECKBOXES);
    }

    /**
     * 功能：执行 `markCheckbox` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> markCheckbox() {
        return waitUntilVisibilityOfElementsLocated(MARKS_CHECKBOX);
    }

    /**
     * 功能：执行 `allEntity` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> allEntity() {
        return waitUntilVisibilityOfElementsLocated(ALL_ENTITY);
    }

    /**
     * 功能：执行 `doneBtnEditViewVisible` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement doneBtnEditViewVisible() {
        return waitUntilVisibilityOfElementLocated(DONE_BTN_EDIT_VIEW);
    }

    /**
     * 功能：执行 `sortByNameBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement sortByNameBtn() {
        return waitUntilElementToBeClickable(SORT_BY_NAME_BTN);
    }

    /**
     * 功能：执行 `sortByTitleBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement sortByTitleBtn() {
        return waitUntilElementToBeClickable(SORT_BY_TITLE_BTN);
    }

    /**
     * 功能：执行 `sortByTimeBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement sortByTimeBtn() {
        return waitUntilElementToBeClickable(SORT_BY_TIME_BTN);
    }

    /**
     * 功能：执行 `createdTime` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> createdTime() {
        return waitUntilVisibilityOfElementsLocated(CREATED_TIME);
    }

    /**
     * 功能：执行 `plusBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement plusBtn() {
        return waitUntilElementToBeClickable(PLUS_BTN);
    }

    /**
     * 功能：保存或创建`Btn C`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addBtnC() {
        return waitUntilElementToBeClickable(CREATE_VIEW_ADD_BTN);
    }

    /**
     * 功能：保存或创建`Btn V`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addBtnV() {
        return waitUntilVisibilityOfElementLocated(CREATE_VIEW_ADD_BTN);
    }

    /**
     * 功能：执行 `warningMessage` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement warningMessage() {
        return waitUntilVisibilityOfElementLocated(WARNING_MESSAGE);
    }

    /**
     * 功能：删除或清理`Btn`。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement deleteBtn(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(DELETE_BTN, entityName));
    }

    /**
     * 功能：执行 `detailsBtn` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement detailsBtn(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(DETAILS_BTN, entityName));
    }

    /**
     * 功能：执行 `entity` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement entity(String entityName) {
        return waitUntilElementToBeClickable(String.format(ENTITY, entityName));
    }

    /**
     * 功能：执行 `errorMessage` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement errorMessage() {
        return waitUntilVisibilityOfElementLocated(ERROR_MESSAGE);
    }

    /**
     * 功能：执行 `entityViewTitle` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityViewTitle() {
        return waitUntilVisibilityOfElementLocated(ENTITY_VIEW_TITLE);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> listOfEntity() {
        return waitUntilElementsToBeClickable(LIST_OF_ENTITY);
    }

    /**
     * 功能：执行 `entityFromList` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement entityFromList(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ENTITY_FROM_LIST, entityName));
    }

    /**
     * 功能：保存或创建实体视图。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW);
    }

    /**
     * 功能：执行 `stateController` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement stateController() {
        return waitUntilVisibilityOfElementLocated(STATE_CONTROLLER);
    }

    /**
     * 功能：获取字段名。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement searchField() {
        return waitUntilElementToBeClickable(SEARCH_FIELD);
    }

    /**
     * 功能：执行 `browseFile` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement browseFile() {
        waitUntilElementToBeClickable(BROWSE_FILE + "/preceding-sibling::button");
        return driver.findElement(By.xpath(BROWSE_FILE));
    }

    /**
     * 功能：执行 `importBrowseFileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement importBrowseFileBtn() {
        return waitUntilElementToBeClickable(IMPORT_BROWSE_FILE);
    }

    /**
     * 功能：执行 `importingFile` 对应的处理。
     * 参数：
     * - `fileName`：名称。
     * 返回：处理结果。
     */
    public WebElement importingFile(String fileName) {
        return waitUntilVisibilityOfElementLocated(String.format(IMPORTING_FILE, fileName));
    }

    /**
     * 功能：删除或清理文件。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement clearImportFileBtn() {
        return waitUntilElementToBeClickable(CLEAR_IMPORT_FILE_BTN);
    }
}
