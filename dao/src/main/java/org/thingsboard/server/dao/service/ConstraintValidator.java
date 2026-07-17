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
 * 1. `ConstraintValidator` 是 ThingsBoard DAO 中负责 `Constraint Validator` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
