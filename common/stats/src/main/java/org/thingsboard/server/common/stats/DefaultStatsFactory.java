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
 * 1. `DefaultStatsFactory` 是 ThingsBoard Common 中创建或提供统计数据对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `StatsFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
     * 1. `StubCounter` 是 ThingsBoard Common 中围绕 `Stub Counter` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 直接依赖的类型边界包括 `Counter`。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
