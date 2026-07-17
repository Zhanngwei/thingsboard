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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `CacheSpecsMapTest` 是 ThingsBoard Common Cache 中验证 `CacheSpecsMap` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CacheSpecsMap.class, TbCaffeineCacheConfiguration.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "cache.type=caffeine",
        "cache.specs.relations.timeToLiveInMinutes=1440",
        "cache.specs.relations.maxSize=0",
        "cache.specs.devices.timeToLiveInMinutes=60",
        "cache.specs.devices.maxSize=100"})
@Slf4j
public class CacheSpecsMapTest {

    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    CacheManager cacheManager;

    /**
     * 功能：校验管理器。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void verifyNotTransactionAwareCacheManagerProxy() {
        // We no longer use built-in transaction support for the caches, because we have our own cache cleanup and transaction logic that implements CAS.
        assertThat(cacheManager).isInstanceOf(SimpleCacheManager.class);
    }

    /**
     * 功能：验证 `givenCacheConfig_whenCacheManagerReady_thenVerifyExistedCachesWithNoTransactionAwareCacheDecorator` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyExistedCachesWithNoTransactionAwareCacheDecorator() {
        // We no longer use built-in transaction support for the caches, because we have our own cache cleanup and transaction logic that implements CAS.
        assertThat(cacheManager.getCache("relations")).isInstanceOf(CaffeineCache.class);
        assertThat(cacheManager.getCache("devices")).isInstanceOf(CaffeineCache.class);
    }

    /**
     * 功能：验证 `givenCacheConfig_whenCacheManagerReady_thenVerifyNonExistedCaches` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyNonExistedCaches() {
        assertThat(cacheManager.getCache("rainbows_and_unicorns")).isNull();
    }
}