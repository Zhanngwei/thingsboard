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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.asset.AssetProfile;

/**
 * 中文说明：
 * 1. `AssetProfileCaffeineCache` 是 ThingsBoard DAO 中管理资产配置缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `CaffeineTbTransactionalCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("AssetProfileCache")
public class AssetProfileCaffeineCache extends CaffeineTbTransactionalCache<AssetProfileCacheKey, AssetProfile> {

    /**
     * 功能：创建 `AssetProfileCaffeineCache` 实例，并初始化必要字段。
     * 参数：
     * - `cacheManager`：管理器对象。
     * 返回：新创建的对象实例。
     */
    public AssetProfileCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.ASSET_PROFILE_CACHE);
    }

}
