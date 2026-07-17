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
 * 1. `UserSettingsServiceImpl` 是 ThingsBoard DAO 中负责用户的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCachedService`、`UserSettingsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
