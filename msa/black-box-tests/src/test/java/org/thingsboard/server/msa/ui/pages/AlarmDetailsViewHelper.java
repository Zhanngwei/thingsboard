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

/**
 * 中文说明：
 * 1. `AlarmDetailsViewHelper` 是 ThingsBoard Microservices 中处理告警通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 直接依赖的类型边界包括 `AlarmDetailsViewElements`。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class AlarmDetailsViewHelper extends AlarmDetailsViewElements {
    /**
     * 功能：创建 `AlarmDetailsViewHelper` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmDetailsViewHelper(WebDriver driver) {
        super(driver);
    }

    /**
     * 功能：执行 `assignAlarmTo` 对应的处理。
     * 参数：
     * - `emailOrName`：名称。
     * 返回：无。
     */
    public void assignAlarmTo(String emailOrName) {
        assignField().click();
        userFromAssignDropdown(emailOrName).click();
    }

    /**
     * 功能：执行 `unassignedAlarm` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void unassignedAlarm() {
        assignField().click();
        unassignedBtn().click();
    }
}
