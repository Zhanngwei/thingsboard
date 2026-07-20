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
package org.thingsboard.server.cache.limits;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;

/**
 * 中文说明：
 * 1. `RateLimitService` 是 ThingsBoard Common Cache 中定义 `Rate Limit` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RateLimitService {

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `api`：`api` 参数。
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    boolean checkRateLimit(LimitedApi api, TenantId tenantId);

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `api`：`api` 参数。
     * - `tenantId`：租户IDID。
     * - `level`：`level` 参数。
     * 返回：判断结果。
     */
    boolean checkRateLimit(LimitedApi api, TenantId tenantId, Object level);

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `api`：`api` 参数。
     * - `level`：`level` 参数。
     * - `rateLimitConfig`：配置对象。
     * 返回：判断结果。
     */
    boolean checkRateLimit(LimitedApi api, Object level, String rateLimitConfig);

    /**
     * 功能：删除或清理`Up`。
     * 参数：
     * - `api`：`api` 参数。
     * - `level`：`level` 参数。
     * 返回：无。
     */
    void cleanUp(LimitedApi api, Object level);

}
