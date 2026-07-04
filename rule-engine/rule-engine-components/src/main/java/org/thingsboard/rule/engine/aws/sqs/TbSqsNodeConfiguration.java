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

@Data
/**
 * AWS SQS 节点配置模型，保存队列类型、Queue URL 模板、消息属性、凭据和区域。
 * 配置类本身不直接创建 SQS 客户端、不发送消息，也不涉及异步回调、数据库或缓存。
 */
public class TbSqsNodeConfiguration implements NodeConfiguration<TbSqsNodeConfiguration> {

    /**
     * SQS 队列类型，决定发送请求中的标准队列延迟或 FIFO 去重/分组字段。
     */
    private QueueType queueType;
    /**
     * SQS Queue URL 模板，运行时基于 TbMsg 解析。
     */
    private String queueUrlPattern;
    /**
     * 标准队列的延迟发送秒数。
     */
    private int delaySeconds;
    /**
     * SQS 消息属性模板集合，键和值都可基于消息解析。
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
     * 构造 AWS SQS 节点默认配置。
     * 本方法只设置默认值，不直接调用 SQS 或处理 Rule Engine 消息确认。
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
     * SQS 队列类型枚举，区分标准队列和 FIFO 队列发送语义。
     */
    public enum QueueType {
        /**
         * 标准队列，支持 delaySeconds。
         */
        STANDARD,
        /**
         * FIFO 队列，需要 messageDeduplicationId 和 messageGroupId。
         */
        FIFO
    }
}

/*
 * 本类总结：
 * 本类描述 AWS SQS 外部节点的队列和发送参数，实际云 SDK 客户端生命周期与 sendMessage 调用在 TbSqsNode 中完成。
 * 配置类本身不直接涉及外部调用、数据库、缓存或异步回调。
 */
