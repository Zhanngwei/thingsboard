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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`RelationServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public class RelationServiceTest extends AbstractServiceTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `relationService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    RelationService relationService;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `before` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void before() {
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `after` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void after() {
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveRelation` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(saveRelation(relation));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteRelation` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDeleteRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA).get());

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteRelationConcurrently` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDeleteRelationConcurrently() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);

        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        List<ListenableFuture<Boolean>> futures = new ArrayList<>();
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (int i = 0; i < 2; i++) {
            // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
            futures.add(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA));
        }
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        List<Boolean> results = Futures.allAsList(futures).get();
        Assert.assertTrue(results.contains(true));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteEntityRelations` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDeleteEntityRelations() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        relationService.deleteEntityRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteEntityCommonRelations` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDeleteEntityCommonRelations() {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(parentId, childId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE);
        EntityRelation relationD = new EntityRelation(childId, subChildId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);

        relationService.deleteEntityCommonRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE));
        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindFrom` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindFrom() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(Uuids.timeBased());
        AssetId parentB = new AssetId(Uuids.timeBased());
        AssetId childA = new AssetId(Uuids.timeBased());
        AssetId childB = new AssetId(Uuids.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.CONTAINS_TYPE, relation.getType());
            Assert.assertEquals(parentA, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.MANAGES_TYPE, relation.getType());
            Assert.assertEquals(parentB, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveRelation` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private Boolean saveRelation(EntityRelation relationA1) {
        return relationService.saveRelation(SYSTEM_TENANT_ID, relationA1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTo` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindTo() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(Uuids.timeBased());
        AssetId parentB = new AssetId(Uuids.timeBased());
        AssetId childA = new AssetId(Uuids.timeBased());
        AssetId childB = new AssetId(Uuids.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByTo(SYSTEM_TENANT_ID, childA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childA, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByTo(SYSTEM_TENANT_ID, childB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childB, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCyclicRecursiveRelation` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testCyclicRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> C -> A
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetA, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testRecursiveRelation` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> [C,D]
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        DeviceId deviceD = new DeviceId(Uuids.timeBased());

        EntityRelation relationAB = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBD = new EntityRelation(assetB, deviceD, EntityRelation.CONTAINS_TYPE);


        saveRelation(relationAB);
        saveRelation(relationBC);
        saveRelation(relationBD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testRecursiveRelationDepth` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testRecursiveRelationDepth() throws ExecutionException, InterruptedException {
        int maxLevel = 1000;
        AssetId root = new AssetId(Uuids.timeBased());
        AssetId left = new AssetId(Uuids.timeBased());
        AssetId right = new AssetId(Uuids.timeBased());

        List<EntityRelation> expected = new ArrayList<>();

        EntityRelation relationAB = new EntityRelation(root, left, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(root, right, EntityRelation.CONTAINS_TYPE);
        saveRelation(relationAB);
        expected.add(relationAB);

        saveRelation(relationBC);
        expected.add(relationBC);

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (int i = 0; i < maxLevel; i++) {
            var newLeft = new AssetId(Uuids.timeBased());
            var newRight = new AssetId(Uuids.timeBased());
            EntityRelation relationLeft = new EntityRelation(left, newLeft, EntityRelation.CONTAINS_TYPE);
            EntityRelation relationRight = new EntityRelation(right, newRight, EntityRelation.CONTAINS_TYPE);
            saveRelation(relationLeft);
            expected.add(relationLeft);
            saveRelation(relationRight);
            expected.add(relationRight);
            left = newLeft;
            right = newRight;
        }


        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(root, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for(EntityRelation r : expected){
            Assert.assertTrue(relations.contains(r));
        }

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for(EntityRelation r : expected){
            Assert.assertTrue(relations.contains(r));
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveRelationWithEmptyFrom` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveRelationWithEmptyFrom() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setTo(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertTrue(saveRelation(relation));
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveRelationWithEmptyTo` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveRelationWithEmptyTo() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertTrue(saveRelation(relation));
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveRelationWithEmptyType` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveRelationWithEmptyType() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setTo(new AssetId(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertTrue(saveRelation(relation));
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryFetchLastOnlyTreeLike` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryFetchLastOnlyTreeLike() throws Exception {
        // A -> B
        // A -> C
        // C -> D
        // C -> E

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryFetchLastOnlySingleLinked` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryFetchLastOnlySingleLinked() throws Exception {
        // A -> B -> C -> D

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(1, relations.size());
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryFetchLastOnlyTreeLikeWithMaxLvl` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryFetchLastOnlyTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryTreeLikeWithMaxLvl` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(4, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryTreeLikeWithUnlimLvl` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryTreeLikeWithUnlimLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(6, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryLargeHierarchyFetchAllWithUnlimLvl` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryLargeHierarchyFetchAllWithUnlimLvl() throws Exception {
        AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, false);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByQueryLargeHierarchyFetchLastOnlyWithUnlimLvl` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByQueryLargeHierarchyFetchLastOnlyWithUnlimLvl() throws Exception {
        AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, true);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAssetRelationsRecursively` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void createAssetRelationsRecursively(AssetId rootAsset, int lvl, List<EntityRelation> entityRelations, boolean lastLvlOnly) throws Exception {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (lvl == 0) return;

        AssetId firstAsset = new AssetId(Uuids.timeBased());
        AssetId secondAsset = new AssetId(Uuids.timeBased());

        EntityRelation firstRelation = new EntityRelation(rootAsset, firstAsset, EntityRelation.CONTAINS_TYPE);
        EntityRelation secondRelation = new EntityRelation(rootAsset, secondAsset, EntityRelation.CONTAINS_TYPE);

        saveRelation(firstRelation);
        saveRelation(secondRelation);

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (!lastLvlOnly || lvl == 1) {
            entityRelations.add(firstRelation);
            entityRelations.add(secondRelation);
        }

        createAssetRelationsRecursively(firstAsset, lvl - 1, entityRelations, lastLvlOnly);
        createAssetRelationsRecursively(secondAsset, lvl - 1, entityRelations, lastLvlOnly);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RelationServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
