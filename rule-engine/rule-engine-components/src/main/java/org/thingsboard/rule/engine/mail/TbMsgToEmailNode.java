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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "to email",
        configClazz = TbMsgToEmailNodeConfiguration.class,
        nodeDescription = "Transforms message to email message",
        nodeDetails = "Transforms message to email message. If transformation completed successfully output message type will be set to <code>SEND_EMAIL</code>.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeToEmailConfig",
        icon = "email"
)
/**
 * 将普通 Rule Engine 消息转换为 SEND_EMAIL 消息的转换节点。
 * 本类本身不直接调用 SMTP 或外部服务，也不直接访问数据库/缓存；后续真正发送由 send email 节点完成。
 */
public class TbMsgToEmailNode implements TbNode {

    /**
     * 邮件内嵌图片元数据键名。
     */
    private static final String IMAGES = "images";
    /**
     * 表示邮件正文类型需要从模板动态解析的配置值。
     */
    private static final String DYNAMIC = "dynamic";

    /**
     * 邮件转换配置，包含发件人、收件人、主题、正文和 HTML 标志模板。
     */
    private TbMsgToEmailNodeConfiguration config;
    /**
     * 是否从 isHtmlTemplate 动态解析 HTML 标志。
     */
    private boolean dynamicMailBodyType;

    /**
     * 初始化邮件转换配置。
     * 本方法不直接涉及外部调用、事务、数据库、缓存、MQTT 或 Actor；仅保存本节点配置。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgToEmailNodeConfiguration.class);
        this.dynamicMailBodyType = DYNAMIC.equals(this.config.getMailBodyType());
     }

    /**
     * 将输入消息转换为 SEND_EMAIL 类型并路由到 Success。
     * 转换失败时走 Failure；本方法不直接发送邮件，也不直接处理 SMTP 连接生命周期。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            TbEmail email = convert(msg);
            TbMsg emailMsg = buildEmailMsg(ctx, msg, email);
            ctx.tellNext(emailMsg, TbNodeConnectionType.SUCCESS);
        } catch (Exception ex) {
            log.warn("Can not convert message to email " + ex.getMessage());
            ctx.tellFailure(msg, ex);
        }
    }

    /**
     * 构造新的 SEND_EMAIL 类型 TbMsg。
     * 本方法只转换本地消息内容，不直接访问外部系统、数据库或缓存。
     */
    private TbMsg buildEmailMsg(TbContext ctx, TbMsg msg, TbEmail email) {
        String emailJson = JacksonUtil.toString(email);
        return ctx.transformMsg(msg, TbMsgType.SEND_EMAIL, msg.getOriginator(), msg.getMetaData().copy(), emailJson);
    }

    /**
     * 根据模板和消息元数据/数据构造 TbEmail 对象。
     * 图片映射来自消息元数据 images；本方法不直接调用邮件服务。
     */
    private TbEmail convert(TbMsg msg) {
        TbEmail.TbEmailBuilder builder = TbEmail.builder();
        builder.from(fromTemplate(config.getFromTemplate(), msg));
        builder.to(fromTemplate(config.getToTemplate(), msg));
        builder.cc(fromTemplate(config.getCcTemplate(), msg));
        builder.bcc(fromTemplate(config.getBccTemplate(), msg));
        String htmlStr = dynamicMailBodyType ?
                fromTemplate(config.getIsHtmlTemplate(), msg) : config.getMailBodyType();
        builder.html(Boolean.parseBoolean(htmlStr));
        builder.subject(fromTemplate(config.getSubjectTemplate(), msg));
        builder.body(fromTemplate(config.getBodyTemplate(), msg));
        String imagesStr = msg.getMetaData().getValue(IMAGES);
        if (!StringUtils.isEmpty(imagesStr)) {
            // images 元数据保存为 JSON map，转换时解析为 TbEmail 的内嵌图片映射。
            Map<String, String> imgMap = JacksonUtil.fromString(imagesStr, new TypeReference<HashMap<String, String>>() {});
            builder.images(imgMap);
        }
        return builder.build();
    }

    /**
     * 对单个模板执行 Rule Engine 模板解析。
     * 本方法只读取当前 TbMsg，不直接访问数据库、缓存或外部系统。
     */
    private String fromTemplate(String template, TbMsg msg) {
        return StringUtils.isNotEmpty(template) ? TbNodeUtils.processPattern(template, msg) : null;
    }

}

/*
 * 本类总结：
 * 本类只负责把 Rule Engine 消息转换为邮件发送节点可识别的 SEND_EMAIL 消息。
 * 它不直接调用 SMTP、MailService、数据库或缓存；失败路由只覆盖本地模板解析和 JSON 转换异常。
 */
