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
package org.thingsboard.server.dao.service.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.ReflectionUtils;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `RuleChainDataValidator` 是 ThingsBoard DAO 中负责规则链存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@Slf4j
public class RuleChainDataValidator extends DataValidator<RuleChain> {

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private RuleChainService ruleChainService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantService tenantService;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `data`：待处理数据。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, RuleChain data) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.RULE_CHAIN);
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, RuleChain ruleChain) {
        validateString("Rule chain name", ruleChain.getName());
        if (ruleChain.getType() == null) {
            ruleChain.setType(RuleChainType.CORE);
        }
        if (ruleChain.getTenantId() == null || ruleChain.getTenantId().isNullUid()) {
            throw new DataValidationException("Rule chain should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(ruleChain.getTenantId())) {
            throw new DataValidationException("Rule chain is referencing to non-existent tenant!");
        }
        if (ruleChain.isRoot() && RuleChainType.CORE.equals(ruleChain.getType())) {
            RuleChain rootRuleChain = ruleChainService.getRootTenantRuleChain(ruleChain.getTenantId());
            if (rootRuleChain != null && !rootRuleChain.getId().equals(ruleChain.getId())) {
                throw new DataValidationException("Another root rule chain is present in scope of current tenant!");
            }
        }
        if (ruleChain.isRoot() && RuleChainType.EDGE.equals(ruleChain.getType())) {
            RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(ruleChain.getTenantId());
            if (edgeTemplateRootRuleChain != null && !edgeTemplateRootRuleChain.getId().equals(ruleChain.getId())) {
                throw new DataValidationException("Another edge template root rule chain is present in scope of current tenant!");
            }
        }
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `ruleChainMetaData`：待处理数据。
     * 返回：判断结果。
     */
    public static List<Throwable> validateMetaData(RuleChainMetaData ruleChainMetaData) {
        validateMetaDataFieldsAndConnections(ruleChainMetaData);
        return ruleChainMetaData.getNodes().stream()
                .map(RuleChainDataValidator::validateRuleNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `ruleChainMetaData`：待处理数据。
     * 返回：无。
     */
    public static void validateMetaDataFieldsAndConnections(RuleChainMetaData ruleChainMetaData) {
        ConstraintValidator.validateFields(ruleChainMetaData);
        if (CollectionUtils.isNotEmpty(ruleChainMetaData.getConnections())) {
            validateCircles(ruleChainMetaData.getConnections());
        }
    }

    /**
     * 功能：校验规则节点。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：判断结果。
     */
    public static Throwable validateRuleNode(RuleNode ruleNode) {
        String errorPrefix = "'" + ruleNode.getName() + "' node configuration is invalid: ";
        ConstraintValidator.validateFields(ruleNode, errorPrefix);
        Object nodeConfig;
        try {
            Class<Object> nodeConfigType = ReflectionUtils.getAnnotationProperty(ruleNode.getType(),
                    "org.thingsboard.rule.engine.api.RuleNode", "configClazz");
            nodeConfig = JacksonUtil.treeToValue(ruleNode.getConfiguration(), nodeConfigType);
        } catch (Throwable t) {
            log.warn("Failed to validate node configuration: {}", ExceptionUtils.getRootCauseMessage(t));
            return t;
        }
        ConstraintValidator.validateFields(nodeConfig, errorPrefix);
        return null;
    }

    /**
     * 功能：校验`Circles`。
     * 参数：
     * - `connectionInfos`：数据列表。
     * 返回：无。
     */
    private static void validateCircles(List<NodeConnectionInfo> connectionInfos) {
        Map<Integer, Set<Integer>> connectionsMap = new HashMap<>();
        for (NodeConnectionInfo nodeConnection : connectionInfos) {
            if (nodeConnection.getFromIndex() == nodeConnection.getToIndex()) {
                throw new DataValidationException("Can't create the relation to yourself.");
            }
            connectionsMap
                    .computeIfAbsent(nodeConnection.getFromIndex(), from -> new HashSet<>())
                    .add(nodeConnection.getToIndex());
        }
        connectionsMap.keySet().forEach(key -> validateCircles(key, connectionsMap.get(key), connectionsMap));
    }

    /**
     * 功能：校验`Circles`。
     * 参数：
     * - `from`：`from` 参数。
     * - `toList`：`toList` 参数。
     * - `connectionsMap`：键值映射。
     * 返回：无。
     */
    private static void validateCircles(int from, Set<Integer> toList, Map<Integer, Set<Integer>> connectionsMap) {
        if (toList == null) {
            return;
        }
        for (Integer to : toList) {
            if (from == to) {
                throw new DataValidationException("Can't create circling relations in rule chain.");
            }
            validateCircles(from, connectionsMap.get(to), connectionsMap);
        }
    }

}
