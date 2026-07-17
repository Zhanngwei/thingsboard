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
package org.thingsboard.server.service.executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AbstractListeningExecutor;

/**
 * 中文说明：
 * 1. `VersionControlExecutor` 是 ThingsBoard Application 中处理版本控制的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractListeningExecutor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
public class VersionControlExecutor extends AbstractListeningExecutor {

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${vc.thread_pool_size:6}")
    private int threadPoolSize;

    /**
     * 功能：获取`Thread Poll Size`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected int getThreadPollSize() {
        return threadPoolSize;
    }
}
