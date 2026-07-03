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
package org.thingsboard.rule.engine.api.sms.exception;

/**
 * 中文说明：
 * 1. 职责：表示短信已经进入发送阶段后发生的外部投递失败。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的短信异常层。
 * 3. 协作对象：与 SmsSender、SmsService、供应商 HTTP/SDK 客户端和规则节点失败路由协作。
 * 4. 生命周期：随一次短信发送失败创建并传播到调用方。
 * 5. 设计原因：发送失败通常需要重试、失败路由或供应商错误展示，需与配置解析失败区分。
 * 6. 技术关联：异常本身不直接涉及事务、缓存、MQTT、Actor、数据库；可能从 Rule Engine 短信节点传播到失败关系。
 */
public class SmsSendException extends SmsException {

    /**
     * 中文说明：
     * 1. 方法职责：用发送失败消息创建异常。
     * 2. 输入参数：msg 是供应商或发送器返回的失败原因。
     * 3. 返回值：构造函数无返回值。
     * 4. 调用时机：供应商返回失败状态但没有底层异常时调用。
     * 5. 调用方：具体 SmsSender 实现。
     * 6. 使用流程：属于短信外部发送失败流程。
     * 7. 线程安全：异常对象随单次错误传播使用。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；可能触发 Rule Engine 失败路由。
     */
    public SmsSendException(String msg) {
        super(msg);
    }

    /**
     * 中文说明：
     * 1. 方法职责：用发送失败消息和底层原因创建异常。
     * 2. 输入参数：msg 是错误描述，cause 是供应商 SDK、HTTP 或网络异常。
     * 3. 返回值：构造函数无返回值。
     * 4. 调用时机：需要保留底层发送异常堆栈时调用。
     * 5. 调用方：具体 SmsSender 实现。
     * 6. 使用流程：属于短信发送失败包装流程。
     * 7. 线程安全：异常对象随单次错误传播使用。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；服务 Rule Engine 外部调用失败处理。
     */
    public SmsSendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：表达短信供应商投递失败。
 * 2. 核心流程：SmsSender 捕获或识别发送失败后抛出 SmsSendException，SmsService 或规则节点处理失败。
 * 3. 关键依赖：SmsException、SmsSender 和外部短信供应商客户端。
 * 4. 学习重点：外部发送失败独立建模，便于 Rule Engine 做失败路由或重试策略。
 */
