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
 * 1. 类目的：`CreateAssetProfileImportTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
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

/*
 * 本类总结：
 * 1. 核心职责：`CreateAssetProfileImportTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
