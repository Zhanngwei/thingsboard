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

import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;

/**
 * 中文说明：
 * 1. 职责：根据短信供应商配置创建对应的 {@link SmsSender}。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的短信供应商工厂层。
 * 3. 协作对象：与 {@link SmsSender}、SmsProviderConfiguration 和 SmsService 实现协作。
 * 4. 生命周期：由 Spring 实现类长期存在，在短信配置加载或刷新时被调用。
 * 5. 设计原因：创建具体供应商客户端需要解析配置，使用工厂避免 SmsService 直接依赖每个供应商构造细节。
 * 6. 设计模式：Factory，集中封装 SmsSender 实例创建逻辑。
 * 7. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；创建出的实现会调用外部短信服务并被 Rule Engine 间接使用。
 */
public interface SmsSenderFactory {

    /**
     * 功能：保存或创建`Sms Sender`。
     * 参数：
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    SmsSender createSmsSender(SmsProviderConfiguration config);

}

/*
 * 本类总结：
 * 1. 核心职责：根据短信配置创建供应商发送器。
 * 2. 核心流程：SmsService 传入 SmsProviderConfiguration，工厂选择并返回对应 SmsSender。
 * 3. 关键依赖：SmsProviderConfiguration、SmsSender 和具体供应商适配实现。
 * 4. 学习重点：Factory 模式把供应商选择和构造逻辑从业务发送流程中剥离。
 */
