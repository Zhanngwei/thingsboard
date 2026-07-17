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
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

/**
 * 中文说明：
 * 1. `EntityTypeExportCtx` 是 ThingsBoard Application 中围绕实体提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `EntitiesExportCtx`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class EntityTypeExportCtx extends EntitiesExportCtx<VersionCreateRequest> {

    /**
     * 实体，用于区分不同处理分支。
     */
    @Getter
    private final EntityType entityType;
    /**
     * 是否满足`overwrite`条件。
     */
    @Getter
    private final boolean overwrite;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final EntityExportSettings settings;

    /**
     * 功能：创建 `EntityTypeExportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `config`：配置对象。
     * - `defaultSyncStrategy`：`defaultSyncStrategy` 参数。
     * - `entityType`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityTypeExportCtx(EntitiesExportCtx<?> parent, EntityTypeVersionCreateConfig config, SyncStrategy defaultSyncStrategy, EntityType entityType) {
        super(parent);
        this.entityType = entityType;
        this.settings = EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .build();
        this.overwrite = ObjectUtils.defaultIfNull(config.getSyncStrategy(), defaultSyncStrategy) == SyncStrategy.OVERWRITE;
    }

}
