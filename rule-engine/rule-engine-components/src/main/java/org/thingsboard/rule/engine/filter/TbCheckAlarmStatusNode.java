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
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 中文说明：`TbCheckAlarmStatusNode` 是检查告警状态节点规则节点，用于根据消息类型、实体类型、关系、脚本或告警状态判断消息路由。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbCheckAlarmStatusNodeConfig`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "alarm status filter",
        configClazz = TbCheckAlarmStatusNodeConfig.class,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Checks alarm status.",
        nodeDetails = "Checks the alarm status to match one of the specified statuses.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckAlarmStatusConfig")
public class TbCheckAlarmStatusNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbCheckAlarmStatusNodeConfig config;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `tbContext`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckAlarmStatusNodeConfig.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
            Objects.requireNonNull(alarm, "alarm is null");
            ListenableFuture<Alarm> latest = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), alarm.getId());

            Futures.addCallback(latest, new FutureCallback<>() {
                /**
                 * 功能：处理`on Success`。
                 * 参数：
                 * - `result`：`result` 参数。
                 * 返回：无。
                 */
                @Override
                public void onSuccess(@Nullable Alarm result) {
                    if (result == null) {
                        ctx.tellFailure(msg, new TbNodeException("No such alarm found."));
                        return;
                    }
                    boolean isPresent = config.getAlarmStatusList().stream()
                            .anyMatch(alarmStatus -> result.getStatus() == alarmStatus);
                    ctx.tellNext(msg, isPresent ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
                }

                /**
                 * 功能：处理失败信息。
                 * 参数：
                 * - `t`：`t` 参数。
                 * 返回：无。
                 */
                @Override
                public void onFailure(Throwable t) {
                    ctx.tellFailure(msg, t);
                }
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
                log.debug("[{}][{}] Failed to parse alarm: [{}] error [{}]", ctx.getTenantId(), ctx.getRuleChainName(), msg.getData(), e.getMessage());
            } else {
                log.error("[{}][{}] Failed to parse alarm: [{}]", ctx.getTenantId(), ctx.getRuleChainName(), msg.getData(), e);
            }
            throw new TbNodeException(e);
        }
    }

    /*
     * 本类总结：`TbCheckAlarmStatusNode` 负责根据消息类型、实体类型、关系、脚本或告警状态判断消息路由；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
