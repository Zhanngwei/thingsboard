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
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mResourceObserve` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@AllArgsConstructor
public class LwM2mResourceObserve {
    /**
     * `id`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "LwM2M Resource Observe id.", example = "0")
    int id;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 2, value = "LwM2M Resource Observe name.", example = "Data")
    String name;
    /**
     * 是否满足`observe`条件。
     */
    @ApiModelProperty(position = 3, value = "LwM2M Resource Observe observe.", example = "false")
    boolean observe;
    /**
     * 是否满足属性条件。
     */
    @ApiModelProperty(position = 4, value = "LwM2M Resource Observe attribute.", example = "false")
    boolean attribute;
    /**
     * 是否满足遥测条件。
     */
    @ApiModelProperty(position = 5, value = "LwM2M Resource Observe telemetry.", example = "false")
    boolean telemetry;
    /**
     * 键，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 6, value = "LwM2M Resource Observe key name.", example = "data")
    String keyName;

    /**
     * 功能：创建 `LwM2mResourceObserve` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `name`：名称。
     * - `observe`：`observe` 参数。
     * - `attribute`：`attribute` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public LwM2mResourceObserve(int id, String name, boolean observe, boolean attribute, boolean telemetry) {
        this.id = id;
        this.name = name;
        this.observe = observe;
        this.attribute = attribute;
        this.telemetry = telemetry;
        this.keyName = getCamelCase (this.name);
    }

    /**
     * 功能：获取`Camel Case`。
     * 参数：
     * - `name`：名称。
     * 返回：文本结果。
     */
    private String getCamelCase (String name) {
        name = name.replaceAll("-", " ");
        name = name.replaceAll("_", " ");
        String [] nameCamel1 = name.split(" ");
        String [] nameCamel2 = new String[nameCamel1.length];
        int[] idx = { 0 };
        Stream.of(nameCamel1).forEach((s -> {
            nameCamel2[idx[0]] = toProperCase(idx[0]++,  s);
        }));
        return String.join("", nameCamel2);
    }

    /**
     * 功能：执行 `toProperCase` 对应的处理。
     * 参数：
     * - `idx`：`idx` 参数。
     * - `s`：`s` 参数。
     * 返回：文本结果。
     */
    private String toProperCase(int idx, String s) {
        if (!s.isEmpty() && s.length()> 0) {
            String s1 = (idx == 0) ? s.substring(0, 1).toLowerCase() : s.substring(0, 1).toUpperCase();
            String s2 = "";
            if (s.length()> 1) s2 = s.substring(1).toLowerCase();
            s = s1 + s2;
        }
        return s;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mResourceObserve` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
