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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `ExportableEntity` 是 ThingsBoard Common Data 中定义实体能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityId`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ExportableEntity<I extends EntityId> extends HasId<I>, HasName {

    /**
     * 功能：更新`Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    void setId(I id);

    /**
     * 功能：获取`External Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 100, value = "JSON object with External Id from the VCS", accessMode = ApiModelProperty.AccessMode.READ_ONLY, hidden = true)
    I getExternalId();

    /**
     * 功能：更新`External Id`。
     * 参数：
     * - `externalId`：`externalId`ID。
     * 返回：无。
     */
    void setExternalId(I externalId);

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    long getCreatedTime();

    /**
     * 功能：更新创建时间。
     * 参数：
     * - `createdTime`：`createdTime` 参数。
     * 返回：无。
     */
    void setCreatedTime(long createdTime);

    /**
     * 功能：更新租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void setTenantId(TenantId tenantId);

}
