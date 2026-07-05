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
package org.thingsboard.server.service.ws;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`WebSocketSessionRef` 是ThingsBoard Application 模块中的WebSocket 服务类型，用于维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 生命周期：随 WebSocket 建连创建订阅，断连或取消订阅时释放。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / Session。
 */
@Builder
@Data
public class WebSocketSessionRef {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID，用于定位对应业务对象。
     */
    private final String sessionId;
    private SecurityUser securityCtx;
    /**
     * 本地地址，用于定位外部资源或本地资源。
     */
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;
    /**
     * 会话，用于区分不同处理分支。
     */
    private final WebSocketSessionType sessionType;
    private final AtomicInteger sessionSubIdSeq = new AtomicInteger();

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
        WebSocketSessionRef that = (WebSocketSessionRef) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        String info = "";
        if (securityCtx != null) {
            info += "[" + securityCtx.getTenantId() + "]";
            info += "[" + securityCtx.getId() + "]";
        }
        info += "[" + sessionId + "]";
        return info;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WebSocketSessionRef` 在 ThingsBoard Application 模块 中承担WebSocket 服务类型职责，核心目的是维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 核心流程：接收订阅请求后注册监听，数据变化时推送到客户端。
 * 3. 关键依赖：主要依赖或协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
