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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

/**
 * 中文说明：
 * 1. `SortByNameTest` 是 ThingsBoard Microservices 中验证 `SortByName` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class SortByNameTest extends AbstractDriverBaseTest {
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
     * 功能：执行 `specialCharacterUp` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'UP'")
    public void specialCharacterUp(String title) {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        this.customerName = title;

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName();

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    /**
     * 功能：执行 `allSortUp` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * - `customerSymbol`：`customerSymbol` 参数。
     * - `customerNumber`：`customerNumber` 参数。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'UP'")
    public void allSortUp(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postCustomer(defaultCustomerPrototype(customerSymbol));
        testRestClient.postCustomer(defaultCustomerPrototype(customer));
        testRestClient.postCustomer(defaultCustomerPrototype(customerNumber));

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName(0);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(2);
        String thirdCustomer = customerPage.getCustomerName();

        testRestClient.deleteCustomer(getCustomerByName(customer).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerNumber).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerSymbol).getId());

        Assert.assertEquals(firstCustomer, customerSymbol);
        Assert.assertEquals(secondCustomer, customerNumber);
        Assert.assertEquals(thirdCustomer, customer);
    }

    /**
     * 功能：执行 `specialCharacterDown` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'DOWN'")
    public void specialCharacterDown(String title) {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        customerName = title;

        sideBarMenuView.customerBtn().click();
        customerPage.sortByNameDown();
        customerPage.setCustomerName(customerPage.allEntity().size() - 1);

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    /**
     * 功能：执行 `allSortDown` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * - `customerSymbol`：`customerSymbol` 参数。
     * - `customerNumber`：`customerNumber` 参数。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'DOWN'")
    public void allSortDown(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postCustomer(defaultCustomerPrototype(customerSymbol));
        testRestClient.postCustomer(defaultCustomerPrototype(customer));
        testRestClient.postCustomer(defaultCustomerPrototype(customerNumber));

        sideBarMenuView.customerBtn().click();
        int lastIndex = customerPage.allEntity().size() - 1;
        customerPage.sortByNameDown();
        customerPage.setCustomerName(lastIndex);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 2);
        String thirdCustomer = customerPage.getCustomerName();

        testRestClient.deleteCustomer(getCustomerByName(customer).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerNumber).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerSymbol).getId());

        Assert.assertEquals(firstCustomer, customerSymbol);
        Assert.assertEquals(secondCustomer, customerNumber);
        Assert.assertEquals(thirdCustomer, customer);
    }
}
