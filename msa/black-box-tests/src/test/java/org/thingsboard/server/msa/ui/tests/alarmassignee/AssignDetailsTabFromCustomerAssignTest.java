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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

/**
 * 中文说明：
 * 1. `AssignDetailsTabFromCustomerAssignTest` 是 ThingsBoard Microservices 中验证 `AssignDetailsTabFromCustomerAssign` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractAssignTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Assign from details tab of entity (by customer)")
public class AssignDetailsTabFromCustomerAssignTest extends AbstractAssignTest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private AlarmId tenantAlarmId;
    private DeviceId tenantDeviceId;
    /**
     * 租户，用于标识或展示当前对象。
     */
    private String tenantDeviceName;
    private String tenantAlarmType;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private AlarmId assignedTenantAlarmId;

    /**
     * 功能：执行 `generateTenantEntity` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void generateTenantEntity() {
        if (getJwtTokenFromLocalStorage() == null) {
            new LoginPageHelper(driver).authorizationTenant();
        }
        tenantAlarmType = "Test tenant alarm " + random();

        tenantDeviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("")).getName();
        tenantDeviceId = testRestClient.getDeviceByName(tenantDeviceName).getId();
        tenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(tenantDeviceId, tenantAlarmType)).getId();
    }

    /**
     * 功能：删除或清理租户。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void deleteTenantEntity() {
        deleteAlarmsByIds(tenantAlarmId, assignedTenantAlarmId);
        deleteDeviceById(tenantDeviceId);
        clearStorage();
    }

    /**
     * 功能：执行 `assignAlarmToYourselfCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Can assign alarm to yourself")
    @Test
    public void assignAlarmToYourselfCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarmType, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    /**
     * 功能：执行 `reassignAlarmByCustomerFromAnotherCustomerUser` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Can reassign alarm from himself to another customer user")
    @Test
    public void reassignAlarmByCustomerFromAnotherCustomerUser() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarmType, userName);

        assertIsDisplayed(alarmPage.assignedUser(userName));
    }

    /**
     * 功能：执行 `unassignedAlarmFromCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Can unassign alarm from himself")
    @Test
    public void unassignedAlarmFromCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    /**
     * 功能：执行 `unassignedAlarmFromAnotherUserFromCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Unassign alarm from any other customer user")
    @Test
    public void unassignedAlarmFromAnotherUserFromCustomer() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    /**
     * 功能：执行 `unassignedAlarmFromTenant` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Unassign alarm from any tenant user")
    @Test
    public void unassignedAlarmFromTenant() {
        String assignedTenantAlarmType = "Test tenant assigned alarm " + random();
        assignedTenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedTenantAlarmType)).getId();

        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedTenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedTenantAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedTenantAlarmType));
    }

    /**
     * 功能：校验客户。
     * 参数：无。
     * 返回：无。
     */
    @Description("Check the display of names (emails)")
    @Test
    public void checkTheDisplayOfNamesEmailsFromCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        devicePage.assignToCustomerBtn(tenantDeviceName).click();
        devicePage.assignToCustomer(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `reassignTenantForOldAlarm` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Check the reassign tenant for old alarm on device")
    @Test
    public void reassignTenantForOldAlarm() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        devicePage.assignToCustomerBtn(tenantDeviceName).click();
        devicePage.assignToCustomer(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        jsClick(alarmPage.assignBtn(tenantAlarmType));

        assertIsDisplayed(alarmPage.accessForbiddenDialogView());
    }

    /**
     * 功能：执行 `reassignTenantForOldAlarmFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Check the reassign tenant for old alarm on device")
    @Test
    public void reassignTenantForOldAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        devicePage.assignToCustomerBtn(tenantDeviceName).click();
        devicePage.assignToCustomer(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.alarmDetailsBtn(tenantAlarmType).click();


        assertIsDisplayed(alarmPage.accessForbiddenDialogView());
    }
}
