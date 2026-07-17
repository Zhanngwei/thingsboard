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

import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.FstStatsService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `FstStatsServiceImpl` 是 ThingsBoard Common 中负责统计数据的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `FstStatsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
public class FstStatsServiceImpl implements FstStatsService {
    private final ConcurrentHashMap<String, StatsCounter> encodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StatsCounter> decodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> encodeTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> decodeTimer = new ConcurrentHashMap<>();

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Autowired
    private StatsFactory statsFactory;

    /**
     * 功能：执行 `incrementEncode` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    @Override
    public void incrementEncode(Class<?> clazz) {
        encodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fst_encode", key)).increment();
    }

    /**
     * 功能：执行 `incrementDecode` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    @Override
    public void incrementDecode(Class<?> clazz) {
        decodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fst_decode", key)).increment();
    }

    /**
     * 功能：执行 `recordEncodeTime` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `startTime`：开始时间戳。
     * 返回：无。
     */
    @Override
    public void recordEncodeTime(Class<?> clazz, long startTime) {
        encodeTimers.computeIfAbsent(clazz.getSimpleName(),
                key -> statsFactory.createTimer("fst_encode_time", "statsName", key)).record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    /**
     * 功能：执行 `recordDecodeTime` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `startTime`：开始时间戳。
     * 返回：无。
     */
    @Override
    public void recordDecodeTime(Class<?> clazz, long startTime) {
        decodeTimer.computeIfAbsent(clazz.getSimpleName(),
                key -> statsFactory.createTimer("fst_decode_time", "statsName", key)).record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

}
