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
 * 1. 类目的：`CustomerPageElements` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
public class CustomerPageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `CustomerPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public CustomerPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER = "//mat-row//span[contains(text(),'%s')]";
    private static final String EMAIL = ENTITY + "/../..//mat-cell[contains(@class,'email')]/span";
    /**
     * `COUNTRY`常量，用于统一引用固定值。
     */
    private static final String COUNTRY = ENTITY + "/../..//mat-cell[contains(@class,'country')]/span";
    private static final String CITY = ENTITY + "/../..//mat-cell[contains(@class,'city')]/span";
    /**
     * `TITLES`常量，用于统一引用固定值。
     */
    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-title')]/span";
    protected static final String EDIT_MENU_DASHBOARD_FIELD = "//input[@formcontrolname='dashboard']";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    private static final String EDIT_MENU_DASHBOARD = "//div[@class='cdk-overlay-pane']//span/span[contains(text(),'%s')]";
    private static final String MANAGE_CUSTOMERS_USERS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' account_circle')]/parent::button";
    /**
     * `MANAGE_CUSTOMERS_ASSETS_BTN`常量，用于统一引用固定值。
     */
    private static final String MANAGE_CUSTOMERS_ASSETS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' domain')]/parent::button";
    private static final String MANAGE_CUSTOMERS_DEVICES_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'devices_other')]/parent::button";
    /**
     * `MANAGE_CUSTOMERS_DASHBOARDS_BTN`常量，用于统一引用固定值。
     */
    private static final String MANAGE_CUSTOMERS_DASHBOARDS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'dashboard')]/parent::button";
    private static final String MANAGE_CUSTOMERS_EDGE_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'router')]/parent::button";
    /**
     * 用户常量，用于统一引用固定值。
     */
    private static final String ADD_USER_EMAIL = "//tb-add-user-dialog//input[@formcontrolname='email']";
    private static final String ACTIVATE_WINDOW_OK_BTN = "//span[contains(text(),'OK')]";
    /**
     * 用户常量，用于统一引用固定值。
     */
    private static final String USER_LOGIN_BTN = "//mat-icon[@data-mat-icon-name='login']/parent::button";
    private static final String USER_LOGIN_BTN_BY_EMAIL = "//mat-cell[contains(@class,'email')]/span[contains(text(),'%s')]" +
            "/ancestor::mat-row//mat-icon[@data-mat-icon-name='login']/parent::button";
    /**
     * 部件常量，用于统一引用固定值。
     */
    private static final String USERS_WIDGET = "//tb-widget";
    private static final String SELECT_COUNTRY_MENU = "//mat-form-field//mat-select[@formcontrolname='country']";
    /**
     * `COUNTRIES`常量，用于统一引用固定值。
     */
    private static final String COUNTRIES = "//span[@class='mdc-list-item__primary-text']";
    protected static final String INPUT_FIELD = "//input[@formcontrolname='%s']";
    /**
     * 名称常量，用于统一引用固定值。
     */
    protected static final String INPUT_FIELD_NAME_TITLE = "title";
    private static final String INPUT_FIELD_NAME_CITY = "city";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String INPUT_FIELD_NAME_STATE = "state";
    private static final String INPUT_FIELD_NAME_ZIP = "zip";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String INPUT_FIELD_NAME_ADDRESS = "address";
    private static final String INPUT_FIELD_NAME_ADDRESS2 = "address2";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String INPUT_FIELD_NAME_EMAIL = "email";
    private static final String INPUT_FIELD_NAME_NUMBER = "phoneNumber";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String INPUT_FIELD_NAME_ASSIGNED_LIST = "entity";
    private static final String ASSIGNED_BTN = "//button[@type='submit']";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    private static final String HIDE_HOME_DASHBOARD_TOOLBAR = "//mat-checkbox[@formcontrolname='homeDashboardHideToolbar']//label";
    private static final String FILTER_BTN = "//tb-filters-edit";
    /**
     * 时间常量，用于统一引用固定值。
     */
    private static final String TIME_BTN = "//tb-timewindow[not(@hidelabel)]";
    private static final String CUSTOMER_ICON_HEADER = "//tb-breadcrumb//span[contains(text(),'Customer %s')]";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_USER_ICON_HEADER = "Users";
    private static final String CUSTOMER_ASSETS_ICON_HEADER = "Assets";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_DEVICES_ICON_HEADER = "Devices";
    private static final String CUSTOMER_DASHBOARD_ICON_HEADER = "Dashboards";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_EDGE_ICON_HEADER = "edge instances";
    private static final String CUSTOMER_USER_ICON_HEAD = "(//mat-drawer-content//span[contains(@class,'tb-entity-table')])[1]";
    /**
     * `MANAGE_BTN_VIEW`常量，用于统一引用固定值。
     */
    private static final String MANAGE_BTN_VIEW = "//span[contains(text(),'%s')]";
    private static final String MANAGE_CUSTOMERS_USERS_BTN_VIEW = "Manage users";
    /**
     * `MANAGE_CUSTOMERS_ASSETS_BTN_VIEW`常量，用于统一引用固定值。
     */
    private static final String MANAGE_CUSTOMERS_ASSETS_BTN_VIEW = "Manage assets";
    private static final String MANAGE_CUSTOMERS_DEVICE_BTN_VIEW = "Manage devices";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    private static final String MANAGE_CUSTOMERS_DASHBOARD_BTN_VIEW = "Manage dashboards";
    private static final String MANAGE_CUSTOMERS_EDGE_BTN_VIEW = "Manage edges ";
    /**
     * `DELETE_FROM_VIEW_BTN`常量，用于统一引用固定值。
     */
    private static final String DELETE_FROM_VIEW_BTN = "//tb-customer//span[contains(text(),' Delete')]";
    private static final String CUSTOMER_DETAILS_VIEW = "//tb-details-panel";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_DETAILS_ALARMS = CUSTOMER_DETAILS_VIEW + "//span[text()='Alarms']";

    /**
     * 功能：执行 `titleFieldAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement titleFieldAddEntityView() {
        return waitUntilElementToBeClickable(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE));
    }

    /**
     * 功能：执行 `titleFieldEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement titleFieldEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE));
    }

    /**
     * 功能：执行 `customer` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement customer(String entityName) {
        return waitUntilElementToBeClickable(String.format(CUSTOMER, entityName));
    }

    /**
     * 功能：执行 `email` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement email(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(EMAIL, entityName));
    }

    /**
     * 功能：执行 `country` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement country(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(COUNTRY, entityName));
    }

    /**
     * 功能：执行 `city` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement city(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CITY, entityName));
    }

    /**
     * 功能：执行 `entityTitles` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    /**
     * 功能：执行 `editMenuDashboardField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement editMenuDashboardField() {
        return waitUntilVisibilityOfElementLocated(EDIT_MENU_DASHBOARD_FIELD);
    }

    /**
     * 功能：执行 `editMenuDashboard` 对应的处理。
     * 参数：
     * - `dashboardName`：名称。
     * 返回：处理结果。
     */
    public WebElement editMenuDashboard(String dashboardName) {
        return waitUntilElementToBeClickable(String.format(EDIT_MENU_DASHBOARD, dashboardName));
    }

    /**
     * 功能：执行 `phoneNumberEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement phoneNumberEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_NUMBER));
    }

    /**
     * 功能：执行 `phoneNumberAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement phoneNumberAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_NUMBER));
    }

    /**
     * 功能：执行 `manageCustomersUserBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageCustomersUserBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_USERS_BTN, title));
    }

    /**
     * 功能：执行 `manageCustomersAssetsBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageCustomersAssetsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_ASSETS_BTN, title));
    }

    /**
     * 功能：执行 `manageCustomersDevicesBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageCustomersDevicesBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_DEVICES_BTN, title));
    }

    /**
     * 功能：执行 `manageCustomersDashboardsBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageCustomersDashboardsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_DASHBOARDS_BTN, title));
    }

    /**
     * 功能：执行 `manageCustomersEdgeBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageCustomersEdgeBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_EDGE_BTN, title));
    }

    /**
     * 功能：保存或创建用户。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addUserEmailField() {
        return waitUntilElementToBeClickable(ADD_USER_EMAIL);
    }

    /**
     * 功能：执行 `activateWindowOkBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement activateWindowOkBtn() {
        return waitUntilElementToBeClickable(ACTIVATE_WINDOW_OK_BTN);
    }

    /**
     * 功能：执行 `userLoginBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement userLoginBtn() {
        return waitUntilElementToBeClickable(USER_LOGIN_BTN);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    public WebElement getUserLoginBtnByEmail(String email) {
        return waitUntilElementToBeClickable(String.format(USER_LOGIN_BTN_BY_EMAIL, email));
    }

    /**
     * 功能：执行 `usersWidget` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement usersWidget() {
        return waitUntilVisibilityOfElementLocated(USERS_WIDGET);
    }

    /**
     * 功能：执行 `countrySelectMenuEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement countrySelectMenuEntityView() {
        return waitUntilElementToBeClickable(SELECT_COUNTRY_MENU);
    }

    /**
     * 功能：执行 `countrySelectMenuAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement countrySelectMenuAddEntityView() {
        return waitUntilElementToBeClickable(ADD_ENTITY_VIEW + SELECT_COUNTRY_MENU);
    }

    /**
     * 功能：执行 `countries` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> countries() {
        return waitUntilElementsToBeClickable(COUNTRIES);
    }

    /**
     * 功能：执行 `cityEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement cityEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_CITY));
    }

    /**
     * 功能：执行 `cityAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement cityAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_CITY));
    }

    /**
     * 功能：执行 `stateEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement stateEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_STATE));
    }

    /**
     * 功能：执行 `stateAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement stateAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_STATE));
    }

    /**
     * 功能：执行 `zipEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement zipEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ZIP));
    }

    /**
     * 功能：执行 `zipAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement zipAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ZIP));
    }

    /**
     * 功能：执行 `addressEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addressEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS));
    }

    /**
     * 功能：执行 `addressAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addressAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS));
    }

    /**
     * 功能：执行 `address2EntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement address2EntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS2));
    }

    /**
     * 功能：执行 `address2AddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement address2AddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS2));
    }

    /**
     * 功能：执行 `emailEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement emailEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_EMAIL));
    }

    /**
     * 功能：执行 `emailAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement emailAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_EMAIL));
    }

    /**
     * 功能：执行 `assignedField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assignedField() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ASSIGNED_LIST));
    }

    /**
     * 功能：发送或提交`Assigned Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement submitAssignedBtn() {
        return waitUntilElementToBeClickable(ASSIGNED_BTN);
    }

    /**
     * 功能：执行 `hideHomeDashboardToolbarCheckbox` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement hideHomeDashboardToolbarCheckbox() {
        return waitUntilElementToBeClickable(HIDE_HOME_DASHBOARD_TOOLBAR);
    }

    /**
     * 功能：执行 `filterBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement filterBtn() {
        return waitUntilVisibilityOfElementLocated(FILTER_BTN);
    }

    /**
     * 功能：执行 `timeBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement timeBtn() {
        return waitUntilVisibilityOfElementLocated(TIME_BTN);
    }

    /**
     * 功能：执行 `customerUserIconHeader` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerUserIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_USER_ICON_HEADER));
    }

    /**
     * 功能：执行 `customerAssetsIconHeader` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerAssetsIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_ASSETS_ICON_HEADER));
    }

    /**
     * 功能：执行 `customerDevicesIconHeader` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerDevicesIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_DEVICES_ICON_HEADER));
    }

    /**
     * 功能：执行 `customerDashboardIconHeader` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerDashboardIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_DASHBOARD_ICON_HEADER));
    }

    /**
     * 功能：执行 `customerEdgeIconHeader` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerEdgeIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_EDGE_ICON_HEADER));
    }

    /**
     * 功能：执行 `customerManageWindowIconHead` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerManageWindowIconHead() {
        return waitUntilVisibilityOfElementLocated(CUSTOMER_USER_ICON_HEAD);
    }

    /**
     * 功能：执行 `manageCustomersUserBtnView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageCustomersUserBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_USERS_BTN_VIEW));
    }

    /**
     * 功能：执行 `manageCustomersAssetsBtnView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageCustomersAssetsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_ASSETS_BTN_VIEW));
    }

    /**
     * 功能：执行 `manageCustomersDeviceBtnView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageCustomersDeviceBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_DEVICE_BTN_VIEW));
    }

    /**
     * 功能：执行 `manageCustomersDashboardsBtnView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageCustomersDashboardsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_DASHBOARD_BTN_VIEW));
    }

    /**
     * 功能：执行 `manageCustomersEdgeBtnView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageCustomersEdgeBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_EDGE_BTN_VIEW));
    }

    /**
     * 功能：执行 `customerViewDeleteBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerViewDeleteBtn() {
        return waitUntilElementToBeClickable(DELETE_FROM_VIEW_BTN);
    }

    /**
     * 功能：执行 `customerDetailsView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerDetailsView() {
        return waitUntilPresenceOfElementLocated(CUSTOMER_DETAILS_VIEW);
    }

    /**
     * 功能：执行 `customerDetailsAlarmsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_DETAILS_ALARMS);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CustomerPageElements` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
