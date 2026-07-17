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
package org.thingsboard.server.service.query;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 中文说明：
 * 1. `EntityQueryService` 是 ThingsBoard Application 中定义实体能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EntityQueryService {

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    long countEntitiesByQuery(SecurityUser securityUser, EntityCountQuery query);

    /**
     * 功能：获取实体。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityData> findEntityDataByQuery(SecurityUser securityUser, EntityDataQuery query);

    /**
     * 功能：获取告警。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmData> findAlarmDataByQuery(SecurityUser securityUser, AlarmDataQuery query);

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    long countAlarmsByQuery(SecurityUser securityUser, AlarmCountQuery query);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * - `isTimeseries`：`isTimeseries` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    DeferredResult<ResponseEntity> getKeysByQuery(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                                  boolean isTimeseries, boolean isAttributes, String attributesScope);

}
