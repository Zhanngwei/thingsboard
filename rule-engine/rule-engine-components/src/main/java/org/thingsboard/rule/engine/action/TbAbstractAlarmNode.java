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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.thingsboard.common.util.DonAsynchron.withCallback;


/**
 * 中文说明：
 * 1. `TbAbstractAlarmNode` 是 ThingsBoard Rule Engine Components 中处理告警的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractAlarmNodeConfiguration`、`TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
public abstract class TbAbstractAlarmNode<C extends TbAbstractAlarmNodeConfiguration> implements TbNode {

    /**
     * 告警常量，用于统一引用固定值。
     */
    static final String PREV_ALARM_DETAILS = "prevAlarmDetails";

    /**
     * 配置，保存当前对象的配置选项。
     */
    protected C config;
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
        this.config = loadAlarmNodeConfig(configuration);
        scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getAlarmDetailsBuildTbel() : config.getAlarmDetailsBuildJs());
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    protected abstract C loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processAlarm(ctx, msg),
                alarmResult -> {
                    if (alarmResult.alarm == null) {
                        ctx.tellNext(msg, TbNodeConnectionType.FALSE);
                    } else if (alarmResult.isCreated) {
                        tellNext(ctx, msg, alarmResult, TbMsgType.ENTITY_CREATED, "Created");
                    } else if (alarmResult.isUpdated || alarmResult.isSeverityUpdated) {
                        tellNext(ctx, msg, alarmResult, TbMsgType.ENTITY_UPDATED, "Updated");
                    } else if (alarmResult.isCleared) {
                        tellNext(ctx, msg, alarmResult, TbMsgType.ALARM_CLEAR, "Cleared");
                    } else {
                        ctx.tellSuccess(msg);
                    }
                },
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg);

    /**
     * 功能：构建告警。
     * 参数：
     * - `msg`：待处理消息。
     * - `previousDetails`：`previousDetails` 参数。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<JsonNode> buildAlarmDetails(TbMsg msg, JsonNode previousDetails) {
        try {
            TbMsg dummyMsg = msg;
            if (previousDetails != null) {
                TbMsgMetaData metaData = msg.getMetaData().copy();
                metaData.putValue(PREV_ALARM_DETAILS, JacksonUtil.toString(previousDetails));
                dummyMsg = TbMsg.transformMsgMetadata(msg, metaData);
            }
            return scriptEngine.executeJsonAsync(dummyMsg);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * 功能：执行 `toAlarmMsg` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `alarmResult`：`alarmResult` 参数。
     * - `originalMsg`：待处理消息。
     * 返回：处理结果。
     */
    public static TbMsg toAlarmMsg(TbContext ctx, TbAlarmResult alarmResult, TbMsg originalMsg) {
        JsonNode jsonNodes = JacksonUtil.valueToTree(alarmResult.alarm);
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        if (alarmResult.isCreated) {
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated || alarmResult.isSeverityUpdated) {
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isCleared) {
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        return ctx.transformMsg(originalMsg, TbMsgType.ALARM, originalMsg.getOriginator(), metaData, data);
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

    /**
     * 功能：执行 `tellNext` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `alarmResult`：`alarmResult` 参数。
     * - `actionMsgType`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void tellNext(TbContext ctx, TbMsg msg, TbAlarmResult alarmResult, TbMsgType actionMsgType, String alarmAction) {
        ctx.enqueue(ctx.alarmActionMsg(alarmResult.alarm, ctx.getSelfId(), actionMsgType),
                () -> ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), alarmAction),
                throwable -> ctx.tellFailure(toAlarmMsg(ctx, alarmResult, msg), throwable));
    }
}
