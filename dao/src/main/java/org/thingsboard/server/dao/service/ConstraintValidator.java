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
package org.thingsboard.server.dao.service;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.internal.cfg.context.DefaultConstraintMapping;
import org.hibernate.validator.internal.engine.ConfigurationImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;
import org.thingsboard.server.dao.exception.DataValidationException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.AssertTrue;
import javax.validation.metadata.ConstraintDescriptor;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`ConstraintValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@Slf4j
@Configuration
public class ConstraintValidator {

    /**
     * 校验器，封装可复用的处理规则。
     */
    private static Validator fieldsValidator;

    static {
        initializeValidators();
    }

    /**
     * 功能：校验`Fields`。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    public static void validateFields(Object data) {
        validateFields(data, "Validation error: ");
    }

    /**
     * 功能：校验`Fields`。
     * 参数：
     * - `data`：待处理数据。
     * - `errorPrefix`：错误信息。
     * 返回：无。
     */
    public static void validateFields(Object data, String errorPrefix) {
        Set<ConstraintViolation<Object>> constraintsViolations = fieldsValidator.validate(data);
        if (!constraintsViolations.isEmpty()) {
            throw new DataValidationException(errorPrefix + getErrorMessage(constraintsViolations));
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `constraintsViolations`：数据列表。
     * 返回：文本结果。
     */
    public static String getErrorMessage(Collection<ConstraintViolation<Object>> constraintsViolations) {
        return constraintsViolations.stream()
                .map(ConstraintValidator::getErrorMessage)
                .distinct().sorted().collect(Collectors.joining(", "));
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `constraintViolation`：`constraintViolation` 参数。
     * 返回：文本结果。
     */
    public static String getErrorMessage(ConstraintViolation<Object> constraintViolation) {
        ConstraintDescriptor<?> constraintDescriptor = constraintViolation.getConstraintDescriptor();
        String property = (String) constraintDescriptor.getAttributes().get("fieldName");
        if (StringUtils.isEmpty(property) && !(constraintDescriptor.getAnnotation() instanceof AssertTrue)) {
            property = Iterators.getLast(constraintViolation.getPropertyPath().iterator()).toString();
        }

        String error = "";
        if (StringUtils.isNotEmpty(property)) {
            error += property + " ";
        }
        error += constraintViolation.getMessage();
        return error;
    }

    /**
     * 功能：执行 `initializeValidators` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private static void initializeValidators() {
        HibernateValidatorConfiguration validatorConfiguration = Validation.byProvider(HibernateValidator.class).configure();

        ConstraintMapping constraintMapping = getCustomConstraintMapping();
        validatorConfiguration.addMapping(constraintMapping);

        fieldsValidator = validatorConfiguration.buildValidatorFactory().getValidator();
    }

    /**
     * 功能：执行 `validatorFactoryBean` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setConfigurationInitializer(configuration -> {
            ((ConfigurationImpl) configuration).addMapping(getCustomConstraintMapping());
        });
        return localValidatorFactoryBean;
    }

    /**
     * 功能：获取`Custom Constraint Mapping`。
     * 参数：无。
     * 返回：键值映射结果。
     */
    private static ConstraintMapping getCustomConstraintMapping() {
        ConstraintMapping constraintMapping = new DefaultConstraintMapping();
        constraintMapping.constraintDefinition(NoXss.class).validatedBy(NoXssValidator.class);
        constraintMapping.constraintDefinition(Length.class).validatedBy(StringLengthValidator.class);
        return constraintMapping;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ConstraintValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
