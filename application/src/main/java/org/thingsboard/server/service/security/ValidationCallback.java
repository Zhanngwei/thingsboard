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
package org.thingsboard.server.service.security;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.exception.AccessDeniedException;
import org.thingsboard.server.exception.EntityNotFoundException;
import org.thingsboard.server.exception.InternalErrorException;
import org.thingsboard.server.exception.UnauthorizedException;

/**
 * Created by ashvayka on 31.03.18.
 */
/**
 * 中文说明：
 * 1. `ValidationCallback` 是 ThingsBoard Application 中处理 `Validation Callback` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `FutureCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public class ValidationCallback<T> implements FutureCallback<ValidationResult> {

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    private final T response;
    private final FutureCallback<T> action;

    /**
     * 功能：创建 `ValidationCallback` 实例，并初始化必要字段。
     * 参数：
     * - `response`：响应对象。
     * - `action`：`action` 参数。
     * 返回：新创建的对象实例。
     */
    public ValidationCallback(T response, FutureCallback<T> action) {
        this.response = response;
        this.action = action;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(ValidationResult result) {
        if (result.getResultCode() == ValidationResultCode.OK) {
            action.onSuccess(response);
        } else {
            onFailure(getException(result));
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable e) {
        action.onFailure(e);
    }

    /**
     * 功能：获取`Exception`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    public static Exception getException(ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        Exception e;
        switch (resultCode) {
            case ENTITY_NOT_FOUND:
                e = new EntityNotFoundException(result.getMessage());
                break;
            case UNAUTHORIZED:
                e = new UnauthorizedException(result.getMessage());
                break;
            case ACCESS_DENIED:
                e = new AccessDeniedException(result.getMessage());
                break;
            case INTERNAL_ERROR:
                e = new InternalErrorException(result.getMessage());
                break;
            default:
                e = new UnauthorizedException("Permission denied.");
                break;
        }
        return e;
    }

}
