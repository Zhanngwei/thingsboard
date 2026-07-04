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
package org.thingsboard.rule.engine.notification;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
/**
 * Slack 发送节点配置，保存 bot token、系统设置开关、消息模板和目标会话。
 * 配置类本身不直接调用 Slack API、不访问数据库或缓存，也不涉及异步回调。
 */
public class TbSlackNodeConfiguration implements NodeConfiguration<TbSlackNodeConfiguration> {

    /**
     * 自定义 Slack bot token。
     */
    private String botToken;
    /**
     * 是否使用系统级 Slack 设置中的 token。
     */
    private boolean useSystemSettings;
    /**
     * Slack 消息模板。
     */
    @NotEmpty
    private String messageTemplate;

    /**
     * Slack 会话类型。
     */
    private SlackConversationType conversationType;
    /**
     * Slack 目标会话。
     */
    @NotNull
    @Valid
    private SlackConversation conversation;

    /**
     * 构造 Slack 节点默认配置。
     * 本方法只设置默认值，不直接读取系统 token 或调用 Slack API。
     */
    @Override
    public TbSlackNodeConfiguration defaultConfiguration() {
        TbSlackNodeConfiguration config = new TbSlackNodeConfiguration();
        config.setUseSystemSettings(true);
        config.setBotToken("xoxb-");
        config.setMessageTemplate("Device ${deviceId}: temperature is $[temperature]");
        config.setConversationType(SlackConversationType.PUBLIC_CHANNEL);
        return config;
    }

}

/*
 * 本类总结：
 * 本类描述 Slack 外部发送节点的 token 来源、消息模板和目标会话配置。
 * 实际异步发送、失败路由和系统设置读取由 TbSlackNode/SlackService 完成；本类不直接涉及数据库或缓存。
 */
