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
package org.thingsboard.rule.engine.rpc;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * `TbSendRPCReplyNodeTest` 测试类，用于验证 `TbSendRPCReplyNode` 相关行为。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbSendRPCReplyNodeTest {

    /**
     * 服务常量，用于统一引用固定值。
     */
    private static final String DUMMY_SERVICE_ID = "testServiceId";
    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final int DUMMY_REQUEST_ID = 0;
    /**
     * 会话常量，用于统一引用固定值。
     */
    private static final UUID DUMMY_SESSION_ID = UUID.randomUUID();
    /**
     * 数据常量，用于统一引用固定值。
     */
    private static final String DUMMY_DATA = "{\"key\":\"value\"}";

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    TbSendRPCReplyNode node;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctx;

    /**
     * RPC，提供当前类调用的业务操作。
     */
    @Mock
    private RuleEngineRpcService rpcService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Mock
    private EdgeEventService edgeEventService;

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    @Mock
    private ListeningExecutor listeningExecutor;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws TbNodeException {
        node = new TbSendRPCReplyNode();
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    /**
     * 功能：发送或提交传输层。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void sendReplyToTransport() {
        Mockito.when(ctx.getRpcService()).thenReturn(rpcService);


        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, getDefaultMetadata(),
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(rpcService).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
        verify(edgeEventService, never()).saveAsync(any());
    }

    /**
     * 功能：发送或提交边缘节点。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void sendReplyToEdgeQueue() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(listeningExecutor);

        TbMsgMetaData defaultMetadata = getDefaultMetadata();
        defaultMetadata.putValue(DataConstants.EDGE_ID, UUID.randomUUID().toString());
        defaultMetadata.putValue(DataConstants.DEVICE_ID, UUID.randomUUID().toString());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, defaultMetadata,
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
        verify(rpcService, never()).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
    }

    /**
     * 功能：获取`Default Metadata`。
     * 参数：无。
     * 返回：处理结果。
     */
    private TbMsgMetaData getDefaultMetadata() {
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(config.getServiceIdMetaDataAttribute(), DUMMY_SERVICE_ID);
        metadata.putValue(config.getSessionIdMetaDataAttribute(), DUMMY_SESSION_ID.toString());
        metadata.putValue(config.getRequestIdMetaDataAttribute(), Integer.toString(DUMMY_REQUEST_ID));
        return metadata;
    }
}
