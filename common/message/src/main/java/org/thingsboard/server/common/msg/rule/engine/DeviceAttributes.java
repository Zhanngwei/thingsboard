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
package org.thingsboard.server.common.msg.rule.engine;

import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`DeviceAttributes` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class DeviceAttributes {

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    private final Map<String, AttributeKvEntry> clientSideAttributesMap;
    private final Map<String, AttributeKvEntry> serverPrivateAttributesMap;
    /**
     * 服务端映射关系，用于按键查找对应值。
     */
    private final Map<String, AttributeKvEntry> serverPublicAttributesMap;

    /**
     * 功能：创建 `DeviceAttributes` 实例，并初始化必要字段。
     * 参数：
     * - `clientSideAttributes`：客户端对象。
     * - `serverPrivateAttributes`：数据列表。
     * - `serverPublicAttributes`：数据列表。
     * 返回：新创建的对象实例。
     */
    public DeviceAttributes(List<AttributeKvEntry> clientSideAttributes, List<AttributeKvEntry> serverPrivateAttributes, List<AttributeKvEntry> serverPublicAttributes) {
        this.clientSideAttributesMap = mapAttributes(clientSideAttributes);
        this.serverPrivateAttributesMap = mapAttributes(serverPrivateAttributes);
        this.serverPublicAttributesMap = mapAttributes(serverPublicAttributes);
    }

    /**
     * 功能：转换`Attributes`。
     * 参数：
     * - `attributes`：数据列表。
     * 返回：处理结果。
     */
    private static Map<String, AttributeKvEntry> mapAttributes(List<AttributeKvEntry> attributes) {
        Map<String, AttributeKvEntry> result = new HashMap<>();
        for (AttributeKvEntry attribute : attributes) {
            result.put(attribute.getKey(), attribute);
        }
        return result;
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Collection<AttributeKvEntry> getClientSideAttributes() {
        return clientSideAttributesMap.values();
    }

    /**
     * 功能：获取服务端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Collection<AttributeKvEntry> getServerSideAttributes() {
        return serverPrivateAttributesMap.values();
    }

    /**
     * 功能：获取服务端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Collection<AttributeKvEntry> getServerSidePublicAttributes() {
        return serverPublicAttributesMap.values();
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `attribute`：`attribute` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<AttributeKvEntry> getClientSideAttribute(String attribute) {
        return Optional.ofNullable(clientSideAttributesMap.get(attribute));
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `attribute`：`attribute` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<AttributeKvEntry> getServerPrivateAttribute(String attribute) {
        return Optional.ofNullable(serverPrivateAttributesMap.get(attribute));
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `attribute`：`attribute` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<AttributeKvEntry> getServerPublicAttribute(String attribute) {
        return Optional.ofNullable(serverPublicAttributesMap.get(attribute));
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    public void remove(AttributeKey key) {
        Map<String, AttributeKvEntry> map = getMapByScope(key.getScope());
        if (map != null) {
            map.remove(key.getAttributeKey());
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `scope`：`scope` 参数。
     * - `values`：值。
     * 返回：无。
     */
    public void update(String scope, List<AttributeKvEntry> values) {
        Map<String, AttributeKvEntry> map = getMapByScope(scope);
        values.forEach(v -> map.put(v.getKey(), v));
    }

    /**
     * 功能：获取`Map By Scope`。
     * 参数：
     * - `scope`：`scope` 参数。
     * 返回：处理结果。
     */
    private Map<String, AttributeKvEntry> getMapByScope(String scope) {
        Map<String, AttributeKvEntry> map = null;
        if (scope.equalsIgnoreCase(DataConstants.CLIENT_SCOPE)) {
            map = clientSideAttributesMap;
        } else if (scope.equalsIgnoreCase(DataConstants.SHARED_SCOPE)) {
            map = serverPublicAttributesMap;
        } else if (scope.equalsIgnoreCase(DataConstants.SERVER_SCOPE)) {
            map = serverPrivateAttributesMap;
        }
        return map;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "DeviceAttributes{" +
                "clientSideAttributesMap=" + clientSideAttributesMap +
                ", serverPrivateAttributesMap=" + serverPrivateAttributesMap +
                ", serverPublicAttributesMap=" + serverPublicAttributesMap +
                '}';
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceAttributes` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
