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

/**
 * 中文说明：
 * 1. `TbPubSubAdmin` 是 ThingsBoard Common Queue 中围绕 `Tb Pub Sub` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueAdmin`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbPubSubAdmin implements TbQueueAdmin {
    /**
     * `ACK_DEADLINE`常量，用于统一引用固定值。
     */
    private static final String ACK_DEADLINE = "ackDeadlineInSec";
    private static final String MESSAGE_RETENTION = "messageRetentionInSec";

    /**
     * 主题，用于发起外部调用或协议交互。
     */
    private final TopicAdminClient topicAdminClient;
    private final SubscriptionAdminClient subscriptionAdminClient;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbPubSubSettings pubSubSettings;
    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();
    private final Set<String> subscriptionSet = ConcurrentHashMap.newKeySet();
    /**
     * 订阅映射关系，用于按键查找对应值。
     */
    private final Map<String, String> subscriptionProperties;

    /**
     * 功能：创建 `TbPubSubAdmin` 实例，并初始化必要字段。
     * 参数：
     * - `pubSubSettings`：配置对象。
     * - `subscriptionSettings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public TbPubSubAdmin(TbPubSubSettings pubSubSettings, Map<String, String> subscriptionSettings) {
        this.pubSubSettings = pubSubSettings;
        this.subscriptionProperties = subscriptionSettings;

        TopicAdminSettings topicAdminSettings;
        try {
            topicAdminSettings = TopicAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create TopicAdminSettings");
            throw new RuntimeException("Failed to create TopicAdminSettings.");
        }

        SubscriptionAdminSettings subscriptionAdminSettings;
        try {
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create SubscriptionAdminSettings");
            throw new RuntimeException("Failed to create SubscriptionAdminSettings.");
        }

        try {
            topicAdminClient = TopicAdminClient.create(topicAdminSettings);

            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            for (Topic topic : response.iterateAll()) {
                topicSet.add(topic.getName());
            }
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

            for (Subscription subscription : response.iterateAll()) {
                subscriptionSet.add(subscription.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get subscriptions.", e);
            throw new RuntimeException("Failed to get subscriptions.", e);
        }
    }

    /**
     * 功能：保存或创建主题。
     * 参数：
     * - `partition`：分区标识或分区信息。
     * - `properties`：`properties` 参数。
     * 返回：无。
     */
    @Override
    public void createTopicIfNotExists(String partition, String properties) {
        TopicName topicName = TopicName.newBuilder()
                .setTopic(partition)
                .setProject(pubSubSettings.getProjectId())
                .build();

        if (topicSet.contains(topicName.toString())) {
            createSubscriptionIfNotExists(partition, topicName);
            return;
        }

        ListTopicsRequest listTopicsRequest =
                ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
        TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
        for (Topic topic : response.iterateAll()) {
            if (topic.getName().contains(topicName.toString())) {
                topicSet.add(topic.getName());
                createSubscriptionIfNotExists(partition, topicName);
                return;
            }
        }

        try {
            topicAdminClient.createTopic(topicName);
            log.info("Created new topic: [{}]", topicName.toString());
        } catch (AlreadyExistsException e) {
            log.info("[{}] Topic already exist.", topicName.toString());
        } finally {
            topicSet.add(topicName.toString());
        }
        createSubscriptionIfNotExists(partition, topicName);
    }

    /**
     * 功能：删除或清理主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    public void deleteTopic(String topic) {
        TopicName topicName = TopicName.newBuilder()
                .setTopic(topic)
                .setProject(pubSubSettings.getProjectId())
                .build();

        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), topic);

        if (topicSet.contains(topicName.toString())) {
            topicAdminClient.deleteTopic(topicName);
        } else {
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
     * 功能：保存或创建订阅。
     * 参数：
     * - `partition`：分区标识或分区信息。
     * - `topicName`：主题名称或主题对象。
     * 返回：无。
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
     * 功能：更新`Ack Deadline`。
     * 参数：
     * - `builder`：`builder` 参数。
     * 返回：无。
     */
    private void setAckDeadline(Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(ACK_DEADLINE)) {
            builder.setAckDeadlineSeconds(Integer.parseInt(subscriptionProperties.get(ACK_DEADLINE)));
        }
    }

    /**
     * 功能：更新消息。
     * 参数：
     * - `builder`：`builder` 参数。
     * 返回：无。
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

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (topicAdminClient != null) {
            topicAdminClient.close();
        }
        if (subscriptionAdminClient != null) {
            subscriptionAdminClient.close();
        }
    }
}
