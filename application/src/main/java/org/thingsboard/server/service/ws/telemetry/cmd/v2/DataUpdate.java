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
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.Getter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.List;

/**
 * 中文说明：
 * 1. `DataUpdate` 是 ThingsBoard Application 中围绕 `Update` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `CmdUpdate`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public abstract class DataUpdate<T> extends CmdUpdate {

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Getter
    private final PageData<T> data;
    /**
     * `update`列表，用于保存一组待处理对象。
     */
    @Getter
    private final List<T> update;

    /**
     * 功能：创建 `DataUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `data`：待处理数据。
     * - `update`：数据列表。
     * - `errorCode`：错误信息。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DataUpdate(int cmdId, PageData<T> data, List<T> update, int errorCode, String errorMsg) {
        super(cmdId, errorCode, errorMsg);
        this.data = data;
        this.update = update;
    }

    /**
     * 功能：创建 `DataUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `data`：待处理数据。
     * - `update`：数据列表。
     * 返回：新创建的对象实例。
     */
    public DataUpdate(int cmdId, PageData<T> data, List<T> update) {
        this(cmdId, data, update, SubscriptionErrorCode.NO_ERROR.getCode(), null);
    }

    /**
     * 功能：创建 `DataUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public DataUpdate(int cmdId, int errorCode, String errorMsg) {
        this(cmdId, null, null, errorCode, errorMsg);
    }

}
