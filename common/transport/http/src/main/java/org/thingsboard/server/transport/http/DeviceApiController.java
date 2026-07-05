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
package org.thingsboard.server.transport.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`DeviceApiController` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@RestController
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.http.enabled}'=='true')")
@RequestMapping("/api/v1")
@Slf4j
public class DeviceApiController implements TbTransportService {

    /**
     * 编码常量，用于统一引用固定值。
     */
    private static final String MARKDOWN_CODE_BLOCK_START = "\n\n```json\n";
    private static final String MARKDOWN_CODE_BLOCK_END = "\n```\n\n";

    private static final String REQUIRE_ACCESS_TOKEN = "The API call is designed to be used by device firmware and requires device access token ('deviceToken'). " +
            "It is not recommended to use this API call by third-party scripts, rule-engine or platform widgets (use 'Telemetry Controller' instead).\n";

    private static final String ATTRIBUTE_PAYLOAD_EXAMPLE = "{\n" +
            " \"stringKey\":\"value1\", \n" +
            " \"booleanKey\":true, \n" +
            " \"doubleKey\":42.0, \n" +
            " \"longKey\":73, \n" +
            " \"jsonKey\": {\n" +
            "    \"someNumber\": 42,\n" +
            "    \"someArray\": [1,2,3],\n" +
            "    \"someNestedObject\": {\"key\": \"value\"}\n" +
            " }\n" +
            "}";

    protected static final String TS_PAYLOAD = "The request payload is a JSON document with three possible formats:\n\n" +
            "Simple format without timestamp. In such a case, current server time will be used: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            " \"stringKey\":\"value1\", \n" +
            " \"booleanKey\":true, \n" +
            " \"doubleKey\":42.0, \n" +
            " \"longKey\":73, \n" +
            " \"jsonKey\": {\n" +
            "    \"someNumber\": 42,\n" +
            "    \"someArray\": [1,2,3],\n" +
            "    \"someNestedObject\": {\"key\": \"value\"}\n" +
            " }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n Single JSON object with timestamp: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n JSON array with timestamps: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "[\n{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}, \n{\"ts\":1634712588000,\"values\":{\"temperature\":25, \"humidity\":88}}\n]" +
            MARKDOWN_CODE_BLOCK_END;

    /**
     * 描述信息常量，用于统一引用固定值。
     */
    private static final String ACCESS_TOKEN_PARAM_DESCRIPTION = "Your device access token.";

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private HttpTransportContext transportContext;

    /**
     * 功能：获取设备。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `scope`：`scope` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Get attributes (getDeviceAttributes)",
            notes = "Returns all attributes that belong to device. "
                    + "Use optional 'clientKeys' and/or 'sharedKeys' parameter to return specific attributes. "
                    + "\n Example of the result: "
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_PAYLOAD_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> getDeviceAttributes(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "Comma separated key names for attribute with client scope", required = true, defaultValue = "state")
            @RequestParam(value = "clientKeys", required = false, defaultValue = "") String clientKeys,
            @ApiParam(value = "Comma separated key names for attribute with shared scope", required = true, defaultValue = "configuration")
            @RequestParam(value = "sharedKeys", required = false, defaultValue = "") String sharedKeys) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    GetAttributeRequestMsg.Builder request = GetAttributeRequestMsg.newBuilder().setRequestId(0);
                    List<String> clientKeySet = !StringUtils.isEmpty(clientKeys) ? Arrays.asList(clientKeys.split(",")) : null;
                    List<String> sharedKeySet = !StringUtils.isEmpty(sharedKeys) ? Arrays.asList(sharedKeys.split(",")) : null;
                    if (clientKeySet != null) {
                        request.addAllClientAttributeNames(clientKeySet);
                    }
                    if (sharedKeySet != null) {
                        request.addAllSharedAttributeNames(sharedKeySet);
                    }
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            transportContext.getDefaultTimeout());
                    transportService.process(sessionInfo, request.build(), new SessionCloseOnErrorCallback(transportService, sessionInfo));
                }));
        return responseWriter;
    }

    /**
     * 功能：执行 `postDeviceAttributes` 对应的处理。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `json`：`json` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Post attributes (postDeviceAttributes)",
            notes = "Post client attribute updates on behalf of device. "
                    + "\n Example of the request: "
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_PAYLOAD_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postDeviceAttributes(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "JSON with attribute key-value pairs. See API call description for example.")
            @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, JsonConverter.convertToAttributesProto(new JsonParser().parse(json)),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    /**
     * 功能：执行 `postTelemetry` 对应的处理。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `json`：`json` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Post time-series data (postTelemetry)",
            notes = "Post time-series data on behalf of device. "
                    + "\n Example of the request: "
                    + TS_PAYLOAD
                    + REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/telemetry", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postTelemetry(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @RequestBody String json, HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, JsonConverter.convertToTelemetryProto(new JsonParser().parse(json)),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    /**
     * 功能：执行 `claimDevice` 对应的处理。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `json`：`json` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Save claiming information (claimDevice)",
            notes = "Saves the information required for user to claim the device. " +
                    "See more info about claiming in the corresponding 'Claiming devices' platform documentation."
                    + "\n Example of the request payload: "
                    + MARKDOWN_CODE_BLOCK_START
                    + "{\"secretKey\":\"value\", \"durationMs\":60000}"
                    + MARKDOWN_CODE_BLOCK_END
                    + "Note: both 'secretKey' and 'durationMs' is optional parameters. " +
                    "In case the secretKey is not specified, the empty string as a default value is used. In case the durationMs is not specified, the system parameter device.claim.duration is used.\n\n"
                    + REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/claim", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> claimDevice(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @RequestBody(required = false) String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
                    transportService.process(sessionInfo, JsonConverter.convertToClaimDeviceProto(deviceId, json),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    /**
     * 功能：订阅`To Commands`。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `seconds`：`seconds` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Subscribe to RPC commands (subscribeToCommands) (Deprecated)",
            notes = "Subscribes to RPC commands using http long polling. " +
                    "Deprecated, since long polling is resource and network consuming. " +
                    "Consider using MQTT or CoAP protocol for light-weight real-time updates. \n\n" +
                    REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToCommands(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "Optional timeout of the long poll. Typically less then 60 seconds, since limited on the server side.")
            @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            timeout == 0 ? transportContext.getDefaultTimeout() : timeout);
                    transportService.process(sessionInfo, SubscribeToRPCMsg.getDefaultInstance(),
                            new SessionCloseOnErrorCallback(transportService, sessionInfo));

                }));
        return responseWriter;
    }

    /**
     * 功能：执行 `replyToCommand` 对应的处理。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `request`：请求对象。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Reply to RPC commands (replyToCommand)",
            notes = "Replies to server originated RPC command identified by 'requestId' parameter. The response is arbitrary JSON.\n\n" +
                    REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/rpc/{requestId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> replyToCommand(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "RPC request id from the incoming RPC request", required = true, defaultValue = "123")
            @PathVariable("requestId") Integer requestId,
            @ApiParam(value = "Reply to the RPC request, JSON. For example: {\"status\":\"success\"}", required = true)
            @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(json).build(), new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    /**
     * 功能：执行 `postRpcRequest` 对应的处理。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `JSON`：`JSON` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Send the RPC command (postRpcRequest)",
            notes = "Send the RPC request to server. The request payload is a JSON document that contains 'method' and 'params'. For example:" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\"method\": \"sumOnServer\", \"params\":{\"a\":2, \"b\":2}}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "The response contains arbitrary JSON with the RPC reply. For example: " +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\"result\": 4}" +
                    MARKDOWN_CODE_BLOCK_END +
                    REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postRpcRequest(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "The RPC request JSON", required = true)
            @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    JsonObject request = new JsonParser().parse(json).getAsJsonObject();
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            transportContext.getDefaultTimeout());
                    transportService.process(sessionInfo, ToServerRpcRequestMsg.newBuilder().setRequestId(0)
                                    .setMethodName(request.get("method").getAsString())
                                    .setParams(request.get("params").toString()).build(),
                            new SessionCloseOnErrorCallback(transportService, sessionInfo));
                }));
        return responseWriter;
    }

    /**
     * 功能：订阅`To Attributes`。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `seconds`：`seconds` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Subscribe to attribute updates (subscribeToAttributes) (Deprecated)",
            notes = "Subscribes to client and shared scope attribute updates using http long polling. " +
                    "Deprecated, since long polling is resource and network consuming. " +
                    "Consider using MQTT or CoAP protocol for light-weight real-time updates. \n\n" +
                    REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes/updates", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToAttributes(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "Optional timeout of the long poll. Typically less then 60 seconds, since limited on the server side.")
            @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            timeout == 0 ? transportContext.getDefaultTimeout() : timeout);
                    transportService.process(sessionInfo, SubscribeToAttributeUpdatesMsg.getDefaultInstance(),
                            new SessionCloseOnErrorCallback(transportService, sessionInfo));

                }));
        return responseWriter;
    }

    /**
     * 功能：获取`Firmware`。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `firmware`：`firmware` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Get Device Firmware (getFirmware)",
            notes = "Downloads the current firmware package." +
                    "When the platform initiates firmware update, " +
                    "it informs the device by updating the 'fw_title', 'fw_version', 'fw_checksum' and 'fw_checksum_algorithm' shared attributes." +
                    "The 'fw_title' and 'fw_version' parameters must be supplied in this request to double-check " +
                    "that the firmware that device is downloading matches the firmware it expects to download. " +
                    "This is important, since the administrator may change the firmware assignment while device is downloading the firmware. \n\n" +
                    "Optional 'chunk' and 'size' parameters may be used to download the firmware in chunks. " +
                    "For example, device may request first 16 KB of firmware using 'chunk'=0 and 'size'=16384. " +
                    "Next 16KB using 'chunk'=1 and 'size'=16384. The last chunk should have less bytes then requested using 'size' parameter. \n\n" +
                    REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/firmware", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getFirmware(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "Title of the firmware, corresponds to the value of 'fw_title' attribute.", required = true)
            @RequestParam(value = "title") String title,
            @ApiParam(value = "Version of the firmware, corresponds to the value of 'fw_version' attribute.", required = true)
            @RequestParam(value = "version") String version,
            @ApiParam(value = "Size of the chunk. Optional. Omit to download the entire file without chunks.")
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @ApiParam(value = "Index of the chunk. Optional. Omit to download the entire file without chunks.")
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.FIRMWARE);
    }

    /**
     * 功能：获取`Software`。
     * 参数：
     * - `ACCESS_TOKEN_PARAM_DESCRIPTION`：`ACCESS_TOKEN_PARAM_DESCRIPTION` 参数。
     * - `true`：`true` 参数。
     * - `deviceToken`：设备信息或设备标识。
     * - `software`：`software` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Get Device Software (getSoftware)",
            notes = "Downloads the current software package." +
                    "When the platform initiates software update, " +
                    "it informs the device by updating the 'sw_title', 'sw_version', 'sw_checksum' and 'sw_checksum_algorithm' shared attributes." +
                    "The 'sw_title' and 'sw_version' parameters must be supplied in this request to double-check " +
                    "that the software that device is downloading matches the software it expects to download. " +
                    "This is important, since the administrator may change the software assignment while device is downloading the software. \n\n" +
                    "Optional 'chunk' and 'size' parameters may be used to download the software in chunks. " +
                    "For example, device may request first 16 KB of software using 'chunk'=0 and 'size'=16384. " +
                    "Next 16KB using 'chunk'=1 and 'size'=16384. The last chunk should have less bytes then requested using 'size' parameter. \n\n" +
                    REQUIRE_ACCESS_TOKEN,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/software", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getSoftware(
            @ApiParam(value = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, defaultValue = "YOUR_DEVICE_ACCESS_TOKEN")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "Title of the software, corresponds to the value of 'sw_title' attribute.", required = true)
            @RequestParam(value = "title") String title,
            @ApiParam(value = "Version of the software, corresponds to the value of 'sw_version' attribute.", required = true)
            @RequestParam(value = "version") String version,
            @ApiParam(value = "Size of the chunk. Optional. Omit to download the entire file without using  chunks.")
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @ApiParam(value = "Index of the chunk. Optional. Omit to download the entire file without using chunks.")
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.SOFTWARE);
    }

    /**
     * 功能：执行 `provisionDevice` 对应的处理。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Provision new device (provisionDevice)",
            notes = "Exchange the provision request to the device credentials. " +
                    "See more info about provisioning in the corresponding 'Device provisioning' platform documentation." +
                    "Requires valid JSON request with the following format: " +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"deviceName\": \"NEW_DEVICE_NAME\",\n" +
                    "  \"provisionDeviceKey\": \"u7piawkboq8v32dmcmpp\",\n" +
                    "  \"provisionDeviceSecret\": \"jpmwdn8ptlswmf4m29bw\"\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "Where 'deviceName' is the name of enw or existing device which depends on the provisioning strategy. " +
                    "The 'provisionDeviceKey' and 'provisionDeviceSecret' matches info configured in one of the existing device profiles. " +
                    "The result of the successful call is the JSON object that contains new credentials:" +
                    MARKDOWN_CODE_BLOCK_START + "{\n" +
                    "  \"credentialsType\":\"ACCESS_TOKEN\",\n" +
                    "  \"credentialsValue\":\"DEVICE_ACCESS_TOKEN\",\n" +
                    "  \"status\":\"SUCCESS\"\n" +
                    "}" + MARKDOWN_CODE_BLOCK_END
            ,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/provision", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> provisionDevice(
            @ApiParam(value = "JSON with provision request. See API call description for example.")
            @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(JsonConverter.convertToProvisionRequestMsg(json),
                new DeviceProvisionCallback(responseWriter));
        return responseWriter;
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `deviceToken`：设备信息或设备标识。
     * - `title`：`title` 参数。
     * - `version`：`version` 参数。
     * - `size`：`size` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    private DeferredResult<ResponseEntity> getOtaPackageCallback(String deviceToken, String title, String version, int size, int chunk, OtaPackageType firmwareType) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportProtos.GetOtaPackageRequestMsg requestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                            .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                            .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                            .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                            .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                            .setType(firmwareType.name()).build();
                    transportContext.getTransportService().process(sessionInfo, requestMsg, new GetOtaPackageCallback(responseWriter, title, version, size, chunk));
                }));
        return responseWriter;
    }

    /**
     * 中文说明：
     * 1. 类目的：`DeviceAuthCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class DeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        private final TransportContext transportContext;
        private final DeferredResult<ResponseEntity> responseWriter;
        /**
         * `onSuccess` 字段，保存当前对象的对应属性。
         */
        private final Consumer<SessionInfoProto> onSuccess;

        DeviceAuthCallback(TransportContext transportContext, DeferredResult<ResponseEntity> responseWriter, Consumer<SessionInfoProto> onSuccess) {
            this.transportContext = transportContext;
            this.responseWriter = responseWriter;
            this.onSuccess = onSuccess;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
            if (msg.hasDeviceInfo()) {
                onSuccess.accept(SessionInfoCreator.create(msg, transportContext, UUID.randomUUID()));
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`DeviceProvisionCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class DeviceProvisionCallback implements TransportServiceCallback<ProvisionDeviceResponseMsg> {
        /**
         * 当前响应对象，封装处理完成后的返回信息。
         */
        private final DeferredResult<ResponseEntity> responseWriter;

        DeviceProvisionCallback(DeferredResult<ResponseEntity> responseWriter) {
            this.responseWriter = responseWriter;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onSuccess(ProvisionDeviceResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`GetOtaPackageCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private class GetOtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        /**
         * 当前响应对象，封装处理完成后的返回信息。
         */
        private final DeferredResult<ResponseEntity> responseWriter;
        private final String title;
        /**
         * 版本号，表示当前对象的对应属性。
         */
        private final String version;
        private final int chuckSize;
        /**
         * `chuck` 字段，保存当前对象的对应属性。
         */
        private final int chuck;

        GetOtaPackageCallback(DeferredResult<ResponseEntity> responseWriter, String title, String version, int chuckSize, int chuck) {
            this.responseWriter = responseWriter;
            this.title = title;
            this.version = version;
            this.chuckSize = chuckSize;
            this.chuck = chuck;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `otaPackageResponseMsg`：响应对象。
         * 返回：无。
         */
        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg otaPackageResponseMsg) {
            if (!TransportProtos.ResponseStatus.SUCCESS.equals(otaPackageResponseMsg.getResponseStatus())) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            } else if (title.equals(otaPackageResponseMsg.getTitle()) && version.equals(otaPackageResponseMsg.getVersion())) {
                String otaPackageId = new UUID(otaPackageResponseMsg.getOtaPackageIdMSB(), otaPackageResponseMsg.getOtaPackageIdLSB()).toString();
                ByteArrayResource resource = new ByteArrayResource(transportContext.getOtaPackageDataCache().get(otaPackageId, chuckSize, chuck));
                ResponseEntity<ByteArrayResource> response = ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackageResponseMsg.getFileName())
                        .header("x-filename", otaPackageResponseMsg.getFileName())
                        .contentLength(resource.contentLength())
                        .contentType(parseMediaType(otaPackageResponseMsg.getContentType()))
                        .body(resource);
                responseWriter.setResult(response);
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`SessionCloseOnErrorCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class SessionCloseOnErrorCallback implements TransportServiceCallback<Void> {
        /**
         * 服务，提供当前类调用的业务操作。
         */
        private final TransportService transportService;
        private final SessionInfoProto sessionInfo;

        SessionCloseOnErrorCallback(TransportService transportService, SessionInfoProto sessionInfo) {
            this.transportService = transportService;
            this.sessionInfo = sessionInfo;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onSuccess(Void msg) {
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            transportService.deregisterSession(sessionInfo);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`HttpOkCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class HttpOkCallback implements TransportServiceCallback<Void> {
        /**
         * 当前响应对象，封装处理完成后的返回信息。
         */
        private final DeferredResult<ResponseEntity> responseWriter;

        /**
         * 功能：创建 `DeviceApiController` 实例，并初始化必要字段。
         * 参数：
         * - `responseWriter`：响应对象。
         * 返回：新创建的对象实例。
         */
        public HttpOkCallback(DeferredResult<ResponseEntity> responseWriter) {
            this.responseWriter = responseWriter;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onSuccess(Void msg) {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`HttpSessionListener` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    @RequiredArgsConstructor
    private static class HttpSessionListener implements SessionMsgListener {

        /**
         * 当前响应对象，封装处理完成后的返回信息。
         */
        private final DeferredResult<ResponseEntity> responseWriter;
        private final TransportService transportService;
        /**
         * 会话，保存当前连接或交互过程的会话信息。
         */
        private final SessionInfoProto sessionInfo;

        /**
         * 功能：处理响应。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onGetAttributesResponse(GetAttributeResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        /**
         * 功能：处理属性。
         * 参数：
         * - `sessionId`：会话ID。
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg msg) {
            log.trace("[{}] Received attributes update notification to device", sessionId);
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        /**
         * 功能：处理会话。
         * 参数：
         * - `sessionId`：会话ID。
         * - `sessionCloseNotification`：会话对象。
         * 返回：无。
         */
        @Override
        public void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification) {
            log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT));
        }

        /**
         * 功能：处理设备。
         * 参数：
         * - `sessionId`：会话ID。
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg msg) {
            log.trace("[{}] Received RPC command to device", sessionId);
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg, true).toString(), HttpStatus.OK));
            transportService.process(sessionInfo, msg, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
        }

        /**
         * 功能：处理RPC。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onToServerRpcResponse(ToServerRpcResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        /**
         * 功能：处理设备。
         * 参数：
         * - `deviceId`：设备IDID。
         * 返回：无。
         */
        @Override
        public void onDeviceDeleted(DeviceId deviceId) {
            UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
            log.trace("[{}] Received device deleted notification for device with id: {}",sessionId, deviceId);
            responseWriter.setResult(new ResponseEntity<>("Device was deleted!", HttpStatus.FORBIDDEN));
        }

    }

    /**
     * 功能：解析类型。
     * 参数：
     * - `contentType`：类型。
     * 返回：处理结果。
     */
    private static MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return DataConstants.HTTP_TRANSPORT_NAME;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceApiController` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
