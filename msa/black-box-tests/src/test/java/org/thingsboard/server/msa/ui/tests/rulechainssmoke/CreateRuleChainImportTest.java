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
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

/**
 * 中文说明：
 * 1. 类目的：`CreateRuleChainImportTest` 是 ThingsBoard MSA 测试模块 中的MSA UI 黑盒测试类型，用于通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 End-to-End Test / Template Method。
 */
@Feature("Import rule chain")
public class CreateRuleChainImportTest extends AbstractRuleChainTest {

    private final String absolutePathToFileImportRuleChain = getClass().getClassLoader().getResource(IMPORT_RULE_CHAIN_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();

    /**
     * 功能：执行 `importRuleChain` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 10, groups = "smoke")
    @Description("Drop json file")
    public void importRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);

        assertIsDisplayed(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME));
    }

    /**
     * 功能：执行 `importRuleChainAndDeleteFile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Drop json file and delete it")
    public void importRuleChainAndDeleteFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.clearImportFileBtn().click();

        assertIsDisplayed(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        ruleChainsPage.assertEntityIsNotPresent(IMPORT_RULE_CHAIN_FILE_NAME);
    }

    /**
     * 功能：执行 `importTxtFile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 20, groups = "smoke")
    @Description("Import txt file")
    public void importTxtFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        assertIsDisplayed(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
    }

    /**
     * 功能：执行 `importRuleChainAndSave` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 30, groups = "smoke")
    @Description("Import rule chain")
    public void importRuleChainAndSave() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        WebElement doneBtn = openRuleChainPage.doneBtn();
        doneBtn.click();
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        sideBarMenuView.ruleChainsBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    /**
     * 功能：执行 `importRuleChainAndSaveWithSameName` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(priority = 40, groups = "smoke")
    @Description("Import rule chain with same name")
    public void importRuleChainAndSaveWithSameName() {
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        WebElement doneBtn = openRuleChainPage.doneBtn();
        doneBtn.click();
        sideBarMenuView.ruleChainsBtn().click();

        assertThat(ruleChainsPage.entities(ruleChainName).size() > 1).
                as("More than 1 rule chains have been created").isTrue();
        ruleChainsPage.entities(ruleChainName).forEach(this::assertIsDisplayed);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CreateRuleChainImportTest` 在 ThingsBoard MSA 测试模块 中承担MSA UI 黑盒测试类型职责，核心目的是通过 Selenium 验证客户、设备、资产、规则链等页面工作流。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
