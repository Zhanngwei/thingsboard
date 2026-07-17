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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * 中文说明：
 * 1. `TestListener` 是 ThingsBoard Microservices 中处理 `Listener` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ITestListener`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class TestListener implements ITestListener {

    /**
     * `driver` 字段，保存当前对象的对应属性。
     */
    WebDriver driver;

    /**
     * 功能：处理`on Test Start`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onTestStart(ITestResult result) {
        log.info("===>>> Test started: " + result.getName());
    }

    /**
     * Invoked when a test succeeds
     */
    /**
     * 功能：处理`on Test Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<<=== Test completed successfully: " + result.getName());

    }

    /**
     * Invoked when a test fails
     */
    /**
     * 功能：处理失败信息。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.info("<<<=== Test failed: " + result.getName());
    }

    /**
     * Invoked when a test skipped
     */
    /**
     * 功能：处理`on Test Skipped`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.info("<<<=== Test skipped: " + result.getName());
    }
}
