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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

/**
 * 中文说明：`TbJsSwitchNode` 是脚本切换节点规则节点，用于根据消息类型、实体类型、关系、脚本或告警状态判断消息路由。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbJsSwitchNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "switch", customRelations = true,
        relationTypes = {},
        configClazz = TbJsSwitchNodeConfiguration.class,
        nodeDescription = "Routes incoming message to one OR multiple output connections.",
        nodeDetails = "Node executes configured TBEL(recommended) or JavaScript function that returns array of strings (connection names). " +
                "If Array is empty - message not routed to next Node. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>msg.temperature < 10;</code><br/>" +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>metadata.customerName === 'John';</code><br/>" +
                "Message type can be accessed via <code>msgType</code> property.<br><br>" +
                "Output connections: <i>Custom connection(s) defined by switch node</i> or <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeSwitchConfig")
public class TbJsSwitchNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbJsSwitchNodeConfiguration config;
    /**
     * 脚本执行器，表示当前对象的对应属性。
     */
    private ScriptEngine scriptEngine;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsSwitchNodeConfiguration.class);
        this.scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript());
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.logJsEvalRequest();
        Futures.addCallback(scriptEngine.executeSwitchAsync(msg), new FutureCallback<>() {
            /**
             * 功能：处理`on Success`。
             * 参数：
             * - `result`：`result` 参数。
             * 返回：无。
             */
            @Override
            public void onSuccess(@Nullable Set<String> result) {
                ctx.logJsEvalResponse();
                processSwitch(ctx, msg, result);
            }

            /**
             * 功能：处理失败信息。
             * 参数：
             * - `t`：`t` 参数。
             * 返回：无。
             */
            @Override
            public void onFailure(Throwable t) {
                ctx.logJsEvalFailure();
                ctx.tellFailure(msg, t);
            }
        }, MoreExecutors.directExecutor()); //usually runs in a callbackExecutor
    }

    /**
     * 功能：处理`Switch`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `nextRelations`：`nextRelations` 参数。
     * 返回：无。
     */
    private void processSwitch(TbContext ctx, TbMsg msg, Set<String> nextRelations) {
        ctx.tellNext(msg, nextRelations);
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
    /*
     * 本类总结：`TbJsSwitchNode` 负责根据消息类型、实体类型、关系、脚本或告警状态判断消息路由；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
