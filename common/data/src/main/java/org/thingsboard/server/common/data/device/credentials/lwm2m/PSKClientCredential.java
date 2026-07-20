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
package org.thingsboard.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * 中文说明：
 * 1. `PSKClientCredential` 是 ThingsBoard Common Data 中承载 `PSK Client Credential` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AbstractLwM2MClientSecurityCredential`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Getter
@Setter
public class PSKClientCredential extends AbstractLwM2MClientSecurityCredential {
    /**
     * `identity` 字段，保存当前对象的对应属性。
     */
    private String identity;

    /**
     * 功能：获取安全模式。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.PSK;
    }

    /**
     * 功能：获取`Decoded`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public byte[] getDecoded() throws IllegalArgumentException, DecoderException {
        if (securityInBytes == null) {
                securityInBytes = Hex.decodeHex(key.toLowerCase().toCharArray());
        }
        return securityInBytes;
    }
}