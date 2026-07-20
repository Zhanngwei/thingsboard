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
 * 1. `AssignDetailsTabAssignTest` 是 ThingsBoard Microservices 中验证 `AssignDetailsTabAssign` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractAssignTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
