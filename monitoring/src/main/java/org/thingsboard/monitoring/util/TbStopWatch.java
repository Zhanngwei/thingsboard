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
package org.thingsboard.monitoring.util;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 中文说明：
 * 1. `TbStopWatch` 是 ThingsBoard Monitoring 中围绕 `Tb Stop Watch` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TbStopWatch {

    private final StopWatch internal = new StopWatch();

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void start() {
        internal.reset();
        internal.start();
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getTime() {
        internal.stop();
        long nanoTime = internal.getNanoTime();
        internal.reset();
        return nanoTime;
    }

}
