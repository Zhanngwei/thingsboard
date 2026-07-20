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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `DeviceProfileEditMenuTest` 是 ThingsBoard Microservices 中验证 `DeviceProfileEditMenu` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class DeviceProfileEditMenuTest extends AbstractDriverBaseTest {

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
     * 功能：执行 `changeName` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String name = ENTITY_NAME + random();
        String newName = "Changed" + getRandomNumber();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        profilesPage.setHeaderName();
        String titleBefore = profilesPage.getHeaderName();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.changeNameEditMenu(newName);
        profilesPage.doneBtnEditView().click();
        this.name = newName;
        profilesPage.setHeaderName();
        String titleAfter = profilesPage.getHeaderName();

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, newName);
    }

    /**
     * 功能：删除或清理名称。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.changeNameEditMenu("");

        Assert.assertFalse(profilesPage.doneBtnEditViewVisible().isEnabled());
    }

    /**
     * 功能：保存或创建名称。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space in name")
    public void saveWithOnlySpaceInName() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.changeNameEditMenu(Keys.SPACE);
        profilesPage.doneBtnEditView().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_DEVICE_PROFILE_MESSAGE);
    }

    /**
     * 功能：执行 `editDescription` 对应的处理。
     * 参数：
     * - `description`：`description` 参数。
     * - `newDescription`：`newDescription` 参数。
     * - `finalDescription`：`finalDescription` 参数。
     * 返回：无。
     */
    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 30, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name, description));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.profileViewDescriptionField().sendKeys(newDescription);
        profilesPage.doneBtnEditView().click();
        profilesPage.setDescription();

        Assert.assertEquals(profilesPage.getDescription(), finalDescription);
    }
}
