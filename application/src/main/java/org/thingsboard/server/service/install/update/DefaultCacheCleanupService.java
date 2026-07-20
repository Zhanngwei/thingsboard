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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.RESOURCE_INFO_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.SECURITY_SETTINGS_CACHE;

/**
 * 中文说明：
 * 1. `DefaultCacheCleanupService` 是 ThingsBoard Application 中负责缓存的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `CacheCleanupService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@RequiredArgsConstructor
@Service
@Profile("install")
@Slf4j
public class DefaultCacheCleanupService implements CacheCleanupService {

    /**
     * 管理器，负责处理对应任务或消息。
     */
    private final CacheManager cacheManager;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;


    /**
     * Cleanup caches that can not deserialize anymore due to schema upgrade or data update using sql scripts.
     * Refer to SqlDatabaseUpgradeService and /data/upgrage/*.sql
     * to discover which tables were changed
     * */
    /**
     * 功能：删除或清理`Cache`。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * 返回：无。
     */
    @Override
    public void clearCache(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "3.6.1":
                log.info("Clearing cache to upgrade from version 3.6.1 to 3.6.2");
                clearCacheByName(SECURITY_SETTINGS_CACHE);
                clearCacheByName(RESOURCE_INFO_CACHE);
                break;
            default:
                //Do nothing, since cache cleanup is optional.
        }
    }

    /**
     * 功能：删除或清理`All Caches`。
     * 参数：无。
     * 返回：无。
     */
    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(this::clearCacheByName);
    }

    /**
     * 功能：删除或清理名称。
     * 参数：
     * - `cacheName`：名称。
     * 返回：无。
     */
    void clearCacheByName(final String cacheName) {
        log.info("Clearing cache [{}]", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        Objects.requireNonNull(cache, "Cache does not exist for name " + cacheName);
        cache.clear();
    }

    /**
     * 功能：删除或清理`All`。
     * 参数：无。
     * 返回：无。
     */
    void clearAll() {
        if (redisTemplate.isPresent()) {
            log.info("Flushing all caches");
            redisTemplate.get().execute((RedisCallback<Object>) connection -> {
                connection.flushAll();
                return null;
            });
            return;
        }
        cacheManager.getCacheNames().forEach(this::clearCacheByName);
    }
}
