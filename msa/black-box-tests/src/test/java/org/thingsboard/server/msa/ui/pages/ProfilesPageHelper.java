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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * 中文说明：
 * 1. 类目的：`ProfilesPageHelper` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
public class ProfilesPageHelper extends ProfilesPageElements {
    /**
     * 功能：创建 `ProfilesPageHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public ProfilesPageHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String name;
    private String ruleChain;
    /**
     * 仪表盘对象，用于描述当前业务场景。
     */
    private String mobileDashboard;
    private String queue;
    /**
     * 描述信息，用于展示或标识当前对象。
     */
    private String description;
    private String profile;

    /**
     * 功能：更新名称。
     * 参数：无。
     * 返回：无。
     */
    public void setName() {
        this.name = profileViewNameField().getAttribute("value");
    }

    /**
     * 功能：更新规则链。
     * 参数：无。
     * 返回：无。
     */
    public void setRuleChain() {
        this.ruleChain = profileViewRuleChainField().getAttribute("value");
    }

    /**
     * 功能：更新仪表盘。
     * 参数：无。
     * 返回：无。
     */
    public void setMobileDashboard() {
        this.mobileDashboard = profileViewMobileDashboardField().getAttribute("value");
    }

    /**
     * 功能：更新队列。
     * 参数：无。
     * 返回：无。
     */
    public void setQueue() {
        this.queue = profileViewQueueField().getAttribute("value");
    }

    /**
     * 功能：更新描述信息。
     * 参数：无。
     * 返回：无。
     */
    public void setDescription() {
        scrollToElement(profileViewDescriptionField());
        this.description = profileViewDescriptionField().getAttribute("value");
    }

    /**
     * 功能：更新配置。
     * 参数：无。
     * 返回：无。
     */
    public void setProfileName() {
        this.profile = profileNames().get(0).getText();
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void setProfileName(int number) {
        this.profile = profileNames().get(number).getText();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getRuleChain() {
        return this.ruleChain;
    }

    /**
     * 功能：获取仪表盘。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getMobileDashboard() {
        return this.mobileDashboard;
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getQueue() {
        return this.queue;
    }

    /**
     * 功能：获取描述信息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getProfileName() {
        return this.profile;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `keysToEnter`：键。
     * 返回：无。
     */
    public void createDeviceProfileEnterName(CharSequence keysToEnter) {
        enterText(addDeviceProfileNameField(), keysToEnter);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：无。
     */
    public void addDeviceProfileViewChooseRuleChain(String ruleChain) {
        addDeviceProfileRuleChainField().click();
        entityFromList(ruleChain).click();
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：无。
     */
    public void addAssetProfileViewChooseRuleChain(String ruleChain) {
        addAssetProfileRuleChainField().click();
        entityFromList(ruleChain).click();
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `mobileDashboard`：`mobileDashboard` 参数。
     * 返回：无。
     */
    public void addDeviceProfileViewChooseMobileDashboard(String mobileDashboard) {
        addDeviceProfileMobileDashboardField().click();
        entityFromList(mobileDashboard).click();
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `mobileDashboard`：`mobileDashboard` 参数。
     * 返回：无。
     */
    public void addAssetProfileViewChooseMobileDashboard(String mobileDashboard) {
        addAssetProfileMobileDashboardField().click();
        entityFromList(mobileDashboard).click();
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    public void addDeviceProfileViewChooseQueue(String queue) {
        addDeviceProfileQueueField().click();
        entityFromList(queue).click();
        waitUntilAttributeContains(addDeviceProfileQueueField(), "aria-expanded", "false");
    }

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    public void addAssetsProfileViewChooseQueue(String queue) {
        addAssetProfileQueueField().click();
        entityFromList(queue).click();
        waitUntilAttributeContains(addAssetProfileQueueField(), "aria-expanded", "false");
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `description`：`description` 参数。
     * 返回：无。
     */
    public void addDeviceProfileViewEnterDescription(String description) {
        addDeviceDescriptionField().sendKeys(description);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `description`：`description` 参数。
     * 返回：无。
     */
    public void addAssetProfileViewEnterDescription(String description) {
        addAssetDescriptionField().sendKeys(description);
    }

    /**
     * 功能：执行 `openCreateDeviceProfileView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openCreateDeviceProfileView() {
        plusBtn().click();
        createNewDeviceProfileBtn().click();
    }

    /**
     * 功能：执行 `openCreateAssetProfileView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openCreateAssetProfileView() {
        plusBtn().click();
        createNewAssetProfileBtn().click();
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    public void addAssetProfileViewEnterName(String name) {
        addAssetProfileNameField().click();
        addAssetProfileNameField().sendKeys(name);
    }

    /**
     * 功能：执行 `openImportDeviceProfileView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openImportDeviceProfileView() {
        plusBtn().click();
        importDeviceProfileBtn().click();
    }

    /**
     * 功能：执行 `openImportAssetProfileView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openImportAssetProfileView() {
        plusBtn().click();
        importAssetProfileBtn().click();
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean deleteDeviceProfileFromViewBtnIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getDeviseProfileViewDeleteBtn())));
    }

    /**
     * 功能：删除或清理资产配置。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean deleteAssetProfileFromViewBtnIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getAssetProfileViewDeleteBtn())));
    }

    /**
     * 功能：执行 `goToProfileHelpPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void goToProfileHelpPage() {
        jsClick(helpBtn());
        goToNextTab(2);
    }

    /**
     * 功能：执行 `sortByNameDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void sortByNameDown() {
        doubleClick(sortByNameBtn());
    }

    /**
     * 功能：执行 `profileIsNotPresent` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：判断结果。
     */
    public boolean profileIsNotPresent(String name) {
        return elementsIsNotPresent(getEntity(name));
    }

    /**
     * 功能：校验`Box Is Displayed`。
     * 参数：
     * - `name`：名称。
     * 返回：判断结果。
     */
    public boolean checkBoxIsDisplayed(String name) {
        return waitUntilPresenceOfElementLocated(getCheckbox(name)).isDisplayed();
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`ProfilesPageHelper` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
