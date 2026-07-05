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
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`EntityType` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public enum EntityType {
    TENANT(1),
    CUSTOMER(2),
    USER(3),
    DASHBOARD(4),
    ASSET(5),
    DEVICE(6),
    ALARM (7),
    RULE_CHAIN (11),
    RULE_NODE (12),

    ENTITY_VIEW (15) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName () {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE (16),
    WIDGET_TYPE (17),
    TENANT_PROFILE (20),
    DEVICE_PROFILE (21),
    ASSET_PROFILE (22),
    API_USAGE_STATE (23),
    TB_RESOURCE (24),
    OTA_PACKAGE (25),
    EDGE (26),
    RPC (27),
    QUEUE (28),
    NOTIFICATION_TARGET (29),
    NOTIFICATION_TEMPLATE (30),
    NOTIFICATION_REQUEST (31),
    NOTIFICATION (32),
    NOTIFICATION_RULE (33);

    /**
     * Protobuf，表示当前对象的对应属性。
     */
    @Getter
    private final int protoNumber; // Corresponds to EntityTypeProto

    /**
     * 功能：创建 `EntityType` 实例，并初始化必要字段。
     * 参数：
     * - `protoNumber`：`protoNumber` 参数。
     * 返回：新创建的对象实例。
     */
    private EntityType(int protoNumber) {
        this.protoNumber = protoNumber;
    }

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).collect(Collectors.toUnmodifiableList());

    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityType` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
