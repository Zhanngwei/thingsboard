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
 * 1. 类目的：`AssignFromAlarmWidgetTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
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

/*
 * 本类总结：
 * 1. 核心职责：`AssignFromAlarmWidgetTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
