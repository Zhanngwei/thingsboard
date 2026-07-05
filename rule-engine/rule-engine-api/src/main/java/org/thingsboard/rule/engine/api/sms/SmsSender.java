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
package org.thingsboard.rule.engine.api.sms;

import org.thingsboard.rule.engine.api.sms.exception.SmsException;

/**
 * 中文说明：
 * 1. 职责：抽象单个短信供应商客户端的发送和销毁能力。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的短信供应商适配层。
 * 3. 协作对象：与 {@link SmsSenderFactory}、{@link org.thingsboard.rule.engine.api.SmsService} 和具体供应商 SDK/HTTP 客户端协作。
 * 4. 生命周期：由工厂根据短信配置创建，随配置实例或发送服务生命周期存在，销毁时释放外部资源。
 * 5. 设计原因：不同短信供应商发送协议不同，通过 Strategy 接口让 SmsService 统一调用。
 * 6. 设计模式：Strategy，具体 SmsSender 实现封装供应商差异。
 * 7. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现会调用外部短信服务，可能被 Rule Engine 短信节点触发。
 */
public interface SmsSender {

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `numberTo`：`numberTo` 参数。
     * - `message`：待处理消息。
     * 返回：数值结果。
     */
    int sendSms(String numberTo, String message) throws SmsException;

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void destroy();

}

/*
 * 本类总结：
 * 1. 核心职责：统一不同短信供应商的发送接口。
 * 2. 核心流程：SmsService 通过工厂创建 SmsSender，调用 sendSms 发送，配置变化或关闭时调用 destroy。
 * 3. 关键依赖：SmsException、SmsSenderFactory、SmsService 和具体短信供应商实现。
 * 4. 学习重点：短信供应商差异通过 Strategy 接口隔离，规则节点不直接感知供应商细节。
 */
