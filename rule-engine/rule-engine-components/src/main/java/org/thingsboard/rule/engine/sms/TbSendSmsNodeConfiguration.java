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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;

/**
 * `TbSendSmsNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbSendSmsNodeConfiguration implements NodeConfiguration {

    /**
     * `numbersToTemplate` 字段，保存当前对象的对应属性。
     */
    private String numbersToTemplate;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private String smsMessageTemplate;
    /**
     * 是否使用配置。
     */
    private boolean useSystemSmsSettings;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private SmsProviderConfiguration smsProviderConfiguration;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NodeConfiguration defaultConfiguration() {
        TbSendSmsNodeConfiguration configuration = new TbSendSmsNodeConfiguration();
        configuration.numbersToTemplate = "${userPhone}";
        configuration.smsMessageTemplate = "Device ${deviceName} has high temperature ${temp}";
        configuration.setUseSystemSmsSettings(true);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类描述 SMS 发送节点的模板和 provider 选择配置。
 * 实际发送、异步执行和失败路由由 TbSendSmsNode 完成；本类不直接涉及数据库、缓存或外部调用。
 */
