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

/**
 * `TbSqsNode` 类，封装当前模块中的一组相关职责。
 */
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
public class TbSqsNode extends TbAbstractExternalNode {

    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String MESSAGE_ID = "messageId";
    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final String REQUEST_ID = "requestId";
    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String MESSAGE_BODY_MD5 = "messageBodyMd5";
    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String MESSAGE_ATTRIBUTES_MD5 = "messageAttributesMd5";
    /**
     * `SEQUENCE_NUMBER`常量，用于统一引用固定值。
     */
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    /**
     * 错误信息常量，用于统一引用固定值。
     */
    private static final String ERROR = "error";

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbSqsNodeConfiguration config;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private AmazonSQS sqsClient;

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
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        withCallback(publishMessageAsync(ctx, tbMsg),
                m -> tellSuccess(ctx, m),
                t -> tellFailure(ctx, processException(tbMsg, t), t));
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(msg));
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private TbMsg publishMessage(TbMsg msg) {
        String queueUrl = TbNodeUtils.processPattern(this.config.getQueueUrlPattern(), msg);
        SendMessageRequest sendMsgRequest =  new SendMessageRequest();
        sendMsgRequest.withQueueUrl(queueUrl);
        sendMsgRequest.withMessageBody(msg.getData());
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        this.config.getMessageAttributes().forEach((k,v) -> {
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
     * 功能：处理消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `result`：`result` 参数。
     * 返回：处理结果。
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
     * 功能：处理`Exception`。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    private TbMsg processException(TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
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
