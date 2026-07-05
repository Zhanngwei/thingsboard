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
package org.thingsboard.server.msa.ui.tests.alarmassignee;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.msa.ui.pages.AssetPageHelper;
import org.thingsboard.server.msa.ui.pages.EntityViewPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

/**
 * 中文说明：
 * 1. 类目的：`AssignDetailsTabAssignTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
 */
@Feature("Assign from details tab of entity (by tenant)")
public class AssignDetailsTabAssignTest extends AbstractAssignTest {

    /**
     * 资产ID，用于定位对应业务对象。
     */
    private AssetId assetId;
    private AlarmId propageteAlarmId;
    /**
     * 告警ID，用于定位对应业务对象。
     */
    private AlarmId propageteAssigneAlarmId;
    private AlarmId customerAlarmId;
    /**
     * 资产ID，用于定位对应业务对象。
     */
    private AlarmId assetAlarmId;
    private AlarmId entityViewAlarmId;
    /**
     * 实体视图ID，用于定位对应业务对象。
     */
    private EntityViewId entityViewId;
    private String assetName;
    /**
     * 实体视图，用于标识或展示当前对象。
     */
    private String entityViewName;
    private String propagateAlarmType;
    /**
     * 告警，用于区分不同处理分支。
     */
    private String propagateAssignedAlarmType;
    private String customerAlarmType;
    /**
     * 资产，用于区分不同处理分支。
     */
    private String assetAlarmType;
    private String entityViewAlarmType;
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    private AssetPageHelper assetPage;
    private EntityViewPageHelper entityViewPage;

    /**
     * 功能：执行 `generateTestEntities` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void generateTestEntities() {
        assetPage = new AssetPageHelper(driver);
        entityViewPage = new EntityViewPageHelper(driver);

        customerAlarmType = "Test customer alarm" + random();
        assetAlarmType = "Test asset alarm" + random();
        entityViewAlarmType = "Test entity view alarm" + random();
        propagateAlarmType = "Test propagated alarm " + random();
        propagateAssignedAlarmType = "Test propagated alarm " + random();

        customerAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(customerId, customerAlarmType)).getId();
        assetId = testRestClient.postAsset(EntityPrototypes.defaultAssetPrototype("Asset", customerId)).getId();
        assetName = testRestClient.getAssetById(assetId).getName();
        assetAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(assetId, assetAlarmType)).getId();
        entityViewId = testRestClient.postEntityView(EntityPrototypes.defaultEntityViewPrototype("Entity view", "", "DEVICE")).getId();
        entityViewName = testRestClient.getEntityViewById(entityViewId).getName();
        entityViewAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(entityViewId, entityViewAlarmType)).getId();
    }

    /**
     * 功能：执行 `generateTestAlarms` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void generateTestAlarms() {
        propageteAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAlarmType, true)).getId();
        propageteAssigneAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAssignedAlarmType, userId, true)).getId();
    }

    /**
     * 功能：删除或清理`Test Entities`。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void deleteTestEntities() {
        deleteAlarmById(customerAlarmId);
        deleteAlarmById(assetAlarmId);
        deleteAlarmById(entityViewAlarmId);
        deleteAssetById(assetId);
        deleteEntityView(entityViewId);
    }

    /**
     * 功能：删除或清理`Test Alarms`。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void deleteTestAlarms() {
        deleteAlarmsByIds(propageteAlarmId, propageteAssigneAlarmId);
    }

    /**
     * 功能：执行 `alarms` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public Object[][] alarms() {
        return new Object[][]{
                {alarmType},
                {propagateAlarmType}};
    }

    /**
     * 功能：执行 `assignedAlarms` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public Object[][] assignedAlarms() {
        return new Object[][]{
                {assignedAlarmType},
                {propagateAssignedAlarmType}};
    }

    /**
     * 功能：执行 `assignAlarmToYourself` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：无。
     */
    @Description("Can assign alarm to yourself/Can assign propagate alarm to yourself")
    @Test(dataProvider = "alarms")
    public void assignAlarmToYourself(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `assignAlarmToAnotherUser` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：无。
     */
    @Description("Can assign alarm to another user/Can assign propagate alarm to another user")
    @Test(dataProvider = "alarms")
    public void assignAlarmToAnotherUser(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    /**
     * 功能：执行 `unassignedAlarm` 对应的处理。
     * 参数：
     * - `assignedAlarm`：`assignedAlarm` 参数。
     * 返回：无。
     */
    @Description("Can unassign alarm/Can unassign propagate alarm")
    @Test(dataProvider = "assignedAlarms")
    public void unassignedAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarm));
    }

    /**
     * 功能：执行 `reassignAlarm` 对应的处理。
     * 参数：
     * - `assignedAlarm`：`assignedAlarm` 参数。
     * 返回：无。
     */
    @Description("Can reassign alarm to another user/Can reassign propagate alarm to another user")
    @Test(dataProvider = "assignedAlarms")
    public void reassignAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：获取邮箱。
     * 参数：无。
     * 返回：无。
     */
    @Description("Search by email")
    @Test
    public void searchByEmail() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarmType, Const.TENANT_EMAIL);
        alarmPage.setUsers();

        assertThat(alarmPage.getUsers()).hasSize(1).as("Search result contains search input").contains(Const.TENANT_EMAIL);
        alarmPage.assignUsers().forEach(this::assertIsDisplayed);
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：无。
     */
    @Description("Search by name")
    @Test
    public void searchByName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarmType, userName);

        assertIsDisplayed(alarmPage.noUsersFoundMessage());
    }

    /**
     * 功能：执行 `assignAlarmToYourselfFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Assign alarm to yourself from details of alarm")
    @Test
    public void assignAlarmToYourselfFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(alarmType).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `assignAlarmToAnotherUserFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Assign alarm to another user from details of alarm")
    @Test
    public void assignAlarmToAnotherUserFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(alarmType).click();
        alarmDetailsView.assignAlarmTo(userEmail);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    /**
     * 功能：执行 `unassignedAlarmFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Unassign alarm from details of alarm")
    @Test
    public void unassignedAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.unassignedAlarm();
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    /**
     * 功能：执行 `reassignAlarmFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Reassign alarm to another user from details of alarm")
    @Test
    public void reassignAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `assignCustomerAlarmToYourself` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Assign alarm to yourself for Customer entity details")
    @Test
    public void assignCustomerAlarmToYourself() {
        sideBarMenuView.customerBtn().click();
        customerPage.openCustomerAlarms(customerTitle);
        alarmPage.assignAlarmTo(customerAlarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `assignAssetAlarmToYourself` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Assign alarm to yourself for Asset details")
    @Test
    public void assignAssetAlarmToYourself() {
        sideBarMenuView.goToAssetsPage();
        assetPage.openAssetAlarms(assetName);
        alarmPage.assignAlarmTo(assetAlarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `assignEntityViewsAlarmToYourself` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Assign alarm to yourself for Entity view details")
    @Test
    public void assignEntityViewsAlarmToYourself() {
        sideBarMenuView.goToEntityViewsPage();
        entityViewPage.openEntityViewAlarms(entityViewName);
        alarmPage.assignAlarmTo(entityViewAlarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AssignDetailsTabAssignTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
