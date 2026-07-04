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
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;

/**
 * 异步加载告警真实 originator 的工具类。
 * 本类不保存共享可变状态；数据库读取或缓存命中由 AlarmService 的异步实现决定，回调在线程池边界由 TbContext 提供。
 */
public class EntitiesAlarmOriginatorIdAsyncLoader {

    /**
     * 根据当前 originator 查找应继续用于规则处理的实体 ID。
     * 目前只支持 ALARM 类型，会通过 AlarmService 异步读取告警并在回调中取出告警 originator；本方法本身不直接发送 Rule Engine 消息。
     *
     * @param ctx 规则节点上下文，提供租户、服务和数据库回调执行器
     * @param originator 当前消息来源实体
     * @return 告警来源实体的异步结果
     */
    public static ListenableFuture<EntityId> findEntityIdAsync(TbContext ctx, EntityId originator) {
        switch (originator.getEntityType()) {
            case ALARM:
                return getAlarmOriginatorAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) originator), ctx);
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType " + originator.getEntityType()));
        }
    }

    /**
     * 将告警异步读取结果转换为告警来源实体 ID。
     * Futures.transformAsync 的回调在 ctx.getDbCallbackExecutor() 上执行；该方法只做结果转换，不修改 Rule Engine 消息上下文。
     *
     * @param future 告警异步读取结果
     * @param ctx 规则节点上下文，提供数据库回调执行器
     * @return 告警 originator 的异步结果，告警不存在时返回 null
     */
    private static ListenableFuture<EntityId> getAlarmOriginatorAsync(ListenableFuture<Alarm> future, TbContext ctx) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(in.getOriginator())
                : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
    }

}

/*
 * 本类总结：
 * 本类为规则节点提供告警 originator 的异步解析能力；它通过服务层发起读取，具体数据库/缓存行为由服务实现决定，回调由 TbContext 的 DB callback executor 承载。
 */
