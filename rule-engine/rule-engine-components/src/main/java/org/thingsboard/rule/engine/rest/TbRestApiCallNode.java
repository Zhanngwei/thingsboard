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
package org.thingsboard.rule.engine.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "rest api call",
        configClazz = TbRestApiCallNodeConfiguration.class,
        version = 1,
        nodeDescription = "Invoke REST API calls to external REST server",
        nodeDetails = "Will invoke REST API call <code>GET | POST | PUT | DELETE</code> to external REST server. " +
                "Message payload added into Request body. Configured attributes can be added into Headers from Message Metadata." +
                " Outbound message will contain response fields " +
                "(<code>status</code>, <code>statusCode</code>, <code>statusReason</code> and response <code>headers</code>) in the Message Metadata." +
                " Response body saved in outbound Message payload. " +
                "For example <b>statusCode</b> field can be accessed with <code>metadata.statusCode</code>." +
                "<br/><b>Note-</b> if you use system proxy properties, the next system proxy properties should be added: \"http.proxyHost\" and \"http.proxyPort\" or  \"https.proxyHost\" and \"https.proxyPort\" or \"socksProxyHost\" and \"socksProxyPort\"," +
                "and if your proxy with auth, the next ones  should be added: \"tb.proxy.user\" and \"tb.proxy.password\" to the thingsboard.conf file.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeRestApiCallConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyBzdHlsZT0iZW5hYmxlLWJhY2tncm91bmQ6bmV3IDAgMCA1MTIgNTEyIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbDpzcGFjZT0icHJlc2VydmUiIHZpZXdCb3g9IjAgMCA1MTIgNTEyIiB2ZXJzaW9uPSIxLjEiIHk9IjBweCIgeD0iMHB4Ij48ZyB0cmFuc2Zvcm09Im1hdHJpeCguOTQ5NzUgMCAwIC45NDk3NSAxNy4xMiAyNi40OTIpIj48cGF0aCBkPSJtMTY5LjExIDEwOC41NGMtOS45MDY2IDAuMDczNC0xOS4wMTQgNi41NzI0LTIyLjAxNCAxNi40NjlsLTY5Ljk5MyAyMzEuMDhjLTMuNjkwNCAxMi4xODEgMy4yODkyIDI1LjIyIDE1LjQ2OSAyOC45MSAyLjIyNTkgMC42NzQ4MSA0LjQ5NjkgMSA2LjcyODUgMSA5Ljk3MjEgMCAxOS4xNjUtNi41MTUzIDIyLjE4Mi0xNi40NjdhNi41MjI0IDYuNTIyNCAwIDAgMCAwLjAwMiAtMC4wMDJsNjkuOTktMjMxLjA3YTYuNTIyNCA2LjUyMjQgMCAwIDAgMCAtMC4wMDJjMy42ODU1LTEyLjE4MS0zLjI4Ny0yNS4yMjUtMTUuNDcxLTI4LjkxMi0yLjI4MjUtMC42OTE0NS00LjYxMTYtMS4wMTY5LTYuODk4NC0xem04NC45ODggMGMtOS45MDQ4IDAuMDczNC0xOS4wMTggNi41Njc1LTIyLjAxOCAxNi40NjlsLTY5Ljk4NiAyMzEuMDhjLTMuNjg5OCAxMi4xNzkgMy4yODUzIDI1LjIxNyAxNS40NjUgMjguOTA4IDIuMjI5NyAwLjY3NjQ3IDQuNTAwOCAxLjAwMiA2LjczMjQgMS4wMDIgOS45NzIxIDAgMTkuMTY1LTYuNTE1MyAyMi4xODItMTYuNDY3YTYuNTIyNCA2LjUyMjQgMCAwIDAgMC4wMDIgLTAuMDAybDY5Ljk4OC0yMzEuMDdjMy42OTA4LTEyLjE4MS0zLjI4NTItMjUuMjIzLTE1LjQ2Ny0yOC45MTItMi4yODE0LTAuNjkyMzEtNC42MTA4LTEuMDE4OS02Ljg5ODQtMS4wMDJ6bS0yMTcuMjkgNDIuMjNjLTEyLjcyOS0wLjAwMDg3LTIzLjE4OCAxMC40NTYtMjMuMTg4IDIzLjE4NiAwLjAwMSAxMi43MjggMTAuNDU5IDIzLjE4NiAyMy4xODggMjMuMTg2IDEyLjcyNy0wLjAwMSAyMy4xODMtMTAuNDU5IDIzLjE4NC0yMy4xODYgMC4wMDA4NzYtMTIuNzI4LTEwLjQ1Ni0yMy4xODUtMjMuMTg0LTIzLjE4NnptMCAxNDYuNjRjLTEyLjcyNy0wLjAwMDg3LTIzLjE4NiAxMC40NTUtMjMuMTg4IDIzLjE4NC0wLjAwMDg3MyAxMi43MjkgMTAuNDU4IDIzLjE4OCAyMy4xODggMjMuMTg4IDEyLjcyOC0wLjAwMSAyMy4xODQtMTAuNDYgMjMuMTg0LTIzLjE4OC0wLjAwMS0xMi43MjYtMTAuNDU3LTIzLjE4My0yMy4xODQtMjMuMTg0em0yNzAuNzkgNDIuMjExYy0xMi43MjcgMC0yMy4xODQgMTAuNDU3LTIzLjE4NCAyMy4xODRzMTAuNDU1IDIzLjE4OCAyMy4xODQgMjMuMTg4aDE1NC45OGMxMi43MjkgMCAyMy4xODYtMTAuNDYgMjMuMTg2LTIzLjE4OCAwLjAwMS0xMi43MjgtMTAuNDU4LTIzLjE4NC0yMy4xODYtMjMuMTg0eiIgdHJhbnNmb3JtPSJtYXRyaXgoMS4wMzc2IDAgMCAxLjAzNzYgLTcuNTY3NiAtMTQuOTI1KSIgc3Ryb2tlLXdpZHRoPSIxLjI2OTMiLz48L2c+PC9zdmc+"
)
/**
 * REST API 外部调用节点，委托 TbHttpClient 执行 HTTP 请求并把响应写回 Rule Engine 消息。
 * 本类本身不直接访问数据库或缓存；外部调用边界、异步回调、失败路由和并发限制在 TbHttpClient 中实现。
 */
public class TbRestApiCallNode extends TbAbstractExternalNode {

    /**
     * 配置升级时用于替代旧 trimDoubleQuotes 字段的新字段名。
     */
    static final String PARSE_TO_PLAIN_TEXT = "parseToPlainText";
    /**
     * 旧版本配置字段名，仅在升级逻辑中读取。
     */
    static final String TRIM_DOUBLE_QUOTES = "trimDoubleQuotes";
    /**
     * REST 客户端封装，负责 HTTP 客户端生命周期和异步请求处理。
     */
    protected TbHttpClient httpClient;

    /**
     * 初始化 REST 节点并创建 HTTP 客户端封装。
     * HTTP 客户端类型、代理、TLS、超时和并发限制均来自节点配置。
     * 本方法本身不直接调用外部 REST 服务，也不直接访问数据库或缓存。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        TbRestApiCallNodeConfiguration config = TbNodeUtils.convert(configuration, TbRestApiCallNodeConfiguration.class);
        httpClient = new TbHttpClient(config, ctx.getSharedEventLoop());
        if (config.isUseRedisQueueForMsgPersistence()) {
            log.warn("[{}][{}] Usage of Redis Template is deprecated starting 2.5 and will have no affect", ctx.getTenantId(), ctx.getSelfId());
        }
    }

    /**
     * 处理 Rule Engine 消息并发起异步 REST 调用。
     * 消息先经 ackIfNeeded 处理确认关系；HTTP 成功回调走 Success，异常或非 2xx 响应走 Failure。
     * 本方法不直接操作底层 HTTP 客户端线程安全细节，具体由 TbHttpClient 和 AsyncRestTemplate 负责。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        httpClient.processMessage(ctx, tbMsg,
                m -> tellSuccess(ctx, m),
                (m, t) -> tellFailure(ctx, m, t));
    }

    /**
     * 销毁 REST 客户端封装和可能独占的事件循环。
     * 本方法不处理消息确认，也不直接访问数据库或缓存。
     */
    @Override
    public void destroy() {
        if (this.httpClient != null) {
            this.httpClient.destroy();
        }
    }

    /**
     * 升级历史配置字段，将 trimDoubleQuotes 迁移为 parseToPlainText。
     * 本方法只修改配置 JSON，不直接调用外部 REST 服务，也不涉及数据库/缓存。
     */
    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.has(PARSE_TO_PLAIN_TEXT) && oldConfiguration.has(TRIM_DOUBLE_QUOTES)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(PARSE_TO_PLAIN_TEXT, oldConfiguration.get(TRIM_DOUBLE_QUOTES).booleanValue());
                    ((ObjectNode) oldConfiguration).remove(TRIM_DOUBLE_QUOTES);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}

/*
 * 本类总结：
 * 本类是 REST API 外部调用节点的 Rule Engine 入口，负责配置转换、消息确认和委托 HTTP 客户端处理。
 * 外部 HTTP 请求、异步回调、并发限制、代理/TLS 细节和失败元数据由 TbHttpClient 实现。
 * 本类本身不直接涉及数据库或缓存，相关能力只可能通过 Rule Engine 上下文或 HTTP 客户端调用链间接涉及。
 */
