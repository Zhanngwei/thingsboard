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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

/**
 * 中文说明：`TbMsgDeleteAttributesNode` 是消息删除属性节点规则节点，用于保存、删除或通知属性与时间序列遥测数据。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbMsgDeleteAttributesNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete attributes",
        configClazz = TbMsgDeleteAttributesNodeConfiguration.class,
        nodeDescription = "Delete attributes for Message Originator.",
        nodeDetails = "Attempt to remove attributes by selected keys. If msg originator doesn't have an attribute with " +
                " a key selected in the configuration, it will be ignored. If delete operation is completed successfully, " +
                " rule node will send the \"Attributes Deleted\" event to the root chain of the message originator and " +
                " send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeleteAttributesConfig",
        icon = "remove_circle"
)
public class TbMsgDeleteAttributesNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgDeleteAttributesNodeConfiguration config;
    /**
     * `keys`列表，用于保存一组待处理对象。
     */
    private List<String> keys;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeleteAttributesNodeConfiguration.class);
        this.keys = config.getKeys();
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
        List<String> keysToDelete = keys.stream()
                .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                .distinct()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (keysToDelete.isEmpty()) {
            ctx.tellSuccess(msg);
        } else {
            String scope = getScope(msg.getMetaData().getValue(SCOPE));
            ctx.getTelemetryService().deleteAndNotify(
                    ctx.getTenantId(),
                    msg.getOriginator(),
                    scope,
                    keysToDelete,
                    checkNotifyDevice(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY), scope),
                    config.isSendAttributesDeletedNotification() ?
                            new AttributesDeleteNodeCallback(ctx, msg, scope, keysToDelete) :
                            new TelemetryNodeCallback(ctx, msg)
            );
        }
    }

    /**
     * 功能：获取`Scope`。
     * 参数：
     * - `mdScopeValue`：值。
     * 返回：文本结果。
     */
    private String getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return mdScopeValue;
        }
        return config.getScope();
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `notifyDeviceMdValue`：设备信息或设备标识。
     * - `scope`：`scope` 参数。
     * 返回：判断结果。
     */
    private boolean checkNotifyDevice(String notifyDeviceMdValue, String scope) {
        return SHARED_SCOPE.equals(scope) && (config.isNotifyDevice() || Boolean.parseBoolean(notifyDeviceMdValue));
    }

    /*
     * 本类总结：`TbMsgDeleteAttributesNode` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
