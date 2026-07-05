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
package org.thingsboard.rule.engine.geo;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：`TbGpsGeofencingFilterNode` 是GPS地理围栏过滤节点规则节点，用于执行 GPS 地理围栏、距离和多边形判断及状态跟踪。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGpsGeofencingFilterNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "gps geofencing filter",
        configClazz = TbGpsGeofencingFilterNodeConfiguration.class,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Filter incoming messages by GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from the incoming message and checks them according to configured perimeter. </br>" +
                "Configuration:</br></br>" +
                "<ul>" +
                "<li>Latitude key name - name of the message field that contains location latitude;</li>" +
                "<li>Longitude key name - name of the message field that contains location longitude;</li>" +
                "<li>Perimeter type - Polygon or Circle;</li>" +
                "<li>Fetch perimeter from message metadata - checkbox to load perimeter from message metadata; " +
                "   Enable if your perimeter is specific to device/asset and you store it as device/asset attribute;</li>" +
                "<li>Perimeter key name - name of the metadata key that stores perimeter information;</li>" +
                "<li>For Polygon perimeter type: <ul>" +
                "    <li>Polygon definition - string that contains array of coordinates in the following format: [[lat1, lon1],[lat2, lon2],[lat3, lon3], ... , [latN, lonN]]</li>" +
                "</ul></li>" +
                "<li>For Circle perimeter type: <ul>" +
                "   <li>Center latitude - latitude of the circle perimeter center;</li>" +
                "   <li>Center longitude - longitude of the circle perimeter center;</li>" +
                "   <li>Range - value of the circle perimeter range, double-precision floating-point value;</li>" +
                "   <li>Range units - one of: Meter, Kilometer, Foot, Mile, Nautical Mile;</li>" +
                "</ul></li></ul></br>" +
                "Rule node will use default metadata key names, if the \"Fetch perimeter from message metadata\" is enabled and \"Perimeter key name\" is not configured. " +
                "Default metadata key names for polygon perimeter type is \"perimeter\". Default metadata key names for circle perimeter are: \"centerLatitude\", \"centerLongitude\", \"range\", \"rangeUnit\"." +
                "</br></br>" +
                "Structure of the circle perimeter definition (stored in server-side attribute, for example):" +
                "</br></br>" +
                "{\"latitude\":  48.198618758582384, \"longitude\": 24.65322245153503, \"radius\":  100.0, \"radiusUnit\": \"METER\" }" +
                "</br></br>" +
                "Available radius units: METER, KILOMETER, FOOT, MILE, NAUTICAL_MILE;<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeGpsGeofencingConfig")
public class TbGpsGeofencingFilterNode extends AbstractGeofencingNode<TbGpsGeofencingFilterNodeConfiguration> {

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ctx.tellNext(msg, checkMatches(msg) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<TbGpsGeofencingFilterNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingFilterNodeConfiguration.class;
    }
    /*
     * 本类总结：`TbGpsGeofencingFilterNode` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
