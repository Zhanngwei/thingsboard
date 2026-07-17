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
package org.thingsboard.server.common.transport.service;

import lombok.Data;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by ashvayka on 15.10.18.
 */
/**
 * 中文说明：
 * 1. `SessionMetaData` 是 ThingsBoard Common Transport 中承载会话信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class SessionMetaData {

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private volatile TransportProtos.SessionInfoProto sessionInfo;
    private final TransportProtos.SessionType sessionType;
    /**
     * 监听器列表，用于保存一组待处理对象。
     */
    private final SessionMsgListener listener;

    /**
     * 异步结果，表示当前对象的对应属性。
     */
    private volatile ScheduledFuture scheduledFuture;
    private volatile boolean subscribedToAttributes;
    /**
     * 是否满足RPC条件。
     */
    private volatile boolean subscribedToRPC;
    private volatile boolean overwriteActivityTime;

    SessionMetaData(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionType sessionType, SessionMsgListener listener) {
        this.sessionInfo = sessionInfo;
        this.sessionType = sessionType;
        this.listener = listener;
        this.scheduledFuture = null;
    }

    /**
     * 功能：更新异步结果。
     * 参数：
     * - `scheduledFuture`：`scheduledFuture` 参数。
     * 返回：无。
     */
    void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    /**
     * 功能：获取异步结果。
     * 参数：无。
     * 返回：异步处理结果。
     */
    public ScheduledFuture getScheduledFuture() {
        return scheduledFuture;
    }

    /**
     * 功能：判断异步结果。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasScheduledFuture() {
        return null != this.scheduledFuture;
    }
}
