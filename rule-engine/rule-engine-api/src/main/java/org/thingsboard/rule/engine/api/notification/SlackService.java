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
     * 中文说明：
     * 1. 方法职责：向指定 Slack 会话发送消息。
     * 2. 输入参数：tenantId 是租户边界，token 是 Slack 访问令牌，conversationId 是会话 ID，message 是消息内容。
     * 3. 返回值：无；失败处理由实现决定。
     * 4. 调用时机：Slack 通知规则节点处理消息或通知中心触发 Slack 投递时调用。
     * 5. 调用方：Slack 通知节点、通知中心。
     * 6. 使用流程：属于 Rule Engine 外部通知发送流程。
     * 7. 线程安全：接口无状态，具体实现需保证 HTTP 客户端和 token 使用并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、MQTT、Actor、数据库；实现可能使用配置缓存并调用 Slack API。
     */
    void sendMessage(TenantId tenantId, String token, String conversationId, String message);

    /**
     * 中文说明：
     * 1. 方法职责：查询 Slack 会话列表。
     * 2. 输入参数：tenantId 是租户边界，token 是 Slack 访问令牌，conversationType 是会话类型。
     * 3. 返回值：SlackConversation 列表，用于配置界面选择目标会话。
     * 4. 调用时机：配置 Slack 通知目标或刷新会话列表时调用。
     * 5. 调用方：通知目标配置流程、Slack 通知节点辅助逻辑。
     * 6. 使用流程：属于外部 Slack 配置辅助流程。
     * 7. 线程安全：实现需保证外部 API 客户端并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现调用 Slack API，可能读取配置缓存。
     */
    List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversationType conversationType);

    /**
     * 中文说明：
     * 1. 方法职责：获取租户可用的 Slack token。
     * 2. 输入参数：tenantId 是租户边界。
     * 3. 返回值：Slack 访问令牌字符串。
     * 4. 调用时机：发送 Slack 消息或列出会话前调用。
     * 5. 调用方：Slack 通知节点、通知目标配置流程。
     * 6. 使用流程：属于 Rule Engine 通知前置配置读取流程。
     * 7. 线程安全：实现需保证 token 读取和缓存并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现可能访问数据库或缓存读取 token，直接服务 Rule Engine。
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
