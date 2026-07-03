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
     * 中文说明：标记该异常是否不可恢复，数据来源于构造函数参数；生命周期与异常对象一致。
     * 设计为字段是为了让 Rule Engine Actor 错误处理链路在捕获异常后仍能判断恢复策略。
     */
    @Getter
    private final boolean unrecoverable;

    /**
     * 中文说明：
     * 1. 方法职责：用错误消息创建默认可恢复的节点异常。
     * 2. 输入参数：message 表示面向日志和失败处理的异常描述。
     * 3. 返回值：构造函数无返回值，生成 unrecoverable=false 的异常实例。
     * 4. 调用时机：节点检测到普通配置或消息处理错误时调用。
     * 5. 调用方：具体规则节点、配置转换工具和 Rule Engine 支撑类。
     * 6. 使用流程：用于 Rule Engine 失败路由或 Actor 错误上报。
     * 7. 线程安全：异常实例创建后除堆栈外无共享可变状态，按异常对象使用是线程安全的。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、数据库；可能被 Actor 错误流程消费，直接涉及 Rule Engine。
     */
    public TbNodeException(String message) {
        this(message, false);
    }

    /**
     * 中文说明：
     * 1. 方法职责：用错误消息和恢复标志创建节点异常。
     * 2. 输入参数：message 是异常描述，unrecoverable 表示 Rule Engine 是否应视为不可恢复错误。
     * 3. 返回值：构造函数无返回值，生成携带恢复语义的异常实例。
     * 4. 调用时机：节点或工具类能明确判断错误恢复策略时调用。
     * 5. 调用方：具体规则节点、配置升级逻辑和 Rule Engine 公共工具。
     * 6. 使用流程：用于消息失败处理、节点初始化失败和 Actor 错误传播。
     * 7. 线程安全：异常对象按单次错误路径使用，不依赖共享可变状态。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接操作事务、缓存、MQTT、数据库；通过 TbActorError 参与 Actor 错误语义，直接涉及 Rule Engine。
     */
    public TbNodeException(String message, boolean unrecoverable) {
        super(message);
        this.unrecoverable = unrecoverable;
    }

    /**
     * 中文说明：
     * 1. 方法职责：包装已有异常为默认可恢复的节点异常。
     * 2. 输入参数：e 是底层配置、脚本、数据库回调或消息处理异常。
     * 3. 返回值：构造函数无返回值，生成 unrecoverable=false 的包装异常。
     * 4. 调用时机：调用方需要把通用异常转换成 Rule Engine 节点异常时调用。
     * 5. 调用方：规则节点实现、工具类和异步回调处理逻辑。
     * 6. 使用流程：用于统一 Rule Engine 异常类型，便于失败路由和日志处理。
     * 7. 线程安全：异常对象随单次错误传播使用，不持有共享运行期资源。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、数据库；可能包装这些流程的异常，直接涉及 Rule Engine 和 Actor 错误语义。
     */
    public TbNodeException(Exception e) {
        this(e, false);
    }

    /**
     * 中文说明：
     * 1. 方法职责：包装已有异常并显式指定是否不可恢复。
     * 2. 输入参数：e 是原始异常，unrecoverable 表示 Actor/Rule Engine 是否应停止恢复尝试。
     * 3. 返回值：构造函数无返回值，生成携带原始异常和恢复语义的实例。
     * 4. 调用时机：底层异常需要保留堆栈且调用方能判断恢复策略时调用。
     * 5. 调用方：节点初始化、配置转换、脚本执行和外部调用包装逻辑。
     * 6. 使用流程：用于 Rule Engine 失败路由、节点停止或错误日志记录。
     * 7. 线程安全：异常实例不应跨线程修改，按异常传播模型使用是安全的。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接操作事务、缓存、MQTT、数据库；通过 TbActorError 参与 Actor 错误通信，直接涉及 Rule Engine。
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
