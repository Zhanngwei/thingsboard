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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.WriteRequest.Mode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`ResourceValue` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Data
public class ResourceValue {

    /**
     * `lwM2mResource` 字段，保存当前对象的对应属性。
     */
    private LwM2mResource lwM2mResource;
    private ResourceModel resourceModel;

    /**
     * 功能：创建 `ResourceValue` 实例，并初始化必要字段。
     * 参数：
     * - `lwM2mResource`：`lwM2mResource` 参数。
     * - `resourceModel`：`resourceModel` 参数。
     * 返回：新创建的对象实例。
     */
    public ResourceValue(LwM2mResource lwM2mResource, ResourceModel resourceModel) {
        this.resourceModel = resourceModel;
        updateLwM2mResource(lwM2mResource, Mode.UPDATE);
    }

    /**
     * 功能：更新`Lw M2m Resource`。
     * 参数：
     * - `lwM2mResource`：`lwM2mResource` 参数。
     * - `mode`：`mode` 参数。
     * 返回：无。
     */
    public void updateLwM2mResource(LwM2mResource lwM2mResource, Mode mode) {
        if (lwM2mResource instanceof LwM2mSingleResource) {
            this.lwM2mResource = LwM2mSingleResource.newResource(lwM2mResource.getId(), lwM2mResource.getValue(), lwM2mResource.getType());
        } else if (lwM2mResource instanceof LwM2mMultipleResource) {
            if (lwM2mResource.getInstances().values().size() > 0) {
                Set<LwM2mResourceInstance> instancesSet = new HashSet<>(lwM2mResource.getInstances().values());
                if (Mode.REPLACE.equals(mode) && this.lwM2mResource != null) {
                    Map<Integer, LwM2mResourceInstance> oldInstances = this.lwM2mResource.getInstances();
                    oldInstances.values().forEach(v -> {
                        if (instancesSet.stream().noneMatch(vIns -> v.getId() == vIns.getId())) {
                            instancesSet.add(v);
                        }
                    });
                }
                LwM2mResourceInstance[] instances = instancesSet.toArray(new LwM2mResourceInstance[0]);
                this.lwM2mResource = new LwM2mMultipleResource(lwM2mResource.getId(), lwM2mResource.getType(), instances);
            }
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ResourceValue` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
