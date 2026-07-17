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
package org.thingsboard.server.queue.sqs;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * 中文说明：
 * 1. `TbAwsSqsSettings` 是 ThingsBoard Common Queue 中描述 `Tb Aws Sqs` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
@Component
@Data
public class TbAwsSqsSettings {

    /**
     * 当前对象是否为默认项。
     */
    @Value("${queue.aws_sqs.use_default_credential_provider_chain}")
    private Boolean useDefaultCredentialProviderChain;

    /**
     * 键ID，用于定位对应业务对象。
     */
    @Value("${queue.aws_sqs.access_key_id}")
    private String accessKeyId;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Value("${queue.aws_sqs.secret_access_key}")
    private String secretAccessKey;

    /**
     * `region` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.aws_sqs.region}")
    private String region;

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Value("${queue.aws_sqs.threads_per_topic}")
    private int threadsPerTopic;

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${queue.aws_sqs.producer_thread_pool_size:50}")
    private int threadPoolSize;

}
