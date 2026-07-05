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
package org.thingsboard.server.common.data.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`Queue` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
public class Queue extends BaseDataWithAdditionalInfo<QueueId> implements HasName, HasTenantId {
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    private String name;
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @NoXss
    @Length(fieldName = "topic")
    private String topic;
    private int pollInterval;
    /**
     * `partitions` 字段，保存当前对象的对应属性。
     */
    private int partitions;
    private boolean consumerPerPartition;
    /**
     * 版本包处理超时时间，用于控制时间范围或等待时长。
     */
    private long packProcessingTimeout;
    private SubmitStrategy submitStrategy;
    /**
     * 策略对象，封装可复用的处理规则。
     */
    private ProcessingStrategy processingStrategy;

    /**
     * 功能：创建 `Queue` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Queue() {
    }

    /**
     * 功能：创建 `Queue` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Queue(QueueId id) {
        super(id);
    }

    /**
     * 功能：创建 `Queue` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueConfiguration`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public Queue(TenantId tenantId, TenantProfileQueueConfiguration queueConfiguration) {
        this.tenantId = tenantId;
        this.name = queueConfiguration.getName();
        this.topic = queueConfiguration.getTopic();
        this.pollInterval = queueConfiguration.getPollInterval();
        this.partitions = queueConfiguration.getPartitions();
        this.consumerPerPartition = queueConfiguration.isConsumerPerPartition();
        this.packProcessingTimeout = queueConfiguration.getPackProcessingTimeout();
        this.submitStrategy = queueConfiguration.getSubmitStrategy();
        this.processingStrategy = queueConfiguration.getProcessingStrategy();
        setAdditionalInfo(queueConfiguration.getAdditionalInfo());
    }


    /**
     * 功能：获取`Custom Properties`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getCustomProperties() {
        return Optional.ofNullable(getAdditionalInfo())
                .map(info -> info.get("customProperties"))
                .filter(JsonNode::isTextual).map(JsonNode::asText).orElse(null);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`Queue` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
