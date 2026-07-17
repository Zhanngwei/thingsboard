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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.common.util.JacksonUtil;
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

import java.util.Objects;

/**
 * 中文说明：
 * 1. `TbLogNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Log Node` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "log",
        configClazz = TbLogNodeConfiguration.class,
        nodeDescription = "Log incoming messages using JS script for transformation Message into String",
        nodeDetails = "Transform incoming Message with configured JS function to String and log final value into Thingsboard log file. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeLogConfig",
        icon = "menu"
)
public class TbLogNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbLogNodeConfiguration config;
    /**
     * 脚本执行器，表示当前对象的对应属性。
     */
    private ScriptEngine scriptEngine;
    /**
     * 是否满足`standard`条件。
     */
    private boolean standard;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbLogNodeConfiguration.class);
        this.standard = isStandard(config);
        this.scriptEngine = this.standard ? null : createScriptEngine(ctx, config);
    }

    /**
     * 功能：保存或创建脚本执行器。
     * 参数：
     * - `ctx`：处理上下文。
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    ScriptEngine createScriptEngine(TbContext ctx, TbLogNodeConfiguration config) {
        return ctx.createScriptEngine(config.getScriptLang(),
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
        if (standard) {
            logStandard(ctx, msg);
            return;
        }

        ctx.logJsEvalRequest();
        Futures.addCallback(scriptEngine.executeToStringAsync(msg), new FutureCallback<String>() {
            /**
             * 功能：处理`on Success`。
             * 参数：
             * - `result`：`result` 参数。
             * 返回：无。
             */
            @Override
            public void onSuccess(@Nullable String result) {
                ctx.logJsEvalResponse();
                log.info(result);
                ctx.tellSuccess(msg);
            }

            /**
             * 功能：处理失败信息。
             * 参数：
             * - `t`：`t` 参数。
             * 返回：无。
             */
            @Override
            public void onFailure(Throwable t) {
                ctx.logJsEvalResponse();
                ctx.tellFailure(msg, t);
            }
        }, MoreExecutors.directExecutor()); //usually js responses runs on js callback executor
    }

    /**
     * 功能：判断`Standard`。
     * 参数：
     * - `conf`：`conf` 参数。
     * 返回：判断结果。
     */
    boolean isStandard(TbLogNodeConfiguration conf) {
        Objects.requireNonNull(conf, "node config is null");
        final TbLogNodeConfiguration defaultConfig = new TbLogNodeConfiguration().defaultConfiguration();

        if (conf.getScriptLang() == null || conf.getScriptLang().equals(ScriptLanguage.JS)) {
            return defaultConfig.getJsScript().equals(conf.getJsScript());
        } else if (conf.getScriptLang().equals(ScriptLanguage.TBEL)) {
            return defaultConfig.getTbelScript().equals(conf.getTbelScript());
        } else {
            log.warn("No rule to define isStandard script for script language [{}], assuming that is non-standard", conf.getScriptLang());
            return false;
        }
    }

    /**
     * 功能：执行 `logStandard` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void logStandard(TbContext ctx, TbMsg msg) {
        log.info(toLogMessage(msg));
        ctx.tellSuccess(msg);
    }

    /**
     * 功能：执行 `toLogMessage` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：文本结果。
     */
    String toLogMessage(TbMsg msg) {
        return "\n" +
                "Incoming message:\n" + msg.getData() + "\n" +
                "Incoming metadata:\n" + JacksonUtil.toString(msg.getMetaData().getData());
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
