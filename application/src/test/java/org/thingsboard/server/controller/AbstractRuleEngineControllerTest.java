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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.function.Predicate;

/**
 * Created by ashvayka on 20.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractRuleEngineControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@TestPropertySource(properties = {
        "js.evaluator=mock",
})
public abstract class AbstractRuleEngineControllerTest extends AbstractControllerTest {

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    protected RuleChainService ruleChainService;

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    protected RuleChain saveRuleChain(RuleChain ruleChain) throws Exception {
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    protected RuleChain getRuleChain(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/" + ruleChainId.getId().toString(), RuleChain.class);
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChainMD`：`ruleChainMD` 参数。
     * 返回：处理结果。
     */
    protected RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMD) throws Exception {
        return doPost("/api/ruleChain/metadata", ruleChainMD, RuleChainMetaData.class);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    protected RuleChainMetaData getRuleChainMetaData(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/metadata/" + ruleChainId.getId().toString(), RuleChainMetaData.class);
    }

    /**
     * 功能：获取`Debug Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    protected PageData<EventInfo> getDebugEvents(TenantId tenantId, EntityId entityId, int limit) throws Exception {
        return getEvents(tenantId, entityId, EventType.DEBUG_RULE_NODE.getOldName(), limit);
    }

    /**
     * 功能：获取`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    protected PageData<EventInfo> getEvents(TenantId tenantId, EntityId entityId, String eventType, int limit) throws Exception {
        TimePageLink pageLink = new TimePageLink(limit);
        return doGetTypedWithTimePageLink("/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&",
                new TypeReference<PageData<EventInfo>>() {
                }, pageLink, entityId.getEntityType(), entityId.getId(), eventType, tenantId.getId());
    }


    /**
     * 功能：获取`Metadata`。
     * 参数：
     * - `outEvent`：`outEvent` 参数。
     * 返回：处理结果。
     */
    protected JsonNode getMetadata(EventInfo outEvent) {
        String metaDataStr = outEvent.getBody().get("metadata").asText();
        try {
            return JacksonUtil.toJsonNode(metaDataStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `filterByPostTelemetryEventType` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Predicate<EventInfo> filterByPostTelemetryEventType() {
        return event -> event.getBody().get("msgType").textValue().equals(TbMsgType.POST_TELEMETRY_REQUEST.name());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractRuleEngineControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
