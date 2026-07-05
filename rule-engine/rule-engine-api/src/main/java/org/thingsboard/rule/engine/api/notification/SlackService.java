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
package org.thingsboard.rule.engine.api.notification;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;

import java.util.List;

/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine 通知节点访问 Slack 的最小服务契约。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的通知集成子模块。
 * 3. 协作对象：与 Slack 通知节点、租户通知目标、Slack token 配置和外部 Slack API 协作。
 * 4. 生命周期：由 Spring 实现类长期存在，通知节点或配置界面在发送消息或列出会话时调用。
 * 5. 设计原因：Slack API 访问需要隔离 token 获取、会话查询和消息发送，避免规则节点直接依赖 Slack SDK/HTTP 细节。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现可能读取 token 配置并调用外部 Slack HTTP API，直接服务 Rule Engine 通知流程。
 */
public interface SlackService {

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `token`：`token` 参数。
     * - `conversationId`：`conversationId`ID。
     * - `message`：待处理消息。
     * 返回：无。
     */
    void sendMessage(TenantId tenantId, String token, String conversationId, String message);

    /**
     * 功能：获取`Conversations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `token`：`token` 参数。
     * - `conversationType`：类型。
     * 返回：匹配的数据集合。
     */
    List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversationType conversationType);

    /**
     * 功能：获取令牌。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：文本结果。
     */
    String getToken(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：为 Rule Engine Slack 通知节点提供 token、会话和消息发送接口。
 * 2. 核心流程：节点获取 token，查询或选择会话，然后调用 sendMessage 投递 Slack 消息。
 * 3. 关键依赖：TenantId、SlackConversation、SlackConversationType 和外部 Slack API 实现。
 * 4. 学习重点：外部通知节点通过服务接口隔离第三方 API 细节，保持规则节点职责集中。
 */
