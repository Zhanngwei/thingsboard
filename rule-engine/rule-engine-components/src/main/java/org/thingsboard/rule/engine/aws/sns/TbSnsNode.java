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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "aws sns",
        configClazz = TbSnsNodeConfiguration.class,
        nodeDescription = "Publish message to the AWS SNS",
        nodeDetails = "Will publish message payload to the AWS SNS topic. Outbound message will contain response fields " +
                "(<code>messageId</code>, <code>requestId</code>) in the Message Metadata from the AWS SNS. " +
                "For example <b>requestId</b> field can be accessed with <code>metadata.requestId</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeSnsConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4Ij48cGF0aCBkPSJNMTMuMjMgMTAuNTZWMTBjLTEuOTQgMC0zLjk5LjM5LTMuOTkgMi42NyAwIDEuMTYuNjEgMS45NSAxLjYzIDEuOTUuNzYgMCAxLjQzLS40NyAxLjg2LTEuMjIuNTItLjkzLjUtMS44LjUtMi44NG0yLjcgNi41M2MtLjE4LjE2LS40My4xNy0uNjMuMDYtLjg5LS43NC0xLjA1LTEuMDgtMS41NC0xLjc5LTEuNDcgMS41LTIuNTEgMS45NS00LjQyIDEuOTUtMi4yNSAwLTQuMDEtMS4zOS00LjAxLTQuMTcgMC0yLjE4IDEuMTctMy42NCAyLjg2LTQuMzggMS40Ni0uNjQgMy40OS0uNzYgNS4wNC0uOTNWNy41YzAtLjY2LjA1LTEuNDEtLjMzLTEuOTYtLjMyLS40OS0uOTUtLjctMS41LS43LTEuMDIgMC0xLjkzLjUzLTIuMTUgMS42MS0uMDUuMjQtLjI1LjQ4LS40Ny40OWwtMi42LS4yOGMtLjIyLS4wNS0uNDYtLjIyLS40LS41Ni42LTMuMTUgMy40NS00LjEgNi00LjEgMS4zIDAgMyAuMzUgNC4wMyAxLjMzQzE3LjExIDQuNTUgMTcgNi4xOCAxNyA3Ljk1djQuMTdjMCAxLjI1LjUgMS44MSAxIDIuNDguMTcuMjUuMjEuNTQgMCAuNzFsLTIuMDYgMS43OGgtLjAxIj48L3BhdGg+PHBhdGggZD0iTTIwLjE2IDE5LjU0QzE4IDIxLjE0IDE0LjgyIDIyIDEyLjEgMjJjLTMuODEgMC03LjI1LTEuNDEtOS44NS0zLjc2LS4yLS4xOC0uMDItLjQzLjI1LS4yOSAyLjc4IDEuNjMgNi4yNSAyLjYxIDkuODMgMi42MSAyLjQxIDAgNS4wNy0uNSA3LjUxLTEuNTMuMzctLjE2LjY2LjI0LjMyLjUxIj48L3BhdGg+PHBhdGggZD0iTTIxLjA3IDE4LjVjLS4yOC0uMzYtMS44NS0uMTctMi41Ny0uMDgtLjE5LjAyLS4yMi0uMTYtLjAzLS4zIDEuMjQtLjg4IDMuMjktLjYyIDMuNTMtLjMzLjI0LjMtLjA3IDIuMzUtMS4yNCAzLjMyLS4xOC4xNi0uMzUuMDctLjI2LS4xMS4yNi0uNjcuODUtMi4xNC41Ny0yLjV6Ij48L3BhdGg+PC9zdmc+"
)
/**
 * AWS SNS 外部发布节点，直接持有 AmazonSNS 客户端并发布消息到 SNS Topic。
 * 本类不直接访问数据库或缓存；阻塞 SDK 调用被放入 externalCallExecutor，回调决定成功或失败路由。
 */
public class TbSnsNode extends TbAbstractExternalNode {

    /**
     * SNS 发布结果中的 messageId 元数据键名。
     */
    private static final String MESSAGE_ID = "messageId";
    /**
     * AWS SDK 响应 requestId 元数据键名。
     */
    private static final String REQUEST_ID = "requestId";
    /**
     * SNS 发布异常写入消息元数据时使用的键名。
     */
    private static final String ERROR = "error";

    /**
     * SNS 节点配置，包含 topic ARN 模板、访问密钥和区域。
     */
    private TbSnsNodeConfiguration config;
    /**
     * AWS SNS 客户端，生命周期由本节点 init/destroy 管理。
     */
    private AmazonSNS snsClient;

    /**
     * 初始化 AWS SNS 客户端。
     * 本方法直接创建云 SDK 客户端；认证和 region 来自节点配置。
     * 本方法本身不直接访问数据库或缓存，线程安全依赖 AWS SDK 客户端实现。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbSnsNodeConfiguration.class);
        AWSCredentials awsCredentials = new BasicAWSCredentials(this.config.getAccessKeyId(), this.config.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);
        try {
            this.snsClient = AmazonSNSClient.builder()
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
     * 处理 Rule Engine 消息并异步发布到 AWS SNS。
     * 消息先经 ackIfNeeded 处理确认关系；发布成功走 Success，异常写入 error 元数据后走 Failure。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var tbMsg = ackIfNeeded(ctx, msg);
        withCallback(publishMessageAsync(ctx, tbMsg),
                m -> tellSuccess(ctx, m),
                t -> tellFailure(ctx, processException(ctx, tbMsg, t), t));
    }

    /**
     * 将 SNS 阻塞发布封装到 Rule Engine 外部调用执行器。
     * 本方法定义异步边界，不直接访问数据库或缓存。
     */
    private ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(ctx, msg));
    }

    /**
     * 执行 AWS SNS publish 外部调用。
     * Topic ARN 从配置模板解析，消息体来自 TbMsg data；返回值被转换为成功路由元数据。
     */
    private TbMsg publishMessage(TbContext ctx, TbMsg msg) {
        String topicArn = TbNodeUtils.processPattern(this.config.getTopicArnPattern(), msg);
        PublishRequest publishRequest = new PublishRequest()
                .withTopicArn(topicArn)
                .withMessage(msg.getData());
        PublishResult result = this.snsClient.publish(publishRequest);
        return processPublishResult(ctx, msg, result);
    }

    /**
     * 将 SNS PublishResult 写入消息元数据。
     * 本方法只处理 SDK 返回值，不直接访问外部系统、数据库或缓存。
     */
    private TbMsg processPublishResult(TbContext ctx, TbMsg origMsg, PublishResult result) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(MESSAGE_ID, result.getMessageId());
        metaData.putValue(REQUEST_ID, result.getSdkResponseMetadata().getRequestId());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 将 SNS 发布异常写入消息元数据。
     * 本方法不直接执行外部调用，供 Failure 路由携带错误信息。
     */
    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 关闭 AWS SNS 客户端。
     * 本方法直接结束云 SDK 客户端生命周期，不执行 Rule Engine 消息确认。
     */
    @Override
    public void destroy() {
        if (this.snsClient != null) {
            try {
                this.snsClient.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown SNS client during destroy()", e);
            }
        }
    }
}

/*
 * 本类总结：
 * 本类直接管理 AmazonSNS 客户端，并把 Rule Engine 消息通过 externalCallExecutor 发布到 AWS SNS。
 * Topic ARN、凭据和 region 来自配置；消息确认在发布前处理，异步回调决定成功或失败路由。
 * 本类本身不直接涉及数据库或缓存，相关行为只可能通过 Rule Engine 上下文、执行器或 AWS SDK 调用链间接涉及。
 */
