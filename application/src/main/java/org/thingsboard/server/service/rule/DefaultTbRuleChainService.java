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
package org.thingsboard.server.service.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNode;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNodeConfiguration;
import org.thingsboard.rule.engine.flow.TbRuleChainOutputNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainOutputLabelsUsage;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.rule.RuleNodeUpdateResult;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.utils.TbNodeUpgradeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultTbRuleChainService` 是 ThingsBoard Application 中负责规则链的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbRuleChainService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@RequiredArgsConstructor
@Service
@TbCoreComponent
@Slf4j
public class DefaultTbRuleChainService extends AbstractTbEntityService implements TbRuleChainService {

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    private final RuleChainService ruleChainService;
    private final RelationService relationService;
    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    private final InstallScripts installScripts;
    private final ComponentDiscoveryService componentDiscoveryService;

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<String> getRuleChainOutputLabels(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId, ruleChainId);
        Set<String> outputLabels = new TreeSet<>();
        for (RuleNode ruleNode : metaData.getNodes()) {
            if (isOutputRuleNode(ruleNode)) {
                outputLabels.add(ruleNode.getName());
            }
        }
        return outputLabels;
    }

    /**
     * 功能：获取显示标签。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleChainOutputLabelsUsage> getOutputLabelUsage(TenantId tenantId, RuleChainId ruleChainId) {
        List<RuleNode> ruleNodes = ruleChainService.findRuleNodesByTenantIdAndType(tenantId, TbRuleChainInputNode.class.getName(), ruleChainId.getId().toString());
        Map<RuleChainId, String> ruleChainNamesCache = new HashMap<>();
        // Additional filter, "just in case" the structure of the JSON configuration will change.
        var filteredRuleNodes = ruleNodes.stream().filter(node -> {
            try {
                TbRuleChainInputNodeConfiguration configuration = JacksonUtil.treeToValue(node.getConfiguration(), TbRuleChainInputNodeConfiguration.class);
                return ruleChainId.getId().toString().equals(configuration.getRuleChainId());
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to decode rule node configuration", tenantId, ruleChainId, e);
                return false;
            }
        }).collect(Collectors.toList());


        return filteredRuleNodes.stream()
                .map(ruleNode -> {
                    RuleChainOutputLabelsUsage usage = new RuleChainOutputLabelsUsage();
                    usage.setRuleNodeId(ruleNode.getId());
                    usage.setRuleNodeName(ruleNode.getName());
                    usage.setRuleChainId(ruleNode.getRuleChainId());
                    List<EntityRelation> relations = ruleChainService.getRuleNodeRelations(tenantId, ruleNode.getId());
                    if (relations != null && !relations.isEmpty()) {
                        usage.setLabels(relations.stream().map(EntityRelation::getType).collect(Collectors.toSet()));
                    }
                    return usage;
                })
                .filter(usage -> usage.getLabels() != null)
                .peek(usage -> {
                    String ruleChainName = ruleChainNamesCache.computeIfAbsent(usage.getRuleChainId(),
                            id -> ruleChainService.findRuleChainById(tenantId, id).getName());
                    usage.setRuleChainName(ruleChainName);
                })
                .sorted(Comparator
                        .comparing(RuleChainOutputLabelsUsage::getRuleChainName)
                        .thenComparing(RuleChainOutputLabelsUsage::getRuleNodeName))
                .collect(Collectors.toList());
    }

    /**
     * 功能：更新`Related Rule Chains`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleChain> updateRelatedRuleChains(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateResult result) {
        Set<RuleChainId> ruleChainIds = new HashSet<>();
        log.debug("[{}][{}] Going to update links in related rule chains", tenantId, ruleChainId);
        if (result.getUpdatedRuleNodes() == null || result.getUpdatedRuleNodes().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> oldLabels = new HashSet<>();
        Set<String> newLabels = new HashSet<>();
        Set<String> confusedLabels = new HashSet<>();
        Map<String, String> updatedLabels = new HashMap<>();
        for (RuleNodeUpdateResult update : result.getUpdatedRuleNodes()) {
            var oldNode = update.getOldRuleNode();
            var newNode = update.getNewRuleNode();
            if (isOutputRuleNode(newNode)) {
                try {
                    oldLabels.add(oldNode.getName());
                    newLabels.add(newNode.getName());
                    if (!oldNode.getName().equals(newNode.getName())) {
                        String oldLabel = oldNode.getName();
                        String newLabel = newNode.getName();
                        if (updatedLabels.containsKey(oldLabel) && !updatedLabels.get(oldLabel).equals(newLabel)) {
                            confusedLabels.add(oldLabel);
                            log.warn("[{}][{}] Can't automatically rename the label from [{}] to [{}] due to conflict [{}]", tenantId, ruleChainId, oldLabel, newLabel, updatedLabels.get(oldLabel));
                        } else {
                            updatedLabels.put(oldLabel, newLabel);
                        }

                    }
                } catch (Exception e) {
                    log.warn("[{}][{}][{}] Failed to decode rule node configuration", tenantId, ruleChainId, newNode.getId(), e);
                }
            }
        }
        // Remove all output labels that are renamed to two or more different labels, since we don't which new label to use;
        confusedLabels.forEach(updatedLabels::remove);
        // Remove all output labels that are renamed but still present in the rule chain;
        newLabels.forEach(updatedLabels::remove);
        if (!oldLabels.equals(newLabels)) {
            ruleChainIds.addAll(updateRelatedRuleChains(tenantId, ruleChainId, updatedLabels));
        }
        return ruleChainIds.stream().map(id -> ruleChainService.findRuleChainById(tenantId, id)).collect(Collectors.toList());
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain save(RuleChain ruleChain, User user) throws Exception {
        TenantId tenantId = ruleChain.getTenantId();
        ActionType actionType = ruleChain.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            RuleChain savedRuleChain = checkNotNull(ruleChainService.saveRuleChain(ruleChain));
            autoCommit(user, savedRuleChain.getId());

            if (RuleChainType.CORE.equals(savedRuleChain.getType())) {
                tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedRuleChain.getId(),
                        actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            }
            notificationEntityService.logEntityAction(tenantId, savedRuleChain.getId(), savedRuleChain, null, actionType, user);
            return savedRuleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ruleChain, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(RuleChain ruleChain, User user) {
        TenantId tenantId = ruleChain.getTenantId();
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            List<RuleNode> referencingRuleNodes = ruleChainService.getReferencingRuleChainNodes(tenantId, ruleChainId);

            Set<RuleChainId> referencingRuleChainIds = referencingRuleNodes.stream().map(RuleNode::getRuleChainId).collect(Collectors.toSet());

            ruleChainService.deleteRuleChainById(tenantId, ruleChainId);

            referencingRuleChainIds.remove(ruleChain.getId());

            if (RuleChainType.CORE.equals(ruleChain.getType())) {
                referencingRuleChainIds.forEach(referencingRuleChainId ->
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, referencingRuleChainId, ComponentLifecycleEvent.UPDATED));

                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChain.getId(), ComponentLifecycleEvent.DELETED);
            }

            notificationEntityService.logEntityAction(tenantId, ruleChainId, ruleChain, null, ActionType.DELETED, user, ruleChainId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ActionType.DELETED,
                    user, e, ruleChainId.toString());
            throw e;
        }
    }

    /**
     * 功能：保存或创建名称。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `request`：请求对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain saveDefaultByName(TenantId tenantId, DefaultRuleChainCreateRequest request, User user) throws Exception {
        try {
            RuleChain savedRuleChain = installScripts.createDefaultRuleChain(tenantId, request.getName());
            autoCommit(user, savedRuleChain.getId());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedRuleChain.getId(), ComponentLifecycleEvent.CREATED);
            notificationEntityService.logEntityAction(tenantId, savedRuleChain.getId(), savedRuleChain, ActionType.ADDED, user);
            return savedRuleChain;
        } catch (Exception e) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName(request.getName());
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ruleChain,
                    ActionType.ADDED, user, e);
            throw e;
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain setRootRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            RuleChain previousRootRuleChain = ruleChainService.getRootTenantRuleChain(tenantId);
            if (ruleChainService.setRootRuleChain(tenantId, ruleChainId)) {
                if (previousRootRuleChain != null) {
                    RuleChainId previousRootRuleChainId = previousRootRuleChain.getId();
                    previousRootRuleChain = ruleChainService.findRuleChainById(tenantId, previousRootRuleChainId);

                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, previousRootRuleChainId,
                            ComponentLifecycleEvent.UPDATED);
                    notificationEntityService.logEntityAction(tenantId, previousRootRuleChainId, previousRootRuleChain,
                            ActionType.UPDATED, user);
                }
                ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);

                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainId,
                        ComponentLifecycleEvent.UPDATED);
                notificationEntityService.logEntityAction(tenantId, ruleChainId, ruleChain, ActionType.UPDATED, user);
            }
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ActionType.UPDATED,
                    user, e, ruleChainId.toString());
            throw e;
        }
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `ruleChainMetaData`：待处理数据。
     * - `updateRelated`：`updateRelated` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public RuleChainMetaData saveRuleChainMetaData(TenantId tenantId, RuleChain ruleChain, RuleChainMetaData ruleChainMetaData,
                                                   boolean updateRelated, User user) throws Exception {
        RuleChainId ruleChainId = ruleChain.getId();
        RuleChainId ruleChainMetaDataId = ruleChainMetaData.getRuleChainId();
        try {
            RuleChainUpdateResult result = ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData, this::updateRuleNodeConfiguration);
            checkNotNull(result.isSuccess() ? true : null);

            List<RuleChain> updatedRuleChains;
            if (updateRelated && result.isSuccess()) {
                updatedRuleChains = updateRelatedRuleChains(tenantId, ruleChainMetaDataId, result);
            } else {
                updatedRuleChains = Collections.emptyList();
            }

            if (updatedRuleChains.isEmpty()) {
                autoCommit(user, ruleChainMetaData.getRuleChainId());
            } else {
                List<UUID> uuids = new ArrayList<>(updatedRuleChains.size() + 1);
                uuids.add(ruleChainMetaData.getRuleChainId().getId());
                updatedRuleChains.forEach(rc -> uuids.add(rc.getId().getId()));
                autoCommit(user, EntityType.RULE_CHAIN, uuids);
            }

            RuleChainMetaData savedRuleChainMetaData = checkNotNull(ruleChainService.loadRuleChainMetaData(tenantId, ruleChainMetaDataId));

            if (RuleChainType.CORE.equals(ruleChain.getType())) {
                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainId, ComponentLifecycleEvent.UPDATED);
                updatedRuleChains.forEach(updatedRuleChain -> {
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, updatedRuleChain.getId(), ComponentLifecycleEvent.UPDATED);
                });
            }

            notificationEntityService.logEntityAction(tenantId, ruleChainId, ruleChain, ActionType.UPDATED, user, ruleChainMetaData);

            for (RuleChain updatedRuleChain : updatedRuleChains) {
                if (RuleChainType.CORE.equals(ruleChain.getType())) {
                    RuleChainMetaData updatedRuleChainMetaData = checkNotNull(ruleChainService.loadRuleChainMetaData(tenantId, updatedRuleChain.getId()));
                    notificationEntityService.logEntityAction(tenantId, updatedRuleChain.getId(), updatedRuleChain,
                            ActionType.UPDATED, user, updatedRuleChainMetaData);
                }
            }
            return savedRuleChainMetaData;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ActionType.ADDED,
                    user, e, ruleChainMetaData);
            throw e;
        }
    }

    /**
     * 功能：执行 `assignRuleChainToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChain ruleChain, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        RuleChainId ruleChainId = ruleChain.getId();
        EdgeId edgeId = edge.getId();
        try {
            RuleChain savedRuleChain = checkNotNull(ruleChainService.assignRuleChainToEdge(tenantId, ruleChainId, edgeId));
            notificationEntityService.logEntityAction(tenantId, ruleChainId, savedRuleChain, null, actionType,
                    user, ruleChainId.toString(), edgeId.toString(), edge.getName());
            return savedRuleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN),
                    actionType, user, e, ruleChainId.toString(), edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignRuleChainFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChain ruleChain, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        RuleChainId ruleChainId = ruleChain.getId();
        EdgeId edgeId = edge.getId();
        try {
            RuleChain savedRuleChain = checkNotNull(ruleChainService.unassignRuleChainFromEdge(tenantId, ruleChainId, edgeId, false));
            notificationEntityService.logEntityAction(tenantId, ruleChainId, savedRuleChain, null, actionType,
                    user, ruleChainId.toString(), edgeId.toString(), edge.getName());
            return savedRuleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN),
                    actionType, user, e, ruleChainId, edgeId);
            throw e;
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            ruleChainService.setEdgeTemplateRootRuleChain(tenantId, ruleChainId);
            notificationEntityService.logEntityAction(tenantId, ruleChainId, ruleChain, ActionType.UPDATED, user);
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ActionType.UPDATED,
                    user, e, ruleChainId.toString());
            throw e;
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            ruleChainService.setAutoAssignToEdgeRuleChain(tenantId, ruleChainId);
            notificationEntityService.logEntityAction(tenantId, ruleChainId, ruleChain, ActionType.UPDATED, user);
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ActionType.UPDATED,
                    user, e, ruleChainId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unsetAutoAssignToEdgeRuleChain` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleChain unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            ruleChainService.unsetAutoAssignToEdgeRuleChain(tenantId, ruleChainId);
            notificationEntityService.logEntityAction(tenantId, ruleChainId, ruleChain, ActionType.UPDATED, user);
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.RULE_CHAIN), ActionType.UPDATED,
                    user, e, ruleChainId.toString());
            throw e;
        }
    }

    /**
     * 功能：更新规则节点。
     * 参数：
     * - `node`：`node` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleNode updateRuleNodeConfiguration(RuleNode node) {
        var ruleChainId = node.getRuleChainId();
        var ruleNodeId = node.getId();
        var ruleNodeType = node.getType();
        try {
            var ruleNodeClass = componentDiscoveryService.getRuleNodeInfo(ruleNodeType)
                    .orElseThrow(() -> new RuntimeException("Rule node " + ruleNodeType + " is not supported!"));
            if (ruleNodeClass.isVersioned()) {
                int fromVersion = node.getConfigurationVersion();
                int toVersion = ruleNodeClass.getCurrentVersion();
                if (fromVersion < toVersion) {
                    log.debug("Going to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                            ruleNodeId, ruleNodeType, fromVersion, toVersion);
                    TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, ruleNodeClass);
                    log.debug("Successfully upgrade rule node with id: {} type: {}, rule chain id: {} fromVersion: {} toVersion: {}",
                            ruleNodeId, ruleNodeType, ruleChainId, fromVersion, toVersion);
                } else {
                    log.debug("Rule node with id: {} type: {} ruleChainId: {} already set to latest version!",
                            ruleNodeId, ruleChainId, ruleNodeType);
                }
            }
        } catch (Exception e) {
            log.error("Failed to upgrade rule node with id: {} type: {}, rule chain id: {}",
                    ruleNodeId, ruleNodeType, ruleChainId, e);
        }
        return node;
    }

    /**
     * 功能：更新`Related Rule Chains`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `labelsMap`：键值映射。
     * 返回：匹配的数据集合。
     */
    private Set<RuleChainId> updateRelatedRuleChains(TenantId tenantId, RuleChainId ruleChainId, Map<String, String> labelsMap) {
        Set<RuleChainId> updatedRuleChains = new HashSet<>();
        List<RuleChainOutputLabelsUsage> usageList = getOutputLabelUsage(tenantId, ruleChainId);
        for (RuleChainOutputLabelsUsage usage : usageList) {
            labelsMap.forEach((oldLabel, newLabel) -> {
                if (usage.getLabels().contains(oldLabel)) {
                    updatedRuleChains.add(usage.getRuleChainId());
                    renameOutgoingLinks(tenantId, usage.getRuleNodeId(), oldLabel, newLabel);
                }
            });
        }
        return updatedRuleChains;
    }

    /**
     * 功能：执行 `renameOutgoingLinks` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * - `oldLabel`：`oldLabel` 参数。
     * - `newLabel`：`newLabel` 参数。
     * 返回：无。
     */
    private void renameOutgoingLinks(TenantId tenantId, RuleNodeId ruleNodeId, String oldLabel, String newLabel) {
        List<EntityRelation> relations = ruleChainService.getRuleNodeRelations(tenantId, ruleNodeId);
        for (EntityRelation relation : relations) {
            if (relation.getType().equals(oldLabel)) {
                relationService.deleteRelation(tenantId, relation);
                relation.setType(newLabel);
                relationService.saveRelation(tenantId, relation);
            }
        }
    }

    /**
     * 功能：判断规则节点。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：判断结果。
     */
    private boolean isOutputRuleNode(RuleNode ruleNode) {
        return isRuleNode(ruleNode, TbRuleChainOutputNode.class);
    }

    /**
     * 功能：判断规则节点。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：判断结果。
     */
    private boolean isInputRuleNode(RuleNode ruleNode) {
        return isRuleNode(ruleNode, TbRuleChainInputNode.class);
    }

    /**
     * 功能：判断规则节点。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：判断结果。
     */
    private boolean isRuleNode(RuleNode ruleNode, Class<?> clazz) {
        return ruleNode != null && ruleNode.getType().equals(clazz.getName());
    }

}
