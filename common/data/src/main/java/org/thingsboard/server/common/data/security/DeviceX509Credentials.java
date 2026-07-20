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
package org.thingsboard.server.common.data.security;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. `DeviceX509Credentials` 是 ThingsBoard Common Data 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DeviceCredentialsFilter`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class DeviceX509Credentials implements DeviceCredentialsFilter {

    /**
     * `sha3Hash` 字段，保存当前对象的对应属性。
     */
    private final String sha3Hash;

    /**
     * 功能：创建 `DeviceX509Credentials` 实例，并初始化必要字段。
     * 参数：
     * - `sha3Hash`：`sha3Hash` 参数。
     * 返回：新创建的对象实例。
     */
    public DeviceX509Credentials(String sha3Hash) {
        this.sha3Hash = sha3Hash;
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getCredentialsId() { return sha3Hash; }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceCredentialsType getCredentialsType() { return DeviceCredentialsType.X509_CERTIFICATE; }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "DeviceX509Credentials [SHA3=" + sha3Hash + "]";
    }
}
