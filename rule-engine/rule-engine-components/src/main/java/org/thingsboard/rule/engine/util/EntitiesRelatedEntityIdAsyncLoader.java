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
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

/**
 * 按通用关系条件异步查找关联实体 ID 的工具类。
 * 本类无共享状态；RelationService 负责实际关系读取，可能访问数据库或缓存，具体行为不在本类中实现。
 */
public class EntitiesRelatedEntityIdAsyncLoader {

    /**
     * 从指定 originator 出发查找第一条匹配关系上的对端实体 ID。
     * FROM 方向返回关系目标实体，TO 方向返回关系来源实体；转换回调在 ctx.getDbCallbackExecutor() 上执行，本方法不直接处理 Rule Engine 消息投递。
     *
     * @param ctx 规则节点上下文，提供租户、关系服务和回调执行器
     * @param originator 查询起点实体
     * @param relationsQuery 通用关系查询配置
     * @return 第一个匹配对端实体 ID 的异步结果，未找到时为 null
     */
    public static ListenableFuture<EntityId> findEntityAsync(
            TbContext ctx,
            EntityId originator,
            RelationsQuery relationsQuery
    ) {
        var relationService = ctx.getRelationService();
        var query = buildQuery(originator, relationsQuery);
        var relationListFuture = relationService.findByQuery(ctx.getTenantId(), query);
        if (relationsQuery.getDirection() == EntitySearchDirection.FROM) {
            return Futures.transformAsync(relationListFuture,
                    relationList -> CollectionUtils.isNotEmpty(relationList) ?
                            Futures.immediateFuture(relationList.get(0).getTo())
                            : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
        } else if (relationsQuery.getDirection() == EntitySearchDirection.TO) {
            return Futures.transformAsync(relationListFuture,
                    relationList -> CollectionUtils.isNotEmpty(relationList) ?
                            Futures.immediateFuture(relationList.get(0).getFrom())
                            : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
        }
        return Futures.immediateFailedFuture(new IllegalStateException("Unknown direction"));
    }

    /**
     * 根据配置构造 RelationService 查询对象。
     * 本方法只创建内存查询对象，不进行数据库读取、缓存读取或消息流操作。
     *
     * @param originator 查询起点实体
     * @param relationsQuery 通用关系查询配置
     * @return RelationService 可消费的查询对象
     */
    private static EntityRelationsQuery buildQuery(EntityId originator, RelationsQuery relationsQuery) {
        var query = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                originator,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

}

/*
 * 本类总结：
 * 本类负责把关系查询配置转换为异步关系查找，并在 DB callback executor 上解析第一条结果；Rule Engine 消息流、失败处理和后续实体使用均由调用方负责。
 */
