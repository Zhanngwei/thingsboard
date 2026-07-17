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
package org.thingsboard.server.service.notification.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultFirebaseService` 是 ThingsBoard Application 中负责 `Firebase` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `FirebaseService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
public class DefaultFirebaseService implements FirebaseService {

    private final Cache<String, FirebaseContext> contexts = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .<String, FirebaseContext>removalListener((key, context, cause) -> {
                if (cause == RemovalCause.EXPIRED && context != null) {
                    context.destroy();
                }
            })
            .build();

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `credentials`：`credentials` 参数。
     * - `fcmToken`：`fcmToken` 参数。
     * - `title`：`title` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void sendMessage(TenantId tenantId, String credentials, String fcmToken, String title, String body,
                            Map<String, String> data, Integer badge) throws FirebaseMessagingException {
        FirebaseContext firebaseContext = contexts.asMap().compute(tenantId.toString(), (key, context) -> {
            if (context == null) {
                return new FirebaseContext(key, credentials);
            } else {
                context.check(credentials);
                return context;
            }
        });

        Aps.Builder apsConfig = Aps.builder()
                .setSound("default");
        if (badge != null) {
            apsConfig.setBadge(badge);
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(apsConfig.build())
                        .build())
                .putAllData(data)
                .build();
        try {
            firebaseContext.getMessaging().send(message);
            log.trace("[{}] Sent message for FCM token {}", tenantId, fcmToken);
        } catch (Throwable t) {
            log.debug("[{}] Failed to send message for FCM token {}", tenantId, fcmToken, t);
            throw t;
        }
    }

    /**
     * 中文说明：
     * 1. `FirebaseContext` 是 ThingsBoard Application 中承载 `Firebase Context` 信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    public static class FirebaseContext {
        /**
         * 键，用于定位映射、配置或数据项。
         */
        private final String key;
        private String credentials;
        /**
         * `app` 字段，保存当前对象的对应属性。
         */
        private FirebaseApp app;
        /**
         * `messaging` 字段，保存当前对象的对应属性。
         */
        @Getter
        private FirebaseMessaging messaging;

        /**
         * 功能：创建 `DefaultFirebaseService` 实例，并初始化必要字段。
         * 参数：
         * - `key`：键。
         * - `credentials`：`credentials` 参数。
         * 返回：新创建的对象实例。
         */
        public FirebaseContext(String key, String credentials) {
            this.key = key;
            this.credentials = credentials;
            init();
        }

        /**
         * 功能：执行 `init` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        private void init() {
            FirebaseOptions options;
            try {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(IOUtils.toInputStream(credentials, StandardCharsets.UTF_8)))
                        .build();
            } catch (IOException e) {
                throw new RuntimeException("Failed to process service account credentials: " + e.getMessage(), e);
            }
            try {
                app = FirebaseApp.initializeApp(options, key);
            } catch (IllegalStateException alreadyExists) { // should never normally happen
                app = FirebaseApp.getInstance(key);
            }
            try {
                messaging = FirebaseMessaging.getInstance(app);
            } catch (IllegalStateException alreadyExists) { // should never normally happen
                messaging = FirebaseMessaging.getInstance(app);
            }
            log.debug("[{}] Initialized new FirebaseContext", key);
        }

        /**
         * 功能：执行 `check` 对应的处理。
         * 参数：
         * - `credentials`：`credentials` 参数。
         * 返回：无。
         */
        public void check(String credentials) {
            if (!this.credentials.equals(credentials)) {
                destroy();
                this.credentials = credentials;
                init();
            } else if (app == null || messaging == null) {
                throw new IllegalStateException("Firebase app couldn't be initialized");
            }
        }

        /**
         * 功能：执行 `destroy` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        public void destroy() {
            app.delete();
            app = null;
            messaging = null;
            log.debug("[{}] Destroyed FirebaseContext", key);
        }
    }

}
