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
 * 1. `AwsSmsSender` 是 ThingsBoard Application 中处理 `Aws Sms Sender` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractSmsSender`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
