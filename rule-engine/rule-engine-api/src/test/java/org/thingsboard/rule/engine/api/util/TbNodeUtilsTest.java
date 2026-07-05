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
package org.thingsboard.rule.engine.api.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 中文说明：
 * 1. 职责：验证 TbNodeUtils 对元数据变量和消息 data 变量模板的替换行为。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的工具类单元测试。
 * 3. 协作对象：与 TbNodeUtils、TbMsg、TbMsgMetaData、JacksonUtil 和 JUnit/Mockito 测试框架协作。
 * 4. 生命周期：由 Maven/JUnit 在测试阶段创建并执行，每个测试方法独立构造输入消息。
 * 5. 设计原因：模板替换被多个规则节点复用，单元测试可固定边界行为，尤其是嵌套对象和数组不替换场景。
 * 6. 技术关联：测试本身不涉及事务、缓存、MQTT、Actor、数据库；直接验证 Rule Engine 模板处理工具。
 */
@RunWith(MockitoJUnitRunner.class)
public class TbNodeUtilsTest {

    /**
     * 数据常量，用于统一引用固定值。
     */
    private static final String DATA_VARIABLE_TEMPLATE = "$[%s]";
    /**
     * `METADATA_VARIABLE_TEMPLATE`常量，用于统一引用固定值。
     */
    private static final String METADATA_VARIABLE_TEMPLATE = "${%s}";

    /**
     * 功能：验证`Simple Replacement`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSimpleReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("metadata_key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("data_key", "data_value");

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value data_value", result);
    }

    /**
     * 功能：验证`No Replacement`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testNoReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals(pattern, result);
    }

    /**
     * 功能：验证`Same Keys Replacement`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSameKeysReplacement() {
        String pattern = "ABC ${key} $[key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value data_value", result);
    }

    /**
     * 功能：验证`Complex Object Replacement`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testComplexObjectReplacement() {
        String pattern = "ABC ${key} $[key1.key2.key3]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode key2Node = JacksonUtil.newObjectNode();
        key2Node.put("key3", "value3");

        ObjectNode key1Node = JacksonUtil.newObjectNode();
        key1Node.set("key2", key2Node);


        ObjectNode node = JacksonUtil.newObjectNode();
        node.set("key1", key1Node);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value value3", result);
    }

    /**
     * 功能：验证`Array Replacement Does Not Work`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testArrayReplacementDoesNotWork() {
        String pattern = "ABC ${key} $[key1.key2[0].key3]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode key2Node = JacksonUtil.newObjectNode();
        key2Node.put("key3", "value3");

        ObjectNode key1Node = JacksonUtil.newObjectNode();
        key1Node.set("key2", key2Node);


        ObjectNode node = JacksonUtil.newObjectNode();
        node.set("key1", key1Node);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value $[key1.key2[0].key3]", result);
    }

    /**
     * 功能：验证 `givenKey_whenFormatDataVarTemplate_thenReturnTheSameStringAsFormat` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenKey_whenFormatDataVarTemplate_thenReturnTheSameStringAsFormat() {
        assertThat(TbNodeUtils.formatDataVarTemplate("key"), is("$[key]"));
        assertThat(TbNodeUtils.formatDataVarTemplate("key"), is(String.format(DATA_VARIABLE_TEMPLATE, "key")));

        assertThat(TbNodeUtils.formatDataVarTemplate(""), is("$[]"));
        assertThat(TbNodeUtils.formatDataVarTemplate(""), is(String.format(DATA_VARIABLE_TEMPLATE, "")));

        assertThat(TbNodeUtils.formatDataVarTemplate(null), is("$[null]"));
        assertThat(TbNodeUtils.formatDataVarTemplate(null), is(String.format(DATA_VARIABLE_TEMPLATE, (String) null)));
    }

    /**
     * 功能：验证 `givenKey_whenFormatMetadataVarTemplate_thenReturnTheSameStringAsFormat` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenKey_whenFormatMetadataVarTemplate_thenReturnTheSameStringAsFormat() {
        assertThat(TbNodeUtils.formatMetadataVarTemplate("key"), is("${key}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate("key"), is(String.format(METADATA_VARIABLE_TEMPLATE, "key")));

        assertThat(TbNodeUtils.formatMetadataVarTemplate(""), is("${}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate(""), is(String.format(METADATA_VARIABLE_TEMPLATE, "")));

        assertThat(TbNodeUtils.formatMetadataVarTemplate(null), is("${null}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate(null), is(String.format(METADATA_VARIABLE_TEMPLATE, (String) null)));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：验证 TbNodeUtils 模板替换和模板格式化行为。
 * 2. 核心流程：每个测试构造 TbMsg 或 key，调用工具方法并断言替换结果。
 * 3. 关键依赖：TbNodeUtils、TbMsg、TbMsgMetaData、JacksonUtil、JUnit 和 Hamcrest。
 * 4. 学习重点：模板工具只支持 metadata 直接替换和 data 对象点路径，数组表达式会保持原样。
 */
