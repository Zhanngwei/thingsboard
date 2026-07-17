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
 * 1. `AbstractAssignTest` 是 ThingsBoard Microservices 中验证 `AbstractAssign` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
