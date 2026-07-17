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
 * 1. `ProfilesPageElements` 是 ThingsBoard Microservices 中围绕 `Profiles Page Elements` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `OtherPageElementsHelper`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class ProfilesPageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `ProfilesPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public ProfilesPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String CREATE_DEVICE_PROFILE_BTN = "//span[text()='Create new device profile']";
    private static final String CREATE_ASSET_PROFILE_BTN = "//span[text()='Create new asset profile']";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String IMPORT_DEVICE_PROFILE_BTN = "//span[text()='Import device profile']";
    private static final String IMPORT_ASSET_PROFILE_BTN = "//span[text()='Import asset profile']";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String ADD_DEVICE_PROFILE_VIEW = "//tb-add-device-profile-dialog";
    private static final String ADD_ASSET_PROFILE_VIEW = "//tb-add-entity-dialog";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_VIEW = "//tb-entity-details-panel";
    private static final String NAME_FIELD = "//input[@formcontrolname='name']";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    private static final String RULE_CHAIN_FIELD = "//input[@formcontrolname='ruleChainId']";
    private static final String DASHBOARD_FIELD = "//input[@formcontrolname='dashboard']";
    /**
     * 队列常量，用于统一引用固定值。
     */
    private static final String QUEUE_FIELD = "//input[@formcontrolname='queueName']";
    private static final String DESCRIPTION_FIELD = "//textarea[@formcontrolname='description']";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String ADD_DEVICE_PROFILE_ADD_BTN = "//span[text()='Add']";
    private static final String ADD_ASSET_PROFILE_ADD_BTN = "//button[@type='submit']";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_VIEW_DELETE_BTN = "//tb-device-profile//span[contains(text(),'Delete')]";
    private static final String ASSET_PROFILE_VIEW_DELETE_BTN = "//tb-entity-details-panel//span[contains(text(),'Delete')]";
    /**
     * 配置常量，用于统一引用固定值。
     */
    private static final String PROFILE_NAMES = "//tbody/mat-row/mat-cell[contains(@class,'name')]";
    private static final String MAKE_DEFAULT_BTN = ENTITY + "/../..//mat-icon[contains(text(),' flag')]/../..";
    /**
     * `DEFAULT`常量，用于统一引用固定值。
     */
    private static final String DEFAULT = ENTITY + "/../..//mat-icon[text() = 'check_box']";
    private static final String DEVICE_PROFILE_VIEW_MAKE_DEFAULT_BTN = "//span[text() = ' Make device profile default ']/..";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    private static final String ASSET_PROFILE_VIEW_MAKE_DEFAULT_BTN = "//span[text() = ' Make asset profile default ']/..";

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：文本结果。
     */
    protected String getDeviseProfileViewDeleteBtn() {
        return DEVICE_PROFILE_VIEW_DELETE_BTN;
    }

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：文本结果。
     */
    protected String getAssetProfileViewDeleteBtn() {
        return ASSET_PROFILE_VIEW_DELETE_BTN;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement createNewDeviceProfileBtn() {
        return waitUntilElementToBeClickable(CREATE_DEVICE_PROFILE_BTN);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement createNewAssetProfileBtn() {
        return waitUntilElementToBeClickable(CREATE_ASSET_PROFILE_BTN);
    }

    /**
     * 功能：执行 `importDeviceProfileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement importDeviceProfileBtn() {
        return waitUntilElementToBeClickable(IMPORT_DEVICE_PROFILE_BTN);
    }

    /**
     * 功能：执行 `importAssetProfileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement importAssetProfileBtn() {
        return waitUntilElementToBeClickable(IMPORT_ASSET_PROFILE_BTN);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceProfileView() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetProfileView() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceProfileNameField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + NAME_FIELD);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetProfileNameField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + NAME_FIELD);
    }

    /**
     * 功能：执行 `profileViewNameField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profileViewNameField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + NAME_FIELD);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceProfileRuleChainField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + RULE_CHAIN_FIELD);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetProfileRuleChainField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + RULE_CHAIN_FIELD);
    }

    /**
     * 功能：执行 `profileViewRuleChainField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profileViewRuleChainField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + RULE_CHAIN_FIELD);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceProfileMobileDashboardField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + DASHBOARD_FIELD);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetProfileMobileDashboardField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + DASHBOARD_FIELD);
    }

    /**
     * 功能：执行 `profileViewMobileDashboardField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profileViewMobileDashboardField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + DASHBOARD_FIELD);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceProfileQueueField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + QUEUE_FIELD);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetProfileQueueField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + QUEUE_FIELD);
    }

    /**
     * 功能：执行 `profileViewQueueField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profileViewQueueField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + QUEUE_FIELD);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceDescriptionField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + DESCRIPTION_FIELD);
    }

    /**
     * 功能：保存或创建资产。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetDescriptionField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + DESCRIPTION_FIELD);
    }

    /**
     * 功能：执行 `profileViewDescriptionField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profileViewDescriptionField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + DESCRIPTION_FIELD);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addDeviceProfileAddBtn() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_ADD_BTN);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addAssetProfileAddBtn() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_ADD_BTN);
    }

    /**
     * 功能：执行 `deviceProfileViewDeleteBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileViewDeleteBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_VIEW_DELETE_BTN);
    }

    /**
     * 功能：执行 `assetProfileViewDeleteBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assetProfileViewDeleteBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_VIEW_DELETE_BTN);
    }

    /**
     * 功能：执行 `profileNames` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> profileNames() {
        return waitUntilElementsToBeClickable(PROFILE_NAMES);
    }

    /**
     * 功能：执行 `makeProfileDefaultBtn` 对应的处理。
     * 参数：
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    public WebElement makeProfileDefaultBtn(String profileName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEFAULT_BTN, profileName));
    }

    /**
     * 功能：执行 `defaultCheckbox` 对应的处理。
     * 参数：
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    public WebElement defaultCheckbox(String profileName) {
        return waitUntilElementToBeClickable(String.format(DEFAULT, profileName));
    }

    /**
     * 功能：执行 `deviceProfileViewMakeDefaultBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileViewMakeDefaultBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_VIEW_MAKE_DEFAULT_BTN);
    }

    /**
     * 功能：执行 `assetProfileViewMakeDefaultBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assetProfileViewMakeDefaultBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_VIEW_MAKE_DEFAULT_BTN);
    }
}