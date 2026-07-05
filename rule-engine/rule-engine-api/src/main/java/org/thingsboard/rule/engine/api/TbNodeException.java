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
package org.thingsboard.rule.engine.api;

import lombok.Getter;
import org.thingsboard.server.common.msg.TbActorError;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. 职责：表示规则节点初始化、消息处理或配置升级过程中抛出的节点级异常。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的错误边界，被规则节点和 Actor 执行链路共同识别。
 * 3. 协作对象：与 {@link TbNode}、{@link TbContext} 以及 {@link TbActorError} 协作，用于区分可恢复和不可恢复错误。
 * 4. 生命周期：随一次异常路径创建，交给 Rule Engine 运行时决定失败路由、重试或停止节点。
 * 5. 设计原因：需要携带 unrecoverable 标志，普通 Exception 无法表达节点错误是否应中止后续恢复流程。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、数据库；通过 TbActorError 间接参与 Actor 错误通信，并直接服务 Rule Engine。
 */
public class TbNodeException extends Exception implements TbActorError {

    /**
     * 是否满足`unrecoverable`条件。
     */
    @Getter
    private final boolean unrecoverable;

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(String message) {
        this(message, false);
    }

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `unrecoverable`：`unrecoverable` 参数。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(String message, boolean unrecoverable) {
        super(message);
        this.unrecoverable = unrecoverable;
    }

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(Exception e) {
        this(e, false);
    }

    /**
     * 功能：创建 `TbNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `e`：`e` 参数。
     * - `unrecoverable`：`unrecoverable` 参数。
     * 返回：新创建的对象实例。
     */
    public TbNodeException(Exception e, boolean unrecoverable) {
        super(e);
        this.unrecoverable = unrecoverable;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：为规则节点错误提供统一异常类型和可恢复性标志。
 * 2. 核心流程：节点或工具类抛出 TbNodeException，Rule Engine/Actor 错误链路读取 unrecoverable 决定处理策略。
 * 3. 关键依赖：TbNode、TbContext、TbActorError 和规则节点失败路由。
 * 4. 学习重点：Rule Engine 的异常不只表达失败原因，还表达错误恢复策略。
 */
