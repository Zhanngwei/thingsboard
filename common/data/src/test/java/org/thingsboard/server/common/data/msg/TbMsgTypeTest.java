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
package org.thingsboard.server.common.data.msg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_DELETE;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DELAY_TIMEOUT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_UPDATE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_ASSIGNED_TO_EDGE;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE;
import static org.thingsboard.server.common.data.msg.TbMsgType.GENERATOR_NODE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.MSG_COUNT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.NA;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_FAILURE;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_SUCCESS;
import static org.thingsboard.server.common.data.msg.TbMsgType.SEND_EMAIL;

/**
 * 中文说明：
 * 1. 类目的：`TbMsgTypeTest` 是ThingsBoard Common 测试模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
class TbMsgTypeTest {

    private static final List<TbMsgType> typesWithNullRuleNodeConnection = List.of(
            ALARM,
            ALARM_DELETE,
            ENTITY_ASSIGNED_TO_EDGE,
            ENTITY_UNASSIGNED_FROM_EDGE,
            PROVISION_FAILURE,
            PROVISION_SUCCESS,
            SEND_EMAIL,
            GENERATOR_NODE_SELF_MSG,
            DEVICE_PROFILE_PERIODIC_SELF_MSG,
            DEVICE_PROFILE_UPDATE_SELF_MSG,
            DEVICE_UPDATE_SELF_MSG,
            DEDUPLICATION_TIMEOUT_SELF_MSG,
            DELAY_TIMEOUT_SELF_MSG,
            MSG_COUNT_SELF_MSG,
            NA
    );

    // backward-compatibility tests

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void getRuleNodeConnectionsTest() {
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (typesWithNullRuleNodeConnection.contains(type)) {
                assertThat(type.getRuleNodeConnection()).isEqualTo(TbNodeConnectionType.OTHER);
            } else {
                assertThat(type.getRuleNodeConnection()).isNotEqualTo(TbNodeConnectionType.OTHER);
            }
        }
    }

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void getRuleNodeConnectionOrElseOtherTest() {
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (typesWithNullRuleNodeConnection.contains(type)) {
                assertThat(type.getRuleNodeConnection())
                        .isEqualTo(TbNodeConnectionType.OTHER);
            } else {
                assertThat(type.getRuleNodeConnection()).isNotNull()
                        .isNotEqualTo(TbNodeConnectionType.OTHER);
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgTypeTest` 在 ThingsBoard Common 测试模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
