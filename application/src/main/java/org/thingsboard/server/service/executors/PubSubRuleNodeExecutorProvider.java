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
package org.thingsboard.server.service.executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ExecutorProvider;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 中文说明：
 * 1. `PubSubRuleNodeExecutorProvider` 是 ThingsBoard Application 中创建或提供规则节点对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `ExecutorProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Lazy
@TbRuleEngineComponent
@Component
public class PubSubRuleNodeExecutorProvider implements ExecutorProvider {

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${service.rule_engine.pubsub.executor_thread_pool_size}")
    private Integer threadPoolSize;

    /**
    * Refers to com.google.cloud.pubsub.v1.Publisher default executor configuration
    */
    /**
     * `THREADS_PER_CPU`常量，用于统一引用固定值。
     */
    private static final int THREADS_PER_CPU = 5;
    private ScheduledExecutorService executor;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        if (threadPoolSize == null) {
            threadPoolSize = THREADS_PER_CPU * Runtime.getRuntime().availableProcessors();
        }
        executor = Executors.newScheduledThreadPool(threadPoolSize, ThingsBoardThreadFactory.forName("pubsub-rule-nodes"));
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
