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
package org.thingsboard.rule.engine.data;

import lombok.Data;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;

/**
 * 通用实体关系查询条件，供规则节点按关系方向、层级和实体类型过滤器查找关联实体。
 * 本类只表达查询参数，本身不直接访问数据库或缓存，也不直接发送或确认 Rule Engine 消息。
 */
@Data
public class RelationsQuery {

    /**
     * 关系搜索方向，决定查询结果从起点实体的来源侧还是目标侧取实体。
     */
    private EntitySearchDirection direction;
    /**
     * 关系遍历最大层级，默认查询一层。
     */
    private int maxLevel = 1;
    /**
     * 关系实体类型过滤器列表，用于限制返回的关联实体范围。
     */
    private List<RelationEntityTypeFilter> filters;
    /**
     * 是否只返回最后一层关系上的实体，默认返回所有匹配层级。
     */
    private boolean fetchLastLevelOnly = false;
}

/*
 * 本类总结：
 * 本类是关系查询的配置模型，线程安全性取决于调用方如何共享其实例；具体数据库读取、缓存命中和规则链消息流由使用它的服务或节点完成。
 */
