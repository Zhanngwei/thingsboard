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
package org.thingsboard.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ClassUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. 类目的：`EntityLoadError` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityLoadError implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 7538450180582109391L;

    /**
     * 类型，用于区分不同处理分支。
     */
    private String type;
    private EntityId source;
    /**
     * 目标对象，表示当前对象的对应属性。
     */
    private EntityId target;
    private String message;

    /**
     * 功能：执行 `credentialsError` 对应的处理。
     * 参数：
     * - `sourceId`：`sourceId`ID。
     * 返回：处理结果。
     */
    public static EntityLoadError credentialsError(EntityId sourceId) {
        return EntityLoadError.builder().type("DEVICE_CREDENTIALS_CONFLICT").source(sourceId).build();
    }

    /**
     * 功能：执行 `referenceEntityError` 对应的处理。
     * 参数：
     * - `sourceId`：`sourceId`ID。
     * - `targetId`：目标对象ID。
     * 返回：处理结果。
     */
    public static EntityLoadError referenceEntityError(EntityId sourceId, EntityId targetId) {
        return EntityLoadError.builder().type("MISSING_REFERENCED_ENTITY").source(sourceId).target(targetId).build();
    }

    /**
     * 功能：执行 `runtimeError` 对应的处理。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：处理结果。
     */
    public static EntityLoadError runtimeError(Throwable e) {
        String message = e.getMessage();
        if (StringUtils.isEmpty(message)) {
            message = "unexpected error (" + ClassUtils.getShortClassName(e.getClass()) + ")";
        }
        return EntityLoadError.builder().type("RUNTIME").message(message).build();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityLoadError` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
