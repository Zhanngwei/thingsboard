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
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseComponentDescriptorService` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service
@Slf4j
public class BaseComponentDescriptorService implements ComponentDescriptorService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private ComponentDescriptorDao componentDescriptorDao;

    /**
     * 校验器，封装可复用的处理规则。
     */
    @Autowired
    private DataValidator<ComponentDescriptor> componentValidator;

    /**
     * 功能：保存或创建`Component`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `component`：`component` 参数。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor saveComponent(TenantId tenantId, ComponentDescriptor component) {
        componentValidator.validate(component, data -> TenantId.SYS_TENANT_ID);
        Optional<ComponentDescriptor> result = componentDescriptorDao.saveIfNotExist(tenantId, component);
        return result.orElseGet(() -> componentDescriptorDao.findByClazz(tenantId, component.getClazz()));
    }

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `componentId`：`componentId`ID。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId) {
        Validator.validateId(componentId, "Incorrect component id for search request.");
        return componentDescriptorDao.findById(tenantId, componentId);
    }

    /**
     * 功能：获取`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor findByClazz(TenantId tenantId, String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for search request.");
        return componentDescriptorDao.findByClazz(tenantId, clazz);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByTypeAndPageLink(tenantId, type, pageLink);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scope`：`scope` 参数。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByScopeAndTypeAndPageLink(tenantId, scope, type, pageLink);
    }

    /**
     * 功能：删除或清理`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    @Override
    public void deleteByClazz(TenantId tenantId, String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for delete request.");
        componentDescriptorDao.deleteByClazz(tenantId, clazz);
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `component`：`component` 参数。
     * - `configuration`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean validate(TenantId tenantId, ComponentDescriptor component, JsonNode configuration) {
        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
        try {
            if (!component.getConfigurationDescriptor().has("schema")) {
                throw new DataValidationException("Configuration descriptor doesn't contain schema property!");
            }
            JsonNode configurationSchema = component.getConfigurationDescriptor().get("schema");
            ProcessingReport report = validator.validate(configurationSchema, configuration);
            return report.isSuccess();
        } catch (ProcessingException e) {
            throw new IncorrectParameterException(e.getMessage(), e);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseComponentDescriptorService` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
