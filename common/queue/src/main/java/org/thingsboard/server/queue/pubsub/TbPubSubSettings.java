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
 * 1. `TbPubSubSettings` 是 ThingsBoard Common Queue 中描述 `Tb Pub Sub` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
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
