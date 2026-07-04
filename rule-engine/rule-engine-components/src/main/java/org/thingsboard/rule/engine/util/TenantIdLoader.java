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
package org.thingsboard.rule.engine.util;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.UUID;

/**
 * 按任意实体 ID 同步解析所属租户 ID 的工具类。
 * 本类不保存共享状态；不同实体类型会调用对应服务或缓存，数据库读取和缓存命中由这些服务实现决定。
 */
public class TenantIdLoader {

    /**
     * 查找实体所属租户 ID。
     * 本方法只返回租户归属信息，不直接发送 Rule Engine 消息；同步服务调用可能读取数据库或命中缓存，ASSET_PROFILE、DEVICE_PROFILE 分支会直接访问上下文缓存。
     *
     * @param ctx 规则节点上下文，提供租户、服务和缓存访问入口
     * @param entityId 待解析租户归属的实体 ID
     * @return 实体所属租户 ID，实体不存在时可能返回 null
     */
    public static TenantId findTenantId(TbContext ctx, EntityId entityId) {
        UUID id = entityId.getId();
        EntityType entityType = entityId.getEntityType();
        TenantId ctxTenantId = ctx.getTenantId();

        HasTenantId tenantEntity;
        switch (entityType) {
            case TENANT:
                // 租户实体自身的 ID 即为租户 ID，不需要数据库或缓存读取。
                return new TenantId(id);
            case CUSTOMER:
                tenantEntity = ctx.getCustomerService().findCustomerById(ctxTenantId, new CustomerId(id));
                break;
            case USER:
                tenantEntity = ctx.getUserService().findUserById(ctxTenantId, new UserId(id));
                break;
            case ASSET:
                tenantEntity = ctx.getAssetService().findAssetById(ctxTenantId, new AssetId(id));
                break;
            case DEVICE:
                tenantEntity = ctx.getDeviceService().findDeviceById(ctxTenantId, new DeviceId(id));
                break;
            case ALARM:
                tenantEntity = ctx.getAlarmService().findAlarmById(ctxTenantId, new AlarmId(id));
                break;
            case RULE_CHAIN:
                tenantEntity = ctx.getRuleChainService().findRuleChainById(ctxTenantId, new RuleChainId(id));
                break;
            case ENTITY_VIEW:
                tenantEntity = ctx.getEntityViewService().findEntityViewById(ctxTenantId, new EntityViewId(id));
                break;
            case DASHBOARD:
                tenantEntity = ctx.getDashboardService().findDashboardById(ctxTenantId, new DashboardId(id));
                break;
            case EDGE:
                tenantEntity = ctx.getEdgeService().findEdgeById(ctxTenantId, new EdgeId(id));
                break;
            case OTA_PACKAGE:
                tenantEntity = ctx.getOtaPackageService().findOtaPackageInfoById(ctxTenantId, new OtaPackageId(id));
                break;
            case ASSET_PROFILE:
                // Profile 信息从上下文缓存读取，是否回源由缓存实现决定。
                tenantEntity = ctx.getAssetProfileCache().get(ctxTenantId, new AssetProfileId(id));
                break;
            case DEVICE_PROFILE:
                // Device Profile 同样通过缓存入口读取，避免本类直接感知数据库细节。
                tenantEntity = ctx.getDeviceProfileCache().get(ctxTenantId, new DeviceProfileId(id));
                break;
            case WIDGET_TYPE:
                tenantEntity = ctx.getWidgetTypeService().findWidgetTypeById(ctxTenantId, new WidgetTypeId(id));
                break;
            case WIDGETS_BUNDLE:
                tenantEntity = ctx.getWidgetBundleService().findWidgetsBundleById(ctxTenantId, new WidgetsBundleId(id));
                break;
            case RPC:
                tenantEntity = ctx.getRpcService().findRpcById(ctxTenantId, new RpcId(id));
                break;
            case QUEUE:
                tenantEntity = ctx.getQueueService().findQueueById(ctxTenantId, new QueueId(id));
                break;
            case API_USAGE_STATE:
                tenantEntity = ctx.getRuleEngineApiUsageStateService().findApiUsageStateById(ctxTenantId, new ApiUsageStateId(id));
                break;
            case TB_RESOURCE:
                tenantEntity = ctx.getResourceService().findResourceInfoById(ctxTenantId, new TbResourceId(id));
                break;
            case RULE_NODE:
                // RuleNode 本身不直接携带租户 ID，先查节点再通过所属规则链解析租户归属。
                RuleNode ruleNode = ctx.getRuleChainService().findRuleNodeById(ctxTenantId, new RuleNodeId(id));
                if (ruleNode != null) {
                    tenantEntity = ctx.getRuleChainService().findRuleChainById(ctxTenantId, ruleNode.getRuleChainId());
                } else {
                    tenantEntity = null;
                }
                break;
            case TENANT_PROFILE:
                if (ctx.getTenantProfile().getId().equals(entityId)) {
                    // 当前租户配置来自上下文，匹配时直接返回上下文租户 ID。
                    return ctxTenantId;
                } else {
                    tenantEntity = null;
                }
                break;
            case NOTIFICATION_TARGET:
                tenantEntity = ctx.getNotificationTargetService().findNotificationTargetById(ctxTenantId, new NotificationTargetId(id));
                break;
            case NOTIFICATION_TEMPLATE:
                tenantEntity = ctx.getNotificationTemplateService().findNotificationTemplateById(ctxTenantId, new NotificationTemplateId(id));
                break;
            case NOTIFICATION_REQUEST:
                tenantEntity = ctx.getNotificationRequestService().findNotificationRequestById(ctxTenantId, new NotificationRequestId(id));
                break;
            case NOTIFICATION:
                return ctxTenantId;
            case NOTIFICATION_RULE:
                tenantEntity = ctx.getNotificationRuleService().findNotificationRuleById(ctxTenantId, new NotificationRuleId(id));
                break;
            default:
                throw new RuntimeException("Unexpected entity type: " + entityId.getEntityType());
        }
        return tenantEntity != null ? tenantEntity.getTenantId() : null;
    }

}

/*
 * 本类总结：
 * 本类提供同步租户归属解析，覆盖多种实体类型；它本身不直接参与 Rule Engine 消息流或事务控制，具体数据库/缓存访问发生在 TbContext 暴露的服务与缓存中。
 */
