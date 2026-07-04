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
package org.thingsboard.server.service.notification.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiRequest;
import com.slack.api.methods.SlackApiTextResponse;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.ConversationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.dao.notification.NotificationSettingsService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultSlackService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultSlackService implements SlackService {

    /**
     * 字段说明：
     * 1. 保存 `notificationSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationSettingsService notificationSettingsService;

    private final Slack slack = Slack.getInstance();
    private final Cache<String, List<SlackConversation>> cache = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();
    /**
     * 字段说明：
     * 1. 保存 `CONVERSATIONS_LOAD_LIMIT` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int CONVERSATIONS_LOAD_LIMIT = 1000;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendMessage` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendMessage(TenantId tenantId, String token, String conversationId, String message) {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(conversationId)
                .text(message)
                .build();
        sendRequest(token, request, MethodsClient::chatPostMessage);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `listConversations` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversationType conversationType) {
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        return cache.get(conversationType + ":" + token, k -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (conversationType == SlackConversationType.DIRECT) {
                UsersListRequest request = UsersListRequest.builder()
                        .limit(CONVERSATIONS_LOAD_LIMIT)
                        .build();

                UsersListResponse response = sendRequest(token, request, MethodsClient::usersList);
                return response.getMembers().stream()
                        .filter(user -> !user.isDeleted() && !user.isStranger() && !user.isBot())
                        .map(user -> {
                            SlackConversation conversation = new SlackConversation();
                            conversation.setType(conversationType);
                            conversation.setId(user.getId());
                            conversation.setName(user.getName());
                            conversation.setWholeName(user.getProfile() != null ? user.getProfile().getRealNameNormalized() : user.getRealName());
                            conversation.setEmail(user.getProfile() != null ? user.getProfile().getEmail() : null);
                            return conversation;
                        })
                        .collect(Collectors.toList());
            } else {
                ConversationsListRequest request = ConversationsListRequest.builder()
                        .types(List.of(conversationType == SlackConversationType.PUBLIC_CHANNEL ?
                                ConversationType.PUBLIC_CHANNEL :
                                ConversationType.PRIVATE_CHANNEL))
                        .limit(CONVERSATIONS_LOAD_LIMIT)
                        .excludeArchived(true)
                        .build();

                ConversationsListResponse response = sendRequest(token, request, MethodsClient::conversationsList);
                return response.getChannels().stream()
                        .filter(channel -> !channel.isArchived())
                        .map(channel -> {
                            SlackConversation conversation = new SlackConversation();
                            conversation.setType(conversationType);
                            conversation.setId(channel.getId());
                            conversation.setName(channel.getName());
                            conversation.setWholeName(channel.getNameNormalized());
                            return conversation;
                        })
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getToken` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getToken(TenantId tenantId) {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (slackConfig != null) {
            return slackConfig.getBotToken();
        } else {
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendRequest` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T extends SlackApiRequest, R extends SlackApiTextResponse> R sendRequest(String token, T request, ThrowingBiFunction<MethodsClient, T, R> method) {
        MethodsClient client = slack.methods(token);
        R response;
        try {
            response = method.apply(client, request);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!response.isOk()) {
            String error = response.getError();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (error == null) {
                error = "unknown error";
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            } else if (error.contains("missing_scope")) {
                String neededScope = response.getNeeded();
                error = "bot token scope '" + neededScope + "' is needed";
            }
            throw new RuntimeException("Slack API error: " + error);
        }

        return response;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultSlackService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
