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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;

import java.security.PublicKey;

/**
 * 中文说明：
 * 1. `LwM2MIdentitySerDes` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class LwM2MIdentitySerDes {

    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String KEY_ID = "id";
    private static final String KEY_CN = "cn";
    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String KEY_RPK = "rpk";
    protected static final String KEY_LWM2MIDENTITY_TYPE = "type";
    /**
     * 类型常量，用于统一引用固定值。
     */
    protected static final String LWM2MIDENTITY_TYPE_UNSECURE = "unsecure";
    protected static final String LWM2MIDENTITY_TYPE_PSK = "psk";
    /**
     * 类型常量，用于统一引用固定值。
     */
    protected static final String LWM2MIDENTITY_TYPE_X509 = "x509";
    protected static final String LWM2MIDENTITY_TYPE_RPK = "rpk";

    /**
     * 功能：执行 `serialize` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    public static JsonObject serialize(Identity identity) {
        JsonObject o = Json.object();

        if (identity.isPSK()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_PSK);
            o.set(KEY_ID, identity.getPskIdentity());
        } else if (identity.isRPK()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_RPK);
            PublicKey publicKey = identity.getRawPublicKey();
            o.set(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
        } else if (identity.isX509()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_X509);
            o.set(KEY_CN, identity.getX509CommonName());
        } else {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_UNSECURE);
            o.set(KEY_ADDRESS, identity.getPeerAddress().getHostString());
            o.set(KEY_PORT, identity.getPeerAddress().getPort());
        }
        return o;
    }

    /**
     * 功能：执行 `deserialize` 对应的处理。
     * 参数：
     * - `peer`：`peer` 参数。
     * 返回：处理结果。
     */
    public static Identity deserialize(JsonObject peer) {
        throw new NotImplementedException();
    }
}