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

import java.util.Map;

/**
 * 中文说明：
 * 1. 职责：定义通过 Firebase Cloud Messaging 发送移动端推送消息的服务契约。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的通知集成子模块。
 * 3. 协作对象：与通知节点、Firebase 凭据配置、租户通知目标和外部 FCM API 协作。
 * 4. 生命周期：由 Spring 实现类长期存在，通知流程或规则节点在需要移动推送时调用。
 * 5. 设计原因：Firebase 调用涉及凭据、token、payload 和外部 HTTP/API 细节，规则节点通过接口保持解耦。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现可能读取凭据配置并调用外部 FCM，直接服务 Rule Engine 通知流程。
 */
public interface FirebaseService {

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `credentials`：`credentials` 参数。
     * - `fcmToken`：`fcmToken` 参数。
     * - `title`：`title` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void sendMessage(TenantId tenantId, String credentials, String fcmToken, String title, String body, Map<String, String> data, Integer badge) throws Exception;

}

/*
 * 本类总结：
 * 1. 核心职责：为 Rule Engine 通知流程抽象 Firebase 推送能力。
 * 2. 核心流程：调用方提供凭据、token 和消息载荷，服务实现调用外部 FCM API。
 * 3. 关键依赖：TenantId、Firebase 凭据、FCM token 和通知节点实现。
 * 4. 学习重点：移动推送是外部通知能力，通过接口隔离第三方 API 细节。
 */
