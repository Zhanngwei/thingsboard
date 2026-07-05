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
package org.thingsboard.server.dao.asset;

import lombok.Data;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. 类目的：`AssetProfileCacheKey` 是 ThingsBoard DAO 模块 中的DAO 缓存和失效事件类型，用于保存实体、配置、类型或会话相关数据的缓存键、缓存值和跨节点失效事件。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Caffeine、Redis、Spring Cache、DAO Service、Application 服务和集群事件总线。
 * 4. 生命周期：缓存对象随 Spring 缓存 Bean 存在，失效事件随单次保存、删除或配置更新流程传播。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Cache-Aside / Observer / Value Object。
 */
@Data
public class AssetProfileCacheKey implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8220455917177676472L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final String name;
    /**
     * 资产配置ID，用于定位对应业务对象。
     */
    private final AssetProfileId assetProfileId;
    private final boolean defaultProfile;

    /**
     * 功能：创建 `AssetProfileCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `assetProfileId`：资产配置ID。
     * - `defaultProfile`：`defaultProfile` 参数。
     * 返回：新创建的对象实例。
     */
    private AssetProfileCacheKey(TenantId tenantId, String name, AssetProfileId assetProfileId, boolean defaultProfile) {
        this.tenantId = tenantId;
        this.name = name;
        this.assetProfileId = assetProfileId;
        this.defaultProfile = defaultProfile;
    }

    /**
     * 功能：执行 `fromName` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    public static AssetProfileCacheKey fromName(TenantId tenantId, String name) {
        return new AssetProfileCacheKey(tenantId, name, null, false);
    }

    /**
     * 功能：执行 `fromId` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    public static AssetProfileCacheKey fromId(AssetProfileId id) {
        return new AssetProfileCacheKey(null, null, id, false);
    }

    /**
     * 功能：执行 `defaultProfile` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    public static AssetProfileCacheKey defaultProfile(TenantId tenantId) {
        return new AssetProfileCacheKey(tenantId, null, null, true);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        if (assetProfileId != null) {
            return assetProfileId.toString();
        } else if (defaultProfile) {
            return tenantId.toString();
        } else {
            return tenantId + "_" + name;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetProfileCacheKey` 在 ThingsBoard DAO 模块 中承担DAO 缓存和失效事件类型职责，核心目的是保存实体、配置、类型或会话相关数据的缓存键、缓存值和跨节点失效事件。
 * 2. 核心流程：根据租户、实体或配置键读取缓存，数据库变更后发布失效事件以维持多节点一致性。
 * 3. 关键依赖：主要依赖或协作对象包括Caffeine、Redis、Spring Cache、DAO Service、Application 服务和集群事件总线。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
