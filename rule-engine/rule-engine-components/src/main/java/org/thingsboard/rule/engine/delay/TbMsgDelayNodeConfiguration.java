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
package org.thingsboard.rule.engine.delay;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * `TbMsgDelayNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbMsgDelayNodeConfiguration implements NodeConfiguration<TbMsgDelayNodeConfiguration> {

    /**
     * `periodInSeconds` 字段，保存当前对象的对应属性。
     */
    private int periodInSeconds;
    /**
     * `maxPendingMsgs` 字段，保存当前对象的对应属性。
     */
    private int maxPendingMsgs;
    /**
     * `periodInSecondsPattern` 字段，保存当前对象的对应属性。
     */
    private String periodInSecondsPattern;
    /**
     * 是否使用`metadata period in seconds patterns`。
     */
    private boolean useMetadataPeriodInSecondsPatterns;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbMsgDelayNodeConfiguration defaultConfiguration() {
        TbMsgDelayNodeConfiguration configuration = new TbMsgDelayNodeConfiguration();
        configuration.setPeriodInSeconds(60);
        configuration.setMaxPendingMsgs(1000);
        configuration.setUseMetadataPeriodInSecondsPatterns(false);
        return configuration;
    }
}
