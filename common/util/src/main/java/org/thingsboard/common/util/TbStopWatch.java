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

import org.springframework.util.StopWatch;

/**
 * Utility method that extends Spring Framework StopWatch
 * It is a MONOTONIC time stopwatch.
 * It is a replacement for any measurements with a wall-clock like System.currentTimeMillis()
 * It is not affected by leap second, day-light saving and wall-clock adjustments by manual or network time synchronization
 * The main features is a single call for common use cases:
 *  - create and start: TbStopWatch sw = TbStopWatch.startNew()
 *  - stop and get: sw.stopAndGetTotalTimeMillis() or sw.stopAndGetLastTaskTimeMillis()
 * */
/**
 * 中文说明：
 * 1. `TbStopWatch` 是 ThingsBoard Common 中围绕 `Tb Stop Watch` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `StopWatch`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbStopWatch extends StopWatch {

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static TbStopWatch create(){
        TbStopWatch stopWatch = new TbStopWatch();
        stopWatch.start();
        return stopWatch;
    }

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：
     * - `taskName`：名称。
     * 返回：处理结果。
     */
    public static TbStopWatch create(String taskName){
        TbStopWatch stopWatch = new TbStopWatch();
        stopWatch.start(taskName);
        return stopWatch;
    }

    /**
     * 功能：初始化或启动`New`。
     * 参数：
     * - `taskName`：名称。
     * 返回：无。
     */
    public void startNew(String taskName){
        stop();
        start(taskName);
    }

    /**
     * 功能：停止或关闭时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long stopAndGetTotalTimeMillis(){
        stop();
        return getTotalTimeMillis();
    }

    /**
     * 功能：停止或关闭时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long stopAndGetTotalTimeNanos(){
        stop();
        return getLastTaskTimeNanos();
    }

    /**
     * 功能：停止或关闭时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long stopAndGetLastTaskTimeMillis(){
        stop();
        return getLastTaskTimeMillis();
    }

    /**
     * 功能：停止或关闭时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long stopAndGetLastTaskTimeNanos(){
        stop();
        return getLastTaskTimeNanos();
    }

}
