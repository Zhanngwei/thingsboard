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
package org.thingsboard.server.common.data.id;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.util.UUID;

/**
 * Created by ashvayka on 25.04.17.
 */
/**
 * 中文说明：
 * 1. `EntityIdFactory` 是 ThingsBoard Common Data 中创建或提供实体对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public class EntityIdFactory {

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndUuid(int type, String uuid) {
        return getByTypeAndUuid(EntityType.values()[type], UUID.fromString(uuid));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndUuid(int type, UUID uuid) {
        return getByTypeAndUuid(EntityType.values()[type], uuid);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndUuid(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndId(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndUuid(String type, UUID uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), uuid);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndUuid(EntityType type, String uuid) {
        return getByTypeAndUuid(type, UUID.fromString(uuid));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByTypeAndUuid(EntityType type, UUID uuid) {
        switch (type) {
            case TENANT:
                return TenantId.fromUUID(uuid);
            case CUSTOMER:
                return new CustomerId(uuid);
            case USER:
                return new UserId(uuid);
            case DASHBOARD:
                return new DashboardId(uuid);
            case DEVICE:
                return new DeviceId(uuid);
            case ASSET:
                return new AssetId(uuid);
            case ALARM:
                return new AlarmId(uuid);
            case RULE_CHAIN:
                return new RuleChainId(uuid);
            case RULE_NODE:
                return new RuleNodeId(uuid);
            case ENTITY_VIEW:
                return new EntityViewId(uuid);
            case WIDGETS_BUNDLE:
                return new WidgetsBundleId(uuid);
            case WIDGET_TYPE:
                return new WidgetTypeId(uuid);
            case DEVICE_PROFILE:
                return new DeviceProfileId(uuid);
            case ASSET_PROFILE:
                return new AssetProfileId(uuid);
            case TENANT_PROFILE:
                return new TenantProfileId(uuid);
            case API_USAGE_STATE:
                return new ApiUsageStateId(uuid);
            case TB_RESOURCE:
                return new TbResourceId(uuid);
            case OTA_PACKAGE:
                return new OtaPackageId(uuid);
            case EDGE:
                return new EdgeId(uuid);
            case RPC:
                return new RpcId(uuid);
            case QUEUE:
                return new QueueId(uuid);
            case NOTIFICATION_TARGET:
                return new NotificationTargetId(uuid);
            case NOTIFICATION_REQUEST:
                return new NotificationRequestId(uuid);
            case NOTIFICATION_RULE:
                return new NotificationRuleId(uuid);
            case NOTIFICATION_TEMPLATE:
                return new NotificationTemplateId(uuid);
            case NOTIFICATION:
                return new NotificationId(uuid);
        }
        throw new IllegalArgumentException("EntityType " + type + " is not supported!");
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeEventType`：类型。
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    public static EntityId getByEdgeEventTypeAndUuid(EdgeEventType edgeEventType, UUID uuid) {
        switch (edgeEventType) {
            case TENANT:
                return new TenantId(uuid);
            case CUSTOMER:
                return new CustomerId(uuid);
            case USER:
                return new UserId(uuid);
            case DASHBOARD:
                return new DashboardId(uuid);
            case DEVICE:
                return new DeviceId(uuid);
            case ASSET:
                return new AssetId(uuid);
            case ALARM:
                return new AlarmId(uuid);
            case RULE_CHAIN:
                return new RuleChainId(uuid);
            case ENTITY_VIEW:
                return new EntityViewId(uuid);
            case WIDGETS_BUNDLE:
                return new WidgetsBundleId(uuid);
            case WIDGET_TYPE:
                return new WidgetTypeId(uuid);
            case DEVICE_PROFILE:
                return new DeviceProfileId(uuid);
            case ASSET_PROFILE:
                return new AssetProfileId(uuid);
            case TENANT_PROFILE:
                return new TenantProfileId(uuid);
            case OTA_PACKAGE:
                return new OtaPackageId(uuid);
            case EDGE:
                return new EdgeId(uuid);
            case QUEUE:
                return new QueueId(uuid);
            case TB_RESOURCE:
                return new TbResourceId(uuid);
        }
        throw new IllegalArgumentException("EdgeEventType " + edgeEventType + " is not supported!");
    }

}
