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
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `NotificationRequestStats` 是 ThingsBoard Common Data 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class NotificationRequestStats {

    /**
     * `sent`映射关系，用于按键查找对应值。
     */
    private final Map<NotificationDeliveryMethod, AtomicInteger> sent;
    /**
     * `totalSent` 字段，保存当前对象的对应属性。
     */
    @JsonIgnore
    private final AtomicInteger totalSent;
    private final Map<NotificationDeliveryMethod, Map<String, String>> errors;
    /**
     * `totalErrors` 字段，保存当前对象的对应属性。
     */
    @JsonIgnore
    private final AtomicInteger totalErrors;
    private String error;
    /**
     * `processedRecipients`集合，用于去重保存或快速判断对象是否存在。
     */
    @JsonIgnore
    private final Map<NotificationDeliveryMethod, Set<Object>> processedRecipients;

    /**
     * 功能：创建 `NotificationRequestStats` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public NotificationRequestStats() {
        this.sent = new ConcurrentHashMap<>();
        this.totalSent = new AtomicInteger();
        this.errors = new ConcurrentHashMap<>();
        this.totalErrors = new AtomicInteger();
        this.processedRecipients = new ConcurrentHashMap<>();
    }

    /**
     * 功能：创建 `NotificationRequestStats` 实例，并初始化必要字段。
     * 参数：
     * - `sent`：键值映射。
     * - `errors`：错误信息。
     * - `error`：错误信息。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public NotificationRequestStats(@JsonProperty("sent") Map<NotificationDeliveryMethod, AtomicInteger> sent,
                                    @JsonProperty("errors") Map<NotificationDeliveryMethod, Map<String, String>> errors,
                                    @JsonProperty("error") String error) {
        this.sent = sent;
        this.totalSent = null;
        this.errors = errors;
        this.totalErrors = null;
        this.error = error;
        this.processedRecipients = Collections.emptyMap();
    }

    /**
     * 功能：上报`Sent`。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipient`：`recipient` 参数。
     * 返回：无。
     */
    public void reportSent(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        sent.computeIfAbsent(deliveryMethod, k -> new AtomicInteger()).incrementAndGet();
        totalSent.incrementAndGet();
    }

    /**
     * 功能：上报错误信息。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `error`：错误信息。
     * - `recipient`：`recipient` 参数。
     * 返回：无。
     */
    public void reportError(NotificationDeliveryMethod deliveryMethod, Throwable error, NotificationRecipient recipient) {
        if (error instanceof AlreadySentException) {
            return;
        }
        String errorMessage = error.getMessage();
        if (errorMessage == null) {
            errorMessage = error.getClass().getSimpleName();
        }
        errors.computeIfAbsent(deliveryMethod, k -> new ConcurrentHashMap<>()).put(recipient.getTitle(), errorMessage);
        totalErrors.incrementAndGet();
    }

    /**
     * 功能：上报`Processed`。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：无。
     */
    public void reportProcessed(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        processedRecipients.computeIfAbsent(deliveryMethod, k -> ConcurrentHashMap.newKeySet()).add(recipientId);
    }

    /**
     * 功能：执行 `contains` 对应的处理。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * 返回：判断结果。
     */
    public boolean contains(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        Set<Object> processedRecipients = this.processedRecipients.get(deliveryMethod);
        return processedRecipients != null && processedRecipients.contains(recipientId);
    }

}
