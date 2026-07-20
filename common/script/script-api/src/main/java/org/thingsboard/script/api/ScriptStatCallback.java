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
package org.thingsboard.script.api;

import com.google.common.util.concurrent.FutureCallback;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `ScriptStatCallback` 是 ThingsBoard Common 中处理脚本执行的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `FutureCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
@AllArgsConstructor
public class ScriptStatCallback<T> implements FutureCallback<T> {

    /**
     * `successMsgs` 字段，保存当前对象的对应属性。
     */
    private final AtomicInteger successMsgs;
    private final AtomicInteger timeoutMsgs;
    /**
     * `failedMsgs` 字段，保存当前对象的对应属性。
     */
    private final AtomicInteger failedMsgs;

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(@Nullable T result) {
        successMsgs.incrementAndGet();
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable t) {
        if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
            timeoutMsgs.incrementAndGet();
        } else {
            failedMsgs.incrementAndGet();
        }
    }
}
