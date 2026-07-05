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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

/**
 * 中文说明：
 * 1. 类目的：`SideBarMenuViewElements` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
public class SideBarMenuViewElements extends AbstractBasePage {
    /**
     * 功能：创建 `SideBarMenuViewElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public SideBarMenuViewElements(WebDriver driver) {
        super(driver);
    }

    /**
     * `RULE_CHAINS_BTN`常量，用于统一引用固定值。
     */
    private static final String RULE_CHAINS_BTN = "//mat-toolbar//a[@href='/ruleChains']";
    private static final String CUSTOMER_BTN = "//mat-toolbar//a[@href='/customers']";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    private static final String DASHBOARD_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String PROFILES_DROPDOWN = "//mat-toolbar//mat-icon[text()='badge']/ancestor::a//span[contains(@class,'pull-right')]";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/deviceProfiles']";
    private static final String ASSET_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/assetProfiles']";
    /**
     * `ALARMS_BTN`常量，用于统一引用固定值。
     */
    private static final String ALARMS_BTN = "//mat-toolbar//a[@href='/alarms']";
    private static final String ENTITIES_DROPDOWN = "//mat-toolbar//mat-icon[text()='category']/ancestor::a//span[contains(@class,'pull-right')]";
    /**
     * `DEVICES_BTN`常量，用于统一引用固定值。
     */
    private static final String DEVICES_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Devices']";
    private static final String ASSETS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Assets']";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String ENTITY_VIEWS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Entity Views']";

    /**
     * 功能：执行 `entitiesDropdown` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entitiesDropdown() {
        return waitUntilElementToBeClickable(ENTITIES_DROPDOWN);
    }

    /**
     * 功能：执行 `ruleChainsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    /**
     * 功能：执行 `customerBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement customerBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_BTN);
    }

    /**
     * 功能：执行 `dashboardBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement dashboardBtn() {
        return waitUntilElementToBeClickable(DASHBOARD_BTN);
    }

    /**
     * 功能：执行 `profilesDropdown` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement profilesDropdown() {
        return waitUntilElementToBeClickable(PROFILES_DROPDOWN);
    }

    /**
     * 功能：执行 `deviceProfileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement deviceProfileBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_BTN);
    }

    /**
     * 功能：执行 `assetProfileBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assetProfileBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_BTN);
    }

    /**
     * 功能：执行 `alarmsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmsBtn() {
        return waitUntilElementToBeClickable(ALARMS_BTN);
    }

    /**
     * 功能：执行 `devicesBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement devicesBtn() {
        return waitUntilElementToBeClickable(DEVICES_BTN);
    }

    /**
     * 功能：执行 `assetsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assetsBtn() {
        return waitUntilElementToBeClickable(ASSETS_BTN);
    }

    /**
     * 功能：执行 `entityViewsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityViewsBtn() {
        return waitUntilElementToBeClickable(ENTITY_VIEWS_BTN);
    }

/*
 * 本类总结：
 * 1. 核心职责：`SideBarMenuViewElements` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}