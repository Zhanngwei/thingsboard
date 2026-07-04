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
 * 邮件发送节点配置模型，保存系统 SMTP 开关、自定义 SMTP、TLS 和代理参数。
 * 配置类本身不直接发送邮件、不访问数据库或缓存，也不涉及异步回调。
 */
public class TbSendEmailNodeConfiguration implements NodeConfiguration {

    /**
     * 是否使用系统级 SMTP 设置。
     */
    private boolean useSystemSmtpSettings;
    /**
     * 自定义 SMTP 主机。
     */
    private String smtpHost;
    /**
     * 自定义 SMTP 端口。
     */
    private int smtpPort;
    /**
     * SMTP 用户名。
     */
    private String username;
    /**
     * SMTP 密码。
     */
    private String password;
    /**
     * SMTP 协议名称。
     */
    private String smtpProtocol;
    /**
     * SMTP 超时时间，单位毫秒。
     */
    private int timeout;
    /**
     * 是否启用 STARTTLS。
     */
    private boolean enableTls;
    /**
     * TLS 协议版本。
     */
    private String tlsVersion;
    /**
     * 是否启用 SMTP 代理。
     */
    private boolean enableProxy;
    /**
     * SMTP 代理主机。
     */
    private String proxyHost;
    /**
     * SMTP 代理端口。
     */
    private String proxyPort;
    /**
     * SMTP 代理用户名。
     */
    private String proxyUser;
    /**
     * SMTP 代理密码。
     */
    private String proxyPassword;

    /**
     * 构造邮件发送节点默认配置。
     * 本方法只设置默认值，不直接创建 JavaMailSender 或发送邮件。
     */
    @Override
    public TbSendEmailNodeConfiguration defaultConfiguration() {
        TbSendEmailNodeConfiguration configuration = new TbSendEmailNodeConfiguration();
        configuration.setUseSystemSmtpSettings(true);
        configuration.setSmtpHost("localhost");
        configuration.setSmtpProtocol("smtp");
        configuration.setSmtpPort(25);
        configuration.setTimeout(10000);
        configuration.setEnableTls(false);
        configuration.setTlsVersion("TLSv1.2");
        configuration.setEnableProxy(false);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类描述发送邮件节点的 SMTP、TLS、代理和系统配置开关。
 * 实际 MailService/SMTP 调用、异步回调和失败路由由 TbSendEmailNode 完成；本类不直接涉及数据库或缓存。
 */
