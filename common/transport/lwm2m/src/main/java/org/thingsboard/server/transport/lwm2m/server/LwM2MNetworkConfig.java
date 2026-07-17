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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.eclipse.californium.core.config.CoapConfig.DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS;

/**
 * 中文说明：
 * 1. `LwM2MNetworkConfig` 是 ThingsBoard Common Transport 中描述 LwM2M 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
public class LwM2MNetworkConfig {

    /**
     * 功能：获取配置。
     * 参数：
     * - `serverPortNoSec`：`serverPortNoSec` 参数。
     * - `serverSecurePort`：`serverSecurePort` 参数。
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    public static Configuration getCoapConfig(Integer serverPortNoSec, Integer serverSecurePort, LwM2MTransportServerConfig config) {
        Configuration coapConfig = new Configuration();
        coapConfig.set(CoapConfig.COAP_PORT, serverPortNoSec);
        coapConfig.set(CoapConfig.COAP_SECURE_PORT, serverSecurePort);
        /**
         Property to indicate if the response should always include the Block2 option \
         when client request early blockwise negociation but the response can be sent on one packet.
         - value of false indicate that the server will respond without block2 option if no further blocks are required.
         - value of true indicate that the server will response with block2 option event if no further blocks are required.
         CoAP client will try to use block mode
         or adapt the block size when receiving a 4.13 Entity too large response code
         */
        coapConfig.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, true);
        /**
         Property to indicate if the response should always include the Block2 option \
         when client request early blockwise negociation but the response can be sent on one packet.
         - value of false indicate that the server will respond without block2 option if no further blocks are required.
         - value of true indicate that the server will response with block2 option event if no further blocks are required.
         */
        coapConfig.set(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);
        /**
         * The maximum amount of time (in milliseconds) allowed between
         * transfers of individual blocks in a blockwise transfer before the
         * blockwise transfer state is discarded.
         * <p>
         * The default value of this property is
         * {@link NetworkConfigDefaults#DEFAULT_BLOCKWISE_STATUS_LIFETIME} = 5 * 60 * 1000; // 5 mins [ms].
         */
        coapConfig.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS, TimeUnit.SECONDS);
        /**
         !!! REQUEST_ENTITY_TOO_LARGE CODE=4.13
         The maximum size of a resource body (in bytes) that will be accepted
         as the payload of a POST/PUT or the response to a GET request in a
         transparent> blockwise transfer.
         This option serves as a safeguard against excessive memory
         consumption when many resources contain large bodies that cannot be
         transferred in a single CoAP message. This option has no impact on
         *manually* managed blockwise transfers in which the blocks are handled individually.
         Note that this option does not prevent local clients or resource
         implementations from sending large bodies as part of a request or response to a peer.
         The default value of this property is DEFAULT_MAX_RESOURCE_BODY_SIZE = 8192
         A value of {@code 0} turns off transparent handling of blockwise transfers altogether.
         */
        coapConfig.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 256 * 1024 * 1024);
        /**
         The default DTLS response matcher.
         Supported values are STRICT, RELAXED, or PRINCIPAL.
         The default value is STRICT.
         Create new instance of udp endpoint context matcher.
         Params:
         checkAddress
         – true with address check, (STRICT, UDP) - if port Registration of client is changed - it is bad
         - false, without
         */
        coapConfig.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.MatcherMode.RELAXED);
        /**
         https://tools.ietf.org/html/rfc7959#section-2.9.3
         The block size (number of bytes) to use when doing a blockwise transfer. \
         This value serves as the upper limit for block size in blockwise transfers
         */
        coapConfig.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
        /**
         The maximum payload size (in bytes) that can be transferred in a
         single message, i.e. without requiring a blockwise transfer.
         NB: this value MUST be adapted to the maximum message size supported by the transport layer.
         In particular, this value cannot exceed the network's MTU if UDP is used as the transport protocol
         DEFAULT_VALUE = 1024
         */
        coapConfig.set(CoapConfig.MAX_MESSAGE_SIZE, 1024);

        coapConfig.set(CoapConfig.MAX_RETRANSMIT, 10);

        if (!CollectionUtils.isEmpty(config.getNetworkConfig())) {
            Properties networkProps = new Properties();
            config.getNetworkConfig().forEach(p -> networkProps.put(p.getKey(), p.getValue()));
            coapConfig.add(networkProps);
        }

        return coapConfig;
    }
}