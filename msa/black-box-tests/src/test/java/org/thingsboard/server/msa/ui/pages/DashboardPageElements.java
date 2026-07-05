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

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`DashboardPageElements` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
public class DashboardPageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `DashboardPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public DashboardPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * `TITLES`常量，用于统一引用固定值。
     */
    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-title')]/span";
    private static final String ASSIGNED_BTN = ENTITY + "/../..//mat-icon[contains(text(),' assignment_ind')]/../..";
    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final String MANAGE_ASSIGNED_ENTITY_LIST_FIELD = "//input[@formcontrolname='entity']";
    private static final String MANAGE_ASSIGNED_ENTITY = "//mat-option//span[contains(text(),'%s')]";
    /**
     * `MANAGE_ASSIGNED_UPDATE_BTN`常量，用于统一引用固定值。
     */
    private static final String MANAGE_ASSIGNED_UPDATE_BTN = "//button[@type='submit']";
    private static final String EDIT_BTN = "//mat-icon[text() = 'edit']/parent::button[@mat-stroked-button]";
    /**
     * `ADD_BTN`常量，用于统一引用固定值。
     */
    private static final String ADD_BTN = "//mat-fab-actions//mat-icon[text() = 'add']/parent::button";
    private static final String ALARM_WIDGET_BUNDLE = "//mat-card-title[text() = 'Alarm widgets']/ancestor::mat-card";
    /**
     * 告警常量，用于统一引用固定值。
     */
    private static final String ALARM_TABLE_WIDGET = "//img[@alt='Alarms table']/ancestor::mat-card";
    private static final String WIDGET_SE_CORNER = "//div[contains(@class,'handle-se')]";
    /**
     * `SAVE_BTN`常量，用于统一引用固定值。
     */
    private static final String SAVE_BTN = "//mat-icon[text() = 'done']/parent::button[@fxhide.lt-lg]";

    /**
     * 功能：执行 `entityTitles` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    /**
     * 功能：执行 `assignedBtn` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement assignedBtn(String title) {
        return waitUntilElementToBeClickable(String.format(ASSIGNED_BTN, title));
    }

    /**
     * 功能：执行 `manageAssignedEntityListField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageAssignedEntityListField() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_ENTITY_LIST_FIELD);
    }

    /**
     * 功能：执行 `manageAssignedEntity` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public WebElement manageAssignedEntity(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_ASSIGNED_ENTITY, title));
    }

    /**
     * 功能：执行 `manageAssignedUpdateBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement manageAssignedUpdateBtn() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_UPDATE_BTN);
    }

    /**
     * 功能：执行 `editBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement editBtn() {
        return waitUntilElementToBeClickable(EDIT_BTN);
    }

    /**
     * 功能：保存或创建`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement addBtn() {
        return waitUntilElementToBeClickable(ADD_BTN);
    }

    /**
     * 功能：执行 `alarmWidgetBundle` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmWidgetBundle() {
        return waitUntilElementToBeClickable(ALARM_WIDGET_BUNDLE);
    }

    /**
     * 功能：执行 `alarmTableWidget` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement alarmTableWidget() {
        return waitUntilElementToBeClickable(ALARM_TABLE_WIDGET);
    }

    /**
     * 功能：执行 `widgetSECorner` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement widgetSECorner() {
        return waitUntilElementToBeClickable(WIDGET_SE_CORNER);
    }

    /**
     * 功能：保存或创建`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement saveBtn() {
        return waitUntilVisibilityOfElementLocated(SAVE_BTN);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardPageElements` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
