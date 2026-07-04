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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`EntityViewDao` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Graph Query。
 */
public interface EntityViewDao extends Dao<EntityView>, ExportableEntityDao<EntityViewId, EntityView> {

    /**
     * Find entity view info by id.
     *
     * @param tenantId the tenant id
     * @param assetId the asset id
     * @return the entity view info object
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewInfoById` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    EntityViewInfo findEntityViewInfoById(TenantId tenantId, UUID entityViewId);

    /**
     * Save or update device object
     *
     * @param entityView the entity-view object
     * @return saved entity-view object
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    EntityView save(TenantId tenantId, EntityView entityView);

    /**
     * Find entity views by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity view infos by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewInfosByTenantId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity views by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndType` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity view infos by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewInfosByTenantIdAndType` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity views by tenantId and entity view name.
     *
     * @param tenantId the tenantId
     * @param name the entity view name
     * @return the optional entity view object
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewByTenantIdAndName` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find entity views by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndCustomerId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                UUID customerId,
                                                                PageLink pageLink);

    /**
     * Find entity view infos by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewInfosByTenantIdAndCustomerId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find entity views by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndCustomerIdAndType` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId,
                                                                       UUID customerId,
                                                                       String type,
                                                                       PageLink pageLink);

    /**
     * Find entity view infos by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewInfosByTenantIdAndCustomerIdAndType` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndEntityId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    List<EntityView> findEntityViewsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * 方法说明：
     * 1. 职责：执行 `existsByTenantIdAndEntityId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * Find tenants entity view types.
     *
     * @return the list of tenant entity view type objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantEntityViewTypesAsync` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId);

    /**
     * Find entity views by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndEdgeId` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(UUID tenantId,
                                                            UUID edgeId,
                                                            PageLink pageLink);

    /**
     * Find entity views by tenantId, edgeId, type and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndEdgeIdAndType` 对应的实体与关系持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(UUID tenantId,
                                                            UUID edgeId,
                                                            String type,
                                                            PageLink pageLink);

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityViewDao` 在 ThingsBoard DAO 模块 中承担实体与关系持久化服务类型职责，核心目的是维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 核心流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
 * 3. 关键依赖：主要依赖或协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
