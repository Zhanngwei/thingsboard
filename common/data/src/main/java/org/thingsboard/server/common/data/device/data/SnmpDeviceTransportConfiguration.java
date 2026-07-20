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
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;

/**
 * 中文说明：
 * 1. `SnmpDeviceTransportConfiguration` 是 ThingsBoard Common Data 中描述设备行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `DeviceTransportConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@ToString(of = {"host", "port", "protocolVersion"})
public class SnmpDeviceTransportConfiguration implements DeviceTransportConfiguration {
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String host;
    private Integer port;
    /**
     * 版本号，表示当前对象的对应属性。
     */
    private SnmpProtocolVersion protocolVersion;

    /*
     * For SNMP v1 and v2c
     * */
    /**
     * `community` 字段，保存当前对象的对应属性。
     */
    private String community;

    /*
     * For SNMP v3
     * */
    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;
    private String securityName;
    /**
     * 名称，用于标识或展示当前对象。
     */
    private String contextName;
    private AuthenticationProtocol authenticationProtocol;
    /**
     * `authenticationPassphrase` 字段，保存当前对象的对应属性。
     */
    private String authenticationPassphrase;
    private PrivacyProtocol privacyProtocol;
    /**
     * `privacyPassphrase` 字段，保存当前对象的对应属性。
     */
    private String privacyPassphrase;
    private String engineId;

    /**
     * 功能：创建 `SnmpDeviceTransportConfiguration` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public SnmpDeviceTransportConfiguration() {
        this.host = "localhost";
        this.port = 161;
        this.protocolVersion = SnmpProtocolVersion.V2C;
        this.community = "public";
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("Transport configuration is not valid");
        }
    }

    /**
     * 功能：判断`Valid`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    private boolean isValid() {
        boolean isValid = StringUtils.isNotBlank(host) && port != null && protocolVersion != null;
        if (isValid) {
            switch (protocolVersion) {
                case V1:
                case V2C:
                    isValid = StringUtils.isNotEmpty(community);
                    break;
                case V3:
                    isValid = StringUtils.isNotBlank(username) && StringUtils.isNotBlank(securityName)
                            && contextName != null && authenticationProtocol != null
                            && StringUtils.isNotBlank(authenticationPassphrase)
                            && privacyProtocol != null && StringUtils.isNotBlank(privacyPassphrase) && engineId != null;
                    break;
            }
        }
        return isValid;
    }
}
