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
 * 1. `DataProviderCredential` 是 ThingsBoard Microservices 中围绕 `Provider Credential` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
