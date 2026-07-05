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
 * 1. 类目的：`DefaultFirebaseService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
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
     * 1. 类目的：`FirebaseContext` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultFirebaseService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
