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
package org.thingsboard.server.common.msg.tools;

import org.awaitility.pollinterval.FixedPollInterval;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 中文说明：
 * 1. 类目的：`RateLimitsTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class RateLimitsTest {

    /**
     * 功能：验证频率相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRateLimits_greedyRefill() {
        testRateLimitWithGreedyRefill(3, 10);
        testRateLimitWithGreedyRefill(3, 3);
        testRateLimitWithGreedyRefill(4, 2);
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：
     * - `capacity`：`capacity` 参数。
     * - `period`：`period` 参数。
     * 返回：无。
     */
    private void testRateLimitWithGreedyRefill(int capacity, int period) {
        String rateLimitConfig = capacity + ":" + period;
        TbRateLimits rateLimits = new TbRateLimits(rateLimitConfig);

        rateLimits.tryConsume(capacity);
        assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();

        int expectedRefillTime = (int) (((double) period / capacity) * 1000);
        int gap = 500;

        for (int i = 0; i < capacity; i++) {
            await("token refill for rate limit " + rateLimitConfig)
                    .pollInterval(new FixedPollInterval(10, TimeUnit.MILLISECONDS))
                    .atLeast(expectedRefillTime - gap, TimeUnit.MILLISECONDS)
                    .atMost(expectedRefillTime + gap, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(rateLimits.tryConsume()).as("token is available").isTrue();
                    });
            assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();
        }
    }

    /**
     * 功能：验证时间间隔相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRateLimits_intervalRefill() {
        testRateLimitWithIntervalRefill(10, 5);
        testRateLimitWithIntervalRefill(3, 3);
        testRateLimitWithIntervalRefill(4, 2);
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：
     * - `capacity`：`capacity` 参数。
     * - `period`：`period` 参数。
     * 返回：无。
     */
    private void testRateLimitWithIntervalRefill(int capacity, int period) {
        String rateLimitConfig = capacity + ":" + period;
        TbRateLimits rateLimits = new TbRateLimits(rateLimitConfig, true);

        rateLimits.tryConsume(capacity);
        assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();

        int expectedRefillTime = period * 1000;
        int gap = 500;

        await("tokens refill for rate limit " + rateLimitConfig)
                .pollInterval(new FixedPollInterval(10, TimeUnit.MILLISECONDS))
                .atLeast(expectedRefillTime - gap, TimeUnit.MILLISECONDS)
                .atMost(expectedRefillTime + gap, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    for (int i = 0; i < capacity; i++) {
                        assertThat(rateLimits.tryConsume()).as("token is available").isTrue();
                    }
                    assertThat(rateLimits.tryConsume()).as("new token is available").isFalse();
                });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RateLimitsTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
