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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. 职责：包装规则节点配置的原始 JSON 数据，作为节点初始化时的统一配置输入。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的节点配置包装层。
 * 3. 协作对象：与 {@link TbNode#init(TbContext, TbNodeConfiguration)}、{@link org.thingsboard.rule.engine.api.util.TbNodeUtils#convert} 和具体 NodeConfiguration 协作。
 * 4. 生命周期：由规则节点运行时在节点初始化或配置升级时创建，随节点配置加载过程存在。
 * 5. 设计原因：Rule Engine 存储的是 JSON 配置，先用统一包装对象传递，再由具体节点转换成强类型配置。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor、数据库；直接参与 Rule Engine 节点初始化流程。
 */
@Data
public final class TbNodeConfiguration {

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final JsonNode data;

}

/*
 * 本类总结：
 * 1. 核心职责：作为规则节点原始 JSON 配置的统一包装对象。
 * 2. 核心流程：Rule Engine 读取节点配置 JSON，包装为 TbNodeConfiguration，节点再转换成强类型配置。
 * 3. 关键依赖：JsonNode、TbNode.init、TbNodeUtils.convert 和具体 NodeConfiguration。
 * 4. 学习重点：Rule Engine 用统一 JSON 包装保持配置存储灵活，再在节点边界做类型转换。
 */
