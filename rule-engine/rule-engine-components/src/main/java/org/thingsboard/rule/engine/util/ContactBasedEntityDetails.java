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
 * 联系信息类实体字段在 Rule Engine 中使用的标准名称枚举。
 * 本枚举不直接读取数据库或缓存，也不直接参与消息处理，仅供字段名映射使用。
 */
public enum ContactBasedEntityDetails {

    /**
     * 实体 ID 字段。
     */
    ID("id"),
    /**
     * 实体标题或名称字段。
     */
    TITLE("title"),
    /**
     * 国家字段。
     */
    COUNTRY("country"),
    /**
     * 城市字段。
     */
    CITY("city"),
    /**
     * 州或省字段。
     */
    STATE("state"),
    /**
     * 邮政编码字段。
     */
    ZIP("zip"),
    /**
     * 第一地址字段。
     */
    ADDRESS("address"),
    /**
     * 第二地址字段。
     */
    ADDRESS2("address2"),
    /**
     * 电话字段。
     */
    PHONE("phone"),
    /**
     * 邮箱字段。
     */
    EMAIL("email"),
    /**
     * 附加信息字段。
     */
    ADDITIONAL_INFO("additionalInfo");

    /**
     * Rule Engine 中引用该字段时使用的字段名。
     */
    @Getter
    private final String ruleEngineName;

    /**
     * 绑定枚举值与 Rule Engine 字段名。
     *
     * @param ruleEngineName Rule Engine 中使用的字段名
     */
    ContactBasedEntityDetails(String ruleEngineName) {
        this.ruleEngineName = ruleEngineName;
    }

}

/*
 * 本类总结：
 * 本枚举提供联系信息字段名常量，属于纯内存映射；具体实体数据读取和消息流使用发生在调用方。
 */
