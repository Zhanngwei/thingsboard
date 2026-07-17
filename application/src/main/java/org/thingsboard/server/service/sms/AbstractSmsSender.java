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
package org.thingsboard.server.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.exception.SmsParseException;

import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `AbstractSmsSender` 是 ThingsBoard Application 中处理 `Sms Sender` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `SmsSender`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class AbstractSmsSender implements SmsSender {

    protected static final Pattern E_164_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final int MAX_SMS_MESSAGE_LENGTH = 1600;
    private static final int MAX_SMS_SEGMENT_LENGTH = 70;

    /**
     * 功能：校验`Phone Number`。
     * 参数：
     * - `phoneNumber`：`phoneNumber` 参数。
     * 返回：判断结果。
     */
    protected String validatePhoneNumber(String phoneNumber) throws SmsParseException {
        phoneNumber = phoneNumber.trim();
        if (!E_164_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new SmsParseException("Invalid phone number format. Phone number must be in E.164 format.");
        }
        return phoneNumber;
    }

    /**
     * 功能：执行 `prepareMessage` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：文本结果。
     */
    protected String prepareMessage(String message) {
        message = message.replaceAll("^\"|\"$", "").replaceAll("\\\\n", "\n");
        if (message.length() > MAX_SMS_MESSAGE_LENGTH) {
            log.warn("SMS message exceeds maximum symbols length and will be truncated");
            message = message.substring(0, MAX_SMS_MESSAGE_LENGTH);
        }
        return message;
    }

    /**
     * 功能：统计消息数量。
     * 参数：
     * - `message`：待处理消息。
     * 返回：数值结果。
     */
    protected int countMessageSegments(String message) {
        return (int)Math.ceil((double) message.length() / (double) MAX_SMS_SEGMENT_LENGTH);
    }

}
