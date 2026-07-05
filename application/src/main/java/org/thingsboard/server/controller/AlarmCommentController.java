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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_COMMENT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_COMMENT_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

/**
 * 中文说明：
 * 1. 类目的：`AlarmCommentController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmCommentController extends BaseController {
    /**
     * 告警ID常量，用于统一引用固定值。
     */
    public static final String ALARM_ID = "alarmId";
    public static final String ALARM_COMMENT_ID = "commentId";

    /**
     * 告警，提供当前类调用的业务操作。
     */
    private final TbAlarmCommentService tbAlarmCommentService;

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `strAlarmId`：告警IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Create or update Alarm Comment ",
            notes = "Creates or Updates the Alarm Comment. " +
                    "When creating comment, platform generates Alarm Comment Id as " + UUID_WIKI_LINK +
                    "The newly created Alarm Comment id will be present in the response. Specify existing Alarm Comment id to update the alarm. " +
                    "Referencing non-existing Alarm Comment Id will cause 'Not Found' error. " +
                    "\n\n To create new Alarm comment entity it is enough to specify 'comment' json element with 'text' node, for example: {\"comment\": { \"text\": \"my comment\"}}. " +
                    "\n\n If comment type is not specified the default value 'OTHER' will be saved. If 'alarmId' or 'userId' specified in body it will be ignored." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/comment", method = RequestMethod.POST)
    @ResponseBody
    public AlarmComment saveAlarmComment(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                                         @PathVariable(ALARM_ID) String strAlarmId, @ApiParam(value = "A JSON value representing the comment.") @RequestBody AlarmComment alarmComment) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmInfoId(alarmId, Operation.WRITE);
        alarmComment.setAlarmId(alarmId);
        return tbAlarmCommentService.saveAlarmComment(alarm, alarmComment, getCurrentUser());
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `strAlarmId`：告警IDID。
     * - `strCommentId`：`strCommentId`ID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete Alarm comment (deleteAlarmComment)",
            notes = "Deletes the Alarm comment. Referencing non-existing Alarm comment Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/comment/{commentId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteAlarmComment(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId, @ApiParam(value = ALARM_COMMENT_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_COMMENT_ID) String strCommentId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);

        AlarmCommentId alarmCommentId = new AlarmCommentId(toUUID(strCommentId));
        AlarmComment alarmComment = checkAlarmCommentId(alarmCommentId, alarmId);
        tbAlarmCommentService.deleteAlarmComment(alarm, alarmComment, getCurrentUser());
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `ALARM_ID_PARAM_DESCRIPTION`：`ALARM_ID_PARAM_DESCRIPTION` 参数。
     * - `strAlarmId`：告警IDID。
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Alarm comments (getAlarmComments)",
            notes = "Returns a page of alarm comments for specified alarm. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/comment", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmCommentInfo> getAlarmComments(
            @ApiParam(value = ALARM_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ALARM_ID) String strAlarmId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_COMMENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return checkNotNull(alarmCommentService.findAlarmComments(alarm.getTenantId(), alarmId, pageLink));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmCommentController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
