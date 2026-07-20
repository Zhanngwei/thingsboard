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
package org.thingsboard.rule.engine.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. `TbCheckRelationNodeConfiguration` 是 ThingsBoard Rule Engine Components 中描述实体关系行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `NodeConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class TbCheckRelationNodeConfiguration implements NodeConfiguration<TbCheckRelationNodeConfiguration> {

    /**
     * `direction` 字段，保存当前对象的对应属性。
     */
    private String direction;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    private String entityId;
    /**
     * 实体，用于区分不同处理分支。
     */
    private String entityType;
    /**
     * 关系，用于区分不同处理分支。
     */
    private String relationType;
    /**
     * 是否满足实体条件。
     */
    private boolean checkForSingleEntity;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbCheckRelationNodeConfiguration defaultConfiguration() {
        var configuration = new TbCheckRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType(EntityRelation.CONTAINS_TYPE);
        configuration.setCheckForSingleEntity(true);
        return configuration;
    }
}
