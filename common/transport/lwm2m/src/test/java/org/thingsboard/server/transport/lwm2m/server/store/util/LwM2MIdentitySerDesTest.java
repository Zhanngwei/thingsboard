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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import com.eclipsesource.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.leshan.core.request.Identity;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MIdentitySerDesTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
class LwM2MIdentitySerDesTest {

    /**
     * 功能：执行 `serializePskIdentity` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void serializePskIdentity() {
        assertThat(LwM2MIdentitySerDes.serialize(Identity.psk(getTestAddress(), "my:psk")).toString())
                .isEqualTo("{\"type\":\"psk\",\"id\":\"my:psk\"}");
    }


    /**
     * 功能：执行 `serializeRpkIdentity` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void serializeRpkIdentity() {
        var public_key = mock(PublicKey.class);
        when(public_key.getEncoded()).thenReturn(new byte[]{1,2,3,4,5,6,7,8,9});

        assertThat(LwM2MIdentitySerDes.serialize(Identity.rpk(getTestAddress(), public_key)).toString())
                .isEqualTo("{\"type\":\"rpk\",\"rpk\":\"010203040506070809\"}");
    }

    /**
     * 功能：执行 `serializeX509Identity` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void serializeX509Identity() {
        assertThat(LwM2MIdentitySerDes.serialize(Identity.x509(getTestAddress(), "MyCommonName")).toString())
                .isEqualTo("{\"type\":\"x509\",\"cn\":\"MyCommonName\"}");
    }

    /**
     * 功能：执行 `serializeUnsecureIdentity` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void serializeUnsecureIdentity() {
        assertThat(LwM2MIdentitySerDes.serialize(Identity.unsecure(getTestAddress())).toString())
                .isEqualTo("{\"type\":\"unsecure\",\"address\":\"1.2.3.4\",\"port\":5684}");
    }
    

    /**
     * 功能：执行 `deserialize` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void deserialize() {
        assertThatThrownBy(() -> LwM2MIdentitySerDes.deserialize(mock(JsonObject.class)))
                .isInstanceOf(NotImplementedException.class);
    }

    /**
     * 功能：获取`Test Address`。
     * 参数：无。
     * 返回：处理结果。
     */
    private static InetSocketAddress getTestAddress() {
        try {
            return new InetSocketAddress(InetAddress.getByName("1.2.3.4"), 5684);
        } catch (UnknownHostException e) {
            throw new AssertionError("Cannot create test address");
        }
    }

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MIdentitySerDesTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}