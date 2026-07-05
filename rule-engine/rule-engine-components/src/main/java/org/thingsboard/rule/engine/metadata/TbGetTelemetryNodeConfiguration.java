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
/**
 * 中文说明：`TbGetTelemetryNodeConfiguration` 是获取遥测节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
public class TbGetTelemetryNodeConfiguration implements NodeConfiguration<TbGetTelemetryNodeConfiguration> {

    /**
     * `FETCH_MODE_FIRST`常量，用于统一引用固定值。
     */
    public static final String FETCH_MODE_FIRST = "FIRST";
    /**
     * `FETCH_MODE_LAST`常量，用于统一引用固定值。
     */
    public static final String FETCH_MODE_LAST = "LAST";
    /**
     * `FETCH_MODE_ALL`常量，用于统一引用固定值。
     */
    public static final String FETCH_MODE_ALL = "ALL";

    /**
     * `MAX_FETCH_SIZE`常量，用于统一引用固定值。
     */
    public static final int MAX_FETCH_SIZE = 1000;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private int startInterval;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private int endInterval;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private String startIntervalPattern;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private String endIntervalPattern;

    /**
     * 是否使用时间间隔。
     */
    private boolean useMetadataIntervalPatterns;

    /**
     * 时间，用于控制时间范围或等待时长。
     */
    private String startIntervalTimeUnit;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    private String endIntervalTimeUnit;
    /**
     * `fetchMode` 字段，保存当前对象的对应属性。
     */
    private String fetchMode; //FIRST, LAST, ALL
    /**
     * `orderBy` 字段，保存当前对象的对应属性。
     */
    private String orderBy; //ASC, DESC
    /**
     * `aggregation` 字段，保存当前对象的对应属性。
     */
    private String aggregation; //MIN, MAX, AVG, SUM, COUNT, NONE;
    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    private int limit;

    /**
     * 时间戳列表，用于保存一组待处理对象。
     */
    private List<String> latestTsKeyNames;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
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
