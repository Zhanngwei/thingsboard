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
package org.thingsboard.rule.engine.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.util.ReflectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.exception.ThingsboardKafkaClientError;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * `TbKafkaNode` 类，封装当前模块中的一组相关职责。
 */
@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "kafka",
        configClazz = TbKafkaNodeConfiguration.class,
        nodeDescription = "Publish messages to Kafka server",
        nodeDetails = "Will send record via Kafka producer to Kafka server. " +
                "Outbound message will contain response fields (<code>offset</code>, <code>partition</code>, <code>topic</code>)" +
                " from the Kafka in the Message Metadata. For example <b>partition</b> field can be accessed with <code>metadata.partition</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeKafkaConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUzOCIgaGVpZ2h0PSIyNTAwIiB2aWV3Qm94PSIwIDAgMjU2IDQxNiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBwcmVzZXJ2ZUFzcGVjdFJhdGlvPSJ4TWlkWU1pZCI+PHBhdGggZD0iTTIwMS44MTYgMjMwLjIxNmMtMTYuMTg2IDAtMzAuNjk3IDcuMTcxLTQwLjYzNCAxOC40NjFsLTI1LjQ2My0xOC4wMjZjMi43MDMtNy40NDIgNC4yNTUtMTUuNDMzIDQuMjU1LTIzLjc5NyAwLTguMjE5LTEuNDk4LTE2LjA3Ni00LjExMi0yMy40MDhsMjUuNDA2LTE3LjgzNWM5LjkzNiAxMS4yMzMgMjQuNDA5IDE4LjM2NSA0MC41NDggMTguMzY1IDI5Ljg3NSAwIDU0LjE4NC0yNC4zMDUgNTQuMTg0LTU0LjE4NCAwLTI5Ljg3OS0yNC4zMDktNTQuMTg0LTU0LjE4NC01NC4xODQtMjkuODc1IDAtNTQuMTg0IDI0LjMwNS01NC4xODQgNTQuMTg0IDAgNS4zNDguODA4IDEwLjUwNSAyLjI1OCAxNS4zODlsLTI1LjQyMyAxNy44NDRjLTEwLjYyLTEzLjE3NS0yNS45MTEtMjIuMzc0LTQzLjMzMy0yNS4xODJ2LTMwLjY0YzI0LjU0NC01LjE1NSA0My4wMzctMjYuOTYyIDQzLjAzNy01My4wMTlDMTI0LjE3MSAyNC4zMDUgOTkuODYyIDAgNjkuOTg3IDAgNDAuMTEyIDAgMTUuODAzIDI0LjMwNSAxNS44MDMgNTQuMTg0YzAgMjUuNzA4IDE4LjAxNCA0Ny4yNDYgNDIuMDY3IDUyLjc2OXYzMS4wMzhDMjUuMDQ0IDE0My43NTMgMCAxNzIuNDAxIDAgMjA2Ljg1NGMwIDM0LjYyMSAyNS4yOTIgNjMuMzc0IDU4LjM1NSA2OC45NHYzMi43NzRjLTI0LjI5OSA1LjM0MS00Mi41NTIgMjcuMDExLTQyLjU1MiA1Mi44OTQgMCAyOS44NzkgMjQuMzA5IDU0LjE4NCA1NC4xODQgNTQuMTg0IDI5Ljg3NSAwIDU0LjE4NC0yNC4zMDUgNTQuMTg0LTU0LjE4NCAwLTI1Ljg4My0xOC4yNTMtNDcuNTUzLTQyLjU1Mi01Mi44OTR2LTMyLjc3NWE2OS45NjUgNjkuOTY1IDAgMCAwIDQyLjYtMjQuNzc2bDI1LjYzMyAxOC4xNDNjLTEuNDIzIDQuODQtMi4yMiA5Ljk0Ni0yLjIyIDE1LjI0IDAgMjkuODc5IDI0LjMwOSA1NC4xODQgNTQuMTg0IDU0LjE4NCAyOS44NzUgMCA1NC4xODQtMjQuMzA1IDU0LjE4NC01NC4xODQgMC0yOS44NzktMjQuMzA5LTU0LjE4NC01NC4xODQtNTQuMTg0em0wLTEyNi42OTVjMTQuNDg3IDAgMjYuMjcgMTEuNzg4IDI2LjI3IDI2LjI3MXMtMTEuNzgzIDI2LjI3LTI2LjI3IDI2LjI3LTI2LjI3LTExLjc4Ny0yNi4yNy0yNi4yN2MwLTE0LjQ4MyAxMS43ODMtMjYuMjcxIDI2LjI3LTI2LjI3MXptLTE1OC4xLTQ5LjMzN2MwLTE0LjQ4MyAxMS43ODQtMjYuMjcgMjYuMjcxLTI2LjI3czI2LjI3IDExLjc4NyAyNi4yNyAyNi4yN2MwIDE0LjQ4My0xMS43ODMgMjYuMjctMjYuMjcgMjYuMjdzLTI2LjI3MS0xMS43ODctMjYuMjcxLTI2LjI3em01Mi41NDEgMzA3LjI3OGMwIDE0LjQ4My0xMS43ODMgMjYuMjctMjYuMjcgMjYuMjdzLTI2LjI3MS0xMS43ODctMjYuMjcxLTI2LjI3YzAtMTQuNDgzIDExLjc4NC0yNi4yNyAyNi4yNzEtMjYuMjdzMjYuMjcgMTEuNzg3IDI2LjI3IDI2LjI3em0tMjYuMjcyLTExNy45N2MtMjAuMjA1IDAtMzYuNjQyLTE2LjQzNC0zNi42NDItMzYuNjM4IDAtMjAuMjA1IDE2LjQzNy0zNi42NDIgMzYuNjQyLTM2LjY0MiAyMC4yMDQgMCAzNi42NDEgMTYuNDM3IDM2LjY0MSAzNi42NDIgMCAyMC4yMDQtMTYuNDM3IDM2LjYzOC0zNi42NDEgMzYuNjM4em0xMzEuODMxIDY3LjE3OWMtMTQuNDg3IDAtMjYuMjctMTEuNzg4LTI2LjI3LTI2LjI3MXMxMS43ODMtMjYuMjcgMjYuMjctMjYuMjcgMjYuMjcgMTEuNzg3IDI2LjI3IDI2LjI3YzAgMTQuNDgzLTExLjc4MyAyNi4yNzEtMjYuMjcgMjYuMjcxeiIvPjwvc3ZnPg=="
)
public class TbKafkaNode extends TbAbstractExternalNode {

    /**
     * 偏移量常量，用于统一引用固定值。
     */
    private static final String OFFSET = "offset";
    /**
     * 分区常量，用于统一引用固定值。
     */
    private static final String PARTITION = "partition";
    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String TOPIC = "topic";
    /**
     * 错误信息常量，用于统一引用固定值。
     */
    private static final String ERROR = "error";
    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final String TB_MSG_MD_PREFIX = "tb_msg_md_";
    /**
     * 字段名常量，用于统一引用固定值。
     */
    private static final Field IO_THREAD_FIELD = ReflectionUtils.findField(KafkaProducer.class, "ioThread");

    static {
        IO_THREAD_FIELD.setAccessible(true);
    }

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbKafkaNodeConfiguration config;
    /**
     * 是否满足键条件。
     */
    private boolean addMetadataKeyValuesAsKafkaHeaders;
    /**
     * `toBytesCharset`集合，用于去重保存或快速判断对象是否存在。
     */
    private Charset toBytesCharset;

    /**
     * 消息生产者，承载当前流程需要传递的内容。
     */
    private Producer<String, String> producer;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    private Throwable initError;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbKafkaNodeConfiguration.class);
        this.initError = null;
        Properties properties = new Properties();
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "producer-tb-kafka-node-" + ctx.getSelfId().getId().toString() + "-" + ctx.getServiceId());
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.getValueSerializer());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.getKeySerializer());
        properties.put(ProducerConfig.ACKS_CONFIG, config.getAcks());
        properties.put(ProducerConfig.RETRIES_CONFIG, config.getRetries());
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getBatchSize());
        properties.put(ProducerConfig.LINGER_MS_CONFIG, config.getLinger());
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.getBufferMemory());
        if (config.getOtherProperties() != null) {
            config.getOtherProperties().forEach((k, v) -> {
                if (SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG.equals(k)
                        || SslConfigs.SSL_KEYSTORE_KEY_CONFIG.equals(k)
                        || SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG.equals(k)) {
                    v = v.replace("\\n", "\n");
                }
                properties.put(k, v);
            });
        }
        addMetadataKeyValuesAsKafkaHeaders = BooleanUtils.toBooleanDefaultIfNull(config.isAddMetadataKeyValuesAsKafkaHeaders(), false);
        toBytesCharset = config.getKafkaHeadersCharset() != null ? Charset.forName(config.getKafkaHeadersCharset()) : StandardCharsets.UTF_8;
        try {
            this.producer = new KafkaProducer<>(properties);
            Thread ioThread = (Thread) ReflectionUtils.getField(IO_THREAD_FIELD, producer);
            ioThread.setUncaughtExceptionHandler((thread, throwable) -> {
                if (throwable instanceof ThingsboardKafkaClientError) {
                    initError = throwable;
                    destroy();
                }
            });
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String topic = TbNodeUtils.processPattern(config.getTopicPattern(), msg);
        String keyPattern = config.getKeyPattern();
        var tbMsg = ackIfNeeded(ctx, msg);
        try {
            if (initError != null) {
                ctx.tellFailure(tbMsg, new RuntimeException("Failed to initialize Kafka rule node producer: " + initError.getMessage()));
            } else {
                ctx.getExternalCallExecutor().executeAsync(() -> {
                    publish(
                            ctx,
                            tbMsg,
                            topic,
                            keyPattern == null || keyPattern.isEmpty()
                                    ? null
                                    : TbNodeUtils.processPattern(config.getKeyPattern(), tbMsg)
                    );
                    return null;
                });
            }
        } catch (Exception e) {
            ctx.tellFailure(tbMsg, e);
        }
    }

    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `topic`：主题名称或主题对象。
     * - `key`：键。
     * 返回：无。
     */
    protected void publish(TbContext ctx, TbMsg msg, String topic, String key) {
        try {
            if (!addMetadataKeyValuesAsKafkaHeaders) {
                //TODO: external system executor
                producer.send(new ProducerRecord<>(topic, key, msg.getData()),
                        (metadata, e) -> processRecord(ctx, msg, metadata, e));
            } else {
                Headers headers = new RecordHeaders();
                msg.getMetaData().values().forEach((k, v) -> headers.add(new RecordHeader(TB_MSG_MD_PREFIX + k, v.getBytes(toBytesCharset))));
                producer.send(new ProducerRecord<>(topic, null, null, key, msg.getData(), headers),
                        (metadata, e) -> processRecord(ctx, msg, metadata, e));
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to process message: {}", ctx.getSelfId(), msg, e);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (this.producer != null) {
            try {
                this.producer.close();
            } catch (Exception e) {
                log.error("Failed to close producer during destroy()", e);
            }
        }
    }

    /**
     * 功能：处理`Record`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `metadata`：待处理数据。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private void processRecord(TbContext ctx, TbMsg msg, RecordMetadata metadata, Exception e) {
        if (e == null) {
            tellSuccess(ctx, processResponse(msg, metadata));
        } else {
            tellFailure(ctx, processException(msg, e), e);
        }
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `recordMetadata`：待处理数据。
     * 返回：处理结果。
     */
    private TbMsg processResponse(TbMsg origMsg, RecordMetadata recordMetadata) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(OFFSET, String.valueOf(recordMetadata.offset()));
        metaData.putValue(PARTITION, String.valueOf(recordMetadata.partition()));
        metaData.putValue(TOPIC, recordMetadata.topic());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 功能：处理`Exception`。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `e`：`e` 参数。
     * 返回：处理结果。
     */
    private TbMsg processException(TbMsg origMsg, Exception e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

}

/*
 * 本类总结：
 * 本类直接管理 KafkaProducer，并把 Rule Engine 消息异步发送到 Kafka。
 * Topic/key、Producer 参数和 Header 行为来自节点配置；ackIfNeeded 先处理消息确认，Kafka send 回调再决定成功或失败路由。
 * 本类本身不直接涉及数据库或缓存，相关行为只可能通过 Rule Engine 上下文、执行器或 Kafka 客户端内部线程间接发生。
 */
