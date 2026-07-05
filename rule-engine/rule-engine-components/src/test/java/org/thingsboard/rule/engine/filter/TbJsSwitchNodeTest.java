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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `TbJsSwitchNodeTest` 测试类，用于验证 `TbJsSwitchNode` 相关行为。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbJsSwitchNodeTest {

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbJsSwitchNode node;

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
     * 规则链ID，用于定位对应业务对象。
     */
    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    /**
     * 功能：执行 `multipleRoutesAreAllowed` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void multipleRoutesAreAllowed() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        when(scriptEngine.executeSwitchAsync(msg)).thenReturn(Futures.immediateFuture(Sets.newHashSet("one", "three")));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, Sets.newHashSet("one", "three"));
    }

    /**
     * 功能：初始化或启动`With Script`。
     * 参数：无。
     * 返回：无。
     */
    private void initWithScript() throws TbNodeException {
        TbJsSwitchNodeConfiguration config = new TbJsSwitchNodeConfiguration();
        config.setScriptLang(ScriptLanguage.JS);
        config.setJsScript("scr");
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "scr")).thenReturn(scriptEngine);

        node = new TbJsSwitchNode();
        node.init(ctx, nodeConfiguration);
    }
}
/*
 * 本类总结：{@code TbJsSwitchNodeTest} 为 {@code TbJsSwitchNode} 的 过滤/分流节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
