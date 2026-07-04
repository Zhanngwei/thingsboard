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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_CHAIN_TABLE_NAME)
/**
 * 中文说明：
 * 1. 类目的：`RuleChainEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
public class RuleChainEntity extends BaseSqlEntity<RuleChain> {

    @Column(name = ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `tenantId` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private UUID tenantId;

    @Column(name = ModelConstants.RULE_CHAIN_NAME_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `name` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.RULE_CHAIN_TYPE_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `type` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private RuleChainType type;

    @Column(name = ModelConstants.RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `firstRuleNodeId` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private UUID firstRuleNodeId;

    @Column(name = ModelConstants.RULE_CHAIN_ROOT_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `root` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private boolean root;

    @Column(name = ModelConstants.DEBUG_MODE)
    /**
     * 字段说明：
     * 1. 保存 `debugMode` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private boolean debugMode;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_CHAIN_CONFIGURATION_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `configuration` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `additionalInfo` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    /**
     * 字段说明：
     * 1. 保存 `externalId` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private UUID externalId;

    /**
     * 方法说明：
     * 1. 职责：执行 `RuleChainEntity` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public RuleChainEntity() {
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `RuleChainEntity` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public RuleChainEntity(RuleChain ruleChain) {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (ruleChain.getId() != null) {
            this.setUuid(ruleChain.getUuidId());
        }
        this.setCreatedTime(ruleChain.getCreatedTime());
        this.tenantId = DaoUtil.getId(ruleChain.getTenantId());
        this.name = ruleChain.getName();
        this.type = ruleChain.getType();
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (ruleChain.getFirstRuleNodeId() != null) {
            this.firstRuleNodeId = ruleChain.getFirstRuleNodeId().getId();
        }
        this.root = ruleChain.isRoot();
        this.debugMode = ruleChain.isDebugMode();
        this.configuration = ruleChain.getConfiguration();
        this.additionalInfo = ruleChain.getAdditionalInfo();
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (ruleChain.getExternalId() != null) {
            this.externalId = ruleChain.getExternalId().getId();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `toData` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public RuleChain toData() {
        RuleChain ruleChain = new RuleChain(new RuleChainId(this.getUuid()));
        ruleChain.setCreatedTime(createdTime);
        ruleChain.setTenantId(TenantId.fromUUID(tenantId));
        ruleChain.setName(name);
        ruleChain.setType(type);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (firstRuleNodeId != null) {
            ruleChain.setFirstRuleNodeId(new RuleNodeId(firstRuleNodeId));
        }
        ruleChain.setRoot(root);
        ruleChain.setDebugMode(debugMode);
        ruleChain.setConfiguration(configuration);
        ruleChain.setAdditionalInfo(additionalInfo);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (externalId != null) {
            ruleChain.setExternalId(new RuleChainId(externalId));
        }
        return ruleChain;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
