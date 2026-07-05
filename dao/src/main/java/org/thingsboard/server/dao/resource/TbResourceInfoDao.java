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
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`TbResourceInfoDao` 是 ThingsBoard DAO 模块 中的资源与 OTA 持久化服务类型，用于管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 生命周期：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
public interface TbResourceInfoDao extends Dao<TbResourceInfo> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<TbResourceInfo> findTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：处理结果。
     */
    TbResourceInfo findByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndResourceTypeAndResourceKey(TenantId tenantId, ResourceType resourceType, String resourceKey);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `prefix`：`prefix` 参数。
     * 返回：匹配的数据集合。
     */
    Set<String> findKeysByTenantIdAndResourceTypeAndResourceKeyPrefix(TenantId tenantId, ResourceType resourceType, String prefix);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `etag`：`etag` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    List<TbResourceInfo> findByTenantIdAndEtagAndKeyStartingWith(TenantId tenantId, String etag, String query);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `etag`：`etag` 参数。
     * 返回：处理结果。
     */
    TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, ResourceType resourceType, String etag);

    /**
     * 功能：判断公钥是否存在。
     * 参数：
     * - `resourceType`：类型。
     * - `publicResourceKey`：键。
     * 返回：判断结果。
     */
    boolean existsByPublicResourceKey(ResourceType resourceType, String publicResourceKey);

    /**
     * 功能：获取公钥。
     * 参数：
     * - `resourceType`：类型。
     * - `publicResourceKey`：键。
     * 返回：处理结果。
     */
    TbResourceInfo findPublicResourceByKey(ResourceType resourceType, String publicResourceKey);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbResourceInfoDao` 在 ThingsBoard DAO 模块 中承担资源与 OTA 持久化服务类型职责，核心目的是管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 核心流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
 * 3. 关键依赖：主要依赖或协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
