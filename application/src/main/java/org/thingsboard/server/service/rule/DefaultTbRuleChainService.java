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

@RequiredArgsConstructor
@Service
@TbCoreComponent
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultTbRuleChainService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultTbRuleChainService extends AbstractTbEntityService implements TbRuleChainService {

    /**
     * 字段说明：
     * 1. 保存 `ruleChainService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final RuleChainService ruleChainService;
    private final RelationService relationService;
    /**
     * 字段说明：
     * 1. 保存 `installScripts` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final InstallScripts installScripts;
    private final ComponentDiscoveryService componentDiscoveryService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getRuleChainOutputLabels` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Set<String> getRuleChainOutputLabels(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId, ruleChainId);
        Set<String> outputLabels = new TreeSet<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (RuleNode ruleNode : metaData.getNodes()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (isOutputRuleNode(ruleNode)) {
                outputLabels.add(ruleNode.getName());
            }
        }
        return outputLabels;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getOutputLabelUsage` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<RuleChainOutputLabelsUsage> getOutputLabelUsage(TenantId tenantId, RuleChainId ruleChainId) {
        List<RuleNode> ruleNodes = ruleChainService.findRuleNodesByTenantIdAndType(tenantId, TbRuleChainInputNode.class.getName(), ruleChainId.getId().toString());
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        Map<RuleChainId, String> ruleChainNamesCache = new HashMap<>();
        // Additional filter, "just in case" the structure of the JSON configuration will change.
        var filteredRuleNodes = ruleNodes.stream().filter(node -> {
            try {
                TbRuleChainInputNodeConfiguration configuration = JacksonUtil.treeToValue(node.getConfiguration(), TbRuleChainInputNodeConfiguration.class);
                return ruleChainId.getId().toString().equals(configuration.getRuleChainId());
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
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
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (relations != null && !relations.isEmpty()) {
                        usage.setLabels(relations.stream().map(EntityRelation::getType).collect(Collectors.toSet()));
                    }
                    return usage;
                })
                .filter(usage -> usage.getLabels() != null)
                .peek(usage -> {
                    // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
                    String ruleChainName = ruleChainNamesCache.computeIfAbsent(usage.getRuleChainId(),
                            id -> ruleChainService.findRuleChainById(tenantId, id).getName());
                    usage.setRuleChainName(ruleChainName);
                })
                .sorted(Comparator
                        .comparing(RuleChainOutputLabelsUsage::getRuleChainName)
                        .thenComparing(RuleChainOutputLabelsUsage::getRuleNodeName))
                .collect(Collectors.toList());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateRelatedRuleChains` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<RuleChain> updateRelatedRuleChains(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateResult result) {
        Set<RuleChainId> ruleChainIds = new HashSet<>();
        log.debug("[{}][{}] Going to update links in related rule chains", tenantId, ruleChainId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (result.getUpdatedRuleNodes() == null || result.getUpdatedRuleNodes().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> oldLabels = new HashSet<>();
        Set<String> newLabels = new HashSet<>();
        Set<String> confusedLabels = new HashSet<>();
        Map<String, String> updatedLabels = new HashMap<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (RuleNodeUpdateResult update : result.getUpdatedRuleNodes()) {
            var oldNode = update.getOldRuleNode();
            var newNode = update.getNewRuleNode();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (isOutputRuleNode(newNode)) {
                try {
                    oldLabels.add(oldNode.getName());
                    newLabels.add(newNode.getName());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!oldNode.getName().equals(newNode.getName())) {
                        String oldLabel = oldNode.getName();
                        String newLabel = newNode.getName();
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (updatedLabels.containsKey(oldLabel) && !updatedLabels.get(oldLabel).equals(newLabel)) {
                            confusedLabels.add(oldLabel);
                            log.warn("[{}][{}] Can't automatically rename the label from [{}] to [{}] due to conflict [{}]", tenantId, ruleChainId, oldLabel, newLabel, updatedLabels.get(oldLabel));
                        } else {
                            updatedLabels.put(oldLabel, newLabel);
                        }

                    }
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `delete` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveDefaultByName` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setRootRuleChain` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveRuleChainMetaData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `assignRuleChainToEdge` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignRuleChainFromEdge` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setEdgeTemplateRootRuleChain` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setAutoAssignToEdgeRuleChain` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `unsetAutoAssignToEdgeRuleChain` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateRuleNodeConfiguration` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
     * 方法说明：
     * 1. 职责：执行 `updateRelatedRuleChains` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `renameOutgoingLinks` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `isOutputRuleNode` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isOutputRuleNode(RuleNode ruleNode) {
        return isRuleNode(ruleNode, TbRuleChainOutputNode.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isInputRuleNode` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isInputRuleNode(RuleNode ruleNode) {
        return isRuleNode(ruleNode, TbRuleChainInputNode.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isRuleNode` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isRuleNode(RuleNode ruleNode, Class<?> clazz) {
        return ruleNode != null && ruleNode.getType().equals(clazz.getName());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbRuleChainService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
