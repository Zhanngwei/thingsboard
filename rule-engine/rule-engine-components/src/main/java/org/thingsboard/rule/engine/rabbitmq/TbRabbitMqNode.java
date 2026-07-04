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
package org.thingsboard.rule.engine.rabbitmq;

import com.google.common.util.concurrent.ListenableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "rabbitmq",
        configClazz = TbRabbitMqNodeConfiguration.class,
        nodeDescription = "Publish messages to the RabbitMQ",
        nodeDetails = "Will publish message payload to RabbitMQ queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeRabbitMqConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbDpzcGFjZT0icHJlc2VydmUiIHZlcnNpb249IjEuMSIgeT0iMHB4IiB4PSIwcHgiIHZpZXdCb3g9IjAgMCAxMDAwIDEwMDAiPjxwYXRoIHN0cm9rZS13aWR0aD0iLjg0OTU2IiBkPSJtODYwLjQ3IDQxNi4zMmgtMjYyLjAxYy0xMi45MTMgMC0yMy42MTgtMTAuNzA0LTIzLjYxOC0yMy42MTh2LTI3Mi43MWMwLTIwLjMwNS0xNi4yMjctMzYuMjc2LTM2LjI3Ni0zNi4yNzZoLTkzLjc5MmMtMjAuMzA1IDAtMzYuMjc2IDE2LjIyNy0zNi4yNzYgMzYuMjc2djI3MC44NGMtMC4yNTQ4NyAxNC4xMDMtMTEuNDY5IDI1LjU3Mi0yNS43NDIgMjUuNTcybC04NS42MzYgMC42Nzk2NWMtMTQuMTAzIDAtMjUuNTcyLTExLjQ2OS0yNS41NzItMjUuNTcybDAuNjc5NjUtMjcxLjUyYzAtMjAuMzA1LTE2LjIyNy0zNi4yNzYtMzYuMjc2LTM2LjI3NmgtOTMuNTM3Yy0yMC4zMDUgMC0zNi4yNzYgMTYuMjI3LTM2LjI3NiAzNi4yNzZ2NzYzLjg0YzAgMTguMDk2IDE0Ljc4MiAzMi40NTMgMzIuNDUzIDMyLjQ1M2g3MjIuODFjMTguMDk2IDAgMzIuNDUzLTE0Ljc4MiAzMi40NTMtMzIuNDUzdi00MzUuMzFjLTEuMTg5NC0xOC4xODEtMTUuMjkyLTMyLjE5OC0zMy4zODgtMzIuMTk4em0tMTIyLjY4IDI4Ny4wN2MwIDIzLjYxOC0xOC44NiA0Mi40NzgtNDIuNDc4IDQyLjQ3OGgtNzMuOTk3Yy0yMy42MTggMC00Mi40NzgtMTguODYtNDIuNDc4LTQyLjQ3OHYtNzQuMjUyYzAtMjMuNjE4IDE4Ljg2LTQyLjQ3OCA0Mi40NzgtNDIuNDc4aDczLjk5N2MyMy42MTggMCA0Mi40NzggMTguODYgNDIuNDc4IDQyLjQ3OHoiLz48L3N2Zz4="
)
/**
 * RabbitMQ 外部发布节点，直接管理 RabbitMQ Connection/Channel 并发布消息。
 * 本类不直接访问数据库或缓存；阻塞发布被放入 Rule Engine 外部调用执行器，回调决定成功或失败路由。
 */
public class TbRabbitMqNode extends TbAbstractExternalNode {

    /**
     * RabbitMQ 消息体编码使用的字符集。
     */
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * RabbitMQ 发布异常写入消息元数据时使用的键名。
     */
    private static final String ERROR = "error";

    /**
     * RabbitMQ 节点配置，包含连接参数、exchange/routingKey 模板和属性名称。
     */
    private TbRabbitMqNodeConfiguration config;

    /**
     * RabbitMQ 连接，生命周期由 init/destroy 直接管理。
     */
    private Connection connection;
    /**
     * RabbitMQ Channel，实际 basicPublish 调用通过该对象完成。
     */
    private Channel channel;

    /**
     * 初始化 RabbitMQ 连接工厂、连接和 Channel。
     * 本方法直接触达 RabbitMQ 客户端连接生命周期；连接参数来自节点配置。
     * 本方法本身不直接涉及数据库或缓存，线程安全依赖 RabbitMQ Channel 使用方式和 Rule Engine 调度。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbRabbitMqNodeConfiguration.class);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.config.getHost());
        factory.setPort(this.config.getPort());
        factory.setVirtualHost(this.config.getVirtualHost());
        factory.setUsername(this.config.getUsername());
        factory.setPassword(this.config.getPassword());
        factory.setAutomaticRecoveryEnabled(this.config.isAutomaticRecoveryEnabled());
        factory.setConnectionTimeout(this.config.getConnectionTimeout());
        factory.setHandshakeTimeout(this.config.getHandshakeTimeout());
        // 将配置中的客户端属性透传给 RabbitMQ ConnectionFactory。
        this.config.getClientProperties().forEach((k,v) -> factory.getClientProperties().put(k,v));
        try {
            this.connection = factory.newConnection();
            this.channel = this.connection.createChannel();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    /**
     * 处理 Rule Engine 消息并异步发布到 RabbitMQ。
     * 消息先通过 ackIfNeeded 处理确认关系，再在外部调用执行器中执行 basicPublish。
     * 发布成功走 Success，异常会写入 error 元数据后走 Failure。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        withCallback(publishMessageAsync(ctx, tbMsg),
                m -> tellSuccess(ctx, m),
                t -> tellFailure(ctx, processException(tbMsg, t), t));
    }

    /**
     * 将 RabbitMQ 阻塞发布封装为 ListenableFuture。
     * 本方法本身不直接发布消息，只定义异步执行边界。
     */
    private ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(ctx, msg));
    }

    /**
     * 执行 RabbitMQ basicPublish 外部调用。
     * exchange、routingKey 和 messageProperties 都来自配置并可结合 TbMsg 解析；本方法不直接访问数据库或缓存。
     */
    private TbMsg publishMessage(TbContext ctx, TbMsg msg) throws Exception {
        String exchangeName = "";
        if (!StringUtils.isEmpty(this.config.getExchangeNamePattern())) {
            exchangeName = TbNodeUtils.processPattern(this.config.getExchangeNamePattern(), msg);
        }
        String routingKey = "";
        if (!StringUtils.isEmpty(this.config.getRoutingKeyPattern())) {
            routingKey = TbNodeUtils.processPattern(this.config.getRoutingKeyPattern(), msg);
        }
        AMQP.BasicProperties properties = null;
        if (!StringUtils.isEmpty(this.config.getMessageProperties())) {
            properties = convert(this.config.getMessageProperties());
        }
        // basicPublish 是本节点的外部系统调用边界，调用完成后返回原消息继续成功路由。
        channel.basicPublish(
                exchangeName,
                routingKey,
                properties,
                msg.getData().getBytes(UTF8));
        return msg;
    }

    /**
     * 将 RabbitMQ 发布异常写入消息元数据，供 Failure 路由读取。
     * 本方法只处理本地消息副本，不直接访问 RabbitMQ、数据库或缓存。
     */
    private TbMsg processException(TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 关闭 RabbitMQ 连接。
     * 本方法直接结束 RabbitMQ 连接生命周期，不执行消息确认或失败路由。
     */
    @Override
    public void destroy() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                log.error("Failed to close connection during destroy()", e);
            }
        }
    }

    /**
     * 将配置中的消息属性名称转换为 RabbitMQ BasicProperties 常量。
     * 本方法不直接调用 RabbitMQ Broker，也不涉及数据库或缓存。
     */
    private static AMQP.BasicProperties convert(String name) throws TbNodeException {
        switch (name) {
            case "BASIC":
                return MessageProperties.BASIC;
            case "TEXT_PLAIN":
                return MessageProperties.TEXT_PLAIN;
            case "MINIMAL_BASIC":
                return MessageProperties.MINIMAL_BASIC;
            case "MINIMAL_PERSISTENT_BASIC":
                return MessageProperties.MINIMAL_PERSISTENT_BASIC;
            case "PERSISTENT_BASIC":
                return MessageProperties.PERSISTENT_BASIC;
            case "PERSISTENT_TEXT_PLAIN":
                return MessageProperties.PERSISTENT_TEXT_PLAIN;
            default:
                throw new TbNodeException("Message Properties: '" + name + "' is undefined!");
        }
    }
}

/*
 * 本类总结：
 * 本类直接管理 RabbitMQ Connection/Channel，并通过 externalCallExecutor 异步执行 basicPublish。
 * exchange、routingKey、消息属性和连接参数来自配置；Rule Engine 消息在发布前确认，异步回调决定成功或失败路由。
 * 本类本身不直接涉及数据库或缓存，相关行为只可能通过 Rule Engine 执行器或上下文调用链间接涉及。
 */

