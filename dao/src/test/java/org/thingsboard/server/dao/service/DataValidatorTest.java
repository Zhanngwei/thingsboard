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
 * 1. 类目的：`DataValidatorTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`DataValidatorTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
