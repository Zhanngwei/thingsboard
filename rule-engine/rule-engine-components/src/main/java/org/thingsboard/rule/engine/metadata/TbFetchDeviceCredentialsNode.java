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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "fetch device credentials",
        version = 1,
        configClazz = TbFetchDeviceCredentialsNodeConfiguration.class,
        nodeDescription = "Adds device credentials to the message or message metadata",
        nodeDetails = "if message originator type is Device and device credentials was successfully fetched, " +
                "rule node enriches message or message metadata with <i>credentialsType</i> and <i>credentials</i> properties. " +
                "Useful when you need to fetch device credentials and use them for further message processing. " +
                "For example, use device credentials to interact with external systems.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeFetchDeviceCredentialsConfig")
/**
 * 中文说明：`TbFetchDeviceCredentialsNode` 是获取设备凭据节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbFetchDeviceCredentialsNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbFetchDeviceCredentialsNode extends TbAbstractNodeWithFetchTo<TbFetchDeviceCredentialsNodeConfiguration> {

    /**
     * 常量字段：定义 `CREDENTIALS`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final String CREDENTIALS = "credentials";
    /**
     * 常量字段：定义 `CREDENTIALS_TYPE`，用于类型匹配条件，本身不触发外部系统调用。
     */
    private static final String CREDENTIALS_TYPE = "credentialsType";

    @Override
    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbFetchDeviceCredentialsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected TbFetchDeviceCredentialsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbFetchDeviceCredentialsNodeConfiguration.class);
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `DeviceCredentialsService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var originator = msg.getOriginator();
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type: " + originator.getEntityType() + "!"));
            return;
        }

        var deviceId = new DeviceId(msg.getOriginator().getId());
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        var deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(ctx.getTenantId(), deviceId);
        if (deviceCredentials == null) {
            ctx.tellFailure(msg, new RuntimeException("Failed to get Device Credentials for device: " + deviceId + "!"));
            return;
        }
        var credentialsType = deviceCredentials.getCredentialsType();
        var credentialsInfo = ctx.getDeviceCredentialsService().toCredentialsInfo(deviceCredentials);
        var metaData = msg.getMetaData().copy();
        if (TbMsgSource.METADATA.equals(fetchTo)) {
            metaData.putValue(CREDENTIALS_TYPE, credentialsType.name());
            if (credentialsType.equals(DeviceCredentialsType.ACCESS_TOKEN) || credentialsType.equals(DeviceCredentialsType.X509_CERTIFICATE)) {
                metaData.putValue(CREDENTIALS, credentialsInfo.asText());
            } else {
                metaData.putValue(CREDENTIALS, JacksonUtil.toString(credentialsInfo));
            }
        } else if (TbMsgSource.DATA.equals(fetchTo)) {
            msgDataAsObjectNode.put(CREDENTIALS_TYPE, credentialsType.name());
            msgDataAsObjectNode.set(CREDENTIALS, credentialsInfo);
        }
        TbMsg transformedMsg = transformMessage(msg, msgDataAsObjectNode, metaData);
        ctx.tellSuccess(transformedMsg);
    }

    @Override
    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "fetchToMetadata",
                        TbMsgSource.METADATA.name(),
                        TbMsgSource.DATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }

    /*
     * 本类总结：`TbFetchDeviceCredentialsNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
