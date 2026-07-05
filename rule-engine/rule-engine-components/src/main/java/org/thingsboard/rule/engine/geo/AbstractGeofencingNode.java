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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.List;

/**
 * 中文说明：`AbstractGeofencingNode` 是抽象地理围栏节点规则节点，用于执行 GPS 地理围栏、距离和多边形判断及状态跟踪。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class AbstractGeofencingNode<T extends TbGpsGeofencingFilterNodeConfiguration> implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    protected T config;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    protected JtsSpatialContext jtsCtx;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, getConfigClazz());
        JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
        factory.normWrapLongitude = true;
        jtsCtx = factory.newSpatialContext();
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    abstract protected Class<T> getConfigClazz();

    /**
     * 功能：校验`Matches`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    protected boolean checkMatches(TbMsg msg) throws TbNodeException {
        JsonElement msgDataElement = JsonParser.parseString(msg.getData());
        if (!msgDataElement.isJsonObject()) {
            throw new TbNodeException("Incoming Message is not a valid JSON object!");
        }
        JsonObject msgDataObj = msgDataElement.getAsJsonObject();
        double latitude = getValueFromMessageByName(msg, msgDataObj, config.getLatitudeKeyName());
        double longitude = getValueFromMessageByName(msg, msgDataObj, config.getLongitudeKeyName());
        List<Perimeter> perimeters = getPerimeters(msg);
        boolean matches = false;
        for (Perimeter perimeter : perimeters) {
            if (checkMatches(perimeter, latitude, longitude)) {
                matches = true;
                break;
            }
        }
        return matches;
    }

    /**
     * 功能：校验`Matches`。
     * 参数：
     * - `perimeter`：`perimeter` 参数。
     * - `latitude`：`latitude` 参数。
     * - `longitude`：`longitude` 参数。
     * 返回：判断结果。
     */
    protected boolean checkMatches(Perimeter perimeter, double latitude, double longitude) throws TbNodeException {
        if (perimeter.getPerimeterType() == PerimeterType.CIRCLE) {
            Coordinates entityCoordinates = new Coordinates(latitude, longitude);
            Coordinates perimeterCoordinates = new Coordinates(perimeter.getCenterLatitude(), perimeter.getCenterLongitude());
            return perimeter.getRange() > GeoUtil.distance(entityCoordinates, perimeterCoordinates, perimeter.getRangeUnit());
        } else if (perimeter.getPerimeterType() == PerimeterType.POLYGON) {
            return GeoUtil.contains(perimeter.getPolygonsDefinition(), new Coordinates(latitude, longitude));
        } else {
            throw new TbNodeException("Unsupported perimeter type: " + perimeter.getPerimeterType()  + "!");
        }
    }

    /**
     * 功能：获取`Perimeters`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected List<Perimeter> getPerimeters(TbMsg msg) throws TbNodeException {
        if (config.isFetchPerimeterInfoFromMessageMetadata()) {
            if (StringUtils.isEmpty(config.getPerimeterKeyName())) {
                // Old configuration before "perimeterKeyName" was introduced
                String perimeterValue = msg.getMetaData().getValue("perimeter");
                if (!StringUtils.isEmpty(perimeterValue)) {
                    Perimeter perimeter = new Perimeter();
                    perimeter.setPerimeterType(PerimeterType.POLYGON);
                    perimeter.setPolygonsDefinition(perimeterValue);
                    return Collections.singletonList(perimeter);
                } else if (!StringUtils.isEmpty(msg.getMetaData().getValue("centerLatitude"))) {
                    Perimeter perimeter = new Perimeter();
                    perimeter.setPerimeterType(PerimeterType.CIRCLE);
                    perimeter.setCenterLatitude(Double.parseDouble(msg.getMetaData().getValue("centerLatitude")));
                    perimeter.setCenterLongitude(Double.parseDouble(msg.getMetaData().getValue("centerLongitude")));
                    perimeter.setRange(Double.parseDouble(msg.getMetaData().getValue("range")));
                    perimeter.setRangeUnit(RangeUnit.valueOf(msg.getMetaData().getValue("rangeUnit")));
                    return Collections.singletonList(perimeter);
                } else {
                    throw new TbNodeException("Missing perimeter definition!");
                }
            } else {
                String perimeterValue = msg.getMetaData().getValue(config.getPerimeterKeyName());
                if (!StringUtils.isEmpty(perimeterValue)) {
                    if (config.getPerimeterType().equals(PerimeterType.POLYGON)) {
                        Perimeter perimeter = new Perimeter();
                        perimeter.setPerimeterType(PerimeterType.POLYGON);
                        perimeter.setPolygonsDefinition(perimeterValue);
                        return Collections.singletonList(perimeter);
                    } else {
                        var circleDef = JacksonUtil.toJsonNode(perimeterValue);
                        Perimeter perimeter = new Perimeter();
                        perimeter.setPerimeterType(PerimeterType.CIRCLE);
                        perimeter.setCenterLatitude(circleDef.get("latitude").asDouble());
                        perimeter.setCenterLongitude(circleDef.get("longitude").asDouble());
                        perimeter.setRange(circleDef.get("radius").asDouble());
                        perimeter.setRangeUnit(circleDef.has("radiusUnit") ? RangeUnit.valueOf(circleDef.get("radiusUnit").asText()) : RangeUnit.METER);
                        return Collections.singletonList(perimeter);
                    }
                } else {
                    throw new TbNodeException("Missing perimeter definition!");
                }
            }
        } else {
            Perimeter perimeter = new Perimeter();
            perimeter.setPerimeterType(config.getPerimeterType());
            perimeter.setCenterLatitude(config.getCenterLatitude());
            perimeter.setCenterLongitude(config.getCenterLongitude());
            perimeter.setRange(config.getRange());
            perimeter.setRangeUnit(config.getRangeUnit());
            perimeter.setPolygonsDefinition(config.getPolygonsDefinition());
            return Collections.singletonList(perimeter);
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `msg`：待处理消息。
     * - `msgDataObj`：待处理消息。
     * - `keyName`：名称。
     * 返回：数值结果。
     */
    protected Double getValueFromMessageByName(TbMsg msg, JsonObject msgDataObj, String keyName) throws TbNodeException {
        double value;
        if (msgDataObj.has(keyName) && msgDataObj.get(keyName).isJsonPrimitive()) {
            value = msgDataObj.get(keyName).getAsDouble();
        } else {
            String valueStr = msg.getMetaData().getValue(keyName);
            if (!StringUtils.isEmpty(valueStr)) {
                value = Double.parseDouble(valueStr);
            } else {
                throw new TbNodeException("Incoming Message has no " + keyName + " in data or metadata!");
            }
        }
        return value;
    }

    /*
     * 本类总结：`AbstractGeofencingNode` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
