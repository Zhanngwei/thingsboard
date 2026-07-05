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
package org.thingsboard.rule.engine.sms;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * `TbSendSmsNode` 类，封装当前模块中的一组相关职责。
 */
@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send sms",
        configClazz = TbSendSmsNodeConfiguration.class,
        nodeDescription = "Sends SMS message via SMS provider.",
        nodeDetails = "Will send SMS message by populating target phone numbers and sms message fields using values derived from message metadata.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeSendSmsConfig",
        icon = "sms"
)
public class TbSendSmsNode extends TbAbstractExternalNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbSendSmsNodeConfiguration config;
    /**
     * `smsSender` 字段，保存当前对象的对应属性。
     */
    private SmsSender smsSender;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        try {
            this.config = TbNodeUtils.convert(configuration, TbSendSmsNodeConfiguration.class);
            if (!this.config.isUseSystemSmsSettings()) {
                smsSender = createSmsSender(ctx);
            }
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
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
        var tbMsg = ackIfNeeded(ctx, msg);
        try {
            withCallback(ctx.getSmsExecutor().executeAsync(() -> {
                        sendSms(ctx, tbMsg);
                        return null;
                    }),
                    ok -> tellSuccess(ctx, tbMsg),
                    fail -> tellFailure(ctx, tbMsg, fail));
        } catch (Exception ex) {
            ctx.tellFailure(tbMsg, ex);
        }
    }

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void sendSms(TbContext ctx, TbMsg msg) throws Exception {
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersToTemplate(), msg);
        String message = TbNodeUtils.processPattern(this.config.getSmsMessageTemplate(), msg);
        String[] numbersToList = numbersTo.split(",");
        if (this.config.isUseSystemSmsSettings()) {
            ctx.getSmsService().sendSms(ctx.getTenantId(), msg.getCustomerId(), numbersToList, message);
        } else {
            for (String numberTo : numbersToList) {
                this.smsSender.sendSms(numberTo, message);
            }
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (this.smsSender != null) {
            this.smsSender.destroy();
        }
    }

    /**
     * 功能：保存或创建`Sms Sender`。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    private SmsSender createSmsSender(TbContext ctx) {
        return ctx.getSmsSenderFactory().createSmsSender(this.config.getSmsProviderConfiguration());
    }

}

/*
 * 本类总结：
 * 本类是短信外部发送节点，负责模板解析、SMS 发送器生命周期和异步发送回调。
 * 消息确认在发送前由 ackIfNeeded 处理，smsExecutor 回调决定成功或失败路由。
 * 本类本身不直接涉及数据库或缓存；系统 SMS 服务、provider 工厂或具体 SmsSender 的实现/调用链可能间接涉及。
 */
