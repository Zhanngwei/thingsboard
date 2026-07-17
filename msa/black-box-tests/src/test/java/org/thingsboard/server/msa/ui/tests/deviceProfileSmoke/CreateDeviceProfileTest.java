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
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.NAME_IS_REQUIRED_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE;

/**
 * 中文说明：
 * 1. `CreateDeviceProfileTest` 是 ThingsBoard Microservices 中验证 `CreateDeviceProfile` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class CreateDeviceProfileTest extends AbstractDriverBaseTest {

    /**
     * `sideBarMenuView` 字段，保存当前对象的对应属性。
     */
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    /**
     * 名称，用于标识或展示当前对象。
     */
    private String name;

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
     * 功能：执行 `delete` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void delete() {
        if (name != null) {
            testRestClient.deleteDeviseProfile(getDeviceProfileByName(name).getId());
            name = null;
        }
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Add device profile after specifying the name (text/numbers /special characters)")
    public void createDeviceProfile() {
        String name = ENTITY_NAME + random();

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.createDeviceProfileEnterName(name);
        profilesPage.addDeviceProfileAddBtn().click();
        profilesPage.refreshBtn().click();
        this.name = name;

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Add device profile after specifying the name with details")
    public void createDeviceProfileWithDetails() {
        String name = ENTITY_NAME + random();
        String ruleChain = "Root Rule Chain";
        String mobileDashboard = "Firmware";
        String queue = "Main";
        String description = "Description";

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.createDeviceProfileEnterName(name);
        profilesPage.addDeviceProfileViewChooseRuleChain(ruleChain);
        profilesPage.addDeviceProfileViewChooseMobileDashboard(mobileDashboard);
        profilesPage.addDeviceProfileViewChooseQueue(queue);
        profilesPage.addDeviceProfileViewEnterDescription(description);
        profilesPage.addDeviceProfileAddBtn().click();
        profilesPage.refreshBtn().click();
        this.name = name;
        profilesPage.entity(name).click();
        profilesPage.setName();
        profilesPage.setRuleChain();
        profilesPage.setMobileDashboard();
        profilesPage.setQueue();
        profilesPage.setDescription();

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
        Assert.assertEquals(name, profilesPage.getName());
        Assert.assertEquals(ruleChain, profilesPage.getRuleChain());
        Assert.assertEquals(mobileDashboard, profilesPage.getMobileDashboard());
        Assert.assertEquals(queue, profilesPage.getQueue());
        Assert.assertEquals(description, profilesPage.getDescription());
    }

    /**
     * 功能：保存或创建配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Create a device profile with the same name")
    public void createDeviseProfileWithSameName() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.createDeviceProfileEnterName(name);
        profilesPage.addDeviceProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addDeviceProfileView());
        Assert.assertTrue(profilesPage.addDeviceProfileView().isDisplayed());
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Add device profile without the name")
    public void createDeviceProfileWithoutName() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.addDeviceProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.addDeviceProfileView());
        Assert.assertTrue(profilesPage.addDeviceProfileView().isDisplayed());
        Assert.assertNotNull(profilesPage.errorMessage());
        Assert.assertEquals(profilesPage.errorMessage().getText(), NAME_IS_REQUIRED_MESSAGE);
    }

    /**
     * 功能：保存或创建配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Create device profile only with spase in name")
    public void createDeviseProfileWithOnlySpaceInName() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.createDeviceProfileEnterName(Keys.SPACE);
        profilesPage.addDeviceProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_DEVICE_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addDeviceProfileView());
        Assert.assertTrue(profilesPage.addDeviceProfileView().isDisplayed());
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Add device profile after specifying the name (text/numbers /special characters) without refresh")
    public void createDeviceProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.createDeviceProfileEnterName(name);
        profilesPage.addDeviceProfileAddBtn().click();
        this.name = name;

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    /**
     * 功能：执行 `documentation` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Create device profile")
    @Test(priority = 30, groups = "smoke")
    @Description("Go to device profile documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/device-profiles/";

        sideBarMenuView.openDeviceProfiles();
        profilesPage.profileNames().get(0).click();
        profilesPage.goToProfileHelpPage();

        Assert.assertTrue(urlContains(urlPath), "URL contains " + urlPath);
    }
}
