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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

/**
 * 中文说明：
 * 1. 类目的：`MakeDevicePrivateTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
 */
@Feature("Make device private")
public class MakeDevicePrivateTest extends AbstractDeviceTest {

    /**
     * 客户对象，用于描述当前业务场景。
     */
    private CustomerPageHelper customerPage;

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void createPublicDevice() {
        customerPage = new CustomerPageHelper(driver);
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random()));
        testRestClient.setDevicePublic(device.getId());
        deviceName = device.getName();
    }

    /**
     * 功能：删除或清理客户。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void deletePublicCustomer() {
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
    }

    /**
     * 功能：执行 `makeDevicePrivateByRightSideBtn` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Make device private by right side btn")
    public void makeDevicePrivateByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.makeDevicePrivateByRightSideBtn(deviceName);
        WebElement customerInColumn = devicePage.deviceCustomerOnPage(deviceName);
        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceName));
        assertInvisibilityOfElement(customerInColumn);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    /**
     * 功能：执行 `makeDevicePrivateFromDetailsTab` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Make device public by btn on details tab")
    public void makeDevicePrivateFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.device(deviceName).click();
        WebElement customerInColumn = devicePage.deviceCustomerOnPage(deviceName);
        devicePage.makeDevicePrivateFromDetailsTab();
        devicePage.closeDeviceDetailsViewBtn().click();
        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceName));
        assertInvisibilityOfElement(customerInColumn);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MakeDevicePrivateTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
