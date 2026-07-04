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
package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityWithDataDao;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`TbResourceDao` 是 ThingsBoard DAO 模块 中的资源与 OTA 持久化服务类型，用于管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 生命周期：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
public interface TbResourceDao extends Dao<TbResource>, TenantEntityWithDataDao, ExportableEntityDao<TbResourceId, TbResource> {

    /**
     * 方法说明：
     * 1. 职责：执行 `findResourceByTenantIdAndKey` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    TbResource findResourceByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceId);

    /**
     * 方法说明：
     * 1. 职责：执行 `findAllByTenantId` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<TbResource> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 方法说明：
     * 1. 职责：执行 `findResourcesByTenantIdAndResourceType` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    PageData<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                ResourceType resourceType,
                                                                PageLink pageLink);

    /**
     * 方法说明：
     * 1. 职责：执行 `findResourcesByTenantIdAndResourceType` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    List<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                            ResourceType resourceType,
                                                            String[] objectIds,
                                                            String searchText);

    /**
     * 方法说明：
     * 1. 职责：执行 `getResourceData` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    byte[] getResourceData(TenantId tenantId, TbResourceId resourceId);

    /**
     * 方法说明：
     * 1. 职责：执行 `getResourcePreview` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    byte[] getResourcePreview(TenantId tenantId, TbResourceId resourceId);

    /**
     * 方法说明：
     * 1. 职责：执行 `getResourceSize` 对应的资源与 OTA 持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    long getResourceSize(TenantId tenantId, TbResourceId resourceId);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbResourceDao` 在 ThingsBoard DAO 模块 中承担资源与 OTA 持久化服务类型职责，核心目的是管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 核心流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
 * 3. 关键依赖：主要依赖或协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
