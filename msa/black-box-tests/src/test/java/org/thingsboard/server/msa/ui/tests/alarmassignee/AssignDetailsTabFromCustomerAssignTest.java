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
 * 1. 类目的：`AssignDetailsTabFromCustomerAssignTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
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

/*
 * 本类总结：
 * 1. 核心职责：`AssignDetailsTabFromCustomerAssignTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
