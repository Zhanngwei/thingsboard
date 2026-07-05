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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * 中文说明：
 * 1. 类目的：`RuleChainsPageHelper` 是 ThingsBoard MSA 测试模块 中的Selenium 页面对象类型，用于封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Page Object / Helper。
 */
@Slf4j
public class RuleChainsPageHelper extends RuleChainsPageElements {
    /**
     * 功能：创建 `RuleChainsPageHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleChainsPageHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 功能：执行 `openCreateRuleChainView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openCreateRuleChainView() {
        plusBtn().click();
        createRuleChainBtn().click();
    }

    /**
     * 功能：执行 `openImportRuleChainView` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openImportRuleChainView() {
        plusBtn().click();
        importRuleChainBtn().click();
    }

    /**
     * 功能：获取数量。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getRandomNumberFromRuleChainsCount() {
        Random random = new Random();
        return random.nextInt(notRootRuleChainsNames().size());
    }

    /**
     * 规则链，用于标识或展示当前对象。
     */
    private String ruleChainName;
    private String description;

    /**
     * 功能：更新规则链。
     * 参数：无。
     * 返回：无。
     */
    public void setRuleChainNameWithoutRoot() {
        this.ruleChainName = notRootRuleChainsNames().get(getRandomNumberFromRuleChainsCount()).getText();
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void setRuleChainNameWithoutRoot(int number) {
        this.ruleChainName = notRootRuleChainsNames().get(number).getText();
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
     * 功能：更新规则链。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：无。
     */
    public void setRuleChainName(int number) {
        this.ruleChainName = allNames().get(number).getText();
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getRuleChainName() {
        return this.ruleChainName;
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
     * 功能：删除或清理规则链。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：文本结果。
     */
    public String deleteRuleChainFromView(String ruleChainName) {
        String s = "";
        if (deleteBtnFromView() != null) {
            deleteBtnFromView().click();
            warningPopUpYesBtn().click();
            if (elementIsNotPresent(getWarningMessage())) {
                return getEntity(ruleChainName);
            }
        } else {
            for (int i = 0; i < notRootRuleChainsNames().size(); i++) {
                notRootRuleChainsNames().get(i).click();
                if (deleteBtnFromView() != null) {
                    deleteBtnFromView().click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        s = notRootRuleChainsNames().get(i).getText();
                        break;
                    }
                }
            }
        }
        return s;
    }

    /**
     * 功能：执行 `assertCheckBoxIsNotDisplayed` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：无。
     */
    public void assertCheckBoxIsNotDisplayed(String entityName) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//mat-checkbox)[2]")));
        Assert.assertFalse(driver.findElement(By.xpath(getCheckbox(entityName))).isDisplayed());
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean deleteBtnInRootRuleChainIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getDeleteRuleChainFromViewBtn())));
    }

    /**
     * 功能：执行 `assertRuleChainsIsNotPresent` 对应的处理。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：判断结果。
     */
    public boolean assertRuleChainsIsNotPresent(String ruleChainName) {
        return elementsIsNotPresent(getEntity(ruleChainName));
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
     * `sort`列表，用于保存一组待处理对象。
     */
    ArrayList<String> sort;

    /**
     * 功能：更新`Sort`。
     * 参数：无。
     * 返回：无。
     */
    public void setSort() {
        ArrayList<String> createdTime = new ArrayList<>();
        createdTime().forEach(x -> createdTime.add(x.getText()));
        Collections.sort(createdTime);
        sort = createdTime;
    }

    /**
     * 功能：获取`Sort`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public ArrayList<String> getSort() {
        return sort;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainsPageHelper` 在 ThingsBoard MSA 测试模块 中承担Selenium 页面对象类型职责，核心目的是封装 Web UI 页面元素定位、表单填写、按钮点击和列表校验。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
