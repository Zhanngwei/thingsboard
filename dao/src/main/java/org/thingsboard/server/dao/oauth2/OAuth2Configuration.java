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
package org.thingsboard.server.dao.oauth2;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "security.oauth2")
@Data
/**
 * 中文说明：
 * 1. 类目的：`OAuth2Configuration` 是 ThingsBoard DAO 模块 中的OAuth2 配置持久化类型，用于维护 OAuth2 客户端、域名映射、登录配置模板和外部用户信息的数据库状态。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括OAuth2Service、Security、UserService、TenantService、缓存和登录流程。
 * 4. 生命周期：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
public class OAuth2Configuration {
    /**
     * 字段说明：
     * 1. 保存 `loginProcessingUrl` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private String loginProcessingUrl;
    private Map<String, String> githubMapper;
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2Configuration` 在 ThingsBoard DAO 模块 中承担OAuth2 配置持久化类型职责，核心目的是维护 OAuth2 客户端、域名映射、登录配置模板和外部用户信息的数据库状态。
 * 2. 核心流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
 * 3. 关键依赖：主要依赖或协作对象包括OAuth2Service、Security、UserService、TenantService、缓存和登录流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
