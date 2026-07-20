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
package org.thingsboard.server.queue.common;

import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `SimpleTbQueueCallback` 是 ThingsBoard Common Queue 中处理队列的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TbQueueCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public class SimpleTbQueueCallback implements TbQueueCallback {

    /**
     * `onSuccess` 字段，保存当前对象的对应属性。
     */
    private final Consumer<TbQueueMsgMetadata> onSuccess;
    private final Consumer<Throwable> onFailure;

    /**
     * 功能：创建 `SimpleTbQueueCallback` 实例，并初始化必要字段。
     * 参数：
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：新创建的对象实例。
     */
    public SimpleTbQueueCallback(Consumer<TbQueueMsgMetadata> onSuccess, Consumer<Throwable> onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `metadata`：待处理数据。
     * 返回：无。
     */
    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        if (onSuccess != null) {
            onSuccess.accept(metadata);
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable t) {
        if (onFailure != null) {
            onFailure.accept(t);
        }
    }

}
