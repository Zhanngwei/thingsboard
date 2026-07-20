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
package org.thingsboard.server.dao.sql.relation;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
/**
 * 中文说明：
 * 1. `JpaRelationDao` 是 ThingsBoard DAO 中负责实体关系存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDaoListeningExecutorService`、`RelationDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
public class JpaRelationDao extends JpaAbstractDaoListeningExecutorService implements RelationDao {

    private static final List<String> ALL_TYPE_GROUP_NAMES = new ArrayList<>();

    static {
        Arrays.stream(RelationTypeGroup.values()).map(RelationTypeGroup::name).forEach(ALL_TYPE_GROUP_NAMES::add);
    }

    /**
     * 关系，用于读取或保存对应领域对象。
     */
    @Autowired
    private RelationRepository relationRepository;

    /**
     * 关系，用于读取或保存对应领域对象。
     */
    @Autowired
    private RelationInsertRepository relationInsertRepository;

    /**
     * 功能：获取`All By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        typeGroup.name()));
    }

    /**
     * 功能：获取`All By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroupIn(
                        from.getId(),
                        from.getEntityType().name(),
                        ALL_TYPE_GROUP_NAMES));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

    /**
     * 功能：获取`All By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        typeGroup.name()));
    }

    /**
     * 功能：获取`All By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroupIn(
                        to.getId(),
                        to.getEntityType().name(),
                        ALL_TYPE_GROUP_NAMES));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

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
    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return service.submit(() -> checkRelation(tenantId, from, to, relationType, typeGroup));
    }

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
    @Override
    public boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return relationRepository.existsById(key);
    }

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
    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return DaoUtil.getData(relationRepository.findById(key));
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：处理结果。
     */
    private RelationCompositeKey getRelationCompositeKey(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return new RelationCompositeKey(from.getId(),
                from.getEntityType().name(),
                to.getId(),
                to.getEntityType().name(),
                relationType,
                typeGroup.name());
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        return relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null;
    }

    /**
     * 功能：保存或创建`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relations`：数据列表。
     * 返回：无。
     */
    @Override
    public void saveRelations(TenantId tenantId, Collection<EntityRelation> relations) {
        List<RelationEntity> entities = relations.stream().map(RelationEntity::new).collect(Collectors.toList());
        relationInsertRepository.saveOrUpdate(entities);
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        return service.submit(() -> relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null);
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return deleteRelationIfExists(key);
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

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
    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return deleteRelationIfExists(key);
    }

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
    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    private boolean deleteRelationIfExists(RelationCompositeKey key) {
        boolean relationExistsBeforeDelete = relationRepository.existsById(key);
        if (relationExistsBeforeDelete) {
            try {
                relationRepository.deleteById(key);
            } catch (DataAccessException e) {
                log.debug("[{}] Concurrency exception while deleting relation", key, e);
            }
        }
        return relationExistsBeforeDelete;
    }

    /**
     * 功能：删除或清理`Outbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    @Override
    public void deleteOutboundRelations(TenantId tenantId, EntityId entity) {
        try {
            relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
    }

    /**
     * 功能：删除或清理`Outbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    @Override
    public void deleteOutboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup) {
        try {
            relationRepository.deleteByFromIdAndFromTypeAndRelationTypeGroupIn(entity.getId(), entity.getEntityType().name(), Collections.singletonList(relationTypeGroup.name()));
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
    }

    /**
     * 功能：删除或清理`Inbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    @Override
    public void deleteInboundRelations(TenantId tenantId, EntityId entity) {
        try {
            relationRepository.deleteByToIdAndToTypeAndRelationTypeGroupIn(entity.getId(), entity.getEntityType().name(), ALL_TYPE_GROUP_NAMES);
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
    }

    /**
     * 功能：删除或清理`Inbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    @Override
    public void deleteInboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup) {
        try {
            relationRepository.deleteByToIdAndToTypeAndRelationTypeGroupIn(entity.getId(), entity.getEntityType().name(), Collections.singletonList(relationTypeGroup.name()));
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
    }

    /**
     * 功能：删除或清理`Outbound Relations Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity) {
        return service.submit(
                () -> {
                    boolean relationExistsBeforeDelete = relationRepository
                            .findAllByFromIdAndFromType(entity.getId(), entity.getEntityType().name())
                            .size() > 0;
                    if (relationExistsBeforeDelete) {
                        relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
                    }
                    return relationExistsBeforeDelete;
                });
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit) {
        return DaoUtil.convertDataList(relationRepository.findRuleNodeToRuleChainRelations(ruleChainType, PageRequest.of(0, limit)));
    }
}
