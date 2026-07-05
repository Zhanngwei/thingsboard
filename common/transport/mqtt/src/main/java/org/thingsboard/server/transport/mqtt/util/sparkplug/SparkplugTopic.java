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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by nickAS21 on 12.12.22
 */
/**
 * 中文说明：
 * 1. 类目的：`SparkplugTopic` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SparkplugTopic {

    /**
     * The Sparkplug namespace version.
     * For the Sparkplug™ B version of the specification, the UTF-8 string constant for the namespace element will be: “spBv1.0”
     */
    /**
     * `namespace` 字段，保存当前对象的对应属性。
     */
    private String namespace;

    /**
     * The ID of the logical grouping of Edge of Network (EoN) Nodes and devices.
     */
    /**
     * `groupId`ID，用于定位对应业务对象。
     */
    private String groupId;

    /**
     * The ID of the Edge of Network (EoN) Node.
     */
    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    private String edgeNodeId;

    /**
     * The ID of the device.
     */
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private String deviceId;

    /**
     * The message type.
     */
    /**
     * 类型，用于区分不同处理分支。
     */
    private SparkplugMessageType type;

     /**
     * Constructor (device).
     *
     * @param namespace the namespace.
     * @param groupId the group ID.
     * @param edgeNodeId the edge node ID.
     * @param deviceId the device ID.
     * @param type the message type.
     */
    /**
     * 功能：创建 `SparkplugTopic` 实例，并初始化必要字段。
     * 参数：
     * - `namespace`：名称。
     * - `groupId`：`groupId`ID。
     * - `edgeNodeId`：边缘节点ID。
     * - `deviceId`：设备IDID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public SparkplugTopic(String namespace, String groupId, String edgeNodeId, String deviceId, SparkplugMessageType type) {
        super();
        this.namespace = namespace;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * Constructor (node).
     *
     * @param namespace the namespace.
     * @param groupId the group ID.
     * @param edgeNodeId the edge node ID.
     * @param type the message type.
     */
    /**
     * 功能：创建 `SparkplugTopic` 实例，并初始化必要字段。
     * 参数：
     * - `namespace`：名称。
     * - `groupId`：`groupId`ID。
     * - `edgeNodeId`：边缘节点ID。
     * - `type`：类型。
     * 返回：新创建的对象实例。
     */
    public SparkplugTopic(String namespace, String groupId, String edgeNodeId, SparkplugMessageType type) {
        super();
        this.namespace = namespace;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = null;
        this.type = type;
    }

    /**
     * 功能：创建 `SparkplugTopic` 实例，并初始化必要字段。
     * 参数：
     * - `sparkplugTopic`：主题名称或主题对象。
     * - `type`：类型。
     * 返回：新创建的对象实例。
     */
    public SparkplugTopic(SparkplugTopic sparkplugTopic,  SparkplugMessageType type) {
        super();
        this.namespace = sparkplugTopic.namespace;
        this.groupId = sparkplugTopic.groupId;
        this.edgeNodeId = sparkplugTopic.edgeNodeId;
        this.deviceId = null;
        this.type = type;
    }
    /**
     * 功能：创建 `SparkplugTopic` 实例，并初始化必要字段。
     * 参数：
     * - `sparkplugTopic`：主题名称或主题对象。
     * - `type`：类型。
     * - `deviceId`：设备IDID。
     * 返回：新创建的对象实例。
     */
    public SparkplugTopic(SparkplugTopic sparkplugTopic,  SparkplugMessageType type, String deviceId) {
        super();
        this.namespace = sparkplugTopic.namespace;
        this.groupId = sparkplugTopic.groupId;
        this.edgeNodeId = sparkplugTopic.edgeNodeId;
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * @return the Sparkplug namespace version
     */
    /**
     * 功能：获取`Namespace`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the ID of the logical grouping of Edge of Network (EoN) Nodes and devices.
     *
     * @return the group ID
     */
    /**
     * 功能：获取`Group Id`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return  the ID of the Edge of Network (EoN) Node
     */
    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    /**
     * @return the device ID
     */
    /**
     * 功能：获取设备ID。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @return the message type
     */
    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    public SparkplugMessageType getType() {
        return type;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getNamespace()).append("/")
                .append(getGroupId()).append("/")
                .append(getType()).append("/")
                .append(getEdgeNodeId());
        if (getDeviceId() != null) {
            sb.append("/").append(getDeviceId());
        }
        return sb.toString();
    }

    /**
     * @param type the type to check
     * @return true if this topic's type matches the passes in type, false otherwise
     */
    /**
     * 功能：判断类型。
     * 参数：
     * - `type`：类型。
     * 返回：判断结果。
     */
    public boolean isType(SparkplugMessageType type) {
        return this.type != null && this.type.equals(type);
    }

    /**
     * 功能：判断节点实例。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isNode() {
        return this.deviceId == null;
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getNodeDeviceName() {
        return isNode() ? edgeNodeId : deviceId;
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`SparkplugTopic` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
