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
package org.thingsboard.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `SnmpAuthService` 是 ThingsBoard Common Transport 中负责 SNMP 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbSnmpTransportComponent
@RequiredArgsConstructor
public class SnmpAuthService {
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final SnmpTransportService snmpTransportService;

    /**
     * `snmpUnderlyingProtocol` 字段，保存当前对象的对应属性。
     */
    @Value("${transport.snmp.underlying_protocol}")
    private String snmpUnderlyingProtocol;

    /**
     * 功能：更新目标对象。
     * 参数：
     * - `profileTransportConfig`：配置对象。
     * - `deviceTransportConfig`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Target setUpSnmpTarget(SnmpDeviceProfileTransportConfiguration profileTransportConfig, SnmpDeviceTransportConfiguration deviceTransportConfig) {
        AbstractTarget target;

        SnmpProtocolVersion protocolVersion = deviceTransportConfig.getProtocolVersion();
        switch (protocolVersion) {
            case V1:
                CommunityTarget communityTargetV1 = new CommunityTarget();
                communityTargetV1.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv1);
                communityTargetV1.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                communityTargetV1.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
                target = communityTargetV1;
                break;
            case V2C:
                CommunityTarget communityTargetV2 = new CommunityTarget();
                communityTargetV2.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
                communityTargetV2.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                communityTargetV2.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
                target = communityTargetV2;
                break;
            case V3:
                OctetString username = new OctetString(deviceTransportConfig.getUsername());
                OctetString securityName = new OctetString(deviceTransportConfig.getSecurityName());
                OctetString engineId = new OctetString(deviceTransportConfig.getEngineId());

                OID authenticationProtocol = new OID(deviceTransportConfig.getAuthenticationProtocol().getOid());
                OID privacyProtocol = new OID(deviceTransportConfig.getPrivacyProtocol().getOid());
                OctetString authenticationPassphrase = new OctetString(deviceTransportConfig.getAuthenticationPassphrase());
                authenticationPassphrase = new OctetString(SecurityProtocols.getInstance().passwordToKey(authenticationProtocol, authenticationPassphrase, engineId.getValue()));
                OctetString privacyPassphrase = new OctetString(deviceTransportConfig.getPrivacyPassphrase());
                privacyPassphrase = new OctetString(SecurityProtocols.getInstance().passwordToKey(privacyProtocol, authenticationProtocol, privacyPassphrase, engineId.getValue()));

                USM usm = snmpTransportService.getSnmp().getUSM();
                if (usm.hasUser(engineId, securityName)) {
                    usm.removeAllUsers(username, engineId);
                }
                usm.addLocalizedUser(
                        engineId.getValue(), username,
                        authenticationProtocol, authenticationPassphrase.getValue(),
                        privacyProtocol, privacyPassphrase.getValue()
                );

                UserTarget userTarget = new UserTarget();
                userTarget.setSecurityName(securityName);
                userTarget.setAuthoritativeEngineID(engineId.getValue());
                userTarget.setSecurityModel(SecurityModel.SECURITY_MODEL_USM);
                userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
                target = userTarget;
                break;
            default:
                throw new UnsupportedOperationException("SNMP protocol version " + protocolVersion + " is not supported");
        }

        Address address = GenericAddress.parse(snmpUnderlyingProtocol + ":" + deviceTransportConfig.getHost() + "/" + deviceTransportConfig.getPort());
        target.setAddress(Optional.ofNullable(address).orElseThrow(() -> new IllegalArgumentException("Address of the SNMP device is invalid")));
        target.setTimeout(profileTransportConfig.getTimeoutMs());
        target.setRetries(profileTransportConfig.getRetries());
        target.setVersion(protocolVersion.getCode());

        return target;
    }

    /**
     * 功能：删除或清理信息对象。
     * 参数：
     * - `sessionContext`：处理上下文。
     * 返回：无。
     */
    public void cleanUpSnmpAuthInfo(DeviceSessionContext sessionContext) {
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        if (deviceTransportConfiguration.getProtocolVersion() == SnmpProtocolVersion.V3) {
            OctetString username = new OctetString(deviceTransportConfiguration.getUsername());
            OctetString engineId = new OctetString(deviceTransportConfiguration.getEngineId());
            snmpTransportService.getSnmp().getUSM().removeAllUsers(username, engineId);
        }
    }

}
