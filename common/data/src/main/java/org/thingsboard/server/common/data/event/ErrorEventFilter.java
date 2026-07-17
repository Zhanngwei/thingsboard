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
package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

/**
 * 中文说明：
 * 1. `ErrorEventFilter` 是 ThingsBoard Common Data 中处理事件的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `EventFilter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Data
@ApiModel
public class ErrorEventFilter implements EventFilter {

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    @ApiModelProperty(position = 1, value = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    /**
     * `method` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 2, value = "String value representing the method name when the error happened", example = "onClusterEventMsg")
    protected String method;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    @ApiModelProperty(position = 3, value = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;

    /**
     * 功能：获取事件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EventType getEventType() {
        return EventType.ERROR;
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || !StringUtils.isEmpty(method) || !StringUtils.isEmpty(errorStr);
    }
}
