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

import java.util.List;

/**
 * 设备关系查询条件，供规则节点按起点实体、关系方向和设备类型查找关联设备。
 * 本类只是规则节点配置数据载体，本身不直接读取数据库、不访问缓存，也不直接参与 Rule Engine 消息转发。
 */
@Data
public class DeviceRelationsQuery {
    /**
     * 关系搜索方向，决定从起点实体向外查找还是向内查找。
     */
    private EntitySearchDirection direction;
    /**
     * 关系搜索最大层级，默认只查询一层关系。
     */
    private int maxLevel = 1;
    /**
     * 关系类型过滤条件，为空时由调用方或底层查询逻辑决定是否不过滤。
     */
    private String relationType;
    /**
     * 允许返回的设备类型列表，用于缩小关联设备查询结果。
     */
    private List<String> deviceTypes;
    /**
     * 是否只返回最后一层关系上的设备。
     */
    private boolean fetchLastLevelOnly;
}

/*
 * 本类总结：
 * 本类封装按关系查找设备时需要的查询参数，不保存运行态状态；数据库、缓存和消息流处理均发生在使用该配置的 loader 或规则节点中。
 */
