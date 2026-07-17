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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SCOPE_ALLOWED_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SCOPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_COUNT_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_DATA_QUERY_DESCRIPTION;

/**
 * 中文说明：
 * 1. `EntityQueryController` 是 ThingsBoard Application 中处理实体请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityQueryController extends BaseController {

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityQueryService entityQueryService;

    /**
     * `MAX_PAGE_SIZE`常量，用于统一引用固定值。
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    @ApiOperation(value = "Count Entities by Query", notes = ENTITY_COUNT_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countEntitiesByQuery(
            @ApiParam(value = "A JSON value representing the entity count query. See API call notes above for more details.")
            @RequestBody EntityCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        return this.entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Find Entity Data by Query", notes = ENTITY_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EntityData> findEntityDataByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        return this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Find Alarms by Query", notes = ALARM_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<AlarmData> findAlarmDataByQuery(
            @ApiParam(value = "A JSON value representing the alarm data query. See API call notes above for more details.")
            @RequestBody AlarmDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getPageLink());
        UserId assigneeId = query.getPageLink().getAssigneeId();
        if (assigneeId != null) {
            checkUserId(assigneeId, Operation.READ);
        }
        return this.entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
    }

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    @ApiOperation(value = "Count Alarms by Query (countAlarmsByQuery)", notes = "Returns the number of alarms that match the query definition.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countAlarmsByQuery(@ApiParam(value = "A JSON value representing the alarm count query.")
                                   @RequestBody AlarmCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        UserId assigneeId = query.getAssigneeId();
        if (assigneeId != null) {
            checkUserId(assigneeId, Operation.READ);
        }
        return this.entityQueryService.countAlarmsByQuery(getCurrentUser(), query);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `query`：`query` 参数。
     * - `isTimeseries`：`isTimeseries` 参数。
     * - `isAttributes`：`isAttributes` 参数。
     * - `ATTRIBUTES_SCOPE_DESCRIPTION`：`ATTRIBUTES_SCOPE_DESCRIPTION` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Find Entity Keys by Query",
            notes = "Uses entity data query (see 'Find Entity Data by Query') to find first 100 entities. Then fetch and return all unique time-series and/or attribute keys. Used mostly for UI hints.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> findEntityTimeseriesAndAttributesKeysByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query,
            @ApiParam(value = "Include all unique time-series keys to the result.")
            @RequestParam("timeseries") boolean isTimeseries,
            @ApiParam(value = "Include all unique attribute keys to the result.")
            @RequestParam("attributes") boolean isAttributes,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, allowableValues = ATTRIBUTES_SCOPE_ALLOWED_VALUES)
            @RequestParam(value = "scope", required = false) String scope) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        checkNotNull(query);
        EntityDataPageLink pageLink = query.getPageLink();
        if (pageLink.getPageSize() > MAX_PAGE_SIZE) {
            pageLink.setPageSize(MAX_PAGE_SIZE);
        }
        return entityQueryService.getKeysByQuery(getCurrentUser(), tenantId, query, isTimeseries, isAttributes, scope);
    }

}
