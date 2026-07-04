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
/**
 * SMS 外部发送节点，根据模板生成手机号和短信正文，并通过系统 SMS 服务或自定义 SmsSender 发送。
 * 本类不直接访问数据库或缓存；外部调用在 smsExecutor 中执行，withCallback 决定成功或失败路由。
 */
public class TbSendSmsNode extends TbAbstractExternalNode {

    /**
     * SMS 节点配置，包含手机号模板、短信正文模板和系统/自定义 provider 配置。
     */
    private TbSendSmsNodeConfiguration config;
    /**
     * 自定义 SMS provider 模式下使用的发送器。
     */
    private SmsSender smsSender;

    /**
     * 初始化 SMS 节点配置和可选自定义 SmsSender。
     * 本方法不直接发送短信；系统 SMS 服务或 provider 工厂的具体实现/调用链可能间接涉及缓存或外部配置。
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
     * 处理 Rule Engine 消息并异步发送短信。
     * 消息先经 ackIfNeeded 处理确认关系；发送成功走 Success，异常走 Failure。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        try {
            withCallback(ctx.getSmsExecutor().executeAsync(() -> {
                        // SMS 发送可能触达外部 provider，因此放入专用 smsExecutor。
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
     * 执行实际 SMS 发送外部调用。
     * 手机号和正文来自配置模板解析；系统模式委托 SmsService，自定义模式逐个调用 SmsSender。
     * 本方法本身不直接访问数据库或缓存，SmsService/provider 的具体实现/调用链可能间接涉及。
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
     * 销毁自定义 SmsSender。
     * 本方法直接结束 provider 发送器生命周期，不处理消息确认或失败路由。
     */
    @Override
    public void destroy() {
        if (this.smsSender != null) {
            this.smsSender.destroy();
        }
    }

    /**
     * 通过上下文中的工厂创建自定义 SmsSender。
     * 本方法不直接发送短信；provider 配置由节点配置提供。
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
