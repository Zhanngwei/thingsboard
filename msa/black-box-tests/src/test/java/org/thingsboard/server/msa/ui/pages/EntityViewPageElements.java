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
 * 1. `EntityViewPageElements` 是 ThingsBoard Microservices 中围绕实体视图提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `OtherPageElementsHelper`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class EntityViewPageElements extends OtherPageElementsHelper {
    /**
     * 功能：创建 `EntityViewPageElements` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public EntityViewPageElements(WebDriver driver) {
        super(driver);
    }

    /**
     * 实体视图常量，用于统一引用固定值。
     */
    private static final String ENTITY_VIEW_DETAILS_VIEW = "//tb-details-panel";
    private static final String ENTITY_VIEW_DETAILS_ALARMS = ENTITY_VIEW_DETAILS_VIEW + "//span[text()='Alarms']";

    /**
     * 功能：执行 `entityViewDetailsView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityViewDetailsView() {
        return waitUntilPresenceOfElementLocated(ENTITY_VIEW_DETAILS_VIEW);
    }

    /**
     * 功能：执行 `entityViewDetailsAlarmsBtn` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebElement entityViewDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(ENTITY_VIEW_DETAILS_ALARMS);
    }

}
