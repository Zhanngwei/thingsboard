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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
/**
 * 邮件消息转换节点配置，保存 TbEmail 各字段的模板。
 * 配置类本身不直接发送邮件、不访问数据库/缓存，也不处理异步回调。
 */
public class TbMsgToEmailNodeConfiguration implements NodeConfiguration<TbMsgToEmailNodeConfiguration> {

    /**
     * 发件人模板。
     */
    private String fromTemplate;
    /**
     * 收件人模板。
     */
    private String toTemplate;
    /**
     * 抄送人模板。
     */
    private String ccTemplate;
    /**
     * 密送人模板。
     */
    private String bccTemplate;
    /**
     * 邮件主题模板。
     */
    private String subjectTemplate;
    /**
     * 邮件正文模板。
     */
    private String bodyTemplate;
    /**
     * 动态 HTML 标志模板，仅在 mailBodyType 为 dynamic 时使用。
     */
    private String isHtmlTemplate;
    /**
     * 邮件正文类型配置，false 表示纯文本，true 表示 HTML，dynamic 表示从 isHtmlTemplate 解析。
     */
    private String mailBodyType; // Plain Text -> false. HTML - true. Dynamic - value used from isHtmlTemplate.

    /**
     * 构造邮件转换节点默认配置。
     * 本方法只设置模板默认值，不直接发送邮件或处理 Rule Engine 消息确认。
     */
    @Override
    public TbMsgToEmailNodeConfiguration defaultConfiguration() {
        var configuration = new TbMsgToEmailNodeConfiguration();
        configuration.setFromTemplate("info@testmail.org");
        configuration.setToTemplate("${userEmail}");
        configuration.setSubjectTemplate("Device ${deviceType} temperature high");
        configuration.setBodyTemplate("Device ${deviceName} has high temperature $[temperature]");
        configuration.setMailBodyType("false");
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类描述 to email 转换节点的模板配置，实际消息转换在 TbMsgToEmailNode 中完成。
 * 它不直接涉及外部调用、异步回调、数据库、缓存或 SMTP 连接。
 */
