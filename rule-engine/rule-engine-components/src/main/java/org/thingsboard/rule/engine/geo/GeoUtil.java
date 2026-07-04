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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.NonNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：`GeoUtil` 是地理工具辅助类，用于执行 GPS 地理围栏、距离和多边形判断及状态跟踪。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class GeoUtil {

    /**
     * 常量字段：定义 `distCtx`，用于规则引擎上下文，本身不触发外部系统调用。
     */
    private static final SpatialContext distCtx = SpatialContext.GEO;
    /**
     * 常量字段：定义 `jtsCtx`，用于规则引擎上下文，本身不触发外部系统调用。
     */
    private static final JtsSpatialContext jtsCtx;

    static {
        JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
        factory.normWrapLongitude = true;
        jtsCtx = factory.newSpatialContext();
    }

    /**
     * 方法说明：执行 `distance` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static synchronized double distance(Coordinates x, Coordinates y, RangeUnit unit) {
        Point xLL = distCtx.getShapeFactory().pointXY(x.getLongitude(), x.getLatitude());
        Point yLL = distCtx.getShapeFactory().pointXY(y.getLongitude(), y.getLatitude());
        return unit.fromKm(distCtx.getDistCalc().distance(xLL, yLL) * DistanceUtils.DEG_TO_KM);
    }

    /**
     * 方法说明：执行 `contains` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static synchronized boolean contains(@NonNull String polygonInString, @NonNull Coordinates coordinates) {
        if (polygonInString.isEmpty() || polygonInString.isBlank()) {
            throw new RuntimeException("Polygon string can't be empty or null!");
        }

        JsonArray polygonsJson = normalizePolygonsJson(JsonParser.parseString(polygonInString).getAsJsonArray());
        List<Geometry> polygons = buildPolygonsFromJson(polygonsJson);
        Set<Geometry> holes = extractHolesFrom(polygons);
        polygons.removeIf(holes::contains);

        Geometry globalGeometry = unionToGlobalGeometry(polygons, holes);
        var point = jtsCtx.getShapeFactory().getGeometryFactory()
                .createPoint(new Coordinate(coordinates.getLatitude(), coordinates.getLongitude()));

        return globalGeometry.contains(point);
    }

    /**
     * 方法说明：执行 `unionToGlobalGeometry` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static Geometry unionToGlobalGeometry(List<Geometry> polygons, Set<Geometry> holes) {
        Geometry globalPolygon = polygons.stream().reduce(Geometry::union).orElseThrow(() ->
                new RuntimeException("Error while calculating globalPolygon - the result of all polygons union is null"));
        Optional<Geometry> globalHole = holes.stream().reduce(Geometry::union);
        if (globalHole.isEmpty()) {
            return globalPolygon;
        } else {
            return globalPolygon.difference(globalHole.get());
        }
    }

    /**
     * 方法说明：执行 `normalizePolygonsJson` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static JsonArray normalizePolygonsJson(JsonArray polygonsJsonArray) {
        JsonArray result = new JsonArray();
        normalizePolygonsJson(polygonsJsonArray, result);
        return result;
    }

    /**
     * 方法说明：执行 `normalizePolygonsJson` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static void normalizePolygonsJson(JsonArray polygonsJsonArray, JsonArray result) {
        if (containsArrayWithPrimitives(polygonsJsonArray)) {
            result.add(polygonsJsonArray);
        } else {
            for (JsonElement element : polygonsJsonArray) {
                if (containsArrayWithPrimitives(element.getAsJsonArray())) {
                    result.add(element);
                } else {
                    normalizePolygonsJson(element.getAsJsonArray(), result);
                }
            }
        }
    }

    /**
     * 方法说明：执行 `extractHolesFrom` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static Set<Geometry> extractHolesFrom(List<Geometry> polygons) {
        Map<Geometry, List<Geometry>> polygonsHoles = new HashMap<>();

        for (Geometry polygon : polygons) {
            List<Geometry> holes = polygons.stream()
                    .filter(another -> !another.equalsExact(polygon))
                    .filter(another -> {
                        JtsGeometry currentGeo = jtsCtx.getShapeFactory().makeShape(polygon);
                        JtsGeometry anotherGeo = jtsCtx.getShapeFactory().makeShape(another);

                        boolean currentContainsAnother = currentGeo
                                .relate(anotherGeo)
                                .equals(SpatialRelation.CONTAINS);

                        boolean anotherWithinCurrent = anotherGeo
                                .relate(currentGeo)
                                .equals(SpatialRelation.WITHIN);

                        return currentContainsAnother && anotherWithinCurrent;
                    })
                    .collect(Collectors.toList());

            if (!holes.isEmpty()) {
                polygonsHoles.put(polygon, holes);
            }
        }

        return polygonsHoles.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * 方法说明：构造告警详情、SSL 上下文或输出对象，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static List<Geometry> buildPolygonsFromJson(JsonArray polygonsJsonArray) {
        List<Geometry> polygons = new LinkedList<>();

        for (JsonElement polygonJsonArray : polygonsJsonArray) {
            polygons.add(
                    buildPolygonFromCoordinates(parseCoordinates(polygonJsonArray.getAsJsonArray()))
            );
        }

        return polygons;
    }

    /**
     * 方法说明：构造告警详情、SSL 上下文或输出对象，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static Geometry buildPolygonFromCoordinates(List<Coordinate> coordinates) {
        if (coordinates.size() == 2) {
            Coordinate a = coordinates.get(0);
            Coordinate c = coordinates.get(1);
            coordinates.clear();

            Coordinate b = new Coordinate(a.x, c.y);
            Coordinate d = new Coordinate(c.x, a.y);
            coordinates.addAll(List.of(a, b, c, d, a));
        }

        CoordinateSequence coordinateSequence = jtsCtx
                .getShapeFactory()
                .getGeometryFactory()
                .getCoordinateSequenceFactory()
                .create(coordinates.toArray(new Coordinate[0]));

        return GeometryFixer.fix(jtsCtx.getShapeFactory().getGeometryFactory().createPolygon(coordinateSequence));
    }

    /**
     * 方法说明：执行 `parseCoordinates` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static List<Coordinate> parseCoordinates(JsonArray coordinatesJson) {
        List<Coordinate> result = new LinkedList<>();

        for (JsonElement coords : coordinatesJson) {
            double x = coords.getAsJsonArray().get(0).getAsDouble();
            double y = coords.getAsJsonArray().get(1).getAsDouble();
            result.add(new Coordinate(x, y));
        }

        if (result.size() >= 3) {
            result.add(result.get(0));
        }

        return result;
    }

    /**
     * 方法说明：执行 `containsPrimitives` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static boolean containsPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            return element.isJsonPrimitive();
        }

        return false;
    }

    /**
     * 方法说明：执行 `containsArrayWithPrimitives` 对应的辅助逻辑，供 `GeoUtil` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static boolean containsArrayWithPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            if (!containsPrimitives(element.getAsJsonArray())) {
                return false;
            }
        }

        return true;
    }

    /*
     * 本类总结：`GeoUtil` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
