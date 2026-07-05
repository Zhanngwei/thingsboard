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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`TbRuleEngineProcessingStrategyFactory` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Component
@Slf4j
public class TbRuleEngineProcessingStrategyFactory {

    /**
     * 功能：执行 `newInstance` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `processingStrategy`：`processingStrategy` 参数。
     * 返回：处理结果。
     */
    public TbRuleEngineProcessingStrategy newInstance(String name, ProcessingStrategy processingStrategy) {
        switch (processingStrategy.getType()) {
            case SKIP_ALL_FAILURES:
                return new SkipStrategy(name, false);
            case SKIP_ALL_FAILURES_AND_TIMED_OUT:
                return new SkipStrategy(name, true);
            case RETRY_ALL:
                return new RetryStrategy(name, true, true, true, processingStrategy);
            case RETRY_FAILED:
                return new RetryStrategy(name, false, true, false, processingStrategy);
            case RETRY_TIMED_OUT:
                return new RetryStrategy(name, false, false, true, processingStrategy);
            case RETRY_FAILED_AND_TIMED_OUT:
                return new RetryStrategy(name, false, true, true, processingStrategy);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + processingStrategy.getType() + " is not supported!");
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`RetryStrategy` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
     * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
     */
    private static class RetryStrategy implements TbRuleEngineProcessingStrategy {
        /**
         * 队列名称，用于标识或展示当前对象。
         */
        private final String queueName;
        private final boolean retrySuccessful;
        /**
         * 当前操作是否失败。
         */
        private final boolean retryFailed;
        private final boolean retryTimeout;
        /**
         * `maxRetries` 字段，保存当前对象的对应属性。
         */
        private final int maxRetries;
        private final double maxAllowedFailurePercentage;
        /**
         * `maxPauseBetweenRetries` 字段，保存当前对象的对应属性。
         */
        private final long maxPauseBetweenRetries;

        /**
         * `pauseBetweenRetries` 字段，保存当前对象的对应属性。
         */
        private long pauseBetweenRetries;

        /**
         * 数量，用于控制数量、位置或分页范围。
         */
        private int initialTotalCount;
        private int retryCount;

        /**
         * 功能：创建 `TbRuleEngineProcessingStrategyFactory` 实例，并初始化必要字段。
         * 参数：
         * - `queueName`：队列名称或队列对象。
         * - `retrySuccessful`：`retrySuccessful` 参数。
         * - `retryFailed`：`retryFailed` 参数。
         * - `retryTimeout`：`retryTimeout` 参数。
         * - 其余参数：补充处理条件。
         * 返回：新创建的对象实例。
         */
        public RetryStrategy(String queueName, boolean retrySuccessful, boolean retryFailed, boolean retryTimeout, ProcessingStrategy processingStrategy) {
            this.queueName = queueName;
            this.retrySuccessful = retrySuccessful;
            this.retryFailed = retryFailed;
            this.retryTimeout = retryTimeout;
            this.maxRetries = processingStrategy.getRetries();
            this.maxAllowedFailurePercentage = processingStrategy.getFailurePercentage();
            this.pauseBetweenRetries = processingStrategy.getPauseBetweenRetries();
            this.maxPauseBetweenRetries = processingStrategy.getMaxPauseBetweenRetries();
        }

        /**
         * 功能：判断超时时间。
         * 参数：无。
         * 返回：判断结果。
         */
        @Override
        public boolean isSkipTimeoutMsgs() {
            return true;
        }

        /**
         * 功能：执行 `analyze` 对应的处理。
         * 参数：
         * - `result`：`result` 参数。
         * 返回：处理结果。
         */
        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (result.isSuccess()) {
                log.trace("[{}] The result of the msg pack processing is successful, going to proceed with processing of the following msgs", queueName);
                return new TbRuleEngineProcessingDecision(true, null);
            } else {
                if (retryCount == 0) {
                    initialTotalCount = result.getPendingMap().size() + result.getFailedMap().size() + result.getSuccessMap().size();
                }
                retryCount++;
                double failedCount = result.getFailedMap().size() + result.getPendingMap().size();
                if (maxRetries > 0 && retryCount > maxRetries) {
                    log.debug("[{}] Skip reprocess of the rule engine pack due to max retries", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else if (maxAllowedFailurePercentage > 0 && (failedCount / initialTotalCount) > maxAllowedFailurePercentage) {
                    log.debug("[{}] Skip reprocess of the rule engine pack due to max allowed failure percentage", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else {
                    log.debug("[{}] The result of msg pack processing is unsuccessful, checking unprocessed msgs and going to reprocess them", queueName);
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toReprocess = new ConcurrentHashMap<>(initialTotalCount);
                    if (retryFailed) {
                        result.getFailedMap().forEach(toReprocess::put);
                    } else if (log.isDebugEnabled() && !result.getFailedMap().isEmpty()) {
                        log.debug("[{}] Skipped {} failed messages due to the processing strategy configuration", queueName, result.getFailedMap().size());
                    }
                    if (retryTimeout) {
                        result.getPendingMap().forEach(toReprocess::put);
                    } else if (log.isDebugEnabled() && !result.getPendingMap().isEmpty()) {
                        log.debug("[{}] Skipped {} timedOut messages due to the processing strategy configuration", queueName, result.getPendingMap().size());
                    }
                    if (retrySuccessful) {
                        result.getSuccessMap().forEach(toReprocess::put);
                    } else if (log.isTraceEnabled() && !result.getSuccessMap().isEmpty()) {
                        log.trace("[{}] Skipped {} successful messages due to the processing strategy configuration", queueName, result.getSuccessMap().size());
                    }
                    if (CollectionUtils.isEmpty(toReprocess)) {
                        if (log.isDebugEnabled()) {
                            log.debug("[{}] Stopping the reprocessing logic due to reprocessing map is empty", queueName);
                        }
                        return new TbRuleEngineProcessingDecision(true, null);
                    }
                    log.debug("[{}] Going to reprocess {} messages", queueName, toReprocess.size());
                    if (log.isTraceEnabled()) {
                        toReprocess.forEach((id, msg) -> log.trace("Going to reprocess [{}]: {}", id, TbMsg.fromBytes(result.getQueueName(), msg.getValue().getTbMsg().toByteArray(), TbMsgCallback.EMPTY)));
                    }
                    if (pauseBetweenRetries > 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pauseBetweenRetries));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (maxPauseBetweenRetries > pauseBetweenRetries) {
                            pauseBetweenRetries = Math.min(maxPauseBetweenRetries, pauseBetweenRetries * 2);
                        }
                    }
                    return new TbRuleEngineProcessingDecision(false, toReprocess);
                }
            }
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`SkipStrategy` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
     * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
     */
    private static class SkipStrategy implements TbRuleEngineProcessingStrategy {

        /**
         * 队列名称，用于标识或展示当前对象。
         */
        private final String queueName;
        private final boolean skipTimeoutMsgs;

        /**
         * 功能：创建 `TbRuleEngineProcessingStrategyFactory` 实例，并初始化必要字段。
         * 参数：
         * - `name`：名称。
         * - `skipTimeoutMsgs`：待处理消息。
         * 返回：新创建的对象实例。
         */
        public SkipStrategy(String name, boolean skipTimeoutMsgs) {
            this.queueName = name;
            this.skipTimeoutMsgs = skipTimeoutMsgs;
        }

        /**
         * 功能：判断超时时间。
         * 参数：无。
         * 返回：判断结果。
         */
        @Override
        public boolean isSkipTimeoutMsgs() {
            return skipTimeoutMsgs;
        }

        /**
         * 功能：执行 `analyze` 对应的处理。
         * 参数：
         * - `result`：`result` 参数。
         * 返回：处理结果。
         */
        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (!result.isSuccess()) {
                log.debug("[{}] Reprocessing skipped for {} failed and {} timeout messages", queueName, result.getFailedMap().size(), result.getPendingMap().size());
            }
            if (log.isTraceEnabled()) {
                result.getFailedMap().forEach((id, msg) -> log.trace("Failed messages [{}]: {}", id, TbMsg.fromBytes(result.getQueueName(), msg.getValue().getTbMsg().toByteArray(), TbMsgCallback.EMPTY)));
            }
            if (log.isTraceEnabled()) {
                result.getPendingMap().forEach((id, msg) -> log.trace("Timeout messages [{}]: {}", id, TbMsg.fromBytes(result.getQueueName(), msg.getValue().getTbMsg().toByteArray(), TbMsgCallback.EMPTY)));
            }
            return new TbRuleEngineProcessingDecision(true, null);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbRuleEngineProcessingStrategyFactory` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
