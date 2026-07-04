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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityFieldsData;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * 异步加载实体字段数据的工具类。
 * 本类按实体类型调用对应服务并转换为 EntityFieldsData；数据库读取、缓存读取和服务层线程模型由 TbContext 暴露的服务实现决定。
 */
public class EntitiesFieldsAsyncLoader {

    /**
     * 根据来源实体 ID 异步加载可用于规则处理的实体字段。
     * 本方法不直接发送、确认或修改 Rule Engine 消息；它只返回 Future 供调用方在消息流中继续使用。
     *
     * @param ctx 规则节点上下文，提供租户、实体服务和数据库回调执行器
     * @param originatorId 当前消息来源实体 ID
     * @return EntityFieldsData 的异步结果
     */
    public static ListenableFuture<EntityFieldsData> findAsync(TbContext ctx, EntityId originatorId) {
        switch (originatorId.getEntityType()) {  // TODO: use EntityServiceRegistry
            case TENANT:
                return toEntityFieldsDataAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), (TenantId) originatorId),
                        EntityFieldsData::new, ctx);
            case CUSTOMER:
                return toEntityFieldsDataAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), (CustomerId) originatorId),
                        EntityFieldsData::new, ctx);
            case USER:
                return toEntityFieldsDataAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) originatorId),
                        EntityFieldsData::new, ctx);
            case ASSET:
                return toEntityFieldsDataAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) originatorId),
                        EntityFieldsData::new, ctx);
            case DEVICE:
                return toEntityFieldsDataAsync(Futures.immediateFuture(ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) originatorId)),
                        EntityFieldsData::new, ctx);
            case ALARM:
                return toEntityFieldsDataAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) originatorId),
                        EntityFieldsData::new, ctx);
            case RULE_CHAIN:
                return toEntityFieldsDataAsync(ctx.getRuleChainService().findRuleChainByIdAsync(ctx.getTenantId(), (RuleChainId) originatorId),
                        EntityFieldsData::new, ctx);
            case ENTITY_VIEW:
                return toEntityFieldsDataAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) originatorId),
                        EntityFieldsData::new, ctx);
            case EDGE:
                return toEntityFieldsDataAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), (EdgeId) originatorId),
                        EntityFieldsData::new, ctx);
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType: " + originatorId.getEntityType()));
        }
    }

    /**
     * 将服务层异步读取到的实体转换为 EntityFieldsData。
     * 回调在 ctx.getDbCallbackExecutor() 上执行；实体不存在时返回失败 Future，调用方需要在 Rule Engine 消息流中处理该失败。
     *
     * @param future 服务层实体异步读取结果
     * @param converter 实体到字段数据的转换函数
     * @param ctx 规则节点上下文，提供数据库回调执行器
     * @param <T> 带 UUID 主键的实体类型
     * @return EntityFieldsData 的异步结果
     */
    private static <T extends BaseData<? extends UUIDBased>> ListenableFuture<EntityFieldsData> toEntityFieldsDataAsync(
            ListenableFuture<T> future,
            Function<T, EntityFieldsData> converter,
            TbContext ctx
    ) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(converter.apply(in))
                : Futures.immediateFailedFuture(new NoSuchElementException("Entity not found!")), ctx.getDbCallbackExecutor());
    }

}

/*
 * 本类总结：
 * 本类是实体字段异步加载 helper，不保存共享状态；服务读取可能落到数据库或缓存，Future 回调和失败传播由调用方接入 Rule Engine 消息流。
 */
