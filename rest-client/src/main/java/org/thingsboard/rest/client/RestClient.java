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
     * 字段说明：
     * 1. 保存内容：`JWT_TOKEN_HEADER_PARAM` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST API 客户端门面` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private static final long AVG_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";
    private final ExecutorService service = ThingsBoardExecutors.newWorkStealingPool(10, getClass());
    /**
     * 字段说明：
     * 1. 保存内容：`restTemplate` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST API 客户端门面` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    protected final RestTemplate restTemplate;
    protected final RestTemplate loginRestTemplate;
    protected final String baseURL;

    /**
     * 字段说明：
     * 1. 保存内容：`username` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST API 客户端门面` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    private String username;
    private String password;
    private String mainToken;
    /**
     * 字段说明：
     * 1. 保存内容：`refreshToken` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST API 客户端门面` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    private String refreshToken;
    private long mainTokenExpTs;
    private long refreshTokenExpTs;
    /**
     * 字段说明：
     * 1. 保存内容：`clientServerTimeDiff` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST API 客户端门面` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    private long clientServerTimeDiff;

    public RestClient(String baseURL) {
        this(new RestTemplate(), baseURL);
    }

    /**
     * 方法说明：
     * 1. 职责：`RestClient` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RestClient(RestTemplate restTemplate, String baseURL) {
        this.restTemplate = restTemplate;
        this.loginRestTemplate = new RestTemplate(restTemplate.getRequestFactory());
        this.baseURL = baseURL;
        this.restTemplate.getInterceptors().add((request, bytes, execution) -> {
            // 使用请求包装器只增强 Header，不改变原始 HTTP 方法、URI 和 body，保持 RestTemplate 调用语义。
            HttpRequest wrapper = new HttpRequestWrapper(request);
            long calculatedTs = System.currentTimeMillis() + clientServerTimeDiff + AVG_REQUEST_TIMEOUT;
            // 在发送请求前预留平均请求耗时，提前刷新 token，避免请求到达服务端时 token 已经过期。
            if (calculatedTs > mainTokenExpTs) {
                // 双重检查放在同步块内，防止多个并发请求同时刷新或重新登录导致 token 状态互相覆盖。
                synchronized (RestClient.this) {
                    // 在发送请求前预留平均请求耗时，提前刷新 token，避免请求到达服务端时 token 已经过期。
                    if (calculatedTs > mainTokenExpTs) {
                        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
                        if (calculatedTs < refreshTokenExpTs) {
                            // refresh token 仍有效时优先刷新主 token，避免频繁使用用户名密码重新登录。
                            refreshToken();
                        } else {
                            // refresh token 也接近失效时退回完整登录流程，保证后续 REST 调用仍带有有效认证头。
                            doLogin();
                        }
                    }
                }
            }
            // 每次请求都在拦截器中写入最新 JWT，避免调用方手工维护认证 Header。
            wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + mainToken);
            return execution.execute(wrapper, bytes);
        });
    }

    /**
     * 方法说明：
     * 1. 职责：`getRestTemplate` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * 方法说明：
     * 1. 职责：`getToken` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public String getToken() {
        return mainToken;
    }

    /**
     * 方法说明：
     * 1. 职责：`getRefreshToken` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // refresh token 仍有效时优先刷新主 token，避免频繁使用用户名密码重新登录。
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 方法说明：
     * 1. 职责：`refreshToken` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // refresh token 仍有效时优先刷新主 token，避免频繁使用用户名密码重新登录。
    public void refreshToken() {
        Map<String, String> refreshTokenRequest = new HashMap<>();
        refreshTokenRequest.put("refreshToken", refreshToken);
        long ts = System.currentTimeMillis();
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/token", refreshTokenRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    /**
     * 方法说明：
     * 1. 职责：`login` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void login(String username, String password) {
        this.username = username;
        this.password = password;
        // refresh token 也接近失效时退回完整登录流程，保证后续 REST 调用仍带有有效认证头。
        doLogin();
    }

    /**
     * 方法说明：
     * 1. 职责：`doLogin` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // refresh token 也接近失效时退回完整登录流程，保证后续 REST 调用仍带有有效认证头。
    private void doLogin() {
        long ts = System.currentTimeMillis();
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    /**
     * 方法说明：
     * 1. 职责：`setTokenInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private synchronized void setTokenInfo(long ts, JsonNode tokenInfo) {
        this.mainToken = tokenInfo.get("token").asText();
        this.refreshToken = tokenInfo.get("refreshToken").asText();
        this.mainTokenExpTs = JWT.decode(this.mainToken).getExpiresAtAsInstant().toEpochMilli();
        this.refreshTokenExpTs = JWT.decode(refreshToken).getExpiresAtAsInstant().toEpochMilli();
        this.clientServerTimeDiff = JWT.decode(this.mainToken).getIssuedAtAsInstant().toEpochMilli() - ts;
    }

    /**
     * 方法说明：
     * 1. 职责：`getAdminSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<AdminSettings> getAdminSettings(String key) {
        try {
            ResponseEntity<AdminSettings> adminSettings = restTemplate.getForEntity(baseURL + "/api/admin/settings/{key}", AdminSettings.class, key);
            return Optional.ofNullable(adminSettings.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveAdminSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/settings", adminSettings, AdminSettings.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`sendTestMail` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void sendTestMail(AdminSettings adminSettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testMail", adminSettings);
    }

    /**
     * 方法说明：
     * 1. 职责：`sendTestSms` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void sendTestSms(TestSmsRequest testSmsRequest) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testSms", testSmsRequest);
    }

    /**
     * 方法说明：
     * 1. 职责：`getSecuritySettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<SecuritySettings> getSecuritySettings() {
        try {
            ResponseEntity<SecuritySettings> securitySettings = restTemplate.getForEntity(baseURL + "/api/admin/securitySettings", SecuritySettings.class);
            return Optional.ofNullable(securitySettings.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveSecuritySettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/securitySettings", securitySettings, SecuritySettings.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getJwtSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JwtSettings> getJwtSettings() {
        try {
            ResponseEntity<JwtSettings> jwtSettings = restTemplate.getForEntity(baseURL + "/api/admin/jwtSettings", JwtSettings.class);
            return Optional.ofNullable(jwtSettings.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveJwtSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public JwtPair saveJwtSettings(JwtSettings jwtSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/jwtSettings", jwtSettings, JwtPair.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getRepositorySettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RepositorySettings> getRepositorySettings() {
        try {
            ResponseEntity<RepositorySettings> repositorySettings = restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings", RepositorySettings.class);
            return Optional.ofNullable(repositorySettings.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`repositorySettingsExists` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Boolean repositorySettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings/exists", Boolean.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveRepositorySettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RepositorySettings saveRepositorySettings(RepositorySettings repositorySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/repositorySettings", repositorySettings, RepositorySettings.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteRepositorySettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteRepositorySettings() {
        restTemplate.delete(baseURL + "/api/admin/repositorySettings");
    }

    /**
     * 方法说明：
     * 1. 职责：`checkRepositoryAccess` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void checkRepositoryAccess(RepositorySettings repositorySettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/repositorySettings/checkAccess", repositorySettings);
    }

    /**
     * 方法说明：
     * 1. 职责：`getAutoCommitSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<AutoCommitSettings> getAutoCommitSettings() {
        try {
            ResponseEntity<AutoCommitSettings> autoCommitSettings = restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings", AutoCommitSettings.class);
            return Optional.ofNullable(autoCommitSettings.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`autoCommitSettingsExists` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Boolean autoCommitSettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings/exists", Boolean.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveAutoCommitSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public AutoCommitSettings saveAutoCommitSettings(AutoCommitSettings autoCommitSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/autoCommitSettings", autoCommitSettings, AutoCommitSettings.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteAutoCommitSettings` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteAutoCommitSettings() {
        restTemplate.delete(baseURL + "/api/admin/autoCommitSettings");
    }

    /**
     * 方法说明：
     * 1. 职责：`checkUpdates` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<UpdateMessage> checkUpdates() {
        try {
            ResponseEntity<UpdateMessage> updateMsg = restTemplate.getForEntity(baseURL + "/api/admin/updates", UpdateMessage.class);
            return Optional.ofNullable(updateMsg.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getSystemInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public SystemInfo getSystemInfo() {
        return restTemplate.getForEntity(baseURL + "/api/admin/systemInfo", SystemInfo.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAlarmById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Alarm> getAlarmById(AlarmId alarmId) {
        try {
            ResponseEntity<Alarm> alarm = restTemplate.getForEntity(baseURL + "/api/alarm/{alarmId}", Alarm.class, alarmId.getId());
            return Optional.ofNullable(alarm.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getAlarmInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<AlarmInfo> getAlarmInfoById(AlarmId alarmId) {
        try {
            ResponseEntity<AlarmInfo> alarmInfo = restTemplate.getForEntity(baseURL + "/api/alarm/info/{alarmId}", AlarmInfo.class, alarmId.getId());
            return Optional.ofNullable(alarmInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Alarm saveAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}", alarmId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`ackAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void ackAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/ack", null, alarmId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`clearAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void clearAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/clear", null, alarmId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`assignAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void assignAlarm(AlarmId alarmId, UserId userId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/assign/{userId}", null, alarmId.getId(), userId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void unassignAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}/assign", alarmId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getAlarms` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AlarmInfo> getAlarms(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status, TimePageLink pageLink, Boolean fetchOriginator) {
        String urlSecondPart = "/api/alarm/{entityType}/{entityId}?fetchOriginator={fetchOriginator}";
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("fetchOriginator", String.valueOf(fetchOriginator));
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (searchStatus != null) {
            params.put("searchStatus", searchStatus.name());
            urlSecondPart += "&searchStatus={searchStatus}";
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (status != null) {
            params.put("status", status.name());
            urlSecondPart += "&status={status}";
        }

        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
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
     * 方法说明：
     * 1. 职责：`getHighestAlarmSeverity` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createAlarm` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Alarm createAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAlarmTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<EntitySubtype> getAlarmTypes(PageLink pageLink) {
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/alarm/types?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveAlarmComment` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public AlarmComment saveAlarmComment(AlarmId alarmId, AlarmComment alarmComment) {
        return restTemplate.postForEntity(baseURL + "/api/alarm/{alarmId}/comment", alarmComment, AlarmComment.class, alarmId.getId()).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteAlarmComment` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteAlarmComment(AlarmId alarmId, AlarmCommentId alarmCommentId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}/comment/{alarmCommentId}",
                alarmId.getId(), alarmCommentId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getAlarmComments` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AlarmCommentInfo> getAlarmComments(AlarmId alarmId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("alarmId", alarmId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/alarm/{alarmId}/comment?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmCommentInfo>>() {
                },
                params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> getAssetById(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/asset/{assetId}", Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<AssetInfo> getAssetInfoById(AssetId assetId) {
        try {
            ResponseEntity<AssetInfo> asset = restTemplate.getForEntity(baseURL + "/api/asset/info/{assetId}", AssetInfo.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Asset saveAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteAsset(AssetId assetId) {
        restTemplate.delete(baseURL + "/api/asset/{assetId}", assetId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`assignAssetToCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> assignAssetToCustomer(CustomerId customerId, AssetId assetId) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("assetId", assetId.getId().toString());

        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", null, Asset.class, params);
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignAssetFromCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> unassignAssetFromCustomer(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.exchange(baseURL + "/api/customer/asset/{assetId}", HttpMethod.DELETE, HttpEntity.EMPTY, Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignAssetToPublicCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> assignAssetToPublicCustomer(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/customer/public/asset/{assetId}", null, Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantAssets` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Asset> getTenantAssets(PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("type", assetType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantAssetInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AssetInfo> getTenantAssetInfos(String type, AssetProfileId assetProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("assetProfileId", assetProfileId != null ? assetProfileId.toString() : null);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AssetInfo>> assets = restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/assetInfos?type={type}&assetProfileId={assetProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetInfo>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> getTenantAsset(String assetName) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, assetName);
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerAssets` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Asset> getCustomerAssets(CustomerId customerId, PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", assetType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerAssetInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AssetInfo> getCustomerAssetInfos(CustomerId customerId, String assetType, AssetProfileId assetProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", assetType);
        params.put("assetProfileId", assetProfileId != null ? assetProfileId.toString() : null);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AssetInfo>> assets = restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/assetInfos?type={type}&assetProfileId={assetProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetInfo>>() {
                },
                params);
        return assets.getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetsByIds` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<Asset> findByQuery(AssetSearchQuery query) {
        return restTemplate.exchange(
                URI.create(baseURL + "/api/assets"),
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Asset>>() {
                }).getBody();
    }

    @Deprecated(since = "3.6.2")
    /**
     * 方法说明：
     * 1. 职责：`getAssetTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<EntitySubtype> getAssetTypes() {
        return restTemplate.exchange(URI.create(
                        baseURL + "/api/asset/types"),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetProfileNames` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`processAssetsBulkImport` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public BulkImportResult<Asset> processAssetsBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/asset/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Asset>>() {
                }).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`findAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> findAsset(String name) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("assetName", name);
        try {
            ResponseEntity<Asset> assetEntity = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, params);
            return Optional.of(assetEntity.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Asset createAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Asset createAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`assignAsset` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Asset assignAsset(CustomerId customerId, AssetId assetId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", HttpEntity.EMPTY, Asset.class,
                customerId.toString(), assetId.toString()).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAuditLogsByCustomerId` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AuditLog> getAuditLogsByCustomerId(CustomerId customerId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
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
     * 方法说明：
     * 1. 职责：`getAuditLogsByUserId` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AuditLog> getAuditLogsByUserId(UserId userId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
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
     * 方法说明：
     * 1. 职责：`getAuditLogsByEntityId` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AuditLog> getAuditLogsByEntityId(EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
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
     * 方法说明：
     * 1. 职责：`getAuditLogs` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AuditLog> getAuditLogs(TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("actionTypes", listEnumToString(actionTypes));
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
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
     * 方法说明：
     * 1. 职责：`getActivateToken` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public String getActivateToken(UserId userId) {
        String activationLink = getActivationLink(userId);
        return activationLink.substring(activationLink.lastIndexOf(ACTIVATE_TOKEN_REGEX) + ACTIVATE_TOKEN_REGEX.length());
    }

    /**
     * 方法说明：
     * 1. 职责：`getUser` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<User> getUser() {
        ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/auth/user", User.class);
        return Optional.ofNullable(user.getBody());
    }

    /**
     * 方法说明：
     * 1. 职责：`logout` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void logout() {
        restTemplate.postForLocation(baseURL + "/api/auth/logout", null);
    }

    /**
     * 方法说明：
     * 1. 职责：`changePassword` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void changePassword(String currentPassword, String newPassword) {
        ObjectNode changePasswordRequest = JacksonUtil.newObjectNode();
        changePasswordRequest.put("currentPassword", currentPassword);
        changePasswordRequest.put("newPassword", newPassword);
        restTemplate.postForLocation(baseURL + "/api/auth/changePassword", changePasswordRequest);
    }

    /**
     * 方法说明：
     * 1. 职责：`getUserPasswordPolicy` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<UserPasswordPolicy> getUserPasswordPolicy() {
        try {
            ResponseEntity<UserPasswordPolicy> userPasswordPolicy = restTemplate.getForEntity(baseURL + "/api/noauth/userPasswordPolicy", UserPasswordPolicy.class);
            return Optional.ofNullable(userPasswordPolicy.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`checkActivateToken` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public ResponseEntity<String> checkActivateToken(UserId userId) {
        String activateToken = getActivateToken(userId);
        return restTemplate.getForEntity(baseURL + "/api/noauth/activate?activateToken={activateToken}", String.class, activateToken);
    }

    /**
     * 方法说明：
     * 1. 职责：`requestResetPasswordByEmail` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void requestResetPasswordByEmail(String email) {
        ObjectNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode();
        resetPasswordByEmailRequest.put("email", email);
        restTemplate.postForLocation(baseURL + "/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest);
    }

    /**
     * 方法说明：
     * 1. 职责：`activateUser` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> activateUser(UserId userId, String password) {
        return activateUser(userId, password, true);
    }

    /**
     * 方法说明：
     * 1. 职责：`activateUser` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> activateUser(UserId userId, String password, boolean sendActivationMail) {
        ObjectNode activateRequest = JacksonUtil.newObjectNode();
        activateRequest.put("activateToken", getActivateToken(userId));
        activateRequest.put("password", password);
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activate?sendActivationMail={sendActivationMail}", activateRequest, JsonNode.class, sendActivationMail);
            return Optional.ofNullable(jsonNode.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getComponentDescriptorByClazz` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<ComponentDescriptor> getComponentDescriptorByClazz(String componentDescriptorClazz) {
        try {
            ResponseEntity<ComponentDescriptor> componentDescriptor = restTemplate.getForEntity(baseURL + "/api/component/{componentDescriptorClazz}", ComponentDescriptor.class, componentDescriptorClazz);
            return Optional.ofNullable(componentDescriptor.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getComponentDescriptorsByType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<ComponentDescriptor> getComponentDescriptorsByType(ComponentType componentType) {
        return getComponentDescriptorsByType(componentType, RuleChainType.CORE);
    }

    /**
     * 方法说明：
     * 1. 职责：`getComponentDescriptorsByType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getComponentDescriptorsByTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(List<ComponentType> componentTypes) {
        return getComponentDescriptorsByTypes(componentTypes, RuleChainType.CORE);
    }

    /**
     * 方法说明：
     * 1. 职责：`getComponentDescriptorsByTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getCustomerById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Customer> getCustomerById(CustomerId customerId) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}", Customer.class, customerId.getId());
            return Optional.ofNullable(customer.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getShortCustomerInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> getShortCustomerInfoById(CustomerId customerId) {
        try {
            ResponseEntity<JsonNode> customerInfo = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}/shortInfo", JsonNode.class, customerId.getId());
            return Optional.ofNullable(customerInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerTitleById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public String getCustomerTitleById(CustomerId customerId) {
        return restTemplate.getForObject(baseURL + "/api/customer/{customerId}/title", String.class, customerId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`saveCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Customer saveCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteCustomer(CustomerId customerId) {
        restTemplate.delete(baseURL + "/api/customer/{customerId}", customerId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomers` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Customer> getCustomers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Customer>> customer = restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customers?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Customer>>() {
                },
                params);
        return customer.getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Customer> getTenantCustomer(String customerTitle) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, customerTitle);
            return Optional.ofNullable(customer.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`findCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Customer> findCustomer(String title) {
        Map<String, String> params = new HashMap<>();
        params.put("customerTitle", title);
        try {
            ResponseEntity<Customer> customerEntity = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, params);
            return Optional.of(customerEntity.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Customer createCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Customer createCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getServerTime` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Long getServerTime() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/serverTime", Long.class);
    }

    /**
     * 方法说明：
     * 1. 职责：`getMaxDatapointsLimit` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Long getMaxDatapointsLimit() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/maxDatapointsLimit", Long.class);
    }

    /**
     * 方法说明：
     * 1. 职责：`getDashboardInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<DashboardInfo> getDashboardInfoById(DashboardId dashboardId) {
        try {
            ResponseEntity<DashboardInfo> dashboardInfo = restTemplate.getForEntity(baseURL + "/api/dashboard/info/{dashboardId}", DashboardInfo.class, dashboardId.getId());
            return Optional.ofNullable(dashboardInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDashboardById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> getDashboardById(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.getForEntity(baseURL + "/api/dashboard/{dashboardId}", Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveDashboard` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Dashboard saveDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteDashboard` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteDashboard(DashboardId dashboardId) {
        restTemplate.delete(baseURL + "/api/dashboard/{dashboardId}", dashboardId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDashboardToCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> assignDashboardToCustomer(CustomerId customerId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/dashboard/{dashboardId}", null, Dashboard.class, customerId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignDashboardFromCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> unassignDashboardFromCustomer(CustomerId customerId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.exchange(baseURL + "/api/customer/{customerId}/dashboard/{dashboardId}", HttpMethod.DELETE, HttpEntity.EMPTY, Dashboard.class, customerId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`updateDashboardCustomers` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> updateDashboardCustomers(DashboardId dashboardId, List<CustomerId> customerIds) {
        Object[] customerIdArray = customerIds.stream().map(customerId -> customerId.getId().toString()).toArray();
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/dashboard/{dashboardId}/customers", customerIdArray, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`addDashboardCustomers` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> addDashboardCustomers(DashboardId dashboardId, List<CustomerId> customerIds) {
        Object[] customerIdArray = customerIds.stream().map(customerId -> customerId.getId().toString()).toArray();
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/dashboard/{dashboardId}/customers/add", customerIdArray, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`removeDashboardCustomers` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> removeDashboardCustomers(DashboardId dashboardId, List<CustomerId> customerIds) {
        Object[] customerIdArray = customerIds.stream().map(customerId -> customerId.getId().toString()).toArray();
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/dashboard/{dashboardId}/customers/remove", customerIdArray, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDashboardToPublicCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> assignDashboardToPublicCustomer(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/customer/public/dashboard/{dashboardId}", null, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignDashboardFromPublicCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> unassignDashboardFromPublicCustomer(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.exchange(baseURL + "/api/customer/public/dashboard/{dashboardId}", HttpMethod.DELETE, HttpEntity.EMPTY, Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantDashboards` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DashboardInfo> getTenantDashboards(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/{tenantId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantDashboards` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DashboardInfo> getTenantDashboards(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerDashboards` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DashboardInfo> getCustomerDashboards(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createDashboard` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Dashboard createDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`findTenantDashboards` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<DashboardInfo> findTenantDashboards() {
        try {
            ResponseEntity<PageData<DashboardInfo>> dashboards =
                    restTemplate.exchange(baseURL + "/api/tenant/dashboards?pageSize=100000", HttpMethod.GET, null, new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                    });
            return dashboards.getBody().getData();
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> getDeviceById(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}", Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<DeviceInfo> getDeviceInfoById(DeviceId deviceId) {
        try {
            ResponseEntity<DeviceInfo> device = restTemplate.getForEntity(baseURL + "/api/device/info/{deviceId}", DeviceInfo.class, deviceId);
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device saveDevice(Device device) {
        return saveDevice(device, null);
    }

    /**
     * 方法说明：
     * 1. 职责：`saveDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device saveDevice(Device device, String accessToken) {
        return restTemplate.postForEntity(baseURL + "/api/device?accessToken={accessToken}", device, Device.class, accessToken).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteDevice(DeviceId deviceId) {
        restTemplate.delete(baseURL + "/api/device/{deviceId}", deviceId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDeviceToCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> assignDeviceToCustomer(CustomerId customerId, DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/device/{deviceId}", null, Device.class, customerId.getId(), deviceId.getId());
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignDeviceFromCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> unassignDeviceFromCustomer(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.exchange(baseURL + "/api/customer/device/{deviceId}", HttpMethod.DELETE, HttpEntity.EMPTY, Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDeviceToPublicCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> assignDeviceToPublicCustomer(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.postForEntity(baseURL + "/api/customer/public/device/{deviceId}", null, Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceCredentialsByDeviceId` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<DeviceCredentials> getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        try {
            ResponseEntity<DeviceCredentials> deviceCredentials = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}/credentials", DeviceCredentials.class, deviceId.getId());
            return Optional.ofNullable(deviceCredentials.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveDeviceCredentials` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public DeviceCredentials saveDeviceCredentials(DeviceCredentials deviceCredentials) {
        return restTemplate.postForEntity(baseURL + "/api/device/credentials", deviceCredentials, DeviceCredentials.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveDeviceWithCredentials` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> saveDeviceWithCredentials(Device device, DeviceCredentials credentials) {
        try {
            SaveDeviceWithCredentialsRequest request = new SaveDeviceWithCredentialsRequest(device, credentials);
            ResponseEntity<Device> deviceOpt = restTemplate.postForEntity(baseURL + "/api/device-with-credentials", request, Device.class);
            return Optional.ofNullable(deviceOpt.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantDevices` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Device> getTenantDevices(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantDeviceInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DeviceInfo> getTenantDeviceInfos(String type, DeviceProfileId deviceProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("deviceProfileId", deviceProfileId != null ? deviceProfileId.toString() : null);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/deviceInfos?type={type}&deviceProfileId={deviceProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> getTenantDevice(String deviceName) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, deviceName);
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerDevices` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Device> getCustomerDevices(CustomerId customerId, String deviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", deviceType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerDeviceInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DeviceInfo> getCustomerDeviceInfos(CustomerId customerId, String deviceType, DeviceProfileId deviceProfileId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.toString());
        params.put("type", deviceType);
        params.put("deviceProfileId", deviceProfileId != null ? deviceProfileId.toString() : null);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/devices?type={type}&deviceProfileId={deviceProfileId}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getDevicesByIds` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<Device> getDevicesByIds(List<DeviceId> deviceIds) {
        return restTemplate.exchange(baseURL + "/api/devices?deviceIds={deviceIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Device>>() {
                }, listIdsToString(deviceIds)).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`findByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<Device> findByQuery(DeviceSearchQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/devices",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Device>>() {
                }).getBody();
    }

    @Deprecated(since = "3.6.2")
    /**
     * 方法说明：
     * 1. 职责：`getDeviceTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<EntitySubtype> getDeviceTypes() {
        return restTemplate.exchange(
                baseURL + "/api/device/types",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceProfileNames` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`claimDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`reClaimDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void reClaimDevice(String deviceName) {
        restTemplate.delete(baseURL + "/api/customer/device/{deviceName}/claim", deviceName);
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDeviceToTenant` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device assignDeviceToTenant(TenantId tenantId, DeviceId deviceId) {
        return restTemplate.postForEntity(
                baseURL + "/api/tenant/{tenantId}/device/{deviceId}",
                HttpEntity.EMPTY, Device.class, tenantId, deviceId).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`countByDeviceProfileAndEmptyOtaPackage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`processDevicesBulkImport` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public BulkImportResult<Device> processDevicesBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/device/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Device>>() {
                }).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device createDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doCreateDevice(device, null);
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device createDevice(Device device) {
        return doCreateDevice(device, null);
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`createDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device createDevice(Device device, String accessToken) {
        return doCreateDevice(device, accessToken);
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`doCreateDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private Device doCreateDevice(Device device, String accessToken) {
        Map<String, String> params = new HashMap<>();
        String deviceCreationUrl = "/api/device";
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (!StringUtils.isEmpty(accessToken)) {
            deviceCreationUrl = deviceCreationUrl + "?accessToken={accessToken}";
            params.put("accessToken", accessToken);
        }
        return restTemplate.postForEntity(baseURL + deviceCreationUrl, device, Device.class, params).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`getCredentials` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public DeviceCredentials getCredentials(DeviceId id) {
        return restTemplate.getForEntity(baseURL + "/api/device/" + id.getId().toString() + "/credentials", DeviceCredentials.class).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`findDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> findDevice(String name) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceName", name);
        try {
            ResponseEntity<Device> deviceEntity = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, params);
            return Optional.of(deviceEntity.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`updateDeviceCredentials` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public DeviceCredentials updateDeviceCredentials(DeviceId deviceId, String token) {
        DeviceCredentials deviceCredentials = getCredentials(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(token);
        return saveDeviceCredentials(deviceCredentials);
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`assignDevice` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Device assignDevice(CustomerId customerId, DeviceId deviceId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/device/{deviceId}", null, Device.class,
                customerId.toString(), deviceId.toString()).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceProfileById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<DeviceProfile> getDeviceProfileById(DeviceProfileId deviceProfileId) {
        try {
            ResponseEntity<DeviceProfile> deviceProfile = restTemplate.getForEntity(baseURL + "/api/deviceProfile/{deviceProfileId}", DeviceProfile.class, deviceProfileId);
            return Optional.ofNullable(deviceProfile.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceProfileInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<DeviceProfileInfo> getDeviceProfileInfoById(DeviceProfileId deviceProfileId) {
        try {
            ResponseEntity<DeviceProfileInfo> deviceProfileInfo = restTemplate.getForEntity(baseURL + "/api/deviceProfileInfo/{deviceProfileId}", DeviceProfileInfo.class, deviceProfileId);
            return Optional.ofNullable(deviceProfileInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDefaultDeviceProfileInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public DeviceProfileInfo getDefaultDeviceProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/deviceProfileInfo/default", DeviceProfileInfo.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveDeviceProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        return restTemplate.postForEntity(baseURL + "/api/deviceProfile", deviceProfile, DeviceProfile.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteDeviceProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteDeviceProfile(DeviceProfileId deviceProfileId) {
        restTemplate.delete(baseURL + "/api/deviceProfile/{deviceProfileId}", deviceProfileId);
    }

    /**
     * 方法说明：
     * 1. 职责：`setDefaultDeviceProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public DeviceProfile setDefaultDeviceProfile(DeviceProfileId deviceProfileId) {
        return restTemplate.postForEntity(
                baseURL + "/api/deviceProfile/{deviceProfileId}/default",
                HttpEntity.EMPTY, DeviceProfile.class, deviceProfileId).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceProfiles` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DeviceProfile> getDeviceProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/deviceProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceProfile>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getDeviceProfileInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DeviceProfileInfo> getDeviceProfileInfos(PageLink pageLink, DeviceTransportType deviceTransportType) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceTransportType", deviceTransportType != null ? deviceTransportType.name() : null);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/deviceProfileInfos?deviceTransportType={deviceTransportType}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceProfileInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetProfileById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<AssetProfile> getAssetProfileById(AssetProfileId assetProfileId) {
        try {
            ResponseEntity<AssetProfile> assetProfile = restTemplate.getForEntity(baseURL + "/api/assetProfile/{assetProfileId}", AssetProfile.class, assetProfileId);
            return Optional.ofNullable(assetProfile.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetProfileInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<AssetProfileInfo> getAssetProfileInfoById(AssetProfileId assetProfileId) {
        try {
            ResponseEntity<AssetProfileInfo> assetProfileInfo = restTemplate.getForEntity(baseURL + "/api/assetProfileInfo/{assetProfileId}", AssetProfileInfo.class, assetProfileId);
            return Optional.ofNullable(assetProfileInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDefaultAssetProfileInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public AssetProfileInfo getDefaultAssetProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/assetProfileInfo/default", AssetProfileInfo.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveAssetProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return restTemplate.postForEntity(baseURL + "/api/assetProfile", assetProfile, AssetProfile.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteAssetProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteAssetProfile(AssetProfileId assetProfileId) {
        restTemplate.delete(baseURL + "/api/assetProfile/{assetProfileId}", assetProfileId);
    }

    /**
     * 方法说明：
     * 1. 职责：`setDefaultAssetProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public AssetProfile setDefaultAssetProfile(AssetProfileId assetProfileId) {
        return restTemplate.postForEntity(
                baseURL + "/api/assetProfile/{assetProfileId}/default",
                HttpEntity.EMPTY, AssetProfile.class, assetProfileId).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetProfiles` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AssetProfile> getAssetProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/assetProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetProfile>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getAssetProfileInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AssetProfileInfo> getAssetProfileInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/assetProfileInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetProfileInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`countEntitiesByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Long countEntitiesByQuery(EntityCountQuery query) {
        return restTemplate.postForObject(baseURL + "/api/entitiesQuery/count", query, Long.class);
    }

    /**
     * 方法说明：
     * 1. 职责：`findEntityDataByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/entitiesQuery/find",
                HttpMethod.POST, new HttpEntity<>(query),
                new ParameterizedTypeReference<PageData<EntityData>>() {
                }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`findAlarmDataByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/alarmsQuery/find",
                HttpMethod.POST, new HttpEntity<>(query),
                new ParameterizedTypeReference<PageData<AlarmData>>() {
                }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`countAlarmsByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Long countAlarmsByQuery(AlarmCountQuery query) {
        return restTemplate.postForObject(baseURL + "/api/alarmsQuery/count", query, Long.class);
    }

    /**
     * 方法说明：
     * 1. 职责：`saveRelation` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void saveRelation(EntityRelation relation) {
        restTemplate.postForLocation(baseURL + "/api/relation", relation);
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteRelation` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`deleteRelations` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteRelations(EntityId entityId) {
        restTemplate.delete(baseURL + "/api/relations?entityId={entityId}&entityType={entityType}", entityId.getId().toString(), entityId.getEntityType().name());
    }

    /**
     * 方法说明：
     * 1. 职责：`getRelation` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`findByFrom` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findInfoByFrom` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findByFrom` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findByTo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findInfoByTo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findByTo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`findInfoByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<EntityRelationInfo> findInfoByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations/info",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                }).getBody();
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`makeRelation` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public EntityRelation makeRelation(String relationType, EntityId idFrom, EntityId idTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(idFrom);
        relation.setTo(idTo);
        relation.setType(relationType);
        return restTemplate.postForEntity(baseURL + "/api/relation", relation, EntityRelation.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getEntityViewById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> getEntityViewById(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/entityView/{entityViewId}", EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEntityViewInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityViewInfo> getEntityViewInfoById(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityViewInfo> entityView = restTemplate.getForEntity(baseURL + "/api/entityView/info/{entityViewId}", EntityViewInfo.class, entityViewId);
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveEntityView` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public EntityView saveEntityView(EntityView entityView) {
        return restTemplate.postForEntity(baseURL + "/api/entityView", entityView, EntityView.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteEntityView` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteEntityView(EntityViewId entityViewId) {
        restTemplate.delete(baseURL + "/api/entityView/{entityViewId}", entityViewId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantEntityView` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> getTenantEntityView(String entityViewName) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/tenant/entityViews?entityViewName={entityViewName}", EntityView.class, entityViewName);
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignEntityViewToCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> assignEntityViewToCustomer(CustomerId customerId, EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/entityView/{entityViewId}", null, EntityView.class, customerId.getId(), entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignEntityViewFromCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> unassignEntityViewFromCustomer(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.exchange(baseURL + "/api/customer/entityView/{entityViewId}", HttpMethod.DELETE, HttpEntity.EMPTY, EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerEntityViews` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityView> getCustomerEntityViews(CustomerId customerId, String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", entityViewType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerEntityViewInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityViewInfo> getCustomerEntityViewInfos(CustomerId customerId, String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.toString());
        params.put("type", entityViewType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/entityViewInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityViewInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantEntityViews` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityView> getTenantEntityViews(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantEntityViewInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityViewInfo> getTenantEntityViewInfos(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/entityViewInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityViewInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`findByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<EntityView> findByQuery(EntityViewSearchQuery query) {
        return restTemplate.exchange(baseURL + "/api/entityViews", HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<List<EntityView>>() {
        }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getEntityViewTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<EntitySubtype> getEntityViewTypes() {
        return restTemplate.exchange(baseURL + "/api/entityView/types", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<EntitySubtype>>() {
        }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`assignEntityViewToPublicCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> assignEntityViewToPublicCustomer(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.postForEntity(baseURL + "/api/customer/public/entityView/{entityViewId}", null, EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEvents` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EventInfo> getEvents(EntityId entityId, String eventType, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("eventType", eventType);
        params.put("tenantId", tenantId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
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
     * 方法说明：
     * 1. 职责：`getEvents` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EventInfo> getEvents(EntityId entityId, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("tenantId", tenantId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EventInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveClientRegistrationTemplate` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        return restTemplate.postForEntity(baseURL + "/api/oauth2/config/template", clientRegistrationTemplate, OAuth2ClientRegistrationTemplate.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteClientRegistrationTemplate` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteClientRegistrationTemplate(OAuth2ClientRegistrationTemplateId oAuth2ClientRegistrationTemplateId) {
        restTemplate.delete(baseURL + "/api/oauth2/config/template/{clientRegistrationTemplateId}", oAuth2ClientRegistrationTemplateId);
    }

    /**
     * 方法说明：
     * 1. 职责：`getClientRegistrationTemplates` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getOAuth2Clients` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<OAuth2ClientInfo> getOAuth2Clients(String pkgName, PlatformType platformType) {
        Map<String, String> params = new HashMap<>();
        StringBuilder urlBuilder = new StringBuilder(baseURL);
        urlBuilder.append("/api/noauth/oauth2Clients");
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pkgName != null) {
            urlBuilder.append("?pkgName={pkgName}");
            params.put("pkgName", pkgName);
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (platformType != null) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
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
     * 方法说明：
     * 1. 职责：`getCurrentOAuth2Info` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public OAuth2Info getCurrentOAuth2Info() {
        return restTemplate.getForEntity(baseURL + "/api/oauth2/config", OAuth2Info.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveOAuth2Info` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public OAuth2Info saveOAuth2Info(OAuth2Info oauth2Info) {
        return restTemplate.postForEntity(baseURL + "/api/oauth2/config", oauth2Info, OAuth2Info.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getLoginProcessingUrl` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public String getLoginProcessingUrl() {
        return restTemplate.getForEntity(baseURL + "/api/oauth2/loginProcessingUrl", String.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`handleOneWayDeviceRPCRequest` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void handleOneWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        restTemplate.postForLocation(baseURL + "/api/rpc/oneway/{deviceId}", requestBody, deviceId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`handleTwoWayDeviceRPCRequest` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getRuleChainById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> getRuleChainById(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}", RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getRuleChainMetaData` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChainMetaData> getRuleChainMetaData(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChainMetaData> ruleChainMetaData = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}/metadata", RuleChainMetaData.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChainMetaData.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain", ruleChain, RuleChain.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RuleChain saveRuleChain(DefaultRuleChainCreateRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/device/default", request, RuleChain.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`setRootRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> setRootRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/root", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveRuleChainMetaData` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMetaData) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        return getRuleChains(RuleChainType.CORE, pageLink);
    }

    /**
     * 方法说明：
     * 1. 职责：`getRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<RuleChain> getRuleChains(RuleChainType ruleChainType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", ruleChainType.name());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/ruleChains?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                },
                params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteRuleChain(RuleChainId ruleChainId) {
        restTemplate.delete(baseURL + "/api/ruleChain/{ruleChainId}", ruleChainId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getLatestRuleNodeDebugInput` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> getLatestRuleNodeDebugInput(RuleNodeId ruleNodeId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/ruleNode/{ruleNodeId}/debugIn", JsonNode.class, ruleNodeId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`testScript` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> testScript(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/ruleChain/testScript", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`exportRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public RuleChainData exportRuleChains(int limit) {
        return restTemplate.getForEntity(baseURL + "/api/ruleChains/export?limit=" + limit, RuleChainData.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`importRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void importRuleChains(RuleChainData ruleChainData, boolean overwrite) {
        restTemplate.postForLocation(baseURL + "/api/ruleChains/import?overwrite=" + overwrite, ruleChainData);
    }

    /**
     * 方法说明：
     * 1. 职责：`getAttributeKeys` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getAttributeKeysByScope` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getAttributeKvEntries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getAttributeKvEntriesAsync` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Future<List<AttributeKvEntry>> getAttributeKvEntriesAsync(EntityId entityId, List<String> keys) {
        return service.submit(() -> getAttributeKvEntries(entityId, keys));
    }

    /**
     * 方法说明：
     * 1. 职责：`getAttributesByScope` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getTimeseriesKeys` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getLatestTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys) {
        return getLatestTimeseries(entityId, keys, true);
    }

    /**
     * 方法说明：
     * 1. 职责：`getLatestTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`getTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink) {
        return getTimeseries(entityId, keys, interval, agg, pageLink, true);
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`getTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink, boolean useStrictDataTypes) {
        SortOrder sortOrder = pageLink.getSortOrder();
        return getTimeseries(entityId, keys, interval, agg, sortOrder != null ? sortOrder.getDirection() : null, pageLink.getStartTime(), pageLink.getEndTime(), 100, useStrictDataTypes);
    }

    /**
     * 方法说明：
     * 1. 职责：`getTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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

        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (startTime != null) {
            urlBuilder.append("&startTs={startTs}");
            params.put("startTs", String.valueOf(startTime));
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
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
     * 方法说明：
     * 1. 职责：`saveDeviceAttributes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public boolean saveDeviceAttributes(DeviceId deviceId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(baseURL + "/api/plugins/telemetry/{deviceId}/{scope}", request, Object.class, deviceId.getId().toString(), scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveEntityAttributesV1` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`saveEntityAttributesV2` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`saveEntityTelemetry` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`saveEntityTelemetryWithTTL` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`deleteEntityTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`deleteEntityLatestTimeseries` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`deleteEntityAttributes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`deleteEntityAttributes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getTenantById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Tenant> getTenantById(TenantId tenantId) {
        try {
            ResponseEntity<Tenant> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/{tenantId}", Tenant.class, tenantId.getId());
            return Optional.ofNullable(tenant.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<TenantInfo> getTenantInfoById(TenantId tenantId) {
        try {
            ResponseEntity<TenantInfo> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/info/{tenantId}", TenantInfo.class, tenantId);
            return Optional.ofNullable(tenant.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveTenant` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Tenant saveTenant(Tenant tenant) {
        return restTemplate.postForEntity(baseURL + "/api/tenant", tenant, Tenant.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteTenant` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteTenant(TenantId tenantId) {
        restTemplate.delete(baseURL + "/api/tenant/{tenantId}", tenantId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenants` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Tenant> getTenants(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenants?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Tenant>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<TenantInfo> getTenantInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenantInfos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TenantInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getUsageInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public UsageInfo getUsageInfo() {
        return restTemplate.exchange(
                baseURL + "/api/usage",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                UsageInfo.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantProfileById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<TenantProfile> getTenantProfileById(TenantProfileId tenantProfileId) {
        try {
            ResponseEntity<TenantProfile> tenantProfile = restTemplate.getForEntity(baseURL + "/api/tenantProfile/{tenantProfileId}", TenantProfile.class, tenantProfileId);
            return Optional.ofNullable(tenantProfile.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantProfileInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityInfo> getTenantProfileInfoById(TenantProfileId tenantProfileId) {
        try {
            ResponseEntity<EntityInfo> entityInfo = restTemplate.getForEntity(baseURL + "/api/tenantProfileInfo/{tenantProfileId}", EntityInfo.class, tenantProfileId);
            return Optional.ofNullable(entityInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getDefaultTenantProfileInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public EntityInfo getDefaultTenantProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/tenantProfileInfo/default", EntityInfo.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveTenantProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TenantProfile saveTenantProfile(TenantProfile tenantProfile) {
        return restTemplate.postForEntity(baseURL + "/api/tenantProfile", tenantProfile, TenantProfile.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteTenantProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteTenantProfile(TenantProfileId tenantProfileId) {
        restTemplate.delete(baseURL + "/api/tenantProfile/{tenantProfileId}", tenantProfileId);
    }

    /**
     * 方法说明：
     * 1. 职责：`setDefaultTenantProfile` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TenantProfile setDefaultTenantProfile(TenantProfileId tenantProfileId) {
        return restTemplate.exchange(baseURL + "/api/tenantProfile/{tenantProfileId}/default", HttpMethod.POST, HttpEntity.EMPTY, TenantProfile.class, tenantProfileId).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantProfiles` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<TenantProfile> getTenantProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenantProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TenantProfile>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantProfileInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityInfo> getTenantProfileInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenantProfileInfos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getUserById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<User> getUserById(UserId userId) {
        try {
            ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/user/{userId}", User.class, userId.getId());
            return Optional.ofNullable(user.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`isUserTokenAccessEnabled` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Boolean isUserTokenAccessEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/user/tokenAccessEnabled", Boolean.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getUserToken` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> getUserToken(UserId userId) {
        try {
            ResponseEntity<JsonNode> userToken = restTemplate.getForEntity(baseURL + "/api/user/{userId}/token", JsonNode.class, userId.getId());
            return Optional.ofNullable(userToken.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveUser` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public User saveUser(User user, boolean sendActivationMail) {
        return restTemplate.postForEntity(baseURL + "/api/user?sendActivationMail={sendActivationMail}", user, User.class, sendActivationMail).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`sendActivationEmail` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void sendActivationEmail(String email) {
        restTemplate.postForLocation(baseURL + "/api/user/sendActivationMail?email={email}", null, email);
    }

    /**
     * 方法说明：
     * 1. 职责：`getActivationLink` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public String getActivationLink(UserId userId) {
        return restTemplate.getForEntity(baseURL + "/api/user/{userId}/activationLink", String.class, userId.getId()).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteUser` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteUser(UserId userId) {
        restTemplate.delete(baseURL + "/api/user/{userId}", userId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getUsers` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<User> getUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantAdmins` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<User> getTenantAdmins(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/{tenantId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerUsers` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<User> getCustomerUsers(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getUsersForAssign` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<UserEmailInfo> getUsersForAssign(AlarmId alarmId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("alarmId", alarmId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/users/assign/{alarmId}" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<UserEmailInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`setUserCredentialsEnabled` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void setUserCredentialsEnabled(UserId userId, boolean userCredentialsEnabled) {
        restTemplate.postForLocation(
                baseURL + "/api/user/{userId}/userCredentialsEnabled?userCredentialsEnabled={userCredentialsEnabled}",
                null,
                userId.getId(),
                userCredentialsEnabled);
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetsBundleById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<WidgetsBundle> getWidgetsBundleById(WidgetsBundleId widgetsBundleId) {
        try {
            ResponseEntity<WidgetsBundle> widgetsBundle =
                    restTemplate.getForEntity(baseURL + "/api/widgetsBundle/{widgetsBundleId}", WidgetsBundle.class, widgetsBundleId.getId());
            return Optional.ofNullable(widgetsBundle.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveWidgetsBundle` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        return restTemplate.postForEntity(baseURL + "/api/widgetsBundle", widgetsBundle, WidgetsBundle.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`updateWidgetsBundleWidgetTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds) {
        var httpEntity = new HttpEntity<>(widgetTypeIds.stream()
                .map(widgetTypeId -> widgetTypeId.getId().toString())
                .collect(Collectors.toList()));
        restTemplate.exchange(baseURL + "/api/widgetsBundle/{widgetsBundleId}/widgetTypes",
                HttpMethod.POST, httpEntity, Void.class, widgetsBundleId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`updateWidgetsBundleWidgetFqns` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetTypeFqns) {
        restTemplate.exchange(baseURL + "/api/widgetsBundle/{widgetsBundleId}/widgetTypeFqns",
                HttpMethod.POST, new HttpEntity<>(widgetTypeFqns), Void.class, widgetsBundleId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteWidgetsBundle` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) {
        restTemplate.delete(baseURL + "/api/widgetsBundle/{widgetsBundleId}", widgetsBundleId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetsBundles` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink) {
        return getWidgetsBundles(pageLink, null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetsBundles` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink, Boolean tenantOnly, Boolean fullSearch) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        addTenantOnlyAndFullSearchToParams(tenantOnly, fullSearch, params);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/widgetsBundles?" + getUrlParams(pageLink) + getTenantOnlyAndFullSearchUrlParams(tenantOnly, fullSearch),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetsBundle>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetsBundles` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getWidgetTypeById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<WidgetTypeDetails> getWidgetTypeById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetTypeDetails> widgetTypeDetails =
                    restTemplate.getForEntity(baseURL + "/api/widgetType/{widgetTypeId}", WidgetTypeDetails.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetTypeDetails.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetTypeInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<WidgetTypeInfo> getWidgetTypeInfoById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetTypeInfo> widgetTypeInfo =
                    restTemplate.getForEntity(baseURL + "/api/widgetTypeInfo/{widgetTypeId}", WidgetTypeInfo.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetTypeInfo.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`saveWidgetType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        return saveWidgetType(widgetTypeDetails, null);
    }

    /**
     * 方法说明：
     * 1. 职责：`saveWidgetType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails, Boolean updateExistingByFqn) {
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (updateExistingByFqn == null) {
            return restTemplate.postForEntity(baseURL + "/api/widgetType", widgetTypeDetails, WidgetTypeDetails.class).getBody();
        }
        return restTemplate.postForEntity(baseURL + "/api/widgetType?updateExistingByFqn={updateExistingByFqn}", widgetTypeDetails, WidgetTypeDetails.class, updateExistingByFqn).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteWidgetType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteWidgetType(WidgetTypeId widgetTypeId) {
        restTemplate.delete(baseURL + "/api/widgetType/{widgetTypeId}", widgetTypeId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<WidgetTypeInfo> getWidgetTypes(PageLink pageLink) {
        return getWidgetTypes(pageLink, null, null, null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<WidgetTypeInfo> getWidgetTypes(PageLink pageLink, Boolean tenantOnly, Boolean fullSearch,
                                                   DeprecatedFilter deprecatedFilter, List<String> widgetTypeList) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        addWidgetInfoFiltersToParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList, params);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/widgetTypes?" + getUrlParams(pageLink) +
                        getWidgetTypeInfoPageRequestUrlParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetTypeInfo>>() {
                },
                params).getBody();
    }

    @Deprecated // current name in the controller: getBundleWidgetTypesByBundleAlias
    /**
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
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
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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

    @Deprecated // current name in the controller: getBundleWidgetTypesDetailsByBundleAlias
    /**
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypesDetails` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
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
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypesDetails` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypeFqns` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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

    @Deprecated // current name in the controller: getBundleWidgetTypesInfosByBundleAlias
    /**
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypesInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
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
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypesInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(WidgetsBundleId widgetsBundleId, PageLink pageLink) {
        return getBundleWidgetTypesInfos(widgetsBundleId, pageLink, null, null, null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：`getBundleWidgetTypesInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(WidgetsBundleId widgetsBundleId, PageLink pageLink,
                                                              Boolean tenantOnly, Boolean fullSearch,
                                                              DeprecatedFilter deprecatedFilter, List<String> widgetTypeList) {
        Map<String, String> params = new HashMap<>();
        params.put("widgetsBundleId", widgetsBundleId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        addWidgetInfoFiltersToParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList, params);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/widgetTypesInfos?widgetsBundleId={widgetsBundleId}&" + getUrlParams(pageLink) +
                        getWidgetTypeInfoPageRequestUrlParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetTypeInfo>>() {
                },
                params).getBody();
    }

    @Deprecated // current name in the controller: getWidgetTypeByBundleAliasAndTypeAlias
    /**
     * 方法说明：
     * 1. 职责：`getWidgetType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
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
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<WidgetType> getWidgetType(String fqn) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(
                            baseURL + "/api/widgetType?fqn={fqn}",
                            WidgetType.class,
                            fqn);
            return Optional.ofNullable(widgetType.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`isEdgesSupportEnabled` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Boolean isEdgesSupportEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/edges/enabled", Boolean.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Edge saveEdge(Edge edge) {
        return restTemplate.postForEntity(baseURL + "/api/edge", edge, Edge.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteEdge(EdgeId edgeId) {
        restTemplate.delete(baseURL + "/api/edge/{edgeId}", edgeId.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Edge> getEdgeById(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.getForEntity(baseURL + "/api/edge/{edgeId}", Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EdgeInfo> getEdgeInfoById(EdgeId edgeId) {
        try {
            ResponseEntity<EdgeInfo> edge = restTemplate.getForEntity(baseURL + "/api/edge/info/{edgeId}", EdgeInfo.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignEdgeToCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Edge> assignEdgeToCustomer(CustomerId customerId, EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/edge/{edgeId}", null, Edge.class, customerId.getId(), edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignEdgeToPublicCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Edge> assignEdgeToPublicCustomer(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.postForEntity(baseURL + "/api/customer/public/edge/{edgeId}", null, Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`setEdgeRootRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Edge> setEdgeRootRuleChain(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<Edge> ruleChain = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/{ruleChainId}/root", null, Edge.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdges` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Edge> getEdges(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edges?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignEdgeFromCustomer` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Edge> unassignEdgeFromCustomer(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.exchange(baseURL + "/api/customer/edge/{edgeId}", HttpMethod.DELETE, HttpEntity.EMPTY, Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDeviceToEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> assignDeviceToEdge(EdgeId edgeId, DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/device/{deviceId}", null, Device.class, edgeId.getId(), deviceId.getId());
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignDeviceFromEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Device> unassignDeviceFromEdge(EdgeId edgeId, DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/device/{deviceId}", HttpMethod.DELETE, HttpEntity.EMPTY, Device.class, edgeId.getId(), deviceId.getId());
            return Optional.ofNullable(device.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeDevices` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Device> getEdgeDevices(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edge/{edgeId}/devices?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`assignAssetToEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> assignAssetToEdge(EdgeId edgeId, AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/asset/{assetId}", null, Asset.class, edgeId.getId(), assetId.getId());
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignAssetFromEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Asset> unassignAssetFromEdge(EdgeId edgeId, AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/asset/{assetId}", HttpMethod.DELETE, HttpEntity.EMPTY, Asset.class, edgeId.getId(), assetId.getId());
            return Optional.ofNullable(asset.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeAssets` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Asset> getEdgeAssets(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edge/{edgeId}/assets?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`assignDashboardToEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> assignDashboardToEdge(EdgeId edgeId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/dashboard/{dashboardId}", null, Dashboard.class, edgeId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignDashboardFromEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Dashboard> unassignDashboardFromEdge(EdgeId edgeId, DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/dashboard/{dashboardId}", HttpMethod.DELETE, HttpEntity.EMPTY, Dashboard.class, edgeId.getId(), dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeDashboards` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<DashboardInfo> getEdgeDashboards(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edge/{edgeId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`assignEntityViewToEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> assignEntityViewToEdge(EdgeId edgeId, EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/entityView/{entityViewId}", null, EntityView.class, edgeId.getId(), entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignEntityViewFromEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EntityView> unassignEntityViewFromEdge(EdgeId edgeId, EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/entityView/{entityViewId}",
                    HttpMethod.DELETE, HttpEntity.EMPTY, EntityView.class, edgeId.getId(), entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeEntityViews` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityView> getEdgeEntityViews(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edge/{edgeId}/entityViews?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`assignRuleChainToEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> assignRuleChainToEdge(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/ruleChain/{ruleChainId}", null, RuleChain.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unassignRuleChainFromEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> unassignRuleChainFromEdge(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/ruleChain/{ruleChainId}", HttpMethod.DELETE, HttpEntity.EMPTY, RuleChain.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<RuleChain> getEdgeRuleChains(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edge/{edgeId}/ruleChains?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`setAutoAssignToEdgeRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> setAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/autoAssignToEdge", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`unsetAutoAssignToEdgeRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> unsetAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.exchange(baseURL + "/api/ruleChain/{ruleChainId}/autoAssignToEdge", HttpMethod.DELETE, HttpEntity.EMPTY, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getAutoAssignToEdgeRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<RuleChain> getAutoAssignToEdgeRuleChains() {
        return restTemplate.exchange(baseURL + "/api/ruleChain/autoAssignToEdgeRuleChains",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<RuleChain>>() {
                }).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`setRootEdgeTemplateRuleChain` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<RuleChain> setRootEdgeTemplateRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/edgeTemplateRoot", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantEdges` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Edge> getTenantEdges(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantEdgeInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EdgeInfo> getTenantEdgeInfos(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/tenant/edgeInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<Edge> getTenantEdge(String edgeName) {
        try {
            ResponseEntity<Edge> edge = restTemplate.getForEntity(baseURL + "/api/tenant/edges?edgeName={edgeName}", Edge.class, edgeName);
            return Optional.ofNullable(edge.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerEdges` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Edge> getCustomerEdges(CustomerId customerId, PageLink pageLink, String edgeType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", edgeType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getCustomerEdgeInfos` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EdgeInfo> getCustomerEdgeInfos(CustomerId customerId, PageLink pageLink, String edgeType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", edgeType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/customer/{customerId}/edgeInfos?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeInfo>>() {
                }, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgesByIds` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public List<Edge> getEdgesByIds(List<EdgeId> edgeIds) {
        return restTemplate.exchange(baseURL + "/api/edges?edgeIds={edgeIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Edge>>() {
                }, listIdsToString(edgeIds)).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`findByQuery` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getEdgeTypes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getEdgeEvents` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EdgeEvent> getEdgeEvents(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.toString());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/edge/{edgeId}/events?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeEvent>>() {
                },
                params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`syncEdge` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void syncEdge(EdgeId edgeId) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.toString());
        restTemplate.postForEntity(baseURL + "/api/edge/sync/{edgeId}", null, EdgeId.class, params);
    }

    /**
     * 方法说明：
     * 1. 职责：`findMissingToRelatedRuleChains` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public String findMissingToRelatedRuleChains(EdgeId edgeId) {
        return restTemplate.getForEntity(baseURL + "/api/edge/missingToRelatedRuleChains/{edgeId}", String.class, edgeId.getId()).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`processEdgesBulkImport` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getEdgeInstallInstructions` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EdgeInstructions> getEdgeInstallInstructions(EdgeId edgeId, String method) {
        ResponseEntity<EdgeInstructions> edgeInstallInstructionsResult =
                restTemplate.getForEntity(baseURL + "/api/edge/instructions/install/{edgeId}/{method}", EdgeInstructions.class, edgeId.getId(), method);
        return Optional.ofNullable(edgeInstallInstructionsResult.getBody());
    }

    /**
     * 方法说明：
     * 1. 职责：`getEdgeUpgradeInstructions` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<EdgeInstructions> getEdgeUpgradeInstructions(String edgeVersion, String method) {
        ResponseEntity<EdgeInstructions> edgeUpgradeInstructionsResult =
                restTemplate.getForEntity(baseURL + "/api/edge/instructions/upgrade/{edgeVersion}/{method}", EdgeInstructions.class, edgeVersion, method);
        return Optional.ofNullable(edgeUpgradeInstructionsResult.getBody());
    }

    /**
     * 方法说明：
     * 1. 职责：`saveEntitiesVersion` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public UUID saveEntitiesVersion(VersionCreateRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/entities/vc/version", request, UUID.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getVersionCreateRequestStatus` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<VersionCreationResult> getVersionCreateRequestStatus(UUID requestId) {
        try {
            ResponseEntity<VersionCreationResult> versionCreateResult = restTemplate.getForEntity(baseURL + "/api/entities/vc/version/{requestId}/status", VersionCreationResult.class, requestId);
            return Optional.ofNullable(versionCreateResult.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`listEntityVersions` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityVersion> listEntityVersions(EntityId externalEntityId, String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", externalEntityId.getEntityType().name());
        params.put("externalEntityUuid", externalEntityId.getId().toString());
        params.put("branch", branch);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/entities/vc/version/{entityType}/{externalEntityUuid}?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`listEntityTypeVersions` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityVersion> listEntityTypeVersions(EntityType entityType, String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType.name());
        params.put("branch", branch);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/entities/vc/version/{entityType}?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`listVersions` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<EntityVersion> listVersions(String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("branch", branch);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/entities/vc/version?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`listEntitiesAtVersion` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`listAllEntitiesAtVersion` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getEntityDataInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public EntityDataInfo getEntityDataInfo(EntityId externalEntityId, String versionId) {
        return restTemplate.getForEntity(baseURL + "/api/entities/vc/info/{versionId}/{entityType}/{externalEntityUuid}",
                EntityDataInfo.class, versionId, externalEntityId.getEntityType(), externalEntityId.getId()).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`compareEntityDataToVersion` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public EntityDataDiff compareEntityDataToVersion(EntityId internalEntityId, String versionId) {
        return restTemplate.getForEntity(baseURL + "/api/entities/vc/diff/{entityType}/{internalEntityUuid}?versionId={versionId}",
                EntityDataDiff.class, internalEntityId.getEntityType(), internalEntityId.getId(), versionId).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`loadEntitiesVersion` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public UUID loadEntitiesVersion(VersionLoadRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/entities/vc/entity", request, UUID.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getVersionLoadRequestStatus` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<VersionLoadResult> getVersionLoadRequestStatus(UUID requestId) {
        try {
            ResponseEntity<VersionLoadResult> versionLoadResult = restTemplate.getForEntity(baseURL + "/api/entities/vc/entity/{requestId}/status", VersionLoadResult.class, requestId);
            return Optional.ofNullable(versionLoadResult.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`listBranches` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`downloadResource` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getResourceInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getResourceId` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`saveResource` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbResource saveResource(TbResource resource) {
        return restTemplate.postForEntity(
                baseURL + "/api/resource",
                resource,
                TbResource.class
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getResources` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<TbResourceInfo> getResources(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/resource?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TbResourceInfo>>() {
                },
                params
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteResource` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteResource(TbResourceId resourceId) {
        restTemplate.delete("/api/resource/{resourceId}", resourceId.getId().toString());
    }

    /**
     * 方法说明：
     * 1. 职责：`getImageInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbResourceInfo getImageInfo(String type, String key) {
        return restTemplate.getForObject(baseURL + "/api/images/{type}/{key}/info", TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        ));
    }

    /**
     * 方法说明：
     * 1. 职责：`getImages` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<TbResourceInfo> getImages(PageLink pageLink, boolean includeSystemImages) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);
        params.put("includeSystemImages", String.valueOf(includeSystemImages));
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        return restTemplate.exchange(baseURL + "/api/images?includeSystemImages={includeSystemImages}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TbResourceInfo>>() {},
                params
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`uploadImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbResourceInfo uploadImage(String fileName, byte[] data, String contentType, String title) {
        // multipart 请求把二进制文件和元数据放在同一个表单体中，匹配服务端资源/图片/OTA 上传接口。
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(fileName, data, contentType, Map.of(
                "title", Strings.nullToEmpty(title)
        ));
        return restTemplate.postForObject(baseURL + "/api/image", request, TbResourceInfo.class);
    }

    /**
     * 方法说明：
     * 1. 职责：`updateImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbResourceInfo updateImage(String type, String key, String fileName, byte[] data, String contentType) {
        // multipart 请求把二进制文件和元数据放在同一个表单体中，匹配服务端资源/图片/OTA 上传接口。
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(fileName, data, contentType, Map.of());
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}", HttpMethod.PUT, request, TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`updateImageInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbResourceInfo updateImageInfo(String type, String key, TbResourceInfo request) {
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}/info", HttpMethod.PUT, new HttpEntity<>(request), TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`updateImagePublicStatus` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void updateImagePublicStatus(String type, String key, boolean isPublic) {
        restTemplate.put(baseURL + "/api/images/{type}/{key}/public/{isPublic}", null, Map.of(
                "type", type,
                "key", key,
                "isPublic", isPublic
        ));
    }

    /**
     * 方法说明：
     * 1. 职责：`downloadImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public byte[] downloadImage(String type, String key) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/{type}/{key}", HttpMethod.GET, null, Resource.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    /**
     * 方法说明：
     * 1. 职责：`downloadImagePreview` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public byte[] downloadImagePreview(String type, String key) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/{type}/{key}/preview", HttpMethod.GET, null, Resource.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    /**
     * 方法说明：
     * 1. 职责：`downloadPublicImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public byte[] downloadPublicImage(String publicResourceKey) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/public/{publicResourceKey}", HttpMethod.GET, null, Resource.class, Map.of(
                "publicResourceKey", publicResourceKey
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    /**
     * 方法说明：
     * 1. 职责：`exportImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public ImageExportData exportImage(String type, String key) {
        return restTemplate.getForObject(baseURL + "/api/images/{type}/{key}/export", ImageExportData.class, Map.of(
                "type", type,
                "key", key
        ));
    }

    /**
     * 方法说明：
     * 1. 职责：`importImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbResourceInfo importImage(ImageExportData exportData) {
        return restTemplate.exchange(baseURL + "/api/image/import", HttpMethod.PUT, new HttpEntity<>(exportData), TbResourceInfo.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteImage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public TbImageDeleteResult deleteImage(String type, String key, boolean force) {
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}?force={force}", HttpMethod.DELETE, null, TbImageDeleteResult.class, Map.of(
                "type", type,
                "key", key,
                "force", force
        )).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`downloadOtaPackage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getOtaPackageInfoById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`getOtaPackageById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`saveOtaPackageInfo` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl) {
        Map<String, String> params = new HashMap<>();
        params.put("isUrl", Boolean.toString(isUrl));
        return restTemplate.postForEntity(baseURL + "/api/otaPackage?isUrl={isUrl}", otaPackageInfo, OtaPackageInfo.class, params).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`saveOtaPackageData` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public OtaPackageInfo saveOtaPackageData(OtaPackageId otaPackageId, String checkSum, ChecksumAlgorithm checksumAlgorithm, String fileName, byte[] fileBytes) throws Exception {
        // multipart 请求把二进制文件和元数据放在同一个表单体中，匹配服务端资源/图片/OTA 上传接口。
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createMultipartRequest(fileName, fileBytes, null, Collections.emptyMap());

        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());
        params.put("checksumAlgorithm", checksumAlgorithm.name());
        String url = "/api/otaPackage/{otaPackageId}?checksumAlgorithm={checksumAlgorithm}";

        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (checkSum != null) {
            url += "&checkSum={checkSum}";
        }

        return restTemplate.postForEntity(
                baseURL + url, requestEntity, OtaPackageInfo.class, params
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getOtaPackages` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<OtaPackageInfo> getOtaPackages(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/otaPackages?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OtaPackageInfo>>() {
                },
                params
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getOtaPackages` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<OtaPackageInfo> getOtaPackages(DeviceProfileId deviceProfileId,
                                                   OtaPackageType otaPackageType,
                                                   boolean hasData,
                                                   PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("hasData", String.valueOf(hasData));
        params.put("deviceProfileId", deviceProfileId.getId().toString());
        params.put("type", otaPackageType.name());
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/otaPackages/{deviceProfileId}/{type}/{hasData}?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OtaPackageInfo>>() {
                },
                params
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteOtaPackage` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteOtaPackage(OtaPackageId otaPackageId) {
        restTemplate.delete(baseURL + "/api/otaPackage/{otaPackageId}", otaPackageId.getId().toString());
    }

    /**
     * 方法说明：
     * 1. 职责：`getQueuesByServiceType` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public PageData<Queue> getQueuesByServiceType(String serviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("serviceType", serviceType);
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
                baseURL + "/api/queues?serviceType={serviceType}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Queue>>() {
                },
                params
        ).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`getQueueById` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
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
     * 方法说明：
     * 1. 职责：`saveQueue` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Queue saveQueue(Queue queue, String serviceType) {
        return restTemplate.postForEntity(baseURL + "/api/queues?serviceType=" + serviceType, queue, Queue.class).getBody();
    }

    /**
     * 方法说明：
     * 1. 职责：`deleteQueue` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public void deleteQueue(QueueId queueId) {
        restTemplate.delete(baseURL + "/api/queues/" + queueId);
    }

    @Deprecated
    /**
     * 方法说明：
     * 1. 职责：`getAttributes` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    public Optional<JsonNode> getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        Map<String, String> params = new HashMap<>();
        params.put("accessToken", accessToken);
        params.put("clientKeys", clientKeys);
        params.put("sharedKeys", sharedKeys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/v1/{accessToken}/attributes?clientKeys={clientKeys}&sharedKeys={sharedKeys}", JsonNode.class, params);
            return Optional.of(telemetryEntity.getBody());
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (HttpClientErrorException exception) {
            // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`getTimeUrlParams` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private String getTimeUrlParams(TimePageLink pageLink) {
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        String urlParams = getUrlParams(pageLink);
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pageLink.getStartTime() != null) {
            urlParams += "&startTime={startTime}";
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pageLink.getEndTime() != null) {
            urlParams += "&endTime={endTime}";
        }
        return urlParams;
    }

    /**
     * 方法说明：
     * 1. 职责：`getUrlParams` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
    private String getUrlParams(PageLink pageLink) {
        String urlParams = "pageSize={pageSize}&page={page}";
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (!isEmpty(pageLink.getTextSearch())) {
            urlParams += "&textSearch={textSearch}";
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pageLink.getSortOrder() != null) {
            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
        }
        return urlParams;
    }

    /**
     * 方法说明：
     * 1. 职责：`getWidgetTypeInfoPageRequestUrlParams` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private String getWidgetTypeInfoPageRequestUrlParams(Boolean tenantOnly, Boolean fullSearch,
                                                         DeprecatedFilter deprecatedFilter,
                                                         List<String> widgetTypeList) {
        String urlParams = getTenantOnlyAndFullSearchUrlParams(tenantOnly, fullSearch);
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (deprecatedFilter != null) {
            urlParams += "&deprecatedFilter={deprecatedFilter}";
        }
        // 空集合直接返回空结果，避免调用方处理 null，同时表达“服务端无数据”而不是转换失败。
        if (!CollectionUtils.isEmpty(widgetTypeList)) {
            urlParams += "&widgetTypeList={widgetTypeList}";
        }
        return urlParams;
    }

    /**
     * 方法说明：
     * 1. 职责：`getTenantOnlyAndFullSearchUrlParams` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private String getTenantOnlyAndFullSearchUrlParams(Boolean tenantOnly, Boolean fullSearch) {
        String urlParams = "";
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (tenantOnly != null) {
            urlParams = "&tenantOnly={tenantOnly}";
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (fullSearch != null) {
            urlParams += "&fullSearch={fullSearch}";
        }
        return urlParams;
    }

    /**
     * 方法说明：
     * 1. 职责：`addTimePageLinkToParam` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
    private void addTimePageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
        this.addPageLinkToParam(params, pageLink);
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pageLink.getStartTime() != null) {
            params.put("startTime", String.valueOf(pageLink.getStartTime()));
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pageLink.getEndTime() != null) {
            params.put("endTime", String.valueOf(pageLink.getEndTime()));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`addPageLinkToParam` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // 分页参数集中处理，保证所有列表 API 使用同一套 page/pageSize/search/sort URL 约定。
    private void addPageLinkToParam(Map<String, String> params, PageLink pageLink) {
        params.put("pageSize", String.valueOf(pageLink.getPageSize()));
        params.put("page", String.valueOf(pageLink.getPage()));
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (!isEmpty(pageLink.getTextSearch())) {
            params.put("textSearch", pageLink.getTextSearch());
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (pageLink.getSortOrder() != null) {
            params.put("sortProperty", pageLink.getSortOrder().getProperty());
            params.put("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`addWidgetInfoFiltersToParams` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private void addWidgetInfoFiltersToParams(Boolean tenantOnly, Boolean fullSearch, DeprecatedFilter deprecatedFilter,
                                              List<String> widgetTypeList, Map<String, String> params) {
        addTenantOnlyAndFullSearchToParams(tenantOnly, fullSearch, params);
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (deprecatedFilter != null) {
            params.put("deprecatedFilter", deprecatedFilter.name());
        }
        // 空集合直接返回空结果，避免调用方处理 null，同时表达“服务端无数据”而不是转换失败。
        if (!CollectionUtils.isEmpty(widgetTypeList)) {
            params.put("widgetTypeList", listToString(widgetTypeList));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`addTenantOnlyAndFullSearchToParams` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private void addTenantOnlyAndFullSearchToParams(Boolean tenantOnly, Boolean fullSearch, Map<String, String> params) {
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (tenantOnly != null) {
            params.put("tenantOnly", tenantOnly.toString());
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (fullSearch != null) {
            params.put("fullSearch", fullSearch.toString());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`listToString` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private String listToString(List<String> list) {
        return String.join(",", list);
    }

    /**
     * 方法说明：
     * 1. 职责：`listIdsToString` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private String listIdsToString(List<? extends EntityId> list) {
        return listToString(list.stream().map(id -> id.getId().toString()).collect(Collectors.toList()));
    }

    /**
     * 方法说明：
     * 1. 职责：`listEnumToString` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    private String listEnumToString(List<? extends Enum> list) {
        return listToString(list.stream().map(Enum::name).collect(Collectors.toList()));
    }

    /**
     * 方法说明：
     * 1. 职责：`createMultipartRequest` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
    // multipart 请求把二进制文件和元数据放在同一个表单体中，匹配服务端资源/图片/OTA 上传接口。
    private HttpEntity<MultiValueMap<String, Object>> createMultipartRequest(String fileName, byte[] fileData, String fileContentType, Map<String, Object> otherParts) {
        HttpHeaders headers = new HttpHeaders();
        // multipart 请求把二进制文件和元数据放在同一个表单体中，匹配服务端资源/图片/OTA 上传接口。
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=file; filename=" + fileName);
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (fileContentType != null) {
            fileMap.add(HttpHeaders.CONTENT_TYPE, fileContentType);
        }
        // multipart 请求把二进制文件和元数据放在同一个表单体中，匹配服务端资源/图片/OTA 上传接口。
        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(fileData), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.setAll(otherParts);
        body.add("file", fileEntity);
        return new HttpEntity<>(body, headers);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：`close` 执行 REST API 客户端门面 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 SDK 使用方或测试代码创建，登录后在客户端会话期间复用，调用 close 时关闭内部异步执行器；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：调用方登录后通过方法级 API 发起 REST 请求，拦截器在请求前检查 token 过期时间并刷新或重新登录，再把响应 DTO 返回给调用方。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：客户端不控制事务，所有事务边界都在被调用的 ThingsBoard 服务端 Controller/Service/DAO 中。
     * 9. 缓存：客户端只保存 token、过期时间和客户端与服务端时间差，不缓存业务实体，避免客户端读到过期数据。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：通过 REST API 间接读写数据库，数据库连接、事务和一致性由服务端模块负责。
     * 13. Rule Engine：规则链、规则节点和遥测相关 REST 方法可能间接影响 Rule Engine 配置或触发数据流，但本类不执行规则逻辑。
     */
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
