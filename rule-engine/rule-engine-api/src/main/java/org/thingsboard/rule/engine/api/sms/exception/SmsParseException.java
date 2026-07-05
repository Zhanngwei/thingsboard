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
     * 功能：创建 `SmsParseException` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public SmsParseException(String msg) {
        super(msg);
    }

    /**
     * 功能：创建 `SmsParseException` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * - `cause`：`cause` 参数。
     * 返回：新创建的对象实例。
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
