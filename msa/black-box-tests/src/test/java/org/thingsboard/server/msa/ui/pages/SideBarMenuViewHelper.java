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

/**
 * 中文说明：
 * 1. `SideBarMenuViewHelper` 是 ThingsBoard Microservices 中处理 `Side Bar Menu` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `SideBarMenuViewElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class SideBarMenuViewHelper extends SideBarMenuViewElements {
    /**
     * 功能：创建 `SideBarMenuViewHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public SideBarMenuViewHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 功能：执行 `openDeviceProfiles` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openDeviceProfiles() {
        openProfilesDropDown();
        deviceProfileBtn().click();
    }

    /**
     * 功能：执行 `openAssetProfiles` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openAssetProfiles() {
        openProfilesDropDown();
        assetProfileBtn().click();
    }

    /**
     * 功能：执行 `goToDevicesPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void goToDevicesPage() {
        openEntitiesDropdown();
        devicesBtn().click();
    }

    /**
     * 功能：执行 `goToAssetsPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void goToAssetsPage() {
        openEntitiesDropdown();
        assetsBtn().click();
    }

    /**
     * 功能：执行 `goToEntityViewsPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void goToEntityViewsPage() {
        openEntitiesDropdown();
        entityViewsBtn().click();
    }

    /**
     * 功能：执行 `openEntitiesDropdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openEntitiesDropdown() {
        if (entitiesDropdownIsClose()) {
            entitiesDropdown().click();
        }
    }

    /**
     * 功能：执行 `openProfilesDropDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openProfilesDropDown() {
        if (profilesIsClose()) {
            profilesDropdown().click();
        }
    }

    /**
     * 功能：执行 `entitiesDropdownIsClose` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean entitiesDropdownIsClose() {
        return dropdownIsClose(entitiesDropdown());
    }

    /**
     * 功能：执行 `profilesIsClose` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean profilesIsClose() {
        return dropdownIsClose(profilesDropdown());
    }

    /**
     * 功能：执行 `dropdownIsClose` 对应的处理。
     * 参数：
     * - `dropdown`：`dropdown` 参数。
     * 返回：判断结果。
     */
    private boolean dropdownIsClose(WebElement dropdown) {
        return !dropdown.getAttribute("class").contains("tb-toggled");
    }
}