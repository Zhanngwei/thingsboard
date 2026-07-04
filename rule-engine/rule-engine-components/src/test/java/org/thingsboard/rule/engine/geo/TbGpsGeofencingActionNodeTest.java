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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.ENTERED;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.INSIDE;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.LEFT;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.OUTSIDE;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

/**
 * 测试目标：验证 {@code TbGpsGeofencingActionNodeTest} 覆盖的 地理围栏组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGpsGeofencingActionNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
class TbGpsGeofencingActionNodeTest extends AbstractRuleNodeUpgradeTest {

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctx;
    /** Mock 依赖字段：{@code attributesService} 保存 {@code AttributesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AttributesService attributesService;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbGpsGeofencingActionNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGpsGeofencingActionNode node;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() {
        node = spy(new TbGpsGeofencingActionNode());
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

    /** 参数源方法：{@code givenReportPresenceStatusOnEachMessage_whenOnMsg_thenVerifyOutputMsgType} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenReportPresenceStatusOnEachMessage_whenOnMsg_thenVerifyOutputMsgType() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long tsNow = System.currentTimeMillis();
        long tsNowMinusMinuteAndMillis = tsNow - Duration.ofMinutes(1).plusMillis(1).toMillis();
        return Stream.of(
                // default config with presenceMonitoringStrategyOnEachMessage false and msgInside true
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false,
                        new EntityGeofencingState(false, 0, false)), ENTERED),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false,
                        new EntityGeofencingState(true, tsNow, false)), SUCCESS),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false,
                        new EntityGeofencingState(true, tsNowMinusMinuteAndMillis, false)), INSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false,
                        new EntityGeofencingState(true, tsNow, true)), SUCCESS),
                // default config with presenceMonitoringStrategyOnEachMessage false and msgInside false
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false,
                        new EntityGeofencingState(false, 0, false)), LEFT),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false,
                        new EntityGeofencingState(false, tsNow, false)), SUCCESS),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false,
                        new EntityGeofencingState(false, tsNowMinusMinuteAndMillis, false)), OUTSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false,
                        new EntityGeofencingState(false, tsNow, true)), SUCCESS),
                // default config with presenceMonitoringStrategyOnEachMessage true and msgInside true
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, true,
                        new EntityGeofencingState(false, 0, false)), ENTERED),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, true,
                        new EntityGeofencingState(true, tsNow, false)), INSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, true,
                        new EntityGeofencingState(true, tsNowMinusMinuteAndMillis, false)), INSIDE),
                // default config with presenceMonitoringStrategyOnEachMessage true and msgInside false
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, true,
                        new EntityGeofencingState(false, 0, false)), LEFT),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, true,
                        new EntityGeofencingState(false, tsNow, false)), OUTSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, true,
                        new EntityGeofencingState(false, tsNowMinusMinuteAndMillis, false)), OUTSIDE)
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenReportPresenceStatusOnEachMessage_whenOnMsg_thenVerifyOutputMsgType(
            GpsGeofencingActionTestCase gpsGeofencingActionTestCase,
            String expectedOutput
    ) throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingActionNodeConfiguration().defaultConfiguration();
        config.setReportPresenceStatusOnEachMessage(gpsGeofencingActionTestCase.isReportPresenceStatusOnEachMessage());

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = gpsGeofencingActionTestCase.isMsgInside() ?
                getInsideRectangleTbMsg(gpsGeofencingActionTestCase.getEntityId()) :
                getOutsideRectangleTbMsg(gpsGeofencingActionTestCase.getEntityId());

        when(ctx.getAttributesService()).thenReturn(attributesService);

        ReflectionTestUtils.setField(node, "entityStates", gpsGeofencingActionTestCase.getEntityStates());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        verify(ctx.getAttributesService(), never()).find(any(), any(), any(), anyString());
        verify(ctx, never()).tellFailure(any(), any(Throwable.class));
        verify(ctx, never()).enqueueForTellNext(any(), eq(expectedOutput), any(), any());
        verify(ctx, never()).ack(any());

        if (SUCCESS.equals(expectedOutput)) {
            verify(ctx).tellSuccess(eq(msg));
        } else {
            verify(ctx).tellNext(eq(msg), eq(expectedOutput));
        }
    }

    /**
     * 辅助方法：{@code getOutsideRectangleTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getOutsideRectangleTbMsg(EntityId entityId) {
        return getTbMsg(entityId, getMetadataForNewVersionPolygonPerimeter(),
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(),
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());
    }

    /**
     * 辅助方法：{@code getInsideRectangleTbMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getInsideRectangleTbMsg(EntityId entityId) {
        return getTbMsg(entityId, getMetadataForNewVersionPolygonPerimeter(),
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(),
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());
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
     * 辅助方法：{@code getMetadataForNewVersionPolygonPerimeter} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsgMetaData getMetadataForNewVersionPolygonPerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("ss_perimeter", GeoUtilTest.SIMPLE_RECT);
        return metadata;
    }

    // Rule nodes upgrade
    /** 参数源方法：{@code givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n",
                        true,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"reportPresenceStatusOnEachMessage\": false,\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"reportPresenceStatusOnEachMessage\": false,\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n",
                        false,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"reportPresenceStatusOnEachMessage\": false,\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n")
        );
    }

    /** 实现方法：{@code getTestNode} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
/*
 * 本类总结：{@code TbGpsGeofencingActionNodeTest} 为 {@code TbGpsGeofencingActionNode} 的 地理围栏组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
