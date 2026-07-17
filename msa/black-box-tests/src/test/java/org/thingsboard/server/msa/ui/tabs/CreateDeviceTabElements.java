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
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

/**
 * 中文说明：
 * 1. `CreateDeviceTabElements` 是 ThingsBoard Microservices 中围绕设备提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractBasePage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class CreateDeviceTabElements extends AbstractBasePage {
    /**
     * 功能：创建 `CreateDeviceTabElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public CreateDeviceTabElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String CREATE_DEVICE_NAME_FIELD = "//tb-device-wizard//input[@formcontrolname='name']";
    private static final String CREATE_NEW_DEVICE_PROFILE_RADIO_BTN = "//span[text() = 'Create new device profile']/ancestor::mat-radio-button";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String SELECT_EXISTING_DEVICE_PROFILE_RADIO_BTN = "//span[text() = 'Select existing device profile']/ancestor::mat-radio-button";
    private static final String DEVICE_PROFILE_TITLE_FIELD = "//input[@formcontrolname='newDeviceProfileTitle']";
    /**
     * `ADD_BTN`常量，用于统一引用固定值。
     */
    private static final String ADD_BTN = "//span[text() = 'Add']";
    private static final String CLEAR_PROFILE_FIELD_BTN = "//button[@aria-label='Clear']";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ENTITY_FROM_DROPDOWN = "//div[@role = 'listbox']//span[text() = '%s']";
    private static final String ASSIGN_ON_CUSTOMER_FIELD = "//input[@formcontrolname='entity']";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_OPTION_BNT = "//div[text() = 'Customer']/ancestor::mat-step-header";
    private static final String CUSTOMER_FROM_DROPDOWN = "//div[@role='listbox']/mat-option//span[contains(text(),'%s')]";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_LABEL_FIELD = "//tb-device-wizard//input[@formcontrolname='label']";
    private static final String CHECKBOX_GATEWAY = "//tb-device-wizard//mat-checkbox[@formcontrolname='gateway']//label";
    /**
     * 时间常量，用于统一引用固定值。
     */
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME = "//tb-device-wizard//mat-checkbox[@formcontrolname='overwriteActivityTime']//label";
    private static final String DESCRIPTION_FIELD = "//tb-device-wizard//textarea[@formcontrolname='description']";

    /**
     * 功能：执行 `nameField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement nameField() {
        return waitUntilElementToBeClickable(CREATE_DEVICE_NAME_FIELD);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement createNewDeviceProfileRadioBtn() {
        return waitUntilElementToBeClickable(CREATE_NEW_DEVICE_PROFILE_RADIO_BTN);
    }

    /**
     * 功能：执行 `selectExistingDeviceProfileRadioBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement selectExistingDeviceProfileRadioBtn() {
        return waitUntilElementToBeClickable(SELECT_EXISTING_DEVICE_PROFILE_RADIO_BTN);
    }

    /**
     * 功能：执行 `deviceProfileTitleField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileTitleField() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_TITLE_FIELD);
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
     * 功能：删除或清理配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement clearProfileFieldBtn() {
        return waitUntilElementToBeClickable(CLEAR_PROFILE_FIELD_BTN);
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
     * 功能：执行 `assignOnCustomerField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assignOnCustomerField() {
        return waitUntilElementToBeClickable(ASSIGN_ON_CUSTOMER_FIELD);
    }

    /**
     * 功能：执行 `customerOptionBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerOptionBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_OPTION_BNT);
    }

    /**
     * 功能：执行 `customerFromDropDown` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public WebElement customerFromDropDown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_FROM_DROPDOWN, entityName));
    }

    /**
     * 功能：执行 `deviceLabelField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceLabelField() {
        return waitUntilElementToBeClickable(DEVICE_LABEL_FIELD);
    }

    /**
     * 功能：执行 `checkboxGateway` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement checkboxGateway() {
        return waitUntilElementToBeClickable(CHECKBOX_GATEWAY);
    }

    /**
     * 功能：执行 `checkboxOverwriteActivityTime` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement checkboxOverwriteActivityTime() {
        return waitUntilElementToBeClickable(CHECKBOX_OVERWRITE_ACTIVITY_TIME);
    }

    /**
     * 功能：执行 `descriptionField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement descriptionField() {
        return waitUntilElementToBeClickable(DESCRIPTION_FIELD);
    }
}
