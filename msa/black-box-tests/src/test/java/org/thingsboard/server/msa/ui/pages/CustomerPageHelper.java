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
package org.thingsboard.server.msa.ui.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * 中文说明：
 * 1. `CustomerPageHelper` 是 ThingsBoard Microservices 中处理客户通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `CustomerPageElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class CustomerPageHelper extends CustomerPageElements {
    /**
     * 功能：创建 `CustomerPageHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public CustomerPageHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 客户，用于标识或展示当前对象。
     */
    private String customerName;
    private String country;
    /**
     * 仪表盘对象，用于描述当前业务场景。
     */
    private String dashboard;
    private String dashboardFromView;
    /**
     * 描述信息，用于展示或标识当前对象。
     */
    private String description;
    private String customerEmail;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    private String customerCountry;
    private String customerCity;

    /**
     * 功能：更新客户。
     * 参数：无。
     * 返回：无。
     */
    public void setCustomerName() {
        this.customerName = entityTitles().get(0).getText();
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void setCustomerName(int number) {
        this.customerName = entityTitles().get(number).getText();
    }

    /**
     * 功能：获取客户。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * 功能：更新`Country`。
     * 参数：无。
     * 返回：无。
     */
    public void setCountry() {
        this.country = countries().get(0).getText();
    }

    /**
     * 功能：获取`Country`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCountry() {
        return country;
    }

    /**
     * 功能：更新仪表盘。
     * 参数：无。
     * 返回：无。
     */
    public void setDashboard() {
        this.dashboard = listOfEntity().get(0).getText();
    }

    /**
     * 功能：更新仪表盘。
     * 参数：无。
     * 返回：无。
     */
    public void setDashboardFromView() {
        this.dashboardFromView = editMenuDashboardField().getAttribute("value");
    }

    /**
     * 功能：更新描述信息。
     * 参数：无。
     * 返回：无。
     */
    public void setDescription() {
        scrollToElement(descriptionEntityView());
        this.description = descriptionEntityView().getAttribute("value");
    }

    /**
     * 功能：获取仪表盘。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDashboard() {
        return dashboard;
    }

    /**
     * 功能：获取仪表盘。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDashboardFromView() {
        return dashboardFromView;
    }

    /**
     * 功能：获取描述信息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：无。
     */
    public void setCustomerEmail(String title) {
        this.customerEmail = email(title).getText();
    }

    /**
     * 功能：获取客户。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCustomerEmail() {
        return customerEmail;
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：无。
     */
    public void setCustomerCountry(String title) {
        this.customerCountry = country(title).getText();
    }

    /**
     * 功能：获取客户。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCustomerCountry() {
        return customerCountry;
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：无。
     */
    public void setCustomerCity(String title) {
        this.customerCity = city(title).getText();
    }

    /**
     * 功能：获取客户。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCustomerCity() {
        return customerCity;
    }

    /**
     * 功能：执行 `changeTitleEditMenu` 对应的处理。
     * 参数：
     * - `newTitle`：`newTitle` 参数。
     * 返回：无。
     */
    public void changeTitleEditMenu(String newTitle) {
        titleFieldEntityView().click();
        titleFieldEntityView().clear();
        wait.until(ExpectedConditions.textToBe(By.xpath(String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE)), ""));
        titleFieldEntityView().sendKeys(newTitle);
    }

    /**
     * 功能：执行 `chooseDashboard` 对应的处理。
     * 参数：
     * - `dashboardName`：名称。
     * 返回：无。
     */
    public void chooseDashboard(String dashboardName) {
        editMenuDashboardField().click();
        editMenuDashboard(dashboardName).click();
    }

    /**
     * 功能：保存或创建用户。
     * 参数：无。
     * 返回：无。
     */
    public void createCustomersUser() {
        plusBtn().click();
        addUserEmailField().click();
        addUserEmailField().sendKeys(getRandomNumber() + "@gmail.com");
        addBtnC().click();
        activateWindowOkBtn().click();
    }

    /**
     * 功能：执行 `selectCountryEntityView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void selectCountryEntityView() {
        countrySelectMenuEntityView().click();
        setCountry();
        countries().get(0).click();
    }

    /**
     * 功能：执行 `selectCountryAddEntityView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void selectCountryAddEntityView() {
        countrySelectMenuAddEntityView().click();
        setCountry();
        countries().get(0).click();
    }

    /**
     * 功能：执行 `assignedDashboard` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void assignedDashboard() {
        plusBtn().click();
        assignedField().click();
        setDashboard();
        listOfEntity().get(0).click();
        assignedField().sendKeys(Keys.ESCAPE);
        submitAssignedBtn().click();
    }

    /**
     * 功能：执行 `assignedDashboard` 对应的处理。
     * 参数：
     * - `dashboardName`：名称。
     * 返回：无。
     */
    public void assignedDashboard(String dashboardName) {
        plusBtn().click();
        assignedField().click();
        entityFromList(dashboardName).click();
        assignedField().sendKeys(Keys.ESCAPE);
        submitAssignedBtn().click();
    }

    /**
     * 功能：执行 `customerIsNotPresent` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：判断结果。
     */
    public boolean customerIsNotPresent(String title) {
        return elementsIsNotPresent(getEntity(title));
    }

    /**
     * 功能：执行 `sortByNameDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void sortByNameDown() {
        doubleClick(sortByTitleBtn());
    }

    /**
     * 功能：保存或创建客户。
     * 参数：
     * - `keysToEnter`：键。
     * 返回：无。
     */
    public void addCustomerViewEnterName(CharSequence keysToEnter) {
        enterText(titleFieldAddEntityView(), keysToEnter);
    }

    /**
     * 功能：执行 `enterPhoneNumber` 对应的处理。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void enterPhoneNumber(String number) {
        phoneNumberEntityView().sendKeys(number);
        phoneNumberEntityView().sendKeys(Keys.TAB);
    }

    /**
     * 功能：执行 `openCustomerAlarms` 对应的处理。
     * 参数：
     * - `customerName`：名称。
     * 返回：无。
     */
    public void openCustomerAlarms(String customerName) {
        if (!customerDetailsView().isDisplayed()) {
            customer(customerName).click();
        }
        customerDetailsAlarmsBtn().click();
    }

    /**
     * 功能：执行 `disableHideHomeDashboardToolbar` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void disableHideHomeDashboardToolbar() {
        hideHomeDashboardToolbarCheckbox().click();
        waitUntilAttributeToBe("//mat-checkbox[@formcontrolname='homeDashboardHideToolbar']//input", "class", "mdc-checkbox__native-control");
    }

    /**
     * 功能：执行 `waitUntilDashboardFieldToBeNotEmpty` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void waitUntilDashboardFieldToBeNotEmpty() {
        waitUntilAttributeToBeNotEmpty(editMenuDashboardField(), "value");
    }
}
