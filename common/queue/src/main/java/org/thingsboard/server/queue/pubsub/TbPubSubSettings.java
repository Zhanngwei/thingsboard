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
package org.thingsboard.server.queue.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`TbPubSubSettings` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
@Component
@Data
public class TbPubSubSettings {

    /**
     * `projectId`ID，用于定位对应业务对象。
     */
    @Value("${queue.pubsub.project_id}")
    private String projectId;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Value("${queue.pubsub.service_account}")
    private String serviceAccount;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Value("${queue.pubsub.max_msg_size}")
    private int maxMsgSize;

    /**
     * `maxMessages` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.pubsub.max_messages}")
    private int maxMessages;

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${queue.pubsub.executor_thread_pool_size:0}")
    private int threadPoolSize;

    /**
     * Refers to com.google.cloud.pubsub.v1.Publisher default executor configuration
     */
    /**
     * `THREADS_PER_CPU`常量，用于统一引用固定值。
     */
    private static final int THREADS_PER_CPU = 5;

    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private FixedExecutorProvider executorProvider;

    /**
     * 凭据，用于按场景创建或提供目标对象。
     */
    private CredentialsProvider credentialsProvider;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() throws IOException {
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(serviceAccount.getBytes()));
        credentialsProvider = FixedCredentialsProvider.create(credentials);
        if (threadPoolSize == 0) {
            threadPoolSize = THREADS_PER_CPU * Runtime.getRuntime().availableProcessors();
        }
        executorProvider = FixedExecutorProvider
                .create(Executors.newScheduledThreadPool(threadPoolSize, ThingsBoardThreadFactory.forName("pubsub-queue-executor")));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (executorProvider != null) {
            executorProvider.getExecutor().shutdownNow();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbPubSubSettings` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
