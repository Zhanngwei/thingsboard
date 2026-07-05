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
package org.thingsboard.rule.engine.aws.sqs;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

/**
 * `TbSqsNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbSqsNodeConfiguration implements NodeConfiguration<TbSqsNodeConfiguration> {

    /**
     * 队列，用于区分不同处理分支。
     */
    private QueueType queueType;
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private String queueUrlPattern;
    /**
     * 延迟时间，用于控制时间范围或等待时长。
     */
    private int delaySeconds;
    /**
     * 消息映射关系，用于按键查找对应值。
     */
    private Map<String, String> messageAttributes;
    /**
     * AWS access key id。
     */
    private String accessKeyId;
    /**
     * AWS secret access key。
     */
    private String secretAccessKey;
    /**
     * AWS region。
     */
    private String region;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbSqsNodeConfiguration defaultConfiguration() {
        TbSqsNodeConfiguration configuration = new TbSqsNodeConfiguration();
        configuration.setQueueType(QueueType.STANDARD);
        configuration.setQueueUrlPattern("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue-name");
        configuration.setDelaySeconds(0);
        configuration.setMessageAttributes(Collections.emptyMap());
        configuration.setRegion("us-east-1");
        return configuration;
    }

    /**
     * `QueueType` 枚举，定义当前流程使用的固定取值。
     */
    public enum QueueType {
        /**
         * `STANDARD`常量，用于统一引用固定值。
         */
        STANDARD,
        /**
         * 字段名，表示当前对象的对应属性。
         */
        FIFO
    }
}

/*
 * 本类总结：
 * 本类描述 AWS SQS 外部节点的队列和发送参数，实际云 SDK 客户端生命周期与 sendMessage 调用在 TbSqsNode 中完成。
 * 配置类本身不直接涉及外部调用、数据库、缓存或异步回调。
 */
