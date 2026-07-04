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
     * 字段说明：
     * 1. 保存 `sideBarMenuView` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private SideBarMenuViewElements sideBarMenuView;
    private LoginPageHelper loginPage;
    /**
     * 字段说明：
     * 1. 保存 `customerPage` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private CustomerPageHelper customerPage;
    private DashboardPageHelper dashboardPage;
    /**
     * 字段说明：
     * 1. 保存 `customerName` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private String customerName;

    @BeforeClass
    /**
     * 方法说明：
     * 1. 职责：执行 `login` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void login() {
        loginPage = new LoginPageHelper(driver);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
        dashboardPage = new DashboardPageHelper(driver);
        loginPage.authorizationTenant();
    }

    @AfterMethod
    /**
     * 方法说明：
     * 1. 职责：执行 `delete` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void delete() {
        // 条件分支用于保护配置、连接状态、测试前置条件或协议状态机边界。
        if (customerName != null) {
            // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @BeforeMethod
    /**
     * 方法说明：
     * 1. 职责：执行 `reLogin` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void reLogin() {
        // 条件分支用于保护配置、连接状态、测试前置条件或协议状态机边界。
        if (getJwtTokenFromLocalStorage() == null) {
            loginPage.authorizationTenant();
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Change title by edit menu")
    /**
     * 方法说明：
     * 1. 职责：执行 `changeTitle` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void changeTitle() {
        String customerName = "Changed" + getRandomNumber();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        testRestClient.postCustomer(defaultCustomerPrototype(ENTITY_NAME + random()));
        this.customerName = customerName;

        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        sideBarMenuView.customerBtn().click();
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.entityTitles().get(0).click();
        customerPage.setHeaderName();
        String titleBefore = customerPage.getHeaderName();
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(customerName);
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();
        String titleAfter = customerPage.getHeaderName();

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, customerName);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Delete title and save")
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTitle` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void deleteTitle() {
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        sideBarMenuView.customerBtn().click();
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.entityTitles().get(0).click();
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space in title")
    /**
     * 方法说明：
     * 1. 职责：执行 `saveOnlyWithSpace` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void saveOnlyWithSpace() {
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.entityTitles().get(0).click();
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(" ");
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertEquals(customerPage.getCustomerName(), customerPage.getHeaderName());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    /**
     * 方法说明：
     * 1. 职责：执行 `editDescription` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Assigned dashboard from dashboards page")
    /**
     * 方法说明：
     * 1. 职责：执行 `assignedDashboardFromDashboard` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Assigned dashboard")
    /**
     * 方法说明：
     * 1. 职责：执行 `assignedDashboard` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = { "smoke", "broken" })
    @Description("Assigned dashboard without hide")
    /**
     * 方法说明：
     * 1. 职责：执行 `assignedDashboardWithoutHide` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add phone number")
    /**
     * 方法说明：
     * 1. 职责：执行 `addPhoneNumber` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description("Add incorrect phone number")
    /**
     * 方法说明：
     * 1. 职责：执行 `addIncorrectPhoneNumber` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 30, groups = "smoke")
    @Description("Add all information")
    /**
     * 方法说明：
     * 1. 职责：执行 `addAllInformation` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(groups = "smoke")
    @Description("Delete phone number")
    /**
     * 方法说明：
     * 1. 职责：执行 `deletePhoneNumber` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
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
