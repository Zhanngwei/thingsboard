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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.service.security.ValidationCallback;

/**
 * Created by ashvayka on 21.02.17.
 */
/**
 * 中文说明：
 * 1. `HttpValidationCallback` 是 ThingsBoard Application 中处理 HTTP 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ValidationCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public class HttpValidationCallback extends ValidationCallback<DeferredResult<ResponseEntity>> {

    /**
     * 功能：创建 `HttpValidationCallback` 实例，并初始化必要字段。
     * 参数：
     * - `response`：响应对象。
     * - `action`：`action` 参数。
     * 返回：新创建的对象实例。
     */
    public HttpValidationCallback(DeferredResult<ResponseEntity> response, FutureCallback<DeferredResult<ResponseEntity>> action) {
       super(response, action);
    }

}
