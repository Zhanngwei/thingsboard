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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. 类目的：`DefaultStatsFactory` 是ThingsBoard Common 模块中的统计指标契约类型，用于定义运行时统计项、计数器和持久化消息的数据结构。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Actor、Queue、Application 统计服务、监控和日志系统。
 * 4. 生命周期：由运行期采样、周期持久化或测试流程创建和消费。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / DTO。
 */
@Service
public class DefaultStatsFactory implements StatsFactory {
    /**
     * `TOTAL_MSGS`常量，用于统一引用固定值。
     */
    private static final String TOTAL_MSGS = "totalMsgs";
    private static final String SUCCESSFUL_MSGS = "successfulMsgs";
    /**
     * `FAILED_MSGS`常量，用于统一引用固定值。
     */
    private static final String FAILED_MSGS = "failedMsgs";

    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String STATS_NAME_TAG = "statsName";

    private static final Counter STUB_COUNTER = new StubCounter();

    /**
     * `meterRegistry` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 是否启用`metrics`。
     */
    @Value("${metrics.enabled:false}")
    private Boolean metricsEnabled;

    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    @Value("${metrics.timer.percentiles:0.5}")
    private String timerPercentilesStr;

    /**
     * 定时器列表，用于保存一组待处理对象。
     */
    private double[] timerPercentiles;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        if (!StringUtils.isEmpty(timerPercentilesStr)) {
            String[] split = timerPercentilesStr.split(",");
            timerPercentiles = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                timerPercentiles[i] = Double.parseDouble(split[i]);
            }
        }
    }


    /**
     * 功能：保存或创建计数器。
     * 参数：
     * - `key`：键。
     * - `statsName`：名称。
     * - `otherTags`：`otherTags` 参数。
     * 返回：处理结果。
     */
    @Override
    public StatsCounter createStatsCounter(String key, String statsName, String... otherTags) {
        String[] tags = new String[]{STATS_NAME_TAG, statsName};
        if (otherTags.length > 0) {
            if (otherTags.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid tags array size");
            }
            tags = ArrayUtils.addAll(tags, otherTags);
        }
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ? meterRegistry.counter(key, tags) : STUB_COUNTER,
                statsName
        );
    }

    /**
     * 功能：保存或创建计数器。
     * 参数：
     * - `key`：键。
     * - `tags`：`tags` 参数。
     * 返回：处理结果。
     */
    @Override
    public DefaultCounter createDefaultCounter(String key, String... tags) {
        return new DefaultCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, tags)
                        : STUB_COUNTER
        );
    }

    /**
     * 功能：保存或创建`Gauge`。
     * 参数：
     * - `key`：键。
     * - `number`：`number` 参数。
     * - `tags`：`tags` 参数。
     * 返回：处理结果。
     */
    @Override
    public <T extends Number> T createGauge(String key, T number, String... tags) {
        return meterRegistry.gauge(key, Tags.of(tags), number);
    }

    /**
     * 功能：保存或创建`Messages Stats`。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public MessagesStats createMessagesStats(String key) {
        StatsCounter totalCounter = createStatsCounter(key, TOTAL_MSGS);
        StatsCounter successfulCounter = createStatsCounter(key, SUCCESSFUL_MSGS);
        StatsCounter failedCounter = createStatsCounter(key, FAILED_MSGS);
        return new DefaultMessagesStats(totalCounter, successfulCounter, failedCounter);
    }

    /**
     * 功能：保存或创建定时器。
     * 参数：
     * - `key`：键。
     * - `tags`：`tags` 参数。
     * 返回：处理结果。
     */
    @Override
    public Timer createTimer(String key, String... tags) {
        Timer.Builder timerBuilder = Timer.builder(key)
                .tags(tags)
                .publishPercentiles();
        if (timerPercentiles != null && timerPercentiles.length > 0) {
            timerBuilder.publishPercentiles(timerPercentiles);
        }
        return timerBuilder.register(meterRegistry);
    }

    /**
     * 中文说明：
     * 1. 类目的：`StubCounter` 是ThingsBoard Common 模块中的统计指标契约类型，用于定义运行时统计项、计数器和持久化消息的数据结构。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括Actor、Queue、Application 统计服务、监控和日志系统。
     * 4. 生命周期：由运行期采样、周期持久化或测试流程创建和消费。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Observer / DTO。
     */
    private static class StubCounter implements Counter {
        /**
         * 功能：执行 `increment` 对应的处理。
         * 参数：
         * - `amount`：`amount` 参数。
         * 返回：无。
         */
        @Override
        public void increment(double amount) {
        }

        /**
         * 功能：执行 `count` 对应的处理。
         * 参数：无。
         * 返回：数值结果。
         */
        @Override
        public double count() {
            return 0;
        }

        /**
         * 功能：获取`Id`。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public Id getId() {
            return null;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultStatsFactory` 在 ThingsBoard Common 模块 中承担统计指标契约类型职责，核心目的是定义运行时统计项、计数器和持久化消息的数据结构。
 * 2. 核心流程：采集运行时指标后聚合为统计消息并交给持久化或监控流程。
 * 3. 关键依赖：主要依赖或协作对象包括Actor、Queue、Application 统计服务、监控和日志系统。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
