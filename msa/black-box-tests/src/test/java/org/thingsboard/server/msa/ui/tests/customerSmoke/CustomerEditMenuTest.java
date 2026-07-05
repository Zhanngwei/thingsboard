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
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PHONE_NUMBER_ERROR_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

/**
 * 中文说明：
 * 1. 类目的：`CustomerEditMenuTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
 */
public class CustomerEditMenuTest extends AbstractDriverBaseTest {

    /**
     * `sideBarMenuView` 字段，保存当前对象的对应属性。
     */
    private SideBarMenuViewElements sideBarMenuView;
    private LoginPageHelper loginPage;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    private CustomerPageHelper customerPage;
    private DashboardPageHelper dashboardPage;
    /**
     * 客户，用于标识或展示当前对象。
     */
    private String customerName;

    /**
     * 功能：执行 `login` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void login() {
        loginPage = new LoginPageHelper(driver);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
        dashboardPage = new DashboardPageHelper(driver);
        loginPage.authorizationTenant();
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    /**
     * 功能：执行 `reLogin` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void reLogin() {
        if (getJwtTokenFromLocalStorage() == null) {
            loginPage.authorizationTenant();
        }
    }

    /**
     * 功能：执行 `changeTitle` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Change title by edit menu")
    public void changeTitle() {
        String customerName = "Changed" + getRandomNumber();
        testRestClient.postCustomer(defaultCustomerPrototype(ENTITY_NAME + random()));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.setHeaderName();
        String titleBefore = customerPage.getHeaderName();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(customerName);
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();
        String titleAfter = customerPage.getHeaderName();

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, customerName);
    }

    /**
     * 功能：删除或清理`Title`。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Delete title and save")
    public void deleteTitle() {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    /**
     * 功能：保存或创建`Only With Space`。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space in title")
    public void saveOnlyWithSpace() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(" ");
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertEquals(customerPage.getCustomerName(), customerPage.getHeaderName());
    }

    /**
     * 功能：执行 `editDescription` 对应的处理。
     * 参数：
     * - `description`：`description` 参数。
     * - `newDescription`：`newDescription` 参数。
     * - `finalDescription`：`finalDescription` 参数。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(name, description));
        customerName = name;

        sideBarMenuView.customerBtn().click();
        customerPage.entity(name).click();
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(newDescription);
        customerPage.doneBtnEditView().click();
        customerPage.setDescription();

        Assert.assertEquals(customerPage.getDescription(), finalDescription);
    }

    /**
     * 功能：执行 `assignedDashboardFromDashboard` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Assigned dashboard from dashboards page")
    public void assignedDashboardFromDashboard() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.dashboardBtn().click();
        dashboardPage.setDashboardTitle();
        dashboardPage.assignedBtn(dashboardPage.getDashboardTitle()).click();
        dashboardPage.assignedCustomer(customerName);
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.editPencilBtn());
        customerPage.chooseDashboard(dashboardPage.getDashboardTitle());
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        jsClick(customerPage.manageCustomersUserBtn(customerName));
        customerPage.createCustomersUser();
        jsClick(customerPage.userLoginBtn());

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboardFromView(), dashboardPage.getDashboardTitle());
    }

    /**
     * 功能：执行 `assignedDashboard` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Assigned dashboard")
    public void assignedDashboard() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(customerName).click();
        customerPage.assignedDashboard();
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.editPencilBtn());
        customerPage.chooseDashboard(customerPage.getDashboard());
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        jsClick(customerPage.manageCustomersUserBtn(customerName));
        customerPage.createCustomersUser();
        jsClick(customerPage.userLoginBtn());

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboard(), customerPage.getDashboardFromView());
    }

    /**
     * 功能：执行 `assignedDashboardWithoutHide` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = { "smoke", "broken" })
    @Description("Assigned dashboard without hide")
    public void assignedDashboardWithoutHide() {
        String customerName = ENTITY_NAME + random();
        String dashboardName = "Firmware";
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(customerName).click();
        customerPage.assignedDashboard(dashboardName);
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.editPencilBtn());
        customerPage.chooseDashboard(dashboardName);
        customerPage.disableHideHomeDashboardToolbar();
        customerPage.doneBtnEditView().click();
        customerPage.waitUntilDashboardFieldToBeNotEmpty();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        jsClick(customerPage.manageCustomersUserBtn(customerName));
        customerPage.createCustomersUser();
        jsClick(customerPage.userLoginBtn());

        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(dashboardName, customerPage.getDashboardFromView());
        Assert.assertNotNull(customerPage.stateController());
        Assert.assertNotNull(customerPage.filterBtn());
        Assert.assertNotNull(customerPage.timeBtn());
        Assert.assertTrue(customerPage.stateController().isDisplayed());
        Assert.assertTrue(customerPage.filterBtn().isDisplayed());
        Assert.assertTrue(customerPage.timeBtn().isDisplayed());
    }

    /**
     * 功能：保存或创建`Phone Number`。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add phone number")
    public void addPhoneNumber() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.doneBtnEditView().click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").contains(number));
    }

    /**
     * 功能：保存或创建`Incorrect Phone Number`。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description("Add incorrect phone number")
    public void addIncorrectPhoneNumber(String number) {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.enterPhoneNumber(number);

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), PHONE_NUMBER_ERROR_MESSAGE);
    }

    /**
     * 功能：保存或创建`All Information`。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 30, groups = "smoke")
    @Description("Add all information")
    public void addAllInformation() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String text = "Text";
        String email = "email@mail.com";
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.selectCountryEntityView();
        customerPage.descriptionEntityView().sendKeys(text);
        customerPage.cityEntityView().sendKeys(text);
        customerPage.stateEntityView().sendKeys(text);
        customerPage.zipEntityView().sendKeys(text);
        customerPage.addressEntityView().sendKeys(text);
        customerPage.address2EntityView().sendKeys(text);
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.emailEntityView().sendKeys(email);
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+1" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
    }

    /**
     * 功能：删除或清理`Phone Number`。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(groups = "smoke")
    @Description("Delete phone number")
    public void deletePhoneNumber() {
        String customerName = ENTITY_NAME;
        int number = 2015550123;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName, number));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().click();
        customerPage.phoneNumberEntityView().sendKeys(Keys.CONTROL + "A" + Keys.BACK_SPACE);
        customerPage.doneBtnEditView().click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").isEmpty(), "Phone field is empty");
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CustomerEditMenuTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
