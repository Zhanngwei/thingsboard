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
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.kv.Aggregation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mshvayka on 04.09.18.
 */
@Data
/**
 * 中文说明：`TbGetTelemetryNodeConfiguration` 是获取遥测节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
public class TbGetTelemetryNodeConfiguration implements NodeConfiguration<TbGetTelemetryNodeConfiguration> {

    /**
     * 配置辅助常量：`FETCH_MODE_FIRST` 不从规则节点 JSON 直接读取，用于为相关配置提供默认值、模板或兼容字段名。
     */
    public static final String FETCH_MODE_FIRST = "FIRST";
    /**
     * 配置辅助常量：`FETCH_MODE_LAST` 不从规则节点 JSON 直接读取，用于为相关配置提供默认值、模板或兼容字段名。
     */
    public static final String FETCH_MODE_LAST = "LAST";
    /**
     * 配置辅助常量：`FETCH_MODE_ALL` 不从规则节点 JSON 直接读取，用于为相关配置提供默认值、模板或兼容字段名。
     */
    public static final String FETCH_MODE_ALL = "ALL";

    /**
     * 配置辅助常量：`MAX_FETCH_SIZE` 不从规则节点 JSON 直接读取，用于为相关配置提供默认值、模板或兼容字段名。
     */
    public static final int MAX_FETCH_SIZE = 1000;

    /**
     * 配置字段：来自规则节点 JSON 的 `startInterval` 配置项，控制时间间隔。
     */
    private int startInterval;
    /**
     * 配置字段：来自规则节点 JSON 的 `endInterval` 配置项，控制时间间隔。
     */
    private int endInterval;

    /**
     * 配置字段：来自规则节点 JSON 的 `startIntervalPattern` 配置项，控制支持从消息解析变量的模式字符串。
     */
    private String startIntervalPattern;
    /**
     * 配置字段：来自规则节点 JSON 的 `endIntervalPattern` 配置项，控制支持从消息解析变量的模式字符串。
     */
    private String endIntervalPattern;

    /**
     * 配置字段：来自规则节点 JSON 的 `useMetadataIntervalPatterns` 配置项，控制消息元数据。
     */
    private boolean useMetadataIntervalPatterns;

    /**
     * 配置字段：来自规则节点 JSON 的 `startIntervalTimeUnit` 配置项，控制距离单位。
     */
    private String startIntervalTimeUnit;
    /**
     * 配置字段：来自规则节点 JSON 的 `endIntervalTimeUnit` 配置项，控制距离单位。
     */
    private String endIntervalTimeUnit;
    /**
     * 配置字段：来自规则节点 JSON 的 `fetchMode` 配置项，控制与本类处理流程相关的运行时值。
     */
    private String fetchMode; //FIRST, LAST, ALL
    /**
     * 配置字段：来自规则节点 JSON 的 `orderBy` 配置项，控制与本类处理流程相关的运行时值。
     */
    private String orderBy; //ASC, DESC
    /**
     * 配置字段：来自规则节点 JSON 的 `aggregation` 配置项，控制与本类处理流程相关的运行时值。
     */
    private String aggregation; //MIN, MAX, AVG, SUM, COUNT, NONE;
    /**
     * 配置字段：来自规则节点 JSON 的 `limit` 配置项，控制与本类处理流程相关的运行时值。
     */
    private int limit;

    /**
     * 配置字段：来自规则节点 JSON 的 `latestTsKeyNames` 配置项，控制消息体、元数据、属性或遥测中的键名。
     */
    private List<String> latestTsKeyNames;

    @Override
    /**
     * 方法说明：构建规则节点 JSON 未显式提供字段时使用的默认配置。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbGetTelemetryNodeConfiguration defaultConfiguration() {
        TbGetTelemetryNodeConfiguration configuration = new TbGetTelemetryNodeConfiguration();
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setFetchMode("FIRST");
        configuration.setStartIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStartInterval(2);
        configuration.setEndIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setEndInterval(1);
        configuration.setUseMetadataIntervalPatterns(false);
        configuration.setStartIntervalPattern("");
        configuration.setEndIntervalPattern("");
        configuration.setOrderBy("ASC");
        configuration.setAggregation(Aggregation.NONE.name());
        configuration.setLimit(MAX_FETCH_SIZE);
        return configuration;
    }
    /*
     * 本类总结：`TbGetTelemetryNodeConfiguration` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
