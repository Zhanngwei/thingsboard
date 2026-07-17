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
package org.thingsboard.server.common.msg.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;

/**
 * 中文说明：
 * 1. `RuleEngineException` 是 ThingsBoard Common Message 中表示 `Rule Engine` 失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `Exception`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
@Slf4j
public class RuleEngineException extends Exception {
    protected static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    private final long ts;

    /**
     * 功能：创建 `RuleEngineException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public RuleEngineException(String message) {
        this(message, null);
    }

    /**
     * 功能：创建 `RuleEngineException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleEngineException(String message, Throwable t) {
        super(message != null ? message : "Unknown", t);
        ts = System.currentTimeMillis();
    }

    /**
     * 功能：执行 `toJsonString` 对应的处理。
     * 参数：
     * - `maxMessageLength`：待处理消息。
     * 返回：文本结果。
     */
    public String toJsonString(int maxMessageLength) {
        try {
            return mapper.writeValueAsString(mapper.createObjectNode()
                    .put("message", truncateIfNecessary(getMessage(), maxMessageLength)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exception ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `truncateIfNecessary` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * - `maxMessageLength`：待处理消息。
     * 返回：文本结果。
     */
    protected String truncateIfNecessary(String message, int maxMessageLength) {
        return StringUtils.truncate(message, maxMessageLength);
    }

}
