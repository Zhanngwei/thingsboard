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
package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `DeleteDeviceProfileTest` 是 ThingsBoard Microservices 中验证 `DeleteDeviceProfile` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class DeleteDeviceProfileTest extends AbstractDriverBaseTest {

    /**
     * `sideBarMenuView` 字段，保存当前对象的对应属性。
     */
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    /**
     * 功能：执行 `login` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the device profile by clicking on the trash icon in the right side of device profile")
    public void removeDeviceProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the device profile by clicking on the 'Delete device profile' btn in the entity view")
    public void removeDeviceProfileFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        profilesPage.deviceProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedDeviceProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default device profile by clicking on the trash icon in the right side of device profile")
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the Default device profile by clicking on the 'Delete device profile' btn in the entity view")
    public void removeDefaultDeviceProfileFromView() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteDeviceProfileFromViewBtnIsNotDisplayed());
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }
    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove the device profile by clicking on the trash icon in the right side of device profile without refresh")
    public void removeDeviceProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }
}
