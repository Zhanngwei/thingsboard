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

/**
 * `TbSendEmailNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbSendEmailNodeConfiguration implements NodeConfiguration {

    /**
     * 是否使用配置。
     */
    private boolean useSystemSmtpSettings;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String smtpHost;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    private int smtpPort;
    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    private String password;
    /**
     * `smtpProtocol` 字段，保存当前对象的对应属性。
     */
    private String smtpProtocol;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private int timeout;
    /**
     * 是否启用`tls`。
     */
    private boolean enableTls;
    /**
     * 版本号，表示当前对象的对应属性。
     */
    private String tlsVersion;
    /**
     * 是否启用`proxy`。
     */
    private boolean enableProxy;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String proxyHost;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    private String proxyPort;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private String proxyUser;
    /**
     * 密码，用于认证或安全校验。
     */
    private String proxyPassword;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
