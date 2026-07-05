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
package org.thingsboard.server.service.sms.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.AwsSnsSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`AwsSmsSender` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public class AwsSmsSender extends AbstractSmsSender {

    private static final Map<String, MessageAttributeValue> SMS_ATTRIBUTES = new HashMap<>();

    static {
        SMS_ATTRIBUTES.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Transactional")
                .withDataType("String"));
    }

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private AmazonSNS snsClient;

    /**
     * 功能：创建 `AwsSmsSender` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * 返回：新创建的对象实例。
     */
    public AwsSmsSender(AwsSnsSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccessKeyId()) || StringUtils.isEmpty(config.getSecretAccessKey()) || StringUtils.isEmpty(config.getRegion())) {
            throw new IllegalArgumentException("Invalid AWS sms provider configuration: aws accessKeyId, aws secretAccessKey and aws region should be specified!");
        }
        AWSCredentials awsCredentials = new BasicAWSCredentials(config.getAccessKeyId(), config.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);
        this.snsClient = AmazonSNSClient.builder()
                .withCredentials(credProvider)
                .withRegion(config.getRegion())
                .build();
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
            PublishRequest publishRequest = new PublishRequest()
                    .withMessageAttributes(SMS_ATTRIBUTES)
                    .withPhoneNumber(numberTo)
                    .withMessage(message);
            this.snsClient.publish(publishRequest);
            return this.countMessageSegments(message);
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
        if (this.snsClient != null) {
            try {
                this.snsClient.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown SNS client during destroy()", e);
            }
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AwsSmsSender` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
