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
package org.thingsboard.rule.engine.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.Properties;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * `TbSendEmailNode` 类，封装当前模块中的一组相关职责。
 */
@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send email",
        configClazz = TbSendEmailNodeConfiguration.class,
        nodeDescription = "Sends email message via SMTP server.",
        nodeDetails = "Expects messages with <b>SEND_EMAIL</b> type. Node works only with messages that " +
                " where created using <code>to Email</code> transformation Node, please connect this Node " +
                "with <code>to Email</code> Node using <code>Successful</code> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeSendEmailConfig",
        icon = "send"
)
public class TbSendEmailNode extends TbAbstractExternalNode {

    /**
     * `MAIL_PROP`常量，用于统一引用固定值。
     */
    private static final String MAIL_PROP = "mail.";
    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbSendEmailNodeConfiguration config;
    /**
     * `mailSender` 字段，保存当前对象的对应属性。
     */
    private JavaMailSenderImpl mailSender;

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
        this.config = TbNodeUtils.convert(configuration, TbSendEmailNodeConfiguration.class);
        try {
            if (!this.config.isUseSystemSmtpSettings()) {
                mailSender = createMailSender();
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
        try {
            validateType(msg);
            TbEmail email = getEmail(msg);
            var tbMsg = ackIfNeeded(ctx, msg);
            withCallback(ctx.getMailExecutor().executeAsync(() -> {
                        sendEmail(ctx, tbMsg, email);
                        return null;
                    }),
                    ok -> tellSuccess(ctx, tbMsg),
                    fail -> tellFailure(ctx, tbMsg, fail));
        } catch (Exception ex) {
            ctx.tellFailure(msg, ex);
        }
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `email`：`email` 参数。
     * 返回：无。
     */
    private void sendEmail(TbContext ctx, TbMsg msg, TbEmail email) throws Exception {
        if (this.config.isUseSystemSmtpSettings()) {
            ctx.getMailService(true).send(ctx.getTenantId(), msg.getCustomerId(), email);
        } else {
            ctx.getMailService(false).send(ctx.getTenantId(), msg.getCustomerId(), email, this.mailSender, config.getTimeout());
        }
    }

    /**
     * 功能：获取邮箱。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private TbEmail getEmail(TbMsg msg) throws IOException {
        TbEmail email = JacksonUtil.fromString(msg.getData(), TbEmail.class);
        if (StringUtils.isBlank(email.getTo())) {
            throw new IllegalStateException("Email destination can not be blank [" + email.getTo() + "]");
        }
        return email;
    }

    /**
     * 功能：校验类型。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void validateType(TbMsg msg) {
        if (!msg.isTypeOf(TbMsgType.SEND_EMAIL)) {
            String type = msg.getType();
            log.warn("Not expected msg type [{}] for SendEmail Node", type);
            throw new IllegalStateException("Not expected msg type " + type + " for SendEmail Node");
        }
    }

    /**
     * 功能：保存或创建`Mail Sender`。
     * 参数：无。
     * 返回：处理结果。
     */
    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(this.config.getSmtpHost());
        mailSender.setPort(this.config.getSmtpPort());
        mailSender.setUsername(this.config.getUsername());
        mailSender.setPassword(this.config.getPassword());
        mailSender.setJavaMailProperties(createJavaMailProperties());
        return mailSender;
    }

    /**
     * 功能：保存或创建`Java Mail Properties`。
     * 参数：无。
     * 返回：处理结果。
     */
    private Properties createJavaMailProperties() {
        Properties javaMailProperties = new Properties();
        String protocol = this.config.getSmtpProtocol();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", this.config.getSmtpHost());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", this.config.getSmtpPort() + "");
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", this.config.getTimeout() + "");
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(this.config.getUsername())));
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", Boolean.valueOf(this.config.isEnableTls()).toString());
        if (this.config.isEnableTls() && StringUtils.isNoneEmpty(this.config.getTlsVersion())) {
            javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", this.config.getTlsVersion());
        }
        if (this.config.isEnableProxy()) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", config.getProxyHost());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", config.getProxyPort());
            if (StringUtils.isNoneEmpty(config.getProxyUser())) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", config.getProxyUser());
            }
            if (StringUtils.isNoneEmpty(config.getProxyPassword())) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", config.getProxyPassword());
            }
        }
        return javaMailProperties;
    }
}

/*
 * 本类总结：
 * 本类是邮件发送外部节点，负责校验 SEND_EMAIL 消息并通过系统 MailService 或自定义 JavaMailSender 发送邮件。
 * 发送在 mailExecutor 中异步执行，ackIfNeeded 先处理消息确认，回调决定成功或失败路由。
 * 本类本身不直接访问数据库或缓存；系统邮件服务和全局 SMTP 设置的具体实现/调用链可能间接涉及。
 */
