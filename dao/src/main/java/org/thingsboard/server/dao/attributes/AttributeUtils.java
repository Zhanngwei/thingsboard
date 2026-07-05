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
package org.thingsboard.server.dao.attributes;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.util.KvUtils;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`AttributeUtils` 是 ThingsBoard DAO 模块 中的属性持久化服务类型，用于处理客户端、共享、服务端属性的保存、查询、删除、缓存和通知边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AttributesService、AttributesDao、缓存、Transport、Rule Engine 和设备会话。
 * 4. 生命周期：由属性 API、设备传输层或规则链流程触发，并随单次属性读写事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Cache-Aside。
 */
public class AttributeUtils {

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `scope`：`scope` 参数。
     * 返回：无。
     */
    public static void validate(EntityId id, String scope) {
        Validator.validateId(id.getId(), "Incorrect id " + id);
        Validator.validateString(scope, "Incorrect scope " + scope);
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `kvEntries`：数据列表。
     * - `valueNoXssValidation`：值。
     * 返回：无。
     */
    public static void validate(List<AttributeKvEntry> kvEntries,  boolean valueNoXssValidation) {
        kvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `kvEntry`：`kvEntry` 参数。
     * - `valueNoXssValidation`：值。
     * 返回：无。
     */
    public static void validate(AttributeKvEntry kvEntry, boolean valueNoXssValidation) {
        KvUtils.validate(kvEntry, valueNoXssValidation);
        if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("Incorrect kvEntry. Data type can't be null");
        } else {
            Validator.validateString(kvEntry.getKey(), "Incorrect kvEntry. Key can't be empty");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "Incorrect last update ts. Ts should be positive");
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AttributeUtils` 在 ThingsBoard DAO 模块 中承担属性持久化服务类型职责，核心目的是处理客户端、共享、服务端属性的保存、查询、删除、缓存和通知边界。
 * 2. 核心流程：校验实体和属性键空间后读写数据库，必要时更新缓存并通知上层属性变更流程。
 * 3. 关键依赖：主要依赖或协作对象包括AttributesService、AttributesDao、缓存、Transport、Rule Engine 和设备会话。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
