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
package org.thingsboard.server.msa.ui.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.thingsboard.server.msa.DisableUIListeners;

/**
 * 中文说明：
 * 1. `RetryAnalyzer` 是 ThingsBoard Microservices 中围绕 `Retry Analyzer` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `IRetryAnalyzer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 2;

    /**
     * 功能：执行 `retry` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean retry(ITestResult result) {
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return false;
        }
        if (retryCount < MAX_RETRY_COUNT) {
            System.out.printf("Retrying test %s for the %d time(s).%n", result.getName(), retryCount + 1);
            retryCount++;
            return true;
        }
        return false;
    }
}
