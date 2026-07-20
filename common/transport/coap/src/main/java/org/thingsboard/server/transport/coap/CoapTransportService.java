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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapServerComponent;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.UnknownHostException;

/**
 * 中文说明：
 * 1. `CoapTransportService` 是 ThingsBoard Common Transport 中负责 CoAP 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbTransportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("CoapTransportService")
@TbCoapServerComponent
@Slf4j
public class CoapTransportService implements TbTransportService {

    /**
     * `V1`常量，用于统一引用固定值。
     */
    private static final String V1 = "v1";
    private static final String API = "api";
    /**
     * `EFENTO`常量，用于统一引用固定值。
     */
    private static final String EFENTO = "efento";
    public static final String MEASUREMENTS = "m";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_INFO = "i";
    public static final String CONFIGURATION = "c";
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    public static final String CURRENT_TIMESTAMP = "t";

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private CoapServerService coapServerService;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private CoapTransportContext coapTransportContext;

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    private CoapServer coapServer;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        coapServer = coapServerService.getCoapServer();
        CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, coapServerService, V1));

        CoapEfentoTransportResource efento = new CoapEfentoTransportResource(coapTransportContext, EFENTO);
        efento.add(new CoapResource(MEASUREMENTS));
        efento.add(new CoapResource(DEVICE_INFO));
        efento.add(new CoapResource(CONFIGURATION));
        efento.add(new CoapResource(CURRENT_TIMESTAMP));
        coapServer.add(api);
        coapServer.add(efento);
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.FIRMWARE));
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.SOFTWARE));
        log.info("CoAP transport started!");
    }

    /**
     * 功能：执行 `shutdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdown() {
        log.info("CoAP transport stopped!");
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return DataConstants.COAP_TRANSPORT_NAME;
    }
}
