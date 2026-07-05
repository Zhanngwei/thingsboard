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

import io.qameta.allure.Epic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsEntityTabHelper;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsViewHelper;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

/**
 * 中文说明：
 * 1. 类目的：`AbstractAssignTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
 */
@Epic("Alarm assign")
abstract public class AbstractAssignTest extends AbstractDriverBaseTest {

    /**
     * 告警ID，用于定位对应业务对象。
     */
    protected AlarmId alarmId;
    protected AlarmId assignedAlarmId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    protected DeviceId deviceId;
    protected UserId userId;
    /**
     * 用户ID，用于定位对应业务对象。
     */
    protected UserId userWithNameId;
    protected CustomerId customerId;
    /**
     * 设备，用于标识或展示当前对象。
     */
    protected String deviceName;
    protected String userName;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    protected String customerTitle;
    protected String userEmail;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    protected String userWithNameEmail;
    protected String alarmType;
    /**
     * 告警，用于区分不同处理分支。
     */
    protected String assignedAlarmType;
    protected SideBarMenuViewHelper sideBarMenuView;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    protected AlarmDetailsEntityTabHelper alarmPage;
    protected DevicePageHelper devicePage;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    protected CustomerPageHelper customerPage;
    protected AlarmDetailsViewHelper alarmDetailsView;

    /**
     * 功能：执行 `generateCommonTestEntity` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void generateCommonTestEntity() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        alarmPage = new AlarmDetailsEntityTabHelper(driver);
        devicePage = new DevicePageHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        alarmDetailsView = new AlarmDetailsViewHelper(driver);

        userName = "User " + random();
        customerTitle = "Customer " + random();
        userEmail = random() + "@thingsboard.org";
        userWithNameEmail = random() + "@thingsboard.org";
        alarmType = "Test alarm " + random();
        assignedAlarmType = "Test assigned alarm " + random();

        customerId = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(customerTitle)).getId();
        userId = testRestClient.postUser(EntityPrototypes.defaultUser(userEmail, getCustomerByName(customerTitle).getId())).getId();
        userWithNameId = testRestClient.postUser(EntityPrototypes.defaultUser(userWithNameEmail, getCustomerByName(customerTitle).getId(), userName)).getId();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device ", customerId)).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
    }

    /**
     * 功能：删除或清理`Common Entities`。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void deleteCommonEntities() {
        deleteCustomerById(customerId);
        deleteDeviceById(deviceId);
    }

    /**
     * 功能：保存或创建`Common Test Alarms`。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void createCommonTestAlarms() {
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, alarmType)).getId();
        assignedAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedAlarmType, userId)).getId();
    }

    /**
     * 功能：删除或清理`Common Created Alarms`。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void deleteCommonCreatedAlarms() {
        deleteAlarmsByIds(alarmId, assignedAlarmId);
    }

    /**
     * 功能：执行 `loginByUser` 对应的处理。
     * 参数：
     * - `userEmail`：`userEmail` 参数。
     * 返回：无。
     */
    public void loginByUser(String userEmail) {
        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersUserBtn(customerTitle).click();
        customerPage.getUserLoginBtnByEmail(userEmail).click();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractAssignTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
