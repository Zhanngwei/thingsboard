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
package org.thingsboard.server.msa.ui.utils;

import org.openqa.selenium.Keys;
import org.testng.annotations.DataProvider;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomSymbol;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_ACTIVE_STATE;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_INACTIVE_STATE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. 类目的：`DataProviderCredential` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
public class DataProviderCredential {

    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String NAME = ENTITY_NAME;
    private static final String SYMBOL = String.valueOf(getRandomSymbol());
    /**
     * `NUMBER`常量，用于统一引用固定值。
     */
    private static final String NUMBER = "1";
    private static final String LONG_PHONE_NUMBER = "20155501231";
    /**
     * `SHORT_PHONE_NUMBER`常量，用于统一引用固定值。
     */
    private static final String SHORT_PHONE_NUMBER = "201555011";
    private static final String RULE_CHAIN_SECOND_WORD_NAME_PATH = "Rule";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_SECOND_WORD_NAME_PATH = "Customer";
    private static final String RULE_CHAIN_FIRST_WORD_NAME_PATH = "Root";
    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_FIRST_WORD_NAME_PATH = "A";
    private static final String DEFAULT_DEVICE_PROFILE_NAME = "Device Profile";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    private static final String DEFAULT_ASSET_PROFILE_NAME = "Asset Profile";

    /**
     * 功能：执行 `ruleChainNameForSearchByFirstAndSecondWord` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] ruleChainNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {RULE_CHAIN_SECOND_WORD_NAME_PATH},
                {RULE_CHAIN_FIRST_WORD_NAME_PATH}};
    }

    /**
     * 功能：执行 `nameForSearchBySymbolAndNumber` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] nameForSearchBySymbolAndNumber() {
        String name = ENTITY_NAME + random();
        return new Object[][]{
                {name, name.split("`")[1]},
                {NAME + getRandomNumber(), "~"},
                {NAME + getRandomNumber(), "`"},
                {NAME + getRandomNumber(), "!"},
                {NAME + getRandomNumber(), "@"},
                {NAME + getRandomNumber(), "#"},
                {NAME + getRandomNumber(), "$"},
                {NAME + getRandomNumber(), "^"},
                {NAME + getRandomNumber(), "&"},
                {NAME + getRandomNumber(), "*"},
                {NAME + getRandomNumber(), "("},
                {NAME + getRandomNumber(), ")"},
                {NAME + getRandomNumber(), "+"},
                {NAME + getRandomNumber(), "="},
                {NAME + getRandomNumber(), "-"}};
    }

    /**
     * 功能：执行 `nameForSort` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] nameForSort() {
        return new Object[][]{
                {NAME + getRandomNumber()},
                {SYMBOL},
                {NUMBER}};
    }

    /**
     * 功能：执行 `nameForAllSort` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] nameForAllSort() {
        return new Object[][]{
                {NAME + getRandomNumber(), SYMBOL, NUMBER}};
    }

    /**
     * 功能：执行 `incorrectPhoneNumber` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] incorrectPhoneNumber() {
        return new Object[][]{
                {LONG_PHONE_NUMBER},
                {SHORT_PHONE_NUMBER},
                {ENTITY_NAME + getRandomNumber()}};
    }

    /**
     * 功能：执行 `customerNameForSearchByFirstAndSecondWord` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] customerNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {CUSTOMER_FIRST_WORD_NAME_PATH},
                {CUSTOMER_SECOND_WORD_NAME_PATH}};
    }

    /**
     * 功能：执行 `deviceProfileSearch` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] deviceProfileSearch() {
        String name = NAME + getRandomNumber();
        return new Object[][]{
                {DEFAULT_DEVICE_PROFILE_NAME, DEFAULT_DEVICE_PROFILE_NAME.split(" ")[0]},
                {DEFAULT_DEVICE_PROFILE_NAME, DEFAULT_DEVICE_PROFILE_NAME.split(" ")[1]},
                {name, name.split("`")[1]},
                {NAME + getRandomNumber(), "~"},
                {NAME + getRandomNumber(), "`"},
                {NAME + getRandomNumber(), "!"},
                {NAME + getRandomNumber(), "@"},
                {NAME + getRandomNumber(), "#"},
                {NAME + getRandomNumber(), "$"},
                {NAME + getRandomNumber(), "^"},
                {NAME + getRandomNumber(), "&"},
                {NAME + getRandomNumber(), "*"},
                {NAME + getRandomNumber(), "("},
                {NAME + getRandomNumber(), ")"},
                {NAME + getRandomNumber(), "+"},
                {NAME + getRandomNumber(), "="},
                {NAME + getRandomNumber(), "-"}};
    }

    /**
     * 功能：执行 `assetProfileSearch` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] assetProfileSearch() {
        String name = NAME + getRandomNumber();
        return new Object[][]{
                {DEFAULT_ASSET_PROFILE_NAME, DEFAULT_ASSET_PROFILE_NAME.split(" ")[0]},
                {DEFAULT_ASSET_PROFILE_NAME, DEFAULT_ASSET_PROFILE_NAME.split(" ")[1]},
                {name, name.split("`")[1]},
                {NAME + getRandomNumber(), "~"},
                {NAME + getRandomNumber(), "`"},
                {NAME + getRandomNumber(), "!"},
                {NAME + getRandomNumber(), "@"},
                {NAME + getRandomNumber(), "#"},
                {NAME + getRandomNumber(), "$"},
                {NAME + getRandomNumber(), "^"},
                {NAME + getRandomNumber(), "&"},
                {NAME + getRandomNumber(), "*"},
                {NAME + getRandomNumber(), "("},
                {NAME + getRandomNumber(), ")"},
                {NAME + getRandomNumber(), "+"},
                {NAME + getRandomNumber(), "="},
                {NAME + getRandomNumber(), "-"}};
    }

    /**
     * 功能：执行 `editMenuDescription` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] editMenuDescription() {
        String newDescription = "Description" + getRandomNumber();
        String description = "Description";
        return new Object[][]{
                {"", newDescription, newDescription},
                {description, newDescription, description + newDescription},
                {description, Keys.CONTROL + "A" + Keys.BACK_SPACE, ""}};
    }

    /**
     * 功能：执行 `enable` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] enable() {
        return new Object[][]{
                {false},
                {true}};
    }

    /**
     * 功能：执行 `editDeviceLabel` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider
    public static Object[][] editDeviceLabel() {
        String newLabel = "Label" + getRandomNumber();
        String label = "Label";
        return new Object[][]{
                {"", newLabel, newLabel},
                {label, newLabel, label + newLabel},
                {label, Keys.CONTROL + "A" + Keys.BACK_SPACE, ""}};
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @DataProvider(name = "filterData")
    public static Object[][] getFilterData() {
        return new Object[][]{
                {DEVICE_ACTIVE_STATE},
                {DEVICE_INACTIVE_STATE}
        };
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DataProviderCredential` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
