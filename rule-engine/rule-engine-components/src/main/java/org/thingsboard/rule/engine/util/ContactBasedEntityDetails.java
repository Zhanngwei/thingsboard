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
package org.thingsboard.rule.engine.util;

import lombok.Getter;

/**
 * `ContactBasedEntityDetails` 枚举，定义当前流程使用的固定取值。
 */
public enum ContactBasedEntityDetails {

    /**
     * `ID`常量，用于统一引用固定值。
     */
    ID("id"),
    /**
     * `TITLE`常量，用于统一引用固定值。
     */
    TITLE("title"),
    /**
     * `COUNTRY`常量，用于统一引用固定值。
     */
    COUNTRY("country"),
    /**
     * `CITY`常量，用于统一引用固定值。
     */
    CITY("city"),
    /**
     * 状态常量，用于统一引用固定值。
     */
    STATE("state"),
    /**
     * `ZIP`常量，用于统一引用固定值。
     */
    ZIP("zip"),
    /**
     * `ADDRESS`常量，用于统一引用固定值。
     */
    ADDRESS("address"),
    /**
     * `ADDRESS2`常量，用于统一引用固定值。
     */
    ADDRESS2("address2"),
    /**
     * `PHONE`常量，用于统一引用固定值。
     */
    PHONE("phone"),
    /**
     * 邮箱常量，用于统一引用固定值。
     */
    EMAIL("email"),
    /**
     * 扩展信息常量，用于统一引用固定值。
     */
    ADDITIONAL_INFO("additionalInfo");

    /**
     * 规则引擎，用于标识或展示当前对象。
     */
    @Getter
    private final String ruleEngineName;

    /**
     * 功能：创建 `ContactBasedEntityDetails` 实例，并初始化必要字段。
     * 参数：
     * - `ruleEngineName`：名称。
     * 返回：新创建的对象实例。
     */
    ContactBasedEntityDetails(String ruleEngineName) {
        this.ruleEngineName = ruleEngineName;
    }

}

/*
 * 本类总结：
 * 本枚举提供联系信息字段名常量，属于纯内存映射；具体实体数据读取和消息流使用发生在调用方。
 */
