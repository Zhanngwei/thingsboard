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
package org.thingsboard.server.service.sms.twilio;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsParseException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.TwilioSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. 类目的：`TwilioSmsSender` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class TwilioSmsSender extends AbstractSmsSender {

    private static final Pattern PHONE_NUMBERS_SID_MESSAGE_SERVICE_SID = Pattern.compile("^(PN|MG).*$");

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private TwilioRestClient twilioRestClient;
    private String numberFrom;

    /**
     * 功能：校验`Phone Twilio Number`。
     * 参数：
     * - `phoneNumber`：`phoneNumber` 参数。
     * 返回：判断结果。
     */
    private String validatePhoneTwilioNumber(String phoneNumber) throws SmsParseException {
        phoneNumber = phoneNumber.trim();
        if (!E_164_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches() && !PHONE_NUMBERS_SID_MESSAGE_SERVICE_SID.matcher(phoneNumber).matches()) {
            throw new SmsParseException("Invalid phone number format. Phone number must be in E.164 format/Phone Number's SID/Messaging Service SID.");
        }
        return phoneNumber;
    }

    /**
     * 功能：创建 `TwilioSmsSender` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * 返回：新创建的对象实例。
     */
    public TwilioSmsSender(TwilioSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccountSid()) || StringUtils.isEmpty(config.getAccountToken()) || StringUtils.isEmpty(config.getNumberFrom())) {
            throw new IllegalArgumentException("Invalid twilio sms provider configuration: accountSid, accountToken and numberFrom should be specified!");
        }
        this.numberFrom = this.validatePhoneTwilioNumber(config.getNumberFrom());
        this.twilioRestClient = new TwilioRestClient.Builder(config.getAccountSid(), config.getAccountToken()).build();
    }

    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `numberTo`：`numberTo` 参数。
     * - `message`：待处理消息。
     * 返回：数值结果。
     */
    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumber(numberTo);
        message = this.prepareMessage(message);
        try {
            String numSegments = Message.creator(new PhoneNumber(numberTo), new PhoneNumber(this.numberFrom), message).create(this.twilioRestClient).getNumSegments();
            return Integer.valueOf(numSegments);
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {

    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TwilioSmsSender` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
