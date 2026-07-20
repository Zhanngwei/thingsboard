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
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. `TbMsgPackCallbackTest` 是 ThingsBoard Application 中验证 `TbMsgPackCallback` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
class TbMsgPackCallbackTest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    TenantId tenantId;
    UUID msgId;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    TbMsgPackProcessingContext ctx;
    TbMsgPackCallback callback;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        msgId = UUID.randomUUID();
        ctx = mock(TbMsgPackProcessingContext.class);
        callback = spy(new TbMsgPackCallback(msgId, tenantId, ctx));
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> testOnFailure_NotRateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("rule engine no cause")),
                Arguments.of(new RuleEngineException("rule engine caused 1 lvl", new RuntimeException())),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl", new RuntimeException(new Exception()))),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl Throwable", new RuntimeException(new Throwable()))),
                Arguments.of(new RuleNodeException("rule node no cause", "RuleChain", new RuleNode()))
        );
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：
     * - `ree`：`ree` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    void testOnFailure_NotRateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback, never()).onRateLimit(any());
        verify(callback, never()).onSuccess();
        verify(ctx, never()).onSuccess(any());
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> testOnFailure_RateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("caused lvl 1", new TbRateLimitsException(EntityType.ASSET))),
                Arguments.of(new RuleEngineException("caused lvl 2", new RuntimeException(new TbRateLimitsException(EntityType.ASSET)))),
                Arguments.of(
                        new RuleEngineException("caused lvl 3",
                                new RuntimeException(
                                        new Exception(
                                                new TbRateLimitsException(EntityType.ASSET)))))
        );
    }

    /**
     * 功能：验证数量限制相关场景。
     * 参数：
     * - `ree`：`ree` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    void testOnFailure_RateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback).onRateLimit(any());
        verify(callback).onFailure(any());
        verify(callback, never()).onSuccess();
        verify(ctx).onSuccess(msgId);
        verify(ctx).onSuccess(any());
        verify(ctx, never()).onFailure(any(), any(), any());
    }

}
