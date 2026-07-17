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
 * 1. `AlarmDetailsEntityTabElements` 是 ThingsBoard Microservices 中围绕告警提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `OtherPageElements`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class AlarmDetailsEntityTabElements extends OtherPageElements {
    /**
     * 功能：创建 `AlarmDetailsEntityTabElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmDetailsEntityTabElements(WebDriver driver) {
        super(driver);
    }

    /**
     * `ASSIGN_BTN`常量，用于统一引用固定值。
     */
    private static final String ASSIGN_BTN = "//span[text() = '%s']/ancestor::mat-row//mat-icon[contains(text(),'keyboard_arrow_down')]/parent::button";
    private static final String USER_ASSIGN_DROPDOWN = "//div[@class='user-display-name']/span[text() = '%s']";
    /**
     * 名称常量，用于统一引用固定值。
     */
    protected static final String ASSIGN_USERS_DISPLAY_NAME = "//div[@class='user-display-name']/span";
    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[@class='user-display-name'][contains(text(),'%s')]";
    /**
     * 字段名常量，用于统一引用固定值。
     */
    private static final String SEARCH_FIELD = "//input[@placeholder='Search users']";
    private static final String UNASSIGNED_BTN = "//div[@role='listbox']//mat-icon[text() = 'account_circle']/following-sibling::span";
    /**
     * `UNASSIGNED`常量，用于统一引用固定值。
     */
    private static final String UNASSIGNED = "//span[text() = '%s']/ancestor::mat-row//span[@class='assignee-cell']//mat-icon[text() = 'account_circle']/following-sibling::span";
    private static final String ALARM_DETAILS_BTN = "//span[text() = '%s']/ancestor::mat-row//mat-icon[contains(text(),'more_horiz')]/parent::button";
    /**
     * `ACCESS_FORBIDDEN_DIALOG_VIEW`常量，用于统一引用固定值。
     */
    private static final String ACCESS_FORBIDDEN_DIALOG_VIEW = "//h2[text() = 'Access Forbidden']/parent::tb-confirm-dialog";
    private static final String ALARM_ASSIGNEE_DROPDOWN = "//tb-alarm-assignee-panel";
    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String NO_USERS_FOUND_MESSAGE = "//div[@class='tb-not-found-content']/span";

    /**
     * 功能：执行 `assignBtn` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public WebElement assignBtn(String type) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_BTN, type));
    }

    /**
     * 功能：执行 `userFromAssignDropDown` 对应的处理。
     * 参数：
     * - `userEmail`：`userEmail` 参数。
     * 返回：处理结果。
     */
    public WebElement userFromAssignDropDown(String userEmail) {
        return waitUntilElementToBeClickable(String.format(USER_ASSIGN_DROPDOWN, userEmail));
    }

    /**
     * 功能：执行 `assignedUser` 对应的处理。
     * 参数：
     * - `userEmail`：`userEmail` 参数。
     * 返回：处理结果。
     */
    public WebElement assignedUser(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }

    /**
     * 功能：执行 `assignUsers` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> assignUsers() {
        return waitUntilElementsToBeClickable(ASSIGN_USERS_DISPLAY_NAME);
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement searchUserField() {
        return waitUntilElementToBeClickable(SEARCH_FIELD);
    }

    /**
     * 功能：执行 `unassignedBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement unassignedBtn() {
        return waitUntilElementToBeClickable(UNASSIGNED_BTN);
    }

    /**
     * 功能：执行 `unassigned` 对应的处理。
     * 参数：
     * - `alarmType`：类型。
     * 返回：处理结果。
     */
    public WebElement unassigned(String alarmType) {
        return waitUntilVisibilityOfElementLocated(String.format(UNASSIGNED, alarmType));
    }

    /**
     * 功能：执行 `alarmDetailsBtn` 对应的处理。
     * 参数：
     * - `alarmType`：类型。
     * 返回：处理结果。
     */
    public WebElement alarmDetailsBtn(String alarmType) {
        return waitUntilElementToBeClickable(String.format(ALARM_DETAILS_BTN, alarmType));
    }

    /**
     * 功能：执行 `accessForbiddenDialogView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement accessForbiddenDialogView() {
        return waitUntilVisibilityOfElementLocated(ACCESS_FORBIDDEN_DIALOG_VIEW);
    }

    /**
     * 功能：执行 `alarmAssigneeDropdown` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmAssigneeDropdown() {
        return waitUntilVisibilityOfElementLocated(ALARM_ASSIGNEE_DROPDOWN);
    }

    /**
     * 功能：执行 `noUsersFoundMessage` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement noUsersFoundMessage() {
        return waitUntilVisibilityOfElementLocated(NO_USERS_FOUND_MESSAGE);
    }
}
