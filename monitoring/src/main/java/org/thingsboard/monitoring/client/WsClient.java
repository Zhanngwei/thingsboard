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
package org.thingsboard.monitoring.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.data.cmd.CmdsWrapper;
import org.thingsboard.monitoring.data.cmd.EntityDataCmd;
import org.thingsboard.monitoring.data.cmd.EntityDataUpdate;
import org.thingsboard.monitoring.data.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `WsClient` 是 ThingsBoard Monitoring 中访问 `Ws Client` 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 直接依赖的类型边界包括 `WebSocketClient`、`AutoCloseable`。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
@Slf4j
public class WsClient extends WebSocketClient implements AutoCloseable {

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    public volatile JsonNode lastMsg;
    private CountDownLatch reply;
    /**
     * `update` 字段，保存当前对象的对应属性。
     */
    private CountDownLatch update;

    private final Lock updateLock = new ReentrantLock();

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private long requestTimeoutMs;

    /**
     * 功能：创建 `WsClient` 实例，并初始化必要字段。
     * 参数：
     * - `serverUri`：`serverUri` 参数。
     * - `requestTimeoutMs`：请求对象。
     * 返回：新创建的对象实例。
     */
    public WsClient(URI serverUri, long requestTimeoutMs) {
        super(serverUri);
        this.requestTimeoutMs = requestTimeoutMs;
    }

    /**
     * 功能：处理`on Open`。
     * 参数：
     * - `serverHandshake`：`serverHandshake` 参数。
     * 返回：无。
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：无。
     */
    @Override
    public void onMessage(String s) {
        if (s == null) {
            return;
        }
        updateLock.lock();
        try {
            lastMsg = JacksonUtil.toJsonNode(s);
            log.trace("Received new msg: {}", lastMsg.toPrettyString());
            if (update != null) {
                update.countDown();
            }
            if (reply != null) {
                reply.countDown();
            }
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 功能：处理`on Close`。
     * 参数：
     * - `i`：`i` 参数。
     * - `s`：`s` 参数。
     * - `b`：`b` 参数。
     * 返回：无。
     */
    @Override
    public void onClose(int i, String s, boolean b) {
        log.debug("WebSocket client is closed");
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onError(Exception e) {
        log.error("WebSocket client error:", e);
    }

    /**
     * 功能：保存或创建`Wait For Update`。
     * 参数：无。
     * 返回：无。
     */
    public void registerWaitForUpdate() {
        updateLock.lock();
        try {
            lastMsg = null;
            update = new CountDownLatch(1);
        } finally {
            updateLock.unlock();
        }
        log.trace("Registered wait for update");
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `text`：`text` 参数。
     * 返回：无。
     */
    @Override
    public void send(String text) throws NotYetConnectedException {
        updateLock.lock();
        try {
            reply = new CountDownLatch(1);
        } finally {
            updateLock.unlock();
        }
        super.send(text);
    }

    /**
     * 功能：订阅遥测。
     * 参数：
     * - `devices`：设备信息或设备标识。
     * - `key`：键。
     * 返回：处理结果。
     */
    public WsClient subscribeForTelemetry(List<UUID> devices, String key) {
        EntityDataCmd cmd = new EntityDataCmd();
        cmd.setCmdId(RandomUtils.nextInt(0, 1000));

        EntityListFilter devicesFilter = new EntityListFilter();
        devicesFilter.setEntityType(EntityType.DEVICE);
        devicesFilter.setEntityList(devices.stream().map(UUID::toString).collect(Collectors.toList()));
        EntityDataPageLink pageLink = new EntityDataPageLink(100,0, null, null);
        EntityDataQuery devicesQuery = new EntityDataQuery(devicesFilter, pageLink, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        cmd.setQuery(devicesQuery);

        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(List.of(new EntityKey(EntityKeyType.TIME_SERIES, key)));
        cmd.setLatestCmd(latestCmd);

        CmdsWrapper wrapper = new CmdsWrapper();
        wrapper.setEntityDataCmds(List.of(cmd));
        send(JacksonUtil.toString(wrapper));
        return this;
    }

    /**
     * 功能：执行 `waitForUpdate` 对应的处理。
     * 参数：
     * - `ms`：`ms` 参数。
     * 返回：处理结果。
     */
    public JsonNode waitForUpdate(long ms) {
        log.trace("update latch count: {}", update.getCount());
        try {
            if (update.await(ms, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for update");
                return getLastMsg();
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No update arrived within {} ms", ms);
        return null;
    }

    /**
     * 功能：执行 `waitForReply` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public JsonNode waitForReply() {
        try {
            if (reply.await(requestTimeoutMs, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for reply");
                return getLastMsg();
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No reply arrived within {} ms", requestTimeoutMs);
        throw new IllegalStateException("No WS reply arrived within " + requestTimeoutMs + " ms");
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    private JsonNode getLastMsg() {
        if (lastMsg != null) {
            JsonNode errorMsg = lastMsg.get("errorMsg");
            if (errorMsg != null && !errorMsg.isNull() && StringUtils.isNotEmpty(errorMsg.asText())) {
                throw new RuntimeException("WS error from server: " + errorMsg.asText());
            } else {
                return lastMsg;
            }
        } else {
            return null;
        }
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    public Object getTelemetryUpdate(UUID deviceId, String key) {
        JsonNode lastMsg = getLastMsg();
        if (lastMsg == null || lastMsg.isNull()) return null;
        EntityDataUpdate update = JacksonUtil.treeToValue(lastMsg, EntityDataUpdate.class);
        return update.getLatest(deviceId, key);
    }

    /**
     * 功能：处理SSL。
     * 参数：
     * - `sslParameters`：`sslParameters` 参数。
     * 返回：无。
     */
    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }
}