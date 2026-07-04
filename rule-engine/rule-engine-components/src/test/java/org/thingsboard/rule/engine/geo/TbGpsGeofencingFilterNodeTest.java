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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 测试目标：验证 {@code TbGpsGeofencingFilterNodeTest} 覆盖的 地理围栏组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGpsGeofencingFilterNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
class TbGpsGeofencingFilterNodeTest {

    /** 测试常量字段：{@code CIRCLE_RANGE} 保存 {@code double} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final double CIRCLE_RANGE = 1.0;
    /** 测试常量字段：{@code CIRCLE_CENTER} 保存 {@code Coordinates} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final Coordinates CIRCLE_CENTER = new Coordinates(49.0384, 31.4513);
    /** 测试常量字段：{@code POINT_INSIDE_CIRCLE} 保存 {@code Coordinates} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final Coordinates POINT_INSIDE_CIRCLE = new Coordinates(49.0354, 31.4513); // distance from center: 0.334 km
    /** 测试常量字段：{@code POINT_OUTSIDE_CIRCLE} 保存 {@code Coordinates} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final Coordinates POINT_OUTSIDE_CIRCLE = new Coordinates(49.0284, 31.4513); // distance from center: 1.112 km

    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbContext ctx;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbGpsGeofencingFilterNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGpsGeofencingFilterNode node;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        node = new TbGpsGeofencingFilterNode();
    }

    /**
     * 生命周期方法：{@code tearDown} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    // Exception tests

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenOnMsg_thenExceptionInvalidMsg} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenOnMsg_thenExceptionInvalidMsg() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getEmptyArrayTbMsg(deviceId);

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.onMsg(ctx, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Incoming Message is not a valid JSON object!");
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenOnMsg_thenExceptionMissingPerimeterDefinitionNewVersion} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenOnMsg_thenExceptionMissingPerimeterDefinitionNewVersion() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.onMsg(ctx, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Missing perimeter definition!");
    }

    /**
     * 测试方法：覆盖 {@code givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenExceptionMissingPerimeterDefinitionOldVersion} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenExceptionMissingPerimeterDefinitionOldVersion() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.onMsg(ctx, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Missing perimeter definition!");
    }

    // Polygon tests

    /**
     * 测试方法：覆盖 {@code givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenTrue() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenFalse} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenFalse() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(), GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenOnMsg_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenOnMsg_thenTrue() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenOnMsg_thenFalse} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenOnMsg_thenFalse() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(), GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypePolygonAndConfigWithPolygonDefined_whenOnMsg_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypePolygonAndConfigWithPolygonDefined_whenOnMsg_thenTrue() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPolygonsDefinition(GeoUtilTest.SIMPLE_RECT);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypePolygonAndConfigWithPolygonDefined_whenOnMsg_thenFalse} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypePolygonAndConfigWithPolygonDefined_whenOnMsg_thenFalse() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPolygonsDefinition(GeoUtilTest.SIMPLE_RECT);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(), GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 辅助方法：{@code getMetadataForOldVersionPolygonPerimeter} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsgMetaData getMetadataForOldVersionPolygonPerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("perimeter", GeoUtilTest.SIMPLE_RECT);
        return metadata;
    }

    /**
     * 辅助方法：{@code getMetadataForNewVersionPolygonPerimeter} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsgMetaData getMetadataForNewVersionPolygonPerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("ss_perimeter", GeoUtilTest.SIMPLE_RECT);
        return metadata;
    }

    // Circle tests

    /**
     * 测试方法：覆盖 {@code givenTypeCircleAndConfigWithoutPerimeterKeyName_whenOnMsg_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypeCircleAndConfigWithoutPerimeterKeyName_whenOnMsg_thenTrue() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_INSIDE_CIRCLE.getLatitude(), POINT_INSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypeCircleAndConfigWithoutPerimeterKeyName_whenOnMsg_thenFalse} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypeCircleAndConfigWithoutPerimeterKeyName_whenOnMsg_thenFalse() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_OUTSIDE_CIRCLE.getLatitude(), POINT_OUTSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypeCircle_whenOnMsg_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypeCircle_whenOnMsg_thenTrue() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterType(PerimeterType.CIRCLE);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_INSIDE_CIRCLE.getLatitude(), POINT_INSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypeCircle_whenOnMsg_thenFalse} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypeCircle_whenOnMsg_thenFalse() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterType(PerimeterType.CIRCLE);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_OUTSIDE_CIRCLE.getLatitude(), POINT_OUTSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypeCircleAndConfigWithCircleDefined_whenOnMsg_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypeCircleAndConfigWithCircleDefined_whenOnMsg_thenTrue() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPerimeterType(PerimeterType.CIRCLE);
        config.setCenterLatitude(CIRCLE_CENTER.getLatitude());
        config.setCenterLongitude(CIRCLE_CENTER.getLongitude());
        config.setRange(CIRCLE_RANGE);
        config.setRangeUnit(RangeUnit.KILOMETER);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                POINT_INSIDE_CIRCLE.getLatitude(), POINT_INSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 测试方法：覆盖 {@code givenTypeCircleAndConfigWithCircleDefined_whenOnMsg_thenFalse} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenTypeCircleAndConfigWithCircleDefined_whenOnMsg_thenFalse() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPerimeterType(PerimeterType.CIRCLE);
        config.setCenterLatitude(CIRCLE_CENTER.getLatitude());
        config.setCenterLongitude(CIRCLE_CENTER.getLongitude());
        config.setRange(CIRCLE_RANGE);
        config.setRangeUnit(RangeUnit.KILOMETER);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                POINT_OUTSIDE_CIRCLE.getLatitude(), POINT_OUTSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    /**
     * 辅助方法：{@code getMetadataForOldVersionCirclePerimeter} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsgMetaData getMetadataForOldVersionCirclePerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("centerLatitude", String.valueOf(CIRCLE_CENTER.getLatitude()));
        metadata.putValue("centerLongitude", String.valueOf(CIRCLE_CENTER.getLongitude()));
        metadata.putValue("range", String.valueOf(CIRCLE_RANGE));
        metadata.putValue("rangeUnit", String.valueOf(RangeUnit.KILOMETER));
        return metadata;
    }

    /**
     * 辅助方法：{@code getMetadataForNewVersionCirclePerimeter} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsgMetaData getMetadataForNewVersionCirclePerimeter() {
        ObjectNode perimeter = JacksonUtil.newObjectNode();
        perimeter.put("latitude", CIRCLE_CENTER.getLatitude());
        perimeter.put("longitude", CIRCLE_CENTER.getLongitude());
        perimeter.put("radius", CIRCLE_RANGE);
        perimeter.put("radiusUnit", String.valueOf(RangeUnit.KILOMETER));
        var metadata = new TbMsgMetaData();
        metadata.putValue("ss_perimeter", JacksonUtil.toString(perimeter));
        return metadata;
    }

    /**
     * 辅助方法：{@code getTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getTbMsg(EntityId entityId, TbMsgMetaData metadata, double latitude, double longitude) {
        String data = "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, metadata, data);
    }

    /**
     * 辅助方法：{@code getEmptyArrayTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getEmptyArrayTbMsg(EntityId entityId) {
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);
    }

}
/*
 * 本类总结：{@code TbGpsGeofencingFilterNodeTest} 为 {@code TbGpsGeofencingFilterNode} 的 地理围栏组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
