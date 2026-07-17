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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `SystemUtil` 是 ThingsBoard Common 中处理 `System Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class SystemUtil {

    /**
     * `HARDWARE`常量，用于统一引用固定值。
     */
    private static final HardwareAbstractionLayer HARDWARE;

    static {
        HARDWARE = new SystemInfo().getHardware();
    }

    /**
     * 功能：获取`Memory Usage`。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public static Optional<Integer> getMemoryUsage() {
        try {
            GlobalMemory memory = HARDWARE.getMemory();
            long total = memory.getTotal();
            long available = memory.getAvailable();
            return Optional.of(toPercent(total - available, total));
        } catch (Throwable e) {
            log.debug("Failed to get memory usage!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取`Total Memory`。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public static Optional<Long> getTotalMemory() {
        try {
            return Optional.of(HARDWARE.getMemory().getTotal());
        } catch (Throwable e) {
            log.debug("Failed to get total memory!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取`Cpu Usage`。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public static Optional<Integer> getCpuUsage() {
        try {
            return Optional.of((int) (HARDWARE.getProcessor().getSystemCpuLoad(1000) * 100.0));
        } catch (Throwable e) {
            log.debug("Failed to get cpu usage!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取数量。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public static Optional<Integer> getCpuCount() {
        try {
            return Optional.of(HARDWARE.getProcessor().getLogicalProcessorCount());
        } catch (Throwable e) {
            log.debug("Failed to get total cpu count!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取`Disc Space Usage`。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public static Optional<Integer> getDiscSpaceUsage() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            long total = store.getTotalSpace();
            long available = store.getUsableSpace();
            return Optional.of(toPercent(total - available, total));
        } catch (Throwable e) {
            log.debug("Failed to get free disc space!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取`Total Disc Space`。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public static Optional<Long> getTotalDiscSpace() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            return Optional.of(store.getTotalSpace());
        } catch (Throwable e) {
            log.debug("Failed to get total disc space!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 功能：执行 `toPercent` 对应的处理。
     * 参数：
     * - `used`：`used` 参数。
     * - `total`：`total` 参数。
     * 返回：数值结果。
     */
    private static int toPercent(long used, long total) {
        BigDecimal u = new BigDecimal(used);
        BigDecimal t = new BigDecimal(total);
        BigDecimal i = new BigDecimal(100);
        return u.multiply(i).divide(t, RoundingMode.HALF_UP).intValue();
    }
}
