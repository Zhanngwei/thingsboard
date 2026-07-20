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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.msa.ui.pages.AlarmWidgetElements;
import org.thingsboard.server.msa.ui.pages.CreateWidgetPopupHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `AssignFromAlarmWidgetTest` 是 ThingsBoard Microservices 中验证 `AssignFromAlarmWidget` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractAssignTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Assign from details tab of entity")
public class AssignFromAlarmWidgetTest extends AbstractAssignTest {

    /**
     * 仪表盘对象，用于描述当前业务场景。
     */
    private Dashboard dashboard;
    private DashboardPageHelper dashboardPage;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private AlarmWidgetElements alarmWidget;

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void create() {
        dashboardPage = new DashboardPageHelper(driver);
        CreateWidgetPopupHelper createWidgetPopup = new CreateWidgetPopupHelper(driver);
        alarmWidget = new AlarmWidgetElements(driver);

        dashboard = testRestClient.postDashboard(EntityPrototypes.defaultDashboardPrototype("Dashboard"));
        sideBarMenuView.dashboardBtn().click();
        dashboardPage.entity(dashboard.getName()).click();
        dashboardPage.editBtn().click();
        dashboardPage.openSelectWidgetsBundleMenu();
        dashboardPage.openCreateWidgetPopup();
        createWidgetPopup.goToCreateEntityAliasPopup("Alias");
        createWidgetPopup.selectFilterType("Single entity");
        createWidgetPopup.selectType("Device");
        createWidgetPopup.selectEntity(deviceName);
        createWidgetPopup.addAliasBtn().click();
        createWidgetPopup.addWidgetBtn().click();
        dashboardPage.increaseSizeOfTheWidget();
        dashboardPage.saveBtn().click();
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void delete() {
        deleteDashboardById(dashboard.getId());
    }

    /**
     * 功能：执行 `goToDashboardPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void goToDashboardPage() {
        sideBarMenuView.dashboardBtn().click();
        dashboardPage.entity(dashboard.getName()).click();
    }

    /**
     * 功能：执行 `assignAlarmToYourself` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Can assign alarm to yourself")
    @Test
    public void assignAlarmToYourself() {
        alarmWidget.assignAlarmTo(alarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmWidget.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `reassignAlarm` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Can reassign alarm to another user")
    @Test
    public void reassignAlarm() {
        alarmWidget.assignAlarmTo(assignedAlarmType, userWithNameEmail);

        assertIsDisplayed(alarmWidget.assignedUser(userName));
    }

    /**
     * 功能：执行 `unassignedAlarm` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Can unassign alarm")
    @Test
    public void unassignedAlarm() {
        alarmWidget.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmWidget.unassigned(assignedAlarmType));
    }

    /**
     * 功能：执行 `assignAlarmToYourselfFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Assign alarm to yourself from details of alarm")
    @Test
    public void assignAlarmToYourselfFromDetails() {
        alarmWidget.alarmDetailsBtn(alarmType).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmWidget.assignedUser(Const.TENANT_EMAIL));
    }

    /**
     * 功能：执行 `reassignAlarmFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Reassign alarm to another user from details of alarm")
    @Test
    public void reassignAlarmFromDetails() {
        alarmWidget.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.assignAlarmTo(userWithNameEmail);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmWidget.assignedUser(userName));
    }

    /**
     * 功能：执行 `unassignedAlarmFromDetails` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Description("Unassign alarm from details of alarm")
    @Test
    public void unassignedAlarmFromDetails() {
        alarmWidget.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.unassignedAlarm();
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmWidget.unassigned(assignedAlarmType));
    }

    /**
     * 功能：获取邮箱。
     * 参数：无。
     * 返回：无。
     */
    @Description("Search by email")
    @Test
    public void searchByEmail() {
        alarmWidget.searchAlarm(alarmType, Const.TENANT_EMAIL);
        alarmWidget.setUsers();

        assertThat(alarmWidget.getUsers()).hasSize(1).as("Search result contains search input").contains(Const.TENANT_EMAIL);
        alarmWidget.assignUsers().forEach(this::assertIsDisplayed);
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：无。
     */
    @Description("Search by name")
    @Test
    public void searchByName() {
        alarmWidget.searchAlarm(alarmType, userName);

        assertIsDisplayed(alarmWidget.noUsersFoundMessage());
    }
}
