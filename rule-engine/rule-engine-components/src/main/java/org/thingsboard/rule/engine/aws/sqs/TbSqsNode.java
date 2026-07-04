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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.common.util.concurrent.ListenableFuture;
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

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "aws sqs",
        configClazz = TbSqsNodeConfiguration.class,
        nodeDescription = "Publish messages to the AWS SQS",
        nodeDetails = "Will publish message payload and metadata attributes to the AWS SQS queue. Outbound message will contain " +
                "response fields (<code>messageId</code>, <code>requestId</code>, <code>messageBodyMd5</code>, <code>messageAttributesMd5</code>" +
                ", <code>sequenceNumber</code>) in the Message Metadata from the AWS SQS." +
                " For example <b>requestId</b> field can be accessed with <code>metadata.requestId</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeSqsConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4Ij48cGF0aCBkPSJNMTMuMjMgMTAuNTZWMTBjLTEuOTQgMC0zLjk5LjM5LTMuOTkgMi42NyAwIDEuMTYuNjEgMS45NSAxLjYzIDEuOTUuNzYgMCAxLjQzLS40NyAxLjg2LTEuMjIuNTItLjkzLjUtMS44LjUtMi44NG0yLjcgNi41M2MtLjE4LjE2LS40My4xNy0uNjMuMDYtLjg5LS43NC0xLjA1LTEuMDgtMS41NC0xLjc5LTEuNDcgMS41LTIuNTEgMS45NS00LjQyIDEuOTUtMi4yNSAwLTQuMDEtMS4zOS00LjAxLTQuMTcgMC0yLjE4IDEuMTctMy42NCAyLjg2LTQuMzggMS40Ni0uNjQgMy40OS0uNzYgNS4wNC0uOTNWNy41YzAtLjY2LjA1LTEuNDEtLjMzLTEuOTYtLjMyLS40OS0uOTUtLjctMS41LS43LTEuMDIgMC0xLjkzLjUzLTIuMTUgMS42MS0uMDUuMjQtLjI1LjQ4LS40Ny40OWwtMi42LS4yOGMtLjIyLS4wNS0uNDYtLjIyLS40LS41Ni42LTMuMTUgMy40NS00LjEgNi00LjEgMS4zIDAgMyAuMzUgNC4wMyAxLjMzQzE3LjExIDQuNTUgMTcgNi4xOCAxNyA3Ljk1djQuMTdjMCAxLjI1LjUgMS44MSAxIDIuNDguMTcuMjUuMjEuNTQgMCAuNzFsLTIuMDYgMS43OGgtLjAxIj48L3BhdGg+PHBhdGggZD0iTTIwLjE2IDE5LjU0QzE4IDIxLjE0IDE0LjgyIDIyIDEyLjEgMjJjLTMuODEgMC03LjI1LTEuNDEtOS44NS0zLjc2LS4yLS4xOC0uMDItLjQzLjI1LS4yOSAyLjc4IDEuNjMgNi4yNSAyLjYxIDkuODMgMi42MSAyLjQxIDAgNS4wNy0uNSA3LjUxLTEuNTMuMzctLjE2LjY2LjI0LjMyLjUxIj48L3BhdGg+PHBhdGggZD0iTTIxLjA3IDE4LjVjLS4yOC0uMzYtMS44NS0uMTctMi41Ny0uMDgtLjE5LjAyLS4yMi0uMTYtLjAzLS4zIDEuMjQtLjg4IDMuMjktLjYyIDMuNTMtLjMzLjI0LjMtLjA3IDIuMzUtMS4yNCAzLjMyLS4xOC4xNi0uMzUuMDctLjI2LS4xMS4yNi0uNjcuODUtMi4xNC41Ny0yLjV6Ij48L3BhdGg+PC9zdmc+"
)
/**
 * AWS SQS 外部发布节点，直接持有 AmazonSQS 客户端并向队列发送消息。
 * 本类不直接访问数据库或缓存；阻塞 SDK 调用放入 externalCallExecutor，回调决定成功或失败路由。
 */
public class TbSqsNode extends TbAbstractExternalNode {

    /**
     * SQS SendMessageResult 中 messageId 元数据键名。
     */
    private static final String MESSAGE_ID = "messageId";
    /**
     * AWS SDK 响应 requestId 元数据键名。
     */
    private static final String REQUEST_ID = "requestId";
    /**
     * SQS 消息体 MD5 元数据键名。
     */
    private static final String MESSAGE_BODY_MD5 = "messageBodyMd5";
    /**
     * SQS 消息属性 MD5 元数据键名。
     */
    private static final String MESSAGE_ATTRIBUTES_MD5 = "messageAttributesMd5";
    /**
     * FIFO 队列返回的 sequenceNumber 元数据键名。
     */
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    /**
     * SQS 发布异常写入消息元数据时使用的键名。
     */
    private static final String ERROR = "error";

    /**
     * SQS 节点配置，包含队列类型、队列 URL 模板、属性、凭据和区域。
     */
    private TbSqsNodeConfiguration config;
    /**
     * AWS SQS 客户端，生命周期由本节点 init/destroy 管理。
     */
    private AmazonSQS sqsClient;

    /**
     * 初始化 AWS SQS 客户端。
     * 本方法直接创建云 SDK 客户端；凭据、region 和超时来自节点配置/固定客户端配置。
     * 本方法本身不直接访问数据库或缓存，线程安全依赖 AWS SDK 客户端实现。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbSqsNodeConfiguration.class);
        AWSCredentials awsCredentials = new BasicAWSCredentials(this.config.getAccessKeyId(), this.config.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);
        try {
            this.sqsClient = AmazonSQSClientBuilder.standard()
                    .withCredentials(credProvider)
                    .withRegion(this.config.getRegion())
                    .withClientConfiguration(new ClientConfiguration()
                            .withConnectionTimeout(10000)
                            .withRequestTimeout(5000))
                    .build();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    /**
     * 处理 Rule Engine 消息并异步发送到 SQS。
     * 消息先经 ackIfNeeded 处理确认关系；发送成功走 Success，异常写入 error 元数据后走 Failure。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        withCallback(publishMessageAsync(ctx, tbMsg),
                m -> tellSuccess(ctx, m),
                t -> tellFailure(ctx, processException(tbMsg, t), t));
    }

    /**
     * 将 SQS 阻塞发送封装到外部调用执行器。
     * 本方法只定义异步边界，不直接访问数据库或缓存。
     */
    private ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(msg));
    }

    /**
     * 执行 AWS SQS sendMessage 外部调用。
     * Queue URL 和消息属性从配置模板解析；标准队列使用 delaySeconds，FIFO 队列使用消息 ID 和 originator 构造去重/分组字段。
     */
    private TbMsg publishMessage(TbMsg msg) {
        String queueUrl = TbNodeUtils.processPattern(this.config.getQueueUrlPattern(), msg);
        SendMessageRequest sendMsgRequest =  new SendMessageRequest();
        sendMsgRequest.withQueueUrl(queueUrl);
        sendMsgRequest.withMessageBody(msg.getData());
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        this.config.getMessageAttributes().forEach((k,v) -> {
            // SQS MessageAttribute 的名称和值都支持从当前 TbMsg 中解析模板。
            String name = TbNodeUtils.processPattern(k, msg);
            String val = TbNodeUtils.processPattern(v, msg);
            messageAttributes.put(name, new MessageAttributeValue().withDataType("String").withStringValue(val));
        });
        sendMsgRequest.setMessageAttributes(messageAttributes);
        if (this.config.getQueueType() == TbSqsNodeConfiguration.QueueType.STANDARD) {
            sendMsgRequest.withDelaySeconds(this.config.getDelaySeconds());
        } else {
            sendMsgRequest.withMessageDeduplicationId(msg.getId().toString());
            sendMsgRequest.withMessageGroupId(msg.getOriginator().toString());
        }
        SendMessageResult result = this.sqsClient.sendMessage(sendMsgRequest);
        return processSendMessageResult(msg, result);
    }

    /**
     * 将 SQS SendMessageResult 写入消息元数据。
     * 本方法只处理 SDK 返回值，不直接调用外部系统、数据库或缓存。
     */
    private TbMsg processSendMessageResult(TbMsg origMsg, SendMessageResult result) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(MESSAGE_ID, result.getMessageId());
        metaData.putValue(REQUEST_ID, result.getSdkResponseMetadata().getRequestId());
        if (!StringUtils.isEmpty(result.getMD5OfMessageBody())) {
            metaData.putValue(MESSAGE_BODY_MD5, result.getMD5OfMessageBody());
        }
        if (!StringUtils.isEmpty(result.getMD5OfMessageAttributes())) {
            metaData.putValue(MESSAGE_ATTRIBUTES_MD5, result.getMD5OfMessageAttributes());
        }
        if (!StringUtils.isEmpty(result.getSequenceNumber())) {
            metaData.putValue(SEQUENCE_NUMBER, result.getSequenceNumber());
        }
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 将 SQS 发送异常写入消息元数据。
     * 本方法供 Failure 路由携带错误信息，不直接执行外部调用。
     */
    private TbMsg processException(TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 关闭 AWS SQS 客户端。
     * 本方法直接结束云 SDK 客户端生命周期，不处理 Rule Engine 消息确认。
     */
    @Override
    public void destroy() {
        if (this.sqsClient != null) {
            try {
                this.sqsClient.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown SQS client during destroy()", e);
            }
        }
    }
}

/*
 * 本类总结：
 * 本类直接管理 AmazonSQS 客户端，并通过 externalCallExecutor 将 Rule Engine 消息发送到 SQS 队列。
 * 队列 URL、队列类型、延迟、消息属性、凭据和 region 来自配置；成功/失败路由由异步回调决定。
 * 本类本身不直接涉及数据库或缓存，相关行为只可能通过 Rule Engine 上下文、执行器或 AWS SDK 调用链间接涉及。
 */
