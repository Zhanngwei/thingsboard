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
package org.thingsboard.server.queue.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `TbPubSubProducerTemplate` 是 ThingsBoard Common Queue 中围绕 `Tb Pub Sub` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueProducer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbPubSubProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final Gson gson = new Gson();

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String defaultTopic;
    private final TbQueueAdmin admin;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbPubSubSettings pubSubSettings;

    private final Map<String, Publisher> publisherMap = new ConcurrentHashMap<>();

    private final ExecutorService pubExecutor = Executors.newCachedThreadPool();

    /**
     * 功能：创建 `TbPubSubProducerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `pubSubSettings`：配置对象。
     * - `defaultTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public TbPubSubProducerTemplate(TbQueueAdmin admin, TbPubSubSettings pubSubSettings, String defaultTopic) {
        this.defaultTopic = defaultTopic;
        this.admin = admin;
        this.pubSubSettings = pubSubSettings;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void init() {

    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();
        pubsubMessageBuilder.setData(getMsg(msg));

        Publisher publisher = getOrCreatePublisher(tpi.getFullTopicName());
        ApiFuture<String> future = publisher.publish(pubsubMessageBuilder.build());

        ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
            public void onSuccess(String messageId) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            public void onFailure(Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        }, pubExecutor);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        publisherMap.forEach((k, v) -> {
            if (v != null) {
                try {
                    v.shutdown();
                    v.awaitTermination(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Failed to shutdown PubSub client during destroy()", e);
                }
            }
        });

        if (pubExecutor != null) {
            pubExecutor.shutdownNow();
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private ByteString getMsg(T msg) {
        String json = gson.toJson(new DefaultTbQueueMsg(msg));
        return ByteString.copyFrom(json.getBytes());
    }

    /**
     * 功能：获取`Or Create Publisher`。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    private Publisher getOrCreatePublisher(String topic) {
        if (publisherMap.containsKey(topic)) {
            return publisherMap.get(topic);
        } else {
            try {
                admin.createTopicIfNotExists(topic);
                ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), topic);
                Publisher publisher = Publisher.newBuilder(topicName)
                        .setCredentialsProvider(pubSubSettings.getCredentialsProvider())
                        .setExecutorProvider(pubSubSettings.getExecutorProvider())
                        .build();
                publisherMap.put(topic, publisher);
                return publisher;
            } catch (IOException e) {
                log.error("Failed to create Publisher for the topic [{}].", topic, e);
                throw new RuntimeException("Failed to create Publisher for the topic.", e);
            }
        }

    }

}
