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

@Data
/**
 * SMS 发送节点配置模型，保存手机号模板、短信正文模板、系统设置开关和自定义 provider 配置。
 * 配置类本身不直接发送短信、不访问数据库或缓存，也不涉及异步回调。
 */
public class TbSendSmsNodeConfiguration implements NodeConfiguration {

    /**
     * 目标手机号模板，支持从 TbMsg 中解析，多个号码以逗号分隔。
     */
    private String numbersToTemplate;
    /**
     * 短信正文模板。
     */
    private String smsMessageTemplate;
    /**
     * 是否使用系统级 SMS 设置。
     */
    private boolean useSystemSmsSettings;
    /**
     * 自定义 SMS provider 配置。
     */
    private SmsProviderConfiguration smsProviderConfiguration;

    /**
     * 构造 SMS 节点默认配置。
     * 本方法只设置默认模板和系统设置开关，不直接创建 SmsSender 或发送短信。
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
