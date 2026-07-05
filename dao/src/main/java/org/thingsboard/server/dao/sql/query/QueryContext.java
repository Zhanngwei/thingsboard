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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.PostgresUUIDType;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`QueryContext` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Slf4j
public class QueryContext implements SqlParameterSource {
    private static final PostgresUUIDType UUID_TYPE = new PostgresUUIDType();

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final QuerySecurityContext securityCtx;
    private final StringBuilder query;
    /**
     * `params`映射关系，用于按键查找对应值。
     */
    private final Map<String, Parameter> params;

    /**
     * 功能：创建 `QueryContext` 实例，并初始化必要字段。
     * 参数：
     * - `securityCtx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public QueryContext(QuerySecurityContext securityCtx) {
        this.securityCtx = securityCtx;
        query = new StringBuilder();
        params = new HashMap<>();
    }

    /**
     * 功能：保存或创建`Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * - `type`：类型。
     * - `typeName`：名称。
     * 返回：无。
     */
    void addParameter(String name, Object value, int type, String typeName) {
        Parameter newParam = new Parameter(value, type, typeName);
        Parameter oldParam = params.put(name, newParam);
        if (oldParam != null && oldParam.value != null && !oldParam.value.equals(newParam.value)) {
            throw new RuntimeException("Parameter with name: " + name + " was already registered!");
        }
        if(value == null){
            log.warn("[{}][{}][{}] Trying to set null value", getTenantId(), getCustomerId(), name);
        }
    }

    /**
     * 功能：执行 `append` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：无。
     */
    public void append(String s) {
        query.append(s);
    }

    /**
     * 功能：判断值。
     * 参数：
     * - `paramName`：名称。
     * 返回：判断结果。
     */
    @Override
    public boolean hasValue(String paramName) {
        return params.containsKey(paramName);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `paramName`：名称。
     * 返回：处理结果。
     */
    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        return checkParameter(paramName).value;
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `paramName`：名称。
     * 返回：数值结果。
     */
    @Override
    public int getSqlType(String paramName) {
        return checkParameter(paramName).type;
    }

    /**
     * 功能：校验`Parameter`。
     * 参数：
     * - `paramName`：名称。
     * 返回：判断结果。
     */
    private Parameter checkParameter(String paramName) {
        Parameter param = params.get(paramName);
        if (param == null) {
            throw new RuntimeException("Parameter with name: " + paramName + " is not set!");
        }
        return param;
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `paramName`：名称。
     * 返回：文本结果。
     */
    @Override
    public String getTypeName(String paramName) {
        return params.get(paramName).name;
    }

    /**
     * 功能：获取`Parameter Names`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public String[] getParameterNames() {
        return params.keySet().toArray(new String[]{});
    }

    /**
     * 功能：保存或创建`Uuid Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addUuidParameter(String name, UUID value) {
        addParameter(name, value, UUID_TYPE.sqlType(), UUID_TYPE.getName());
    }

    /**
     * 功能：保存或创建`String Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addStringParameter(String name, String value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    /**
     * 功能：保存或创建`Double Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addDoubleParameter(String name, double value) {
        addParameter(name, value, Types.DOUBLE, "DOUBLE");
    }

    /**
     * 功能：保存或创建`Long Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addLongParameter(String name, long value) {
        addParameter(name, value, Types.BIGINT, "BIGINT");
    }

    /**
     * 功能：保存或创建`String List Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addStringListParameter(String name, List<String> value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    /**
     * 功能：保存或创建`Boolean Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addBooleanParameter(String name, boolean value) {
        addParameter(name, value, Types.BOOLEAN, "BOOLEAN");
    }

    /**
     * 功能：保存或创建`Uuid List Parameter`。
     * 参数：
     * - `name`：名称。
     * - `value`：值。
     * 返回：无。
     */
    public void addUuidListParameter(String name, List<UUID> value) {
        addParameter(name, value, UUID_TYPE.sqlType(), UUID_TYPE.getName());
    }

    /**
     * 功能：获取查询条件。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getQuery() {
        return query.toString();
    }


    /**
     * 中文说明：
     * 1. 类目的：`Parameter` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
     * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Repository / DAO / Adapter。
     */
    public static class Parameter {
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private final Object value;
        private final int type;
        /**
         * 名称，用于标识或展示当前对象。
         */
        private final String name;

        /**
         * 功能：创建 `QueryContext` 实例，并初始化必要字段。
         * 参数：
         * - `value`：值。
         * - `type`：类型。
         * - `name`：名称。
         * 返回：新创建的对象实例。
         */
        public Parameter(Object value, int type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantId getTenantId() {
        return securityCtx.getTenantId();
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public CustomerId getCustomerId() {
        return securityCtx.getCustomerId();
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    public EntityType getEntityType() {
        return securityCtx.getEntityType();
    }

    /**
     * 功能：判断`Ignore Permission Check`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isIgnorePermissionCheck() {
        return securityCtx.isIgnorePermissionCheck();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`QueryContext` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
