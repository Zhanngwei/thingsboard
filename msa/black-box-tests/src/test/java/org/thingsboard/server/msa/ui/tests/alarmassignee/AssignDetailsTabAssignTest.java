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
package org.thingsboard.server.msa.ui.tests.alarmassignee;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.msa.ui.pages.AssetPageHelper;
import org.thingsboard.server.msa.ui.pages.EntityViewPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

@Feature("Assign from details tab of entity (by tenant)")
/**
 * 中文说明：
 * 1. 类目的：`AssignDetailsTabAssignTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
 */
public class AssignDetailsTabAssignTest extends AbstractAssignTest {

    /**
     * 字段说明：
     * 1. 保存 `assetId` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private AssetId assetId;
    private AlarmId propageteAlarmId;
    /**
     * 字段说明：
     * 1. 保存 `propageteAssigneAlarmId` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private AlarmId propageteAssigneAlarmId;
    private AlarmId customerAlarmId;
    /**
     * 字段说明：
     * 1. 保存 `assetAlarmId` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private AlarmId assetAlarmId;
    private AlarmId entityViewAlarmId;
    /**
     * 字段说明：
     * 1. 保存 `entityViewId` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private EntityViewId entityViewId;
    private String assetName;
    /**
     * 字段说明：
     * 1. 保存 `entityViewName` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private String entityViewName;
    private String propagateAlarmType;
    /**
     * 字段说明：
     * 1. 保存 `propagateAssignedAlarmType` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private String propagateAssignedAlarmType;
    private String customerAlarmType;
    /**
     * 字段说明：
     * 1. 保存 `assetAlarmType` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private String assetAlarmType;
    private String entityViewAlarmType;
    /**
     * 字段说明：
     * 1. 保存 `assetPage` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private AssetPageHelper assetPage;
    private EntityViewPageHelper entityViewPage;

    @BeforeClass
    /**
     * 方法说明：
     * 1. 职责：执行 `generateTestEntities` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void generateTestEntities() {
        assetPage = new AssetPageHelper(driver);
        entityViewPage = new EntityViewPageHelper(driver);

        customerAlarmType = "Test customer alarm" + random();
        assetAlarmType = "Test asset alarm" + random();
        entityViewAlarmType = "Test entity view alarm" + random();
        propagateAlarmType = "Test propagated alarm " + random();
        propagateAssignedAlarmType = "Test propagated alarm " + random();

        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        customerAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(customerId, customerAlarmType)).getId();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        assetId = testRestClient.postAsset(EntityPrototypes.defaultAssetPrototype("Asset", customerId)).getId();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        assetName = testRestClient.getAssetById(assetId).getName();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        assetAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(assetId, assetAlarmType)).getId();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        entityViewId = testRestClient.postEntityView(EntityPrototypes.defaultEntityViewPrototype("Entity view", "", "DEVICE")).getId();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        entityViewName = testRestClient.getEntityViewById(entityViewId).getName();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        entityViewAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(entityViewId, entityViewAlarmType)).getId();
    }

    @BeforeMethod
    /**
     * 方法说明：
     * 1. 职责：执行 `generateTestAlarms` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void generateTestAlarms() {
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        propageteAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAlarmType, true)).getId();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        propageteAssigneAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAssignedAlarmType, userId, true)).getId();
    }

    @AfterClass
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTestEntities` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void deleteTestEntities() {
        deleteAlarmById(customerAlarmId);
        deleteAlarmById(assetAlarmId);
        deleteAlarmById(entityViewAlarmId);
        deleteAssetById(assetId);
        deleteEntityView(entityViewId);
    }

    @AfterMethod
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTestAlarms` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void deleteTestAlarms() {
        deleteAlarmsByIds(propageteAlarmId, propageteAssigneAlarmId);
    }

    @DataProvider
    /**
     * 方法说明：
     * 1. 职责：执行 `alarms` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public Object[][] alarms() {
        return new Object[][]{
                {alarmType},
                {propagateAlarmType}};
    }

    @DataProvider
    /**
     * 方法说明：
     * 1. 职责：执行 `assignedAlarms` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public Object[][] assignedAlarms() {
        return new Object[][]{
                {assignedAlarmType},
                {propagateAssignedAlarmType}};
    }

    @Description("Can assign alarm to yourself/Can assign propagate alarm to yourself")
    @Test(dataProvider = "alarms")
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAlarmToYourself` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignAlarmToYourself(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Can assign alarm to another user/Can assign propagate alarm to another user")
    @Test(dataProvider = "alarms")
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAlarmToAnotherUser` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignAlarmToAnotherUser(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Description("Can unassign alarm/Can unassign propagate alarm")
    @Test(dataProvider = "assignedAlarms")
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignedAlarm` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void unassignedAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarm));
    }

    @Description("Can reassign alarm to another user/Can reassign propagate alarm to another user")
    @Test(dataProvider = "assignedAlarms")
    /**
     * 方法说明：
     * 1. 职责：执行 `reassignAlarm` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void reassignAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Search by email")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `searchByEmail` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void searchByEmail() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarmType, Const.TENANT_EMAIL);
        alarmPage.setUsers();

        assertThat(alarmPage.getUsers()).hasSize(1).as("Search result contains search input").contains(Const.TENANT_EMAIL);
        alarmPage.assignUsers().forEach(this::assertIsDisplayed);
    }

    @Description("Search by name")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `searchByName` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void searchByName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarmType, userName);

        assertIsDisplayed(alarmPage.noUsersFoundMessage());
    }

    @Description("Assign alarm to yourself from details of alarm")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAlarmToYourselfFromDetails` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignAlarmToYourselfFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        alarmPage.alarmDetailsBtn(alarmType).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Assign alarm to another user from details of alarm")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAlarmToAnotherUserFromDetails` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignAlarmToAnotherUserFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        alarmPage.alarmDetailsBtn(alarmType).click();
        alarmDetailsView.assignAlarmTo(userEmail);
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Description("Unassign alarm from details of alarm")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignedAlarmFromDetails` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void unassignedAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        // Selenium 操作依赖页面实时状态，通常需要等待元素可见或可点击后再继续断言。
        alarmPage.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.unassignedAlarm();
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    @Description("Reassign alarm to another user from details of alarm")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `reassignAlarmFromDetails` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void reassignAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarmType).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Assign alarm to yourself for Customer entity details")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `assignCustomerAlarmToYourself` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignCustomerAlarmToYourself() {
        sideBarMenuView.customerBtn().click();
        customerPage.openCustomerAlarms(customerTitle);
        alarmPage.assignAlarmTo(customerAlarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Assign alarm to yourself for Asset details")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `assignAssetAlarmToYourself` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignAssetAlarmToYourself() {
        sideBarMenuView.goToAssetsPage();
        assetPage.openAssetAlarms(assetName);
        alarmPage.assignAlarmTo(assetAlarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Description("Assign alarm to yourself for Entity view details")
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `assignEntityViewsAlarmToYourself` 对应的MSA UI 黑盒测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void assignEntityViewsAlarmToYourself() {
        sideBarMenuView.goToEntityViewsPage();
        entityViewPage.openEntityViewAlarms(entityViewName);
        alarmPage.assignAlarmTo(entityViewAlarmType, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AssignDetailsTabAssignTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
