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
package org.thingsboard.server.queue.util;

import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.FSTUtils;
import org.thingsboard.server.common.data.FstStatsService;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `ProtoWithFSTService` 是 ThingsBoard Common Queue 中负责 `Proto With FST` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `DataDecodingEncodingService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
public class ProtoWithFSTService implements DataDecodingEncodingService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private FstStatsService fstStatsService;

    public static final FSTConfiguration CONFIG = FSTConfiguration.createDefaultConfiguration();

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `byteArray`：`byteArray` 参数。
     * 返回：可能存在的结果。
     */
    @Override
    public <T> Optional<T> decode(byte[] byteArray) {
        try {
            long startTime = System.nanoTime();
            Optional<T> optional = Optional.ofNullable(FSTUtils.decode(byteArray));
            optional.ifPresent(obj -> {
                fstStatsService.recordDecodeTime(obj.getClass(), startTime);
                fstStatsService.incrementDecode(obj.getClass());
            });
            return optional;
        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
            return Optional.empty();
        }
    }


    /**
     * 功能：执行 `encode` 对应的处理。
     * 参数：
     * - `msq`：`msq` 参数。
     * 返回：处理结果。
     */
    @Override
    public <T> byte[] encode(T msq) {
        long startTime = System.nanoTime();
        var bytes = FSTUtils.encode(msq);
        fstStatsService.recordEncodeTime(msq.getClass(), startTime);
        fstStatsService.incrementEncode(msq.getClass());
        return bytes;
    }


}
