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

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.CacheConstants;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 中文说明：
 * 1. `CacheSpecsMap` 是 ThingsBoard Common Cache 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 它直接协作于缓存实现、领域标识符和调用该缓存的服务。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Configuration
@ConfigurationProperties(prefix = "cache")
@Data
public class CacheSpecsMap {

    /**
     * 刷新令牌过期时间，用于控制时间范围或等待时长。
     */
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private int refreshTokenExpTime;

    /**
     * `specs`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, CacheSpecs> specs;

    /**
     * 功能：执行 `replaceTheJWTTokenRefreshExpTime` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void replaceTheJWTTokenRefreshExpTime() {
        if (specs != null) {
            var cacheSpecs = specs.get(CacheConstants.USERS_SESSION_INVALIDATION_CACHE);
            if (cacheSpecs != null) {
                cacheSpecs.setTimeToLiveInMinutes((refreshTokenExpTime / 60) + 1);
            }
        }
    }

}
