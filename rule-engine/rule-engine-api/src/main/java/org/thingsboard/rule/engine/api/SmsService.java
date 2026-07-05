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
     * 功能：更新`Sms Configuration`。
     * 参数：无。
     * 返回：无。
     */
    void updateSmsConfiguration();

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `numbersTo`：`numbersTo` 参数。
     * - `message`：待处理消息。
     * 返回：无。
     */
    void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException;;

    /**
     * 功能：发送或提交`Test Sms`。
     * 参数：
     * - `testSmsRequest`：请求对象。
     * 返回：无。
     */
    void sendTestSms(TestSmsRequest testSmsRequest) throws ThingsboardException;

    /**
     * 功能：判断`Configured`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
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
