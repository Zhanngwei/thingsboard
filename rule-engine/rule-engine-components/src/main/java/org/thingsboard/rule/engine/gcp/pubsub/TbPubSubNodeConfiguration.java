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
package org.thingsboard.rule.engine.gcp.pubsub;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

@Data
/**
 * GCP Pub/Sub 节点配置模型，保存项目、Topic、消息属性和服务账号密钥。
 * 配置类本身不直接创建 Publisher、不发布消息，也不涉及异步回调、数据库或缓存。
 */
public class TbPubSubNodeConfiguration implements NodeConfiguration<TbPubSubNodeConfiguration> {

    /**
     * Google Cloud project id。
     */
    private String projectId;
    /**
     * Pub/Sub topic name。
     */
    private String topicName;
    /**
     * Pub/Sub 消息属性模板集合，键和值都可基于 TbMsg 解析。
     */
    private Map<String, String> messageAttributes;
    /**
     * 服务账号 JSON 密钥内容。
     */
    private String serviceAccountKey;
    /**
     * 服务账号密钥文件名，供 UI/配置来源标识使用。
     */
    private String serviceAccountKeyFileName;

    /**
     * 构造 GCP Pub/Sub 节点默认配置。
     * 本方法只设置默认值，不直接创建 Publisher 或处理 Rule Engine 消息确认。
     */
    @Override
    public TbPubSubNodeConfiguration defaultConfiguration() {
        TbPubSubNodeConfiguration configuration = new TbPubSubNodeConfiguration();
        configuration.setProjectId("my-google-cloud-project-id");
        configuration.setTopicName("my-pubsub-topic-name");
        configuration.setMessageAttributes(Collections.emptyMap());
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类描述 GCP Pub/Sub 外部节点的项目、Topic、属性和认证配置。
 * 实际 Publisher 创建、异步 publish、成功/失败路由由 TbPubSubNode 完成；本类不直接涉及数据库或缓存。
 */
