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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.HeaderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.thingsboard.server.common.data.StringUtils.isEmpty;

/**
 * 中文说明：
 * 1. 类目的：`TestRestClient` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
public class TestRestClient {
    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final RequestSpecification requestSpec;
    private String token;
    /**
     * 令牌，用于认证或安全校验。
     */
    private String refreshToken;

    /**
     * 功能：创建 `TestRestClient` 实例，并初始化必要字段。
     * 参数：
     * - `url`：`url` 参数。
     * 返回：新创建的对象实例。
     */
    public TestRestClient(String url) {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        requestSpec = given().baseUri(url)
                .contentType(ContentType.JSON)
                .config(RestAssuredConfig.config()
                        .headerConfig(HeaderConfig.headerConfig()
                                .overwriteHeadersWithName(JWT_TOKEN_HEADER_PARAM, CONTENT_TYPE_HEADER)));

        if (url.matches("^(https)://.*$")) {
            requestSpec.relaxedHTTPSValidation();
        }
    }

    /**
     * 功能：执行 `login` 对应的处理。
     * 参数：
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    public void login(String username, String password) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        JsonPath jsonPath = given().spec(requestSpec).body(loginRequest)
                .post("/api/auth/login")
                .getBody().jsonPath();
        token = jsonPath.get("token");
        refreshToken = jsonPath.get("refreshToken");
        requestSpec.header(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
    }

    /**
     * 功能：执行 `postDevice` 对应的处理。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Device postDevice(String accessToken, Device device) {
        return given().spec(requestSpec).body(device)
                .pathParams("accessToken", accessToken)
                .post("/api/device?accessToken={accessToken}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Device getDeviceByName(String deviceName) {
        return given().spec(requestSpec).pathParam("deviceName", deviceName)
                .get("/api/tenant/devices?deviceName={deviceName}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `statusCode`：`statusCode` 参数。
     * 返回：处理结果。
     */
    public ValidatableResponse getDeviceById(DeviceId deviceId, int statusCode) {
        return given().spec(requestSpec)
                .pathParams("deviceId", deviceId.getId())
                .get("/api/device/{deviceId}")
                .then()
                .statusCode(statusCode);
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    public Device getDeviceById(DeviceId deviceId) {
        return getDeviceById(deviceId, HTTP_OK)
                .extract()
                .as(Device.class);
    }

    /**
     * 功能：获取`Devices`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Device> getDevices(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/tenant/devices")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<Device>>() {
                });
    }

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    public DeviceCredentials getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        return given().spec(requestSpec).get("/api/device/{deviceId}/credentials", deviceId.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceCredentials.class);
    }

    /**
     * 功能：执行 `postTelemetry` 对应的处理。
     * 参数：
     * - `credentialsId`：凭据ID。
     * - `telemetry`：`telemetry` 参数。
     * 返回：处理结果。
     */
    public ValidatableResponse postTelemetry(String credentialsId, JsonNode telemetry) {
        return given().spec(requestSpec).body(telemetry)
                .post("/api/v1/{credentialsId}/telemetry", credentialsId)
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    public ValidatableResponse deleteDevice(DeviceId deviceId) {
        return given().spec(requestSpec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    public ValidatableResponse deleteDeviceIfExists(DeviceId deviceId) {
        return given().spec(requestSpec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
    }

    /**
     * 功能：执行 `postTelemetryAttribute` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `attribute`：`attribute` 参数。
     * 返回：处理结果。
     */
    public ValidatableResponse postTelemetryAttribute(String entityType, DeviceId deviceId, String scope, JsonNode attribute) {
        return given().spec(requestSpec).body(attribute)
                .post("/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}", entityType, deviceId.getId(), scope)
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：执行 `postAttribute` 对应的处理。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `attribute`：`attribute` 参数。
     * 返回：处理结果。
     */
    public ValidatableResponse postAttribute(String accessToken, JsonNode attribute) {
        return given().spec(requestSpec).body(attribute)
                .post("/api/v1/{accessToken}/attributes/", accessToken)
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：获取`Attributes`。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `clientKeys`：客户端对象。
     * - `sharedKeys`：键。
     * 返回：处理结果。
     */
    public JsonNode getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        return given().spec(requestSpec)
                .queryParam("clientKeys", clientKeys)
                .queryParam("sharedKeys", sharedKeys)
                .get("/api/v1/{accessToken}/attributes", accessToken)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
    }

    /**
     * 功能：执行 `postProvisionRequest` 对应的处理。
     * 参数：
     * - `provisionRequest`：请求对象。
     * 返回：处理结果。
     */
    public JsonPath postProvisionRequest(String provisionRequest) {
        return given().spec(requestSpec)
                .body(provisionRequest)
                .post("/api/v1/provision")
                .getBody()
                .jsonPath();
    }

    /**
     * 功能：获取`Rule Chains`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/ruleChains")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<RuleChain>>() {
                });
    }

    /**
     * 功能：执行 `postRuleChain` 对应的处理。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    public RuleChain postRuleChain(RuleChain ruleChain) {
        return given().spec(requestSpec)
                .body(ruleChain)
                .post("/api/ruleChain")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(RuleChain.class);
    }

    /**
     * 功能：执行 `postRuleChainMetadata` 对应的处理。
     * 参数：
     * - `ruleChainMetaData`：待处理数据。
     * 返回：处理结果。
     */
    public RuleChainMetaData postRuleChainMetadata(RuleChainMetaData ruleChainMetaData) {
        return given().spec(requestSpec)
                .body(ruleChainMetaData)
                .post("/api/ruleChain/metadata")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(RuleChainMetaData.class);
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    public void setRootRuleChain(RuleChainId ruleChainId) {
        given().spec(requestSpec)
                .post("/api/ruleChain/{ruleChainId}/root", ruleChainId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    public void deleteRuleChain(RuleChainId ruleChainId) {
        given().spec(requestSpec)
                .delete("/api/ruleChain/{ruleChainId}", ruleChainId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：文本结果。
     */
    private String getUrlParams(PageLink pageLink) {
        String urlParams = "pageSize={pageSize}&page={page}";
        if (!isEmpty(pageLink.getTextSearch())) {
            urlParams += "&textSearch={textSearch}";
        }
        if (pageLink.getSortOrder() != null) {
            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
        }
        return urlParams;
    }

    /**
     * 功能：保存或创建分页查询条件。
     * 参数：
     * - `params`：键值映射。
     * - `pageLink`：`pageLink` 参数。
     * 返回：无。
     */
    private void addPageLinkToParam(Map<String, String> params, PageLink pageLink) {
        params.put("pageSize", String.valueOf(pageLink.getPageSize()));
        params.put("page", String.valueOf(pageLink.getPage()));
        if (!isEmpty(pageLink.getTextSearch())) {
            params.put("textSearch", pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            params.put("sortProperty", pageLink.getSortOrder().getProperty());
            params.put("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelation> findRelationByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return given().spec(requestSpec)
                .pathParams(params)
                .get("/api/relations?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<EntityRelation>>() {
                });
    }

    /**
     * 功能：执行 `postServerSideRpc` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `serverRpcPayload`：`serverRpcPayload` 参数。
     * 返回：处理结果。
     */
    public JsonNode postServerSideRpc(DeviceId deviceId, JsonNode serverRpcPayload) {
        return given().spec(requestSpec)
                .body(serverRpcPayload)
                .post("/api/rpc/twoway/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DeviceProfile> getDeviceProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/deviceProfiles")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<DeviceProfile>>() {
                });
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    public DeviceProfile getDeviceProfileById(DeviceProfileId deviceProfileId) {
        return given().spec(requestSpec).get("/api/deviceProfile/{deviceProfileId}", deviceProfileId.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceProfile.class);
    }

    /**
     * 功能：执行 `postDeviceProfile` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public DeviceProfile postDeviceProfile(DeviceProfile deviceProfile) {
        return given().spec(requestSpec).body(deviceProfile)
                .post("/api/deviceProfile")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceProfile.class);
    }

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    public void deleteDeviseProfile(DeviceProfileId deviceProfileId) {
        given().spec(requestSpec)
                .delete("/api/deviceProfile/{deviceProfileId}", deviceProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    public void setDefaultDeviceProfile(DeviceProfileId deviceProfileId) {
        given().spec(requestSpec)
                .post("/api/deviceProfile/{deviceProfileId}/default", deviceProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：执行 `postAssetProfile` 对应的处理。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：匹配的数据集合。
     */
    public AssetProfile postAssetProfile(AssetProfile assetProfile) {
        return given().spec(requestSpec).body(assetProfile)
                .post("/api/assetProfile")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(AssetProfile.class);
    }

    /**
     * 功能：获取资产。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AssetProfile> getAssetProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/assetProfiles")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<AssetProfile>>() {
                });
    }

    /**
     * 功能：删除或清理资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：无。
     */
    public void deleteAssetProfile(AssetProfileId assetProfileId) {
        given().spec(requestSpec)
                .delete("/api/assetProfile/{assetProfileId}", assetProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：无。
     */
    public void setDefaultAssetProfile(AssetProfileId assetProfileId) {
        given().spec(requestSpec)
                .post("/api/assetProfile/{assetProfileId}/default", assetProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：执行 `postCustomer` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：处理结果。
     */
    public Customer postCustomer(Customer customer) {
        return given().spec(requestSpec)
                .body(customer)
                .post("/api/customer")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Customer.class);
    }

    /**
     * 功能：删除或清理客户。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    public void deleteCustomer(CustomerId customerId) {
        given().spec(requestSpec)
                .delete("/api/customer/{customerId}", customerId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：获取`Customers`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Customer> getCustomers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/customers")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<Customer>>() {
                });
    }

    /**
     * 功能：执行 `postAlarm` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    public Alarm postAlarm(Alarm alarm) {
        return given().spec(requestSpec)
                .body(alarm)
                .post("/api/alarm")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Alarm.class);
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：无。
     */
    public void deleteAlarm(AlarmId alarmId) {
        given().spec(requestSpec)
                .delete("/api/alarm/{alarmId}", alarmId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：执行 `postUser` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    public User postUser(User user) {
        return given().spec(requestSpec)
                .body(user)
                .post("/api/user?sendActivationMail=false")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(User.class);
    }

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：无。
     */
    public void deleteUser(UserId userId) {
        given().spec(requestSpec)
                .delete("/api/user/{userId}", userId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getToken() {
        return token;
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 功能：执行 `postAsset` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    public Asset postAsset(Asset asset) {
        return given().spec(requestSpec)
                .body(asset)
                .post("/api/asset")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Asset.class);
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Asset getAssetById(AssetId assetId) {
        return given().spec(requestSpec)
                .get("/api/asset/{assetId}", assetId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Asset.class);
    }

    /**
     * 功能：删除或清理资产。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    public void deleteAsset(AssetId assetId) {
        given().spec(requestSpec)
                .delete("/api/asset/{assetId}", assetId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：执行 `postEntityView` 对应的处理。
     * 参数：
     * - `entityView`：实体对象。
     * 返回：处理结果。
     */
    public EntityView postEntityView(EntityView entityView) {
        return given().spec(requestSpec)
                .body(entityView)
                .post("/api/entityView")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityView.class);
    }

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：处理结果。
     */
    public EntityView getEntityViewById(EntityViewId entityViewId) {
        return given().spec(requestSpec)
                .get("/api/entityView/{entityViewId}", entityViewId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityView.class);
    }

    /**
     * 功能：删除或清理实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：无。
     */
    public void deleteEntityView(EntityViewId entityViewId) {
        given().spec(requestSpec)
                .delete("/api/entityView/{entityViewId}", entityViewId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：执行 `postDashboard` 对应的处理。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：处理结果。
     */
    public Dashboard postDashboard(Dashboard dashboard) {
        return given().spec(requestSpec)
                .body(dashboard)
                .post("/api/dashboard")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Dashboard.class);
    }

    /**
     * 功能：删除或清理仪表盘。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    public void deleteDashboard(DashboardId dashboardId) {
        given().spec(requestSpec)
                .delete("/api/dashboard/{dashboardId}", dashboardId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    public void setDevicePublic(DeviceId deviceId) {
        given().spec(requestSpec)
                .post("/api/customer/public/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * 功能：获取`Events`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EventInfo> getEvents(EntityId entityId, EventType eventType, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("eventType", eventType.name());
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return given().spec(requestSpec)
                .get("/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&" + getTimeUrlParams(pageLink), params)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<>() {});
    }

    /**
     * 功能：保存或创建分页查询条件。
     * 参数：
     * - `params`：键值映射。
     * - `pageLink`：`pageLink` 参数。
     * 返回：无。
     */
    private void addTimePageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        this.addPageLinkToParam(params, pageLink);
        if (pageLink.getStartTime() != null) {
            params.put("startTime", String.valueOf(pageLink.getStartTime()));
        }
        if (pageLink.getEndTime() != null) {
            params.put("endTime", String.valueOf(pageLink.getEndTime()));
        }
    }

    /**
     * 功能：获取时间。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：文本结果。
     */
    private String getTimeUrlParams(TimePageLink pageLink) {
        String urlParams = getUrlParams(pageLink);
        if (pageLink.getStartTime() != null) {
            urlParams += "&startTime={startTime}";
        }
        if (pageLink.getEndTime() != null) {
            urlParams += "&endTime={endTime}";
        }
        return urlParams;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TestRestClient` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
