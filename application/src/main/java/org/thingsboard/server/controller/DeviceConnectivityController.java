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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PROTOCOL;
import static org.thingsboard.server.controller.ControllerConstants.PROTOCOL_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.CA_ROOT_CERT_PEM;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.DOCKER_COMPOSE_YML;

/**
 * 中文说明：
 * 1. `DeviceConnectivityController` 是 ThingsBoard Application 中处理设备请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceConnectivityController extends BaseController {

    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final DeviceConnectivityService deviceConnectivityService;
    private final SystemSecurityService systemSecurityService;

    /**
     * 功能：获取设备。
     * 参数：
     * - `strDeviceId`：设备IDID。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get commands to publish device telemetry (getDevicePublishTelemetryCommands)",
            notes = "Fetch the list of commands to publish device telemetry based on device profile " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the device is assigned to the same customer. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK",
                    examples = @io.swagger.annotations.Example(
                            value = {
                                    @io.swagger.annotations.ExampleProperty(
                                            mediaType = "application/json",
                                            value = "{\"http\":\"curl -v -X POST http://localhost:8080/api/v1/0ySs4FTOn5WU15XLmal8/telemetry --header Content-Type:application/json --data {temperature:25}\"," +
                                                    "\"mqtt\":\"mosquitto_pub -d -q 1 -h localhost -t v1/devices/me/telemetry -i myClient1 -u myUsername1 -P myPassword -m {temperature:25}\"," +
                                                    "\"coap\":\"coap-client -m POST coap://localhost:5683/api/v1/0ySs4FTOn5WU15XLmal8/telemetry -t json -e {temperature:25}\"}")}))})
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device-connectivity/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getDevicePublishTelemetryCommands(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                                      @PathVariable(DEVICE_ID) String strDeviceId, HttpServletRequest request) throws ThingsboardException, URISyntaxException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);

        String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
        return deviceConnectivityService.findDevicePublishTelemetryCommands(baseUrl, device);
    }

    /**
     * 功能：执行 `downloadServerCertificate` 对应的处理。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download server certificate using file path defined in device.connectivity properties (downloadServerCertificate)", notes = "Download server certificate.")
    @RequestMapping(value = "/device-connectivity/{protocol}/certificate/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadServerCertificate(@ApiParam(value = PROTOCOL_PARAM_DESCRIPTION)
                                                                                          @PathVariable(PROTOCOL) String protocol) throws ThingsboardException, IOException {
        checkParameter(PROTOCOL, protocol);
        var pemCert =
                checkNotNull(deviceConnectivityService.getPemCertFile(protocol), protocol + " pem cert file is not found!");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + CA_ROOT_CERT_PEM)
                .header("x-filename", CA_ROOT_CERT_PEM)
                .contentLength(pemCert.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pemCert);
    }

    /**
     * 功能：执行 `downloadGatewayDockerCompose` 对应的处理。
     * 参数：
     * - `strDeviceId`：设备IDID。
     * - `request`：请求对象。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download generated docker-compose.yml file for gateway (downloadGatewayDockerCompose)", notes = "Download generated docker-compose.yml for gateway.")
    @RequestMapping(value = "/device-connectivity/gateway-launch/{deviceId}/docker-compose/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadGatewayDockerCompose(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                                                                             @PathVariable(DEVICE_ID) String strDeviceId, HttpServletRequest request) throws ThingsboardException, URISyntaxException, IOException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);

        if (!checkIsGateway(device)) {
            throw new ThingsboardException("The device must be a gateway!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
        var dockerCompose = checkNotNull(deviceConnectivityService.createGatewayDockerComposeFile(baseUrl, device), "Failed to create docker-compose.yml file!");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + DOCKER_COMPOSE_YML)
                .header("x-filename", DOCKER_COMPOSE_YML)
                .contentLength(dockerCompose.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(dockerCompose);
    }

    /**
     * 功能：校验`Is Gateway`。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：判断结果。
     */
    private static boolean checkIsGateway(Device device) {
        return device.getAdditionalInfo().has(DataConstants.GATEWAY_PARAMETER) &&
                device.getAdditionalInfo().get(DataConstants.GATEWAY_PARAMETER).asBoolean();
    }
}
