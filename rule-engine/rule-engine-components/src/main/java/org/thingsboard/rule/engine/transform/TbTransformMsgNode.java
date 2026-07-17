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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

/**
 * 中文说明：
 * 1. `TbTransformMsgNode` 是 ThingsBoard Rule Engine Components 中处理消息的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractTransformNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "script",
        configClazz = TbTransformMsgNodeConfiguration.class,
        nodeDescription = "Change Message payload, Metadata or Message type using JavaScript",
        nodeDetails = "JavaScript function receive 3 input parameters <br/> " +
                "<code>msg</code> - is a message payload.<br/>" +
                "<code>metadata</code> - is a message metadata.<br/>" +
                "<code>msgType</code> - is a message type.<br/>" +
                "Should return the following structure:<br/>" +
                "<code>{ msg: <i style=\"color: #666;\">new payload</i>,<br/>&nbsp&nbsp&nbspmetadata: <i style=\"color: #666;\">new metadata</i>,<br/>&nbsp&nbsp&nbspmsgType: <i style=\"color: #666;\">new msgType</i> }</code><br/>" +
                "All fields in resulting object are optional and will be taken from original message if not specified.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeScriptConfig"
)
public class TbTransformMsgNode extends TbAbstractTransformNode<TbTransformMsgNodeConfiguration> {

    /**
     * 脚本执行器，表示当前对象的对应属性。
     */
    private ScriptEngine scriptEngine;

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbTransformMsgNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbTransformMsgNodeConfiguration.class);
        scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript());
        return config;
    }

    /**
     * 功能：执行 `transform` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ctx.logJsEvalRequest();
        return scriptEngine.executeUpdateAsync(msg);
    }

    /**
     * 功能：转换失败信息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：无。
     */
    @Override
    protected void transformFailure(TbContext ctx, TbMsg msg, Throwable t) {
        ctx.logJsEvalFailure();
        super.transformFailure(ctx, msg, t);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (scriptEngine != null) {
            scriptEngine.destroy();
        }
    }
}
