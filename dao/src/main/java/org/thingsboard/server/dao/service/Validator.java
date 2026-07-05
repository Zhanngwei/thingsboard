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

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. 类目的：`Validator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public class Validator {

    public static final Pattern PROPERTY_PATTERN = Pattern.compile("^[\\p{L}0-9_-]+$"); // Unicode letters, numbers, '_' and '-' allowed

    /**
     * This method validate <code>EntityId</code> entity id. If entity id is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param entityId          the entityId
     * @param errorMessage the error message for exception
     */
    /**
     * 功能：校验实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void validateEntityId(EntityId entityId, String errorMessage) {
        if (entityId == null || entityId.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>EntityId</code> entity id. If entity id is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param entityId                  the entityId
     * @param errorMessageFunction      the error message for exception that apply entityId
     */
    /**
     * 功能：校验实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * - `errorMessageFunction`：待处理消息。
     * 返回：无。
     */
    public static void validateEntityId(EntityId entityId, Function<EntityId, String> errorMessageFunction) {
        if (entityId == null || entityId.getId() == null) {
            throw new IncorrectParameterException(errorMessageFunction.apply(entityId));
        }
    }

    /**
     * This method validate <code>String</code> string. If string is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val          the val
     * @param errorMessage the error message for exception
     */
    /**
     * 功能：校验`String`。
     * 参数：
     * - `val`：`val` 参数。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void validateString(String val, String errorMessage) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /*
     * This method validate <code>String</code> string. If string is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val                       the value
     * @param errorMessageFunction      the error message function that apply value
     */
    /**
     * 功能：校验`String`。
     * 参数：
     * - `val`：`val` 参数。
     * - `errorMessageFunction`：待处理消息。
     * 返回：无。
     */
    public static void validateString(String val, Function<String, String> errorMessageFunction) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessageFunction.apply(val));
        }
    }

    /**
     * This method validate <code>long</code> value. If value isn't positive than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val          the val
     * @param errorMessage the error message for exception
     */
    /**
     * 功能：校验`Positive Number`。
     * 参数：
     * - `val`：`val` 参数。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void validatePositiveNumber(long val, String errorMessage) {
        if (val <= 0) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>UUID</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id           the id
     * @param errorMessage the error message for exception
     */
    /**
     * 功能：校验`Id`。
     * 参数：
     * - `id`：`id`ID。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void validateId(UUID id, String errorMessage) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }


    /**
     * This method validate <code>UUIDBased</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id           the id
     * @param errorMessage the error message for exception
     */
    /**
     * 功能：校验`Id`。
     * 参数：
     * - `id`：`id`ID。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void validateId(UUIDBased id, String errorMessage) {
        if (id == null || id.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate list of <code>UUIDBased</code> ids. If at least one of the ids is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param ids          the list of ids
     * @param errorMessage the error message for exception
     */
    /**
     * 功能：校验`Ids`。
     * 参数：
     * - `ids`：数据列表。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void validateIds(List<? extends UUIDBased> ids, String errorMessage) {
        if (ids == null || ids.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        } else {
            for (UUIDBased id : ids) {
                validateId(id, errorMessage);
            }
        }
    }

    /**
     * This method validate <code>PageLink</code> page link. If pageLink is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param pageLink     the page link
     */
    /**
     * 功能：校验分页查询条件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：无。
     */
    public static void validatePageLink(PageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect page link page size '"+pageLink.getPageSize()+"'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect page link page '"+pageLink.getPage()+"'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null) {
            if (!isValidProperty(pageLink.getSortOrder().getProperty())) {
                throw new IncorrectParameterException("Invalid page link sort property");
            }
        }
    }

    /**
     * 功能：校验分页查询条件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：无。
     */
    public static void validateEntityDataPageLink(EntityDataPageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Entity Data Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect entity data page link page size '" + pageLink.getPageSize() + "'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect entity data page link page '" + pageLink.getPage() + "'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null && pageLink.getSortOrder().getKey() != null) {
            EntityKey sortKey = pageLink.getSortOrder().getKey();
            if ((sortKey.getType() == EntityKeyType.ENTITY_FIELD || sortKey.getType() == EntityKeyType.ALARM_FIELD)
                    && !isValidProperty(sortKey.getKey())) {
                throw new IncorrectParameterException("Invalid entity data page link sort property");
            }
        }
    }

    /**
     * 功能：判断`Valid Property`。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    public static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * - `errorMessage`：待处理消息。
     * 返回：无。
     */
    public static void checkNotNull(Object reference, String errorMessage) {
        if (reference == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`Validator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
