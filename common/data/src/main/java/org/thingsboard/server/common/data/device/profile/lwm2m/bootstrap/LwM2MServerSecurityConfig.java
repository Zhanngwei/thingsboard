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
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MServerSecurityConfig` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
public class LwM2MServerSecurityConfig {

    /**
     * 服务端ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "Server short Id. Used as link to associate server Object Instance. This identifier uniquely identifies each LwM2M Server configured for the LwM2M Client. " +
            "This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'. " +
            "The values ID:0 and ID:65535 values MUST NOT be used for identifying the LwM2M Server.", example = "123", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected Integer shortServerId = 123;
    /** Security -> ObjectId = 0 'LWM2M Security' */
    /**
     * 是否满足服务端条件。
     */
    @ApiModelProperty(position = 2, value = "Is Bootstrap Server or Lwm2m Server. " +
            "The LwM2M Client MAY be configured to use one or more LwM2M Server Account(s). " +
            "The LwM2M Client MUST have at most one LwM2M Bootstrap-Server Account. " +
            "(*) The LwM2M client MUST have at least one LwM2M server account after completing the boot sequence specified.", example = "true or false", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected boolean bootstrapServerIs = false;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @ApiModelProperty(position = 3, value = "Host for 'No Security' mode", example = "0.0.0.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @ApiModelProperty(position = 4, value = "Port for  Lwm2m Server: 'No Security' mode: Lwm2m Server or Bootstrap Server", example = "'5685' or '5687'", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected Integer port;
    /**
     * 时间，用于发起外部调用或协议交互。
     */
    @ApiModelProperty(position = 7, value = "Client Hold Off Time. The number of seconds to wait before initiating a Client Initiated Bootstrap once the LwM2M Client has determined it should initiate this bootstrap mode. (This information is relevant for use with a Bootstrap-Server only.)", example = "1", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected Integer clientHoldOffTime = 1;
    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    @ApiModelProperty(position = 8, value = "Server Public Key for 'Security' mode (DTLS): RPK or X509. Format: base64 encoded", example = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAZ0pSaGKHk/GrDaUDnQZpeEdGwX7m3Ws+U/kiVat\n" +
            "+44sgk3c8g0LotfMpLlZJPhPwJ6ipXV+O1r7IZUjBs3LNA==", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected String serverPublicKey;
    /**
     * 证书，用于认证或安全校验。
     */
    @ApiModelProperty(position = 9, value = "Server Public Key for 'Security' mode (DTLS): X509. Format: base64 encoded", example = "MMIICODCCAd6gAwIBAgIUI88U1zowOdrxDK/dOV+36gJxI2MwCgYIKoZIzj0EAwIwejELMAkGA1UEBhMCVUs\n" +
            "xEjAQBgNVBAgTCUt5aXYgY2l0eTENMAsGA1UEBxMES3lpdjEUMBIGA1UEChMLVGhpbmdzYm9hcmQxFzAVBgNVBAsMDkRFVkVMT1BFUl9URVNUMRkwFwYDVQQDDBBpbnRlcm1lZGlhdGVfY2EwMB4XDTIyMDEwOTEzMDMwMFoXDTI3MDEwODEzMDMwMFowFDESMBAGA1UEAxM\n" +
            "JbG9jYWxob3N0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUO3vBo/JTv0eooY7XHiKAIVDoWKFqtrU7C6q8AIKqpLcqhCdW+haFeBOH3PjY6EwaWkY04Bir4oanU0s7tz2uKOBpzCBpDAOBgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYIKwYBBQUHAwEwDAYDVR0TAQH/\n" +
            "BAIwADAdBgNVHQ4EFgQUEjc3Q4a0TxzP/3x3EV4fHxYUg0YwHwYDVR0jBBgwFoAUuSquGycMU6Q0SYNcbtSkSD3TfH0wLwYDVR0RBCgwJoIVbG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3SCAiAtMAoGCCqGSM49BAMCA0gAMEUCIQD7dbZObyUaoDiNbX+9fUNp\n" +
            "AWrD7N7XuJUwZ9FcN75R3gIgb2RNjDkHoyUyF1YajwkBk+7XmIXNClmizNJigj908mw=", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected String serverCertificate;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @ApiModelProperty(position = 10, value = "Bootstrap Server Account Timeout (If the value is set to 0, or if this resource is not instantiated, the Bootstrap-Server Account lifetime is infinite.)", example = "0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    Integer bootstrapServerAccountTimeout = 0;

    /** Config -> ObjectId = 1 'LwM2M Server' */
    /**
     * `lifetime` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 11, value = "Specify the lifetime of the registration in seconds.", example = "300", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Integer lifetime = 300;
    /**
     * `defaultMinPeriod` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 12, value = "The default value the LwM2M Client should use for the Minimum Period of an Observation in the absence of this parameter being included in an Observation. " +
            "If this Resource doesn’t exist, the default value is 0.", example = "1", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Integer defaultMinPeriod = 1;
    /** ResourceID=6 'Notification Storing When Disabled or Offline' */
    /**
     * 是否禁用`notif if`。
     */
    @ApiModelProperty(position = 13, value = "If true, the LwM2M Client stores “Notify” operations to the LwM2M Server while the LwM2M Server account is disabled or the LwM2M Client is offline. After the LwM2M Server account is enabled or the LwM2M Client is online, the LwM2M Client reports the stored “Notify” operations to the Server. " +
            "If false, the LwM2M Client discards all the “Notify” operations or temporarily disables the Observe function while the LwM2M Server is disabled or the LwM2M Client is offline. " +
            "The default value is true.", example = "true", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private boolean notifIfDisabled = true;
    /**
     * 绑定模式，用于区分当前对象的状态或类别。
     */
    @ApiModelProperty(position = 14, value = "This Resource defines the transport binding configured for the LwM2M Client. " +
            "If the LwM2M Client supports the binding specified in this Resource, the LwM2M Client MUST use that transport for the Current Binding Mode.", example = "U", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String binding = "U";
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MServerSecurityConfig` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
