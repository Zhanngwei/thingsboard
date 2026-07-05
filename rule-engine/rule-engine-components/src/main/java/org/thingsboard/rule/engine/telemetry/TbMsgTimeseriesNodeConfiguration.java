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
package org.thingsboard.rule.engine.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * 中文说明：`TbMsgTimeseriesNodeConfiguration` 是消息时间序列节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
public class TbMsgTimeseriesNodeConfiguration implements NodeConfiguration<TbMsgTimeseriesNodeConfiguration> {

    /**
     * `defaultTTL` 字段，保存当前对象的对应属性。
     */
    private long defaultTTL;
    /**
     * 是否满足`skipLatestPersistence`条件。
     */
    private boolean skipLatestPersistence;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private boolean useServerTs;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbMsgTimeseriesNodeConfiguration defaultConfiguration() {
        TbMsgTimeseriesNodeConfiguration configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setDefaultTTL(0L);
        configuration.setSkipLatestPersistence(false);
        configuration.setUseServerTs(false);
        return configuration;
    }
    /*
     * 本类总结：`TbMsgTimeseriesNodeConfiguration` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
