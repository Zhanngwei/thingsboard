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
package org.thingsboard.server.dao.rule;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.rule.RuleNodeUpdateResult;
import org.thingsboard.server.common.data.util.ReflectionUtils;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.TENANT;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validatePositiveNumber;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * Created by igor on 3/12/18.
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseRuleChainService` 是 ThingsBoard DAO 模块 中的规则链持久化服务类型，用于维护规则链、规则节点、节点状态和规则引擎元数据的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括RuleChainService、RuleNodeStateService、Rule Engine、Actor、缓存和审计服务。
 * 4. 生命周期：由规则链编辑、部署、导入导出、节点状态保存或规则引擎加载流程调用。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Command。
 */
@Service("RuleChainDaoService")
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

    /**
     * `DEFAULT_PAGE_SIZE`常量，用于统一引用固定值。
     */
    private static final int DEFAULT_PAGE_SIZE = 1000;

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String TB_RULE_CHAIN_INPUT_NODE = "org.thingsboard.rule.engine.flow.TbRuleChainInputNode";
    /**
     * 规则链，用于读取或保存对应领域对象。
     */
    @Autowired
    private RuleChainDao ruleChainDao;

    /**
     * 规则节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private RuleNodeDao ruleNodeDao;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityCountService entityCountService;

    /**
     * 规则链对象，用于描述当前业务场景。
     */
    @Autowired
    private DataValidator<RuleChain> ruleChainValidator;

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    @Override
    @Transactional
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        try {
            RuleChain savedRuleChain = ruleChainDao.save(ruleChain.getTenantId(), ruleChain);
            if (ruleChain.getId() == null) {
                entityCountService.publishCountEntityEvictEvent(ruleChain.getTenantId(), EntityType.RULE_CHAIN);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedRuleChain.getTenantId())
                    .entity(savedRuleChain).entityId(savedRuleChain.getId()).created(ruleChain.getId() == null).build());
            return savedRuleChain;
        } catch (Exception e) {
            checkConstraintViolation(e, "rule_chain_external_id_unq_key", "Rule Chain with such external id already exists!");
            throw e;
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    @Override
    @Transactional
    public boolean setRootRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (!ruleChain.isRoot()) {
            RuleChain previousRootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
            if (previousRootRuleChain == null) {
                setRootAndSave(tenantId, ruleChain);
                return true;
            } else if (!previousRootRuleChain.getId().equals(ruleChain.getId())) {
                previousRootRuleChain.setRoot(false);
                ruleChainDao.save(tenantId, previousRootRuleChain);
                setRootAndSave(tenantId, ruleChain);
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：更新`Root And Save`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：无。
     */
    private void setRootAndSave(TenantId tenantId, RuleChain ruleChain) {
        ruleChain.setRoot(true);
        ruleChainDao.save(tenantId, ruleChain);
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainMetaData`：待处理数据。
     * - `ruleNodeUpdater`：`ruleNodeUpdater` 参数。
     * 返回：处理结果。
     */
    @Override
    @Transactional
    public RuleChainUpdateResult saveRuleChainMetaData(TenantId tenantId, RuleChainMetaData ruleChainMetaData, Function<RuleNode, RuleNode> ruleNodeUpdater) {
        Validator.validateId(ruleChainMetaData.getRuleChainId(), "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainMetaData.getRuleChainId());
        if (ruleChain == null) {
            return RuleChainUpdateResult.failed();
        }
        RuleChainDataValidator.validateMetaDataFieldsAndConnections(ruleChainMetaData);

        List<RuleNode> nodes = ruleChainMetaData.getNodes();
        List<RuleNode> toAddOrUpdate = new ArrayList<>();
        List<RuleNode> toDelete = new ArrayList<>();
        List<EntityRelation> relations = new ArrayList<>();

        Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        if (nodes != null) {
            for (RuleNode node : nodes) {
                setSingletonMode(node);
                if (node.getId() != null) {
                    ruleNodeIndexMap.put(node.getId(), nodes.indexOf(node));
                } else {
                    toAddOrUpdate.add(node);
                }
            }
        }

        List<RuleNodeUpdateResult> updatedRuleNodes = new ArrayList<>();
        List<RuleNode> existingRuleNodes = getRuleChainNodes(tenantId, ruleChainMetaData.getRuleChainId());
        for (RuleNode existingNode : existingRuleNodes) {
            deleteEntityRelations(tenantId, existingNode.getId());
            Integer index = ruleNodeIndexMap.get(existingNode.getId());
            RuleNode newRuleNode = null;
            if (index != null) {
                newRuleNode = ruleChainMetaData.getNodes().get(index);
                toAddOrUpdate.add(newRuleNode);
            } else {
                updatedRuleNodes.add(new RuleNodeUpdateResult(existingNode, null));
                toDelete.add(existingNode);
            }
            updatedRuleNodes.add(new RuleNodeUpdateResult(existingNode, newRuleNode));
        }
        RuleChainId ruleChainId = ruleChain.getId();
        if (nodes != null) {
            for (RuleNode node : toAddOrUpdate) {
                node.setRuleChainId(ruleChainId);
                node = ruleNodeUpdater.apply(node);
                RuleChainDataValidator.validateRuleNode(node);
                RuleNode savedNode = ruleNodeDao.save(tenantId, node);
                relations.add(new EntityRelation(ruleChainMetaData.getRuleChainId(), savedNode.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                int index = nodes.indexOf(node);
                nodes.set(index, savedNode);
                ruleNodeIndexMap.put(savedNode.getId(), index);
            }
        }
        if (!toDelete.isEmpty()) {
            deleteRuleNodes(tenantId, toDelete);
        }
        RuleNodeId firstRuleNodeId = null;
        if (nodes != null) {
            if (ruleChainMetaData.getFirstNodeIndex() != null) {
                firstRuleNodeId = nodes.get(ruleChainMetaData.getFirstNodeIndex()).getId();
            }
            if ((ruleChain.getFirstRuleNodeId() != null && !ruleChain.getFirstRuleNodeId().equals(firstRuleNodeId))
                    || (ruleChain.getFirstRuleNodeId() == null && firstRuleNodeId != null)) {
                ruleChain.setFirstRuleNodeId(firstRuleNodeId);
                ruleChainDao.save(tenantId, ruleChain);
            }
            if (ruleChainMetaData.getConnections() != null) {
                for (NodeConnectionInfo nodeConnection : ruleChainMetaData.getConnections()) {
                    EntityId from = nodes.get(nodeConnection.getFromIndex()).getId();
                    EntityId to = nodes.get(nodeConnection.getToIndex()).getId();
                    String type = nodeConnection.getType();
                    relations.add(new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE));
                }
            }
            if (ruleChainMetaData.getRuleChainConnections() != null) {
                for (RuleChainConnectionInfo nodeToRuleChainConnection : ruleChainMetaData.getRuleChainConnections()) {
                    RuleChainId targetRuleChainId = nodeToRuleChainConnection.getTargetRuleChainId();
                    RuleChain targetRuleChain = findRuleChainById(TenantId.SYS_TENANT_ID, targetRuleChainId);
                    RuleNode targetNode = new RuleNode();
                    targetNode.setName(targetRuleChain != null ? targetRuleChain.getName() : "Rule Chain Input");
                    targetNode.setRuleChainId(ruleChainId);
                    targetNode.setType("org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
                    var configuration = JacksonUtil.newObjectNode();
                    configuration.put("ruleChainId", targetRuleChainId.getId().toString());
                    targetNode.setConfiguration(configuration);
                    ObjectNode layout = (ObjectNode) nodeToRuleChainConnection.getAdditionalInfo();
                    layout.remove("description");
                    layout.remove("ruleChainNodeId");
                    targetNode.setAdditionalInfo(layout);
                    targetNode.setDebugMode(false);
                    targetNode = ruleNodeDao.save(tenantId, targetNode);

                    EntityRelation sourceRuleChainToRuleNode = new EntityRelation();
                    sourceRuleChainToRuleNode.setFrom(ruleChainId);
                    sourceRuleChainToRuleNode.setTo(targetNode.getId());
                    sourceRuleChainToRuleNode.setType(EntityRelation.CONTAINS_TYPE);
                    sourceRuleChainToRuleNode.setTypeGroup(RelationTypeGroup.RULE_CHAIN);
                    relations.add(sourceRuleChainToRuleNode);

                    EntityRelation sourceRuleNodeToTargetRuleNode = new EntityRelation();
                    EntityId from = nodes.get(nodeToRuleChainConnection.getFromIndex()).getId();
                    sourceRuleNodeToTargetRuleNode.setFrom(from);
                    sourceRuleNodeToTargetRuleNode.setTo(targetNode.getId());
                    sourceRuleNodeToTargetRuleNode.setType(nodeToRuleChainConnection.getType());
                    sourceRuleNodeToTargetRuleNode.setTypeGroup(RelationTypeGroup.RULE_NODE);
                    relations.add(sourceRuleNodeToTargetRuleNode);
                }
            }
        }

        if (!relations.isEmpty()) {
            relationService.saveRelations(tenantId, relations);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(ruleChain).entityId(ruleChain.getId()).build());

        return RuleChainUpdateResult.successful(updatedRuleNodes);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    @Override
    public RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        if (ruleChain == null) {
            return null;
        }
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        List<RuleNode> ruleNodes = getRuleChainNodes(tenantId, ruleChainId);
        Collections.sort(ruleNodes, Comparator.comparingLong(RuleNode::getCreatedTime).thenComparing(RuleNode::getId, Comparator.comparing(RuleNodeId::getId)));
        Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        for (RuleNode node : ruleNodes) {
            ruleNodeIndexMap.put(node.getId(), ruleNodes.indexOf(node));
        }
        ruleChainMetaData.setNodes(ruleNodes);
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChainMetaData.setFirstNodeIndex(ruleNodeIndexMap.get(ruleChain.getFirstRuleNodeId()));
        }
        for (RuleNode node : ruleNodes) {
            int fromIndex = ruleNodeIndexMap.get(node.getId());
            List<EntityRelation> nodeRelations = getRuleNodeRelations(tenantId, node.getId());
            for (EntityRelation nodeRelation : nodeRelations) {
                String type = nodeRelation.getType();
                if (nodeRelation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    RuleNodeId toNodeId = new RuleNodeId(nodeRelation.getTo().getId());
                    int toIndex = ruleNodeIndexMap.get(toNodeId);
                    ruleChainMetaData.addConnectionInfo(fromIndex, toIndex, type);
                } else if (nodeRelation.getTo().getEntityType() == EntityType.RULE_CHAIN) {
                    log.warn("[{}][{}] Unsupported node relation: {}", tenantId, ruleChainId, nodeRelation.getTo());
                }
            }
        }
        if (ruleChainMetaData.getConnections() != null) {
            Collections.sort(ruleChainMetaData.getConnections(), Comparator.comparingInt(NodeConnectionInfo::getFromIndex)
                    .thenComparing(NodeConnectionInfo::getToIndex).thenComparing(NodeConnectionInfo::getType));
        }
        return ruleChainMetaData;
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    @Override
    public RuleChain findRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findById(tenantId, ruleChainId.getId());
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    @Override
    public RuleNode findRuleNodeById(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findById(tenantId, ruleNodeId.getId());
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findByIdAsync(tenantId, ruleChainId.getId());
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findByIdAsync(tenantId, ruleNodeId.getId());
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public RuleChain getRootTenantRuleChain(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        return ruleChainDao.findRootRuleChainByTenantIdAndType(tenantId.getId(), RuleChainType.CORE);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        List<RuleNode> ruleNodes = new ArrayList<>();
        for (EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getTo().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            } else {
                relationService.deleteRelation(tenantId, relation);
            }
        }
        return ruleNodes;
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getNodeToRuleChainRelations(tenantId, ruleChainId);
        List<RuleNode> ruleNodes = new ArrayList<>();
        for (EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getFrom().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            }
        }
        return ruleNodes;
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, ruleNodeId, RelationTypeGroup.RULE_NODE);
        List<EntityRelation> validRelations = new ArrayList<>();
        for (EntityRelation relation : relations) {
            boolean valid = true;
            EntityType toType = relation.getTo().getEntityType();
            if (toType == EntityType.RULE_NODE || toType == EntityType.RULE_CHAIN) {
                BaseData<?> entity;
                if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    entity = ruleNodeDao.findById(tenantId, relation.getTo().getId());
                } else {
                    entity = ruleChainDao.findById(tenantId, relation.getTo().getId());
                }
                if (entity == null) {
                    relationService.deleteRelation(tenantId, relation);
                    valid = false;
                }
            }
            if (valid) {
                validRelations.add(relation);
            }
        }
        return validRelations;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleChain> findTenantRuleChainsByType(TenantId tenantId, RuleChainType type, PageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink);
        return ruleChainDao.findRuleChainsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public Collection<RuleChain> findTenantRuleChainsByTypeAndName(TenantId tenantId, RuleChainType type, String name) {
        return ruleChainDao.findByTenantIdAndTypeAndName(tenantId, type, name);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (ruleChain != null) {
            if (ruleChain.isRoot()) {
                throw new DataValidationException("Deletion of Root Tenant Rule Chain is prohibited!");
            }
            if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                PageData<Edge> pageData;
                do {
                    pageData = edgeService.findEdgesByTenantIdAndEntityId(tenantId, ruleChainId, pageLink);
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        for (Edge edge : pageData.getData()) {
                            if (edge.getRootRuleChainId() != null && edge.getRootRuleChainId().equals(ruleChainId)) {
                                throw new DataValidationException("Can't delete rule chain that is root for edge [" + edge.getName() + "]. Please assign another root rule chain first to the edge!");
                            }
                        }
                        if (pageData.hasNext()) {
                            pageLink = pageLink.nextPageLink();
                        }
                    }
                } while (pageData != null && pageData.hasNext());
            }
        }
        checkRuleNodesAndDelete(tenantId, ruleChainId);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：执行 `exportTenantRuleChains` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChainData exportTenantRuleChains(TenantId tenantId, PageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink);
        PageData<RuleChain> ruleChainData = ruleChainDao.findRuleChainsByTenantId(tenantId.getId(), pageLink);
        List<RuleChain> ruleChains = ruleChainData.getData();
        List<RuleChainMetaData> metadata = ruleChains.stream().map(rc -> loadRuleChainMetaData(tenantId, rc.getId())).collect(Collectors.toList());
        RuleChainData rcData = new RuleChainData();
        rcData.setRuleChains(ruleChains);
        rcData.setMetadata(metadata);
        setRandomRuleChainIds(rcData);
        resetRuleNodeIds(metadata);
        return rcData;
    }

    /**
     * 功能：执行 `importTenantRuleChains` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainData`：待处理数据。
     * - `overwrite`：`overwrite` 参数。
     * - `ruleNodeUpdater`：`ruleNodeUpdater` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleChainImportResult> importTenantRuleChains(TenantId tenantId, RuleChainData ruleChainData, boolean overwrite, Function<RuleNode, RuleNode> ruleNodeUpdater) {
        List<RuleChainImportResult> importResults = new ArrayList<>();

        setRandomRuleChainIds(ruleChainData);
        resetRuleNodeIds(ruleChainData.getMetadata());
        resetRuleChainMetadataTenantIds(tenantId, ruleChainData.getMetadata());

        for (RuleChain ruleChain : ruleChainData.getRuleChains()) {
            RuleChainImportResult importResult = new RuleChainImportResult();

            ruleChain.setTenantId(tenantId);
            ruleChain.setRoot(false);

            if (overwrite) {
                Collection<RuleChain> existingRuleChains = findTenantRuleChainsByTypeAndName(tenantId,
                        Optional.ofNullable(ruleChain.getType()).orElse(RuleChainType.CORE), ruleChain.getName());
                Optional<RuleChain> existingRuleChain = existingRuleChains.stream().findFirst();
                if (existingRuleChain.isPresent()) {
                    setNewRuleChainId(ruleChain, ruleChainData.getMetadata(), ruleChain.getId(), existingRuleChain.get().getId());
                    ruleChain.setRoot(existingRuleChain.get().isRoot());
                    importResult.setUpdated(true);
                }
            }

            try {
                ruleChain = saveRuleChain(ruleChain);
            } catch (Exception e) {
                importResult.setError(ExceptionUtils.getRootCauseMessage(e));
            }

            importResult.setTenantId(tenantId);
            importResult.setRuleChainId(ruleChain.getId());
            importResult.setRuleChainName(ruleChain.getName());
            importResults.add(importResult);
        }

        if (CollectionUtils.isNotEmpty(ruleChainData.getMetadata())) {
            ruleChainData.getMetadata().forEach(md -> saveRuleChainMetaData(tenantId, md, ruleNodeUpdater));
        }

        return importResults;
    }

    /**
     * 功能：执行 `resetRuleChainMetadataTenantIds` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `metaData`：待处理数据。
     * 返回：无。
     */
    private void resetRuleChainMetadataTenantIds(TenantId tenantId, List<RuleChainMetaData> metaData) {
        for (RuleChainMetaData md : metaData) {
            for (RuleNode node : md.getNodes()) {
                JsonNode nodeConfiguration = node.getConfiguration();
                searchTenantIdRecursive(tenantId, nodeConfiguration);
            }
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `node`：`node` 参数。
     * 返回：无。
     */
    private void searchTenantIdRecursive(TenantId tenantId, JsonNode node) {
        Iterator<String> iter = node.fieldNames();
        boolean isTenantId = false;
        while (iter.hasNext()) {
            String field = iter.next();
            if ("entityType".equals(field) && TENANT.equals(node.get(field).asText())) {
                isTenantId = true;
                break;
            }
        }
        if (isTenantId) {
            ObjectNode objNode = (ObjectNode) node;
            if (objNode.has("id")) {
                objNode.put("id", tenantId.getId().toString());
            }
        } else {
            for (JsonNode jsonNode : node) {
                searchTenantIdRecursive(tenantId, jsonNode);
            }
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChainData`：待处理数据。
     * 返回：无。
     */
    private void setRandomRuleChainIds(RuleChainData ruleChainData) {
        for (RuleChain ruleChain : ruleChainData.getRuleChains()) {
            RuleChainId oldRuleChainId = ruleChain.getId();
            RuleChainId newRuleChainId = new RuleChainId(Uuids.timeBased());
            setNewRuleChainId(ruleChain, ruleChainData.getMetadata(), oldRuleChainId, newRuleChainId);
            ruleChain.setTenantId(null);
        }
    }

    /**
     * 功能：执行 `resetRuleNodeIds` 对应的处理。
     * 参数：
     * - `metaData`：待处理数据。
     * 返回：无。
     */
    private void resetRuleNodeIds(List<RuleChainMetaData> metaData) {
        for (RuleChainMetaData md : metaData) {
            for (RuleNode node : md.getNodes()) {
                node.setId(null);
                node.setRuleChainId(null);
            }
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * - `metadata`：待处理数据。
     * - `oldRuleChainId`：规则链ID。
     * - `newRuleChainId`：规则链ID。
     * 返回：无。
     */
    private void setNewRuleChainId(RuleChain ruleChain, List<RuleChainMetaData> metadata, RuleChainId oldRuleChainId, RuleChainId newRuleChainId) {
        ruleChain.setId(newRuleChainId);
        for (RuleChainMetaData metaData : metadata) {
            if (metaData.getRuleChainId().equals(oldRuleChainId)) {
                metaData.setRuleChainId(newRuleChainId);
            }
            if (!CollectionUtils.isEmpty(metaData.getRuleChainConnections())) {
                for (RuleChainConnectionInfo rcConnInfo : metaData.getRuleChainConnections()) {
                    if (rcConnInfo.getTargetRuleChainId().equals(oldRuleChainId)) {
                        rcConnInfo.setTargetRuleChainId(newRuleChainId);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(metaData.getNodes())) {
                metaData.getNodes().stream()
                        .filter(ruleNode -> ruleNode.getType().equals(TB_RULE_CHAIN_INPUT_NODE))
                        .forEach(ruleNode -> {
                            ObjectNode configuration = (ObjectNode) ruleNode.getConfiguration();
                            if (configuration.has("ruleChainId")) {
                                if (configuration.get("ruleChainId").asText().equals(oldRuleChainId.toString())) {
                                    configuration.put("ruleChainId", newRuleChainId.toString());
                                    ruleNode.setConfiguration(configuration);
                                }
                            }
                        });
            }
        }
    }

    /**
     * 功能：执行 `assignRuleChainToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    @Override
    public RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign ruleChain to non-existent edge!");
        }
        if (!edge.getTenantId().equals(ruleChain.getTenantId())) {
            throw new DataValidationException("Can't assign ruleChain to edge from different tenant!");
        }
        if (!RuleChainType.EDGE.equals(ruleChain.getType())) {
            throw new DataValidationException("Can't assign non EDGE ruleChain to edge!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create ruleChain relation. Edge Id: [{}]", ruleChainId, edgeId);
            throw new RuntimeException(e);
        }
        if (!ruleChainId.equals(edge.getRootRuleChainId())) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(ruleChainId)
                    .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        }
        return ruleChain;
    }

    /**
     * 功能：执行 `unassignRuleChainFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `edgeId`：边缘节点ID。
     * - `remove`：`remove` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId, boolean remove) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign rule chain from non-existent edge!");
        }
        if (!remove && edge.getRootRuleChainId() != null && edge.getRootRuleChainId().equals(ruleChainId)) {
            throw new DataValidationException("Can't unassign root rule chain from edge [" + edge.getName() + "]. Please assign another root rule chain first!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete rule chain relation. Edge Id: [{}]", ruleChainId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(ruleChainId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return ruleChain;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findRuleChainsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        Validator.validatePageLink(pageLink);
        return ruleChainDao.findRuleChainsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public RuleChain getEdgeTemplateRootRuleChain(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        return ruleChainDao.findRootRuleChainByTenantIdAndType(tenantId.getId(), RuleChainType.EDGE);
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    @Override
    public boolean setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        RuleChain previousEdgeTemplateRootRuleChain = getEdgeTemplateRootRuleChain(ruleChain.getTenantId());
        if (previousEdgeTemplateRootRuleChain == null || !previousEdgeTemplateRootRuleChain.getId().equals(ruleChain.getId())) {
            try {
                if (previousEdgeTemplateRootRuleChain != null) {
                    previousEdgeTemplateRootRuleChain.setRoot(false);
                    ruleChainDao.save(tenantId, previousEdgeTemplateRootRuleChain);
                }
                ruleChain.setRoot(true);
                ruleChainDao.save(tenantId, ruleChain);
                return true;
            } catch (Exception e) {
                log.warn("Failed to set edge template root rule chain, ruleChainId: [{}]", ruleChainId, e);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    @Override
    public boolean setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            createRelation(tenantId, new EntityRelation(tenantId, ruleChainId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN));
            return true;
        } catch (Exception e) {
            log.warn("Failed to set auto assign to edge rule chain, ruleChainId: [{}]", ruleChainId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `unsetAutoAssignToEdgeRuleChain` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    @Override
    public boolean unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            deleteRelation(tenantId, new EntityRelation(tenantId, ruleChainId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN));
            return true;
        } catch (Exception e) {
            log.warn("Failed to unset auto assign to edge rule chain, ruleChainId: [{}]", ruleChainId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAutoAssignToEdgeRuleChainsByTenantId, tenantId [{}], pageLink {}", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return ruleChainDao.findAutoAssignToEdgeRuleChainsByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `search`：`search` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String search) {
        log.trace("Executing findRuleNodes, tenantId [{}], type {}, search {}", tenantId, type, search);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type of the rule node");
        validateString(search, "Incorrect search text");
        return ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId, type, search);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type) {
        log.trace("Executing findRuleNodes, tenantId [{}], type {}", tenantId, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type of the rule node");
        return ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId, type, "");
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink) {
        log.trace("Executing findAllRuleNodesByType, type {}, pageLink {}", type, pageLink);
        validateString(type, "Incorrect type of the rule node");
        validatePageLink(pageLink);
        return ruleNodeDao.findAllRuleNodesByType(type, pageLink);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `version`：`version` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNode> findAllRuleNodesByTypeAndVersionLessThan(String type, int version, PageLink pageLink) {
        log.trace("Executing findAllRuleNodesByTypeAndVersionLessThan, type {}, pageLink {}, version {}", type, pageLink, version);
        validateString(type, "Incorrect type of the rule node");
        validatePositiveNumber(version, "Incorrect version to compare with. Version should be greater than 0!");
        validatePageLink(pageLink);
        return ruleNodeDao.findAllRuleNodesByTypeAndVersionLessThan(type, version, pageLink);
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `type`：类型。
     * - `version`：`version` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNodeId> findAllRuleNodeIdsByTypeAndVersionLessThan(String type, int version, PageLink pageLink) {
        log.trace("Executing findAllRuleNodeIdsByTypeAndVersionLessThan, type {}, pageLink {}, version {}", type, pageLink, version);
        validateString(type, "Incorrect type of the rule node");
        validatePositiveNumber(version, "Incorrect version to compare with. Version should be greater than 0!");
        validatePageLink(pageLink);
        return ruleNodeDao.findAllRuleNodeIdsByTypeAndVersionLessThan(type, version, pageLink);
    }

    /**
     * 功能：获取`All Rule Nodes By Ids`。
     * 参数：
     * - `ruleNodeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> findAllRuleNodesByIds(List<RuleNodeId> ruleNodeIds) {
        log.trace("Executing findAllRuleNodesByIds, ruleNodeIds {}", ruleNodeIds);
        validateIds(ruleNodeIds, "Incorrect ruleNodeIds " + ruleNodeIds);
        assert ruleNodeIds.size() <= 1024;
        return ruleNodeDao.findAllRuleNodeByIds(ruleNodeIds);
    }

    /**
     * 功能：保存或创建规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleNode saveRuleNode(TenantId tenantId, RuleNode ruleNode) {
        return ruleNodeDao.save(tenantId, ruleNode);
    }

    /**
     * 功能：校验`Rule Nodes And Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    private void checkRuleNodesAndDelete(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            entityCountService.publishCountEntityEvictEvent(tenantId, EntityType.RULE_CHAIN);
            ruleChainDao.removeById(tenantId, ruleChainId.getId());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(ruleChainId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_rule_chain_device_profile")) {
                throw new DataValidationException("The rule chain referenced by the device profiles cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_rule_chain_asset_profile")) {
                throw new DataValidationException("The rule chain referenced by the asset profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
        deleteRuleNodes(tenantId, ruleChainId);
    }

    /**
     * 功能：删除或清理`Rule Nodes`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodes`：数据列表。
     * 返回：无。
     */
    private void deleteRuleNodes(TenantId tenantId, List<RuleNode> ruleNodes) {
        List<RuleNodeId> ruleNodeIds = ruleNodes.stream().map(RuleNode::getId).collect(Collectors.toList());
        for (var node : ruleNodes) {
            deleteEntityRelations(tenantId, node.getId());
        }
        ruleNodeDao.deleteByIdIn(ruleNodeIds);
    }

    /**
     * 功能：删除或清理`Rule Nodes`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteRuleNodes(TenantId tenantId, RuleChainId ruleChainId) {
        List<EntityRelation> nodeRelations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        for (EntityRelation relation : nodeRelations) {
            deleteRuleNode(tenantId, relation.getTo());
        }
        deleteEntityRelations(tenantId, ruleChainId);
    }


    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        HasId<?> hasId = EntityType.RULE_NODE.equals(entityId.getEntityType()) ?
                findRuleNodeById(tenantId, new RuleNodeId(entityId.getId())) :
                findRuleChainById(tenantId, new RuleChainId(entityId.getId()));
        return Optional.ofNullable(hasId);
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long countByTenantId(TenantId tenantId) {
        return ruleChainDao.countByTenantId(tenantId);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteRuleChainById(tenantId, (RuleChainId) id);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    private List<EntityRelation> getRuleChainToNodeRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByFrom(tenantId, ruleChainId, RelationTypeGroup.RULE_CHAIN);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    private List<EntityRelation> getNodeToRuleChainRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByTo(tenantId, ruleChainId, RelationTypeGroup.RULE_NODE);
    }

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    private void deleteRuleNode(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId);
        ruleNodeDao.removeById(tenantId, entityId.getId());
    }

    private final PaginatedRemover<TenantId, RuleChain> tenantRuleChainsRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<RuleChain> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, RuleChain entity) {
                    checkRuleNodesAndDelete(tenantId, entity.getId());
                }
            };

    /**
     * 功能：更新`Singleton Mode`。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：无。
     */
    private void setSingletonMode(RuleNode ruleNode) {
        boolean singletonMode;
        try {
            ComponentClusteringMode nodeConfigType = ReflectionUtils.getAnnotationProperty(ruleNode.getType(),
                    "org.thingsboard.rule.engine.api.RuleNode", "clusteringMode");

            switch (nodeConfigType) {
                case ENABLED:
                    singletonMode = false;
                    break;
                case SINGLETON:
                    singletonMode = true;
                    break;
                case USER_PREFERENCE:
                default:
                    singletonMode = ruleNode.isSingletonMode();
                    break;
            }
        } catch (Exception e) {
            log.warn("Failed to get clustering mode: {}", ExceptionUtils.getRootCauseMessage(e));
            singletonMode = false;
        }

        ruleNode.setSingletonMode(singletonMode);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseRuleChainService` 在 ThingsBoard DAO 模块 中承担规则链持久化服务类型职责，核心目的是维护规则链、规则节点、节点状态和规则引擎元数据的数据库访问。
 * 2. 核心流程：读写规则链元数据后触发缓存失效、审计记录和规则引擎侧的配置更新流程。
 * 3. 关键依赖：主要依赖或协作对象包括RuleChainService、RuleNodeStateService、Rule Engine、Actor、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
