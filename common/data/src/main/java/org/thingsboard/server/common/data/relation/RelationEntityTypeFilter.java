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
package org.thingsboard.server.common.data.relation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
/**
 * 中文说明：
 * 1. `RelationEntityTypeFilter` 是 ThingsBoard Common Data 中处理实体关系的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Data
@AllArgsConstructor
@ApiModel
public class RelationEntityTypeFilter {

    /**
     * 关系，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 1, value = "Type of the relation between root entity and other entity (e.g. 'Contains' or 'Manages').", example = "Contains")
    private String relationType;

    /**
     * 实体列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(position = 2, value = "Array of entity types to filter the related entities (e.g. 'DEVICE', 'ASSET').")
    private List<EntityType> entityTypes;
}
