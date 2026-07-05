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
package org.thingsboard.server.service.sync.vc;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`VersionControlRequestCtx` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@RequiredArgsConstructor
@Data
public class VersionControlRequestCtx {
    /**
     * 节点实例ID，用于定位对应业务对象。
     */
    private final String nodeId;
    private final UUID requestId;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final RepositorySettings settings;

    /**
     * 功能：创建 `VersionControlRequestCtx` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * - `settings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public VersionControlRequestCtx(ToVersionControlServiceMsg msg, RepositorySettings settings) {
        this.nodeId = msg.getNodeId();
        this.requestId = new UUID(msg.getRequestIdMSB(), msg.getRequestIdLSB());
        this.tenantId = new TenantId(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        this.settings = settings;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "VersionControlRequestCtx{" +
                "nodeId='" + nodeId + '\'' +
                ", requestId=" + requestId +
                ", tenantId=" + tenantId +
                '}';
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`VersionControlRequestCtx` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
