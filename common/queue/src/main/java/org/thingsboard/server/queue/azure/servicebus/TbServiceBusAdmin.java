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
package org.thingsboard.server.queue.azure.servicebus;

import com.microsoft.azure.servicebus.management.ManagementClient;
import com.microsoft.azure.servicebus.management.QueueDescription;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.MessagingEntityAlreadyExistsException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：
 * 1. `TbServiceBusAdmin` 是 ThingsBoard Common Queue 中围绕 `Tb Bus Admin` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueAdmin`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbServiceBusAdmin implements TbQueueAdmin {
    /**
     * `MAX_SIZE`常量，用于统一引用固定值。
     */
    private final String MAX_SIZE = "maxSizeInMb";
    private final String MESSAGE_TIME_TO_LIVE = "messageTimeToLiveInSec";
    /**
     * 锁常量，用于统一引用固定值。
     */
    private final String LOCK_DURATION = "lockDurationInSec";

    /**
     * 队列映射关系，用于按键查找对应值。
     */
    private final Map<String, String> queueConfigs;
    private final Set<String> queues = ConcurrentHashMap.newKeySet();

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final ManagementClient client;

    /**
     * 功能：创建 `TbServiceBusAdmin` 实例，并初始化必要字段。
     * 参数：
     * - `serviceBusSettings`：服务对象。
     * - `queueConfigs`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public TbServiceBusAdmin(TbServiceBusSettings serviceBusSettings, Map<String, String> queueConfigs) {
        this.queueConfigs = queueConfigs;

        ConnectionStringBuilder builder = new ConnectionStringBuilder(
                serviceBusSettings.getNamespaceName(),
                "queues",
                serviceBusSettings.getSasKeyName(),
                serviceBusSettings.getSasKey());

        client = new ManagementClient(builder);

        try {
            client.getQueues().forEach(queueDescription -> queues.add(queueDescription.getPath()));
        } catch (ServiceBusException | InterruptedException e) {
            log.error("Failed to get queues.", e);
            throw new RuntimeException("Failed to get queues.", e);
        }
    }

    /**
     * 功能：保存或创建主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `properties`：`properties` 参数。
     * 返回：无。
     */
    @Override
    public void createTopicIfNotExists(String topic, String properties) {
        if (queues.contains(topic)) {
            return;
        }

        try {
            QueueDescription queueDescription = new QueueDescription(topic);
            queueDescription.setRequiresDuplicateDetection(false);
            setQueueConfigs(queueDescription, PropertyUtils.getProps(queueConfigs, properties));

            client.createQueue(queueDescription);
            queues.add(topic);
        } catch (ServiceBusException | InterruptedException e) {
            if (e instanceof MessagingEntityAlreadyExistsException) {
                queues.add(topic);
                log.info("[{}] queue already exists.", topic);
            } else {
                log.error("Failed to create queue: [{}]", topic, e);
            }
        }
    }

    /**
     * 功能：删除或清理主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    public void deleteTopic(String topic) {
        if (queues.contains(topic)) {
            doDelete(topic);
        } else {
            try {
                if (client.getQueue(topic) != null) {
                    doDelete(topic);
                } else {
                    log.warn("Azure Service Bus Queue [{}] is not exist.", topic);
                }
            } catch (ServiceBusException | InterruptedException e) {
                log.error("Failed to delete Azure Service Bus queue [{}]", topic, e);
            }
        }
    }

    /**
     * 功能：执行 `doDelete` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    private void doDelete(String topic) {
        try {
            client.deleteTopic(topic);
        } catch (ServiceBusException | InterruptedException e) {
            log.error("Failed to delete Azure Service Bus queue [{}]", topic, e);
        }
    }

    /**
     * 功能：更新队列。
     * 参数：
     * - `queueDescription`：队列名称或队列对象。
     * - `queueConfigs`：队列名称或队列对象。
     * 返回：无。
     */
    private void setQueueConfigs(QueueDescription queueDescription, Map<String, String> queueConfigs) {
        queueConfigs.forEach((confKey, confValue) -> {
            switch (confKey) {
                case MAX_SIZE:
                    queueDescription.setMaxSizeInMB(Long.parseLong(confValue));
                    break;
                case MESSAGE_TIME_TO_LIVE:
                    queueDescription.setDefaultMessageTimeToLive(Duration.ofSeconds(Long.parseLong(confValue)));
                    break;
                case LOCK_DURATION:
                    queueDescription.setLockDuration(Duration.ofSeconds(Long.parseLong(confValue)));
                    break;
                default:
                    log.error("Unknown config: [{}]", confKey);
            }
        });
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void destroy() {
        try {
            client.close();
        } catch (IOException e) {
            log.error("Failed to close ManagementClient.");
        }
    }
}
