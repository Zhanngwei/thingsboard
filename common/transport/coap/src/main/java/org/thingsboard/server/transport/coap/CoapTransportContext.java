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
package org.thingsboard.server.transport.coap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.adaptors.JsonCoapAdaptor;
import org.thingsboard.server.transport.coap.adaptors.ProtoCoapAdaptor;
import org.thingsboard.server.transport.coap.client.CoapClientContext;
import org.thingsboard.server.transport.coap.efento.adaptor.EfentoCoapAdaptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by ashvayka on 18.10.18.
 */
/**
 * 中文说明：
 * 1. `CoapTransportContext` 是 ThingsBoard Common Transport 中承载 CoAP 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TransportContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.coap.enabled}'=='true')")
@Component
@Getter
public class CoapTransportContext extends TransportContext {

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    /**
     * JSON，表示当前对象的对应属性。
     */
    @Autowired
    private JsonCoapAdaptor jsonCoapAdaptor;

    /**
     * Protobuf，表示当前对象的对应属性。
     */
    @Autowired
    private ProtoCoapAdaptor protoCoapAdaptor;

    /**
     * `efentoCoapAdaptor` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private EfentoCoapAdaptor efentoCoapAdaptor;

    /**
     * 上下文，用于发起外部调用或协议交互。
     */
    @Autowired
    private CoapClientContext clientContext;

    private final ConcurrentMap<Integer, TransportProtos.ToDeviceRpcRequestMsg> rpcAwaitingAck = new ConcurrentHashMap<>();

}
