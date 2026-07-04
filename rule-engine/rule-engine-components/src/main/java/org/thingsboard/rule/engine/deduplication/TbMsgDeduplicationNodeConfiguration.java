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
package org.thingsboard.rule.engine.deduplication;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
/**
 * 中文说明：`TbMsgDeduplicationNodeConfiguration` 是消息去重节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
public class TbMsgDeduplicationNodeConfiguration implements NodeConfiguration<TbMsgDeduplicationNodeConfiguration> {

    /**
     * 配置字段：来自规则节点 JSON 的 `interval` 配置项，控制时间间隔。
     */
    private int interval;
    /**
     * 配置字段：来自规则节点 JSON 的 `strategy` 配置项，控制去重策略。
     */
    private DeduplicationStrategy strategy;

    // only for DeduplicationStrategy.ALL:
    /**
     * 配置字段：来自规则节点 JSON 的 `outMsgType` 配置项，控制类型匹配条件。
     */
    private String outMsgType;

    // Advanced settings:
    /**
     * 配置字段：来自规则节点 JSON 的 `maxPendingMsgs` 配置项，控制当前规则链消息。
     */
    private int maxPendingMsgs;
    /**
     * 配置字段：来自规则节点 JSON 的 `maxRetries` 配置项，控制与本类处理流程相关的运行时值。
     */
    private int maxRetries;

    @Override
    /**
     * 方法说明：构建规则节点 JSON 未显式提供字段时使用的默认配置。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbMsgDeduplicationNodeConfiguration defaultConfiguration() {
        TbMsgDeduplicationNodeConfiguration configuration = new TbMsgDeduplicationNodeConfiguration();
        configuration.setInterval(60);
        configuration.setStrategy(DeduplicationStrategy.FIRST);
        configuration.setMaxPendingMsgs(100);
        configuration.setMaxRetries(3);
        return configuration;
    }
    /*
     * 本类总结：`TbMsgDeduplicationNodeConfiguration` 负责按配置聚合、去重、延迟和输出消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
