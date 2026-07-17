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
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

/**
 * 中文说明：
 * 1. `LoginPageElements` 是 ThingsBoard Microservices 中围绕 `Login Page Elements` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractBasePage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class LoginPageElements extends AbstractBasePage {
    /**
     * 功能：创建 `LoginPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public LoginPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 邮箱常量，用于统一引用固定值。
     */
    private static final String EMAIL_FIELD = "//input[@id='username-input']";
    private static final String PASSWORD_FIELD = "//input[@id='password-input']";
    /**
     * `SUBMIT_BTN`常量，用于统一引用固定值。
     */
    private static final String SUBMIT_BTN = "//button[@type='submit']";
    private static final String TITLE_LOGO = "//img[@class='tb-logo-title']";

    /**
     * 功能：执行 `emailField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement emailField() {
        return waitUntilElementToBeClickable(EMAIL_FIELD);
    }

    /**
     * 功能：执行 `passwordField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement passwordField() {
        return waitUntilElementToBeClickable(PASSWORD_FIELD);
    }

    /**
     * 功能：发送或提交`Btn`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement submitBtn() {
        return waitUntilElementToBeClickable(SUBMIT_BTN);
    }

    /**
     * 功能：执行 `titleLogo` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement titleLogo() {
        return waitUntilVisibilityOfElementLocated(TITLE_LOGO);
    }

}
