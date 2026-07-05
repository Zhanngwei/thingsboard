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
package org.thingsboard.server.common.data.relation;

import io.swagger.annotations.ApiModelProperty;

/**
 * 中文说明：
 * 1. 类目的：`EntityRelationInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public class EntityRelationInfo extends EntityRelation {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2807343097519543363L;

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String fromName;
    private String toName;

    /**
     * 功能：创建 `EntityRelationInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EntityRelationInfo() {
        super();
    }

    /**
     * 功能：创建 `EntityRelationInfo` 实例，并初始化必要字段。
     * 参数：
     * - `entityRelation`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityRelationInfo(EntityRelation entityRelation) {
        super(entityRelation);
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, value = "Name of the entity for [from] direction.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "A4B72CCDFF33")
    public String getFromName() {
        return fromName;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `fromName`：名称。
     * 返回：无。
     */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, value = "Name of the entity for [to] direction.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "A4B72CCDFF35")
    public String getToName() {
        return toName;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `toName`：名称。
     * 返回：无。
     */
    public void setToName(String toName) {
        this.toName = toName;
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EntityRelationInfo that = (EntityRelationInfo) o;

        return toName != null ? toName.equals(that.toName) : that.toName == null;

    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (toName != null ? toName.hashCode() : 0);
        return result;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityRelationInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
