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
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.tabs.AssignDeviceTabHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

/**
 * 中文说明：
 * 1. `AssignToCustomerTest` 是 ThingsBoard Microservices 中验证 `AssignToCustomer` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDeviceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Assign to customer")
public class AssignToCustomerTest extends AbstractDeviceTest {

    /**
     * 设备对象，用于描述当前业务场景。
     */
    private AssignDeviceTabHelper assignDeviceTab;
    private CustomerPageHelper customerPage;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    private CustomerId customerId;
    private Device device;
    /**
     * `device1` 字段，保存当前对象的对应属性。
     */
    private Device device1;
    private String customerName;

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void create() {
        assignDeviceTab = new AssignDeviceTabHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        Customer customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customerId = customer.getId();
        customerName = customer.getName();
        device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device " + random()));
    }

    /**
     * 功能：删除或清理客户。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void deleteCustomer() {
        deleteCustomerById(customerId);
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
        deleteDeviceByName(device1.getName());
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void createDevice() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    /**
     * 功能：执行 `assignToCustomerByRightSideBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Assign to customer by right side of device btn")
    public void assignToCustomerByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignBtn(deviceName).click();
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    /**
     * 功能：执行 `assignToCustomerFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Assign to customer by 'Assign to customer' btn on details tab")
    public void assignToCustomerFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.device(deviceName).click();
        devicePage.assignBtnDetailsTab().click();
        assignDeviceTab.assignOnCustomer(customerName);
        String customerInAssignedField = devicePage.assignFieldDetailsTab().getAttribute("value");
        devicePage.closeDeviceDetailsViewBtn().click();
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);
        assertThat(customerInAssignedField)
                .as("Customer in details tab added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    /**
     * 功能：执行 `assignToCustomerMarkedDevice` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Assign marked device by btn on the top")
    public void assignToCustomerMarkedDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignSelectedDevices(deviceName);
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    /**
     * 功能：执行 `unassignedFromCustomerByRightSideBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Unassign from customer by right side of device btn")
    public void unassignedFromCustomerByRightSideBtn() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);

        sideBarMenuView.goToDevicesPage();
        WebElement element = devicePage.deviceCustomerOnPage(deviceName);
        devicePage.unassignedDeviceByRightSideBtn(deviceName);
        assertInvisibilityOfElement(element);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    /**
     * 功能：执行 `unassignedFromCustomerFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Unassign from customer by 'Unassign from customer' btn on details tab")
    public void unassignedFromCustomerFromDetailsTab() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);

        sideBarMenuView.goToDevicesPage();
        WebElement customerInColumn = devicePage.deviceCustomerOnPage(deviceName);
        devicePage.device(deviceName).click();
        WebElement assignFieldDetailsTab = devicePage.assignFieldDetailsTab();
        devicePage.unassignedDeviceFromDetailsTab();
        assertInvisibilityOfElement(customerInColumn);
        assertInvisibilityOfElement(assignFieldDetailsTab);

        devicePage.closeDeviceDetailsViewBtn().click();
        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    /**
     * 功能：执行 `assignToSeveralCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Can't assign device on several customer")
    public void assignToSeveralCustomer() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);
        sideBarMenuView.goToDevicesPage();

        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    /**
     * 功能：执行 `assignPublicDevice` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Can't assign public device")
    public void assignPublicDevice() {
        testRestClient.setDevicePublic(device.getId());

        sideBarMenuView.goToDevicesPage();
        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    /**
     * 功能：执行 `assignSeveralDevices` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Assign several devices by btn on the top")
    public void assignSeveralDevices() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignSelectedDevices(deviceName, device1.getName());
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);
        assertThat(devicePage.deviceCustomerOnPage(device1.getName()).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        List.of(deviceName, device1.getName()).
                forEach(d -> assertIsDisplayed(devicePage.device(d)));
    }
}
