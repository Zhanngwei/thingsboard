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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.service.ws.AuthCmd;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCommandsWrapper;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.TimeSeriesCmd;

import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`TbTestWebSocketClient` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@Slf4j
public class TbTestWebSocketClient extends WebSocketClient {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Getter
    private volatile String lastMsg;
    private volatile CountDownLatch reply;
    /**
     * `update` 字段，保存当前对象的对应属性。
     */
    private volatile CountDownLatch update;

    /**
     * 功能：创建 `TbTestWebSocketClient` 实例，并初始化必要字段。
     * 参数：
     * - `serverUri`：`serverUri` 参数。
     * 返回：新创建的对象实例。
     */
    public TbTestWebSocketClient(URI serverUri) {
        super(serverUri);
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
     * 功能：执行 `authenticate` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：无。
     */
    public void authenticate(String token) {
        WsCommandsWrapper cmdsWrapper = new WsCommandsWrapper();
        cmdsWrapper.setAuthCmd(new AuthCmd(1, token));
        send(JacksonUtil.toString(cmdsWrapper));
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：无。
     */
    @Override
    public void onMessage(String s) {
        log.info("RECEIVED: {}", s);
        lastMsg = s;
        if (update != null) {
            update.countDown();
        }
        if (reply != null) {
            reply.countDown();
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
        log.info("CLOSED.");
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onError(Exception e) {
        log.error("ERROR:", e);
    }

    /**
     * 功能：保存或创建`Wait For Update`。
     * 参数：无。
     * 返回：无。
     */
    public void registerWaitForUpdate() {
        registerWaitForUpdate(1);
    }

    /**
     * 功能：保存或创建`Wait For Update`。
     * 参数：
     * - `count`：`count` 参数。
     * 返回：无。
     */
    public void registerWaitForUpdate(int count) {
        log.debug("registerWaitForUpdate [{}]", count);
        lastMsg = null;
        update = new CountDownLatch(count);
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `text`：`text` 参数。
     * 返回：无。
     */
    @Override
    public void send(String text) throws NotYetConnectedException {
        log.debug("send [{}]", text);
        reply = new CountDownLatch(1);
        super.send(text);
    }

    /**
     * 功能：执行 `waitForUpdate` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String waitForUpdate() {
        return waitForUpdate(false);
    }

    /**
     * 功能：执行 `waitForUpdate` 对应的处理。
     * 参数：
     * - `throwExceptionOnTimeout`：`throwExceptionOnTimeout` 参数。
     * 返回：文本结果。
     */
    public String waitForUpdate(boolean throwExceptionOnTimeout) {
        return waitForUpdate(TIMEOUT, throwExceptionOnTimeout);
    }

    /**
     * 功能：执行 `waitForUpdate` 对应的处理。
     * 参数：
     * - `ms`：`ms` 参数。
     * 返回：文本结果。
     */
    public String waitForUpdate(long ms) {
        return waitForUpdate(ms, false);
    }

    /**
     * 功能：执行 `waitForUpdate` 对应的处理。
     * 参数：
     * - `ms`：`ms` 参数。
     * - `throwExceptionOnTimeout`：`throwExceptionOnTimeout` 参数。
     * 返回：文本结果。
     */
    public String waitForUpdate(long ms, boolean throwExceptionOnTimeout) {
        log.debug("waitForUpdate [{}]", ms);
        try {
            if (update.await(ms, TimeUnit.MILLISECONDS)) {
                return lastMsg;
            } else {
                log.warn("Failed to await update (waiting time [{}]ms elapsed)", ms, new RuntimeException("stacktrace"));
            }
        } catch (InterruptedException e) {
            log.warn("Failed to await update", e);
        }
        if (throwExceptionOnTimeout) {
            throw new AssertionError("Waited for update for " + ms + " ms but none arrived");
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `waitForReply` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String waitForReply() {
        return waitForReply(false);
    }

    /**
     * 功能：执行 `waitForReply` 对应的处理。
     * 参数：
     * - `throwExceptionOnTimeout`：`throwExceptionOnTimeout` 参数。
     * 返回：文本结果。
     */
    public String waitForReply(boolean throwExceptionOnTimeout) {
        return waitForReply(TIMEOUT, throwExceptionOnTimeout);
    }

    /**
     * 功能：执行 `waitForReply` 对应的处理。
     * 参数：
     * - `ms`：`ms` 参数。
     * - `throwExceptionOnTimeout`：`throwExceptionOnTimeout` 参数。
     * 返回：文本结果。
     */
    public String waitForReply(long ms, boolean throwExceptionOnTimeout) {
        log.debug("waitForReply [{}]", ms);
        try {
            if (reply.await(ms, TimeUnit.MILLISECONDS)) {
                return lastMsg;
            } else {
                log.warn("Failed to await reply (waiting time [{}]ms elapsed)", ms, new RuntimeException("stacktrace"));
            }
        } catch (InterruptedException e) {
            log.warn("Failed to await reply", e);
        }
        if (throwExceptionOnTimeout) {
            throw new AssertionError("Waited for reply for " + ms + " ms but none arrived");
        } else {
            return null;
        }
    }

    /**
     * 功能：解析数据。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public EntityDataUpdate parseDataReply(String msg) {
        return JacksonUtil.fromString(msg, EntityDataUpdate.class);
    }

    /**
     * 功能：解析数量。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public EntityCountUpdate parseCountReply(String msg) {
        return JacksonUtil.fromString(msg, EntityCountUpdate.class);
    }

    /**
     * 功能：解析告警。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public AlarmCountUpdate parseAlarmCountReply(String msg) {
        return JacksonUtil.fromString(msg, AlarmCountUpdate.class);
    }

    /**
     * 功能：订阅`Latest Update`。
     * 参数：
     * - `keys`：键。
     * - `entityFilter`：实体对象。
     * 返回：处理结果。
     */
    public EntityDataUpdate subscribeLatestUpdate(List<EntityKey> keys, EntityFilter entityFilter) {
        EntityDataQuery edq = new EntityDataQuery(entityFilter, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return subscribeLatestUpdate(keys, edq);
    }

    /**
     * 功能：订阅`Latest Update`。
     * 参数：
     * - `keys`：键。
     * 返回：处理结果。
     */
    public EntityDataUpdate subscribeLatestUpdate(List<EntityKey> keys) {
        return subscribeLatestUpdate(keys, (EntityDataQuery) null);
    }

    /**
     * 功能：订阅`Latest Update`。
     * 参数：
     * - `keys`：键。
     * - `edq`：`edq` 参数。
     * 返回：处理结果。
     */
    public EntityDataUpdate subscribeLatestUpdate(List<EntityKey> keys, EntityDataQuery edq) {
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(keys);
        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        send(cmd);
        return parseDataReply(waitForReply());
    }

    /**
     * 功能：订阅时间戳。
     * 参数：
     * - `keys`：键。
     * - `startTs`：时间戳。
     * - `timeWindow`：`timeWindow` 参数。
     * 返回：处理结果。
     */
    public EntityDataUpdate subscribeTsUpdate(List<String> keys, long startTs, long timeWindow) {
        return subscribeTsUpdate(keys, startTs, timeWindow, (EntityDataQuery) null);
    }

    /**
     * 功能：订阅时间戳。
     * 参数：
     * - `keys`：键。
     * - `startTs`：时间戳。
     * - `timeWindow`：`timeWindow` 参数。
     * - `entityFilter`：实体对象。
     * 返回：处理结果。
     */
    public EntityDataUpdate subscribeTsUpdate(List<String> keys, long startTs, long timeWindow, EntityFilter entityFilter) {
        EntityDataQuery edq = new EntityDataQuery(entityFilter, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return subscribeTsUpdate(keys, startTs, timeWindow, edq);
    }

    /**
     * 功能：订阅时间戳。
     * 参数：
     * - `keys`：键。
     * - `startTs`：时间戳。
     * - `timeWindow`：`timeWindow` 参数。
     * - `edq`：`edq` 参数。
     * 返回：处理结果。
     */
    public EntityDataUpdate subscribeTsUpdate(List<String> keys, long startTs, long timeWindow, EntityDataQuery edq) {
        TimeSeriesCmd tsCmd = new TimeSeriesCmd();
        tsCmd.setKeys(keys);
        tsCmd.setAgg(Aggregation.NONE);
        tsCmd.setLimit(1000);
        tsCmd.setStartTs(startTs - timeWindow);
        tsCmd.setTimeWindow(timeWindow);

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, null, tsCmd);

        send(cmd);
        return parseDataReply(waitForReply());
    }

    /**
     * 功能：订阅`For Attributes`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：处理结果。
     */
    public JsonNode subscribeForAttributes(EntityId entityId, String scope, List<String> keys) {
        AttributesSubscriptionCmd cmd = new AttributesSubscriptionCmd();
        cmd.setCmdId(1);
        cmd.setEntityType(entityId.getEntityType().toString());
        cmd.setEntityId(entityId.getId().toString());
        cmd.setScope(scope);
        cmd.setKeys(String.join(",", keys));
        send(cmd);
        return JacksonUtil.toJsonNode(waitForReply());
    }

    /**
     * 功能：发送或提交历史数据订阅命令。
     * 参数：
     * - `keys`：键。
     * - `startTs`：时间戳。
     * - `timeWindow`：`timeWindow` 参数。
     * 返回：处理结果。
     */
    public EntityDataUpdate sendHistoryCmd(List<String> keys, long startTs, long timeWindow) {
        return sendHistoryCmd(keys, startTs, timeWindow, (EntityDataQuery) null);
    }

    /**
     * 功能：发送或提交历史数据订阅命令。
     * 参数：
     * - `keys`：键。
     * - `startTs`：时间戳。
     * - `timeWindow`：`timeWindow` 参数。
     * - `entityFilter`：实体对象。
     * 返回：处理结果。
     */
    public EntityDataUpdate sendHistoryCmd(List<String> keys, long startTs, long timeWindow, EntityFilter entityFilter) {
        EntityDataQuery edq = new EntityDataQuery(entityFilter,
                new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return sendHistoryCmd(keys, startTs, timeWindow, edq);
    }

    /**
     * 功能：发送或提交历史数据订阅命令。
     * 参数：
     * - `keys`：键。
     * - `startTs`：时间戳。
     * - `timeWindow`：`timeWindow` 参数。
     * - `edq`：`edq` 参数。
     * 返回：处理结果。
     */
    public EntityDataUpdate sendHistoryCmd(List<String> keys, long startTs, long timeWindow, EntityDataQuery edq) {
        EntityHistoryCmd historyCmd = new EntityHistoryCmd();
        historyCmd.setKeys(keys);
        historyCmd.setAgg(Aggregation.NONE);
        historyCmd.setLimit(1000);
        historyCmd.setStartTs(startTs - timeWindow);
        historyCmd.setEndTs(startTs);

        EntityDataCmd cmd = new EntityDataCmd(1, edq, historyCmd, null, null);

        send(cmd);
        return parseDataReply(this.waitForReply());
    }

    /**
     * 功能：发送或提交实体。
     * 参数：
     * - `edq`：`edq` 参数。
     * 返回：处理结果。
     */
    public EntityDataUpdate sendEntityDataQuery(EntityDataQuery edq) {
        log.warn("sendEntityDataQuery {}", edq);
        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, null, null);
        send(cmd);
        String msg = this.waitForReply();
        return parseDataReply(msg);
    }

    /**
     * 功能：发送或提交实体。
     * 参数：
     * - `entityFilter`：实体对象。
     * 返回：处理结果。
     */
    public EntityDataUpdate sendEntityDataQuery(EntityFilter entityFilter) {
        log.warn("sendEntityDataQuery {}", entityFilter);
        EntityDataQuery edq = new EntityDataQuery(entityFilter, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return sendEntityDataQuery(edq);
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `cmds`：`cmds` 参数。
     * 返回：无。
     */
    public void send(WsCmd... cmds) {
        WsCommandsWrapper cmdsWrapper = new WsCommandsWrapper();
        cmdsWrapper.setCmds(List.of(cmds));
        send(JacksonUtil.toString(cmdsWrapper));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbTestWebSocketClient` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
