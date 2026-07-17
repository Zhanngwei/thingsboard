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

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. `WebSocketSessionRef` 是 ThingsBoard Application 中围绕会话提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Builder
@Data
public class WebSocketSessionRef {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID，用于定位对应业务对象。
     */
    private final String sessionId;
    private SecurityUser securityCtx;
    /**
     * 本地地址，用于定位外部资源或本地资源。
     */
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;
    /**
     * 会话，用于区分不同处理分支。
     */
    private final WebSocketSessionType sessionType;
    private final AtomicInteger sessionSubIdSeq = new AtomicInteger();

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketSessionRef that = (WebSocketSessionRef) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        String info = "";
        if (securityCtx != null) {
            info += "[" + securityCtx.getTenantId() + "]";
            info += "[" + securityCtx.getId() + "]";
        }
        info += "[" + sessionId + "]";
        return info;
    }

}
