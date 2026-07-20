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
package org.thingsboard.rule.engine.transform;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `TbTransformMsgNodeTest` 测试类，用于验证 `TbTransformMsgNode` 相关行为。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbTransformMsgNodeTest {

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbTransformMsgNode node;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctx;
    /**
     * 脚本执行器，表示当前对象的对应属性。
     */
    @Mock
    private ScriptEngine scriptEngine;

    /**
     * 功能：执行 `metadataCanBeUpdated` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void metadataCanBeUpdated() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        String rawJson = "{\"passed\": 5}";

        RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, metaData, TbMsgDataType.JSON,rawJson, ruleChainId, ruleNodeId);
        TbMsg transformedMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, metaData, TbMsgDataType.JSON, "{new}", ruleChainId, ruleNodeId);
        when(scriptEngine.executeUpdateAsync(msg)).thenReturn(Futures.immediateFuture(Collections.singletonList(transformedMsg)));

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(captor.capture());
        TbMsg actualMsg = captor.getValue();
        assertEquals(transformedMsg, actualMsg);
    }

    /**
     * 功能：执行 `exceptionHandledCorrectly` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void exceptionHandledCorrectly() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        String rawJson = "{\"passed\": 5";

        RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        when(scriptEngine.executeUpdateAsync(msg)).thenReturn(Futures.immediateFailedFuture(new IllegalStateException("error")));

        node.onMsg(ctx, msg);
        verifyError(msg, "error", IllegalStateException.class);
    }

    /**
     * 功能：初始化或启动`With Script`。
     * 参数：无。
     * 返回：无。
     */
    private void initWithScript() throws TbNodeException {
        TbTransformMsgNodeConfiguration config = new TbTransformMsgNodeConfiguration();
        config.setScriptLang(ScriptLanguage.JS);
        config.setJsScript("scr");
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "scr")).thenReturn(scriptEngine);

        node = new TbTransformMsgNode();
        node.init(ctx, nodeConfiguration);
    }

    /**
     * 功能：校验错误信息。
     * 参数：
     * - `msg`：待处理消息。
     * - `message`：待处理消息。
     * - `expectedClass`：`expectedClass` 参数。
     * 返回：无。
     */
    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }
}
