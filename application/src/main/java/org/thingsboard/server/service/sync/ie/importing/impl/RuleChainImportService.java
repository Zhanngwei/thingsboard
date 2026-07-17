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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.rule.RuleNodeDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `RuleChainImportService` 是 ThingsBoard Application 中负责规则链的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends BaseEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.RULE_CHAIN, EntityType.DEVICE, EntityType.ASSET));
    public static final Pattern PROCESSED_CONFIG_FIELDS_PATTERN = Pattern.compile(".*[iI]d.*");

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    private final TbRuleChainService tbRuleChainService;
    private final RuleChainService ruleChainService;
    /**
     * 规则节点，用于读取或保存对应领域对象。
     */
    private final RuleNodeDao ruleNodeDao;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChain`：`ruleChain` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, RuleChain ruleChain, IdProvider idProvider) {
        ruleChain.setTenantId(tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ruleChain`：`ruleChain` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected RuleChain findExistingEntity(EntitiesImportCtx ctx, RuleChain ruleChain, IdProvider idProvider) {
        RuleChain existingRuleChain = super.findExistingEntity(ctx, ruleChain, idProvider);
        if (existingRuleChain == null && ctx.isFindExistingByName()) {
            existingRuleChain = ruleChainService.findTenantRuleChainsByTypeAndName(ctx.getTenantId(), ruleChain.getType(), ruleChain.getName()).stream().findFirst().orElse(null);
        }
        return existingRuleChain;
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ruleChain`：`ruleChain` 参数。
     * - `old`：`old` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected RuleChain prepare(EntitiesImportCtx ctx, RuleChain ruleChain, RuleChain old, RuleChainExportData exportData, IdProvider idProvider) {
        RuleChainMetaData metaData = exportData.getMetaData();
        List<RuleNode> ruleNodes = Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList());
        if (old != null) {
            List<RuleNodeId> nodeIds = ruleNodes.stream().map(RuleNode::getId).collect(Collectors.toList());
            List<RuleNode> existing = ruleNodeDao.findByExternalIds(old.getId(), nodeIds);
            existing.forEach(node -> ctx.putInternalId(node.getExternalId(), node.getId()));
            ruleNodes.forEach(node -> {
                node.setRuleChainId(old.getId());
                node.setExternalId(node.getId());
                node.setId((RuleNodeId) ctx.getInternalId(node.getId()));
            });
        } else {
            ruleNodes.forEach(node -> {
                node.setRuleChainId(null);
                node.setExternalId(node.getId());
                node.setId(null);
            });
        }

        ruleNodes.forEach(ruleNode -> replaceIdsRecursively(ctx, idProvider, ruleNode.getConfiguration(),
                Collections.emptySet(), PROCESSED_CONFIG_FIELDS_PATTERN, HINTS));
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(idProvider.getInternalId(ruleChainConnectionInfo.getTargetRuleChainId(), false));
                });
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChain.setFirstRuleNodeId((RuleNodeId) ctx.getInternalId(ruleChain.getFirstRuleNodeId()));
        }
        return ruleChain;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ruleChain`：`ruleChain` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected RuleChain saveOrUpdate(EntitiesImportCtx ctx, RuleChain ruleChain, RuleChainExportData exportData, IdProvider idProvider) {
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        if (ctx.isFinalImportAttempt() || ctx.getCurrentImportResult().isUpdatedAllExternalIds()) {
            exportData.getMetaData().setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), exportData.getMetaData(), tbRuleChainService::updateRuleNodeConfiguration);
            return ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChain.getId());
        } else {
            return ruleChain;
        }
    }

    /**
     * 功能：执行 `compare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `exportData`：待处理数据。
     * - `prepared`：`prepared` 参数。
     * - `existing`：`existing` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean compare(EntitiesImportCtx ctx, RuleChainExportData exportData, RuleChain prepared, RuleChain existing) {
        boolean different = super.compare(ctx, exportData, prepared, existing);
        if (!different) {
            RuleChainMetaData newMD = exportData.getMetaData();
            RuleChainMetaData existingMD = ruleChainService.loadRuleChainMetaData(ctx.getTenantId(), prepared.getId());
            existingMD.setRuleChainId(null);
            different = !newMD.equals(existingMD);
        }
        return different;
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedRuleChain`：`savedRuleChain` 参数。
     * - `oldRuleChain`：`oldRuleChain` 参数。
     * 返回：无。
     */
    @Override
    protected void onEntitySaved(User user, RuleChain savedRuleChain, RuleChain oldRuleChain) {
        entityActionService.logEntityAction(user, savedRuleChain.getId(), savedRuleChain, null,
                oldRuleChain == null ? ActionType.ADDED : ActionType.UPDATED, null);
        if (savedRuleChain.getType() == RuleChainType.CORE) {
            clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedRuleChain.getId(),
                    oldRuleChain == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        }
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    @Override
    protected RuleChain deepCopy(RuleChain ruleChain) {
        return new RuleChain(ruleChain);
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

}
