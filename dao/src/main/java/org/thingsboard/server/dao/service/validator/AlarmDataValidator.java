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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

/**
 * 中文说明：
 * 1. `AlarmDataValidator` 是 ThingsBoard DAO 中负责告警存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@AllArgsConstructor
public class AlarmDataValidator extends DataValidator<Alarm> {

    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarm`：`alarm` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, Alarm alarm) {
        validateString("Alarm type", alarm.getType());
        if (alarm.getOriginator() == null) {
            throw new DataValidationException("Alarm originator should be specified!");
        }
        if (alarm.getSeverity() == null) {
            throw new DataValidationException("Alarm severity should be specified!");
        }
        if (alarm.getStatus() == null) {
            throw new DataValidationException("Alarm status should be specified!");
        }
        if (alarm.getTenantId() == null) {
            throw new DataValidationException("Alarm should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(alarm.getTenantId())) {
                throw new DataValidationException("Alarm is referencing to non-existent tenant!");
            }
        }
    }
}
