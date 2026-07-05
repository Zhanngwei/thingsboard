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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.importing.csv.AbstractBulkImportService;

import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`EdgeBulkImportService` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EdgeBulkImportService extends AbstractBulkImportService<Edge> {
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    private final EdgeService edgeService;
    private final TbEdgeService tbEdgeService;
    /**
     * 规则链，提供当前类调用的业务操作。
     */
    private final RuleChainService ruleChainService;

    /**
     * 功能：更新实体。
     * 参数：
     * - `entity`：实体对象。
     * - `fields`：键值映射。
     * 返回：无。
     */
    @Override
    protected void setEntityFields(Edge entity, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(entity);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    entity.setName(value);
                    break;
                case TYPE:
                    entity.setType(value);
                    break;
                case LABEL:
                    entity.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case ROUTING_KEY:
                    entity.setRoutingKey(value);
                    break;
                case SECRET:
                    entity.setSecret(value);
                    break;
            }
        });
        entity.setAdditionalInfo(additionalInfo);
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `entity`：实体对象。
     * - `fields`：键值映射。
     * 返回：处理结果。
     */
    @SneakyThrows
    @Override
    protected Edge saveEntity(SecurityUser user, Edge entity, Map<BulkImportColumnType, String> fields) {
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        return tbEdgeService.save(entity, edgeTemplateRootRuleChain, user);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    protected Edge findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(edgeService.findEdgeByTenantIdAndName(tenantId, name))
                .orElseGet(Edge::new);
    }

    /**
     * 功能：更新`Owners`。
     * 参数：
     * - `entity`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwners(Edge entity, SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected EntityType getEntityType() {
        return EntityType.EDGE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeBulkImportService` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
