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
package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.Collection;
import java.util.List;

/**
 * Created by ashvayka on 25.04.17.
 */
/**
 * 中文说明：
 * 1. `RelationDao` 是 ThingsBoard DAO 中定义实体关系能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RelationDao {

    /**
     * 功能：获取`All By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`All By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`All By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`All By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：校验关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：校验关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    boolean saveRelation(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：保存或创建`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relations`：数据列表。
     * 返回：无。
     */
    void saveRelations(TenantId tenantId, Collection<EntityRelation> relations);

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    boolean deleteRelation(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：删除或清理`Outbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    void deleteOutboundRelations(TenantId tenantId, EntityId entity);

    /**
     * 功能：删除或清理`Outbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    void deleteOutboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup);

    /**
     * 功能：删除或清理`Inbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    void deleteInboundRelations(TenantId tenantId, EntityId entity);

    /**
     * 功能：删除或清理`Inbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    void deleteInboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup);

    /**
     * 功能：删除或清理`Outbound Relations Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit);
}
