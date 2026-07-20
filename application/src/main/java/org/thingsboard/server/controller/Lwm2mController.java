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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.service.lwm2m.LwM2MService;

import java.util.Map;

import static org.thingsboard.server.controller.ControllerConstants.IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

/**
 * 中文说明：
 * 1. `Lwm2mController` 是 ThingsBoard Application 中处理 LwM2M 请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@Slf4j
@RestController
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && '${transport.lwm2m.enabled:false}'=='true'")
@RequestMapping("/api")
public class Lwm2mController extends BaseController {

    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Autowired
    private DeviceController deviceController;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private LwM2MService lwM2MService;

    /**
     * 服务端常量，用于统一引用固定值。
     */
    public static final String IS_BOOTSTRAP_SERVER = "isBootstrapServer";

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `bootstrapServer`：`bootstrapServer` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Lwm2m Bootstrap SecurityInfo (getLwm2mBootstrapSecurityInfo)",
            notes = "Get the Lwm2m Bootstrap SecurityInfo object (of the current server) based on the provided isBootstrapServer parameter. If isBootstrapServer == true, get the parameters of the current Bootstrap Server. If isBootstrapServer == false, get the parameters of the current Lwm2m Server. Used for client settings when starting the client in Bootstrap mode. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/bootstrap/{isBootstrapServer}", method = RequestMethod.GET)
    @ResponseBody
    public LwM2MServerSecurityConfigDefault getLwm2mBootstrapSecurityInfo(
        @ApiParam(value = IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION)
        @PathVariable(IS_BOOTSTRAP_SERVER) boolean bootstrapServer) throws ThingsboardException {
            return lwM2MService.getServerSecurityInfo(bootstrapServer);
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `deviceWithDeviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @ApiOperation(hidden = true, value = "Save device with credentials (Deprecated)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/device-credentials", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDeviceWithCredentials(@RequestBody Map<Class<?>, Object> deviceWithDeviceCredentials) throws ThingsboardException {
        Device device = checkNotNull(JacksonUtil.convertValue(deviceWithDeviceCredentials.get(Device.class), Device.class));
        DeviceCredentials credentials = checkNotNull(JacksonUtil.convertValue(deviceWithDeviceCredentials.get(DeviceCredentials.class), DeviceCredentials.class));
        return deviceController.saveDeviceWithCredentials(new SaveDeviceWithCredentialsRequest(device, credentials));
    }
}
