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
 * 中文说明：
 * 1. `GeoUtil` 是 ThingsBoard Rule Engine Components 中处理 `Geo Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class GeoUtil {

    /**
     * 上下文常量，用于统一引用固定值。
     */
    private static final SpatialContext distCtx = SpatialContext.GEO;
    /**
     * 上下文常量，用于统一引用固定值。
     */
    private static final JtsSpatialContext jtsCtx;

    static {
        JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
        factory.normWrapLongitude = true;
        jtsCtx = factory.newSpatialContext();
    }

    /**
     * 功能：执行 `distance` 对应的处理。
     * 参数：
     * - `x`：`x` 参数。
     * - `y`：`y` 参数。
     * - `unit`：`unit` 参数。
     * 返回：数值结果。
     */
    public static synchronized double distance(Coordinates x, Coordinates y, RangeUnit unit) {
        Point xLL = distCtx.getShapeFactory().pointXY(x.getLongitude(), x.getLatitude());
        Point yLL = distCtx.getShapeFactory().pointXY(y.getLongitude(), y.getLatitude());
        return unit.fromKm(distCtx.getDistCalc().distance(xLL, yLL) * DistanceUtils.DEG_TO_KM);
    }

    /**
     * 功能：执行 `contains` 对应的处理。
     * 参数：
     * - `polygonInString`：`polygonInString` 参数。
     * - `coordinates`：`coordinates` 参数。
     * 返回：判断结果。
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
     * 功能：执行 `unionToGlobalGeometry` 对应的处理。
     * 参数：
     * - `polygons`：数据列表。
     * - `holes`：`holes` 参数。
     * 返回：处理结果。
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
     * 功能：执行 `normalizePolygonsJson` 对应的处理。
     * 参数：
     * - `polygonsJsonArray`：`polygonsJsonArray` 参数。
     * 返回：处理结果。
     */
    private static JsonArray normalizePolygonsJson(JsonArray polygonsJsonArray) {
        JsonArray result = new JsonArray();
        normalizePolygonsJson(polygonsJsonArray, result);
        return result;
    }

    /**
     * 功能：执行 `normalizePolygonsJson` 对应的处理。
     * 参数：
     * - `polygonsJsonArray`：`polygonsJsonArray` 参数。
     * - `result`：`result` 参数。
     * 返回：无。
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
     * 功能：执行 `extractHolesFrom` 对应的处理。
     * 参数：
     * - `polygons`：数据列表。
     * 返回：匹配的数据集合。
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
     * 功能：构建JSON。
     * 参数：
     * - `polygonsJsonArray`：`polygonsJsonArray` 参数。
     * 返回：匹配的数据集合。
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
     * 功能：构建`Polygon From Coordinates`。
     * 参数：
     * - `coordinates`：数据列表。
     * 返回：处理结果。
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
     * 功能：解析`Coordinates`。
     * 参数：
     * - `coordinatesJson`：`coordinatesJson` 参数。
     * 返回：匹配的数据集合。
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
     * 功能：执行 `containsPrimitives` 对应的处理。
     * 参数：
     * - `array`：`array` 参数。
     * 返回：判断结果。
     */
    private static boolean containsPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            return element.isJsonPrimitive();
        }

        return false;
    }

    /**
     * 功能：执行 `containsArrayWithPrimitives` 对应的处理。
     * 参数：
     * - `array`：`array` 参数。
     * 返回：判断结果。
     */
    private static boolean containsArrayWithPrimitives(JsonArray array) {
        for (JsonElement element : array) {
            if (!containsPrimitives(element.getAsJsonArray())) {
                return false;
            }
        }

        return true;
    }
}
