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
package org.thingsboard.server.queue.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`TopicService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Service
public class TopicService {

    /**
     * `prefix` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.prefix:}")
    private String prefix;

    private Map<String, TopicPartitionInfo> tbCoreNotificationTopics = new HashMap<>();
    private Map<String, TopicPartitionInfo> tbRuleEngineNotificationTopics = new HashMap<>();

    /**
     * Each Service should start a consumer for messages that target individual service instance based on serviceId.
     * This topic is likely to have single partition, and is always assigned to the service.
     * @param serviceType
     * @param serviceId
     * @return
     */
    /**
     * 功能：获取主题。
     * 参数：
     * - `serviceType`：服务对象。
     * - `serviceId`：服务ID。
     * 返回：处理结果。
     */
    public TopicPartitionInfo getNotificationsTopic(ServiceType serviceType, String serviceId) {
        switch (serviceType) {
            case TB_CORE:
                return tbCoreNotificationTopics.computeIfAbsent(serviceId,
                        id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            case TB_RULE_ENGINE:
                return tbRuleEngineNotificationTopics.computeIfAbsent(serviceId,
                        id -> buildNotificationsTopicPartitionInfo(serviceType, serviceId));
            default:
                return buildNotificationsTopicPartitionInfo(serviceType, serviceId);
        }
    }

    /**
     * 功能：构建主题。
     * 参数：
     * - `serviceType`：服务对象。
     * - `serviceId`：服务ID。
     * 返回：处理结果。
     */
    private TopicPartitionInfo buildNotificationsTopicPartitionInfo(ServiceType serviceType, String serviceId) {
        return buildTopicPartitionInfo(serviceType.name().toLowerCase() + ".notifications." + serviceId, null, null, false);
    }

    /**
     * 功能：构建主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `tenantId`：租户IDID。
     * - `partition`：分区标识或分区信息。
     * - `myPartition`：分区标识或分区信息。
     * 返回：处理结果。
     */
    public TopicPartitionInfo buildTopicPartitionInfo(String topic, TenantId tenantId, Integer partition, boolean myPartition) {
        return new TopicPartitionInfo(buildTopicName(topic), tenantId, partition, myPartition);
    }

    /**
     * 功能：构建主题名称。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：文本结果。
     */
    public String buildTopicName(String topic) {
        return prefix.isBlank() ? topic : prefix + "." + topic;
    }

/*
 * 本类总结：
 * 1. 核心职责：`TopicService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}