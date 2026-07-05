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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.JwtTokenAuthenticationProcessingFilter;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenProcessingFilter;
import org.thingsboard.server.service.security.auth.jwt.SkipPathRequestMatcher;
import org.thingsboard.server.service.security.auth.jwt.extractor.TokenExtractor;
import org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationProvider;
import org.thingsboard.server.service.security.auth.rest.RestLoginProcessingFilter;
import org.thingsboard.server.service.security.auth.rest.RestPublicLoginProcessingFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`ThingsboardSecurityConfiguration` 是ThingsBoard Application 模块中的Spring 配置类型，用于声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 生命周期：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Configuration / Factory。
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.BASIC_AUTH_ORDER)
@TbCoreComponent
public class ThingsboardSecurityConfiguration {

    /**
     * 令牌常量，用于统一引用固定值。
     */
    public static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    public static final String JWT_TOKEN_HEADER_PARAM_V2 = "Authorization";
    /**
     * 查询条件常量，用于统一引用固定值。
     */
    public static final String JWT_TOKEN_QUERY_PARAM = "token";

    /**
     * `WEBJARS_ENTRY_POINT`常量，用于统一引用固定值。
     */
    public static final String WEBJARS_ENTRY_POINT = "/webjars/**";
    public static final String DEVICE_API_ENTRY_POINT = "/api/v1/**";
    /**
     * `FORM_BASED_LOGIN_ENTRY_POINT`常量，用于统一引用固定值。
     */
    public static final String FORM_BASED_LOGIN_ENTRY_POINT = "/api/auth/login";
    public static final String PUBLIC_LOGIN_ENTRY_POINT = "/api/auth/login/public";
    /**
     * 令牌常量，用于统一引用固定值。
     */
    public static final String TOKEN_REFRESH_ENTRY_POINT = "/api/auth/token";
    protected static final String[] NON_TOKEN_BASED_AUTH_ENTRY_POINTS = new String[] {"/index.html", "/assets/**", "/static/**", "/api/noauth/**", "/webjars/**",  "/api/license/**", "/api/images/public/**"};
    /**
     * 令牌常量，用于统一引用固定值。
     */
    public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/**";
    public static final String WS_ENTRY_POINT = "/api/ws/**";
    /**
     * `MAIL_OAUTH2_PROCESSING_ENTRY_POINT`常量，用于统一引用固定值。
     */
    public static final String MAIL_OAUTH2_PROCESSING_ENTRY_POINT = "/api/admin/mail/oauth2/code";
    public static final String DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT = "/api/device-connectivity/mqtts/certificate/download";


    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired private ThingsboardErrorResponseHandler restAccessDeniedHandler;

    @Autowired(required = false)
    @Qualifier("oauth2AuthenticationSuccessHandler")
    private AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired(required = false)
    @Qualifier("oauth2AuthenticationFailureHandler")
    private AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    /**
     * 请求，用于读取或保存对应领域对象。
     */
    @Autowired(required = false)
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    @Qualifier("defaultAuthenticationSuccessHandler")
    private AuthenticationSuccessHandler successHandler;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    @Qualifier("defaultAuthenticationFailureHandler")
    private AuthenticationFailureHandler failureHandler;

    /**
     * 令牌，用于认证或安全校验。
     */
    @Autowired private RestAuthenticationProvider restAuthenticationProvider;
    @Autowired private JwtAuthenticationProvider jwtAuthenticationProvider;
    @Autowired private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    @Autowired(required = false) OAuth2Configuration oauth2Configuration;

    @Autowired
    @Qualifier("jwtHeaderTokenExtractor")
    private TokenExtractor jwtHeaderTokenExtractor;

    /**
     * 功能：构建`Etag Filter`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Autowired private AuthenticationManager authenticationManager;

    @Autowired private RateLimitProcessingFilter rateLimitProcessingFilter;

    @Bean
    protected FilterRegistrationBean<ShallowEtagHeaderFilter> buildEtagFilter() throws Exception {
        ShallowEtagHeaderFilter etagFilter = new ShallowEtagHeaderFilter();
        etagFilter.setWriteWeakETag(true);
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean
                = new FilterRegistrationBean<>( etagFilter);
        filterRegistrationBean.addUrlPatterns("*.js","*.css","*.ico","/assets/*","/static/*");
        filterRegistrationBean.setName("etagFilter");
        return filterRegistrationBean;
    }

    /**
     * 功能：构建`Rest Login Processing Filter`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    protected RestLoginProcessingFilter buildRestLoginProcessingFilter() throws Exception {
        RestLoginProcessingFilter filter = new RestLoginProcessingFilter(FORM_BASED_LOGIN_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    /**
     * 功能：构建`Rest Public Login Processing Filter`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    protected RestPublicLoginProcessingFilter buildRestPublicLoginProcessingFilter() throws Exception {
        RestPublicLoginProcessingFilter filter = new RestPublicLoginProcessingFilter(PUBLIC_LOGIN_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    /**
     * 功能：构建令牌。
     * 参数：无。
     * 返回：处理结果。
     */
    protected JwtTokenAuthenticationProcessingFilter buildJwtTokenAuthenticationProcessingFilter() throws Exception {
        List<String> pathsToSkip = new ArrayList<>(Arrays.asList(NON_TOKEN_BASED_AUTH_ENTRY_POINTS));
        pathsToSkip.addAll(Arrays.asList(WS_ENTRY_POINT, TOKEN_REFRESH_ENTRY_POINT, FORM_BASED_LOGIN_ENTRY_POINT,
                PUBLIC_LOGIN_ENTRY_POINT, DEVICE_API_ENTRY_POINT, WEBJARS_ENTRY_POINT, MAIL_OAUTH2_PROCESSING_ENTRY_POINT,
                DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT));
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, TOKEN_BASED_AUTH_ENTRY_POINT);
        JwtTokenAuthenticationProcessingFilter filter
                = new JwtTokenAuthenticationProcessingFilter(failureHandler, jwtHeaderTokenExtractor, matcher);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    /**
     * 功能：构建令牌。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    protected RefreshTokenProcessingFilter buildRefreshTokenProcessingFilter() throws Exception {
        RefreshTokenProcessingFilter filter = new RefreshTokenProcessingFilter(TOKEN_REFRESH_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    /**
     * 功能：执行 `authenticationManager` 对应的处理。
     * 参数：
     * - `objectPostProcessor`：处理器对象。
     * 返回：处理结果。
     */
    @Bean
    public AuthenticationManager authenticationManager(ObjectPostProcessor<Object> objectPostProcessor) throws Exception {
        DefaultAuthenticationEventPublisher eventPublisher = objectPostProcessor
                .postProcess(new DefaultAuthenticationEventPublisher());
        var auth = new AuthenticationManagerBuilder(objectPostProcessor);
        auth.authenticationEventPublisher(eventPublisher);
        auth.authenticationProvider(restAuthenticationProvider);
        auth.authenticationProvider(jwtAuthenticationProvider);
        auth.authenticationProvider(refreshTokenAuthenticationProvider);
        return auth.build();
    }

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Autowired
    private OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver;

    /**
     * 功能：执行 `resources` 对应的处理。
     * 参数：
     * - `http`：`http` 参数。
     * 返回：处理结果。
     */
    @Bean
    @Order(0)
    SecurityFilterChain resources(HttpSecurity http) throws Exception {
        http
                .requestMatchers((matchers) -> matchers.antMatchers("/*.js","/*.css","/*.ico","/assets/**","/static/**"))
                .headers().defaultsDisabled()
                .addHeaderWriter(new StaticHeadersWriter(HttpHeaders.CACHE_CONTROL, "max-age=0, public"))
                .and()
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll())
                .requestCache().disable()
                .securityContext().disable()
                .sessionManagement().disable();
        return http.build();
    }

    /**
     * 功能：执行 `filterChain` 对应的处理。
     * 参数：
     * - `http`：`http` 参数。
     * 返回：处理结果。
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers().cacheControl().and().frameOptions().disable()
                .and()
                .cors()
                .and()
                .csrf().disable()
                .exceptionHandling()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(WEBJARS_ENTRY_POINT).permitAll() // Webjars
                .antMatchers(DEVICE_API_ENTRY_POINT).permitAll() // Device HTTP Transport API
                .antMatchers(FORM_BASED_LOGIN_ENTRY_POINT).permitAll() // Login end-point
                .antMatchers(PUBLIC_LOGIN_ENTRY_POINT).permitAll() // Public login end-point
                .antMatchers(TOKEN_REFRESH_ENTRY_POINT).permitAll() // Token refresh end-point
                .antMatchers(MAIL_OAUTH2_PROCESSING_ENTRY_POINT).permitAll() // Mail oauth2 code processing url
                .antMatchers(DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT).permitAll() // Mail oauth2 code processing url
                .antMatchers(NON_TOKEN_BASED_AUTH_ENTRY_POINTS).permitAll() // static resources, user activation and password reset end-points
                .and()
                .authorizeRequests()
                .antMatchers(WS_ENTRY_POINT).permitAll() // WebSocket API End-points
                .antMatchers(TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected API End-points
                .and()
                .exceptionHandling().accessDeniedHandler(restAccessDeniedHandler)
                .and()
                .addFilterBefore(buildRestLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildRestPublicLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildRefreshTokenProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitProcessingFilter, UsernamePasswordAuthenticationFilter.class);
        if (oauth2Configuration != null) {
            http.oauth2Login()
                    .authorizationEndpoint()
                    .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                    .authorizationRequestResolver(oAuth2AuthorizationRequestResolver)
                    .and()
                    .loginPage("/oauth2Login")
                    .loginProcessingUrl(oauth2Configuration.getLoginProcessingUrl())
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .failureHandler(oauth2AuthenticationFailureHandler);
        }
        return http.build();
    }

    /**
     * 功能：执行 `corsFilter` 对应的处理。
     * 参数：
     * - `mvcCorsProperties`：`mvcCorsProperties` 参数。
     * 返回：处理结果。
     */
    @Bean
    @ConditionalOnMissingBean(CorsFilter.class)
    public CorsFilter corsFilter(@Autowired MvcCorsProperties mvcCorsProperties) {
        if (mvcCorsProperties.getMappings().size() == 0) {
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        } else {
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.setCorsConfigurations(mvcCorsProperties.getMappings());
            return new CorsFilter(source);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardSecurityConfiguration` 在 ThingsBoard Application 模块 中承担Spring 配置类型职责，核心目的是声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 核心流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
