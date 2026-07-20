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
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

/**
 * 中文说明：
 * 1. `SideBarMenuViewElements` 是 ThingsBoard Microservices 中围绕 `Side Bar Menu` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractBasePage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class SideBarMenuViewElements extends AbstractBasePage {
    /**
     * 功能：创建 `SideBarMenuViewElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public SideBarMenuViewElements(WebDriver driver) {
        super(driver);
    }

    /**
     * `RULE_CHAINS_BTN`常量，用于统一引用固定值。
     */
    private static final String RULE_CHAINS_BTN = "//mat-toolbar//a[@href='/ruleChains']";
    private static final String CUSTOMER_BTN = "//mat-toolbar//a[@href='/customers']";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    private static final String DASHBOARD_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String PROFILES_DROPDOWN = "//mat-toolbar//mat-icon[text()='badge']/ancestor::a//span[contains(@class,'pull-right')]";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/deviceProfiles']";
    private static final String ASSET_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/assetProfiles']";
    /**
     * `ALARMS_BTN`常量，用于统一引用固定值。
     */
    private static final String ALARMS_BTN = "//mat-toolbar//a[@href='/alarms']";
    private static final String ENTITIES_DROPDOWN = "//mat-toolbar//mat-icon[text()='category']/ancestor::a//span[contains(@class,'pull-right')]";
    /**
     * `DEVICES_BTN`常量，用于统一引用固定值。
     */
    private static final String DEVICES_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Devices']";
    private static final String ASSETS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Assets']";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ENTITY_VIEWS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Entity Views']";

    /**
     * 功能：执行 `entitiesDropdown` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entitiesDropdown() {
        return waitUntilElementToBeClickable(ENTITIES_DROPDOWN);
    }

    /**
     * 功能：执行 `ruleChainsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    /**
     * 功能：执行 `customerBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_BTN);
    }

    /**
     * 功能：执行 `dashboardBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement dashboardBtn() {
        return waitUntilElementToBeClickable(DASHBOARD_BTN);
    }

    /**
     * 功能：执行 `profilesDropdown` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profilesDropdown() {
        return waitUntilElementToBeClickable(PROFILES_DROPDOWN);
    }

    /**
     * 功能：执行 `deviceProfileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_BTN);
    }

    /**
     * 功能：执行 `assetProfileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assetProfileBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_BTN);
    }

    /**
     * 功能：执行 `alarmsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmsBtn() {
        return waitUntilElementToBeClickable(ALARMS_BTN);
    }

    /**
     * 功能：执行 `devicesBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement devicesBtn() {
        return waitUntilElementToBeClickable(DEVICES_BTN);
    }

    /**
     * 功能：执行 `assetsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assetsBtn() {
        return waitUntilElementToBeClickable(ASSETS_BTN);
    }

    /**
     * 功能：执行 `entityViewsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityViewsBtn() {
        return waitUntilElementToBeClickable(ENTITY_VIEWS_BTN);
    }
}