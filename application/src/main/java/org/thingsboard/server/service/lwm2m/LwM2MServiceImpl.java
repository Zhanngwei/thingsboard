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
package org.thingsboard.server.service.lwm2m;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `LwM2MServiceImpl` 是 ThingsBoard Application 中负责 LwM2M 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `LwM2MService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && '${transport.lwm2m.enabled:false}'=='true'")
public class LwM2MServiceImpl implements LwM2MService {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private final LwM2MTransportServerConfig serverConfig;
    private final Optional<LwM2MTransportBootstrapConfig> bootstrapConfig;

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `bootstrapServer`：`bootstrapServer` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer) {
        LwM2MSecureServerConfig bsServerConfig = bootstrapServer ? bootstrapConfig.orElse(null) : serverConfig;
        if (bsServerConfig!= null) {
            LwM2MServerSecurityConfigDefault result = getServerSecurityConfig(bsServerConfig);
            result.setBootstrapServerIs(bootstrapServer);
            return result;
        }
        else {
            return  null;
        }
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `bsServerConfig`：配置对象。
     * 返回：处理结果。
     */
    private LwM2MServerSecurityConfigDefault getServerSecurityConfig(LwM2MSecureServerConfig bsServerConfig) {
        LwM2MServerSecurityConfigDefault bsServ = new LwM2MServerSecurityConfigDefault();
        bsServ.setShortServerId(bsServerConfig.getId());
        bsServ.setHost(bsServerConfig.getHost());
        bsServ.setPort(bsServerConfig.getPort());
        bsServ.setSecurityHost(bsServerConfig.getSecureHost());
        bsServ.setSecurityPort(bsServerConfig.getSecurePort());
        byte[] publicKeyBase64 = getPublicKey(bsServerConfig);
        if (publicKeyBase64 == null) {
            bsServ.setServerPublicKey("");
        } else {
            bsServ.setServerPublicKey(Base64.encodeBase64String(publicKeyBase64));
        }
        byte[] certificateBase64 = getCertificate(bsServerConfig);
        if (certificateBase64 == null) {
            bsServ.setServerCertificate("");
        } else {
            bsServ.setServerCertificate(Base64.encodeBase64String(certificateBase64));
        }
        return bsServ;
    }

    /**
     * 功能：获取公钥。
     * 参数：
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    private byte[] getPublicKey(LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getPublicKey().getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch public key from key store!", e);
        }
        return null;
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    private byte[] getCertificate(LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getCertificateChain()[0].getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch certificate from key store!", e);
        }
        return null;
    }
}
