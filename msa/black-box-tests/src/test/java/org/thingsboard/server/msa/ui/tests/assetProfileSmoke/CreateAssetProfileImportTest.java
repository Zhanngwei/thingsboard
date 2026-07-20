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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_ASSET_PROFILE_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_ASSET_PROFILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE;

/**
 * 中文说明：
 * 1. `CreateAssetProfileImportTest` 是 ThingsBoard Microservices 中验证 `CreateAssetProfileImport` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class CreateAssetProfileImportTest extends AbstractDriverBaseTest {

    /**
     * `sideBarMenuView` 字段，保存当前对象的对应属性。
     */
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private final String absolutePathToFileImportAssetProfile = getClass().getClassLoader().getResource(IMPORT_ASSET_PROFILE_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();
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
            testRestClient.deleteAssetProfile(getAssetProfileByName(name).getId());
            name = null;
        }
    }

    /**
     * 功能：执行 `importAssetProfile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import asset profile")
    public void importAssetProfile() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.importBrowseFileBtn().click();
        name = IMPORT_ASSET_PROFILE_NAME;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME));
        Assert.assertTrue(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME).isDisplayed());
    }

    /**
     * 功能：执行 `importTxtFile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import txt file")
    public void importTxtFile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openImportDeviceProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        Assert.assertNotNull(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    /**
     * 功能：保存或创建文件。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Drop json file and delete it")
    public void addFileToImportAndRemove() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.clearImportFileBtn().click();

        Assert.assertNotNull(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(IMPORT_ASSET_PROFILE_NAME));
    }

    /**
     * 功能：执行 `importAssetProfileWithSameName` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import asset profile with same name")
    public void importAssetProfileWithSameName() {
        String name = IMPORT_ASSET_PROFILE_NAME;
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.importBrowseFileBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE);
    }

    /**
     * 功能：执行 `importAssetProfileWithoutRefresh` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import asset profile without refresh")
    public void importAssetProfileWithoutRefresh() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.importBrowseFileBtn().click();
        name = IMPORT_ASSET_PROFILE_NAME;

        Assert.assertNotNull(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME));
        Assert.assertTrue(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME).isDisplayed());
    }
}
