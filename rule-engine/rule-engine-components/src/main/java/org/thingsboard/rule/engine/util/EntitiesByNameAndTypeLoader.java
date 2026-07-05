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
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

/**
 * `EntitiesByNameAndTypeLoader` 类，封装当前模块中的一组相关职责。
 */
public class EntitiesByNameAndTypeLoader {

    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final List<EntityType> AVAILABLE_ENTITY_TYPES = List.of(
            EntityType.DEVICE,
            EntityType.ASSET,
            EntityType.ENTITY_VIEW,
            EntityType.EDGE,
            EntityType.USER);

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityType`：实体对象。
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public static EntityId findEntityId(TbContext ctx, EntityType entityType, String entityName) {
        BaseData<? extends EntityId> targetEntity;
        switch (entityType) {
            case DEVICE:
                targetEntity = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case ASSET:
                targetEntity = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case ENTITY_VIEW:
                targetEntity = ctx.getEntityViewService().findEntityViewByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case EDGE:
                targetEntity = ctx.getEdgeService().findEdgeByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case USER:
                targetEntity = ctx.getUserService().findUserByTenantIdAndEmail(ctx.getTenantId(), entityName);
                break;
            default:
                throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
        if (targetEntity == null) {
            throw new IllegalStateException("Failed to found " + entityType.name() + "  entity by name: '" + entityName + "'!");
        }
        return targetEntity.getId();
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：无。
     */
    public static void checkEntityType(EntityType entityType) {
        if (!AVAILABLE_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
    }

}

/*
 * 本类总结：
 * 本类提供按名称解析实体 ID 的同步工具方法；它自身无共享可变状态，Rule Engine 消息流和线程调度由调用该工具的节点负责。
 */
