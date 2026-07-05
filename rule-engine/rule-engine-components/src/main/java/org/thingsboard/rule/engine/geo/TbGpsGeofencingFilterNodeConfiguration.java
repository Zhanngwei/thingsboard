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
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：`TbGpsGeofencingFilterNodeConfiguration` 是GPS地理围栏过滤节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
public class TbGpsGeofencingFilterNodeConfiguration implements NodeConfiguration<TbGpsGeofencingFilterNodeConfiguration> {

    /**
     * 键，用于标识或展示当前对象。
     */
    private String latitudeKeyName;
    /**
     * 键，用于标识或展示当前对象。
     */
    private String longitudeKeyName;
    /**
     * 类型，用于区分不同处理分支。
     */
    private PerimeterType perimeterType;

    /**
     * 是否满足消息条件。
     */
    private boolean fetchPerimeterInfoFromMessageMetadata;
    // If Perimeter is fetched from metadata
    /**
     * 键，用于标识或展示当前对象。
     */
    private String perimeterKeyName;

    //For Polygons
    /**
     * `polygonsDefinition` 字段，保存当前对象的对应属性。
     */
    private String polygonsDefinition;

    //For Circles
    /**
     * `centerLatitude` 字段，保存当前对象的对应属性。
     */
    private Double centerLatitude;
    /**
     * `centerLongitude` 字段，保存当前对象的对应属性。
     */
    private Double centerLongitude;
    /**
     * `range` 字段，保存当前对象的对应属性。
     */
    private Double range;
    /**
     * 单位，表示当前对象的对应属性。
     */
    private RangeUnit rangeUnit;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbGpsGeofencingFilterNodeConfiguration defaultConfiguration() {
        TbGpsGeofencingFilterNodeConfiguration configuration = new TbGpsGeofencingFilterNodeConfiguration();
        configuration.setLatitudeKeyName("latitude");
        configuration.setLongitudeKeyName("longitude");
        configuration.setPerimeterType(PerimeterType.POLYGON);
        configuration.setFetchPerimeterInfoFromMessageMetadata(true);
        configuration.setPerimeterKeyName("ss_perimeter");
        return configuration;
    }
    /*
     * 本类总结：`TbGpsGeofencingFilterNodeConfiguration` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
