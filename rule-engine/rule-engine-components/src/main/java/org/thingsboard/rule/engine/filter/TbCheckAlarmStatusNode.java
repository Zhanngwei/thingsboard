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
/**
 * 中文说明：`TbCheckAlarmStatusNode` 是检查告警状态节点规则节点，用于根据消息类型、实体类型、关系、脚本或告警状态判断消息路由。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbCheckAlarmStatusNodeConfig`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbCheckAlarmStatusNode implements TbNode {

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    private TbCheckAlarmStatusNodeConfig config;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckAlarmStatusNodeConfig.class);
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
            Objects.requireNonNull(alarm, "alarm is null");
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
            ListenableFuture<Alarm> latest = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), alarm.getId());

            Futures.addCallback(latest, new FutureCallback<>() {
                @Override
                /**
                 * 方法说明：处理异步调用成功回调并继续规则链投递。
                 * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
                 */
                public void onSuccess(@Nullable Alarm result) {
                    if (result == null) {
                        ctx.tellFailure(msg, new TbNodeException("No such alarm found."));
                        return;
                    }
                    boolean isPresent = config.getAlarmStatusList().stream()
                            .anyMatch(alarmStatus -> result.getStatus() == alarmStatus);
                    ctx.tellNext(msg, isPresent ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
                }

                @Override
                /**
                 * 方法说明：处理异步调用失败回调并转入失败关系。
                 * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
                 */
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
