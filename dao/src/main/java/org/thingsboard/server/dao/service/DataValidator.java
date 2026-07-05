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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.TenantEntityWithDataDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.EntitiesLimitException;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. 类目的：`DataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@Slf4j
public abstract class DataValidator<D extends BaseData<?>> {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern QUEUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+$");

    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String NAME = "name";
    private static final String TOPIC = "topic";

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired @Lazy
    private ApiLimitService apiLimitService;

    // Returns old instance of the same object that is fetched during validation.
    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * - `tenantIdFunction`：租户信息或租户标识。
     * 返回：处理结果。
     */
    public D validate(D data, Function<D, TenantId> tenantIdFunction) {
        try {
            if (data == null) {
                throw new DataValidationException("Data object can't be null!");
            }

            ConstraintValidator.validateFields(data);

            TenantId tenantId = tenantIdFunction.apply(data);
            validateDataImpl(tenantId, data);
            D old;
            if (data.getId() == null) {
                validateCreate(tenantId, data);
                old = null;
            } else {
                old = validateUpdate(tenantId, data);
            }
            return old;
        } catch (DataValidationException e) {
            log.error("{} object is invalid: [{}]", data == null ? "Data" : data.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `data`：待处理数据。
     * 返回：无。
     */
    protected void validateDataImpl(TenantId tenantId, D data) {
    }

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `data`：待处理数据。
     * 返回：无。
     */
    protected void validateCreate(TenantId tenantId, D data) {
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `data`：待处理数据。
     * 返回：判断结果。
     */
    protected D validateUpdate(TenantId tenantId, D data) {
        return null;
    }

    /**
     * 功能：校验`Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    public void validateDelete(TenantId tenantId, EntityId entityId) {
    }

    /**
     * 功能：校验`String`。
     * 参数：
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * - `name`：名称。
     * 返回：无。
     */
    public void validateString(String exceptionPrefix, String name) {
        if (StringUtils.isEmpty(name) || name.trim().length() == 0) {
            throw new DataValidationException(exceptionPrefix + " should be specified!");
        }
        if (StringUtils.contains0x00(name)) {
            throw new DataValidationException(exceptionPrefix + " should not contain 0x00 symbol!");
        }
    }

    /**
     * 功能：判断数据。
     * 参数：
     * - `existentData`：待处理数据。
     * - `actualData`：待处理数据。
     * 返回：判断结果。
     */
    protected boolean isSameData(D existentData, D actualData) {
        return actualData.getId() != null && existentData.getId().equals(actualData.getId());
    }

    /**
     * 功能：校验邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    public static void validateEmail(String email) {
        if (!doValidateEmail(email)) {
            throw new DataValidationException("Invalid email address format '" + email + "'!");
        }
    }

    /**
     * 功能：执行 `doValidateEmail` 对应的处理。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：判断结果。
     */
    public static boolean doValidateEmail(String email) {
        if (email == null) {
            return false;
        }

        Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
        return emailMatcher.matches();
    }

    /**
     * 功能：校验租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    protected void validateNumberOfEntitiesPerTenant(TenantId tenantId,
                                                     EntityType entityType) {
        if (!apiLimitService.checkEntitiesLimit(tenantId, entityType)) {
            throw new EntitiesLimitException(tenantId, entityType);
        }
    }

    /**
     * 功能：校验租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dataDao`：待处理数据。
     * - `maxSumDataSize`：待处理数据。
     * - `currentDataSize`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void validateMaxSumDataSizePerTenant(TenantId tenantId,
                                                   TenantEntityWithDataDao dataDao,
                                                   long maxSumDataSize,
                                                   long currentDataSize,
                                                   EntityType entityType) {
        if (maxSumDataSize > 0) {
            if (dataDao.sumDataSizeByTenantId(tenantId) + currentDataSize > maxSumDataSize) {
                throw new DataValidationException(String.format("%ss total size exceeds the maximum of " + FileUtils.byteCountToDisplaySize(maxSumDataSize), entityType.getNormalName()));
            }
        }
    }

    /**
     * 功能：校验JSON。
     * 参数：
     * - `expectedNode`：`expectedNode` 参数。
     * - `actualNode`：`actualNode` 参数。
     * 返回：无。
     */
    protected static void validateJsonStructure(JsonNode expectedNode, JsonNode actualNode) {
        Set<String> expectedFields = new HashSet<>();
        Iterator<String> fieldsIterator = expectedNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            expectedFields.add(fieldsIterator.next());
        }

        Set<String> actualFields = new HashSet<>();
        fieldsIterator = actualNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            actualFields.add(fieldsIterator.next());
        }

        if (!expectedFields.containsAll(actualFields) || !actualFields.containsAll(expectedFields)) {
            throw new DataValidationException("Provided json structure is different from stored one '" + actualNode + "'!");
        }
    }

    /**
     * 功能：校验队列名称。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    protected static void validateQueueName(String name) {
        validateQueueNameOrTopic(name, NAME);
    }

    /**
     * 功能：校验队列。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    protected static void validateQueueTopic(String topic) {
        validateQueueNameOrTopic(topic, TOPIC);
    }

    /**
     * 功能：校验队列名称。
     * 参数：
     * - `value`：值。
     * - `fieldName`：名称。
     * 返回：无。
     */
    static void validateQueueNameOrTopic(String value, String fieldName) {
        if (StringUtils.isEmpty(value) || value.trim().length() == 0) {
            throw new DataValidationException(String.format("Queue %s should be specified!", fieldName));
        }
        if (!QUEUE_PATTERN.matcher(value).matches()) {
            throw new DataValidationException(
                    String.format("Queue %s contains a character other than ASCII alphanumerics, '.', '_' and '-'!", fieldName));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
