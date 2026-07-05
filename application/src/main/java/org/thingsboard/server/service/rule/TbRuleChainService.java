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

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainOutputLabelsUsage;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;

import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`TbRuleChainService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public interface TbRuleChainService extends SimpleTbEntityService<RuleChain> {

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    Set<String> getRuleChainOutputLabels(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取显示标签。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    List<RuleChainOutputLabelsUsage> getOutputLabelUsage(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：更新`Related Rule Chains`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    List<RuleChain> updateRelatedRuleChains(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateResult result);

    /**
     * 功能：保存或创建名称。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `request`：请求对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain saveDefaultByName(TenantId tenantId, DefaultRuleChainCreateRequest request, User user) throws Exception;

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain setRootRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException;

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
    RuleChainMetaData saveRuleChainMetaData(TenantId tenantId, RuleChain ruleChain, RuleChainMetaData ruleChainMetaData,
                                            boolean updateRelated, User user) throws Exception;

    /**
     * 功能：执行 `assignRuleChainToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChain ruleChain, Edge edge, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignRuleChainFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChain ruleChain, Edge edge, User user) throws ThingsboardException;

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException;

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unsetAutoAssignToEdgeRuleChain` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    RuleChain unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, User user) throws ThingsboardException;

    /**
     * 功能：更新规则节点。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：处理结果。
     */
    RuleNode updateRuleNodeConfiguration(RuleNode ruleNode);
}

/*
 * 本类总结：
 * 1. 核心职责：`TbRuleChainService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
