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
 * 1. 职责：作为短信配置解析和发送失败的统一运行时异常基类。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的短信异常层。
 * 3. 协作对象：与 SmsSender、SmsService、短信供应商适配器和规则节点失败处理协作。
 * 4. 生命周期：随一次短信错误路径创建，向调用方传播后由 Rule Engine 或配置测试流程处理。
 * 5. 设计原因：短信异常需要与其它外部调用错误区分，基类便于统一捕获并保留供应商错误原因。
 * 6. 技术关联：异常本身不直接涉及事务、缓存、MQTT、Actor、数据库；可能从 Rule Engine 短信节点传播到失败路由。
 */
public abstract class SmsException extends RuntimeException {

    /**
     * 功能：创建 `SmsException` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public SmsException(String msg) {
        super(msg);
    }

    /**
     * 功能：创建 `SmsException` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * - `cause`：`cause` 参数。
     * 返回：新创建的对象实例。
     */
    public SmsException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：统一短信模块异常类型。
 * 2. 核心流程：短信配置解析或发送失败时抛出 SmsException 子类，调用方统一捕获并处理。
 * 3. 关键依赖：SmsSender、SmsService 和具体供应商异常。
 * 4. 学习重点：外部供应商错误通过领域异常包装，避免泄露供应商 SDK 异常到规则节点。
 */
