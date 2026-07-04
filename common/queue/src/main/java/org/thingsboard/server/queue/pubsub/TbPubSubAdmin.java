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
package org.thingsboard.server.queue.pubsub;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`TbPubSubAdmin` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbPubSubAdmin implements TbQueueAdmin {
    /**
     * 字段说明：
     * 1. 保存 `ACK_DEADLINE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String ACK_DEADLINE = "ackDeadlineInSec";
    private static final String MESSAGE_RETENTION = "messageRetentionInSec";

    /**
     * 字段说明：
     * 1. 保存 `topicAdminClient` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TopicAdminClient topicAdminClient;
    private final SubscriptionAdminClient subscriptionAdminClient;

    /**
     * 字段说明：
     * 1. 保存 `pubSubSettings` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbPubSubSettings pubSubSettings;
    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();
    private final Set<String> subscriptionSet = ConcurrentHashMap.newKeySet();
    /**
     * 字段说明：
     * 1. 保存 `subscriptionProperties` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final Map<String, String> subscriptionProperties;

    /**
     * 方法说明：
     * 1. 职责：执行 `TbPubSubAdmin` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbPubSubAdmin(TbPubSubSettings pubSubSettings, Map<String, String> subscriptionSettings) {
        this.pubSubSettings = pubSubSettings;
        this.subscriptionProperties = subscriptionSettings;

        TopicAdminSettings topicAdminSettings;
        try {
            topicAdminSettings = TopicAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IOException e) {
            log.error("Failed to create TopicAdminSettings");
            throw new RuntimeException("Failed to create TopicAdminSettings.");
        }

        SubscriptionAdminSettings subscriptionAdminSettings;
        try {
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IOException e) {
            log.error("Failed to create SubscriptionAdminSettings");
            throw new RuntimeException("Failed to create SubscriptionAdminSettings.");
        }

        try {
            topicAdminClient = TopicAdminClient.create(topicAdminSettings);

            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (Topic topic : response.iterateAll()) {
                topicSet.add(topic.getName());
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IOException e) {
            log.error("Failed to get topics.", e);
            throw new RuntimeException("Failed to get topics.", e);
        }

        try {
            subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);

            ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (Subscription subscription : response.iterateAll()) {
                subscriptionSet.add(subscription.getName());
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IOException e) {
            log.error("Failed to get subscriptions.", e);
            throw new RuntimeException("Failed to get subscriptions.", e);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createTopicIfNotExists` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createTopicIfNotExists(String partition, String properties) {
        TopicName topicName = TopicName.newBuilder()
                .setTopic(partition)
                .setProject(pubSubSettings.getProjectId())
                .build();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (topicSet.contains(topicName.toString())) {
            createSubscriptionIfNotExists(partition, topicName);
            return;
        }

        ListTopicsRequest listTopicsRequest =
                ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
        TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Topic topic : response.iterateAll()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (topic.getName().contains(topicName.toString())) {
                topicSet.add(topic.getName());
                createSubscriptionIfNotExists(partition, topicName);
                return;
            }
        }

        try {
            topicAdminClient.createTopic(topicName);
            log.info("Created new topic: [{}]", topicName.toString());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (AlreadyExistsException e) {
            log.info("[{}] Topic already exist.", topicName.toString());
        } finally {
            topicSet.add(topicName.toString());
        }
        createSubscriptionIfNotExists(partition, topicName);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTopic` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteTopic(String topic) {
        TopicName topicName = TopicName.newBuilder()
                .setTopic(topic)
                .setProject(pubSubSettings.getProjectId())
                .build();

        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), topic);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (topicSet.contains(topicName.toString())) {
            topicAdminClient.deleteTopic(topicName);
        } else {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (topicAdminClient.getTopic(topicName) != null) {
                topicAdminClient.deleteTopic(topicName);
            } else {
                log.warn("PubSub topic [{}] does not exist.", topic);
            }
        }

        if (subscriptionSet.contains(subscriptionName.toString())) {
            subscriptionAdminClient.deleteSubscription(subscriptionName);
        } else {
            if (subscriptionAdminClient.getSubscription(subscriptionName) != null) {
                subscriptionAdminClient.deleteSubscription(subscriptionName);
            } else {
                log.warn("PubSub subscription [{}] does not exist.", topic);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSubscriptionIfNotExists` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void createSubscriptionIfNotExists(String partition, TopicName topicName) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), partition);

        if (subscriptionSet.contains(subscriptionName.toString())) {
            return;
        }

        ListSubscriptionsRequest listSubscriptionsRequest =
                ListSubscriptionsRequest.newBuilder().setProject(ProjectName.of(pubSubSettings.getProjectId()).toString()).build();
        SubscriptionAdminClient.ListSubscriptionsPagedResponse response = subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);
        for (Subscription subscription : response.iterateAll()) {
            if (subscription.getName().equals(subscriptionName.toString())) {
                subscriptionSet.add(subscription.getName());
                return;
            }
        }

        Subscription.Builder subscriptionBuilder = Subscription
                .newBuilder()
                .setName(subscriptionName.toString())
                .setTopic(topicName.toString());

        setAckDeadline(subscriptionBuilder);
        setMessageRetention(subscriptionBuilder);

        try {
            subscriptionAdminClient.createSubscription(subscriptionBuilder.build());
            log.info("Created new subscription: [{}]", subscriptionName.toString());
        } catch (AlreadyExistsException e) {
            log.info("[{}] Subscription already exist.", subscriptionName.toString());
        } finally {
            subscriptionSet.add(subscriptionName.toString());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setAckDeadline` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setAckDeadline(Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(ACK_DEADLINE)) {
            builder.setAckDeadlineSeconds(Integer.parseInt(subscriptionProperties.get(ACK_DEADLINE)));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setMessageRetention` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setMessageRetention(Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(MESSAGE_RETENTION)) {
            Duration duration = Duration
                    .newBuilder()
                    .setSeconds(Long.parseLong(subscriptionProperties.get(MESSAGE_RETENTION)))
                    .build();
            builder.setMessageRetentionDuration(duration);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `destroy` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void destroy() {
        if (topicAdminClient != null) {
            topicAdminClient.close();
        }
        if (subscriptionAdminClient != null) {
            subscriptionAdminClient.close();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbPubSubAdmin` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
