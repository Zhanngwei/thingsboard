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

/**
 * 中文说明：
 * 1. `AlarmWidgetElements` 是 ThingsBoard Microservices 中围绕告警提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AlarmDetailsEntityTabHelper`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class AlarmWidgetElements extends AlarmDetailsEntityTabHelper {
    /**
     * 功能：创建 `AlarmWidgetElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmWidgetElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 用户常量，用于统一引用固定值。
     */
    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[contains(@class,'assigned-container')]/span[contains(text(),'%s')]";
    private static final String UNASSIGNED = "//span[text() = '%s']/ancestor::mat-row//span[contains(@class,'assigned-container')]" +
            "//mat-icon[text() = 'account_circle']/following-sibling::span";

    /**
     * 功能：执行 `assignedUser` 对应的处理。
     * 参数：
     * - `userEmail`：`userEmail` 参数。
     * 返回：处理结果。
     */
    @Override
    public WebElement assignedUser(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }

    /**
     * 功能：执行 `unassigned` 对应的处理。
     * 参数：
     * - `alarmType`：类型。
     * 返回：处理结果。
     */
    @Override
    public WebElement unassigned(String alarmType) {
        return waitUntilVisibilityOfElementLocated(String.format(UNASSIGNED, alarmType));
    }
}
