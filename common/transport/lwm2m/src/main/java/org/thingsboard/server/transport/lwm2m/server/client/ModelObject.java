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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;

import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`ModelObject` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Data
public class ModelObject  implements Cloneable {
    /**
     * model one on all instance
     * for each instance only id resource with parameters of resources (observe, attr, telemetry)
     */
    /**
     * `objectModel` 字段，保存当前对象的对应属性。
     */
    private ObjectModel objectModel;
    private Map<Integer, LwM2mObjectInstance> instances;

     /**
      * 功能：创建 `ModelObject` 实例，并初始化必要字段。
      * 参数：
      * - `objectModel`：`objectModel` 参数。
      * - `instances`：键值映射。
      * 返回：新创建的对象实例。
      */
     public ModelObject(ObjectModel objectModel, Map<Integer, LwM2mObjectInstance> instances) {
        this.objectModel = objectModel;
        this.instances = instances;
    }

    /**
     * 功能：删除或清理`Instance`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    public boolean removeInstance (int id ) {
        LwM2mObjectInstance instance = this.instances.get(id);
         return this.instances.remove(id, instance);
    }

    /**
     * 功能：执行 `clone` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public ModelObject clone() throws CloneNotSupportedException {
        return (ModelObject) super.clone();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ModelObject` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
