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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultAssetProfile;

/**
 * 中文说明：
 * 1. `SortByNameTest` 是 ThingsBoard Microservices 中验证 `SortByName` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class SortByNameTest extends AbstractDriverBaseTest {
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
            testRestClient.deleteAssetProfile(getAssetProfileByName(name).getId());
            name = null;
        }
    }

    /**
     * 功能：执行 `specialCharacterUp` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort asset profile 'UP'")
    public void specialCharacterUp(String name) {
        testRestClient.postAssetProfile(defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName();

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    /**
     * 功能：执行 `allSortUp` 对应的处理。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `assetProfileSymbol`：`assetProfileSymbol` 参数。
     * - `assetProfileNumber`：`assetProfileNumber` 参数。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort asset profile 'UP'")
    public void allSortUp(String assetProfile, String assetProfileSymbol, String assetProfileNumber) {
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileSymbol));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfile));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileNumber));

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName(0);
        String firstAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(1);
        String secondAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(2);
        String thirdAssetProfile = profilesPage.getProfileName();

        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfile).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileNumber).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileSymbol).getId());

        Assert.assertEquals(firstAssetProfile, assetProfileSymbol);
        Assert.assertEquals(secondAssetProfile, assetProfileNumber);
        Assert.assertEquals(thirdAssetProfile, assetProfile);
    }

    /**
     * 功能：执行 `specialCharacterDown` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort asset profile 'DAWN'")
    public void specialCharacterDown(String name) {
        testRestClient.postAssetProfile(defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(profilesPage.allEntity().size() - 1);

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    /**
     * 功能：执行 `allSortDown` 对应的处理。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `assetProfileSymbol`：`assetProfileSymbol` 参数。
     * - `assetProfileNumber`：`assetProfileNumber` 参数。
     * 返回：无。
     */
    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort asset profile 'DOWN'")
    public void allSortDown(String assetProfile, String assetProfileSymbol, String assetProfileNumber) {
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileSymbol));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfile));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileNumber));

        sideBarMenuView.openAssetProfiles();
        int lastIndex = profilesPage.allEntity().size() - 1;
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(lastIndex);
        String firstAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 1);
        String secondAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 2);
        String thirdAssetProfile = profilesPage.getProfileName();

        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfile).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileNumber).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileSymbol).getId());

        Assert.assertEquals(firstAssetProfile, assetProfileSymbol);
        Assert.assertEquals(secondAssetProfile, assetProfileNumber);
        Assert.assertEquals(thirdAssetProfile, assetProfile);
    }
}
