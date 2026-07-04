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
@Data
/**
 * 中文说明：`TbGpsGeofencingFilterNodeConfiguration` 是GPS地理围栏过滤节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
public class TbGpsGeofencingFilterNodeConfiguration implements NodeConfiguration<TbGpsGeofencingFilterNodeConfiguration> {

    /**
     * 配置字段：来自规则节点 JSON 的 `latitudeKeyName` 配置项，控制消息体、元数据、属性或遥测中的键名。
     */
    private String latitudeKeyName;
    /**
     * 配置字段：来自规则节点 JSON 的 `longitudeKeyName` 配置项，控制消息体、元数据、属性或遥测中的键名。
     */
    private String longitudeKeyName;
    /**
     * 配置字段：来自规则节点 JSON 的 `perimeterType` 配置项，控制类型匹配条件。
     */
    private PerimeterType perimeterType;

    /**
     * 配置字段：来自规则节点 JSON 的 `fetchPerimeterInfoFromMessageMetadata` 配置项，控制消息元数据。
     */
    private boolean fetchPerimeterInfoFromMessageMetadata;
    // If Perimeter is fetched from metadata
    /**
     * 配置字段：来自规则节点 JSON 的 `perimeterKeyName` 配置项，控制消息体、元数据、属性或遥测中的键名。
     */
    private String perimeterKeyName;

    //For Polygons
    /**
     * 配置字段：来自规则节点 JSON 的 `polygonsDefinition` 配置项，控制与本类处理流程相关的运行时值。
     */
    private String polygonsDefinition;

    //For Circles
    /**
     * 配置字段：来自规则节点 JSON 的 `centerLatitude` 配置项，控制纬度。
     */
    private Double centerLatitude;
    /**
     * 配置字段：来自规则节点 JSON 的 `centerLongitude` 配置项，控制经度。
     */
    private Double centerLongitude;
    /**
     * 配置字段：来自规则节点 JSON 的 `range` 配置项，控制距离范围。
     */
    private Double range;
    /**
     * 配置字段：来自规则节点 JSON 的 `rangeUnit` 配置项，控制距离范围。
     */
    private RangeUnit rangeUnit;

    @Override
    /**
     * 方法说明：构建规则节点 JSON 未显式提供字段时使用的默认配置。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
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
