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
package org.thingsboard.server.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.Weigher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbCaffeineCacheConfiguration` 是 ThingsBoard Common Cache 中描述缓存行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@EnableCaching
@Slf4j
public class TbCaffeineCacheConfiguration {

    /**
     * `configuration`映射关系，用于按键查找对应值。
     */
    private final CacheSpecsMap configuration;

    /**
     * 功能：创建 `TbCaffeineCacheConfiguration` 实例，并初始化必要字段。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：新创建的对象实例。
     */
    public TbCaffeineCacheConfiguration(CacheSpecsMap configuration) {
        this.configuration = configuration;
    }

    /**
     * Transaction aware CaffeineCache implementation with TransactionAwareCacheManagerProxy
     * to synchronize cache put/evict operations with ongoing Spring-managed transactions.
     */
    /**
     * 功能：执行 `cacheManager` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public CacheManager cacheManager() {
        log.trace("Initializing cache: {} specs {}", Arrays.toString(RemovalCause.values()), configuration.getSpecs());
        SimpleCacheManager manager = new SimpleCacheManager();
        if (configuration.getSpecs() != null) {
            List<CaffeineCache> caches =
                    configuration.getSpecs().entrySet().stream()
                            .map(entry -> buildCache(entry.getKey(),
                                    entry.getValue()))
                            .collect(Collectors.toList());
            manager.setCaches(caches);
        }

        //SimpleCacheManager is not a bean (will be wrapped), so call initializeCaches manually
        manager.initializeCaches();

        return manager;
    }

    /**
     * 功能：构建`Cache`。
     * 参数：
     * - `name`：名称。
     * - `cacheSpec`：`cacheSpec` 参数。
     * 返回：处理结果。
     */
    private CaffeineCache buildCache(String name, CacheSpecs cacheSpec) {

        final Caffeine<Object, Object> caffeineBuilder
                = Caffeine.newBuilder()
                .weigher(collectionSafeWeigher())
                .maximumWeight(cacheSpec.getMaxSize())
                .expireAfterWrite(cacheSpec.getTimeToLiveInMinutes(), TimeUnit.MINUTES)
                .ticker(ticker());
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    /**
     * 功能：执行 `ticker` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * 功能：执行 `collectionSafeWeigher` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private Weigher<? super Object, ? super Object> collectionSafeWeigher() {
        return (Weigher<Object, Object>) (key, value) -> {
            if (value instanceof Collection) {
                return ((Collection) value).size();
            }
            return 1;
        };
    }

}
