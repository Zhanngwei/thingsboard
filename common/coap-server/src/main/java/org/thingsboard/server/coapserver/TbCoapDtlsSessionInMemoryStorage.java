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
package org.thingsboard.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 中文说明：
 * 1. `TbCoapDtlsSessionInMemoryStorage` 是 ThingsBoard Common 中围绕会话提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
@Data
public class TbCoapDtlsSessionInMemoryStorage {

    private final ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> dtlsSessionsMap = new ConcurrentHashMap<>();
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private long dtlsSessionInactivityTimeout;
    private long dtlsSessionReportTimeout;


    /**
     * 功能：创建 `TbCoapDtlsSessionInMemoryStorage` 实例，并初始化必要字段。
     * 参数：
     * - `dtlsSessionInactivityTimeout`：会话对象。
     * - `dtlsSessionReportTimeout`：会话对象。
     * 返回：新创建的对象实例。
     */
    public TbCoapDtlsSessionInMemoryStorage(long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout) {
        this.dtlsSessionInactivityTimeout = dtlsSessionInactivityTimeout;
        this.dtlsSessionReportTimeout = dtlsSessionReportTimeout;
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `remotePeer`：`remotePeer` 参数。
     * - `dtlsSessionInfo`：会话对象。
     * 返回：无。
     */
    public void put(InetSocketAddress remotePeer, TbCoapDtlsSessionInfo dtlsSessionInfo) {
        log.trace("DTLS session added to in-memory store: [{}] timestamp: [{}]", remotePeer, dtlsSessionInfo.getLastActivityTime());
        dtlsSessionsMap.putIfAbsent(remotePeer, dtlsSessionInfo);
    }

    /**
     * 功能：删除或清理超时时间。
     * 参数：无。
     * 返回：无。
     */
    public void evictTimeoutSessions() {
        long expTime = System.currentTimeMillis() - dtlsSessionInactivityTimeout;
        dtlsSessionsMap.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastActivityTime() < expTime) {
                log.trace("DTLS session was removed from in-memory store: [{}]", entry.getKey());
                return true;
            } else {
                return false;
            }
        });
    }
}