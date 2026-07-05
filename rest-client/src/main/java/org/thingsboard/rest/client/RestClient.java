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
package org.thingsboard.rest.client;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rest.client.utils.RestJsonConverter;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.ImageExportData;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isEmpty;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`RestClient` 是 ThingsBoard Rest Client 模块 中的REST API 客户端门面，用于把 ThingsBoard 服务端 REST 接口包装成 Java 方法，统一处理 JWT 登录、刷新、URL 参数、分页查询、文件上传下载和 DTO 映射。
 * 2. 所属模块：位于 rest-client，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Spring RestTemplate、JWT、common data DTO、ThingsBoard 服务端 REST Controller、租户/客户/设备/规则链/资源等业务模块。
 * 4. 生命周期：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器。
 * 5. 存在原因：使用门面类集中 REST 调用可以稳定客户端 API，调用方无需散落拼接 URL、Header、分页参数和 token 刷新逻辑。
 * 6. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
 * 7. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
 * 8. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
 * 9. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
 * 10. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
 * 11. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
 * 12. 设计模式：主要体现 Facade / Adapter。
 */
public class RestClient implements Closeable {
    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private static final long AVG_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";
    private final ExecutorService service = ThingsBoardExecutors.newWorkStealingPool(10, getClass());
    /**
     * HTTP 客户端，用于支撑当前网络或外部服务交互。
     */
    protected final RestTemplate restTemplate;
    protected final RestTemplate loginRestTemplate;
    protected final String baseURL;

    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;
    private String password;
    private String mainToken;
    /**
     * 令牌，用于认证或安全校验。
     */
    private String refreshToken;
    private long mainTokenExpTs;
    private long refreshTokenExpTs;
    /**
     * 时间，用于发起外部调用或协议交互。
     */
    private long clientServerTimeDiff;

    public RestClient(String baseURL) {
        this(new RestTemplate(), baseURL);
    }

    /**
     * 功能：创建 `RestClient` 实例，并初始化必要字段。
     * 参数：
     * - `restTemplate`：`restTemplate` 参数。
     * - `baseURL`：`baseURL` 参数。
     * 返回：新创建的对象实例。
     */
    public RestClient(RestTemplate restTemplate, String baseURL) {
        this.restTemplate = restTemplate;
        this.loginRestTemplate = new RestTemplate(restTemplate.getRequestFactory());
        this.baseURL = baseURL;
        this.restTemplate.getInterceptors().add((request, bytes, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            long calculatedTs = System.currentTimeMillis() + clientServerTimeDiff + AVG_REQUEST_TIMEOUT;
            if (calculatedTs > mainTokenExpTs) {
                synchronized (RestClient.this) {
                    if (calculatedTs > mainTokenExpTs) {
                        if (calculatedTs < refreshTokenExpTs) {
                            refreshToken();
                        } else {
                            doLogin();
                        }
                    }
                }
            }
            wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + mainToken);
            return execution.execute(wrapper, bytes);
        });
    }

    /**
     * 功能：获取HTTP 客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getToken() {
        return mainToken;
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
     * 功能：更新令牌。
     * 参数：无。
     * 返回：无。
     */
    public void refreshToken() {
        Map<String, String> refreshTokenRequest = new HashMap<>();
        refreshTokenRequest.put("refreshToken", refreshToken);
        long ts = System.currentTimeMillis();
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/token", refreshTokenRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    /**
     * 功能：执行 `login` 对应的处理。
     * 参数：
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    public void login(String username, String password) {
        this.username = username;
        this.password = password;
        doLogin();
    }

    /**
     * 功能：执行 `doLogin` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void doLogin() {
        long ts = System.currentTimeMillis();
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    /**
     * 功能：更新信息对象。
     * 参数：
     * - `ts`：时间戳。
     * - `tokenInfo`：`tokenInfo` 参数。
     * 返回：无。
     */
    private synchronized void setTokenInfo(long ts, JsonNode tokenInfo) {
        this.mainToken = tokenInfo.get("token").asText();
        this.refreshToken = tokenInfo.get("refreshToken").asText();
        this.mainTokenExpTs = JWT.decode(this.mainToken).getExpiresAtAsInstant().toEpochMilli();
        this.refreshTokenExpTs = JWT.decode(refreshToken).getExpiresAtAsInstant().toEpochMilli();
        this.clientServerTimeDiff = JWT.decode(this.mainToken).getIssuedAtAsInstant().toEpochMilli() - ts;
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    public Optional<AdminSettings> getAdminSettings(String key) {
        try {
            ResponseEntity<AdminSettings> adminSettings = restTemplate.getForEntity(baseURL + "/api/admin/settings/{key}", AdminSettings.class, key);
            return Optional.ofNullable(adminSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/settings", adminSettings, AdminSettings.class).getBody();
    }

    /**
     * 功能：发送或提交`Test Mail`。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：无。
     */
    public void sendTestMail(AdminSettings adminSettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testMail", adminSettings);
    }

    /**
     * 功能：发送或提交`Test Sms`。
     * 参数：
     * - `testSmsRequest`：请求对象。
     * 返回：无。
     */
    public void sendTestSms(TestSmsRequest testSmsRequest) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testSms", testSmsRequest);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Optional<SecuritySettings> getSecuritySettings() {
        try {
            ResponseEntity<SecuritySettings> securitySettings = restTemplate.getForEntity(baseURL + "/api/admin/securitySettings", SecuritySettings.class);
            return Optional.ofNullable(securitySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `securitySettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/securitySettings", securitySettings, SecuritySettings.class).getBody();
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Optional<JwtSettings> getJwtSettings() {
        try {
            ResponseEntity<JwtSettings> jwtSettings = restTemplate.getForEntity(baseURL + "/api/admin/jwtSettings", JwtSettings.class);
            return Optional.ofNullable(jwtSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `jwtSettings`：配置对象。
     * 返回：处理结果。
     */
    public JwtPair saveJwtSettings(JwtSettings jwtSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/jwtSettings", jwtSettings, JwtPair.class).getBody();
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Optional<RepositorySettings> getRepositorySettings() {
        try {
            ResponseEntity<RepositorySettings> repositorySettings = restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings", RepositorySettings.class);
            return Optional.ofNullable(repositorySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `repositorySettingsExists` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public Boolean repositorySettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings/exists", Boolean.class).getBody();
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `repositorySettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    public RepositorySettings saveRepositorySettings(RepositorySettings repositorySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/repositorySettings", repositorySettings, RepositorySettings.class).getBody();
    }

    /**
     * 功能：删除或清理配置。
     * 参数：无。
     * 返回：无。
     */
    public void deleteRepositorySettings() {
        restTemplate.delete(baseURL + "/api/admin/repositorySettings");
    }

    /**
     * 功能：校验存取组件。
     * 参数：
     * - `repositorySettings`：配置对象。
     * 返回：无。
     */
    public void checkRepositoryAccess(RepositorySettings repositorySettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/repositorySettings/checkAccess", repositorySettings);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Optional<AutoCommitSettings> getAutoCommitSettings() {
        try {
            ResponseEntity<AutoCommitSettings> autoCommitSettings = restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings", AutoCommitSettings.class);
            return Optional.ofNullable(autoCommitSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `autoCommitSettingsExists` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public Boolean autoCommitSettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings/exists", Boolean.class).getBody();
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `autoCommitSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    public AutoCommitSettings saveAutoCommitSettings(AutoCommitSettings autoCommitSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/autoCommitSettings", autoCommitSettings, AutoCommitSettings.class).getBody();
    }

    /**
     * 功能：删除或清理配置。
     * 参数：无。
     * 返回：无。
     */
    public void deleteAutoCommitSettings() {
        restTemplate.delete(baseURL + "/api/admin/autoCommitSettings");
    }

    /**
     * 功能：校验`Updates`。
     * 参数：无。
     * 返回：判断结果。
     */
    public Optional<UpdateMessage> checkUpdates() {
        try {
            ResponseEntity<UpdateMessage> updateMsg = restTemplate.getForEntity(baseURL + "/api/admin/updates", UpdateMessage.class);
            return Optional.ofNullable(updateMsg.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    public SystemInfo getSystemInfo() {
        return restTemplate.getForEntity(baseURL + "/api/admin/systemInfo", SystemInfo.class).getBody();
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Alarm> getAlarmById(AlarmId alarmId) {
        try {
            ResponseEntity<Alarm> alarm = restTemplate.getForEntity(baseURL + "/api/alarm/{alarmId}", Alarm.class, alarmId.getId());
            return Optional.ofNullable(alarm.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：可能存在的结果。
     */
    public Optional<AlarmInfo> getAlarmInfoById(AlarmId alarmId) {
        try {
            ResponseEntity<AlarmInfo> alarmInfo = restTemplate.getForEntity(baseURL + "/api/alarm/info/{alarmId}", AlarmInfo.class, alarmId.getId());
            return Optional.ofNullable(alarmInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    public Alarm saveAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：无。
     */
    public void deleteAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}", alarmId.getId());
    }

    /**
     * 功能：执行 `ackAlarm` 对应的处理。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：无。
     */
    public void ackAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/ack", null, alarmId.getId());
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：无。
     */
    public void clearAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/clear", null, alarmId.getId());
    }

    /**
     * 功能：执行 `assignAlarm` 对应的处理。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    public void assignAlarm(AlarmId alarmId, UserId userId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/assign/{userId}", null, alarmId.getId(), userId.getId());
    }

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：无。
     */
    public void unassignAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}/assign", alarmId.getId());
    }

    /**
     * 功能：获取`Alarms`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `searchStatus`：`searchStatus` 参数。
     * - `status`：`status` 参数。
     * - `pageLink`：`pageLink` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    public PageData<AlarmInfo> getAlarms(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status, TimePageLink pageLink, Boolean fetchOriginator) {
        String urlSecondPart = "/api/alarm/{entityType}/{entityId}?fetchOriginator={fetchOriginator}";
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("fetchOriginator", String.valueOf(fetchOriginator));
        if (searchStatus != null) {
            params.put("searchStatus", searchStatus.name());
            urlSecondPart += "&searchStatus={searchStatus}";
        }
        if (status != null) {
            params.put("status", status.name());
            urlSecondPart += "&status={status}";
        }

        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + urlSecondPart + "&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `entityId`：实体IDID。
     * - `searchStatus`：`searchStatus` 参数。
     * - `status`：`status` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<AlarmSeverity> getHighestAlarmSeverity(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("searchStatus", searchStatus.name());
        params.put("status", status.name());
        try {
            ResponseEntity<AlarmSeverity> alarmSeverity = restTemplate.getForEntity(baseURL + "/api/alarm/highestSeverity/{entityType}/{entityId}?searchStatus={searchStatus}&status={status}", AlarmSeverity.class, params);
            return Optional.ofNullable(alarmSeverity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public Alarm createAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public List<EntitySubtype> getAlarmTypes(PageLink pageLink) {
        return restTemplate.exchange(
                baseURL + "/api/alarm/types?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    public AlarmComment saveAlarmComment(AlarmId alarmId, AlarmComment alarmComment) {
        return restTemplate.postForEntity(baseURL + "/api/alarm/{alarmId}/comment", alarmComment, AlarmComment.class, alarmId.getId()).getBody();
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `alarmCommentId`：告警IDID。
     * 返回：无。
     */
    public void deleteAlarmComment(AlarmId alarmId, AlarmCommentId alarmCommentId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}/comment/{alarmCommentId}",
                alarmId.getId(), alarmCommentId.getId());
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AlarmCommentInfo> getAlarmComments(AlarmId alarmId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("alarmId", alarmId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/alarm/{alarmId}/comment?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmCommentInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> getAssetById(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/asset/{assetId}", Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<AssetInfo> getAssetInfoById(AssetId assetId) {
        try {
            ResponseEntity<AssetInfo> asset = restTemplate.getForEntity(baseURL + "/api/asset/info/{assetId}", AssetInfo.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    public Asset saveAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    /**
     * 功能：删除或清理资产。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    public void deleteAsset(AssetId assetId) {
        restTemplate.delete(baseURL + "/api/asset/{assetId}", assetId.getId());
    }

    /**
     * 功能：执行 `assignAssetToCustomer` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> assignAssetToCustomer(CustomerId customerId, AssetId assetId) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("assetId", assetId.getId().toString());

        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", null, Asset.class, params);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignAssetFromCustomer` 对应的处理。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> unassignAssetFromCustomer(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.exchange(baseURL + "/api/customer/asset/{assetId}", HttpMethod.DELETE, HttpEntity.EMPTY, Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignAssetToPublicCustomer` 对应的处理。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> assignAssetToPublicCustomer(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/customer/public/asset/{assetId}", null, Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `assetType`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<Asset> getTenantAssets(PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/api/tenant/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `type`：类型。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AssetInfo> getTenantAssetInfos(String type, AssetProfileId assetProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("assetProfileId", assetProfileId != null ? assetProfileId.toString() : null);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AssetInfo>> assets = restTemplate.exchange(
                baseURL + "/api/tenant/assetInfos?type={type}&assetProfileId={assetProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetInfo>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `assetName`：名称。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> getTenantAsset(String assetName) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, assetName);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `assetType`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<Asset> getCustomerAssets(CustomerId customerId, PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `assetType`：类型。
     * - `assetProfileId`：资产配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AssetInfo> getCustomerAssetInfos(CustomerId customerId, String assetType, AssetProfileId assetProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", assetType);
        params.put("assetProfileId", assetProfileId != null ? assetProfileId.toString() : null);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AssetInfo>> assets = restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/assetInfos?type={type}&assetProfileId={assetProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetInfo>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 功能：获取`Assets By Ids`。
     * 参数：
     * - `assetIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    public List<Asset> getAssetsByIds(List<AssetId> assetIds) {
        return restTemplate.exchange(
                        baseURL + "/api/assets?assetIds={assetIds}",
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<Asset>>() {
                        },
                        listIdsToString(assetIds))
                .getBody();
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public List<Asset> findByQuery(AssetSearchQuery query) {
        return restTemplate.exchange(
                URI.create(baseURL + "/api/assets"),
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Asset>>() {
                }).getBody();
    }

    /**
     * 功能：获取资产。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.2")
    public List<EntitySubtype> getAssetTypes() {
        return restTemplate.exchange(URI.create(
                        baseURL + "/api/asset/types"),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    public List<EntitySubtype> getAssetProfileNames(boolean activeOnly) {
        return restTemplate.exchange(
                baseURL + "/api/assetProfile/names?activeOnly={activeOnly}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }, activeOnly).getBody();
    }

    /**
     * 功能：处理`Assets Bulk Import`。
     * 参数：
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    public BulkImportResult<Asset> processAssetsBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/asset/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Asset>>() {
                }).getBody();
    }

    /**
     * 功能：获取资产。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public Optional<Asset> findAsset(String name) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("assetName", name);
        try {
            ResponseEntity<Asset> assetEntity = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, params);
            return Optional.of(assetEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public Asset createAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public Asset createAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    /**
     * 功能：执行 `assignAsset` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public Asset assignAsset(CustomerId customerId, AssetId assetId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", HttpEntity.EMPTY, Asset.class,
                customerId.toString(), assetId.toString()).getBody();
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `actionTypes`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<AuditLog> getAuditLogsByCustomerId(CustomerId customerId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/customer/{customerId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `userId`：用户ID。
     * - `pageLink`：`pageLink` 参数。
     * - `actionTypes`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<AuditLog> getAuditLogsByUserId(UserId userId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/user/{userId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * - `actionTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AuditLog> getAuditLogsByEntityId(EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/entity/{entityType}/{entityId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    /**
     * 功能：获取`Audit Logs`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `actionTypes`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<AuditLog> getAuditLogs(TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    /**
     * 功能：获取令牌。
     * 参数：
     * - `userId`：用户ID。
     * 返回：文本结果。
     */
    public String getActivateToken(UserId userId) {
        String activationLink = getActivationLink(userId);
        return activationLink.substring(activationLink.lastIndexOf(ACTIVATE_TOKEN_REGEX) + ACTIVATE_TOKEN_REGEX.length());
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public Optional<User> getUser() {
        ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/auth/user", User.class);
        return Optional.ofNullable(user.getBody());
    }

    /**
     * 功能：执行 `logout` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void logout() {
        restTemplate.postForLocation(baseURL + "/api/auth/logout", null);
    }

    /**
     * 功能：执行 `changePassword` 对应的处理。
     * 参数：
     * - `currentPassword`：`currentPassword` 参数。
     * - `newPassword`：`newPassword` 参数。
     * 返回：无。
     */
    public void changePassword(String currentPassword, String newPassword) {
        ObjectNode changePasswordRequest = JacksonUtil.newObjectNode();
        changePasswordRequest.put("currentPassword", currentPassword);
        changePasswordRequest.put("newPassword", newPassword);
        restTemplate.postForLocation(baseURL + "/api/auth/changePassword", changePasswordRequest);
    }

    /**
     * 功能：获取密码。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public Optional<UserPasswordPolicy> getUserPasswordPolicy() {
        try {
            ResponseEntity<UserPasswordPolicy> userPasswordPolicy = restTemplate.getForEntity(baseURL + "/api/noauth/userPasswordPolicy", UserPasswordPolicy.class);
            return Optional.ofNullable(userPasswordPolicy.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：校验令牌。
     * 参数：
     * - `userId`：用户ID。
     * 返回：判断结果。
     */
    public ResponseEntity<String> checkActivateToken(UserId userId) {
        String activateToken = getActivateToken(userId);
        return restTemplate.getForEntity(baseURL + "/api/noauth/activate?activateToken={activateToken}", String.class, activateToken);
    }

    /**
     * 功能：执行 `requestResetPasswordByEmail` 对应的处理。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    public void requestResetPasswordByEmail(String email) {
        ObjectNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode();
        resetPasswordByEmailRequest.put("email", email);
        restTemplate.postForLocation(baseURL + "/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest);
    }

    /**
     * 功能：执行 `activateUser` 对应的处理。
     * 参数：
     * - `userId`：用户ID。
     * - `password`：`password` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<JsonNode> activateUser(UserId userId, String password) {
        return activateUser(userId, password, true);
    }

    /**
     * 功能：执行 `activateUser` 对应的处理。
     * 参数：
     * - `userId`：用户ID。
     * - `password`：`password` 参数。
     * - `sendActivationMail`：`sendActivationMail` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<JsonNode> activateUser(UserId userId, String password, boolean sendActivationMail) {
        ObjectNode activateRequest = JacksonUtil.newObjectNode();
        activateRequest.put("activateToken", getActivateToken(userId));
        activateRequest.put("password", password);
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activate?sendActivationMail={sendActivationMail}", activateRequest, JsonNode.class, sendActivationMail);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取`Component Descriptor By Clazz`。
     * 参数：
     * - `componentDescriptorClazz`：`componentDescriptorClazz` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<ComponentDescriptor> getComponentDescriptorByClazz(String componentDescriptorClazz) {
        try {
            ResponseEntity<ComponentDescriptor> componentDescriptor = restTemplate.getForEntity(baseURL + "/api/component/{componentDescriptorClazz}", ComponentDescriptor.class, componentDescriptorClazz);
            return Optional.ofNullable(componentDescriptor.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `componentType`：类型。
     * 返回：匹配的数据集合。
     */
    public List<ComponentDescriptor> getComponentDescriptorsByType(ComponentType componentType) {
        return getComponentDescriptorsByType(componentType, RuleChainType.CORE);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `componentType`：类型。
     * - `ruleChainType`：类型。
     * 返回：匹配的数据集合。
     */
    public List<ComponentDescriptor> getComponentDescriptorsByType(ComponentType componentType, RuleChainType ruleChainType) {
        return restTemplate.exchange(
                baseURL + "/api/components/" + componentType.name() + "/?ruleChainType={ruleChainType}",
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ComponentDescriptor>>() {
                },
                ruleChainType).getBody();
    }

    /**
     * 功能：获取`Component Descriptors By Types`。
     * 参数：
     * - `componentTypes`：类型。
     * 返回：匹配的数据集合。
     */
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(List<ComponentType> componentTypes) {
        return getComponentDescriptorsByTypes(componentTypes, RuleChainType.CORE);
    }

    /**
     * 功能：获取`Component Descriptors By Types`。
     * 参数：
     * - `componentTypes`：类型。
     * - `ruleChainType`：类型。
     * 返回：匹配的数据集合。
     */
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(List<ComponentType> componentTypes, RuleChainType ruleChainType) {
        return restTemplate.exchange(
                        baseURL + "/api/components?componentTypes={componentTypes}&ruleChainType={ruleChainType}",
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<ComponentDescriptor>>() {
                        },
                        listEnumToString(componentTypes),
                        ruleChainType)
                .getBody();
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Customer> getCustomerById(CustomerId customerId) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}", Customer.class, customerId.getId());
            return Optional.ofNullable(customer.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：可能存在的结果。
     */
    public Optional<JsonNode> getShortCustomerInfoById(CustomerId customerId) {
        try {
            ResponseEntity<JsonNode> customerInfo = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}/shortInfo", JsonNode.class, customerId.getId());
            return Optional.ofNullable(customerInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：文本结果。
     */
    public String getCustomerTitleById(CustomerId customerId) {
        return restTemplate.getForObject(baseURL + "/api/customer/{customerId}/title", String.class, customerId.getId());
    }

    /**
     * 功能：保存或创建客户。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：处理结果。
     */
    public Customer saveCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    /**
     * 功能：删除或清理客户。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    public void deleteCustomer(CustomerId customerId) {
        restTemplate.delete(baseURL + "/api/customer/{customerId}", customerId.getId());
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

        ResponseEntity<PageData<Customer>> customer = restTemplate.exchange(
                baseURL + "/api/customers?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Customer>>() {
                },
                params);
        return customer.getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `customerTitle`：`customerTitle` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<Customer> getTenantCustomer(String customerTitle) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, customerTitle);
            return Optional.ofNullable(customer.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：可能存在的结果。
     */
    @Deprecated
    public Optional<Customer> findCustomer(String title) {
        Map<String, String> params = new HashMap<>();
        params.put("customerTitle", title);
        try {
            ResponseEntity<Customer> customerEntity = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, params);
            return Optional.of(customerEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建客户。
     * 参数：
     * - `customer`：`customer` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public Customer createCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    /**
     * 功能：保存或创建客户。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public Customer createCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public Long getServerTime() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/serverTime", Long.class);
    }

    /**
     * 功能：获取数量限制。
     * 参数：无。
     * 返回：数值结果。
     */
    public Long getMaxDatapointsLimit() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/maxDatapointsLimit", Long.class);
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<DashboardInfo> getDashboardInfoById(DashboardId dashboardId) {
        try {
            ResponseEntity<DashboardInfo> dashboardInfo = restTemplate.getForEntity(baseURL + "/api/dashboard/info/{dashboardId}", DashboardInfo.class, dashboardId.getId());
            return Optional.ofNullable(dashboardInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> getDashboardById(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.getForEntity(baseURL + "/api/dashboard/{dashboardId}", Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：处理结果。
     */
    public Dashboard saveDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    /**
     * 功能：删除或清理仪表盘。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    public void deleteDashboard(DashboardId dashboardId) {
        restTemplate.delete(baseURL + "/api/dashboard/{dashboardId}", dashboardId.getId());
    }

    /**
     * 功能：执行 `assignDashboardToCustomer` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> assignDashboardToCustomer(CustomerId customerId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/dashboard/{dashboardId}", null, Dashboard.class, customerId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignDashboardFromCustomer` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> unassignDashboardFromCustomer(CustomerId customerId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.exchange(baseURL + "/api/customer/{customerId}/dashboard/{dashboardId}", HttpMethod.DELETE, HttpEntity.EMPTY, Dashboard.class, customerId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：更新仪表盘。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * - `customerIds`：数据列表。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> updateDashboardCustomers(DashboardId dashboardId, List<CustomerId> customerIds) {
        Object[] customerIdArray = customerIds.stream().map(customerId -> customerId.getId().toString()).toArray();
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/dashboard/{dashboardId}/customers", customerIdArray, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * - `customerIds`：数据列表。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> addDashboardCustomers(DashboardId dashboardId, List<CustomerId> customerIds) {
        Object[] customerIdArray = customerIds.stream().map(customerId -> customerId.getId().toString()).toArray();
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/dashboard/{dashboardId}/customers/add", customerIdArray, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：删除或清理仪表盘。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * - `customerIds`：数据列表。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> removeDashboardCustomers(DashboardId dashboardId, List<CustomerId> customerIds) {
        Object[] customerIdArray = customerIds.stream().map(customerId -> customerId.getId().toString()).toArray();
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/dashboard/{dashboardId}/customers/remove", customerIdArray, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignDashboardToPublicCustomer` 对应的处理。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> assignDashboardToPublicCustomer(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/customer/public/dashboard/{dashboardId}", null, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignDashboardFromPublicCustomer` 对应的处理。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> unassignDashboardFromPublicCustomer(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.exchange(baseURL + "/api/customer/public/dashboard/{dashboardId}", HttpMethod.DELETE, HttpEntity.EMPTY, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DashboardInfo> getTenantDashboards(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/{tenantId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DashboardInfo> getTenantDashboards(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DashboardInfo> getCustomerDashboards(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public Dashboard createDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public List<DashboardInfo> findTenantDashboards() {
        try {
            ResponseEntity<PageData<DashboardInfo>> dashboards =
                    restTemplate.exchange(baseURL + "/api/tenant/dashboards?pageSize=100000", HttpMethod.GET, null, new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                    });
            return dashboards.getBody().getData();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Device> getDeviceById(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}", Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<DeviceInfo> getDeviceInfoById(DeviceId deviceId) {
        try {
            ResponseEntity<DeviceInfo> device = restTemplate.getForEntity(baseURL + "/api/device/info/{deviceId}", DeviceInfo.class, deviceId);
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Device saveDevice(Device device) {
        return saveDevice(device, null);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    public Device saveDevice(Device device, String accessToken) {
        return restTemplate.postForEntity(baseURL + "/api/device?accessToken={accessToken}", device, Device.class, accessToken).getBody();
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    public void deleteDevice(DeviceId deviceId) {
        restTemplate.delete(baseURL + "/api/device/{deviceId}", deviceId.getId());
    }

    /**
     * 功能：执行 `assignDeviceToCustomer` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Device> assignDeviceToCustomer(CustomerId customerId, DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/device/{deviceId}", null, Device.class, customerId.getId(), deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignDeviceFromCustomer` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Device> unassignDeviceFromCustomer(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.exchange(baseURL + "/api/customer/device/{deviceId}", HttpMethod.DELETE, HttpEntity.EMPTY, Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignDeviceToPublicCustomer` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Device> assignDeviceToPublicCustomer(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.postForEntity(baseURL + "/api/customer/public/device/{deviceId}", null, Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<DeviceCredentials> getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        try {
            ResponseEntity<DeviceCredentials> deviceCredentials = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}/credentials", DeviceCredentials.class, deviceId.getId());
            return Optional.ofNullable(deviceCredentials.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public DeviceCredentials saveDeviceCredentials(DeviceCredentials deviceCredentials) {
        return restTemplate.postForEntity(baseURL + "/api/device/credentials", deviceCredentials, DeviceCredentials.class).getBody();
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `credentials`：`credentials` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<Device> saveDeviceWithCredentials(Device device, DeviceCredentials credentials) {
        try {
            SaveDeviceWithCredentialsRequest request = new SaveDeviceWithCredentialsRequest(device, credentials);
            ResponseEntity<Device> deviceOpt = restTemplate.postForEntity(baseURL + "/api/device-with-credentials", request, Device.class);
            return Optional.ofNullable(deviceOpt.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Device> getTenantDevices(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `type`：类型。
     * - `deviceProfileId`：设备配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DeviceInfo> getTenantDeviceInfos(String type, DeviceProfileId deviceProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("deviceProfileId", deviceProfileId != null ? deviceProfileId.toString() : null);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/deviceInfos?type={type}&deviceProfileId={deviceProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：可能存在的结果。
     */
    public Optional<Device> getTenantDevice(String deviceName) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, deviceName);
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `deviceType`：设备信息或设备标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Device> getCustomerDevices(CustomerId customerId, String deviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", deviceType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `deviceType`：设备信息或设备标识。
     * - `deviceProfileId`：设备配置ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DeviceInfo> getCustomerDeviceInfos(CustomerId customerId, String deviceType, DeviceProfileId deviceProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.toString());
        params.put("type", deviceType);
        params.put("deviceProfileId", deviceProfileId != null ? deviceProfileId.toString() : null);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/devices?type={type}&deviceProfileId={deviceProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取`Devices By Ids`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    public List<Device> getDevicesByIds(List<DeviceId> deviceIds) {
        return restTemplate.exchange(baseURL + "/api/devices?deviceIds={deviceIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Device>>() {
                }, listIdsToString(deviceIds)).getBody();
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public List<Device> findByQuery(DeviceSearchQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/devices",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Device>>() {
                }).getBody();
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.2")
    public List<EntitySubtype> getDeviceTypes() {
        return restTemplate.exchange(
                baseURL + "/api/device/types",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    public List<EntitySubtype> getDeviceProfileNames(boolean activeOnly) {
        return restTemplate.exchange(
                baseURL + "/api/deviceProfile/names?activeOnly={activeOnly}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }, activeOnly).getBody();
    }

    /**
     * 功能：执行 `claimDevice` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `claimRequest`：请求对象。
     * 返回：处理结果。
     */
    public JsonNode claimDevice(String deviceName, ClaimRequest claimRequest) {
        return restTemplate.exchange(
                baseURL + "/api/customer/device/{deviceName}/claim",
                HttpMethod.POST,
                new HttpEntity<>(claimRequest),
                new ParameterizedTypeReference<JsonNode>() {
                }, deviceName).getBody();
    }

    /**
     * 功能：执行 `reClaimDevice` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void reClaimDevice(String deviceName) {
        restTemplate.delete(baseURL + "/api/customer/device/{deviceName}/claim", deviceName);
    }

    /**
     * 功能：执行 `assignDeviceToTenant` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    public Device assignDeviceToTenant(TenantId tenantId, DeviceId deviceId) {
        return restTemplate.postForEntity(
                baseURL + "/api/tenant/{tenantId}/device/{deviceId}",
                HttpEntity.EMPTY, Device.class, tenantId, deviceId).getBody();
    }

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `otaPackageType`：类型。
     * - `deviceProfileId`：设备配置ID。
     * 返回：数值结果。
     */
    public Long countByDeviceProfileAndEmptyOtaPackage(OtaPackageType otaPackageType, DeviceProfileId deviceProfileId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageType", otaPackageType.name());
        params.put("deviceProfileId", deviceProfileId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/devices/count/{otaPackageType}/{deviceProfileId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Long>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：处理`Devices Bulk Import`。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public BulkImportResult<Device> processDevicesBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/device/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Device>>() {
                }).getBody();
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * 返回：处理结果。
     */
    @Deprecated
    public Device createDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doCreateDevice(device, null);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Deprecated
    public Device createDevice(Device device) {
        return doCreateDevice(device, null);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public Device createDevice(Device device, String accessToken) {
        return doCreateDevice(device, accessToken);
    }

    /**
     * 功能：执行 `doCreateDevice` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    private Device doCreateDevice(Device device, String accessToken) {
        Map<String, String> params = new HashMap<>();
        String deviceCreationUrl = "/api/device";
        if (!StringUtils.isEmpty(accessToken)) {
            deviceCreationUrl = deviceCreationUrl + "?accessToken={accessToken}";
            params.put("accessToken", accessToken);
        }
        return restTemplate.postForEntity(baseURL + deviceCreationUrl, device, Device.class, params).getBody();
    }

    /**
     * 功能：获取凭据。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Deprecated
    public DeviceCredentials getCredentials(DeviceId id) {
        return restTemplate.getForEntity(baseURL + "/api/device/" + id.getId().toString() + "/credentials", DeviceCredentials.class).getBody();
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    @Deprecated
    public Optional<Device> findDevice(String name) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceName", name);
        try {
            ResponseEntity<Device> deviceEntity = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, params);
            return Optional.of(deviceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：更新设备凭据。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `token`：`token` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public DeviceCredentials updateDeviceCredentials(DeviceId deviceId, String token) {
        DeviceCredentials deviceCredentials = getCredentials(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(token);
        return saveDeviceCredentials(deviceCredentials);
    }

    /**
     * 功能：执行 `assignDevice` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    @Deprecated
    public Device assignDevice(CustomerId customerId, DeviceId deviceId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/device/{deviceId}", null, Device.class,
                customerId.toString(), deviceId.toString()).getBody();
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：可能存在的结果。
     */
    public Optional<DeviceProfile> getDeviceProfileById(DeviceProfileId deviceProfileId) {
        try {
            ResponseEntity<DeviceProfile> deviceProfile = restTemplate.getForEntity(baseURL + "/api/deviceProfile/{deviceProfileId}", DeviceProfile.class, deviceProfileId);
            return Optional.ofNullable(deviceProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：可能存在的结果。
     */
    public Optional<DeviceProfileInfo> getDeviceProfileInfoById(DeviceProfileId deviceProfileId) {
        try {
            ResponseEntity<DeviceProfileInfo> deviceProfileInfo = restTemplate.getForEntity(baseURL + "/api/deviceProfileInfo/{deviceProfileId}", DeviceProfileInfo.class, deviceProfileId);
            return Optional.ofNullable(deviceProfileInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public DeviceProfileInfo getDefaultDeviceProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/deviceProfileInfo/default", DeviceProfileInfo.class).getBody();
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        return restTemplate.postForEntity(baseURL + "/api/deviceProfile", deviceProfile, DeviceProfile.class).getBody();
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    public void deleteDeviceProfile(DeviceProfileId deviceProfileId) {
        restTemplate.delete(baseURL + "/api/deviceProfile/{deviceProfileId}", deviceProfileId);
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    public DeviceProfile setDefaultDeviceProfile(DeviceProfileId deviceProfileId) {
        return restTemplate.postForEntity(
                baseURL + "/api/deviceProfile/{deviceProfileId}/default",
                HttpEntity.EMPTY, DeviceProfile.class, deviceProfileId).getBody();
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
        return restTemplate.exchange(
                baseURL + "/api/deviceProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceProfile>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `deviceTransportType`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    public PageData<DeviceProfileInfo> getDeviceProfileInfos(PageLink pageLink, DeviceTransportType deviceTransportType) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceTransportType", deviceTransportType != null ? deviceTransportType.name() : null);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/deviceProfileInfos?deviceTransportType={deviceTransportType}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceProfileInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    public Optional<AssetProfile> getAssetProfileById(AssetProfileId assetProfileId) {
        try {
            ResponseEntity<AssetProfile> assetProfile = restTemplate.getForEntity(baseURL + "/api/assetProfile/{assetProfileId}", AssetProfile.class, assetProfileId);
            return Optional.ofNullable(assetProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    public Optional<AssetProfileInfo> getAssetProfileInfoById(AssetProfileId assetProfileId) {
        try {
            ResponseEntity<AssetProfileInfo> assetProfileInfo = restTemplate.getForEntity(baseURL + "/api/assetProfileInfo/{assetProfileId}", AssetProfileInfo.class, assetProfileId);
            return Optional.ofNullable(assetProfileInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public AssetProfileInfo getDefaultAssetProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/assetProfileInfo/default", AssetProfileInfo.class).getBody();
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：匹配的数据集合。
     */
    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return restTemplate.postForEntity(baseURL + "/api/assetProfile", assetProfile, AssetProfile.class).getBody();
    }

    /**
     * 功能：删除或清理资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：无。
     */
    public void deleteAssetProfile(AssetProfileId assetProfileId) {
        restTemplate.delete(baseURL + "/api/assetProfile/{assetProfileId}", assetProfileId);
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    public AssetProfile setDefaultAssetProfile(AssetProfileId assetProfileId) {
        return restTemplate.postForEntity(
                baseURL + "/api/assetProfile/{assetProfileId}/default",
                HttpEntity.EMPTY, AssetProfile.class, assetProfileId).getBody();
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
        return restTemplate.exchange(
                baseURL + "/api/assetProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetProfile>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AssetProfileInfo> getAssetProfileInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/assetProfileInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetProfileInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    public Long countEntitiesByQuery(EntityCountQuery query) {
        return restTemplate.postForObject(baseURL + "/api/entitiesQuery/count", query, Long.class);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/entitiesQuery/find",
                HttpMethod.POST, new HttpEntity<>(query),
                new ParameterizedTypeReference<PageData<EntityData>>() {
                }).getBody();
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/alarmsQuery/find",
                HttpMethod.POST, new HttpEntity<>(query),
                new ParameterizedTypeReference<PageData<AlarmData>>() {
                }).getBody();
    }

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    public Long countAlarmsByQuery(AlarmCountQuery query) {
        return restTemplate.postForObject(baseURL + "/api/alarmsQuery/count", query, Long.class);
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `relation`：`relation` 参数。
     * 返回：无。
     */
    public void saveRelation(EntityRelation relation) {
        restTemplate.postForLocation(baseURL + "/api/relation", relation);
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `relationType`：类型。
     * - `relationTypeGroup`：类型。
     * - `toId`：`toId`ID。
     * 返回：无。
     */
    public void deleteRelation(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        restTemplate.delete(baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}", params);
    }

    /**
     * 功能：删除或清理`Relations`。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    public void deleteRelations(EntityId entityId) {
        restTemplate.delete(baseURL + "/api/relations?entityId={entityId}&entityType={entityType}", entityId.getId().toString(), entityId.getEntityType().name());
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `relationType`：类型。
     * - `relationTypeGroup`：类型。
     * - `toId`：`toId`ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityRelation> getRelation(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());

        try {
            ResponseEntity<EntityRelation> entityRelation = restTemplate.getForEntity(
                    baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}",
                    EntityRelation.class,
                    params);
            return Optional.ofNullable(entityRelation.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取`By From`。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelation> findByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelationInfo> findInfoByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations/info?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取`By From`。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `relationType`：类型。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelation> findByFrom(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取`By To`。
     * 参数：
     * - `toId`：`toId`ID。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelation> findByTo(EntityId toId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `toId`：`toId`ID。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelationInfo> findInfoByTo(EntityId toId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations/info?toId={toId}&toType={toType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取`By To`。
     * 参数：
     * - `toId`：`toId`ID。
     * - `relationType`：类型。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelation> findByTo(EntityId toId, String relationType, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelation> findByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelation>>() {
                }).getBody();
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public List<EntityRelationInfo> findInfoByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations/info",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                }).getBody();
    }

    /**
     * 功能：执行 `makeRelation` 对应的处理。
     * 参数：
     * - `relationType`：类型。
     * - `idFrom`：`idFrom` 参数。
     * - `idTo`：`idTo` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    public EntityRelation makeRelation(String relationType, EntityId idFrom, EntityId idTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(idFrom);
        relation.setTo(idTo);
        relation.setType(relationType);
        return restTemplate.postForEntity(baseURL + "/api/relation", relation, EntityRelation.class).getBody();
    }

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> getEntityViewById(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/entityView/{entityViewId}", EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityViewInfo> getEntityViewInfoById(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityViewInfo> entityView = restTemplate.getForEntity(baseURL + "/api/entityView/info/{entityViewId}", EntityViewInfo.class, entityViewId);
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建实体视图。
     * 参数：
     * - `entityView`：实体对象。
     * 返回：处理结果。
     */
    public EntityView saveEntityView(EntityView entityView) {
        return restTemplate.postForEntity(baseURL + "/api/entityView", entityView, EntityView.class).getBody();
    }

    /**
     * 功能：删除或清理实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：无。
     */
    public void deleteEntityView(EntityViewId entityViewId) {
        restTemplate.delete(baseURL + "/api/entityView/{entityViewId}", entityViewId.getId());
    }

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `entityViewName`：实体对象。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> getTenantEntityView(String entityViewName) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/tenant/entityViews?entityViewName={entityViewName}", EntityView.class, entityViewName);
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignEntityViewToCustomer` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> assignEntityViewToCustomer(CustomerId customerId, EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/entityView/{entityViewId}", null, EntityView.class, customerId.getId(), entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignEntityViewFromCustomer` 对应的处理。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> unassignEntityViewFromCustomer(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.exchange(baseURL + "/api/customer/entityView/{entityViewId}", HttpMethod.DELETE, HttpEntity.EMPTY, EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `entityViewType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityView> getCustomerEntityViews(CustomerId customerId, String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `customerId`：客户IDID。
     * - `entityViewType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityViewInfo> getCustomerEntityViewInfos(CustomerId customerId, String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.toString());
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/entityViewInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityViewInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `entityViewType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityView> getTenantEntityViews(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取实体视图。
     * 参数：
     * - `entityViewType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityViewInfo> getTenantEntityViewInfos(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/entityViewInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityViewInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public List<EntityView> findByQuery(EntityViewSearchQuery query) {
        return restTemplate.exchange(baseURL + "/api/entityViews", HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<List<EntityView>>() {
        }).getBody();
    }

    /**
     * 功能：获取实体视图。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<EntitySubtype> getEntityViewTypes() {
        return restTemplate.exchange(baseURL + "/api/entityView/types", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<EntitySubtype>>() {
        }).getBody();
    }

    /**
     * 功能：执行 `assignEntityViewToPublicCustomer` 对应的处理。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> assignEntityViewToPublicCustomer(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.postForEntity(baseURL + "/api/customer/public/entityView/{entityViewId}", null, EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
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
    public PageData<EventInfo> getEvents(EntityId entityId, String eventType, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("eventType", eventType);
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EventInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取`Events`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EventInfo> getEvents(EntityId entityId, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EventInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：处理结果。
     */
    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        return restTemplate.postForEntity(baseURL + "/api/oauth2/config/template", clientRegistrationTemplate, OAuth2ClientRegistrationTemplate.class).getBody();
    }

    /**
     * 功能：删除或清理客户端。
     * 参数：
     * - `oAuth2ClientRegistrationTemplateId`：客户端ID。
     * 返回：无。
     */
    public void deleteClientRegistrationTemplate(OAuth2ClientRegistrationTemplateId oAuth2ClientRegistrationTemplateId) {
        restTemplate.delete(baseURL + "/api/oauth2/config/template/{clientRegistrationTemplateId}", oAuth2ClientRegistrationTemplateId);
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<OAuth2ClientRegistrationTemplate> getClientRegistrationTemplates() {
        return restTemplate.exchange(
                baseURL + "/api/oauth2/config/template",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<OAuth2ClientRegistrationTemplate>>() {
                }).getBody();
    }

    /**
     * 功能：获取`O Auth2 Clients`。
     * 参数：
     * - `pkgName`：名称。
     * - `platformType`：类型。
     * 返回：匹配的数据集合。
     */
    public List<OAuth2ClientInfo> getOAuth2Clients(String pkgName, PlatformType platformType) {
        Map<String, String> params = new HashMap<>();
        StringBuilder urlBuilder = new StringBuilder(baseURL);
        urlBuilder.append("/api/noauth/oauth2Clients");
        if (pkgName != null) {
            urlBuilder.append("?pkgName={pkgName}");
            params.put("pkgName", pkgName);
        }
        if (platformType != null) {
            if (pkgName != null) {
                urlBuilder.append("&");
            } else {
                urlBuilder.append("?");
            }
            urlBuilder.append("platform={platform}");
            params.put("platform", platformType.name());
        }
        return restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<OAuth2ClientInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    public OAuth2Info getCurrentOAuth2Info() {
        return restTemplate.getForEntity(baseURL + "/api/oauth2/config", OAuth2Info.class).getBody();
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `oauth2Info`：`oauth2Info` 参数。
     * 返回：处理结果。
     */
    public OAuth2Info saveOAuth2Info(OAuth2Info oauth2Info) {
        return restTemplate.postForEntity(baseURL + "/api/oauth2/config", oauth2Info, OAuth2Info.class).getBody();
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getLoginProcessingUrl() {
        return restTemplate.getForEntity(baseURL + "/api/oauth2/loginProcessingUrl", String.class).getBody();
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `requestBody`：请求对象。
     * 返回：无。
     */
    public void handleOneWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        restTemplate.postForLocation(baseURL + "/api/rpc/oneway/{deviceId}", requestBody, deviceId.getId());
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `requestBody`：请求对象。
     * 返回：处理结果。
     */
    public JsonNode handleTwoWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rpc/twoway/{deviceId}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                deviceId.getId()).getBody();
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> getRuleChainById(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}", RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChainMetaData> getRuleChainMetaData(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChainMetaData> ruleChainMetaData = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}/metadata", RuleChainMetaData.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChainMetaData.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain", ruleChain, RuleChain.class).getBody();
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public RuleChain saveRuleChain(DefaultRuleChainCreateRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/device/default", request, RuleChain.class).getBody();
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> setRootRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/root", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChainMetaData`：待处理数据。
     * 返回：处理结果。
     */
    public RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMetaData) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class).getBody();
    }

    /**
     * 功能：获取`Rule Chains`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        return getRuleChains(RuleChainType.CORE, pageLink);
    }

    /**
     * 功能：获取`Rule Chains`。
     * 参数：
     * - `ruleChainType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<RuleChain> getRuleChains(RuleChainType ruleChainType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", ruleChainType.name());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/ruleChains?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                },
                params).getBody();
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    public void deleteRuleChain(RuleChainId ruleChainId) {
        restTemplate.delete(baseURL + "/api/ruleChain/{ruleChainId}", ruleChainId.getId());
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：可能存在的结果。
     */
    public Optional<JsonNode> getLatestRuleNodeDebugInput(RuleNodeId ruleNodeId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/ruleNode/{ruleNodeId}/debugIn", JsonNode.class, ruleNodeId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：验证`Script`相关场景。
     * 参数：
     * - `inputParams`：`inputParams` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<JsonNode> testScript(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/ruleChain/testScript", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `exportRuleChains` 对应的处理。
     * 参数：
     * - `limit`：数量限制。
     * 返回：处理结果。
     */
    public RuleChainData exportRuleChains(int limit) {
        return restTemplate.getForEntity(baseURL + "/api/ruleChains/export?limit=" + limit, RuleChainData.class).getBody();
    }

    /**
     * 功能：执行 `importRuleChains` 对应的处理。
     * 参数：
     * - `ruleChainData`：待处理数据。
     * - `overwrite`：`overwrite` 参数。
     * 返回：无。
     */
    public void importRuleChains(RuleChainData ruleChainData, boolean overwrite) {
        restTemplate.postForLocation(baseURL + "/api/ruleChains/import?overwrite=" + overwrite, ruleChainData);
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    public List<String> getAttributeKeys(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString()).getBody();
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    public List<String> getAttributeKeysByScope(EntityId entityId, String scope) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes/{scope}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                scope).getBody();
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    public List<AttributeKvEntry> getAttributeKvEntries(EntityId entityId, List<String> keys) {
        List<JsonNode> attributes = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes?keys={keys}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<JsonNode>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId(),
                listToString(keys)).getBody();

        return RestJsonConverter.toAttributes(attributes);
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    public Future<List<AttributeKvEntry>> getAttributeKvEntriesAsync(EntityId entityId, List<String> keys) {
        return service.submit(() -> getAttributeKvEntries(entityId, keys));
    }

    /**
     * 功能：获取`Attributes By Scope`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    public List<AttributeKvEntry> getAttributesByScope(EntityId entityId, String scope, List<String> keys) {
        List<JsonNode> attributes = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes/{scope}?keys={keys}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<JsonNode>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                scope,
                listToString(keys)).getBody();

        return RestJsonConverter.toAttributes(attributes);
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    public List<String> getTimeseriesKeys(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/timeseries",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString()).getBody();
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys) {
        return getLatestTimeseries(entityId, keys, true);
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `useStrictDataTypes`：待处理数据。
     * 返回：匹配的数据集合。
     */
    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys, boolean useStrictDataTypes) {
        Map<String, List<JsonNode>> timeseries = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&useStrictDataTypes={useStrictDataTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, List<JsonNode>>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                listToString(keys),
                useStrictDataTypes).getBody();

        return RestJsonConverter.toTimeseries(timeseries);
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `interval`：`interval` 参数。
     * - `agg`：`agg` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink) {
        return getTimeseries(entityId, keys, interval, agg, pageLink, true);
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `interval`：`interval` 参数。
     * - `agg`：`agg` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Deprecated
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink, boolean useStrictDataTypes) {
        SortOrder sortOrder = pageLink.getSortOrder();
        return getTimeseries(entityId, keys, interval, agg, sortOrder != null ? sortOrder.getDirection() : null, pageLink.getStartTime(), pageLink.getEndTime(), 100, useStrictDataTypes);
    }

    /**
     * 功能：获取时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `interval`：`interval` 参数。
     * - `agg`：`agg` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, SortOrder.Direction sortOrder, Long startTime, Long endTime, Integer limit, boolean useStrictDataTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));
        params.put("interval", interval == null ? "0" : interval.toString());
        params.put("agg", agg == null ? "NONE" : agg.name());
        params.put("limit", limit != null ? limit.toString() : "100");
        params.put("orderBy", sortOrder != null ? sortOrder.name() : "DESC");
        params.put("useStrictDataTypes", Boolean.toString(useStrictDataTypes));

        StringBuilder urlBuilder = new StringBuilder(baseURL);
        urlBuilder.append("/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&interval={interval}&limit={limit}&agg={agg}&useStrictDataTypes={useStrictDataTypes}&orderBy={orderBy}");

        if (startTime != null) {
            urlBuilder.append("&startTs={startTs}");
            params.put("startTs", String.valueOf(startTime));
        }
        if (endTime != null) {
            urlBuilder.append("&endTs={endTs}");
            params.put("endTs", String.valueOf(endTime));
        }

        Map<String, List<JsonNode>> timeseries = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, List<JsonNode>>>() {
                },
                params).getBody();

        return RestJsonConverter.toTimeseries(timeseries);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    public boolean saveDeviceAttributes(DeviceId deviceId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(baseURL + "/api/plugins/telemetry/{deviceId}/{scope}", request, Object.class, deviceId.getId().toString(), scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    public boolean saveEntityAttributesV1(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    public boolean saveEntityAttributesV2(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    public boolean saveEntityTelemetry(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `ttl`：`ttl` 参数。
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    public boolean saveEntityTelemetryWithTTL(EntityId entityId, String scope, Long ttl, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}/{ttl}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope,
                        ttl)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `deleteAllDataForKeys`：待处理数据。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    public boolean deleteEntityTimeseries(EntityId entityId,
                                          List<String> keys,
                                          boolean deleteAllDataForKeys,
                                          Long startTs,
                                          Long endTs,
                                          boolean rewriteLatestIfDeleted,
                                          boolean deleteLatest) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));
        params.put("deleteAllDataForKeys", String.valueOf(deleteAllDataForKeys));
        params.put("startTs", startTs.toString());
        params.put("endTs", endTs.toString());
        params.put("rewriteLatestIfDeleted", String.valueOf(rewriteLatestIfDeleted));
        params.put("deleteLatest", String.valueOf(deleteLatest));

        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/delete?keys={keys}&deleteAllDataForKeys={deleteAllDataForKeys}&startTs={startTs}&endTs={endTs}&rewriteLatestIfDeleted={rewriteLatestIfDeleted}&deleteLatest={deleteLatest}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        params)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：判断结果。
     */
    public boolean deleteEntityLatestTimeseries(EntityId entityId, List<String> keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));

        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/latest/delete?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        params)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：判断结果。
     */
    public boolean deleteEntityAttributes(DeviceId deviceId, String scope, List<String> keys) {
        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{deviceId}/{scope}?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        deviceId.getId().toString(),
                        scope,
                        listToString(keys))
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：判断结果。
     */
    public boolean deleteEntityAttributes(EntityId entityId, String scope, List<String> keys) {
        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/{scope}?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope,
                        listToString(keys))
                .getStatusCode()
                .is2xxSuccessful();

    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Tenant> getTenantById(TenantId tenantId) {
        try {
            ResponseEntity<Tenant> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/{tenantId}", Tenant.class, tenantId.getId());
            return Optional.ofNullable(tenant.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：可能存在的结果。
     */
    public Optional<TenantInfo> getTenantInfoById(TenantId tenantId) {
        try {
            ResponseEntity<TenantInfo> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/info/{tenantId}", TenantInfo.class, tenantId);
            return Optional.ofNullable(tenant.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * 返回：处理结果。
     */
    public Tenant saveTenant(Tenant tenant) {
        return restTemplate.postForEntity(baseURL + "/api/tenant", tenant, Tenant.class).getBody();
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void deleteTenant(TenantId tenantId) {
        restTemplate.delete(baseURL + "/api/tenant/{tenantId}", tenantId.getId());
    }

    /**
     * 功能：获取`Tenants`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Tenant> getTenants(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenants?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Tenant>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<TenantInfo> getTenantInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenantInfos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TenantInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    public UsageInfo getUsageInfo() {
        return restTemplate.exchange(
                baseURL + "/api/usage",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                UsageInfo.class).getBody();
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：可能存在的结果。
     */
    public Optional<TenantProfile> getTenantProfileById(TenantProfileId tenantProfileId) {
        try {
            ResponseEntity<TenantProfile> tenantProfile = restTemplate.getForEntity(baseURL + "/api/tenantProfile/{tenantProfileId}", TenantProfile.class, tenantProfileId);
            return Optional.ofNullable(tenantProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityInfo> getTenantProfileInfoById(TenantProfileId tenantProfileId) {
        try {
            ResponseEntity<EntityInfo> entityInfo = restTemplate.getForEntity(baseURL + "/api/tenantProfileInfo/{tenantProfileId}", EntityInfo.class, tenantProfileId);
            return Optional.ofNullable(entityInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    public EntityInfo getDefaultTenantProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/tenantProfileInfo/default", EntityInfo.class).getBody();
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：处理结果。
     */
    public TenantProfile saveTenantProfile(TenantProfile tenantProfile) {
        return restTemplate.postForEntity(baseURL + "/api/tenantProfile", tenantProfile, TenantProfile.class).getBody();
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：无。
     */
    public void deleteTenantProfile(TenantProfileId tenantProfileId) {
        restTemplate.delete(baseURL + "/api/tenantProfile/{tenantProfileId}", tenantProfileId);
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    public TenantProfile setDefaultTenantProfile(TenantProfileId tenantProfileId) {
        return restTemplate.exchange(baseURL + "/api/tenantProfile/{tenantProfileId}/default", HttpMethod.POST, HttpEntity.EMPTY, TenantProfile.class, tenantProfileId).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<TenantProfile> getTenantProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenantProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TenantProfile>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityInfo> getTenantProfileInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenantProfileInfos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：可能存在的结果。
     */
    public Optional<User> getUserById(UserId userId) {
        try {
            ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/user/{userId}", User.class, userId.getId());
            return Optional.ofNullable(user.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：判断用户。
     * 参数：无。
     * 返回：判断结果。
     */
    public Boolean isUserTokenAccessEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/user/tokenAccessEnabled", Boolean.class).getBody();
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：可能存在的结果。
     */
    public Optional<JsonNode> getUserToken(UserId userId) {
        try {
            ResponseEntity<JsonNode> userToken = restTemplate.getForEntity(baseURL + "/api/user/{userId}/token", JsonNode.class, userId.getId());
            return Optional.ofNullable(userToken.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `user`：`user` 参数。
     * - `sendActivationMail`：`sendActivationMail` 参数。
     * 返回：处理结果。
     */
    public User saveUser(User user, boolean sendActivationMail) {
        return restTemplate.postForEntity(baseURL + "/api/user?sendActivationMail={sendActivationMail}", user, User.class, sendActivationMail).getBody();
    }

    /**
     * 功能：发送或提交邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    public void sendActivationEmail(String email) {
        restTemplate.postForLocation(baseURL + "/api/user/sendActivationMail?email={email}", null, email);
    }

    /**
     * 功能：获取`Activation Link`。
     * 参数：
     * - `userId`：用户ID。
     * 返回：文本结果。
     */
    public String getActivationLink(UserId userId) {
        return restTemplate.getForEntity(baseURL + "/api/user/{userId}/activationLink", String.class, userId.getId()).getBody();
    }

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：无。
     */
    public void deleteUser(UserId userId) {
        restTemplate.delete(baseURL + "/api/user/{userId}", userId.getId());
    }

    /**
     * 功能：获取`Users`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<User> getUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<User> getTenantAdmins(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/tenant/{tenantId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<User> getCustomerUsers(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取`Users For Assign`。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<UserEmailInfo> getUsersForAssign(AlarmId alarmId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("alarmId", alarmId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/users/assign/{alarmId}" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<UserEmailInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：更新用户。
     * 参数：
     * - `userId`：用户ID。
     * - `userCredentialsEnabled`：`userCredentialsEnabled` 参数。
     * 返回：无。
     */
    public void setUserCredentialsEnabled(UserId userId, boolean userCredentialsEnabled) {
        restTemplate.postForLocation(
                baseURL + "/api/user/{userId}/userCredentialsEnabled?userCredentialsEnabled={userCredentialsEnabled}",
                null,
                userId.getId(),
                userCredentialsEnabled);
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：可能存在的结果。
     */
    public Optional<WidgetsBundle> getWidgetsBundleById(WidgetsBundleId widgetsBundleId) {
        try {
            ResponseEntity<WidgetsBundle> widgetsBundle =
                    restTemplate.getForEntity(baseURL + "/api/widgetsBundle/{widgetsBundleId}", WidgetsBundle.class, widgetsBundleId.getId());
            return Optional.ofNullable(widgetsBundle.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：保存或创建部件包。
     * 参数：
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：处理结果。
     */
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        return restTemplate.postForEntity(baseURL + "/api/widgetsBundle", widgetsBundle, WidgetsBundle.class).getBody();
    }

    /**
     * 功能：更新部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeIds`：类型。
     * 返回：无。
     */
    public void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds) {
        var httpEntity = new HttpEntity<>(widgetTypeIds.stream()
                .map(widgetTypeId -> widgetTypeId.getId().toString())
                .collect(Collectors.toList()));
        restTemplate.exchange(baseURL + "/api/widgetsBundle/{widgetsBundleId}/widgetTypes",
                HttpMethod.POST, httpEntity, Void.class, widgetsBundleId.getId());
    }

    /**
     * 功能：更新部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeFqns`：类型。
     * 返回：无。
     */
    public void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetTypeFqns) {
        restTemplate.exchange(baseURL + "/api/widgetsBundle/{widgetsBundleId}/widgetTypeFqns",
                HttpMethod.POST, new HttpEntity<>(widgetTypeFqns), Void.class, widgetsBundleId.getId());
    }

    /**
     * 功能：删除或清理部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：无。
     */
    public void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) {
        restTemplate.delete(baseURL + "/api/widgetsBundle/{widgetsBundleId}", widgetsBundleId.getId());
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink) {
        return getWidgetsBundles(pageLink, null, null);
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink, Boolean tenantOnly, Boolean fullSearch) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        addTenantOnlyAndFullSearchToParams(tenantOnly, fullSearch, params);
        return restTemplate.exchange(
                baseURL + "/api/widgetsBundles?" + getUrlParams(pageLink) + getTenantOnlyAndFullSearchUrlParams(tenantOnly, fullSearch),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetsBundle>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<WidgetsBundle> getWidgetsBundles() {
        return restTemplate.exchange(
                baseURL + "/api/widgetsBundles",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetsBundle>>() {
                }).getBody();
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：可能存在的结果。
     */
    public Optional<WidgetTypeDetails> getWidgetTypeById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetTypeDetails> widgetTypeDetails =
                    restTemplate.getForEntity(baseURL + "/api/widgetType/{widgetTypeId}", WidgetTypeDetails.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetTypeDetails.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：可能存在的结果。
     */
    public Optional<WidgetTypeInfo> getWidgetTypeInfoById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetTypeInfo> widgetTypeInfo =
                    restTemplate.getForEntity(baseURL + "/api/widgetTypeInfo/{widgetTypeId}", WidgetTypeInfo.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetTypeInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * 功能：保存或创建部件类型。
     * 参数：
     * - `widgetTypeDetails`：类型。
     * 返回：处理结果。
     */
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        return saveWidgetType(widgetTypeDetails, null);
    }

    /**
     * 功能：保存或创建部件类型。
     * 参数：
     * - `widgetTypeDetails`：类型。
     * - `updateExistingByFqn`：`updateExistingByFqn` 参数。
     * 返回：处理结果。
     */
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails, Boolean updateExistingByFqn) {
        if (updateExistingByFqn == null) {
            return restTemplate.postForEntity(baseURL + "/api/widgetType", widgetTypeDetails, WidgetTypeDetails.class).getBody();
        }
        return restTemplate.postForEntity(baseURL + "/api/widgetType?updateExistingByFqn={updateExistingByFqn}", widgetTypeDetails, WidgetTypeDetails.class, updateExistingByFqn).getBody();
    }

    /**
     * 功能：删除或清理部件类型。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：无。
     */
    public void deleteWidgetType(WidgetTypeId widgetTypeId) {
        restTemplate.delete(baseURL + "/api/widgetType/{widgetTypeId}", widgetTypeId.getId());
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<WidgetTypeInfo> getWidgetTypes(PageLink pageLink) {
        return getWidgetTypes(pageLink, null, null, null, null);
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    public PageData<WidgetTypeInfo> getWidgetTypes(PageLink pageLink, Boolean tenantOnly, Boolean fullSearch,
                                                   DeprecatedFilter deprecatedFilter, List<String> widgetTypeList) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        addWidgetInfoFiltersToParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList, params);
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?" + getUrlParams(pageLink) +
                        getWidgetTypeInfoPageRequestUrlParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetTypeInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `isSystem`：`isSystem` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * 返回：匹配的数据集合。
     */
    @Deprecated // current name in the controller: getBundleWidgetTypesByBundleAlias
    public List<WidgetType> getBundleWidgetTypes(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetType>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    public List<WidgetType> getBundleWidgetTypes(WidgetsBundleId widgetsBundleId) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetType>>() {
                },
                widgetsBundleId.getId()).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `isSystem`：`isSystem` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * 返回：匹配的数据集合。
     */
    @Deprecated // current name in the controller: getBundleWidgetTypesDetailsByBundleAlias
    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesDetails?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetTypeDetails>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `inlineImages`：`inlineImages` 参数。
     * 返回：匹配的数据集合。
     */
    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(WidgetsBundleId widgetsBundleId, boolean inlineImages) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesDetails?widgetsBundleId={widgetsBundleId}&inlineImages={inlineImages}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetTypeDetails>>() {
                },
                widgetsBundleId.getId(),
                inlineImages).getBody();
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    public List<String> getBundleWidgetTypeFqns(WidgetsBundleId widgetsBundleId) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypeFqns?widgetsBundleId={widgetsBundleId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                widgetsBundleId.getId()).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `isSystem`：`isSystem` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * 返回：匹配的数据集合。
     */
    @Deprecated // current name in the controller: getBundleWidgetTypesInfosByBundleAlias
    public List<WidgetTypeInfo> getBundleWidgetTypesInfos(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesInfos?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetTypeInfo>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(WidgetsBundleId widgetsBundleId, PageLink pageLink) {
        return getBundleWidgetTypesInfos(widgetsBundleId, pageLink, null, null, null, null);
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `pageLink`：`pageLink` 参数。
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(WidgetsBundleId widgetsBundleId, PageLink pageLink,
                                                              Boolean tenantOnly, Boolean fullSearch,
                                                              DeprecatedFilter deprecatedFilter, List<String> widgetTypeList) {
        Map<String, String> params = new HashMap<>();
        params.put("widgetsBundleId", widgetsBundleId.getId().toString());
        addPageLinkToParam(params, pageLink);
        addWidgetInfoFiltersToParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList, params);
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesInfos?widgetsBundleId={widgetsBundleId}&" + getUrlParams(pageLink) +
                        getWidgetTypeInfoPageRequestUrlParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetTypeInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `isSystem`：`isSystem` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * - `alias`：`alias` 参数。
     * 返回：可能存在的结果。
     */
    @Deprecated // current name in the controller: getWidgetTypeByBundleAliasAndTypeAlias
    public Optional<WidgetType> getWidgetType(boolean isSystem, String bundleAlias, String alias) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(
                            baseURL + "/api/widgetType?isSystem={isSystem}&bundleAlias={bundleAlias}&alias={alias}",
                            WidgetType.class,
                            isSystem,
                            bundleAlias,
                            alias);
            return Optional.ofNullable(widgetType.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `fqn`：`fqn` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<WidgetType> getWidgetType(String fqn) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(
                            baseURL + "/api/widgetType?fqn={fqn}",
                            WidgetType.class,
                            fqn);
            return Optional.ofNullable(widgetType.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * 功能：判断`Edges Support Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public Boolean isEdgesSupportEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/edges/enabled", Boolean.class).getBody();
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：处理结果。
     */
    public Edge saveEdge(Edge edge) {
        return restTemplate.postForEntity(baseURL + "/api/edge", edge, Edge.class).getBody();
    }

    /**
     * 功能：删除或清理边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    public void deleteEdge(EdgeId edgeId) {
        restTemplate.delete(baseURL + "/api/edge/{edgeId}", edgeId.getId());
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：可能存在的结果。
     */
    public Optional<Edge> getEdgeById(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.getForEntity(baseURL + "/api/edge/{edgeId}", Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：可能存在的结果。
     */
    public Optional<EdgeInfo> getEdgeInfoById(EdgeId edgeId) {
        try {
            ResponseEntity<EdgeInfo> edge = restTemplate.getForEntity(baseURL + "/api/edge/info/{edgeId}", EdgeInfo.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignEdgeToCustomer` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：可能存在的结果。
     */
    public Optional<Edge> assignEdgeToCustomer(CustomerId customerId, EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/edge/{edgeId}", null, Edge.class, customerId.getId(), edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignEdgeToPublicCustomer` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：可能存在的结果。
     */
    public Optional<Edge> assignEdgeToPublicCustomer(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.postForEntity(baseURL + "/api/customer/public/edge/{edgeId}", null, Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<Edge> setEdgeRootRuleChain(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<Edge> ruleChain = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/{ruleChainId}/root", null, Edge.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取`Edges`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Edge> getEdges(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edges?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    /**
     * 功能：执行 `unassignEdgeFromCustomer` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：可能存在的结果。
     */
    public Optional<Edge> unassignEdgeFromCustomer(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.exchange(baseURL + "/api/customer/edge/{edgeId}", HttpMethod.DELETE, HttpEntity.EMPTY, Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `assignDeviceToEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Device> assignDeviceToEdge(EdgeId edgeId, DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/device/{deviceId}", null, Device.class, edgeId.getId(), deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignDeviceFromEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `deviceId`：设备IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Device> unassignDeviceFromEdge(EdgeId edgeId, DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/device/{deviceId}", HttpMethod.DELETE, HttpEntity.EMPTY, Device.class, edgeId.getId(), deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Device> getEdgeDevices(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/devices?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    /**
     * 功能：执行 `assignAssetToEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> assignAssetToEdge(EdgeId edgeId, AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/asset/{assetId}", null, Asset.class, edgeId.getId(), assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignAssetFromEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    public Optional<Asset> unassignAssetFromEdge(EdgeId edgeId, AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/asset/{assetId}", HttpMethod.DELETE, HttpEntity.EMPTY, Asset.class, edgeId.getId(), assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Asset> getEdgeAssets(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/assets?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                }, params).getBody();
    }

    /**
     * 功能：执行 `assignDashboardToEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> assignDashboardToEdge(EdgeId edgeId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/dashboard/{dashboardId}", null, Dashboard.class, edgeId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignDashboardFromEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：可能存在的结果。
     */
    public Optional<Dashboard> unassignDashboardFromEdge(EdgeId edgeId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/dashboard/{dashboardId}", HttpMethod.DELETE, HttpEntity.EMPTY, Dashboard.class, edgeId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<DashboardInfo> getEdgeDashboards(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：执行 `assignEntityViewToEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> assignEntityViewToEdge(EdgeId edgeId, EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/entityView/{entityViewId}", null, EntityView.class, edgeId.getId(), entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignEntityViewFromEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `entityViewId`：实体视图ID。
     * 返回：可能存在的结果。
     */
    public Optional<EntityView> unassignEntityViewFromEdge(EdgeId edgeId, EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/entityView/{entityViewId}",
                    HttpMethod.DELETE, HttpEntity.EMPTY, EntityView.class, edgeId.getId(), entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityView> getEdgeEntityViews(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/entityViews?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    /**
     * 功能：执行 `assignRuleChainToEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> assignRuleChainToEdge(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/ruleChain/{ruleChainId}", null, RuleChain.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unassignRuleChainFromEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> unassignRuleChainFromEdge(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/ruleChain/{ruleChainId}", HttpMethod.DELETE, HttpEntity.EMPTY, RuleChain.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<RuleChain> getEdgeRuleChains(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/ruleChains?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                }, params).getBody();
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> setAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/autoAssignToEdge", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：执行 `unsetAutoAssignToEdgeRuleChain` 对应的处理。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> unsetAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.exchange(baseURL + "/api/ruleChain/{ruleChainId}/autoAssignToEdge", HttpMethod.DELETE, HttpEntity.EMPTY, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<RuleChain> getAutoAssignToEdgeRuleChains() {
        return restTemplate.exchange(baseURL + "/api/ruleChain/autoAssignToEdgeRuleChains",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<RuleChain>>() {
                }).getBody();
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：可能存在的结果。
     */
    public Optional<RuleChain> setRootEdgeTemplateRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/edgeTemplateRoot", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Edge> getTenantEdges(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EdgeInfo> getTenantEdgeInfos(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/edgeInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `edgeName`：名称。
     * 返回：可能存在的结果。
     */
    public Optional<Edge> getTenantEdge(String edgeName) {
        try {
            ResponseEntity<Edge> edge = restTemplate.getForEntity(baseURL + "/api/tenant/edges?edgeName={edgeName}", Edge.class, edgeName);
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `edgeType`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<Edge> getCustomerEdges(CustomerId customerId, PageLink pageLink, String edgeType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", edgeType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `edgeType`：类型。
     * 返回：匹配的数据集合。
     */
    public PageData<EdgeInfo> getCustomerEdgeInfos(CustomerId customerId, PageLink pageLink, String edgeType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", edgeType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/edgeInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeInfo>>() {
                }, params).getBody();
    }

    /**
     * 功能：获取`Edges By Ids`。
     * 参数：
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    public List<Edge> getEdgesByIds(List<EdgeId> edgeIds) {
        return restTemplate.exchange(baseURL + "/api/edges?edgeIds={edgeIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Edge>>() {
                }, listIdsToString(edgeIds)).getBody();
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    public List<Edge> findByQuery(EdgeSearchQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/edges",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Edge>>() {
                }).getBody();
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<EntitySubtype> getEdgeTypes() {
        return restTemplate.exchange(
                baseURL + "/api/edge/types",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EdgeEvent> getEdgeEvents(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/events?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeEvent>>() {
                },
                params).getBody();
    }

    /**
     * 功能：执行 `syncEdge` 对应的处理。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    public void syncEdge(EdgeId edgeId) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.toString());
        restTemplate.postForEntity(baseURL + "/api/edge/sync/{edgeId}", null, EdgeId.class, params);
    }

    /**
     * 功能：获取`Missing To Related Rule Chains`。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：文本结果。
     */
    public String findMissingToRelatedRuleChains(EdgeId edgeId) {
        return restTemplate.getForEntity(baseURL + "/api/edge/missingToRelatedRuleChains/{edgeId}", String.class, edgeId.getId()).getBody();
    }

    /**
     * 功能：处理`Edges Bulk Import`。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public BulkImportResult<Edge> processEdgesBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/edge/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Edge>>() {
                }).getBody();
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `method`：`method` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<EdgeInstructions> getEdgeInstallInstructions(EdgeId edgeId, String method) {
        ResponseEntity<EdgeInstructions> edgeInstallInstructionsResult =
                restTemplate.getForEntity(baseURL + "/api/edge/instructions/install/{edgeId}/{method}", EdgeInstructions.class, edgeId.getId(), method);
        return Optional.ofNullable(edgeInstallInstructionsResult.getBody());
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * - `method`：`method` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<EdgeInstructions> getEdgeUpgradeInstructions(String edgeVersion, String method) {
        ResponseEntity<EdgeInstructions> edgeUpgradeInstructionsResult =
                restTemplate.getForEntity(baseURL + "/api/edge/instructions/upgrade/{edgeVersion}/{method}", EdgeInstructions.class, edgeVersion, method);
        return Optional.ofNullable(edgeUpgradeInstructionsResult.getBody());
    }

    /**
     * 功能：保存或创建版本号。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public UUID saveEntitiesVersion(VersionCreateRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/entities/vc/version", request, UUID.class).getBody();
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `requestId`：请求ID。
     * 返回：可能存在的结果。
     */
    public Optional<VersionCreationResult> getVersionCreateRequestStatus(UUID requestId) {
        try {
            ResponseEntity<VersionCreationResult> versionCreateResult = restTemplate.getForEntity(baseURL + "/api/entities/vc/version/{requestId}/status", VersionCreationResult.class, requestId);
            return Optional.ofNullable(versionCreateResult.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `externalEntityId`：实体IDID。
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityVersion> listEntityVersions(EntityId externalEntityId, String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", externalEntityId.getEntityType().name());
        params.put("externalEntityUuid", externalEntityId.getId().toString());
        params.put("branch", branch);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/version/{entityType}/{externalEntityUuid}?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityType`：实体对象。
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityVersion> listEntityTypeVersions(EntityType entityType, String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType.name());
        params.put("branch", branch);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/version/{entityType}?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<EntityVersion> listVersions(String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("branch", branch);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/version?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `entityType`：实体对象。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    public List<VersionedEntityInfo> listEntitiesAtVersion(EntityType entityType, String versionId) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType.name());
        params.put("versionId", versionId);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/entity/{entityType}/{versionId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<VersionedEntityInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    public List<VersionedEntityInfo> listAllEntitiesAtVersion(String versionId) {
        Map<String, String> params = new HashMap<>();
        params.put("versionId", versionId);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/entity/{versionId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<VersionedEntityInfo>>() {
                },
                params).getBody();
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `externalEntityId`：实体IDID。
     * - `versionId`：版本号ID。
     * 返回：处理结果。
     */
    public EntityDataInfo getEntityDataInfo(EntityId externalEntityId, String versionId) {
        return restTemplate.getForEntity(baseURL + "/api/entities/vc/info/{versionId}/{entityType}/{externalEntityUuid}",
                EntityDataInfo.class, versionId, externalEntityId.getEntityType(), externalEntityId.getId()).getBody();
    }

    /**
     * 功能：执行 `compareEntityDataToVersion` 对应的处理。
     * 参数：
     * - `internalEntityId`：实体IDID。
     * - `versionId`：版本号ID。
     * 返回：处理结果。
     */
    public EntityDataDiff compareEntityDataToVersion(EntityId internalEntityId, String versionId) {
        return restTemplate.getForEntity(baseURL + "/api/entities/vc/diff/{entityType}/{internalEntityUuid}?versionId={versionId}",
                EntityDataDiff.class, internalEntityId.getEntityType(), internalEntityId.getId(), versionId).getBody();
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public UUID loadEntitiesVersion(VersionLoadRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/entities/vc/entity", request, UUID.class).getBody();
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `requestId`：请求ID。
     * 返回：可能存在的结果。
     */
    public Optional<VersionLoadResult> getVersionLoadRequestStatus(UUID requestId) {
        try {
            ResponseEntity<VersionLoadResult> versionLoadResult = restTemplate.getForEntity(baseURL + "/api/entities/vc/entity/{requestId}/status", VersionLoadResult.class, requestId);
            return Optional.ofNullable(versionLoadResult.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 功能：获取`Branches`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<BranchInfo> listBranches() {
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/branches",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<BranchInfo>>() {
                }).getBody();
    }

    /**
     * 功能：执行 `downloadResource` 对应的处理。
     * 参数：
     * - `resourceId`：`resourceId`ID。
     * 返回：响应结果。
     */
    public ResponseEntity<Resource> downloadResource(TbResourceId resourceId) {
        Map<String, String> params = new HashMap<>();
        params.put("resourceId", resourceId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/resource/{resourceId}/download",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                params
        );
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    public TbResourceInfo getResourceInfoById(TbResourceId resourceId) {
        Map<String, String> params = new HashMap<>();
        params.put("resourceId", resourceId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/resource/info/{resourceId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TbResourceInfo>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：获取`Resource Id`。
     * 参数：
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    public TbResource getResourceId(TbResourceId resourceId) {
        Map<String, String> params = new HashMap<>();
        params.put("resourceId", resourceId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/resource/{resourceId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TbResource>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：保存或创建`Resource`。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    public TbResource saveResource(TbResource resource) {
        return restTemplate.postForEntity(
                baseURL + "/api/resource",
                resource,
                TbResource.class
        ).getBody();
    }

    /**
     * 功能：获取`Resources`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<TbResourceInfo> getResources(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/resource?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TbResourceInfo>>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：删除或清理`Resource`。
     * 参数：
     * - `resourceId`：`resourceId`ID。
     * 返回：无。
     */
    public void deleteResource(TbResourceId resourceId) {
        restTemplate.delete("/api/resource/{resourceId}", resourceId.getId().toString());
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：处理结果。
     */
    public TbResourceInfo getImageInfo(String type, String key) {
        return restTemplate.getForObject(baseURL + "/api/images/{type}/{key}/info", TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        ));
    }

    /**
     * 功能：获取`Images`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `includeSystemImages`：`includeSystemImages` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<TbResourceInfo> getImages(PageLink pageLink, boolean includeSystemImages) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        params.put("includeSystemImages", String.valueOf(includeSystemImages));
        return restTemplate.exchange(baseURL + "/api/images?includeSystemImages={includeSystemImages}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TbResourceInfo>>() {},
                params
        ).getBody();
    }

    /**
     * 功能：执行 `uploadImage` 对应的处理。
     * 参数：
     * - `fileName`：名称。
     * - `data`：待处理数据。
     * - `contentType`：类型。
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public TbResourceInfo uploadImage(String fileName, byte[] data, String contentType, String title) {
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(fileName, data, contentType, Map.of(
                "title", Strings.nullToEmpty(title)
        ));
        return restTemplate.postForObject(baseURL + "/api/image", request, TbResourceInfo.class);
    }

    /**
     * 功能：更新图片资源。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * - `fileName`：名称。
     * - `data`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public TbResourceInfo updateImage(String type, String key, String fileName, byte[] data, String contentType) {
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(fileName, data, contentType, Map.of());
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}", HttpMethod.PUT, request, TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
    }

    /**
     * 功能：更新信息对象。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    public TbResourceInfo updateImageInfo(String type, String key, TbResourceInfo request) {
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}/info", HttpMethod.PUT, new HttpEntity<>(request), TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * - `isPublic`：`isPublic` 参数。
     * 返回：无。
     */
    public void updateImagePublicStatus(String type, String key, boolean isPublic) {
        restTemplate.put(baseURL + "/api/images/{type}/{key}/public/{isPublic}", null, Map.of(
                "type", type,
                "key", key,
                "isPublic", isPublic
        ));
    }

    /**
     * 功能：执行 `downloadImage` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：处理结果。
     */
    public byte[] downloadImage(String type, String key) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/{type}/{key}", HttpMethod.GET, null, Resource.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    /**
     * 功能：执行 `downloadImagePreview` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：处理结果。
     */
    public byte[] downloadImagePreview(String type, String key) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/{type}/{key}/preview", HttpMethod.GET, null, Resource.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    /**
     * 功能：执行 `downloadPublicImage` 对应的处理。
     * 参数：
     * - `publicResourceKey`：键。
     * 返回：处理结果。
     */
    public byte[] downloadPublicImage(String publicResourceKey) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/public/{publicResourceKey}", HttpMethod.GET, null, Resource.class, Map.of(
                "publicResourceKey", publicResourceKey
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    /**
     * 功能：执行 `exportImage` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：处理结果。
     */
    public ImageExportData exportImage(String type, String key) {
        return restTemplate.getForObject(baseURL + "/api/images/{type}/{key}/export", ImageExportData.class, Map.of(
                "type", type,
                "key", key
        ));
    }

    /**
     * 功能：执行 `importImage` 对应的处理。
     * 参数：
     * - `exportData`：待处理数据。
     * 返回：处理结果。
     */
    public TbResourceInfo importImage(ImageExportData exportData) {
        return restTemplate.exchange(baseURL + "/api/image/import", HttpMethod.PUT, new HttpEntity<>(exportData), TbResourceInfo.class).getBody();
    }

    /**
     * 功能：删除或清理图片资源。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * - `force`：`force` 参数。
     * 返回：处理结果。
     */
    public TbImageDeleteResult deleteImage(String type, String key, boolean force) {
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}?force={force}", HttpMethod.DELETE, null, TbImageDeleteResult.class, Map.of(
                "type", type,
                "key", key,
                "force", force
        )).getBody();
    }

    /**
     * 功能：执行 `downloadOtaPackage` 对应的处理。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：响应结果。
     */
    public ResponseEntity<Resource> downloadOtaPackage(OtaPackageId otaPackageId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/otaPackage/{otaPackageId}/download",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                params
        );
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：处理结果。
     */
    public OtaPackageInfo getOtaPackageInfoById(OtaPackageId otaPackageId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/otaPackage/info/{otaPackageId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<OtaPackageInfo>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：获取`Ota Package By Id`。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：处理结果。
     */
    public OtaPackage getOtaPackageById(OtaPackageId otaPackageId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/otaPackage/{otaPackageId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<OtaPackage>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * - `isUrl`：`isUrl` 参数。
     * 返回：处理结果。
     */
    public OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl) {
        Map<String, String> params = new HashMap<>();
        params.put("isUrl", Boolean.toString(isUrl));
        return restTemplate.postForEntity(baseURL + "/api/otaPackage?isUrl={isUrl}", otaPackageInfo, OtaPackageInfo.class, params).getBody();
    }

    /**
     * 功能：保存或创建数据。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * - `checkSum`：`checkSum` 参数。
     * - `checksumAlgorithm`：`checksumAlgorithm` 参数。
     * - `fileName`：名称。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public OtaPackageInfo saveOtaPackageData(OtaPackageId otaPackageId, String checkSum, ChecksumAlgorithm checksumAlgorithm, String fileName, byte[] fileBytes) throws Exception {
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createMultipartRequest(fileName, fileBytes, null, Collections.emptyMap());

        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());
        params.put("checksumAlgorithm", checksumAlgorithm.name());
        String url = "/api/otaPackage/{otaPackageId}?checksumAlgorithm={checksumAlgorithm}";

        if (checkSum != null) {
            url += "&checkSum={checkSum}";
        }

        return restTemplate.postForEntity(
                baseURL + url, requestEntity, OtaPackageInfo.class, params
        ).getBody();
    }

    /**
     * 功能：获取`Ota Packages`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<OtaPackageInfo> getOtaPackages(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/otaPackages?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OtaPackageInfo>>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：获取`Ota Packages`。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * - `otaPackageType`：类型。
     * - `hasData`：待处理数据。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<OtaPackageInfo> getOtaPackages(DeviceProfileId deviceProfileId,
                                                   OtaPackageType otaPackageType,
                                                   boolean hasData,
                                                   PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("hasData", String.valueOf(hasData));
        params.put("deviceProfileId", deviceProfileId.getId().toString());
        params.put("type", otaPackageType.name());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/otaPackages/{deviceProfileId}/{type}/{hasData}?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OtaPackageInfo>>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：删除或清理`Ota Package`。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：无。
     */
    public void deleteOtaPackage(OtaPackageId otaPackageId) {
        restTemplate.delete(baseURL + "/api/otaPackage/{otaPackageId}", otaPackageId.getId().toString());
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `serviceType`：服务对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Queue> getQueuesByServiceType(String serviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("serviceType", serviceType);
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/queues?serviceType={serviceType}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Queue>>() {
                },
                params
        ).getBody();
    }

    /**
     * 功能：获取队列。
     * 参数：
     * - `queueId`：队列ID。
     * 返回：处理结果。
     */
    public Queue getQueueById(QueueId queueId) {
        return restTemplate.exchange(
                baseURL + "/api/queues/" + queueId,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Queue>() {
                }
        ).getBody();
    }

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * - `serviceType`：服务对象。
     * 返回：处理结果。
     */
    public Queue saveQueue(Queue queue, String serviceType) {
        return restTemplate.postForEntity(baseURL + "/api/queues?serviceType=" + serviceType, queue, Queue.class).getBody();
    }

    /**
     * 功能：删除或清理队列。
     * 参数：
     * - `queueId`：队列ID。
     * 返回：无。
     */
    public void deleteQueue(QueueId queueId) {
        restTemplate.delete(baseURL + "/api/queues/" + queueId);
    }

    /**
     * 功能：获取`Attributes`。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `clientKeys`：客户端对象。
     * - `sharedKeys`：键。
     * 返回：可能存在的结果。
     */
    @Deprecated
    public Optional<JsonNode> getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        Map<String, String> params = new HashMap<>();
        params.put("accessToken", accessToken);
        params.put("clientKeys", clientKeys);
        params.put("sharedKeys", sharedKeys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/v1/{accessToken}/attributes?clientKeys={clientKeys}&sharedKeys={sharedKeys}", JsonNode.class, params);
            return Optional.of(telemetryEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
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
     * 功能：获取部件类型。
     * 参数：
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypeList`：类型。
     * 返回：文本结果。
     */
    private String getWidgetTypeInfoPageRequestUrlParams(Boolean tenantOnly, Boolean fullSearch,
                                                         DeprecatedFilter deprecatedFilter,
                                                         List<String> widgetTypeList) {
        String urlParams = getTenantOnlyAndFullSearchUrlParams(tenantOnly, fullSearch);
        if (deprecatedFilter != null) {
            urlParams += "&deprecatedFilter={deprecatedFilter}";
        }
        if (!CollectionUtils.isEmpty(widgetTypeList)) {
            urlParams += "&widgetTypeList={widgetTypeList}";
        }
        return urlParams;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * 返回：文本结果。
     */
    private String getTenantOnlyAndFullSearchUrlParams(Boolean tenantOnly, Boolean fullSearch) {
        String urlParams = "";
        if (tenantOnly != null) {
            urlParams = "&tenantOnly={tenantOnly}";
        }
        if (fullSearch != null) {
            urlParams += "&fullSearch={fullSearch}";
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
     * 功能：保存或创建部件。
     * 参数：
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypeList`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void addWidgetInfoFiltersToParams(Boolean tenantOnly, Boolean fullSearch, DeprecatedFilter deprecatedFilter,
                                              List<String> widgetTypeList, Map<String, String> params) {
        addTenantOnlyAndFullSearchToParams(tenantOnly, fullSearch, params);
        if (deprecatedFilter != null) {
            params.put("deprecatedFilter", deprecatedFilter.name());
        }
        if (!CollectionUtils.isEmpty(widgetTypeList)) {
            params.put("widgetTypeList", listToString(widgetTypeList));
        }
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantOnly`：租户信息或租户标识。
     * - `fullSearch`：`fullSearch` 参数。
     * - `params`：键值映射。
     * 返回：无。
     */
    private void addTenantOnlyAndFullSearchToParams(Boolean tenantOnly, Boolean fullSearch, Map<String, String> params) {
        if (tenantOnly != null) {
            params.put("tenantOnly", tenantOnly.toString());
        }
        if (fullSearch != null) {
            params.put("fullSearch", fullSearch.toString());
        }
    }

    /**
     * 功能：获取`To String`。
     * 参数：
     * - `list`：数据列表。
     * 返回：文本结果。
     */
    private String listToString(List<String> list) {
        return String.join(",", list);
    }

    /**
     * 功能：获取`Ids To String`。
     * 参数：
     * - `list`：数据列表。
     * 返回：文本结果。
     */
    private String listIdsToString(List<? extends EntityId> list) {
        return listToString(list.stream().map(id -> id.getId().toString()).collect(Collectors.toList()));
    }

    /**
     * 功能：获取`Enum To String`。
     * 参数：
     * - `list`：数据列表。
     * 返回：文本结果。
     */
    private String listEnumToString(List<? extends Enum> list) {
        return listToString(list.stream().map(Enum::name).collect(Collectors.toList()));
    }

    /**
     * 功能：保存或创建请求。
     * 参数：
     * - `fileName`：名称。
     * - `fileData`：待处理数据。
     * - `fileContentType`：类型。
     * - `otherParts`：键值映射。
     * 返回：处理结果。
     */
    private HttpEntity<MultiValueMap<String, Object>> createMultipartRequest(String fileName, byte[] fileData, String fileContentType, Map<String, Object> otherParts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=file; filename=" + fileName);
        if (fileContentType != null) {
            fileMap.add(HttpHeaders.CONTENT_TYPE, fileContentType);
        }
        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(fileData), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.setAll(otherParts);
        body.add("file", fileEntity);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 功能：执行 `close` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void close() {
        service.shutdown();
    }

    /**
     * 本类总结：
     * 1. 核心职责：`RestClient` 负责把 ThingsBoard 服务端 REST 接口包装成 Java 方法，统一处理 JWT 登录、刷新、URL 参数、分页查询、文件上传下载和 DTO 映射。
     * 2. 核心流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 3. 关键依赖：Spring RestTemplate、JWT、common data DTO、ThingsBoard 服务端 REST Controller、租户/客户/设备/规则链/资源等业务模块。
     * 4. 设计重点：通过 Facade / Adapter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
