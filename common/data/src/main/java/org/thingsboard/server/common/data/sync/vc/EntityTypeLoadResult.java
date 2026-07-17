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
package org.thingsboard.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `EntityTypeLoadResult` 是 ThingsBoard Common Data 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityTypeLoadResult implements Serializable {
    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -8428039809651395241L;

    /**
     * 实体，用于区分不同处理分支。
     */
    private EntityType entityType;
    private int created;
    /**
     * `updated` 字段，保存当前对象的对应属性。
     */
    private int updated;
    private int deleted;

    /**
     * 功能：创建 `EntityTypeLoadResult` 实例，并初始化必要字段。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityTypeLoadResult(EntityType entityType) {
        this.entityType = entityType;
    }
}
