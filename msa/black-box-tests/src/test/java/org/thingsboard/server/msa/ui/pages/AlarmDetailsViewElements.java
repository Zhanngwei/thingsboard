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
 * 1. `AlarmDetailsViewElements` 是 ThingsBoard Microservices 中围绕告警提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractBasePage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class AlarmDetailsViewElements extends AbstractBasePage {
    /**
     * 功能：创建 `AlarmDetailsViewElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmDetailsViewElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 字段名常量，用于统一引用固定值。
     */
    private static final String ASSIGN_FIELD = "//mat-label[text()='Assignee']/parent::label/parent::div//input";
    private static final String USER_FROM_DROP_DOWN = "//div[@class='user-display-name']/span[text() = '%s']";
    /**
     * 告警常量，用于统一引用固定值。
     */
    private static final String CLOSE_ALARM_DETAILS_VIEW_BTN = "//mat-dialog-container//mat-icon[contains(text(),'close')]/parent::button";
    private static final String UNASSIGNED_BTN = "//div[@role='listbox']//mat-icon[text() = 'account_circle']/following-sibling::span";

    /**
     * 功能：执行 `assignField` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement assignField() {
        return waitUntilElementToBeClickable(ASSIGN_FIELD);
    }

    /**
     * 功能：执行 `userFromAssignDropdown` 对应的处理。
     * 参数：
     * - `emailOrName`：名称。
     * 返回：处理结果。
     */
    public WebElement userFromAssignDropdown(String emailOrName) {
        return waitUntilElementToBeClickable(String.format(USER_FROM_DROP_DOWN, emailOrName));
    }

    /**
     * 功能：停止或关闭告警。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement closeAlarmDetailsViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_ALARM_DETAILS_VIEW_BTN);
    }

    /**
     * 功能：执行 `unassignedBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement unassignedBtn() {
        return waitUntilElementToBeClickable(UNASSIGNED_BTN);
    }
}
