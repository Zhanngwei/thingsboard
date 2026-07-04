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
package org.thingsboard.rule.engine.aws.sns;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
/**
 * AWS SNS 节点配置模型，保存 Topic ARN 模板、访问密钥和区域。
 * 配置类本身不直接创建 SNS 客户端、不调用 AWS，也不涉及异步回调、数据库或缓存。
 */
public class TbSnsNodeConfiguration implements NodeConfiguration<TbSnsNodeConfiguration> {

    /**
     * SNS Topic ARN 模板，运行时结合 TbMsg 解析。
     */
    private String topicArnPattern;
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
     * 构造 AWS SNS 节点默认配置。
     * 本方法只设置默认值，不直接调用 SNS 或处理 Rule Engine 消息确认。
     */
    @Override
    public TbSnsNodeConfiguration defaultConfiguration() {
        TbSnsNodeConfiguration configuration = new TbSnsNodeConfiguration();
        configuration.setTopicArnPattern("arn:aws:sns:us-east-1:123456789012:MyNewTopic");
        configuration.setRegion("us-east-1");
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类仅描述 AWS SNS 发布节点所需配置，实际客户端生命周期和 publish 调用在 TbSnsNode 中完成。
 * 它不直接涉及外部调用、数据库、缓存或异步回调。
 */
