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
package org.thingsboard.server.dao.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.security.DeviceCredentials;

/**
 * 中文说明：
 * 1. 类目的：`DeviceCredentialsCaffeineCache` 是 ThingsBoard DAO 模块 中的DAO 缓存和失效事件类型，用于保存实体、配置、类型或会话相关数据的缓存键、缓存值和跨节点失效事件。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Caffeine、Redis、Spring Cache、DAO Service、Application 服务和集群事件总线。
 * 4. 生命周期：缓存对象随 Spring 缓存 Bean 存在，失效事件随单次保存、删除或配置更新流程传播。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Cache-Aside / Observer / Value Object。
 */
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DeviceCredentialsCache")
public class DeviceCredentialsCaffeineCache extends CaffeineTbTransactionalCache<String, DeviceCredentials> {

    /**
     * 功能：创建 `DeviceCredentialsCaffeineCache` 实例，并初始化必要字段。
     * 参数：
     * - `cacheManager`：管理器对象。
     * 返回：新创建的对象实例。
     */
    public DeviceCredentialsCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DEVICE_CREDENTIALS_CACHE);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceCredentialsCaffeineCache` 在 ThingsBoard DAO 模块 中承担DAO 缓存和失效事件类型职责，核心目的是保存实体、配置、类型或会话相关数据的缓存键、缓存值和跨节点失效事件。
 * 2. 核心流程：根据租户、实体或配置键读取缓存，数据库变更后发布失效事件以维持多节点一致性。
 * 3. 关键依赖：主要依赖或协作对象包括Caffeine、Redis、Spring Cache、DAO Service、Application 服务和集群事件总线。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
