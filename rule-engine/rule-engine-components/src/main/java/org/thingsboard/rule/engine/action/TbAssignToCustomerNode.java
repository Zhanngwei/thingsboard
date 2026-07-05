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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * 中文说明：`TbAssignToCustomerNode` 是分配到客户节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAssignToCustomerNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "assign to customer",
        configClazz = TbAssignToCustomerNodeConfiguration.class,
        nodeDescription = "Assign Message Originator Entity to Customer",
        nodeDetails = "Finds target Customer by customer name pattern and then assign Originator Entity to this customer. " +
                "Will create new Customer if it doesn't exists and 'Create new Customer if not exists' is set to true.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAssignToCustomerConfig",
        icon = "add_circle"
)
public class TbAssignToCustomerNode extends TbAbstractCustomerActionNode<TbAssignToCustomerNodeConfiguration> {

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean createCustomerIfNotExists() {
        return config.isCreateCustomerIfNotExists();
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbAssignToCustomerNodeConfiguration loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAssignToCustomerNodeConfiguration.class);
    }

    /**
     * 功能：执行 `doProcessCustomerAction` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    protected void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId) {
        processAssign(ctx, msg, customerId);
    }

    /**
     * 功能：处理`Assign`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void processAssign(TbContext ctx, TbMsg msg, CustomerId customerId) {
        EntityType originatorType = msg.getOriginator().getEntityType();
        switch (originatorType) {
            case DEVICE:
                processAssignDevice(ctx, msg, customerId);
                break;
            case ASSET:
                processAssignAsset(ctx, msg, customerId);
                break;
            case ENTITY_VIEW:
                processAssignEntityView(ctx, msg, customerId);
                break;
            case EDGE:
                processAssignEdge(ctx, msg, customerId);
                break;
            case DASHBOARD:
                processAssignDashboard(ctx, msg, customerId);
                break;
            default:
                ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + originatorType +
                        "'! Only 'DEVICE', 'ASSET',  'ENTITY_VIEW' or 'DASHBOARD' types are allowed."));
                break;
        }
    }

    /**
     * 功能：处理资产。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void processAssignAsset(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()), customerId);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void processAssignDevice(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()), customerId);
    }

    /**
     * 功能：处理实体视图。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void processAssignEntityView(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()), customerId);
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void processAssignEdge(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getEdgeService().assignEdgeToCustomer(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId()), customerId);
    }

    /**
     * 功能：处理仪表盘。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void processAssignDashboard(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().assignDashboardToCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

    /*
     * 本类总结：`TbAssignToCustomerNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
