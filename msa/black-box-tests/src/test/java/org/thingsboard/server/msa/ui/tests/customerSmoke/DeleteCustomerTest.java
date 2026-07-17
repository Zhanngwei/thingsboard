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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

/**
 * 中文说明：
 * 1. `DeleteCustomerTest` 是 ThingsBoard Microservices 中验证 `DeleteCustomer` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDriverBaseTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class DeleteCustomerTest extends AbstractDriverBaseTest {

    /**
     * `sideBarMenuView` 字段，保存当前对象的对应属性。
     */
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    /**
     * `ruleChainsPage` 字段，保存当前对象的对应属性。
     */
    private RuleChainsPageHelper ruleChainsPage;

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
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    /**
     * 功能：删除或清理客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the customer by clicking on the trash icon in the right side of refresh")
    public void removeCustomerByRightSideBtn() {
        String customer = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customer));

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(deletedCustomer));
    }

    /**
     * 功能：删除或清理客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove customer by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedCustomer() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteSelected(customerName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedCustomer));
    }

    /**
     * 功能：删除或清理客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer by clicking on the 'Delete customer' btn in the entity view")
    public void removeFromCustomerView() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.customerViewDeleteBtn());
        customerPage.warningPopUpYesBtn().click();
        jsClick(customerPage.refreshBtn());

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(customerName));
    }

    /**
     * 功能：删除或清理客户。
     * 参数：无。
     * 返回：无。
     */
    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer by clicking on the trash icon in the right side of customer without refresh")
    public void removeCustomerByRightSideBtnWithoutRefresh() {
        String customer = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customer));

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(deletedCustomer));
    }
}
