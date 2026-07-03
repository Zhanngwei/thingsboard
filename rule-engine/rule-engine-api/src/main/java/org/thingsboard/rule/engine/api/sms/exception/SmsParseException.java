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
 * 1. 职责：表示短信供应商配置解析失败。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的短信异常层。
 * 3. 协作对象：与短信配置、SmsSenderFactory 和 SmsService 配置刷新流程协作。
 * 4. 生命周期：随一次配置解析失败创建并传播到调用方。
 * 5. 设计原因：配置错误与发送错误处理方式不同，单独异常类型便于向管理员反馈配置问题。
 * 6. 技术关联：异常本身不直接涉及事务、缓存、MQTT、Actor、数据库；可能阻断 Rule Engine 短信发送前置配置流程。
 */
public class SmsParseException extends SmsException {

    /**
     * 中文说明：
     * 1. 方法职责：用解析错误消息创建异常。
     * 2. 输入参数：msg 是配置解析失败原因。
     * 3. 返回值：构造函数无返回值。
     * 4. 调用时机：短信配置缺失、格式错误或供应商参数非法时调用。
     * 5. 调用方：SmsSenderFactory 或配置解析工具。
     * 6. 使用流程：属于短信配置验证流程。
     * 7. 线程安全：异常对象随单次错误传播使用。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；间接影响 Rule Engine 短信节点可用性。
     */
    public SmsParseException(String msg) {
        super(msg);
    }

    /**
     * 中文说明：
     * 1. 方法职责：用解析错误消息和底层原因创建异常。
     * 2. 输入参数：msg 是错误描述，cause 是 JSON/配置转换底层异常。
     * 3. 返回值：构造函数无返回值。
     * 4. 调用时机：需要保留配置解析堆栈时调用。
     * 5. 调用方：SmsSenderFactory 或配置解析工具。
     * 6. 使用流程：属于短信配置验证失败包装流程。
     * 7. 线程安全：异常对象随单次错误传播使用。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；服务 Rule Engine 配置错误处理。
     */
    public SmsParseException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：表达短信供应商配置解析失败。
 * 2. 核心流程：配置解析发现问题后抛出 SmsParseException，配置测试或发送流程向调用方反馈。
 * 3. 关键依赖：SmsException、SmsSenderFactory 和短信配置模型。
 * 4. 学习重点：解析错误独立建模，便于区分配置问题和外部发送失败。
 */
