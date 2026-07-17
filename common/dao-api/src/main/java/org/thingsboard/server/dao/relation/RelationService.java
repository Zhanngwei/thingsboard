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
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.List;

/**
 * Created by ashvayka on 27.04.17.
 */
/**
 * 中文说明：
 * 1. `RelationService` 是 ThingsBoard Common 中定义实体关系能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RelationService {

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
    void saveRelations(TenantId tenantId, List<EntityRelation> relations);

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
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    void deleteEntityRelations(TenantId tenantId, EntityId entity);

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    void deleteEntityCommonRelations(TenantId tenantId, EntityId entity);

    /**
     * 功能：获取`By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`By From Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelation>> findByFromAsync(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelation>> findByFromAndTypeAsync(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`By To Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelation>> findByToAsync(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelationInfo>> findInfoByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelation>> findByToAndTypeAsync(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelation>> findByQuery(TenantId tenantId, EntityRelationsQuery query);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityRelationInfo>> findInfoByQuery(TenantId tenantId, EntityRelationsQuery query);

    /**
     * 功能：删除或清理`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    void removeRelations(TenantId tenantId, EntityId entityId);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findRuleNodeToRuleChainRelations(TenantId tenantId, RuleChainType ruleChainType, int limit);

//    TODO: This method may be useful for some validations in the future
//    ListenableFuture<Boolean> checkRecursiveRelation(EntityId from, EntityId to);

}
