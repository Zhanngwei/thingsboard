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

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.Getter;

import java.time.Duration;

/**
 * Created by ashvayka on 22.10.18.
 */
/**
 * 中文说明：
 * 1. `TbRateLimits` 是 ThingsBoard Common Message 中围绕 `Tb Rate Limits` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbRateLimits {
    /**
     * `bucket` 字段，保存当前对象的对应属性。
     */
    private final LocalBucket bucket;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @Getter
    private final String configuration;

    /**
     * 功能：创建 `TbRateLimits` 实例，并初始化必要字段。
     * 参数：
     * - `limitsConfiguration`：配置对象。
     * 返回：新创建的对象实例。
     */
    public TbRateLimits(String limitsConfiguration) {
        this(limitsConfiguration, false);
    }

    /**
     * 功能：创建 `TbRateLimits` 实例，并初始化必要字段。
     * 参数：
     * - `limitsConfiguration`：配置对象。
     * - `refillIntervally`：`refillIntervally` 参数。
     * 返回：新创建的对象实例。
     */
    public TbRateLimits(String limitsConfiguration, boolean refillIntervally) {
        LocalBucketBuilder builder = Bucket4j.builder();
        boolean initialized = false;
        for (String limitSrc : limitsConfiguration.split(",")) {
            long capacity = Long.parseLong(limitSrc.split(":")[0]);
            long duration = Long.parseLong(limitSrc.split(":")[1]);
            Refill refill = refillIntervally ? Refill.intervally(capacity, Duration.ofSeconds(duration)) : Refill.greedy(capacity, Duration.ofSeconds(duration));
            builder.addLimit(Bandwidth.classic(capacity, refill));
            initialized = true;
        }
        if (initialized) {
            bucket = builder.build();
        } else {
            throw new IllegalArgumentException("Failed to parse rate limits configuration: " + limitsConfiguration);
        }
        this.configuration = limitsConfiguration;
    }

    /**
     * 功能：执行 `tryConsume` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean tryConsume() {
        return bucket.tryConsume(1);
    }

    /**
     * 功能：执行 `tryConsume` 对应的处理。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：判断结果。
     */
    public boolean tryConsume(long number) {
        return bucket.tryConsume(number);
    }

}
