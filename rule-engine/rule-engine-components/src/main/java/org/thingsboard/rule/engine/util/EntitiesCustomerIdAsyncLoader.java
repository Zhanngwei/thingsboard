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
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

/**
 * 异步解析实体所属客户 ID 的工具类。
 * 本类无共享可变字段；服务层读取可能访问数据库或缓存，转换回调使用 TbContext 提供的数据库回调执行器。
 */
public class EntitiesCustomerIdAsyncLoader {

    /**
     * 根据实体 ID 查找其所属客户 ID。
     * CUSTOMER 类型会立即返回自身；USER、ASSET、DEVICE 通过对应服务读取实体后转换，方法本身不直接发送 Rule Engine 消息。
     *
     * @param ctx 规则节点上下文，提供租户、实体服务和回调执行器
     * @param originator 当前消息来源实体
     * @return 客户 ID 的异步结果
     */
    public static ListenableFuture<CustomerId> findEntityIdAsync(TbContext ctx, EntityId originator) {
        switch (originator.getEntityType()) {
            case CUSTOMER:
                return Futures.immediateFuture((CustomerId) originator);
            case USER:
                return toCustomerIdAsync(ctx, ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) originator));
            case ASSET:
                return toCustomerIdAsync(ctx, ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) originator));
            case DEVICE:
                return toCustomerIdAsync(ctx, Futures.immediateFuture(ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) originator)));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType: " + originator.getEntityType()));
        }
    }

    /**
     * 将带客户归属的实体异步结果转换为客户 ID。
     * Futures.transform 的回调在 ctx.getDbCallbackExecutor() 上执行，避免在服务返回线程中执行后续转换；空实体会转换为 null。
     *
     * @param ctx 规则节点上下文，提供数据库回调执行器
     * @param future 实体异步读取结果
     * @param <T> 实现 HasCustomerId 的实体类型
     * @return 客户 ID 的异步结果
     */
    private static <T extends HasCustomerId> ListenableFuture<CustomerId> toCustomerIdAsync(TbContext ctx, ListenableFuture<T> future) {
        return Futures.transform(future, in -> in != null ? in.getCustomerId() : null, ctx.getDbCallbackExecutor());
    }

}

/*
 * 本类总结：
 * 本类为规则节点提供客户归属解析的异步 helper；具体数据库/缓存读取由服务层决定，本类只做 Future 转换且不持有跨消息状态。
 */
