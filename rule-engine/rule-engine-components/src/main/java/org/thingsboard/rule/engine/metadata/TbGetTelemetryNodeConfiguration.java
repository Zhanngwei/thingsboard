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
 * 中文说明：
 * 1. `TbGetTelemetryNodeConfiguration` 是 ThingsBoard Rule Engine Components 中描述遥测数据行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `NodeConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
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
}
