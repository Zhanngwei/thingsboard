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
package org.thingsboard.server.transport.lwm2m.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.util.CollectionsUtil.diffSets;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MModelConfig` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Data
@NoArgsConstructor
@ToString(exclude = "toCancelRead")
@Slf4j
public class LwM2MModelConfig {
    /**
     * `endpoint` 字段，保存当前对象的对应属性。
     */
    private String endpoint;
    private Map<String, ObjectAttributes> attributesToAdd;
    /**
     * `attributesToRemove`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<String> attributesToRemove;
    private Set<String> toObserve;
    /**
     * `toCancelObserve`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<String> toCancelObserve;
    private Set<String> toRead;
    /**
     * `toCancelRead`集合，用于去重保存或快速判断对象是否存在。
     */
    @JsonIgnore
    private Set<String> toCancelRead;

    /**
     * 功能：创建 `LwM2MModelConfig` 实例，并初始化必要字段。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2MModelConfig(String endpoint) {
        this.endpoint = endpoint;
        this.attributesToAdd = new ConcurrentHashMap<>();
        this.attributesToRemove = ConcurrentHashMap.newKeySet();
        this.toObserve = ConcurrentHashMap.newKeySet();
        this.toCancelObserve = ConcurrentHashMap.newKeySet();
        this.toRead = ConcurrentHashMap.newKeySet();
        this.toCancelRead = new HashSet<>();
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `modelConfig`：配置对象。
     * 返回：无。
     */
    public void merge(LwM2MModelConfig modelConfig) {
        if (modelConfig.isEmpty() && modelConfig.getToCancelRead().isEmpty()) {
            return;
        }

        modelConfig.getAttributesToAdd().forEach((k, v) -> {
            if (this.attributesToRemove.contains(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToAdd.put(k, v);
            }
        });

        modelConfig.getAttributesToRemove().forEach(k -> {
            if (this.attributesToAdd.containsKey(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToRemove.add(k);
            }
        });

        this.toObserve.addAll(diffSets(this.toCancelObserve, modelConfig.getToObserve()));
        this.toCancelObserve.addAll(diffSets(this.toObserve, modelConfig.getToCancelObserve()));

        this.toObserve.removeAll(modelConfig.getToCancelObserve());
        this.toCancelObserve.removeAll(modelConfig.getToObserve());

        this.toRead.removeAll(modelConfig.getToObserve());
        this.toRead.removeAll(modelConfig.getToCancelRead());
        this.toRead.addAll(modelConfig.getToRead());
    }

    /**
     * 功能：判断`Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isEmpty() {
        return attributesToAdd.isEmpty() && toObserve.isEmpty() && toCancelObserve.isEmpty() && toRead.isEmpty();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MModelConfig` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
