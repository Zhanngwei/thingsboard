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
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

/**
 * 按关系条件异步查找关联设备 ID 的工具类。
 * 本类不保存状态；关系查询和设备查询由 DeviceService 处理，数据库或缓存读取策略由服务层决定。
 */
public class EntitiesRelatedDeviceIdAsyncLoader {

    /**
     * 从指定 originator 出发查找第一条匹配关系上的设备 ID。
     * 本方法返回异步结果，转换回调在 ctx.getDbCallbackExecutor() 上执行；它不直接发送 Rule Engine 消息，只为调用方提供后续流转所需的实体 ID。
     *
     * @param ctx 规则节点上下文，提供租户、设备服务和回调执行器
     * @param originator 查询起点实体
     * @param deviceRelationsQuery 设备关系查询配置
     * @return 第一个匹配设备 ID 的异步结果，未找到时为 null
     */
    public static ListenableFuture<DeviceId> findDeviceAsync(
            TbContext ctx,
            EntityId originator,
            DeviceRelationsQuery deviceRelationsQuery
    ) {
        var deviceService = ctx.getDeviceService();
        var query = buildQuery(originator, deviceRelationsQuery);
        var devicesListFuture = deviceService.findDevicesByQuery(ctx.getTenantId(), query);
        return Futures.transformAsync(devicesListFuture,
                deviceList -> CollectionUtils.isNotEmpty(deviceList) ?
                        Futures.immediateFuture(deviceList.get(0).getId())
                        : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
    }

    /**
     * 根据规则节点配置构造设备搜索查询。
     * 本方法只组装内存查询对象，不访问数据库、缓存或 Rule Engine 上下文。
     *
     * @param originator 查询起点实体
     * @param deviceRelationsQuery 设备关系查询配置
     * @return DeviceService 可消费的查询对象
     */
    private static DeviceSearchQuery buildQuery(EntityId originator, DeviceRelationsQuery deviceRelationsQuery) {
        var query = new DeviceSearchQuery();
        var parameters = new RelationsSearchParameters(
                originator,
                deviceRelationsQuery.getDirection(),
                deviceRelationsQuery.getMaxLevel(),
                deviceRelationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setRelationType(deviceRelationsQuery.getRelationType());
        query.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());
        return query;
    }

}

/*
 * 本类总结：
 * 本类把规则节点的设备关系配置转换为异步设备查询；具体读取和缓存行为在 DeviceService 内，线程安全边界在无状态 helper 与 DB callback executor 之间。
 */
