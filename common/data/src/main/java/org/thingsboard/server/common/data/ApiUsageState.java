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
package org.thingsboard.server.common.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `ApiUsageState` 是 ThingsBoard Common Data 中承载用量统计信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasTenantId`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ApiUsageState extends BaseData<ApiUsageStateId> implements HasTenantId {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8250339805336035966L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private EntityId entityId;
    /**
     * 状态，表示当前对象所处状态。
     */
    private ApiUsageStateValue transportState;
    private ApiUsageStateValue dbStorageState;
    /**
     * 状态，表示当前对象所处状态。
     */
    private ApiUsageStateValue reExecState;
    private ApiUsageStateValue jsExecState;
    /**
     * 状态，表示当前对象所处状态。
     */
    private ApiUsageStateValue tbelExecState;
    private ApiUsageStateValue emailExecState;
    /**
     * 状态，表示当前对象所处状态。
     */
    private ApiUsageStateValue smsExecState;
    private ApiUsageStateValue alarmExecState;

    /**
     * 功能：创建 `ApiUsageState` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ApiUsageState() {
        super();
    }

    /**
     * 功能：创建 `ApiUsageState` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public ApiUsageState(ApiUsageStateId id) {
        super(id);
    }

    /**
     * 功能：创建 `ApiUsageState` 实例，并初始化必要字段。
     * 参数：
     * - `ur`：`ur` 参数。
     * 返回：新创建的对象实例。
     */
    public ApiUsageState(ApiUsageState ur) {
        super(ur);
        this.tenantId = ur.getTenantId();
        this.entityId = ur.getEntityId();
        this.transportState = ur.getTransportState();
        this.dbStorageState = ur.getDbStorageState();
        this.reExecState = ur.getReExecState();
        this.jsExecState = ur.getJsExecState();
        this.tbelExecState = ur.getTbelExecState();
        this.emailExecState = ur.getEmailExecState();
        this.smsExecState = ur.getSmsExecState();
        this.alarmExecState = ur.getAlarmExecState();
    }

    /**
     * 功能：判断传输层。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isTransportEnabled() {
        return !ApiUsageStateValue.DISABLED.equals(transportState);
    }

    /**
     * 功能：判断`Re Exec Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isReExecEnabled() {
        return !ApiUsageStateValue.DISABLED.equals(reExecState);
    }

    /**
     * 功能：判断`Db Storage Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isDbStorageEnabled() {
        return !ApiUsageStateValue.DISABLED.equals(dbStorageState);
    }

    /**
     * 功能：判断`Js Exec Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isJsExecEnabled() {
        return !ApiUsageStateValue.DISABLED.equals(jsExecState);
    }

    /**
     * 功能：判断`Tbel Exec Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isTbelExecEnabled() {
        return !ApiUsageStateValue.DISABLED.equals(tbelExecState);
    }

    /**
     * 功能：判断邮箱。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isEmailSendEnabled(){
        return !ApiUsageStateValue.DISABLED.equals(emailExecState);
    }

    /**
     * 功能：判断`Sms Send Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSmsSendEnabled(){
        return !ApiUsageStateValue.DISABLED.equals(smsExecState);
    }

    /**
     * 功能：判断告警。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isAlarmCreationEnabled() {
        return alarmExecState != ApiUsageStateValue.DISABLED;
    }
}
