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
package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.dao.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

/**
 * 中文说明：
 * 1. `DataValidatorTest` 是 ThingsBoard DAO 中验证 `DataValidator` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public class DataValidatorTest {

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    DataValidator<?> dataValidator;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        dataValidator = spy(DataValidator.class);
    }

    /**
     * 功能：验证 `validateName_thenOK` 描述的测试场景。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "coffee", "1", "big box", "世界", "!", "--", "~!@#$%^&*()_+=-/|\\[]{};:'`\"?<>,.", "\uD83D\uDC0C", "\041",
            "Gdy Pomorze nie pomoże, to pomoże może morze, a gdy morze nie pomoże, to pomoże może Gdańsk",
    })
    void validateName_thenOK(final String name) {
        dataValidator.validateString("Device name", name);
        dataValidator.validateString("Asset name", name);
        dataValidator.validateString("Asset profile name", name);
        dataValidator.validateString("Alarm type", name);
        dataValidator.validateString("Customer name", name);
        dataValidator.validateString("Tenant name", name);
    }

    /**
     * 功能：验证 `validateName_thenDataValidationException` 描述的测试场景。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "F0929906\000\000\000\000\000\000\000\000\000", "\000\000\000F0929906",
            "\u0000F0929906", "F092\u00009906", "F0929906\u0000"
    })
    void validateName_thenDataValidationException(final String name) {
        DataValidationException exception;
        exception = Assertions.assertThrows(DataValidationException.class, () -> dataValidator.validateString("Asset name", name));
        log.warn("Exception message Asset name: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Asset name").containsPattern("Asset name .*");

        exception = Assertions.assertThrows(DataValidationException.class, () -> dataValidator.validateString("Device name", name));
        log.warn("Exception message Device name: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Device name").containsPattern("Device name .*");
    }

    /**
     * 功能：校验邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "aZ1_!#$%&'*+/=?`{|}~^.-@mail.io", "support@thingsboard.io",
    })
    public void validateEmail(String email) {
        DataValidator.validateEmail(email);
    }

    /**
     * 功能：校验邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "test:1@mail.io", "test()1@mail.io", "test[]1@mail.io",
            "test\\1@mail.io", "test\"1@mail.io", "test<>1@mail.io",
    })
    public void validateEmailInvalid(String email) {
        Assertions.assertThrows(DataValidationException.class, () -> DataValidator.validateEmail(email));
    }

    /**
     * 功能：校验队列名称。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "azAZ09_.-", "topic",
    })
    public void validateQueueNameOrTopic(String value) {
        DataValidator.validateQueueNameOrTopic(value, "name");
        DataValidator.validateQueueNameOrTopic(value, "topic");
    }

    /**
     * 功能：校验队列名称。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "topic@home", "!", ",", "Łódź",
            "\uD83D\uDC0C", "\041",
            "F0929906\000\000\000\000\000\000\000\000\000",
    })
    public void validateQueueNameOrTopicInvalid(String value) {
        Assertions.assertThrows(DataValidationException.class, () -> DataValidator.validateQueueNameOrTopic(value, "name"));
        Assertions.assertThrows(DataValidationException.class, () -> DataValidator.validateQueueNameOrTopic(value, "topic"));
    }

}
