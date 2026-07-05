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

/**
 * 中文说明：
 * 1. 类目的：`OtherPageElementsHelper` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
public class OtherPageElementsHelper extends OtherPageElements {
    /**
     * 功能：创建 `OtherPageElementsHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public OtherPageElementsHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String headerName;

    /**
     * 功能：更新名称。
     * 参数：无。
     * 返回：无。
     */
    public void setHeaderName() {
        this.headerName = headerNameView().getText();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * 功能：执行 `assertEntityIsNotPresent` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：判断结果。
     */
    public boolean assertEntityIsNotPresent(String entityName) {
        return elementIsNotPresent(getEntity(entityName));
    }

    /**
     * 功能：执行 `goToHelpPage` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void goToHelpPage() {
        helpBtn().click();
        goToNextTab(2);
    }

    /**
     * 功能：执行 `clickOnCheckBoxes` 对应的处理。
     * 参数：
     * - `count`：`count` 参数。
     * 返回：无。
     */
    public void clickOnCheckBoxes(int count) {
        for (int i = 0; i < count; i++) {
            checkBoxes().get(i).click();
        }
    }

    /**
     * 功能：执行 `changeNameEditMenu` 对应的处理。
     * 参数：
     * - `keysToSend`：键。
     * 返回：无。
     */
    public void changeNameEditMenu(CharSequence keysToSend) {
        nameFieldEditMenu().click();
        nameFieldEditMenu().clear();
        nameFieldEditMenu().sendKeys(keysToSend);
    }

    /**
     * 功能：执行 `changeDescription` 对应的处理。
     * 参数：
     * - `newDescription`：`newDescription` 参数。
     * 返回：无。
     */
    public void changeDescription(String newDescription) {
        descriptionEntityView().click();
        descriptionEntityView().clear();
        descriptionEntityView().sendKeys(newDescription);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：文本结果。
     */
    public String deleteRuleChainTrash(String entityName) {
        deleteBtn(entityName).click();
        warningPopUpYesBtn().click();
        return entityName;
    }

    /**
     * 功能：删除或清理`Selected`。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：文本结果。
     */
    public String deleteSelected(String entityName) {
        checkBox(entityName).click();
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
        return entityName;
    }

    /**
     * 功能：删除或清理`Selected`。
     * 参数：
     * - `countOfCheckBoxes`：`countOfCheckBoxes` 参数。
     * 返回：无。
     */
    public void deleteSelected(int countOfCheckBoxes) {
        clickOnCheckBoxes(countOfCheckBoxes);
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `namePath`：名称。
     * 返回：无。
     */
    public void searchEntity(String namePath) {
        searchBtn().click();
        searchField().sendKeys(namePath);
        sleep(0.5);
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`OtherPageElementsHelper` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
