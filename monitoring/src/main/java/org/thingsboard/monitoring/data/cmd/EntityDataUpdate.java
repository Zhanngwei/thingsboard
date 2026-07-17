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
package org.thingsboard.monitoring.data.cmd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `EntityDataUpdate` 是 ThingsBoard Monitoring 中围绕实体提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityDataUpdate {

    /**
     * `update`列表，用于保存一组待处理对象。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<EntityData> update;

    /**
     * 功能：获取`Latest`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：文本结果。
     */
    public String getLatest(UUID entityId, String key) {
        if (update == null) return null;

        return update.stream()
                .filter(entityData -> entityData.getEntityId().getId().equals(entityId)).findFirst()
                .map(EntityData::getLatest).map(latest -> latest.get(EntityKeyType.TIME_SERIES))
                .map(latest -> latest.get(key)).map(TsValue::getValue)
                .orElse(null);
    }

}
