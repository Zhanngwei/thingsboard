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
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_CUSTOMER_MESSAGE;

/**
 * 中文说明：
 * 1. `CreateCustomerTest` 是 ThingsBoard Microservices 中验证 `CreateCustomer` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class CreateCustomerTest extends AbstractDriverBaseTest {

    /**
     * `sideBarMenuView` 字段，保存当前对象的对应属性。
     */
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
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
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
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
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer specifying the name (text/numbers /special characters)")
    public void createCustomer() {
        String customerName = ENTITY_NAME + random();

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.refreshBtn().click();

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer after specifying the name (text/numbers /special characters) with full information")
    public void createCustomerWithFullInformation() {
        String customerName = ENTITY_NAME + random();
        String text = "Text";
        String email = "email@mail.com";
        String number = "12015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.selectCountryAddEntityView();
        customerPage.descriptionAddEntityView().sendKeys(text);
        customerPage.cityAddEntityView().sendKeys(text);
        customerPage.stateAddEntityView().sendKeys(text);
        customerPage.zipAddEntityView().sendKeys(text);
        customerPage.addressAddEntityView().sendKeys(text);
        customerPage.address2AddEntityView().sendKeys(text);
        customerPage.phoneNumberAddEntityView().sendKeys(number);
        customerPage.emailAddEntityView().sendKeys(email);
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.setCustomerEmail(customerName);
        customerPage.setCustomerCountry(customerName);
        customerPage.setCustomerCity(customerName);
        customerPage.entity(customerName).click();

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertEquals(customerPage.entityViewTitle().getText(), customerName);
        Assert.assertEquals(customerPage.titleFieldEntityView().getAttribute("value"), customerName);
        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
        Assert.assertEquals(customerPage.getCustomerEmail(), email);
        Assert.assertEquals(customerPage.getCustomerCountry(), customerPage.getCountry());
        Assert.assertEquals(customerPage.getCustomerCity(), text);
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer without the name")
    public void createCustomerWithoutName() {
        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();

        Assert.assertFalse(customerPage.addBtnV().isEnabled());
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Create customer only with spase in name")
    public void createCustomerWithOnlySpace() {
        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(Keys.SPACE);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Create a customer with the same name")
    public void createCustomerSameName() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        String customerName = customerPage.getCustomerName();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer specifying the name (text/numbers /special characters) without refresh")
    public void createCustomerWithoutRefresh() {
        String customerName = ENTITY_NAME + random();

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    /**
     * 功能：执行 `documentation` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(priority = 40, groups = "smoke")
    @Description("Go to customer documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/customers/";

        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.customer(customerPage.getCustomerName()).click();
        customerPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath), "URL contains " + urlPath);
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Create customer")
    @Test(groups = "smoke")
    @Description("Go to customer documentation page")
    public void createCustomerAddAndRemovePhoneNumber() {
        String customerName = ENTITY_NAME;
        String number = "12015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.addCustomerViewEnterName(customerName);
        customerPage.enterText(customerPage.phoneNumberAddEntityView(), number);
        customerPage.clearInputField(customerPage.phoneNumberAddEntityView());
        customerPage.addBtnC().click();
        this.customerName = customerName;
        customerPage.entity(customerName).click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").isEmpty(), "Phone field is empty");
    }
}
