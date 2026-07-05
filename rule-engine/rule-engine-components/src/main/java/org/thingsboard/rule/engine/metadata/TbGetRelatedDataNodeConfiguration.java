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
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.Collections;
import java.util.HashMap;

/**
 * 中文说明：`TbGetRelatedDataNodeConfiguration` 是获取关联数据节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetRelatedDataNodeConfiguration extends TbGetEntityDataNodeConfiguration {

    /**
     * 查询条件，表示当前对象的对应属性。
     */
    private RelationsQuery relationsQuery;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbGetRelatedDataNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetRelatedDataNodeConfiguration();
        var dataMapping = new HashMap<String, String>();
        dataMapping.putIfAbsent("serialNumber", "sn");
        configuration.setDataMapping(dataMapping);
        configuration.setDataToFetch(DataToFetch.ATTRIBUTES);
        configuration.setFetchTo(TbMsgSource.METADATA);

        var relationsQuery = new RelationsQuery();
        var relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));
        configuration.setRelationsQuery(relationsQuery);

        return configuration;
    }

    /*
     * 本类总结：`TbGetRelatedDataNodeConfiguration` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
