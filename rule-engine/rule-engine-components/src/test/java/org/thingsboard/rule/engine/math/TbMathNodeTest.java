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
package org.thingsboard.rule.engine.math;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.Timeout;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbMathNodeTest} 覆盖的 数学计算节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbMathNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class TbMathNodeTest {

    /** 测试常量字段：{@code RULE_DISPATCHER_POOL_SIZE} 保存 {@code int} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    static final int RULE_DISPATCHER_POOL_SIZE = 2;
    /** 测试常量字段：{@code DB_CALLBACK_POOL_SIZE} 保存 {@code int} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    static final int DB_CALLBACK_POOL_SIZE = 3;
    /** 测试常量字段：{@code TIMEOUT} 保存 {@code long} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    static final long TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    /** 固定 fixture 字段：{@code originator} 保存 {@code EntityId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final EntityId originator = DeviceId.fromString("ccd71696-0586-422d-940e-755a41ec3b0d");
    /** 固定 fixture 字段：{@code tenantId} 保存 {@code TenantId} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("e7f46b23-0c7d-42f5-9b06-fc35ab17af8a"));

    /** Mock 依赖字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock(lenient = true)
    private TbContext ctx;
    /** Mock 依赖字段：{@code attributesService} 保存 {@code AttributesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AttributesService attributesService;
    /** Mock 依赖字段：{@code tsService} 保存 {@code TimeseriesService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TimeseriesService tsService;
    /** Mock 依赖字段：{@code telemetryService} 保存 {@code RuleEngineTelemetryService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private RuleEngineTelemetryService telemetryService;
    /** 可变 fixture 字段：{@code dbCallbackExecutor} 保存 {@code AbstractListeningExecutor} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private AbstractListeningExecutor dbCallbackExecutor;
    /** 可变 fixture 字段：{@code ruleEngineDispatcherExecutor} 保存 {@code AbstractListeningExecutor} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private AbstractListeningExecutor ruleEngineDispatcherExecutor;

    /**
     * 生命周期方法：{@code before} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void before() {
        dbCallbackExecutor = new DBCallbackExecutor();
        dbCallbackExecutor.init();
        ruleEngineDispatcherExecutor = new RuleDispatcherExecutor();
        ruleEngineDispatcherExecutor.init();

        willReturn(dbCallbackExecutor).given(ctx).getDbCallbackExecutor();
        willReturn(attributesService).given(ctx).getAttributesService();
        willReturn(telemetryService).given(ctx).getTelemetryService();
        willReturn(tsService).given(ctx).getTimeseriesService();
        willReturn(tenantId).given(ctx).getTenantId();
    }

    /**
     * 生命周期方法：{@code after} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    public void after() {
        ruleEngineDispatcherExecutor.executor().shutdownNow();
        dbCallbackExecutor.executor().shutdownNow();
    }

    /**
     * 辅助方法：{@code initNode} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMathNode initNode(TbRuleNodeMathFunctionType operation, TbMathResult result, TbMathArgument... arguments) {
        return initNode(operation, null, result, arguments);
    }

    /**
     * 辅助方法：{@code initNodeWithCustomFunction} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMathNode initNodeWithCustomFunction(String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(TbRuleNodeMathFunctionType.CUSTOM, expression, result, arguments);
    }

    /**
     * 辅助方法：{@code initNodeWithCustomFunction} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMathNode initNodeWithCustomFunction(TbContext ctx, String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(ctx, TbRuleNodeMathFunctionType.CUSTOM, expression, result, arguments);
    }

    /**
     * 辅助方法：{@code initNode} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMathNode initNode(TbRuleNodeMathFunctionType operation, String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(this.ctx, operation, expression, result, arguments);
    }

    /**
     * 辅助方法：{@code initNode} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMathNode initNode(TbContext ctx, TbRuleNodeMathFunctionType operation, String expression, TbMathResult result, TbMathArgument... arguments) {
        try {
            TbMathNodeConfiguration configuration = new TbMathNodeConfiguration();
            configuration.setOperation(operation);
            if (TbRuleNodeMathFunctionType.CUSTOM.equals(operation)) {
                configuration.setCustomFunction(expression);
            }
            configuration.setResult(result);
            configuration.setArguments(Arrays.asList(arguments));
            TbMathNode node = new TbMathNode();
            node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));
            return node;
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 测试方法：覆盖 {@code testExp4j} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testExp4j() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNodeWithCustomFunction("2a+3b",
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "${key1}", 2, false, false, null),
                new TbMathArgument("a", TbMathArgumentType.MESSAGE_BODY, "${key2}"),
                new TbMathArgument("b", TbMathArgumentType.MESSAGE_BODY, "$[key3]")
        );

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key1", "firstMsgResult");
        metaData.putValue("key2", "argumentA");
        ObjectNode msgNode = JacksonUtil.newObjectNode()
                .put("key3", "argumentB").put("argumentA", 2).put("argumentB", 2);
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, msgNode.toString());

        node.onMsg(ctx, msg);

        metaData.putValue("key1", "secondMsgResult");
        metaData.putValue("key2", "argumentC");
        msgNode = JacksonUtil.newObjectNode()
                .put("key3", "argumentD").put("argumentC", 4).put("argumentD", 3);
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, msgNode.toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT).times(2)).tellSuccess(msgCaptor.capture());

        List<TbMsg> resultMsgs = msgCaptor.getAllValues();
        assertFalse(resultMsgs.isEmpty());
        assertEquals(2, resultMsgs.size());

        for (int i = 0; i < resultMsgs.size(); i++) {
            TbMsg outMsg = resultMsgs.get(i);
            assertNotNull(outMsg);
            assertNotNull(outMsg.getData());
            var resultJson = JacksonUtil.toJsonNode(outMsg.getData());
            String resultKey = i == 0 ? "firstMsgResult" : "secondMsgResult";
            assertTrue(resultJson.has(resultKey));
            assertEquals(i == 0 ? 10 : 17, resultJson.get(resultKey).asInt());
        }
    }

    /** 参数源方法：{@code testSimpleTwoArgumentFunction} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> testSimpleTwoArgumentFunction() {
        return Stream.of(
                Arguments.of(TbRuleNodeMathFunctionType.ADD, 2.1, 2.2, 4.3),
                Arguments.of(TbRuleNodeMathFunctionType.SUB, 2.1, 2.2, -0.1),
                Arguments.of(TbRuleNodeMathFunctionType.MULT, 2.1, 2.0, 4.2),
                Arguments.of(TbRuleNodeMathFunctionType.DIV, 4.2, 2.0, 2.1),
                Arguments.of(TbRuleNodeMathFunctionType.ATAN2, 0.5, 0.3, 1.03),
                Arguments.of(TbRuleNodeMathFunctionType.HYPOT, 4, 5, 6.4),
                Arguments.of(TbRuleNodeMathFunctionType.FLOOR_DIV, 5, 3, 1),
                Arguments.of(TbRuleNodeMathFunctionType.FLOOR_MOD, 6, 3, 0),
                Arguments.of(TbRuleNodeMathFunctionType.MIN, 5, 3, 3),
                Arguments.of(TbRuleNodeMathFunctionType.MAX, 5, 3, 5),
                Arguments.of(TbRuleNodeMathFunctionType.POW, 5, 3, 125)
        );
    }

    /**
     * 测试方法：覆盖 {@code testSimpleTwoArgumentFunction} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @MethodSource
    public void testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType function, double arg1, double arg2, double result) {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(function,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", arg1).put("b", arg2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT).times(1)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        assertTrue(resultJson.has("result"));
        assertEquals(result, resultJson.get("result").asDouble(), 0d);
    }

    /** 参数源方法：{@code testSimpleOneArgumentFunction} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> testSimpleOneArgumentFunction() {
        return Stream.of(
                Arguments.of(TbRuleNodeMathFunctionType.SIN, Math.toRadians(30), 0.5),
                Arguments.of(TbRuleNodeMathFunctionType.SIN, Math.toRadians(90), 1.0),

                Arguments.of(TbRuleNodeMathFunctionType.SINH, Math.toRadians(0), 0.0),
                Arguments.of(TbRuleNodeMathFunctionType.COSH, Math.toRadians(0), 1.0),

                Arguments.of(TbRuleNodeMathFunctionType.COS, Math.toRadians(60), 0.5),
                Arguments.of(TbRuleNodeMathFunctionType.COS, Math.toRadians(0), 1.0),

                Arguments.of(TbRuleNodeMathFunctionType.TAN, Math.toRadians(45), 1),
                Arguments.of(TbRuleNodeMathFunctionType.TAN, Math.toRadians(0), 0),
                Arguments.of(TbRuleNodeMathFunctionType.TANH, 90, 1),

                Arguments.of(TbRuleNodeMathFunctionType.ACOS, 0.5, 1.05),
                Arguments.of(TbRuleNodeMathFunctionType.ASIN, 0.5, 0.52),
                Arguments.of(TbRuleNodeMathFunctionType.ATAN, 0.5, 0.46),

                Arguments.of(TbRuleNodeMathFunctionType.EXP, 1, 2.72),
                Arguments.of(TbRuleNodeMathFunctionType.EXPM1, 1, 1.72),
                Arguments.of(TbRuleNodeMathFunctionType.ABS, -1, 1),
                Arguments.of(TbRuleNodeMathFunctionType.SQRT, 4, 2),
                Arguments.of(TbRuleNodeMathFunctionType.CBRT, 8, 2),

                Arguments.of(TbRuleNodeMathFunctionType.GET_EXP, 4, 2),

                Arguments.of(TbRuleNodeMathFunctionType.LOG, 4, 1.39),
                Arguments.of(TbRuleNodeMathFunctionType.LOG10, 4, 0.6),
                Arguments.of(TbRuleNodeMathFunctionType.LOG1P, 4, 1.61),

                Arguments.of(TbRuleNodeMathFunctionType.CEIL, 1.55, 2),
                Arguments.of(TbRuleNodeMathFunctionType.FLOOR, 23.97, 23),

                Arguments.of(TbRuleNodeMathFunctionType.SIGNUM, 0.55, 1),
                Arguments.of(TbRuleNodeMathFunctionType.RAD, 5, 0.09),
                Arguments.of(TbRuleNodeMathFunctionType.DEG, 5, 286.48)
        );
    }

    /**
     * 测试方法：覆盖 {@code testSimpleOneArgumentFunction} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @MethodSource
    public void testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType function, double arg1, double result) {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(function,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", arg1).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT).times(1)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        assertTrue(resultJson.has("result"));
        assertEquals(result, resultJson.get("result").asDouble(), 0d);
    }

    /**
     * 测试方法：覆盖 {@code test_2_plus_2_body} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_2_plus_2_body() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.ADD,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        assertTrue(resultJson.has("result"));
        assertEquals(4, resultJson.get("result").asInt());
    }

    /**
     * 测试方法：覆盖 {@code test_2_plus_2_meta} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_2_plus_2_meta() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.ADD,
                new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 0, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        assertNotNull(resultMsg.getMetaData());
        var result = resultMsg.getMetaData().getValue("result");
        assertNotNull(result);
        assertEquals("4", result);
    }

    /**
     * 测试方法：覆盖 {@code test_2_plus_2_attr_and_ts} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_2_plus_2_attr_and_ts() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.ADD,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.ATTRIBUTE, "a"),
                new TbMathArgument(TbMathArgumentType.TIME_SERIES, "b")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().toString());

        when(attributesService.find(tenantId, originator, DataConstants.SERVER_SCOPE, "a"))
                .thenReturn(Futures.immediateFuture(Optional.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("a", 2.0)))));

        when(tsService.findLatest(tenantId, originator, "b"))
                .thenReturn(Futures.immediateFuture(Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("b", 2L)))));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        assertTrue(resultJson.has("result"));
        assertEquals(4, resultJson.get("result").asInt());
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_body} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_body() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 3, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 5).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        assertTrue(resultJson.has("result"));
        assertEquals(2.236, resultJson.get("result").asDouble(), 0.0);
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_meta} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_meta() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 3, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 5).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        assertNotNull(result);
        assertEquals("2.236", result);
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_to_attribute_and_metadata} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_to_attribute_and_metadata() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.ATTRIBUTE, "result", 3, false, true, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 5).toString());

        when(telemetryService.saveAttrAndNotify(any(), any(), anyString(), anyString(), anyDouble()))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());
        verify(telemetryService, times(1)).saveAttrAndNotify(any(), any(), anyString(), anyString(), anyDouble());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        assertNotNull(result);
        assertEquals("2.236", result);
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_to_timeseries_and_data} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_to_timeseries_and_data() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 5).toString());
        when(telemetryService.saveAndNotify(any(), any(), any(TsKvEntry.class)))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());
        verify(telemetryService, times(1)).saveAndNotify(any(), any(), any(TsKvEntry.class));

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        assertTrue(resultJson.has("result"));
        assertEquals(2.236, resultJson.get("result").asDouble(), 0.0);
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_to_timeseries_and_metadata_and_data} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_to_timeseries_and_metadata_and_data() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, true, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 5).toString());
        when(telemetryService.saveAndNotify(any(), any(), any(TsKvEntry.class)))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());
        verify(telemetryService, times(1)).saveAndNotify(any(), any(), any(TsKvEntry.class));

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var resultMetadata = resultMsg.getMetaData().getValue("result");
        var resultData = JacksonUtil.toJsonNode(resultMsg.getData());

        assertTrue(resultData.has("result"));
        assertEquals(2.236, resultData.get("result").asDouble(), 0.0);

        assertNotNull(resultMetadata);
        assertEquals("2.236", resultMetadata);
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_default_value} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_default_value() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        tbMathArgument.setDefaultValue(5.0);
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 3, false, false, null),
                tbMathArgument
        );
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 10).toString());

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, timeout(TIMEOUT)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        assertNotNull(result);
        assertEquals("2.236", result);
    }

    /**
     * 测试方法：覆盖 {@code test_sqrt_5_default_value_failure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void test_sqrt_5_default_value_failure() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey")
        );
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 10).toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> tCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(eq(msg), tCaptor.capture());
        Assert.assertNotNull(tCaptor.getValue().getMessage());
    }

    /**
     * 测试方法：覆盖 {@code testConvertMsgBodyIfRequiredFailure} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testConvertMsgBodyIfRequiredFailure() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY,  TbMsg.EMPTY_JSON_ARRAY);
        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> tCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(eq(msg), tCaptor.capture());
        Assert.assertNotNull(tCaptor.getValue().getMessage());
    }

    /**
     * 测试方法：覆盖 {@code testExp4j_concurrent} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testExp4j_concurrent() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbMathNode node = spy(initNodeWithCustomFunction("2a+3b",
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        ));
        EntityId originatorSlow = DeviceId.fromString("7f01170d-6bba-419c-b95c-2b4c3ba32f30");
        EntityId originatorFast = DeviceId.fromString("c45360ff-7906-4102-a2ae-3495a86168d0");
        CountDownLatch slowProcessingLatch = new CountDownLatch(1);

        List<TbMsg> slowMsgList = IntStream.range(0, 5)
                .mapToObj(x -> TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originatorSlow, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString()))
                .collect(Collectors.toList());
        List<TbMsg> fastMsgList = IntStream.range(0, 2)
                .mapToObj(x -> TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originatorFast, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString()))
                .collect(Collectors.toList());

        assertThat(slowMsgList.size()).as("slow msgs >= rule-dispatcher pool size").isGreaterThanOrEqualTo(RULE_DISPATCHER_POOL_SIZE);

        log.debug("rule-dispatcher [{}], db-callback [{}], slowMsg [{}], fastMsg [{}]", RULE_DISPATCHER_POOL_SIZE, DB_CALLBACK_POOL_SIZE, slowMsgList.size(), fastMsgList.size());

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("\uD83D\uDC0C processMsgAsync slow originator [{}][{}]", msg.getOriginator(), msg);
            try {
                assertThat(slowProcessingLatch.await(30, TimeUnit.SECONDS)).as("await on slowProcessingLatch").isTrue();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(eq(ctx), argThat(slowMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("\u26A1\uFE0F processMsgAsync FAST originator [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(eq(ctx), argThat(fastMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("submit slow originator onMsg [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).onMsg(eq(ctx), argThat(slowMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("submit FAST originator onMsg [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).onMsg(eq(ctx), argThat(fastMsgList::contains));

        // submit slow msg may block all rule engine dispatcher threads
        slowMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));
        // wait until dispatcher threads started with all slowMsg
        verify(node, timeout(TIMEOUT).times(slowMsgList.size())).onMsg(eq(ctx), argThat(slowMsgList::contains));

        // submit fast have to return immediately
        fastMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));
        // wait until all fast messages processed
        verify(ctx, timeout(TIMEOUT).times(fastMsgList.size())).tellSuccess(any());

        slowProcessingLatch.countDown();

        verify(ctx, timeout(TIMEOUT).times(fastMsgList.size() + slowMsgList.size())).tellSuccess(any());

        verify(ctx, never()).tellFailure(any(), any());
    }

    /**
     * 测试方法：覆盖 {@code testExp4j_concurrentBySingleOriginator_processMsgAsyncException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testExp4j_concurrentBySingleOriginator_processMsgAsyncException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbMathNode node = spy(initNodeWithCustomFunction("2a+3b",
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        ));

        willThrow(new RuntimeException("Message body has no 'delta'")).given(node).resolveArguments(any(), any(), any(), any());

        EntityId originatorSlow = DeviceId.fromString("7f01170d-6bba-419c-b95c-2b4c3ba32f30");
        CountDownLatch slowProcessingLatch = new CountDownLatch(1);

        List<TbMsg> slowMsgList = IntStream.range(0, 5)
                .mapToObj(x -> TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originatorSlow, TbMsgMetaData.EMPTY, JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString()))
                .collect(Collectors.toList());

        assertThat(slowMsgList.size()).as("slow msgs >= rule-dispatcher pool size").isGreaterThanOrEqualTo(RULE_DISPATCHER_POOL_SIZE);

        log.debug("rule-dispatcher [{}], db-callback [{}], slowMsg [{}]", RULE_DISPATCHER_POOL_SIZE, DB_CALLBACK_POOL_SIZE, slowMsgList.size());

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            if (slowProcessingLatch.getCount() > 0) {
                log.debug("Await on slowProcessingLatch before processMsgAsync");
                try {
                    assertThat(slowProcessingLatch.await(30, TimeUnit.SECONDS)).as("await on slowProcessingLatch").isTrue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.debug("\uD83D\uDC0C processMsgAsync with exception [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(eq(ctx), argThat(slowMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("submit slow originator onMsg [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).onMsg(eq(ctx), argThat(slowMsgList::contains));

        // submit slow msg may block all rule engine dispatcher threads
        slowMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));
        // wait until dispatcher threads started with all slowMsg
        verify(node, new Timeout(TimeUnit.SECONDS.toMillis(5), times(slowMsgList.size()))).onMsg(eq(ctx), argThat(slowMsgList::contains));

        slowProcessingLatch.countDown();

        verify(ctx, new Timeout(TimeUnit.SECONDS.toMillis(5), times(slowMsgList.size()))).tellFailure(any(), any());
        verify(ctx, never()).tellSuccess(any());

    }

    /**
     * 测试方法：覆盖 {@code testExp4j_concurrentBySingleOriginator_SingleMsg_manyNodesWithDifferentOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void testExp4j_concurrentBySingleOriginator_SingleMsg_manyNodesWithDifferentOutput() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        assertThat(RULE_DISPATCHER_POOL_SIZE).as("dispatcher pool size have to be > 1").isGreaterThan(1);
        CountDownLatch processingLatch = new CountDownLatch(1);
        List<Triple<TbContext, String, TbMathNode>> ctxNodes = IntStream.range(0, RULE_DISPATCHER_POOL_SIZE * 2)
                .mapToObj(x -> {
                    final TbContext ctx = mock(TbContext.class); // many rule nodes - many contexts
                    willReturn(dbCallbackExecutor).given(ctx).getDbCallbackExecutor();
                    final String resultKey = "result" + x;
                    final TbMathNode node = spy(initNodeWithCustomFunction(ctx, "2a+3b",
                            new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, resultKey, 1, false, true, null),
                            new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                            new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")));
                    willAnswer(invocation -> {
                        if (processingLatch.getCount() > 0) {
                            log.debug("Await on processingLatch before processMsgAsync");
                            try {
                                assertThat(processingLatch.await(30, TimeUnit.SECONDS)).as("await on processingLatch").isTrue();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        log.debug("\uD83D\uDC0C processMsgAsync on node with expected resultKey [{}]", resultKey);
                        return invocation.callRealMethod();
                    }).given(node).processMsgAsync(any(), any());
                    willAnswer(invocation -> {
                        TbMsg msg = invocation.getArgument(1);
                        log.debug("submit originator onMsg [{}][{}]", msg.getOriginator(), msg);
                        return invocation.callRealMethod();
                    }).given(node).onMsg(any(), any());
                    return Triple.of(ctx, resultKey, node);
                })
                .collect(Collectors.toList());
        ctxNodes.forEach(ctxNode -> ruleEngineDispatcherExecutor.executeAsync(() -> ctxNode.getRight()
                .onMsg(ctxNode.getLeft(), TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, "{\"a\":2,\"b\":2}"))));
        ctxNodes.forEach(ctxNode -> verify(ctxNode.getRight(), timeout(5000)).onMsg(eq(ctxNode.getLeft()), any()));
        processingLatch.countDown();

        SoftAssertions softly = new SoftAssertions();
        ctxNodes.forEach(ctxNode -> {
            final TbContext ctx = ctxNode.getLeft();
            final String resultKey = ctxNode.getMiddle();
            ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            verify(ctx, timeout(5000)).tellSuccess(msgCaptor.capture());

            TbMsg resultMsg = msgCaptor.getValue();
            assertThat(resultMsg).as("result msg non null for result key " + resultKey).isNotNull();
            log.debug("asserting result key [{}] in metadata [{}]", resultKey, resultMsg.getMetaData().getData());
            softly.assertThat(resultMsg.getMetaData().getValue(resultKey)).as("asserting result key " + resultKey)
                    .isEqualTo("10.0");
        });

        softly.assertAll();
        verify(ctx, never()).tellFailure(any(), any());
    }

    /**
     * 测试目标：验证 {@code RuleDispatcherExecutor} 覆盖的 数学计算节点 行为，重点说明配置、消息和断言路径。
     * 所属生产节点/组件：{@code RuleDispatcherExecutor}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
     * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
     * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
     * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
     */
    static class RuleDispatcherExecutor extends AbstractListeningExecutor {
        /** 实现方法：{@code getThreadPollSize} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
        @Override
        protected int getThreadPollSize() {
            return RULE_DISPATCHER_POOL_SIZE;
        }
    }

    /**
     * 测试目标：验证 {@code DBCallbackExecutor} 覆盖的 数学计算节点 行为，重点说明配置、消息和断言路径。
     * 所属生产节点/组件：{@code DBCallbackExecutor}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
     * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
     * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
     * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
     */
    static class DBCallbackExecutor extends AbstractListeningExecutor {
        /** 实现方法：{@code getThreadPollSize} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
        @Override
        protected int getThreadPollSize() {
            return DB_CALLBACK_POOL_SIZE;
        }
    }

}
/*
 * 本类总结：{@code TbMathNodeTest} 为 {@code TbMathNode} 的 数学计算节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
