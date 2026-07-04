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
 * 延迟节点的配置对象，描述固定延迟、最大挂起消息数和可选的元数据周期模板。
 * 本类仅保存配置，不直接进行数据库事务、缓存访问或 Rule Engine 消息调度。
 */
@Data
public class TbMsgDelayNodeConfiguration implements NodeConfiguration<TbMsgDelayNodeConfiguration> {

    /**
     * 固定延迟秒数，在未启用元数据模板时使用。
     */
    private int periodInSeconds;
    /**
     * 节点内存中允许挂起等待的最大消息数量。
     */
    private int maxPendingMsgs;
    /**
     * 从消息元数据解析延迟秒数的模板表达式。
     */
    private String periodInSecondsPattern;
    /**
     * 是否使用元数据模板动态解析延迟秒数。
     */
    private boolean useMetadataPeriodInSecondsPatterns;

    /**
     * 创建延迟节点默认配置。
     * 本方法只生成配置对象，不直接参与消息确认、延迟自消息调度或持久化操作。
     *
     * @return 默认延迟 60 秒、最多挂起 1000 条消息的配置实例
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

/*
 * 本类总结：
 * 本类定义 deprecated delay 节点的配置默认值；实际内存挂起、ack 和自消息调度由 TbMsgDelayNode 执行。
 */
