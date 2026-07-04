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
package org.thingsboard.server.config;

import com.fasterxml.classmate.TypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.ThingsboardCredentialsExpiredResponse;
import org.thingsboard.server.exception.ThingsboardErrorResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;
import org.thingsboard.server.service.security.auth.rest.LoginResponse;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ExampleBuilder;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.RepresentationBuilder;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.builders.ResponseBuilder;
import springfox.documentation.schema.Example;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiListing;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.HttpLoginPasswordScheme;
import springfox.documentation.service.ParameterType;
import springfox.documentation.service.Response;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.ApiListingScannerPlugin;
import springfox.documentation.spi.service.contexts.ApiListingContext;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.readers.operation.CachingOperationNameGenerator;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.ModelRendering;
import springfox.documentation.swagger.web.OperationsSorter;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.function.Predicate.not;
import static springfox.documentation.builders.PathSelectors.any;
import static springfox.documentation.builders.PathSelectors.regex;

@Slf4j
@Configuration
@TbCoreComponent
@Profile("!test")
/**
 * 中文说明：
 * 1. 类目的：`SwaggerConfiguration` 是ThingsBoard Application 模块中的Spring 配置类型，用于声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 生命周期：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Configuration / Factory。
 */
public class SwaggerConfiguration {

    @Value("${swagger.enabled:true}")
    /**
     * 字段说明：
     * 1. 保存 `enabled` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean enabled;
    @Value("${swagger.api_path_regex}")
    /**
     * 字段说明：
     * 1. 保存 `apiPathRegex` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String apiPathRegex;
    @Value("${swagger.security_path_regex}")
    /**
     * 字段说明：
     * 1. 保存 `securityPathRegex` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String securityPathRegex;
    @Value("${swagger.non_security_path_regex}")
    /**
     * 字段说明：
     * 1. 保存 `nonSecurityPathRegex` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String nonSecurityPathRegex;
    @Value("${swagger.title}")
    /**
     * 字段说明：
     * 1. 保存 `title` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String title;
    @Value("${swagger.description}")
    /**
     * 字段说明：
     * 1. 保存 `description` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String description;
    @Value("${swagger.contact.name}")
    /**
     * 字段说明：
     * 1. 保存 `contactName` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String contactName;
    @Value("${swagger.contact.url}")
    /**
     * 字段说明：
     * 1. 保存 `contactUrl` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String contactUrl;
    @Value("${swagger.contact.email}")
    /**
     * 字段说明：
     * 1. 保存 `contactEmail` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String contactEmail;
    @Value("${swagger.license.title}")
    /**
     * 字段说明：
     * 1. 保存 `licenseTitle` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String licenseTitle;
    @Value("${swagger.license.url}")
    /**
     * 字段说明：
     * 1. 保存 `licenseUrl` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String licenseUrl;
    @Value("${swagger.version}")
    /**
     * 字段说明：
     * 1. 保存 `version` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String version;
    @Value("${app.version:unknown}")
    /**
     * 字段说明：
     * 1. 保存 `appVersion` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String appVersion;

    @Bean
    /**
     * 方法说明：
     * 1. 职责：执行 `thingsboardApi` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Docket thingsboardApi() {
        TypeResolver typeResolver = new TypeResolver();
        return new Docket(DocumentationType.OAS_30)
                .enable(enabled)
                .groupName("thingsboard")
                .apiInfo(apiInfo())
                .additionalModels(
                        typeResolver.resolve(ThingsboardErrorResponse.class),
                        typeResolver.resolve(ThingsboardCredentialsExpiredResponse.class),
                        typeResolver.resolve(LoginRequest.class),
                        typeResolver.resolve(LoginResponse.class)
                )
                .select()
                .paths(apiPaths())
                .paths(any())
                .build()
                .globalResponses(HttpMethod.GET,
                        defaultErrorResponses(false)
                )
                .globalResponses(HttpMethod.POST,
                        defaultErrorResponses(true)
                )
                .globalResponses(HttpMethod.DELETE,
                        defaultErrorResponses(false)
                )
                .securitySchemes(newArrayList(httpLogin()))
                .securityContexts(newArrayList(securityContext()))
                .ignoredParameterTypes(AuthenticationPrincipal.class)
                .enableUrlTemplating(true);
    }

    @Bean
    @Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
    /**
     * 方法说明：
     * 1. 职责：执行 `loginEndpointListingScanner` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    ApiListingScannerPlugin loginEndpointListingScanner(final CachingOperationNameGenerator operationNames) {
        return new ApiListingScannerPlugin() {
            @Override
            public List<ApiDescription> apply(DocumentationContext context) {
                return List.of(loginEndpointApiDescription(operationNames));
            }

            @Override
            public boolean supports(DocumentationType delimiter) {
                return DocumentationType.SWAGGER_2.equals(delimiter) || DocumentationType.OAS_30.equals(delimiter);
            }
        };
    }

    @Bean
    @Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
    /**
     * 方法说明：
     * 1. 职责：执行 `loginEndpointListingBuilder` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    ApiListingBuilderPlugin loginEndpointListingBuilder() {
        return new ApiListingBuilderPlugin() {
            @Override
            public void apply(ApiListingContext apiListingContext) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (apiListingContext.getResourceGroup().getGroupName().equals("default")) {
                    ApiListing apiListing = apiListingContext.apiListingBuilder().build();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (apiListing.getResourcePath().equals("/api/auth/login")) {
                        apiListingContext.apiListingBuilder().tags(Set.of(new Tag("login-endpoint", "Login Endpoint")));
                        apiListingContext.apiListingBuilder().description("Login Endpoint");
                    }
                }
            }

            @Override
            public boolean supports(DocumentationType delimiter) {
                return DocumentationType.SWAGGER_2.equals(delimiter) || DocumentationType.OAS_30.equals(delimiter);
            }
        };
    }

    @Bean
    /**
     * 方法说明：
     * 1. 职责：执行 `uiConfig` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    UiConfiguration uiConfig() {
        return UiConfigurationBuilder.builder()
                .deepLinking(true)
                .displayOperationId(false)
                .defaultModelsExpandDepth(1)
                .defaultModelExpandDepth(1)
                .defaultModelRendering(ModelRendering.EXAMPLE)
                .displayRequestDuration(false)
                .docExpansion(DocExpansion.LIST)
                .filter(false)
                .maxDisplayedTags(null)
                .operationsSorter(OperationsSorter.ALPHA)
                .showExtensions(false)
                .showCommonExtensions(false)
                .supportedSubmitMethods(UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS)
                .validatorUrl(null)
                .persistAuthorization(true)
                .syntaxHighlightActivate(true)
                .syntaxHighlightTheme("agate")
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `httpLogin` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private SecurityScheme httpLogin() {
        return HttpLoginPasswordScheme
                .X_AUTHORIZATION_BUILDER
                .loginEndpoint("/api/auth/login")
                .name("HTTP login form")
                .description("Enter Username / Password")
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `securityContext` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .operationSelector(securityPathOperationSelector())
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `apiPaths` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Predicate<String> apiPaths() {
        return regex(apiPathRegex);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `securityPathOperationSelector` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Predicate<OperationContext> securityPathOperationSelector() {
        return new SecurityPathOperationSelector(securityPathRegex, nonSecurityPathRegex);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `defaultAuth` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    List<SecurityReference> defaultAuth() {
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[3];
        authorizationScopes[0] = new AuthorizationScope(Authority.SYS_ADMIN.name(), "System administrator");
        authorizationScopes[1] = new AuthorizationScope(Authority.TENANT_ADMIN.name(), "Tenant administrator");
        authorizationScopes[2] = new AuthorizationScope(Authority.CUSTOMER_USER.name(), "Customer");
        return newArrayList(
                new SecurityReference("HTTP login form", authorizationScopes));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `apiInfo` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ApiInfo apiInfo() {
        String apiVersion = version;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isEmpty(apiVersion)) {
            apiVersion = appVersion;
        }
        return new ApiInfoBuilder()
                .title(title)
                .description(description)
                .contact(new Contact(contactName, contactUrl, contactEmail))
                .license(licenseTitle)
                .licenseUrl(licenseUrl)
                .version(apiVersion)
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `loginEndpointApiDescription` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ApiDescription loginEndpointApiDescription(final CachingOperationNameGenerator operationNames) {
        return new ApiDescription(null, "/api/auth/login", "Login method to get user JWT token data", "Login endpoint", Collections.singletonList(
                new OperationBuilder(operationNames)
                        .summary("Login method to get user JWT token data")
                        .tags(Set.of("login-endpoint"))
                        .authorizations(new ArrayList<>())
                        .position(0)
                        .codegenMethodNameStem("loginPost")
                        .method(HttpMethod.POST)
                        .notes("Login method used to authenticate user and get JWT token data.\n\nValue of the response **token** " +
                                "field can be used as **X-Authorization** header value:\n\n`X-Authorization: Bearer $JWT_TOKEN_VALUE`.")
                        .requestParameters(
                                List.of(
                                        new RequestParameterBuilder()
                                                .in(ParameterType.BODY)
                                                .required(true)
                                                .description("Login request")
                                                .content(c ->
                                                         c.requestBody(true)
                                                          .representation(MediaType.APPLICATION_JSON)
                                                          .apply(classRepresentation(LoginRequest.class, false))
                                                )
                                                .build()
                                )
                        )
                        .responses(loginResponses())
                        .build()
        ), false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `loginResponses` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Collection<Response> loginResponses() {
        List<Response> responses = new ArrayList<>();
        responses.add(
                new ResponseBuilder()
                        .code("200")
                        .description("OK")
                        .representation(MediaType.APPLICATION_JSON)
                        .apply(classRepresentation(LoginResponse.class, true)).
                        build()
        );
        responses.addAll(loginErrorResponses());
        return responses;
    }

    /** Helper methods **/

    /**
     * 方法说明：
     * 1. 职责：执行 `defaultErrorResponses` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<Response> defaultErrorResponses(boolean isPost) {
        return List.of(
                errorResponse("400", "Bad Request",
                        ThingsboardErrorResponse.of(isPost ? "Invalid request body" : "Invalid UUID string: 123", ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.BAD_REQUEST)),
                errorResponse("401", "Unauthorized",
                        ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                errorResponse("403", "Forbidden",
                        ThingsboardErrorResponse.of("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN)),
                errorResponse("404", "Not Found",
                        ThingsboardErrorResponse.of("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND)),
                errorResponse("429", "Too Many Requests",
                        ThingsboardErrorResponse.of("Too many requests for current tenant!",
                        ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS))
        );
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `loginErrorResponses` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<Response> loginErrorResponses() {
        return List.of(
                errorResponse("401", "Unauthorized",
                        List.of(
                                errorExample("bad-credentials", "Bad credentials",
                                    ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                                 errorExample("token-expired", "JWT token expired",
                                    ThingsboardErrorResponse.of("Token has expired", ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED)),
                                errorExample("account-disabled", "Disabled account",
                                    ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                                errorExample("account-locked", "Locked account",
                                    ThingsboardErrorResponse.of("User account is locked due to security policy", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                                errorExample("authentication-failed", "General authentication error",
                                    ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED))
                        )
                ),
                errorResponse("401 ", "Unauthorized (**Expired credentials**)",
                        List.of(
                                errorExample("credentials-expired", "Expired credentials",
                                        ThingsboardCredentialsExpiredResponse.of("User password expired!", StringUtils.randomAlphanumeric(30)))
                        ), ThingsboardCredentialsExpiredResponse.class
                )
        );
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `errorResponse` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Response errorResponse(String code, String description, ThingsboardErrorResponse example) {
        return errorResponse(code, description,  List.of(errorExample("error-code-" + code, description, example)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `errorResponse` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Response errorResponse(String code, String description, List<Example> examples) {
        return errorResponse(code, description, examples, ThingsboardErrorResponse.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `errorResponse` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Response errorResponse(String code, String description, List<Example> examples,
                                   Class<? extends ThingsboardErrorResponse> errorResponseClass) {
        return new ResponseBuilder()
                .code(code)
                .description(description)
                .examples(examples)
                .representation(MediaType.APPLICATION_JSON)
                .apply(classRepresentation(errorResponseClass, true))
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `errorExample` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Example errorExample(String id, String summary, ThingsboardErrorResponse example) {
        return new ExampleBuilder()
                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                .summary(summary)
                .id(id)
                .value(example).build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `classRepresentation` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Consumer<RepresentationBuilder> classRepresentation(Class<?> clazz, boolean isResponse) {
        return r -> r.model(
                m ->
                        m.referenceModel(ref ->
                                ref.key(k ->
                                        k.qualifiedModelName(q ->
                                                q.namespace(clazz.getPackageName())
                                                        .name(clazz.getSimpleName())).isResponse(isResponse)))
        );
    }

    /**
     * 中文说明：
     * 1. 类目的：`SecurityPathOperationSelector` 是ThingsBoard Application 模块中的Spring 配置类型，用于声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
     * 4. 生命周期：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Configuration / Factory。
     */
    private static class SecurityPathOperationSelector implements Predicate<OperationContext> {

        /**
         * 字段说明：
         * 1. 保存 `securityPathSelector` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final Predicate<String> securityPathSelector;

        SecurityPathOperationSelector(String securityPathRegex, String nonSecurityPathRegex) {
            this.securityPathSelector = regex(securityPathRegex).and(
                not(
                    regex(nonSecurityPathRegex)
            ));
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `test` 对应的Spring 配置类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public boolean test(OperationContext operationContext) {
            return this.securityPathSelector.test(operationContext.requestMappingPattern());
        }
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`SwaggerConfiguration` 在 ThingsBoard Application 模块 中承担Spring 配置类型职责，核心目的是声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 核心流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
