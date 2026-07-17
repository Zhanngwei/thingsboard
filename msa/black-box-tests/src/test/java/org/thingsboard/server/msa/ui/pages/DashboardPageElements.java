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
 * 1. `DashboardPageElements` 是 ThingsBoard Microservices 中围绕仪表盘提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `OtherPageElementsHelper`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class DashboardPageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `DashboardPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public DashboardPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * `TITLES`常量，用于统一引用固定值。
     */
    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-title')]/span";
    private static final String ASSIGNED_BTN = ENTITY + "/../..//mat-icon[contains(text(),' assignment_ind')]/../..";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String MANAGE_ASSIGNED_ENTITY_LIST_FIELD = "//input[@formcontrolname='entity']";
    private static final String MANAGE_ASSIGNED_ENTITY = "//mat-option//span[contains(text(),'%s')]";
    /**
     * `MANAGE_ASSIGNED_UPDATE_BTN`常量，用于统一引用固定值。
     */
    private static final String MANAGE_ASSIGNED_UPDATE_BTN = "//button[@type='submit']";
    private static final String EDIT_BTN = "//mat-icon[text() = 'edit']/parent::button[@mat-stroked-button]";
    /**
     * `ADD_BTN`常量，用于统一引用固定值。
     */
    private static final String ADD_BTN = "//mat-fab-actions//mat-icon[text() = 'add']/parent::button";
    private static final String ALARM_WIDGET_BUNDLE = "//mat-card-title[text() = 'Alarm widgets']/ancestor::mat-card";
    /**
     * 告警常量，用于统一引用固定值。
     */
    private static final String ALARM_TABLE_WIDGET = "//img[@alt='Alarms table']/ancestor::mat-card";
    private static final String WIDGET_SE_CORNER = "//div[contains(@class,'handle-se')]";
    /**
     * `SAVE_BTN`常量，用于统一引用固定值。
     */
    private static final String SAVE_BTN = "//mat-icon[text() = 'done']/parent::button[@fxhide.lt-lg]";

    /**
     * 功能：执行 `entityTitles` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    /**
     * 功能：执行 `assignedBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement assignedBtn(String title) {
        return waitUntilElementToBeClickable(String.format(ASSIGNED_BTN, title));
    }

    /**
     * 功能：执行 `manageAssignedEntityListField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageAssignedEntityListField() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_ENTITY_LIST_FIELD);
    }

    /**
     * 功能：执行 `manageAssignedEntity` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageAssignedEntity(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_ASSIGNED_ENTITY, title));
    }

    /**
     * 功能：执行 `manageAssignedUpdateBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageAssignedUpdateBtn() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_UPDATE_BTN);
    }

    /**
     * 功能：执行 `editBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement editBtn() {
        return waitUntilElementToBeClickable(EDIT_BTN);
    }

    /**
     * 功能：保存或创建`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addBtn() {
        return waitUntilElementToBeClickable(ADD_BTN);
    }

    /**
     * 功能：执行 `alarmWidgetBundle` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmWidgetBundle() {
        return waitUntilElementToBeClickable(ALARM_WIDGET_BUNDLE);
    }

    /**
     * 功能：执行 `alarmTableWidget` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmTableWidget() {
        return waitUntilElementToBeClickable(ALARM_TABLE_WIDGET);
    }

    /**
     * 功能：执行 `widgetSECorner` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement widgetSECorner() {
        return waitUntilElementToBeClickable(WIDGET_SE_CORNER);
    }

    /**
     * 功能：保存或创建`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement saveBtn() {
        return waitUntilVisibilityOfElementLocated(SAVE_BTN);
    }
}
