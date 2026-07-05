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
package org.thingsboard.server.dao.sql.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`EntityDataAdapter` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class EntityDataAdapter {

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `selectionMapping`：数据列表。
     * - `rows`：数据列表。
     * - `totalElements`：`totalElements` 参数。
     * 返回：匹配的数据集合。
     */
    public static PageData<EntityData> createEntityData(EntityDataPageLink pageLink,
                                                        List<EntityKeyMapping> selectionMapping,
                                                        List<Map<String, Object>> rows,
                                                        int totalElements) {
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        List<EntityData> entitiesData = convertListToEntityData(rows, selectionMapping);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    /**
     * 功能：转换实体。
     * 参数：
     * - `result`：数据列表。
     * - `selectionMapping`：数据列表。
     * 返回：匹配的数据集合。
     */
    private static List<EntityData> convertListToEntityData(List<Map<String, Object>> result, List<EntityKeyMapping> selectionMapping) {
        return result.stream().map(row -> toEntityData(row, selectionMapping)).collect(Collectors.toList());
    }

    /**
     * 功能：执行 `toEntityData` 对应的处理。
     * 参数：
     * - `row`：键值映射。
     * - `selectionMapping`：数据列表。
     * 返回：处理结果。
     */
    private static EntityData toEntityData(Map<String, Object> row, List<EntityKeyMapping> selectionMapping) {
        UUID id = (UUID)row.get("id");
        EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
        Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
        //Maybe avoid empty hashmaps?
        EntityData entityData = new EntityData(entityId, latest, new HashMap<>(), new HashMap<>());
        for (EntityKeyMapping mapping : selectionMapping) {
            if (!mapping.isIgnore()) {
                EntityKey entityKey = mapping.getEntityKey();
                Object value = row.get(mapping.getValueAlias());
                String strValue;
                long ts;
                if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                    strValue = value != null ? value.toString() : "";
                    ts = System.currentTimeMillis();
                } else {
                    strValue = convertValue(value);
                    Object tsObject = row.get(mapping.getTsAlias());
                    ts = tsObject != null ? Long.parseLong(tsObject.toString()) : 0;
                }
                TsValue tsValue = new TsValue(ts, strValue);
                latest.computeIfAbsent(entityKey.getType(), entityKeyType -> new HashMap<>()).put(entityKey.getKey(), tsValue);
            }
        }
        return entityData;
    }

    /**
     * 功能：转换值。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    static String convertValue(Object value) {
        if (value != null) {
            String strVal = value.toString();
            // check number
            if (NumberUtils.isParsable(strVal)) {
                if (strVal.startsWith("0") && !strVal.startsWith("0.")) {
                    return strVal;
                }
                try {
                    BigInteger longVal = new BigInteger(strVal);
                    return longVal.toString();
                } catch (NumberFormatException ignored) {
                }
                try {
                    double dblVal = Double.parseDouble(strVal);
                    String doubleAsString = Double.toString(dblVal);
                    if (!Double.isInfinite(dblVal) && isSimpleDouble(doubleAsString)) {
                        return doubleAsString;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return strVal;
        } else {
            return "";
        }
    }

    /**
     * 功能：判断`Simple Double`。
     * 参数：
     * - `valueAsString`：值。
     * 返回：判断结果。
     */
    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`EntityDataAdapter` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
