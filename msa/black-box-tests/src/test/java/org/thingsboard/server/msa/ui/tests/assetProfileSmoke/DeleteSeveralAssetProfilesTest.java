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
package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

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
 * 1. `DeleteSeveralAssetProfilesTest` 是 ThingsBoard Microservices 中验证 `DeleteSeveralAssetProfiles` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class DeleteSeveralAssetProfilesTest extends AbstractDriverBaseTest {
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
     * 功能：执行 `canDeleteSeveralAssetProfilesByTopBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Delete several asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove several asset profiles by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void canDeleteSeveralAssetProfilesByTopBtn() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    /**
     * 功能：执行 `selectAllDAssetProfiles` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Delete several asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove several asset profiles by mark all the asset profiles on the page by clicking in the topmost " +
            "checkbox and then clicking on the trash icon in the menu that appears")
    public void selectAllDAssetProfiles() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() +"2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.selectAllCheckBox().click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    /**
     * 功能：删除或清理资产配置。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Delete several asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default asset profile by mark all the asset profiles on the page by clicking in the " +
            "topmost checkbox and then clicking on the trash icon in the menu that appears")
    public void removeDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.selectAllCheckBox().click();

        Assert.assertFalse(profilesPage.checkBoxIsDisplayed("default"));
        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    /**
     * 功能：删除或清理资产。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Delete several asset profile")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove several asset profiles by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top without refresh")
    public void deleteSeveralAssetProfilesByTopBtnWithoutRefresh() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }
}
