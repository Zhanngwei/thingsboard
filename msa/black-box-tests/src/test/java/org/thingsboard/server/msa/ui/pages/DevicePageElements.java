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
 * 1. 类目的：`DevicePageElements` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
public class DevicePageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `DevicePageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public DevicePageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE = "//table//span[text()='%s']";
    private static final String DEVICE_DETAILS_VIEW = "//tb-details-panel";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_DETAILS_ALARMS = DEVICE_DETAILS_VIEW + "//span[text()='Alarms']";
    private static final String ASSIGN_TO_CUSTOMER_BTN = "//mat-cell[contains(@class,'name')]/span[text()='%s']" +
            "/ancestor::mat-row//mat-icon[contains(text(),'assignment_ind')]/parent::button";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CHOOSE_CUSTOMER_FOR_ASSIGN_FIELD = "//input[@formcontrolname='entity']";
    private static final String ENTITY_FROM_DROPDOWN = "//div[@role = 'listbox']//span[text() = '%s']";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String CLOSE_DEVICE_DETAILS_VIEW = "//header//mat-icon[contains(text(),'close')]/parent::button";
    private static final String SUBMIT_BTN = "//button[@type='submit']";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String ADD_DEVICE_BTN = "//mat-icon[text() = 'insert_drive_file']/parent::button";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String ADD_DEVICE_VIEW = "//tb-device-wizard";
    private static final String DELETE_BTN_DETAILS_TAB = "//span[contains(text(),'Delete device')]/parent::button";
    /**
     * `CHECKBOX_GATEWAY_EDIT`常量，用于统一引用固定值。
     */
    private static final String CHECKBOX_GATEWAY_EDIT = "//mat-checkbox[@formcontrolname='gateway']//label";
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME_EDIT = "//mat-checkbox[@formcontrolname='overwriteActivityTime']//label";
    /**
     * `CHECKBOX_GATEWAY_DETAILS`常量，用于统一引用固定值。
     */
    private static final String CHECKBOX_GATEWAY_DETAILS = "//mat-checkbox[@formcontrolname='gateway']//input";
    private static final String CHECKBOX_GATEWAY_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-gateway')]//mat-icon[text() = 'check_box']";
    /**
     * 时间常量，用于统一引用固定值。
     */
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME_DETAILS = "//mat-checkbox[@formcontrolname='overwriteActivityTime']//input";
    private static final String CLEAR_PROFILE_FIELD_BTN = "//button[@aria-label='Clear']";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_REDIRECTED_BTN = "//a[@aria-label='Open device profile']";
    private static final String DEVICE_LABEL_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-label')]/span";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String DEVICE_CUSTOMER_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-customerTitle')]/span";
    private static final String DEVICE_LABEL_EDIT = "//input[@formcontrolname='label']";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_DEVICE_PROFILE_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-deviceProfileName')]/span";
    private static final String ASSIGN_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'assignment_ind')]/ancestor::button";
    /**
     * `UNASSIGN_BTN`常量，用于统一引用固定值。
     */
    private static final String UNASSIGN_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' assignment_return')]/ancestor::button";
    private static final String ASSIGN_BTN_DETAILS_TAB = "//span[contains(text(),'Assign to customer')]/parent::button";
    /**
     * `UNASSIGN_BTN_DETAILS_TAB`常量，用于统一引用固定值。
     */
    private static final String UNASSIGN_BTN_DETAILS_TAB = "//span[contains(text(),'Unassign from customer')]/parent::button";
    private static final String ASSIGNED_FIELD_DETAILS_TAB = "//mat-label[text() = 'Assigned to customer']/parent::label/parent::div/input";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String ASSIGN_MARKED_DEVICE_BTN = "//mat-icon[text() = 'assignment_ind']/parent::button";
    private static final String FILTER_BTN = "//tb-device-info-filter/button";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_FIELD = "(//input[@formcontrolname='deviceProfile'])[2]";
    private static final String DEVICE_STATE_SELECT = "//div[contains(@class,'tb-filter-panel')]//mat-select[@role='combobox']";
    /**
     * 状态常量，用于统一引用固定值。
     */
    private static final String LIST_OF_DEVICES_STATE = "//div[@class='status']";
    private static final String LIST_OF_DEVICES_PROFILE = "//mat-cell[contains(@class,'deviceProfileName')]";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String MAKE_DEVICE_PUBLIC_BTN = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'share')]/parent::button";
    private static final String DEVICE_IS_PUBLIC_CHECKBOX = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'check_box')]";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String MAKE_DEVICE_PUBLIC_BTN_DETAILS_TAB = "//span[contains(text(),'Make device public')]/parent::button";
    private static final String MAKE_DEVICE_PRIVATE_BTN = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'reply')]/parent::button";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_IS_PRIVATE_CHECKBOX = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'check_box_outline_blank')]";
    private static final String MAKE_DEVICE_PRIVATE_BTN_DETAILS_TAB = "//span[contains(text(),'Make device private')]/parent::button";

    /**
     * 功能：执行 `device` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement device(String deviceName) {
        return waitUntilElementToBeClickable(String.format(DEVICE, deviceName));
    }

    /**
     * 功能：执行 `deviceDetailsAlarmsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(DEVICE_DETAILS_ALARMS);
    }

    /**
     * 功能：执行 `deviceDetailsView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceDetailsView() {
        return waitUntilPresenceOfElementLocated(DEVICE_DETAILS_VIEW);
    }

    /**
     * 功能：执行 `assignToCustomerBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement assignToCustomerBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_TO_CUSTOMER_BTN, deviceName));
    }

    /**
     * 功能：执行 `chooseCustomerForAssignField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement chooseCustomerForAssignField() {
        return waitUntilElementToBeClickable(CHOOSE_CUSTOMER_FOR_ASSIGN_FIELD);
    }

    /**
     * 功能：执行 `entityFromDropdown` 对应的处理。
     * 参数：
     * - `customerTitle`：`customerTitle` 参数。
     * 返回：处理结果。
     */
    public WebElement entityFromDropdown(String customerTitle) {
        return waitUntilElementToBeClickable(String.format(ENTITY_FROM_DROPDOWN, customerTitle));
    }

    /**
     * 功能：停止或关闭设备。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement closeDeviceDetailsViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_DEVICE_DETAILS_VIEW);
    }

    /**
     * 功能：发送或提交`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement submitBtn() {
        return waitUntilElementToBeClickable(SUBMIT_BTN);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceBtn() {
        return waitUntilElementToBeClickable(ADD_DEVICE_BTN);
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
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceView() {
        return waitUntilPresenceOfElementLocated(ADD_DEVICE_VIEW);
    }

    /**
     * 功能：删除或清理`Btn Details Tab`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deleteBtnDetailsTab() {
        return waitUntilElementToBeClickable(DELETE_BTN_DETAILS_TAB);
    }

    /**
     * 功能：执行 `checkboxGatewayEdit` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement checkboxGatewayEdit() {
        return waitUntilElementToBeClickable(CHECKBOX_GATEWAY_EDIT);
    }

    /**
     * 功能：执行 `checkboxOverwriteActivityTimeEdit` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement checkboxOverwriteActivityTimeEdit() {
        return waitUntilElementToBeClickable(CHECKBOX_OVERWRITE_ACTIVITY_TIME_EDIT);
    }

    /**
     * 功能：执行 `checkboxGatewayDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement checkboxGatewayDetailsTab() {
        return waitUntilPresenceOfElementLocated(CHECKBOX_GATEWAY_DETAILS);
    }

    /**
     * 功能：执行 `checkboxGatewayPage` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement checkboxGatewayPage(String deviceName) {
        return waitUntilPresenceOfElementLocated(String.format(CHECKBOX_GATEWAY_PAGE, deviceName));
    }

    /**
     * 功能：执行 `checkboxOverwriteActivityTimeDetails` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement checkboxOverwriteActivityTimeDetails() {
        return waitUntilPresenceOfElementLocated(CHECKBOX_OVERWRITE_ACTIVITY_TIME_DETAILS);
    }

    /**
     * 功能：删除或清理配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement clearProfileFieldBtn() {
        return waitUntilElementToBeClickable(CLEAR_PROFILE_FIELD_BTN);
    }

    /**
     * 功能：执行 `deviceProfileRedirectedBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileRedirectedBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_REDIRECTED_BTN);
    }

    /**
     * 功能：执行 `deviceLabelOnPage` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement deviceLabelOnPage(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_LABEL_PAGE, deviceName));
    }

    /**
     * 功能：执行 `deviceCustomerOnPage` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement deviceCustomerOnPage(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_CUSTOMER_PAGE, deviceName));
    }

    /**
     * 功能：执行 `deviceLabelEditField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceLabelEditField() {
        return waitUntilElementToBeClickable(DEVICE_LABEL_EDIT);
    }

    /**
     * 功能：执行 `deviceLabelDetailsField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceLabelDetailsField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_LABEL_EDIT);
    }

    /**
     * 功能：执行 `deviceDeviceProfileOnPage` 对应的处理。
     * 参数：
     * - `deviceProfileTitle`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement deviceDeviceProfileOnPage(String deviceProfileTitle) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_DEVICE_PROFILE_PAGE, deviceProfileTitle));
    }

    /**
     * 功能：执行 `assignBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement assignBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_BTN, deviceName));
    }

    /**
     * 功能：执行 `assignBtnVisible` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement assignBtnVisible(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(ASSIGN_BTN, deviceName));
    }

    /**
     * 功能：执行 `unassignBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement unassignBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(UNASSIGN_BTN, deviceName));
    }

    /**
     * 功能：执行 `assignBtnDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assignBtnDetailsTab() {
        return waitUntilElementToBeClickable(ASSIGN_BTN_DETAILS_TAB);
    }

    /**
     * 功能：执行 `unassignBtnDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement unassignBtnDetailsTab() {
        return waitUntilElementToBeClickable(UNASSIGN_BTN_DETAILS_TAB);
    }

    /**
     * 功能：执行 `assignFieldDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assignFieldDetailsTab() {
        return waitUntilVisibilityOfElementLocated(ASSIGNED_FIELD_DETAILS_TAB);
    }

    /**
     * 功能：执行 `assignMarkedDeviceBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assignMarkedDeviceBtn() {
        return waitUntilVisibilityOfElementLocated(ASSIGN_MARKED_DEVICE_BTN);
    }

    /**
     * 功能：执行 `filterBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement filterBtn() {
        return waitUntilElementToBeClickable(FILTER_BTN);
    }

    /**
     * 功能：执行 `deviceProfileField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileField() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_FIELD);
    }

    /**
     * 功能：执行 `deviceStateSelect` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceStateSelect() {
        return waitUntilElementToBeClickable(DEVICE_STATE_SELECT);
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> listOfDevicesState() {
        return waitUntilVisibilityOfElementsLocated(LIST_OF_DEVICES_STATE);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> listOfDevicesProfile() {
        return waitUntilVisibilityOfElementsLocated(LIST_OF_DEVICES_PROFILE);
    }

    /**
     * 功能：执行 `makeDevicePublicBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement makeDevicePublicBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEVICE_PUBLIC_BTN, deviceName));
    }

    /**
     * 功能：执行 `deviceIsPublicCheckbox` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement deviceIsPublicCheckbox(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_IS_PUBLIC_CHECKBOX, deviceName));
    }

    /**
     * 功能：执行 `makeDevicePublicBtnDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement makeDevicePublicBtnDetailsTab() {
        return waitUntilElementToBeClickable(MAKE_DEVICE_PUBLIC_BTN_DETAILS_TAB);
    }

    /**
     * 功能：执行 `makeDevicePrivateBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement makeDevicePrivateBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEVICE_PRIVATE_BTN, deviceName));
    }

    /**
     * 功能：执行 `deviceIsPrivateCheckbox` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public WebElement deviceIsPrivateCheckbox(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_IS_PRIVATE_CHECKBOX, deviceName));
    }

    /**
     * 功能：执行 `makeDevicePrivateBtnDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement makeDevicePrivateBtnDetailsTab() {
        return waitUntilElementToBeClickable(MAKE_DEVICE_PRIVATE_BTN_DETAILS_TAB);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DevicePageElements` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
