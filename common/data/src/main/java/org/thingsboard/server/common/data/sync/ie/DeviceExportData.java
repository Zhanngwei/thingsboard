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
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;

/**
 * 中文说明：
 * 1. `DeviceExportData` 是 ThingsBoard Common Data 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `EntityExportData`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class DeviceExportData extends EntityExportData<Device> {

    /**
     * 凭据，用于认证或安全校验。
     */
    @JsonProperty(index = 3)
    @JsonIgnoreProperties({"id", "deviceId", "createdTime"})
    private DeviceCredentials credentials;

    /**
     * 功能：判断凭据。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    @Override
    public boolean hasCredentials() {
        return credentials != null;
    }
}
