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
package org.thingsboard.rule.engine.geo;

import lombok.Data;

@Data
/**
 * 中文说明：`Perimeter` 是围栏辅助类，用于执行 GPS 地理围栏、距离和多边形判断及状态跟踪。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class Perimeter {

    /**
     * 字段说明：保存 `perimeterType`，表示类型匹配条件，供本类方法在规则节点处理流程中使用。
     */
    private PerimeterType perimeterType;

    //For Polygons
    /**
     * 字段说明：保存 `polygonsDefinition`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private String polygonsDefinition;

    //For Circles
    /**
     * 字段说明：保存 `centerLatitude`，表示纬度，供本类方法在规则节点处理流程中使用。
     */
    private Double centerLatitude;
    /**
     * 字段说明：保存 `centerLongitude`，表示经度，供本类方法在规则节点处理流程中使用。
     */
    private Double centerLongitude;
    /**
     * 字段说明：保存 `range`，表示距离范围，供本类方法在规则节点处理流程中使用。
     */
    private Double range;
    /**
     * 字段说明：保存 `rangeUnit`，表示距离范围，供本类方法在规则节点处理流程中使用。
     */
    private RangeUnit rangeUnit;

    /*
     * 本类总结：`Perimeter` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
