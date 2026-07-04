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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.common.data.oauth2.OAuth2MobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.common.data.oauth2.TenantNameStrategyType;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
/**
 * 中文说明：
 * 1. 类目的：`OAuth2ServiceImpl` 是 ThingsBoard DAO 模块 中的OAuth2 配置持久化类型，用于维护 OAuth2 客户端、域名映射、登录配置模板和外部用户信息的数据库状态。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括OAuth2Service、Security、UserService、TenantService、缓存和登录流程。
 * 4. 生命周期：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
public class OAuth2ServiceImpl extends AbstractEntityService implements OAuth2Service {
    /**
     * 字段说明：
     * 1. 保存 `INCORRECT_TENANT_ID` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CLIENT_REGISTRATION_ID = "Incorrect clientRegistrationId ";
    /**
     * 字段说明：
     * 1. 保存 `INCORRECT_DOMAIN_NAME` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String INCORRECT_DOMAIN_NAME = "Incorrect domainName ";
    public static final String INCORRECT_DOMAIN_SCHEME = "Incorrect domainScheme ";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `oauth2ParamsDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private OAuth2ParamsDao oauth2ParamsDao;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `oauth2RegistrationDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private OAuth2RegistrationDao oauth2RegistrationDao;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `oauth2DomainDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private OAuth2DomainDao oauth2DomainDao;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `oauth2MobileDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private OAuth2MobileDao oauth2MobileDao;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getOAuth2Clients` 对应的OAuth2 配置持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public List<OAuth2ClientInfo> getOAuth2Clients(String domainSchemeStr, String domainName, String pkgName, PlatformType platformType) {
        log.trace("Executing getOAuth2Clients [{}://{}] pkgName=[{}] platformType=[{}]", domainSchemeStr, domainName, pkgName, platformType);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (domainSchemeStr == null) {
            throw new IncorrectParameterException(INCORRECT_DOMAIN_SCHEME);
        }
        SchemeType domainScheme;
        try {
            domainScheme = SchemeType.valueOf(domainSchemeStr.toUpperCase());
        // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
        } catch (IllegalArgumentException e){
            throw new IncorrectParameterException(INCORRECT_DOMAIN_SCHEME);
        }
        validateString(domainName, INCORRECT_DOMAIN_NAME + domainName);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return oauth2RegistrationDao.findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(
                Arrays.asList(domainScheme, SchemeType.MIXED), domainName, pkgName, platformType)
                .stream()
                .map(OAuth2Utils::toClientInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    /**
     * 方法说明：
     * 1. 职责：执行 `saveOAuth2Info` 对应的OAuth2 配置持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void saveOAuth2Info(OAuth2Info oauth2Info) {
        log.trace("Executing saveOAuth2Info [{}]", oauth2Info);
        oauth2InfoValidator.accept(oauth2Info);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        oauth2ParamsDao.deleteAll();
        oauth2Info.getOauth2ParamsInfos().forEach(oauth2ParamsInfo -> {
            OAuth2Params oauth2Params = OAuth2Utils.infoToOAuth2Params(oauth2Info);
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            OAuth2Params savedOauth2Params = oauth2ParamsDao.save(TenantId.SYS_TENANT_ID, oauth2Params);
            oauth2ParamsInfo.getClientRegistrations().forEach(registrationInfo -> {
                OAuth2Registration registration = OAuth2Utils.toOAuth2Registration(savedOauth2Params.getId(), registrationInfo);
                // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
                oauth2RegistrationDao.save(TenantId.SYS_TENANT_ID, registration);
            });
            oauth2ParamsInfo.getDomainInfos().forEach(domainInfo -> {
                OAuth2Domain domain = OAuth2Utils.toOAuth2Domain(savedOauth2Params.getId(), domainInfo);
                // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
                oauth2DomainDao.save(TenantId.SYS_TENANT_ID, domain);
            });
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (oauth2ParamsInfo.getMobileInfos() != null) {
                oauth2ParamsInfo.getMobileInfos().forEach(mobileInfo -> {
                    OAuth2Mobile mobile = OAuth2Utils.toOAuth2Mobile(savedOauth2Params.getId(), mobileInfo);
                    // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
                    oauth2MobileDao.save(TenantId.SYS_TENANT_ID, mobile);
                });
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findOAuth2Info` 对应的OAuth2 配置持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public OAuth2Info findOAuth2Info() {
        log.trace("Executing findOAuth2Info");
        OAuth2Info oauth2Info = new OAuth2Info();
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        List<OAuth2Params> oauth2ParamsList = oauth2ParamsDao.find(TenantId.SYS_TENANT_ID);
        oauth2Info.setEnabled(oauth2ParamsList.stream().anyMatch(param -> param.isEnabled()));
        List<OAuth2ParamsInfo> oauth2ParamsInfos = new ArrayList<>();
        oauth2Info.setOauth2ParamsInfos(oauth2ParamsInfos);
        oauth2ParamsList.stream().sorted(Comparator.comparing(BaseData::getUuidId)).forEach(oauth2Params -> {
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            List<OAuth2Registration> registrations = oauth2RegistrationDao.findByOAuth2ParamsId(oauth2Params.getId().getId());
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            List<OAuth2Domain> domains = oauth2DomainDao.findByOAuth2ParamsId(oauth2Params.getId().getId());
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            List<OAuth2Mobile> mobiles = oauth2MobileDao.findByOAuth2ParamsId(oauth2Params.getId().getId());
            oauth2ParamsInfos.add(OAuth2Utils.toOAuth2ParamsInfo(registrations, domains, mobiles));
        });
        return oauth2Info;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findRegistration` 对应的OAuth2 配置持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public OAuth2Registration findRegistration(UUID id) {
        log.trace("Executing findRegistration [{}]", id);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return oauth2RegistrationDao.findById(null, id);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAppSecret` 对应的OAuth2 配置持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public String findAppSecret(UUID id, String pkgName) {
        log.trace("Executing findAppSecret [{}][{}]", id, pkgName);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        validateString(pkgName, "Incorrect package name");
        return oauth2RegistrationDao.findAppSecret(id, pkgName);
    }


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAllRegistrations` 对应的OAuth2 配置持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public List<OAuth2Registration> findAllRegistrations() {
        log.trace("Executing findAllRegistrations");
        return oauth2RegistrationDao.find(TenantId.SYS_TENANT_ID);
    }

    private final Consumer<OAuth2Info> oauth2InfoValidator = oauth2Info -> {
        if (oauth2Info == null
                || oauth2Info.getOauth2ParamsInfos() == null) {
            throw new DataValidationException("OAuth2 param infos should be specified!");
        }
        for (OAuth2ParamsInfo oauth2Params : oauth2Info.getOauth2ParamsInfos()) {
            if (oauth2Params.getDomainInfos() == null
                    || oauth2Params.getDomainInfos().isEmpty()) {
                throw new DataValidationException("List of domain configuration should be specified!");
            }
            for (OAuth2DomainInfo domainInfo : oauth2Params.getDomainInfos()) {
                if (StringUtils.isEmpty(domainInfo.getName())) {
                    throw new DataValidationException("Domain name should be specified!");
                }
                if (domainInfo.getScheme() == null) {
                    throw new DataValidationException("Domain scheme should be specified!");
                }
            }
            oauth2Params.getDomainInfos().stream()
                    .collect(Collectors.groupingBy(OAuth2DomainInfo::getName))
                    .forEach((domainName, domainInfos) -> {
                        if (domainInfos.size() > 1 && domainInfos.stream().anyMatch(domainInfo -> domainInfo.getScheme() == SchemeType.MIXED)) {
                            throw new DataValidationException("MIXED scheme type shouldn't be combined with another scheme type!");
                        }
                        domainInfos.stream()
                                .collect(Collectors.groupingBy(OAuth2DomainInfo::getScheme))
                                .forEach((schemeType, domainInfosBySchemeType) -> {
                                    if (domainInfosBySchemeType.size() > 1) {
                                        throw new DataValidationException("Domain name and protocol must be unique within OAuth2 parameters!");
                                    }
                                });
                    });
            if (oauth2Params.getMobileInfos() != null) {
                for (OAuth2MobileInfo mobileInfo : oauth2Params.getMobileInfos()) {
                    if (StringUtils.isEmpty(mobileInfo.getPkgName())) {
                        throw new DataValidationException("Package should be specified!");
                    }
                    if (StringUtils.isEmpty(mobileInfo.getAppSecret())) {
                        throw new DataValidationException("Application secret should be specified!");
                    }
                    if (mobileInfo.getAppSecret().length() < 16) {
                        throw new DataValidationException("Application secret should be at least 16 characters!");
                    }
                }
                oauth2Params.getMobileInfos().stream()
                        .collect(Collectors.groupingBy(OAuth2MobileInfo::getPkgName))
                        .forEach((pkgName, mobileInfos) -> {
                            if (mobileInfos.size() > 1) {
                                throw new DataValidationException("Mobile app package name must be unique within OAuth2 parameters!");
                            }
                        });
            }
            if (oauth2Params.getClientRegistrations() == null || oauth2Params.getClientRegistrations().isEmpty()) {
                throw new DataValidationException("Client registrations should be specified!");
            }
            for (OAuth2RegistrationInfo clientRegistration : oauth2Params.getClientRegistrations()) {
                if (StringUtils.isEmpty(clientRegistration.getClientId())) {
                    throw new DataValidationException("Client ID should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientSecret())) {
                    throw new DataValidationException("Client secret should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAuthorizationUri())) {
                    throw new DataValidationException("Authorization uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAccessTokenUri())) {
                    throw new DataValidationException("Token uri should be specified!");
                }
                if (CollectionUtils.isEmpty(clientRegistration.getScope())) {
                    throw new DataValidationException("Scope should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getUserNameAttributeName())) {
                    throw new DataValidationException("User name attribute name should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientAuthenticationMethod())) {
                    throw new DataValidationException("Client authentication method should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getLoginButtonLabel())) {
                    throw new DataValidationException("Login button label should be specified!");
                }
                OAuth2MapperConfig mapperConfig = clientRegistration.getMapperConfig();
                if (mapperConfig == null) {
                    throw new DataValidationException("Mapper config should be specified!");
                }
                if (mapperConfig.getType() == null) {
                    throw new DataValidationException("Mapper config type should be specified!");
                }
                if (mapperConfig.getType() == MapperType.BASIC) {
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
                    if (basicConfig == null) {
                        throw new DataValidationException("Basic config should be specified!");
                    }
                    if (StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                        throw new DataValidationException("Email attribute key should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == null) {
                        throw new DataValidationException("Tenant name strategy should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                            && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                        throw new DataValidationException("Tenant name pattern should be specified!");
                    }
                }
                if (mapperConfig.getType() == MapperType.GITHUB) {
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
                    if (basicConfig == null) {
                        throw new DataValidationException("Basic config should be specified!");
                    }
                    if (!StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                        throw new DataValidationException("Email attribute key cannot be configured for GITHUB mapper type!");
                    }
                    if (basicConfig.getTenantNameStrategy() == null) {
                        throw new DataValidationException("Tenant name strategy should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                            && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                        throw new DataValidationException("Tenant name pattern should be specified!");
                    }
                }
                if (mapperConfig.getType() == MapperType.CUSTOM) {
                    OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
                    if (customConfig == null) {
                        throw new DataValidationException("Custom config should be specified!");
                    }
                    if (StringUtils.isEmpty(customConfig.getUrl())) {
                        throw new DataValidationException("Custom mapper URL should be specified!");
                    }
                }
            }
        }
    };
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2ServiceImpl` 在 ThingsBoard DAO 模块 中承担OAuth2 配置持久化类型职责，核心目的是维护 OAuth2 客户端、域名映射、登录配置模板和外部用户信息的数据库状态。
 * 2. 核心流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
 * 3. 关键依赖：主要依赖或协作对象包括OAuth2Service、Security、UserService、TenantService、缓存和登录流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
