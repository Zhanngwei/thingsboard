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

@Component
@Slf4j
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
public class TbRuleEngineProcessingStrategyFactory {

    /**
     * 方法说明：
     * 1. 职责：执行 `newInstance` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbRuleEngineProcessingStrategy newInstance(String name, ProcessingStrategy processingStrategy) {
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
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
         * 字段说明：
         * 1. 保存 `queueName` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final String queueName;
        private final boolean retrySuccessful;
        /**
         * 字段说明：
         * 1. 保存 `retryFailed` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final boolean retryFailed;
        private final boolean retryTimeout;
        /**
         * 字段说明：
         * 1. 保存 `maxRetries` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final int maxRetries;
        private final double maxAllowedFailurePercentage;
        /**
         * 字段说明：
         * 1. 保存 `maxPauseBetweenRetries` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final long maxPauseBetweenRetries;

        /**
         * 字段说明：
         * 1. 保存 `pauseBetweenRetries` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private long pauseBetweenRetries;

        /**
         * 字段说明：
         * 1. 保存 `initialTotalCount` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private int initialTotalCount;
        private int retryCount;

        /**
         * 方法说明：
         * 1. 职责：执行 `RetryStrategy` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `isSkipTimeoutMsgs` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public boolean isSkipTimeoutMsgs() {
            return true;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `analyze` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (result.isSuccess()) {
                log.trace("[{}] The result of the msg pack processing is successful, going to proceed with processing of the following msgs", queueName);
                return new TbRuleEngineProcessingDecision(true, null);
            } else {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (retryCount == 0) {
                    initialTotalCount = result.getPendingMap().size() + result.getFailedMap().size() + result.getSuccessMap().size();
                }
                retryCount++;
                double failedCount = result.getFailedMap().size() + result.getPendingMap().size();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (maxRetries > 0 && retryCount > maxRetries) {
                    log.debug("[{}] Skip reprocess of the rule engine pack due to max retries", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (maxAllowedFailurePercentage > 0 && (failedCount / initialTotalCount) > maxAllowedFailurePercentage) {
                    log.debug("[{}] Skip reprocess of the rule engine pack due to max allowed failure percentage", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else {
                    log.debug("[{}] The result of msg pack processing is unsuccessful, checking unprocessed msgs and going to reprocess them", queueName);
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toReprocess = new ConcurrentHashMap<>(initialTotalCount);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (retryFailed) {
                        result.getFailedMap().forEach(toReprocess::put);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    } else if (log.isDebugEnabled() && !result.getFailedMap().isEmpty()) {
                        log.debug("[{}] Skipped {} failed messages due to the processing strategy configuration", queueName, result.getFailedMap().size());
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (retryTimeout) {
                        result.getPendingMap().forEach(toReprocess::put);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    } else if (log.isDebugEnabled() && !result.getPendingMap().isEmpty()) {
                        log.debug("[{}] Skipped {} timedOut messages due to the processing strategy configuration", queueName, result.getPendingMap().size());
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (retrySuccessful) {
                        result.getSuccessMap().forEach(toReprocess::put);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
         * 字段说明：
         * 1. 保存 `queueName` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final String queueName;
        private final boolean skipTimeoutMsgs;

        /**
         * 方法说明：
         * 1. 职责：执行 `SkipStrategy` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public SkipStrategy(String name, boolean skipTimeoutMsgs) {
            this.queueName = name;
            this.skipTimeoutMsgs = skipTimeoutMsgs;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `isSkipTimeoutMsgs` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public boolean isSkipTimeoutMsgs() {
            return skipTimeoutMsgs;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `analyze` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
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
