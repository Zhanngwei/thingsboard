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
 * 按实体类型和名称同步查找实体 ID 的工具类。
 * 本类不保存运行态状态；底层服务可能读取数据库或命中缓存，具体由服务实现决定，本类不直接参与 Rule Engine 消息投递。
 */
public class EntitiesByNameAndTypeLoader {

    /**
     * 支持按名称查找的实体类型集合。
     */
    private static final List<EntityType> AVAILABLE_ENTITY_TYPES = List.of(
            EntityType.DEVICE,
            EntityType.ASSET,
            EntityType.ENTITY_VIEW,
            EntityType.EDGE,
            EntityType.USER);

    /**
     * 根据实体类型和名称查找实体 ID。
     * 本方法是同步封装，会直接调用对应服务；数据库读取、缓存读取和租户隔离由服务层处理，本方法不发送或确认 Rule Engine 消息。
     *
     * @param ctx 规则节点上下文，提供租户和实体服务
     * @param entityType 目标实体类型
     * @param entityName 目标实体名称或用户邮箱
     * @return 查找到的实体 ID
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
     * 校验实体类型是否支持按名称查找。
     * 本方法只检查内存常量列表，不访问数据库或缓存。
     *
     * @param entityType 待校验实体类型
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
