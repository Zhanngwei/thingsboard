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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`AssetEdgeProcessorV1` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Component
@TbCoreComponent
public class AssetEdgeProcessorV1 extends AssetEdgeProcessor {

    /**
     * 功能：执行 `constructAssetFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `assetUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Asset constructAssetFromUpdateMsg(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetUpdateMsg.getName());
        asset.setCreatedTime(Uuids.unixTimestamp(assetId.getId()));
        asset.setType(assetUpdateMsg.getType());
        asset.setLabel(assetUpdateMsg.hasLabel() ? assetUpdateMsg.getLabel() : null);
        asset.setAdditionalInfo(assetUpdateMsg.hasAdditionalInfo()
                ? JacksonUtil.toJsonNode(assetUpdateMsg.getAdditionalInfo()) : null);

        UUID assetProfileUUID = safeGetUUID(assetUpdateMsg.getAssetProfileIdMSB(), assetUpdateMsg.getAssetProfileIdLSB());
        asset.setAssetProfileId(assetProfileUUID != null ? new AssetProfileId(assetProfileUUID) : null);

        CustomerId customerId = safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB());
        asset.setCustomerId(customerId);
        return asset;
    }

    /**
     * 功能：更新客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `asset`：`asset` 参数。
     * - `assetUpdateMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg) {
        CustomerId customerUUID = safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB());
        asset.setCustomerId(customerUUID != null ? customerUUID : customerId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetEdgeProcessorV1` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
