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

/**
 * `TbPubSubNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
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
     * 消息映射关系，用于按键查找对应值。
     */
    private Map<String, String> messageAttributes;
    /**
     * 键，提供当前类调用的业务操作。
     */
    private String serviceAccountKey;
    /**
     * 键，提供当前类调用的业务操作。
     */
    private String serviceAccountKeyFileName;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
