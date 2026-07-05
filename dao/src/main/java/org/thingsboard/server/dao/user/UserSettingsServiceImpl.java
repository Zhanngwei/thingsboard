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
package org.thingsboard.server.dao.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;

import java.util.Iterator;
import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. 类目的：`UserSettingsServiceImpl` 是 ThingsBoard DAO 模块 中的租户、客户或用户持久化服务类型，用于管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 生命周期：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository。
 */
@Service("UserSettingsDaoService")
@Slf4j
@RequiredArgsConstructor
public class UserSettingsServiceImpl extends AbstractCachedService<UserSettingsCompositeKey, UserSettings, UserSettingsEvictEvent> implements UserSettingsService {
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String INCORRECT_USER_ID = "Incorrect userId ";
    private final UserSettingsDao userSettingsDao;

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserSettings saveUserSettings(TenantId tenantId, UserSettings userSettings) {
        log.trace("Executing saveUserSettings for user [{}], [{}]", userSettings.getUserId(), userSettings);
        validateId(userSettings.getUserId(), INCORRECT_USER_ID + userSettings.getUserId());
        return doSaveUserSettings(tenantId, userSettings);
    }

    /**
     * 功能：更新用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * - `settings`：配置对象。
     * 返回：无。
     */
    @Override
    public void updateUserSettings(TenantId tenantId, UserId userId, UserSettingsType type, JsonNode settings) {
        log.trace("Executing updateUserSettings for user [{}], [{}]", userId, settings);
        validateId(userId, INCORRECT_USER_ID + userId);

        var key = new UserSettingsCompositeKey(userId.getId(), type.name());
        UserSettings oldSettings = userSettingsDao.findById(tenantId, key);
        JsonNode oldSettingsJson = oldSettings != null ? oldSettings.getSettings() : JacksonUtil.newObjectNode();

        UserSettings newUserSettings = new UserSettings();
        newUserSettings.setUserId(userId);
        newUserSettings.setType(type);
        newUserSettings.setSettings(update(oldSettingsJson, settings));
        doSaveUserSettings(tenantId, newUserSettings);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserSettings findUserSettings(TenantId tenantId, UserId userId, UserSettingsType type) {
        log.trace("Executing findUserSettings for user [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);

        var key = new UserSettingsCompositeKey(userId.getId(), type.name());
        return cache.getAndPutInTransaction(key,
                () -> userSettingsDao.findById(tenantId, key), true);
    }

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * - `jsonPaths`：文件或资源路径。
     * 返回：无。
     */
    @Override
    public void deleteUserSettings(TenantId tenantId, UserId userId, UserSettingsType type, List<String> jsonPaths) {
        log.trace("Executing deleteUserSettings for user [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        var key = new UserSettingsCompositeKey(userId.getId(), type.name());
        UserSettings userSettings = userSettingsDao.findById(tenantId, key);
        if (userSettings == null) {
            return;
        }
        try {
            DocumentContext dcSettings = JsonPath.parse(userSettings.getSettings().toString());
            for (String s : jsonPaths) {
                dcSettings = dcSettings.delete("$." + s);
            }
            userSettings.setSettings(JacksonUtil.fromString(dcSettings.jsonString(), ObjectNode.class));
        } catch (Exception t) {
            handleEvictEvent(new UserSettingsEvictEvent(key));
            throw new RuntimeException(t);
        }
        doSaveUserSettings(tenantId, userSettings);
    }

    /**
     * 功能：执行 `doSaveUserSettings` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    private UserSettings doSaveUserSettings(TenantId tenantId, UserSettings userSettings) {
        try {
            ConstraintValidator.validateFields(userSettings);
            validateJsonKeys(userSettings.getSettings());
            UserSettings saved = userSettingsDao.save(tenantId, userSettings);
            publishEvictEvent(new UserSettingsEvictEvent(new UserSettingsCompositeKey(userSettings)));
            return saved;
        } catch (Exception t) {
            handleEvictEvent(new UserSettingsEvictEvent(new UserSettingsCompositeKey(userSettings)));
            throw t;
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = UserSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(UserSettingsEvictEvent event) {
        cache.evict(event.getKey());
    }

    /**
     * 功能：校验JSON。
     * 参数：
     * - `userSettings`：配置对象。
     * 返回：无。
     */
    private void validateJsonKeys(JsonNode userSettings) {
        Iterator<String> fieldNames = userSettings.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.contains(".") || fieldName.contains(",")) {
                throw new DataValidationException("Json field name should not contain \".\" or \",\" symbols");
            }
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `mainNode`：`mainNode` 参数。
     * - `updateNode`：`updateNode` 参数。
     * 返回：处理结果。
     */
    public JsonNode update(JsonNode mainNode, JsonNode updateNode) {
        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldExpression = fieldNames.next();
            String[] fieldPath = fieldExpression.trim().split("\\.");
            var node = (ObjectNode) mainNode;
            for (int i = 0; i < fieldPath.length; i++) {
                var fieldName = fieldPath[i];
                var last = i == (fieldPath.length - 1);
                if (last) {
                    node.set(fieldName, updateNode.get(fieldExpression));
                } else {
                    if (!node.has(fieldName)) {
                        node.set(fieldName, JacksonUtil.newObjectNode());
                    }
                    node = (ObjectNode) node.get(fieldName);
                }
            }
        }
        return mainNode;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`UserSettingsServiceImpl` 在 ThingsBoard DAO 模块 中承担租户、客户或用户持久化服务类型职责，核心目的是管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 核心流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
 * 3. 关键依赖：主要依赖或协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
