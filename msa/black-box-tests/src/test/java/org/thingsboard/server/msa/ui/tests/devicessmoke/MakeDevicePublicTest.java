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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.tabs.AssignDeviceTabHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

/**
 * 中文说明：
 * 1. `MakeDevicePublicTest` 是 ThingsBoard Microservices 中验证 `MakeDevicePublic` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDeviceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Make device public")
public class MakeDevicePublicTest extends AbstractDeviceTest {

    /**
     * 客户对象，用于描述当前业务场景。
     */
    private CustomerPageHelper customerPage;
    private AssignDeviceTabHelper assignDeviceTab;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private String deviceName1;

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void createFirstDevice() {
        customerPage = new CustomerPageHelper(driver);
        assignDeviceTab = new AssignDeviceTabHelper(driver);

        deviceName1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random())).getName();
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void cleanUp() {
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
        deleteDeviceByName(deviceName1);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void createSecondDevice() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random())).getName();
    }

    /**
     * 功能：执行 `makeDevicePublicByRightSideBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke", priority = 10)
    @Description("Make device public by right side btn")
    public void makeDevicePublicByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.makeDevicePublicByRightSideBtn(deviceName);

        assertIsDisplayed(devicePage.deviceIsPublicCheckbox(deviceName));
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer in customer column is Public customer")
                .isEqualTo(PUBLIC_CUSTOMER_NAME);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    /**
     * 功能：执行 `makeDevicePublicFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke", priority = 10)
    @Description("Make device public by btn on details tab")
    public void makeDevicePublicFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.device(deviceName).click();
        devicePage.makeDevicePublicFromDetailsTab();
        devicePage.closeDeviceDetailsViewBtn().click();

        assertIsDisplayed(devicePage.deviceIsPublicCheckbox(deviceName));
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer in customer column is Public customer")
                .isEqualTo(PUBLIC_CUSTOMER_NAME);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    /**
     * 功能：执行 `makeDevicePublicByAssignToPublicCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke", priority = 20)
    @Description("Make device public by assign to public customer")
    public void makeDevicePublicByAssignToPublicCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignBtn(deviceName).click();
        assignDeviceTab.assignOnCustomer(PUBLIC_CUSTOMER_NAME);
        assertIsDisplayed(devicePage.deviceIsPublicCheckbox(deviceName));
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText()).isEqualTo(PUBLIC_CUSTOMER_NAME);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    /**
     * 功能：执行 `makePublicSeveralDevicesByAssignOnPublicCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke", priority = 20)
    @Description("Make several devices public by assign to public customer")
    public void makePublicSeveralDevicesByAssignOnPublicCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignSelectedDevices(deviceName, deviceName1);
        assignDeviceTab.assignOnCustomer(PUBLIC_CUSTOMER_NAME);
        assertIsDisplayed(devicePage.deviceIsPublicCheckbox(deviceName));
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer in customer column is Public customer")
                .isEqualTo(PUBLIC_CUSTOMER_NAME);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        assertIsDisplayed(devicePage.device(deviceName));
        assertIsDisplayed(devicePage.device(deviceName1));
    }
}
