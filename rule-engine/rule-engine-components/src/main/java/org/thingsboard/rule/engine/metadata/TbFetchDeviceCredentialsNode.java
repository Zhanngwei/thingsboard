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

/**
 * 中文说明：
 * 1. `TbFetchDeviceCredentialsNode` 是 ThingsBoard Rule Engine Components 中处理设备凭据的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractNodeWithFetchTo`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
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
public class TbFetchDeviceCredentialsNode extends TbAbstractNodeWithFetchTo<TbFetchDeviceCredentialsNodeConfiguration> {

    /**
     * 凭据常量，用于统一引用固定值。
     */
    private static final String CREDENTIALS = "credentials";
    /**
     * 凭据常量，用于统一引用固定值。
     */
    private static final String CREDENTIALS_TYPE = "credentialsType";

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbFetchDeviceCredentialsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbFetchDeviceCredentialsNodeConfiguration.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var originator = msg.getOriginator();
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type: " + originator.getEntityType() + "!"));
            return;
        }

        var deviceId = new DeviceId(msg.getOriginator().getId());
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

    /**
     * 功能：执行 `upgrade` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "fetchToMetadata",
                        TbMsgSource.METADATA.name(),
                        TbMsgSource.DATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }
}
