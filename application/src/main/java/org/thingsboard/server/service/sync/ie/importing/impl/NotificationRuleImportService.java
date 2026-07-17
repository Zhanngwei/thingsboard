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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeCommunicationFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `NotificationRuleImportService` 是 ThingsBoard Application 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationRuleImportService extends BaseEntityImportService<NotificationRuleId, NotificationRule, EntityExportData<NotificationRule>> {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationRuleService notificationRuleService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationRule`：`notificationRule` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, NotificationRule notificationRule, IdProvider idProvider) {
        notificationRule.setTenantId(tenantId);
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationRule`：`notificationRule` 参数。
     * - `oldNotificationRule`：`oldNotificationRule` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected NotificationRule prepare(EntitiesImportCtx ctx, NotificationRule notificationRule, NotificationRule oldNotificationRule, EntityExportData<NotificationRule> exportData, IdProvider idProvider) {
        notificationRule.setTemplateId(idProvider.getInternalId(notificationRule.getTemplateId()));

        NotificationRuleTriggerConfig ruleTriggerConfig = notificationRule.getTriggerConfig();
        NotificationRuleTriggerType triggerType = ruleTriggerConfig.getTriggerType();
        switch (triggerType) {
            case DEVICE_ACTIVITY: {
                DeviceActivityNotificationRuleTriggerConfig triggerConfig = (DeviceActivityNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> devices = triggerConfig.getDevices();
                if (devices != null) {
                    triggerConfig.setDevices(devices.stream().map(DeviceId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }

                Set<UUID> deviceProfiles = triggerConfig.getDeviceProfiles();
                if (deviceProfiles != null) {
                    triggerConfig.setDeviceProfiles(deviceProfiles.stream().map(DeviceProfileId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }
                break;
            }
            case RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT: {
                RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig = (RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> ruleChains = triggerConfig.getRuleChains();
                if (ruleChains != null) {
                    triggerConfig.setRuleChains(ruleChains.stream().map(RuleChainId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }
                break;
            }
            case EDGE_CONNECTION: {
                EdgeConnectionNotificationRuleTriggerConfig triggerConfig = (EdgeConnectionNotificationRuleTriggerConfig) ruleTriggerConfig;
                triggerConfig.setEdges(null);
                break;
            }
            case EDGE_COMMUNICATION_FAILURE: {
                EdgeCommunicationFailureNotificationRuleTriggerConfig triggerConfig = (EdgeCommunicationFailureNotificationRuleTriggerConfig) ruleTriggerConfig;
                triggerConfig.setEdges(null);
                break;
            }
        }
        if (!triggerType.isTenantLevel()) {
            throw new IllegalArgumentException("Trigger type " + triggerType + " is not available for tenants");
        }

        NotificationRuleRecipientsConfig ruleRecipientsConfig = notificationRule.getRecipientsConfig();
        switch (triggerType) {
            case ALARM: {
                EscalatedNotificationRuleRecipientsConfig recipientsConfig = (EscalatedNotificationRuleRecipientsConfig) ruleRecipientsConfig;
                Map<Integer, List<UUID>> escalationTable = new LinkedHashMap<>(recipientsConfig.getEscalationTable());
                escalationTable.replaceAll((delay, targets) -> targets.stream()
                        .map(NotificationTargetId::new).map(idProvider::getInternalId)
                        .map(UUIDBased::getId).collect(Collectors.toList()));
                recipientsConfig.setEscalationTable(escalationTable);
                break;
            }
            default: {
                DefaultNotificationRuleRecipientsConfig recipientsConfig = (DefaultNotificationRuleRecipientsConfig) ruleRecipientsConfig;
                List<UUID> targets = recipientsConfig.getTargets().stream()
                        .map(NotificationTargetId::new).map(idProvider::getInternalId)
                        .map(UUIDBased::getId).collect(Collectors.toList());
                recipientsConfig.setTargets(targets);
                break;
            }
        }
        return notificationRule;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationRule`：`notificationRule` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected NotificationRule saveOrUpdate(EntitiesImportCtx ctx, NotificationRule notificationRule, EntityExportData<NotificationRule> exportData, IdProvider idProvider) {
        ConstraintValidator.validateFields(notificationRule);
        return notificationRuleService.saveNotificationRule(ctx.getTenantId(), notificationRule);
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedEntity`：实体对象。
     * - `oldEntity`：实体对象。
     * 返回：无。
     */
    @Override
    protected void onEntitySaved(User user, NotificationRule savedEntity, NotificationRule oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedEntity.getId(),
                oldEntity == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `notificationRule`：`notificationRule` 参数。
     * 返回：处理结果。
     */
    @Override
    protected NotificationRule deepCopy(NotificationRule notificationRule) {
        return new NotificationRule(notificationRule);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_RULE;
    }

}
