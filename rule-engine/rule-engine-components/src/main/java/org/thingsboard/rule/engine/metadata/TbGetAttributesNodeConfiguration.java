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
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. `TbGetAttributesNodeConfiguration` 是 ThingsBoard Rule Engine Components 中描述 `Tb Get Attributes` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `TbAbstractFetchToNodeConfiguration`、`NodeConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetAttributesNodeConfiguration extends TbAbstractFetchToNodeConfiguration implements NodeConfiguration<TbGetAttributesNodeConfiguration> {

    /**
     * 属性列表，用于保存一组待处理对象。
     */
    private List<String> clientAttributeNames;
    /**
     * 属性列表，用于保存一组待处理对象。
     */
    private List<String> sharedAttributeNames;
    /**
     * 属性列表，用于保存一组待处理对象。
     */
    private List<String> serverAttributeNames;

    /**
     * 时间戳列表，用于保存一组待处理对象。
     */
    private List<String> latestTsKeyNames;

    /**
     * 是否满足失败信息条件。
     */
    private boolean tellFailureIfAbsent;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private boolean getLatestValueWithTs;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbGetAttributesNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetAttributesNodeConfiguration();
        configuration.setClientAttributeNames(Collections.emptyList());
        configuration.setSharedAttributeNames(Collections.emptyList());
        configuration.setServerAttributeNames(Collections.emptyList());
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setTellFailureIfAbsent(true);
        configuration.setGetLatestValueWithTs(false);
        configuration.setFetchTo(TbMsgSource.METADATA);
        return configuration;
    }
}
