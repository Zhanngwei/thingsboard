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
package org.thingsboard.server.transport.lwm2m.server.downlink;

/**
 * 中文说明：
 * 1. `DownlinkRequestCallback` 是 ThingsBoard Common Transport 中定义请求能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DownlinkRequestCallback<R, T> {

    /**
     * 功能：处理`on Sent`。
     * 参数：
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    default boolean onSent(R request) {
        return true;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：无。
     */
    void onSuccess(R request, T response);

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `params`：`params` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void onValidationError(String params, String msg);

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `params`：`params` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    void onError(String params, Exception e);

}
