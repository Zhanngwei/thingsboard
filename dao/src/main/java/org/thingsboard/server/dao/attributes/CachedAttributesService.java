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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.cache.CacheExecutorService;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.attributes.AttributeUtils.validate;

/**
 * 中文说明：
 * 1. `CachedAttributesService` 是 ThingsBoard DAO 中负责 `Cached Attributes` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AttributesService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Primary
@Slf4j
public class CachedAttributesService implements AttributesService {
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String STATS_NAME = "attributes.cache";
    public static final String LOCAL_CACHE_TYPE = "caffeine";

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final AttributesDao attributesDao;
    private final JpaExecutorService jpaExecutorService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final CacheExecutorService cacheExecutorService;
    private final DefaultCounter hitCounter;
    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final DefaultCounter missCounter;
    private final TbTransactionalCache<AttributeCacheKey, AttributeKvEntry> cache;
    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    private ListeningExecutorService cacheExecutor;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Value("${cache.type:caffeine}")
    private String cacheType;
    /**
     * 是否满足值条件。
     */
    @Value("${sql.attributes.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    /**
     * 功能：创建 `CachedAttributesService` 实例，并初始化必要字段。
     * 参数：
     * - `attributesDao`：`attributesDao` 参数。
     * - `jpaExecutorService`：服务对象。
     * - `statsFactory`：`statsFactory` 参数。
     * - `cacheExecutorService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public CachedAttributesService(AttributesDao attributesDao,
                                   JpaExecutorService jpaExecutorService,
                                   StatsFactory statsFactory,
                                   CacheExecutorService cacheExecutorService,
                                   TbTransactionalCache<AttributeCacheKey, AttributeKvEntry> cache) {
        this.attributesDao = attributesDao;
        this.jpaExecutorService = jpaExecutorService;
        this.cacheExecutorService = cacheExecutorService;
        this.cache = cache;

        this.hitCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "hit");
        this.missCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "miss");
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.cacheExecutor = getExecutor(cacheType, cacheExecutorService);
    }

    /**
     * Will return:
     * - for the <b>local</b> cache type (cache.type="coffeine"): directExecutor (run callback immediately in the same thread)
     * - for the <b>remote</b> cache: dedicated thread pool for the cache IO calls to unblock any caller thread
     */
    /**
     * 功能：获取执行器。
     * 参数：
     * - `cacheType`：类型。
     * - `cacheExecutorService`：服务对象。
     * 返回：匹配的数据集合。
     */
    ListeningExecutorService getExecutor(String cacheType, CacheExecutorService cacheExecutorService) {
        if (StringUtils.isEmpty(cacheType) || LOCAL_CACHE_TYPE.equals(cacheType)) {
            log.info("Going to use directExecutor for the local cache type {}", cacheType);
            return MoreExecutors.newDirectExecutorService();
        }
        log.info("Going to use cacheExecutorService for the remote cache type {}", cacheType);
        return cacheExecutorService.executor();
    }


    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKey`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);

        return cacheExecutor.submit(() -> {
            AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, attributeKey);
            TbCacheValueWrapper<AttributeKvEntry> cachedAttributeValue = cache.get(attributeCacheKey);
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                AttributeKvEntry cachedAttributeKvEntry = cachedAttributeValue.get();
                return Optional.ofNullable(cachedAttributeKvEntry);
            } else {
                missCounter.increment();
                var cacheTransaction = cache.newTransactionForKey(attributeCacheKey);
                try {
                    Optional<AttributeKvEntry> result = attributesDao.find(tenantId, entityId, scope, attributeKey);
                    cacheTransaction.putIfAbsent(attributeCacheKey, result.orElse(null));
                    cacheTransaction.commit();
                    return result;
                } catch (Throwable e) {
                    cacheTransaction.rollback();
                    log.debug("Could not find attribute from cache: [{}] [{}] [{}]", entityId, scope, attributeKey, e);
                    throw e;
                }
            }
        });
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKeysNonUnique`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, final Collection<String> attributeKeysNonUnique) {
        validate(entityId, scope);
        final var attributeKeys = new LinkedHashSet<>(attributeKeysNonUnique); // deduplicate the attributes
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));

        //CacheExecutor for Redis or DirectExecutor for local Caffeine
        return Futures.transformAsync(cacheExecutor.submit(() -> findCachedAttributes(entityId, scope, attributeKeys)),
                wrappedCachedAttributes -> {

        List<AttributeKvEntry> cachedAttributes = wrappedCachedAttributes.values().stream()
                .map(TbCacheValueWrapper::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (wrappedCachedAttributes.size() == attributeKeys.size()) {
            log.trace("[{}][{}] Found all attributes from cache: {}", entityId, scope, attributeKeys);
            return Futures.immediateFuture(cachedAttributes);
        }

        Set<String> notFoundAttributeKeys = new HashSet<>(attributeKeys);
        notFoundAttributeKeys.removeAll(wrappedCachedAttributes.keySet());

        List<AttributeCacheKey> notFoundKeys = notFoundAttributeKeys.stream().map(k -> new AttributeCacheKey(scope, entityId, k)).collect(Collectors.toList());

        // DB call should run in DB executor, not in cache-related executor
        return jpaExecutorService.submit(() -> {
            var cacheTransaction = cache.newTransactionForKeys(notFoundKeys);
            try {
                log.trace("[{}][{}] Lookup attributes from db: {}", entityId, scope, notFoundAttributeKeys);
                List<AttributeKvEntry> result = attributesDao.find(tenantId, entityId, scope, notFoundAttributeKeys);
                for (AttributeKvEntry foundInDbAttribute : result) {
                    AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, foundInDbAttribute.getKey());
                    cacheTransaction.putIfAbsent(attributeCacheKey, foundInDbAttribute);
                    notFoundAttributeKeys.remove(foundInDbAttribute.getKey());
                }
                for (String key : notFoundAttributeKeys) {
                    cacheTransaction.putIfAbsent(new AttributeCacheKey(scope, entityId, key), null);
                }
                List<AttributeKvEntry> mergedAttributes = new ArrayList<>(cachedAttributes);
                mergedAttributes.addAll(result);
                cacheTransaction.commit();
                log.trace("[{}][{}] Commit cache transaction: {}", entityId, scope, notFoundAttributeKeys);
                return mergedAttributes;
            } catch (Throwable e) {
                cacheTransaction.rollback();
                log.debug("Could not find attributes from cache: [{}] [{}] [{}]", entityId, scope, notFoundAttributeKeys, e);
                throw e;
            }
        });

        }, MoreExecutors.directExecutor()); // cacheExecutor analyse and returns results or submit to DB executor
    }

    /**
     * 功能：获取`Cached Attributes`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKeys`：键。
     * 返回：处理结果。
     */
    private Map<String, TbCacheValueWrapper<AttributeKvEntry>> findCachedAttributes(EntityId entityId, String scope, Collection<String> attributeKeys) {
        Map<String, TbCacheValueWrapper<AttributeKvEntry>> cachedAttributes = new HashMap<>();
        for (String attributeKey : attributeKeys) {
            var cachedAttributeValue = cache.get(new AttributeCacheKey(scope, entityId, attributeKey));
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                cachedAttributes.put(attributeKey, cachedAttributeValue);
            } else {
                missCounter.increment();
            }
        }
        return cachedAttributes;
    }

    /**
     * 功能：获取`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String scope) {
        validate(entityId, scope);
        // We can`t watch on cache because the keys are unknown.
        return jpaExecutorService.submit(() -> attributesDao.findAll(tenantId, entityId, scope));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds, String scope) {
        if (StringUtils.isEmpty(scope)) {
            return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
        } else {
            return attributesDao.findAllKeysByEntityIdsAndAttributeType(tenantId, entityType, entityIds, scope);
        }
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attribute`：`attribute` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String scope, AttributeKvEntry attribute) {
        validate(entityId, scope);
        AttributeUtils.validate(attribute, valueNoXssValidation);
        ListenableFuture<String> future = attributesDao.save(tenantId, entityId, scope, attribute);
        return Futures.transform(future, key -> evict(entityId, scope, attribute, key), cacheExecutor);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<String>> save(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        AttributeUtils.validate(attributes, valueNoXssValidation);

        List<ListenableFuture<String>> futures = new ArrayList<>(attributes.size());
        for (var attribute : attributes) {
            ListenableFuture<String> future = attributesDao.save(tenantId, entityId, scope, attribute);
            futures.add(Futures.transform(future, key -> evict(entityId, scope, attribute, key), cacheExecutor));
        }

        return Futures.allAsList(futures);
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attribute`：`attribute` 参数。
     * - `key`：键。
     * 返回：文本结果。
     */
    private String evict(EntityId entityId, String scope, AttributeKvEntry attribute, String key) {
        log.trace("[{}][{}][{}] Before cache evict: {}", entityId, scope, key, attribute);
        cache.evictOrPut(new AttributeCacheKey(scope, entityId, key), attribute);
        log.trace("[{}][{}][{}] after cache evict.", entityId, scope, key);
        return key;
    }

    /**
     * 功能：删除或清理`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKeys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        List<ListenableFuture<String>> futures = attributesDao.removeAll(tenantId, entityId, scope, attributeKeys);
        return Futures.allAsList(futures.stream().map(future -> Futures.transform(future, key -> {
            cache.evict(new AttributeCacheKey(scope, entityId, key));
            return key;
        }, cacheExecutor)).collect(Collectors.toList()));
    }

}
