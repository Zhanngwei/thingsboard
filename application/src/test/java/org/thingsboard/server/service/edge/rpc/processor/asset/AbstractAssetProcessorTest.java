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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessorTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.willReturn;

/**
 * 中文说明：
 * 1. 类目的：`AbstractAssetProcessorTest` 是ThingsBoard Application 测试模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public abstract class AbstractAssetProcessorTest extends BaseEdgeProcessorTest {


    /**
     * 资产ID，用于定位对应业务对象。
     */
    protected AssetId assetId;
    protected AssetProfileId assetProfileId;
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    protected AssetProfile assetProfile;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        assetProfileId = new AssetProfileId(UUID.randomUUID());

        assetProfile = new AssetProfile();
        assetProfile.setId(assetProfileId);
        assetProfile.setName("AssetProfile");
        assetProfile.setDefault(true);

        Asset asset = new Asset();
        asset.setAssetProfileId(assetProfileId);
        asset.setId(assetId);
        asset.setName("Asset");
        asset.setType(assetProfile.getName());

        edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);


        willReturn(asset).given(assetService).findAssetById(tenantId, assetId);
        willReturn(assetProfile).given(assetProfileService).findAssetProfileById(tenantId, assetProfileId);
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `expectedDashboardIdMSB`：`expectedDashboardIdMSB` 参数。
     * - `expectedDashboardIdLSB`：`expectedDashboardIdLSB` 参数。
     * - `expectedRuleChainIdMSB`：`expectedRuleChainIdMSB` 参数。
     * - `expectedRuleChainIdLSB`：`expectedRuleChainIdLSB` 参数。
     * 返回：无。
     */
    protected void updateAssetProfileDefaultFields(long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DashboardId dashboardId = getDashboardId(expectedDashboardIdMSB, expectedDashboardIdLSB);
        RuleChainId ruleChainId = getRuleChainId(expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        assetProfile.setDefaultDashboardId(dashboardId);
        assetProfile.setDefaultEdgeRuleChainId(ruleChainId);

    }

    /**
     * 功能：执行 `verify` 对应的处理。
     * 参数：
     * - `downlinkMsg`：待处理消息。
     * - `expectedDashboardIdMSB`：`expectedDashboardIdMSB` 参数。
     * - `expectedDashboardIdLSB`：`expectedDashboardIdLSB` 参数。
     * - `expectedRuleChainIdMSB`：`expectedRuleChainIdMSB` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void verify(DownlinkMsg downlinkMsg, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                          long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        AssetProfileUpdateMsg assetProfileUpdateMsg = downlinkMsg.getAssetProfileUpdateMsgList().get(0);
        assertNotNull(assetProfileUpdateMsg);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultDashboardIdMSB()).isEqualTo(expectedDashboardIdMSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultDashboardIdLSB()).isEqualTo(expectedDashboardIdLSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultRuleChainIdMSB()).isEqualTo(expectedRuleChainIdMSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultRuleChainIdLSB()).isEqualTo(expectedRuleChainIdLSB);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractAssetProcessorTest` 在 ThingsBoard Application 测试模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
