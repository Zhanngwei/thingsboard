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
package org.thingsboard.server.service.ws;

import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `SessionEvent` 是 ThingsBoard Application 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString
public class SessionEvent {

    /**
     * 中文说明：
     * 1. `SessionEventType` 是 ThingsBoard Application 中定义事件固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
     */
    public enum SessionEventType {
        ESTABLISHED, CLOSED, ERROR
    };

    /**
     * 事件，用于区分不同处理分支。
     */
    @Getter
    private final SessionEventType eventType;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    @Getter
    private final Optional<Throwable> error;

    /**
     * 功能：创建 `SessionEvent` 实例，并初始化必要字段。
     * 参数：
     * - `eventType`：类型。
     * - `error`：错误信息。
     * 返回：新创建的对象实例。
     */
    private SessionEvent(SessionEventType eventType, Throwable error) {
        super();
        this.eventType = eventType;
        this.error = Optional.ofNullable(error);
    }

    /**
     * 功能：处理`on Established`。
     * 参数：无。
     * 返回：处理结果。
     */
    public static SessionEvent onEstablished() {
        return new SessionEvent(SessionEventType.ESTABLISHED, null);
    }

    /**
     * 功能：处理`on Closed`。
     * 参数：无。
     * 返回：处理结果。
     */
    public static SessionEvent onClosed() {
        return new SessionEvent(SessionEventType.CLOSED, null);
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    public static SessionEvent onError(Throwable t) {
        return new SessionEvent(SessionEventType.ERROR, t);
    }

}
