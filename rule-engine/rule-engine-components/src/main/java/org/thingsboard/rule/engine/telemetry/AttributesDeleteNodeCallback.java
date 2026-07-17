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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 中文说明：
 * 1. `AttributesDeleteNodeCallback` 是 ThingsBoard Rule Engine Components 中处理 `Attributes Delete Node` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TelemetryNodeCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class AttributesDeleteNodeCallback extends TelemetryNodeCallback {

    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    private String scope;
    /**
     * `keys`列表，用于保存一组待处理对象。
     */
    private List<String> keys;

    /**
     * 功能：创建 `AttributesDeleteNodeCallback` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：新创建的对象实例。
     */
    public AttributesDeleteNodeCallback(TbContext ctx, TbMsg msg, String scope, List<String> keys) {
        super(ctx, msg);
        this.scope = scope;
        this.keys = keys;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(@Nullable Void result) {
        TbContext ctx = this.getCtx();
        TbMsg tbMsg = this.getMsg();
        ctx.enqueue(ctx.attributesDeletedActionMsg(tbMsg.getOriginator(), ctx.getSelfId(), scope, keys),
                () -> ctx.tellSuccess(tbMsg),
                throwable -> ctx.tellFailure(tbMsg, throwable));
    }
}
