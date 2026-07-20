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
package org.thingsboard.rule.engine.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `TbAssetTypeSwitchNodeTest` 测试类，用于验证 `TbAssetTypeSwitchNode` 相关行为。
 */
class TbAssetTypeSwitchNodeTest {

    /**
     * 资产ID，用于定位对应业务对象。
     */
    private AssetId assetId;
    /**
     * 资产ID集合，用于去重保存或快速判断对象是否存在。
     */
    private AssetId assetIdDeleted;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    /**
     * 节点实例集合，用于去重保存或快速判断对象是否存在。
     */
    private TbAssetTypeSwitchNode node;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    private TbMsgCallback callback;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() throws TbNodeException {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        assetIdDeleted = new AssetId(UUID.randomUUID());

        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName("TestAssetProfile");

        //node
        EmptyNodeConfiguration config = new EmptyNodeConfiguration();
        node = new TbAssetTypeSwitchNode();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        //init mock
        ctx = mock(TbContext.class);
        RuleEngineAssetProfileCache assetProfileCache = mock(RuleEngineAssetProfileCache.class);
        callback = mock(TbMsgCallback.class);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAssetProfileCache()).thenReturn(assetProfileCache);

        doReturn(assetProfile).when(assetProfileCache).get(tenantId, assetId);
        doReturn(null).when(assetProfileCache).get(tenantId, assetIdDeleted);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_then_Fail` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_then_Fail() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        assertThatThrownBy(() -> {
            node.onMsg(ctx, getTbMsg(customerId));
        }).isInstanceOf(TbNodeException.class).hasMessageContaining("Unsupported originator type");
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_EntityIdDeleted_then_Fail` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_EntityIdDeleted_then_Fail() {
        assertThatThrownBy(() -> {
            node.onMsg(ctx, getTbMsg(assetIdDeleted));
        }).isInstanceOf(TbNodeException.class).hasMessageContaining("Asset profile for entity id");
    }

    /**
     * 功能：验证 `givenMsg_whenOnMsg_then_Success` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenMsg_whenOnMsg_then_Success() throws TbNodeException {
        TbMsg msg = getTbMsg(assetId);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq("TestAssetProfile"));
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private TbMsg getTbMsg(EntityId entityId) {
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT, callback);
    }

}
