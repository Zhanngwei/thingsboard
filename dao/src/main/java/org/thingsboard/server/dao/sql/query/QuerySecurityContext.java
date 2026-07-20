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
package org.thingsboard.server.dao.sql.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `QuerySecurityContext` 是 ThingsBoard DAO 中承载安全配置信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@AllArgsConstructor
public class QuerySecurityContext {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    private final TenantId tenantId;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Getter
    private final CustomerId customerId;
    /**
     * 实体，用于区分不同处理分支。
     */
    @Getter
    private final EntityType entityType;
    /**
     * 是否忽略对应检查。
     */
    @Getter
    private final boolean ignorePermissionCheck;

    /**
     * 功能：创建 `QuerySecurityContext` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityType`：实体对象。
     * 返回：新创建的对象实例。
     */
    public QuerySecurityContext(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        this(tenantId, customerId, entityType, false);
    }
}