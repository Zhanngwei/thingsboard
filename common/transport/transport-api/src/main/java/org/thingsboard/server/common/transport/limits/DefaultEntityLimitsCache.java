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
package org.thingsboard.server.common.transport.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultEntityLimitsCache` 是 ThingsBoard Common Transport 中管理实体缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `EntityLimitsCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Service
@TbTransportComponent
@Slf4j
public class DefaultEntityLimitsCache implements EntityLimitsCache {

    /**
     * `DEVIATION`常量，用于统一引用固定值。
     */
    private static final int DEVIATION = 10;
    private final Cache<EntityLimitKey, Boolean> cache;

    /**
     * 功能：创建 `DefaultEntityLimitsCache` 实例，并初始化必要字段。
     * 参数：
     * - `ttl`：`ttl` 参数。
     * - `maxSize`：`maxSize` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultEntityLimitsCache(@Value("${cache.entityLimits.timeToLiveInMinutes:5}") int ttl,
                                    @Value("${cache.entityLimits.maxSize:100000}") int maxSize) {
        // We use the 'random' expiration time to avoid peak loads.
        long mainPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * (100 - DEVIATION);
        long randomPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * DEVIATION;
        cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<EntityLimitKey, Boolean>() {
                    @Override
                    public long expireAfterCreate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime) {
                        return mainPart + (long) (randomPart * ThreadLocalRandom.current().nextDouble());
                    }

                    @Override
                    public long expireAfterUpdate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .maximumSize(maxSize)
                .build();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean get(EntityLimitKey key) {
        var result = cache.getIfPresent(key);
        return result != null ? result : false;
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void put(EntityLimitKey key, boolean value) {
        cache.put(key, value);
    }
}
