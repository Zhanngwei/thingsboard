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
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509BootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DeviceCredentialsServiceImpl` 是 ThingsBoard DAO 模块 中的设备持久化服务类型，用于维护设备、凭据、配置、声明、连接状态和设备画像相关的数据库访问流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DeviceService、DeviceCredentialsService、Transport、Rule Engine、缓存和审计服务。
 * 4. 生命周期：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Factory。
 */
public class DeviceCredentialsServiceImpl extends AbstractCachedEntityService<String, DeviceCredentials, DeviceCredentialsEvictEvent> implements DeviceCredentialsService {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceCredentialsDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DeviceCredentialsDao deviceCredentialsDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `credentialsValidator` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DataValidator<DeviceCredentials> credentialsValidator;

    @TransactionalEventListener(classes = DeviceCredentialsEvictEvent.class)
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleEvictEvent` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void handleEvictEvent(DeviceCredentialsEvictEvent event) {
        // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
        cache.evict(event.getNewCedentialsId());
        // 审计或事件记录用于保留业务变更轨迹，便于后续查询、告警或外部系统消费。
        if (StringUtils.isNotEmpty(event.getOldCredentialsId()) && !event.getNewCedentialsId().equals(event.getOldCredentialsId())) {
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            cache.evict(event.getOldCredentialsId());
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findDeviceCredentialsByDeviceId` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceCredentialsByDeviceId [{}]", deviceId);
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return deviceCredentialsDao.findByDeviceId(tenantId, deviceId.getId());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findDeviceCredentialsByCredentialsId` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId) {
        log.trace("Executing findDeviceCredentialsByCredentialsId [{}]", credentialsId);
        validateString(credentialsId, "Incorrect credentialsId " + credentialsId);
        // 事务边界决定本次数据库写入与缓存失效是否作为一个原子业务步骤提交。
        return cache.getAndPutInTransaction(credentialsId,
                // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
                () -> deviceCredentialsDao.findByCredentialsId(TenantId.SYS_TENANT_ID, credentialsId),
                true); // caching null values is essential for permanently invalid requests
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateDeviceCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials) {
        return saveOrUpdate(tenantId, deviceCredentials);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `createDeviceCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials) {
        return saveOrUpdate(tenantId, deviceCredentials);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveOrUpdate` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private DeviceCredentials saveOrUpdate(TenantId tenantId, DeviceCredentials deviceCredentials) {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceCredentials.getCredentialsType() == null) {
            throw new DataValidationException("Device credentials type should be specified");
        }
        formatCredentials(deviceCredentials);
        log.trace("Executing updateDeviceCredentials [{}]", deviceCredentials);
        credentialsValidator.validate(deviceCredentials, id -> tenantId);
        DeviceCredentials oldDeviceCredentials = null;
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (deviceCredentials.getDeviceId() != null) {
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            oldDeviceCredentials = deviceCredentialsDao.findByDeviceId(tenantId, deviceCredentials.getDeviceId().getId());
        }
        try {
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            var value = deviceCredentialsDao.saveAndFlush(tenantId, deviceCredentials);
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            publishEvictEvent(new DeviceCredentialsEvictEvent(value.getCredentialsId(), oldDeviceCredentials != null ? oldDeviceCredentials.getCredentialsId() : null));
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (oldDeviceCredentials != null) {
                // 审计或事件记录用于保留业务变更轨迹，便于后续查询、告警或外部系统消费。
                eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(value.getDeviceId()).actionType(ActionType.CREDENTIALS_UPDATED).build());
            }
            return value;
        // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
        } catch (Exception t) {
            handleEvictEvent(new DeviceCredentialsEvictEvent(deviceCredentials.getCredentialsId(), oldDeviceCredentials != null ? oldDeviceCredentials.getCredentialsId() : null));
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && (e.getConstraintName().equalsIgnoreCase("device_credentials_id_unq_key") || e.getConstraintName().equalsIgnoreCase("device_credentials_device_id_unq_key"))) {
                throw new DataValidationException("Specified credentials are already registered!");
            } else {
                throw t;
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `formatCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void formatCredentials(DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case X509_CERTIFICATE:
                formatCertData(deviceCredentials);
                break;
            case MQTT_BASIC:
                formatSimpleMqttCredentials(deviceCredentials);
                break;
            case LWM2M_CREDENTIALS:
                formatAndValidateSimpleLwm2mCredentials(deviceCredentials);
                break;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `toCredentialsInfo` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public JsonNode toCredentialsInfo(DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                return JacksonUtil.valueToTree(deviceCredentials.getCredentialsId());
            case X509_CERTIFICATE:
                return JacksonUtil.valueToTree(deviceCredentials.getCredentialsValue());
        }
        return JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), JsonNode.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `formatSimpleMqttCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void formatSimpleMqttCredentials(DeviceCredentials deviceCredentials) {
        BasicMqttCredentials mqttCredentials;
        try {
            mqttCredentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
            if (mqttCredentials == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new DeviceCredentialsValidationException("Invalid credentials body for simple mqtt credentials!");
        }

        if (StringUtils.isEmpty(mqttCredentials.getClientId()) && StringUtils.isEmpty(mqttCredentials.getUserName())) {
            throw new DeviceCredentialsValidationException("Both mqtt client id and user name are empty!");
        }
        if (StringUtils.isNotEmpty(mqttCredentials.getClientId()) && StringUtils.isNotEmpty(mqttCredentials.getPassword()) && StringUtils.isEmpty(mqttCredentials.getUserName())) {
            throw new DeviceCredentialsValidationException("Password cannot be specified along with client id");
        }

        if (StringUtils.isEmpty(mqttCredentials.getClientId())) {
            deviceCredentials.setCredentialsId(mqttCredentials.getUserName());
        } else if (StringUtils.isEmpty(mqttCredentials.getUserName())) {
            deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(mqttCredentials.getClientId()));
        } else {
            deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash("|", mqttCredentials.getClientId(), mqttCredentials.getUserName()));
        }
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(mqttCredentials));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `formatCertData` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void formatCertData(DeviceCredentials deviceCredentials) {
        String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);
        deviceCredentials.setCredentialsId(sha3Hash);
        deviceCredentials.setCredentialsValue(cert);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `formatAndValidateSimpleLwm2mCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void formatAndValidateSimpleLwm2mCredentials(DeviceCredentials deviceCredentials) {
        LwM2MDeviceCredentials lwM2MCredentials;
        try {
            lwM2MCredentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), LwM2MDeviceCredentials.class);
            validateLwM2MDeviceCredentials(lwM2MCredentials);
        } catch (IllegalArgumentException e) {
            throw new DeviceCredentialsValidationException("Invalid credentials body for LwM2M credentials!");
        }

        String credentialsId = null;
        LwM2MClientCredential clientCredentials = lwM2MCredentials.getClient();
        switch (clientCredentials.getSecurityConfigClientMode()) {
            case NO_SEC:
            case RPK:
                deviceCredentials.setCredentialsValue(JacksonUtil.toString(lwM2MCredentials));
                credentialsId = clientCredentials.getEndpoint();
                break;
            case PSK:
                credentialsId = ((PSKClientCredential) clientCredentials).getIdentity();
                break;
            case X509:
                deviceCredentials.setCredentialsValue(JacksonUtil.toString(lwM2MCredentials));
                X509ClientCredential x509ClientConfig = (X509ClientCredential) clientCredentials;
                if ((StringUtils.isNotBlank(x509ClientConfig.getCert()))) {
                    credentialsId = EncryptionUtil.getSha3Hash(x509ClientConfig.getCert());
                } else {
                    credentialsId = x509ClientConfig.getEndpoint();
                }
                break;
        }
        if (credentialsId == null) {
            throw new DeviceCredentialsValidationException("Invalid credentials body for LwM2M credentials!");
        }
        deviceCredentials.setCredentialsId(credentialsId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateLwM2MDeviceCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateLwM2MDeviceCredentials(LwM2MDeviceCredentials lwM2MCredentials) {
        if (lwM2MCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M credentials must be specified!");
        }

        LwM2MClientCredential clientCredentials = lwM2MCredentials.getClient();
        if (clientCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M client credentials must be specified!");
        }
        validateLwM2MClientCredentials(clientCredentials);

        LwM2MBootstrapClientCredentials bootstrapCredentials = lwM2MCredentials.getBootstrap();
        if (bootstrapCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M bootstrap credentials must be specified!");
        }

        LwM2MBootstrapClientCredential bootstrapServerCredentials = bootstrapCredentials.getBootstrapServer();
        if (bootstrapServerCredentials == null) {
            throw new DeviceCredentialsValidationException("LwM2M bootstrap server credentials must be specified!");
        }
        validateServerCredentials(bootstrapServerCredentials, "Bootstrap server");

        LwM2MBootstrapClientCredential lwm2MBootstrapClientCredential = bootstrapCredentials.getLwm2mServer();
        if (lwm2MBootstrapClientCredential == null) {
            throw new DeviceCredentialsValidationException("LwM2M lwm2m server credentials must be specified!");
        }
        validateServerCredentials(lwm2MBootstrapClientCredential, "LwM2M server");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateLwM2MClientCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateLwM2MClientCredentials(LwM2MClientCredential clientCredentials) {
        if (StringUtils.isBlank(clientCredentials.getEndpoint())) {
            throw new DeviceCredentialsValidationException("LwM2M client endpoint must be specified!");
        }

        switch (clientCredentials.getSecurityConfigClientMode()) {
            case NO_SEC:
                break;
            case PSK:
                PSKClientCredential pskCredentials = (PSKClientCredential) clientCredentials;
                if (StringUtils.isBlank(pskCredentials.getIdentity())) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK identity must be specified and must be an utf8 string!");
                }
                // SecurityMode.NO_SEC.toString() == "NO_SEC";
                if (pskCredentials.getIdentity().equals(SecurityMode.NO_SEC.toString())) {
                    throw new DeviceCredentialsValidationException("The PSK ID of the LwM2M client must not be '" + SecurityMode.NO_SEC + "'!");
                }

                String pskKey = pskCredentials.getKey();
                if (StringUtils.isBlank(pskKey)) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK key must be specified!");
                }

                if (!pskKey.matches("-?[0-9a-fA-F]+")) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK key must be random sequence in hex encoding!");
                }

                if (pskKey.length() % 32 != 0 || pskKey.length() > 128) {
                    throw new DeviceCredentialsValidationException("LwM2M client PSK key length = " + pskKey.length() + ". Key must be HexDec format: 32, 64, 128 characters!");
                }

                break;
            case RPK:
                RPKClientCredential rpkCredentials = (RPKClientCredential) clientCredentials;
                if (StringUtils.isBlank(rpkCredentials.getKey())) {
                    throw new DeviceCredentialsValidationException("LwM2M client RPK key must be specified!");
                }

                try {
                    String pubkClient = EncryptionUtil.pubkTrimNewLines(rpkCredentials.getKey());
                    rpkCredentials.setKey(pubkClient);
                    SecurityUtil.publicKey.decode(rpkCredentials.getDecoded());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException("LwM2M client RPK key must be in standard [RFC7250] and support only EC algorithm and then encoded to Base64 format!");
                }
                break;
            case X509:
                X509ClientCredential x509CCredentials = (X509ClientCredential) clientCredentials;
                if (StringUtils.isNotEmpty(x509CCredentials.getCert())) {
                    try {
                        String certClient = EncryptionUtil.certTrimNewLines(x509CCredentials.getCert());
                        x509CCredentials.setCert(certClient);
                        SecurityUtil.certificate.decode(x509CCredentials.getDecoded());
                    } catch (Exception e) {
                        throw new DeviceCredentialsValidationException("LwM2M client X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!");
                    }
                }
                break;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateServerCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void validateServerCredentials(LwM2MBootstrapClientCredential serverCredentials, String server) {
        switch (serverCredentials.getSecurityMode()) {
            case NO_SEC:
                break;
            case PSK:
                PSKBootstrapClientCredential pskCredentials = (PSKBootstrapClientCredential) serverCredentials;
                if (StringUtils.isBlank(pskCredentials.getClientPublicKeyOrId())) {
                    throw new DeviceCredentialsValidationException(server + " client PSK public key or id must be specified and must be an utf8 string!");
                }

                // SecurityMode.NO_SEC.toString() == "NO_SEC";
                if (pskCredentials.getClientPublicKeyOrId().equals(SecurityMode.NO_SEC.toString())) {
                    throw new DeviceCredentialsValidationException(server + " client PSK public key or id must not be '" + SecurityMode.NO_SEC + "'!");
                }

                String pskKey = pskCredentials.getClientSecretKey();
                if (StringUtils.isBlank(pskKey)) {
                    throw new DeviceCredentialsValidationException(server + " client PSK key must be specified!");
                }

                if (!pskKey.matches("-?[0-9a-fA-F]+")) {
                    throw new DeviceCredentialsValidationException(server + " client PSK key must be random sequence in hex encoding!");
                }

                if (pskKey.length() % 32 != 0 || pskKey.length() > 128) {
                    throw new DeviceCredentialsValidationException(server + " client PSK key length = " + pskKey.length() + ". Key must be HexDec format: 32, 64, 128 characters!");
                }
                break;
            case RPK:
                RPKBootstrapClientCredential rpkServerCredentials = (RPKBootstrapClientCredential) serverCredentials;
                if (StringUtils.isEmpty(rpkServerCredentials.getClientPublicKeyOrId())) {
                    throw new DeviceCredentialsValidationException(server + " client RPK public key or id must be specified!");
                }
                try {
                    String pubkRpkSever = EncryptionUtil.pubkTrimNewLines(rpkServerCredentials.getClientPublicKeyOrId());
                    rpkServerCredentials.setClientPublicKeyOrId(pubkRpkSever);
                    SecurityUtil.publicKey.decode(rpkServerCredentials.getDecodedClientPublicKeyOrId());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client RPK public key or id must be in standard [RFC7250 ] and then encoded to Base64 format!");
                }

                if (StringUtils.isEmpty(rpkServerCredentials.getClientSecretKey())) {
                    throw new DeviceCredentialsValidationException(server + " client RPK secret key must be specified!");
                }

                try {
                    String prikRpkSever = EncryptionUtil.prikTrimNewLines(rpkServerCredentials.getClientSecretKey());
                    rpkServerCredentials.setClientSecretKey(prikRpkSever);
                    SecurityUtil.privateKey.decode(rpkServerCredentials.getDecodedClientSecretKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client RPK secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!");
                }
                break;
            case X509:
                X509BootstrapClientCredential x509ServerCredentials = (X509BootstrapClientCredential) serverCredentials;
                if (StringUtils.isBlank(x509ServerCredentials.getClientPublicKeyOrId())) {
                    throw new DeviceCredentialsValidationException(server + " client X509 public key or id must be specified!");
                }

                try {
                    String certServer = EncryptionUtil.certTrimNewLines(x509ServerCredentials.getClientPublicKeyOrId());
                    x509ServerCredentials.setClientPublicKeyOrId(certServer);
                    SecurityUtil.certificate.decode(x509ServerCredentials.getDecodedClientPublicKeyOrId());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client X509 public key or id must be in DER-encoded X509v3 format  and support only EC algorithm and then encoded to Base64 format!");
                }
                if (StringUtils.isBlank(x509ServerCredentials.getClientSecretKey())) {
                    throw new DeviceCredentialsValidationException(server + " client X509 secret key must be specified!");
                }

                try {
                    String prikX509Sever = EncryptionUtil.prikTrimNewLines(x509ServerCredentials.getClientSecretKey());
                    x509ServerCredentials.setClientSecretKey(prikX509Sever);
                    SecurityUtil.privateKey.decode(x509ServerCredentials.getDecodedClientSecretKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " client X509 secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!");
                }
                break;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteDeviceCredentials` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void deleteDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials) {
        log.trace("Executing deleteDeviceCredentials [{}]", deviceCredentials);
        deviceCredentialsDao.removeById(tenantId, deviceCredentials.getUuidId());
        publishEvictEvent(new DeviceCredentialsEvictEvent(deviceCredentials.getCredentialsId(), null));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteDeviceCredentialsByDeviceId` 对应的设备持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void deleteDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing deleteDeviceCredentialsByDeviceId [{}]", deviceId);
        DeviceCredentials credentials = deviceCredentialsDao.removeByDeviceId(tenantId, deviceId);
        if (credentials != null) {
            publishEvictEvent(new DeviceCredentialsEvictEvent(credentials.getCredentialsId(), null));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceCredentialsServiceImpl` 在 ThingsBoard DAO 模块 中承担设备持久化服务类型职责，核心目的是维护设备、凭据、配置、声明、连接状态和设备画像相关的数据库访问流程。
 * 2. 核心流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
 * 3. 关键依赖：主要依赖或协作对象包括DeviceService、DeviceCredentialsService、Transport、Rule Engine、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
