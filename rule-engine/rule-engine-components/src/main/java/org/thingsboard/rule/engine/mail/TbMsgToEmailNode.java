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

/**
 * `TbMsgToEmailNode` 类，封装当前模块中的一组相关职责。
 */
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
public class TbMsgToEmailNode implements TbNode {

    /**
     * `IMAGES`常量，用于统一引用固定值。
     */
    private static final String IMAGES = "images";
    /**
     * `DYNAMIC`常量，用于统一引用固定值。
     */
    private static final String DYNAMIC = "dynamic";

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgToEmailNodeConfiguration config;
    /**
     * 是否满足类型条件。
     */
    private boolean dynamicMailBodyType;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgToEmailNodeConfiguration.class);
        this.dynamicMailBodyType = DYNAMIC.equals(this.config.getMailBodyType());
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
            TbEmail email = convert(msg);
            TbMsg emailMsg = buildEmailMsg(ctx, msg, email);
            ctx.tellNext(emailMsg, TbNodeConnectionType.SUCCESS);
        } catch (Exception ex) {
            log.warn("Can not convert message to email " + ex.getMessage());
            ctx.tellFailure(msg, ex);
        }
    }

    /**
     * 功能：构建消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    private TbMsg buildEmailMsg(TbContext ctx, TbMsg msg, TbEmail email) {
        String emailJson = JacksonUtil.toString(email);
        return ctx.transformMsg(msg, TbMsgType.SEND_EMAIL, msg.getOriginator(), msg.getMetaData().copy(), emailJson);
    }

    /**
     * 功能：执行 `convert` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
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
            Map<String, String> imgMap = JacksonUtil.fromString(imagesStr, new TypeReference<HashMap<String, String>>() {});
            builder.images(imgMap);
        }
        return builder.build();
    }

    /**
     * 功能：执行 `fromTemplate` 对应的处理。
     * 参数：
     * - `template`：`template` 参数。
     * - `msg`：待处理消息。
     * 返回：文本结果。
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
