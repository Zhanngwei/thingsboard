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
 * 1. `DevicePageHelper` 是 ThingsBoard Microservices 中处理设备通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `DevicePageElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class DevicePageHelper extends DevicePageElements {
    /**
     * 功能：创建 `DevicePageHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public DevicePageHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    private String description;
    private String label;

    /**
     * 功能：执行 `openDeviceAlarms` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void openDeviceAlarms(String deviceName) {
        if (!deviceDetailsView().isDisplayed()) {
            device(deviceName).click();
        }
        deviceDetailsAlarmsBtn().click();
    }

    /**
     * 功能：执行 `assignToCustomer` 对应的处理。
     * 参数：
     * - `customerTitle`：`customerTitle` 参数。
     * 返回：无。
     */
    public void assignToCustomer(String customerTitle) {
        chooseCustomerForAssignField().click();
        entityFromDropdown(customerTitle).click();
        submitBtn().click();
    }

    /**
     * 功能：执行 `openCreateDeviceView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openCreateDeviceView() {
        plusBtn().click();
        addDeviceBtn().click();
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void deleteDeviceByRightSideBtn(String deviceName) {
        deleteBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    public void deleteDeviceFromDetailsTab() {
        deleteBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：更新描述信息。
     * 参数：无。
     * 返回：无。
     */
    public void setDescription() {
        scrollToElement(descriptionEntityView());
        description = descriptionEntityView().getAttribute("value");
    }

    /**
     * 功能：更新显示标签。
     * 参数：无。
     * 返回：无。
     */
    public void setLabel() {
        label = deviceLabelDetailsField().getAttribute("value");
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
     * 功能：获取显示标签。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getLabel() {
        return label;
    }

    /**
     * 功能：执行 `changeDeviceProfile` 对应的处理。
     * 参数：
     * - `deviceProfileName`：设备信息或设备标识。
     * 返回：无。
     */
    public void changeDeviceProfile(String deviceProfileName) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileName).click();
    }

    /**
     * 功能：执行 `unassignedDeviceByRightSideBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void unassignedDeviceByRightSideBtn(String deviceName) {
        unassignBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：执行 `unassignedDeviceFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void unassignedDeviceFromDetailsTab() {
        unassignBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：执行 `selectDevices` 对应的处理。
     * 参数：
     * - `deviceNames`：设备信息或设备标识。
     * 返回：无。
     */
    public void selectDevices(String... deviceNames) {
        for (String deviceName : deviceNames) {
            checkBox(deviceName).click();
        }
    }

    /**
     * 功能：执行 `assignSelectedDevices` 对应的处理。
     * 参数：
     * - `deviceNames`：设备信息或设备标识。
     * 返回：无。
     */
    public void assignSelectedDevices(String... deviceNames) {
        selectDevices(deviceNames);
        assignMarkedDeviceBtn().click();
    }

    /**
     * 功能：删除或清理`Selected Devices`。
     * 参数：
     * - `deviceNames`：设备信息或设备标识。
     * 返回：无。
     */
    public void deleteSelectedDevices(String... deviceNames) {
        selectDevices(deviceNames);
        deleteSelectedBtn().click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：执行 `filterDeviceByDeviceProfile` 对应的处理。
     * 参数：
     * - `deviceProfileTitle`：设备信息或设备标识。
     * 返回：无。
     */
    public void filterDeviceByDeviceProfile(String deviceProfileTitle) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileTitle).click();
        submitBtn().click();
    }

    /**
     * 功能：执行 `filterDeviceByState` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    public void filterDeviceByState(String state) {
        deviceStateSelect().click();
        entityFromDropdown(" " + state + " ").click();
        sleep(2); //wait until the action is counted
        submitBtn().click();
    }

    /**
     * 功能：执行 `filterDeviceByDeviceProfileAndState` 对应的处理。
     * 参数：
     * - `deviceProfileTitle`：设备信息或设备标识。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    public void filterDeviceByDeviceProfileAndState(String deviceProfileTitle, String state) {
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileTitle).click();
        deviceStateSelect().click();
        entityFromDropdown(" " + state + " ").click();
        sleep(2); //wait until the action is counted
        submitBtn().click();
    }

    /**
     * 功能：执行 `makeDevicePublicByRightSideBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void makeDevicePublicByRightSideBtn(String deviceName) {
        makeDevicePublicBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：执行 `makeDevicePublicFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void makeDevicePublicFromDetailsTab() {
        makeDevicePublicBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：执行 `makeDevicePrivateByRightSideBtn` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void makeDevicePrivateByRightSideBtn(String deviceName) {
        makeDevicePrivateBtn(deviceName).click();
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：执行 `makeDevicePrivateFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void makeDevicePrivateFromDetailsTab() {
        makeDevicePrivateBtnDetailsTab().click();
        warningPopUpYesBtn().click();
    }
}
