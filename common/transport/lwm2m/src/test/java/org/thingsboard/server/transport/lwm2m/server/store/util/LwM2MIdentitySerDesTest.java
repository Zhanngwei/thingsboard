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
 * 1. `LwM2MIdentitySerDesTest` 是 ThingsBoard Common Transport 中验证 `LwM2MIdentitySerDes` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
}