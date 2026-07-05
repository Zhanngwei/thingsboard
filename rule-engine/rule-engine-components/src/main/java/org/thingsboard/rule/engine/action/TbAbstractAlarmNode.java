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
 * 中文说明：`TbAbstractAlarmNode` 是抽象告警节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAbstractAlarmNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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
    /*
     * 本类总结：`TbAbstractAlarmNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
