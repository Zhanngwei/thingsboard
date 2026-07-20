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

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;

/**
 * 中文说明：
 * 1. `DeleteSeveralDeviceProfilesTest` 是 ThingsBoard Microservices 中验证 `DeleteSeveralDeviceProfiles` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class DeleteSeveralDeviceProfilesTest extends AbstractDriverBaseTest {
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
     * 功能：执行 `canDeleteSeveralDeviceProfilesByTopBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove several device profiles by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void canDeleteSeveralDeviceProfilesByTopBtn() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postDeviceProfile(defaultDeviceProfile(name1));
        testRestClient.postDeviceProfile(defaultDeviceProfile(name2));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    /**
     * 功能：执行 `selectAllDeviceProfiles` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove several device profiles by mark all the device profiles on the page by clicking in the topmost checkbox and then clicking on the trash icon in the menu that appears")
    public void selectAllDeviceProfiles() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.selectAllCheckBox().click();
        profilesPage.deleteSelectedBtn().click();

        Assert.assertNotNull(profilesPage.warningPopUpTitle());
        Assert.assertTrue(profilesPage.warningPopUpTitle().isDisplayed());
        Assert.assertTrue(profilesPage.warningPopUpTitle().getText().contains(String.valueOf(profilesPage.markCheckbox().size())));
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default device profile by mark all the device profiles on the page by clicking in the topmost checkbox and then clicking on the trash icon in the menu that appears")
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.selectAllCheckBox().click();

        Assert.assertFalse(profilesPage.checkBoxIsDisplayed("default"));
        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove several device profiles by mark in the checkbox and then click on the trash can icon in the menu that appears at the top without refresh")
    public void deleteSeveralDeviceProfilesByTopBtnWithoutRefresh() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postDeviceProfile(defaultDeviceProfile(name1));
        testRestClient.postDeviceProfile(defaultDeviceProfile(name2));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }
}
