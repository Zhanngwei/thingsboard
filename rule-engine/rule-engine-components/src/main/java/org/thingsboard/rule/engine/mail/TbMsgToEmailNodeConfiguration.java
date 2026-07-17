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
 * `TbMsgToEmailNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbMsgToEmailNodeConfiguration implements NodeConfiguration<TbMsgToEmailNodeConfiguration> {

    /**
     * `fromTemplate` 字段，保存当前对象的对应属性。
     */
    private String fromTemplate;
    /**
     * `toTemplate` 字段，保存当前对象的对应属性。
     */
    private String toTemplate;
    /**
     * `ccTemplate` 字段，保存当前对象的对应属性。
     */
    private String ccTemplate;
    /**
     * `bccTemplate` 字段，保存当前对象的对应属性。
     */
    private String bccTemplate;
    /**
     * `subjectTemplate` 字段，保存当前对象的对应属性。
     */
    private String subjectTemplate;
    /**
     * `bodyTemplate` 字段，保存当前对象的对应属性。
     */
    private String bodyTemplate;
    /**
     * 是否为`html template`。
     */
    private String isHtmlTemplate;
    /**
     * 类型，用于区分不同处理分支。
     */
    private String mailBodyType; // Plain Text -> false. HTML - true. Dynamic - value used from isHtmlTemplate.

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
