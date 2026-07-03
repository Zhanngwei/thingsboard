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

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;

/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine 和系统通知流程发送短信的统一服务边界。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的外部短信服务接口。
 * 3. 协作对象：与短信规则节点、通知中心、租户短信配置、{@link org.thingsboard.rule.engine.api.sms.SmsSenderFactory} 协作。
 * 4. 生命周期：由 Spring 实现类提供，规则节点或系统流程通过 {@link TbContext} 获取后调用。
 * 5. 设计原因：规则节点不应直接依赖具体短信供应商，通过接口隔离配置刷新、测试和发送实现。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；实现可能读取配置缓存并调用外部短信服务，直接服务 Rule Engine。
 */
public interface SmsService {

    /**
     * 中文说明：
     * 1. 方法职责：刷新短信发送配置。
     * 2. 输入参数：无。
     * 3. 返回值：无，刷新后的配置由实现类内部维护。
     * 4. 调用时机：短信配置变更后调用。
     * 5. 调用方：配置管理流程或系统服务。
     * 6. 使用流程：属于短信配置生命周期，不处理单条规则消息。
     * 7. 线程安全：具体实现需保证配置刷新与发送并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能更新配置缓存并服务 Rule Engine。
     */
    void updateSmsConfiguration();

    /**
     * 中文说明：
     * 1. 方法职责：向一个或多个号码发送短信。
     * 2. 输入参数：tenantId/customerId 表示租户和客户上下文，numbersTo 是目标号码数组，message 是短信内容。
     * 3. 返回值：无；失败通过 ThingsboardException 抛出。
     * 4. 调用时机：短信规则节点或通知流程完成模板渲染后调用。
     * 5. 调用方：短信规则节点、通知中心。
     * 6. 使用流程：直接服务 Rule Engine 外部短信发送流程。
     * 7. 线程安全：接口无状态，具体实现需保证供应商客户端和配置并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及 MQTT、Actor；实现可能读取配置缓存并调用外部短信 API。
     */
    void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException;;

    /**
     * 中文说明：
     * 1. 方法职责：使用测试请求验证短信配置并发送测试短信。
     * 2. 输入参数：testSmsRequest 包含测试短信供应商配置、号码和消息内容。
     * 3. 返回值：无；配置或发送失败通过 ThingsboardException 抛出。
     * 4. 调用时机：管理员测试短信配置时调用。
     * 5. 调用方：短信配置管理 API。
     * 6. 使用流程：属于配置验证流程，不属于规则链消息路由。
     * 7. 线程安全：由实现隔离测试客户端和共享配置。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、缓存、MQTT、Actor、数据库；执行外部短信服务调用。
     */
    void sendTestSms(TestSmsRequest testSmsRequest) throws ThingsboardException;

    /**
     * 中文说明：
     * 1. 方法职责：判断租户短信服务是否已配置。
     * 2. 输入参数：tenantId 表示租户边界。
     * 3. 返回值：true 表示可发送短信，false 表示未配置或不可用。
     * 4. 调用时机：发送前校验或 UI 展示配置状态时调用。
     * 5. 调用方：短信规则节点、通知中心、配置管理流程。
     * 6. 使用流程：属于 Rule Engine/通知发送前置校验。
     * 7. 线程安全：实现需保证配置读取并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能读取配置缓存。
     */
    boolean isConfigured(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：为规则节点和系统通知提供短信发送 API。
 * 2. 核心流程：调用方准备号码和内容，SmsService 实现读取配置并调用外部短信供应商。
 * 3. 关键依赖：TenantId、CustomerId、TestSmsRequest、SmsSenderFactory 和短信供应商配置。
 * 4. 学习重点：短信节点通过服务接口隔离供应商差异，Rule Engine 不直接绑定具体短信 SDK。
 */
