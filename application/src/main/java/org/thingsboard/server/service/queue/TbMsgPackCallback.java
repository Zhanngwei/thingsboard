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

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ExceptionUtil;
import org.thingsboard.server.common.data.exception.AbstractRateLimitException;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `TbMsgPackCallback` 是 ThingsBoard Application 中处理消息的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TbMsgCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class TbMsgPackCallback implements TbMsgCallback {
    /**
     * `id`ID，用于定位对应业务对象。
     */
    private final UUID id;
    private final TenantId tenantId;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbMsgPackProcessingContext ctx;
    private final long startMsgProcessing;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final Timer successfulMsgTimer;
    private final Timer failedMsgTimer;

    /**
     * 功能：创建 `TbMsgPackCallback` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `ctx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public TbMsgPackCallback(UUID id, TenantId tenantId, TbMsgPackProcessingContext ctx) {
        this(id, tenantId, ctx, null, null);
    }

    /**
     * 功能：创建 `TbMsgPackCallback` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `ctx`：处理上下文。
     * - `successfulMsgTimer`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbMsgPackCallback(UUID id, TenantId tenantId, TbMsgPackProcessingContext ctx, Timer successfulMsgTimer, Timer failedMsgTimer) {
        this.id = id;
        this.tenantId = tenantId;
        this.ctx = ctx;
        this.successfulMsgTimer = successfulMsgTimer;
        this.failedMsgTimer = failedMsgTimer;
        startMsgProcessing = System.currentTimeMillis();
    }

    /**
     * 功能：处理`on Success`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onSuccess() {
        log.trace("[{}] ON SUCCESS", id);
        if (successfulMsgTimer != null) {
            successfulMsgTimer.record(System.currentTimeMillis() - startMsgProcessing, TimeUnit.MILLISECONDS);
        }
        ctx.onSuccess(id);
    }

    /**
     * 功能：处理数量限制。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onRateLimit(RuleEngineException e) {
        log.debug("[{}] ON RATE LIMIT", id, e);
        //TODO notify tenant on rate limit
        if (failedMsgTimer != null) {
            failedMsgTimer.record(System.currentTimeMillis() - startMsgProcessing, TimeUnit.MILLISECONDS);
        }
        ctx.onSuccess(id);
    }
    
    /**
     * 功能：处理失败信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(RuleEngineException e) {
        if (ExceptionUtil.lookupExceptionInCause(e, AbstractRateLimitException.class) != null) {
            onRateLimit(e);
            return;
        }

        log.trace("[{}] ON FAILURE", id, e);
        if (failedMsgTimer != null) {
            failedMsgTimer.record(System.currentTimeMillis() - startMsgProcessing, TimeUnit.MILLISECONDS);
        }
        ctx.onFailure(tenantId, id, e);
    }

    /**
     * 功能：判断消息。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isMsgValid() {
        return !ctx.isCanceled();
    }

    /**
     * 功能：处理`on Processing Start`。
     * 参数：
     * - `ruleNodeInfo`：`ruleNodeInfo` 参数。
     * 返回：无。
     */
    @Override
    public void onProcessingStart(RuleNodeInfo ruleNodeInfo) {
        log.trace("[{}] ON PROCESSING START: {}", id, ruleNodeInfo);
        ctx.onProcessingStart(id, ruleNodeInfo);
    }

    /**
     * 功能：处理`on Processing End`。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    @Override
    public void onProcessingEnd(RuleNodeId ruleNodeId) {
        log.trace("[{}] ON PROCESSING END: {}", id, ruleNodeId);
        ctx.onProcessingEnd(id, ruleNodeId);
    }
}
