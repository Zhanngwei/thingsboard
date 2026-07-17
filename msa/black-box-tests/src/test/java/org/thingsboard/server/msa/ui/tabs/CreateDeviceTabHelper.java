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
package org.thingsboard.server.msa.ui.tabs;

import org.openqa.selenium.WebDriver;

/**
 * 中文说明：
 * 1. `CreateDeviceTabHelper` 是 ThingsBoard Microservices 中处理设备通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `CreateDeviceTabElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class CreateDeviceTabHelper extends CreateDeviceTabElements {
    /**
     * 功能：创建 `CreateDeviceTabHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public CreateDeviceTabHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 功能：执行 `enterName` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void enterName(String deviceName) {
        enterText(nameField(), deviceName);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfileTitle`：设备信息或设备标识。
     * 返回：无。
     */
    public void createNewDeviceProfile(String deviceProfileTitle) {
        if (!createNewDeviceProfileRadioBtn().getAttribute("class").contains("checked")) {
            createNewDeviceProfileRadioBtn().click();
        }
        deviceProfileTitleField().sendKeys(deviceProfileTitle);
    }

    /**
     * 功能：执行 `changeDeviceProfile` 对应的处理。
     * 参数：
     * - `deviceProfileName`：设备信息或设备标识。
     * 返回：无。
     */
    public void changeDeviceProfile(String deviceProfileName) {
        if (!selectExistingDeviceProfileRadioBtn().getAttribute("class").contains("checked")) {
            selectExistingDeviceProfileRadioBtn().click();
        }
        clearProfileFieldBtn().click();
        entityFromDropdown(deviceProfileName).click();
    }

    /**
     * 功能：执行 `assignOnCustomer` 对应的处理。
     * 参数：
     * - `customerTitle`：`customerTitle` 参数。
     * 返回：无。
     */
    public void assignOnCustomer(String customerTitle) {
        customerOptionBtn().click();
        assignOnCustomerField().click();
        customerFromDropDown(customerTitle).click();
        sleep(2); //waiting for the action to count
    }

    /**
     * 功能：执行 `enterLabel` 对应的处理。
     * 参数：
     * - `label`：`label` 参数。
     * 返回：无。
     */
    public void enterLabel(String label) {
        enterText(deviceLabelField(), label);
    }

    /**
     * 功能：执行 `enterDescription` 对应的处理。
     * 参数：
     * - `description`：`description` 参数。
     * 返回：无。
     */
    public void enterDescription(String description) {
        enterText(descriptionField(), description);
    }
}
