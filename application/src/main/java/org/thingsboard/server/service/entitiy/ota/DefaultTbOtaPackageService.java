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
package org.thingsboard.server.service.entitiy.ota;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.nio.ByteBuffer;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbOtaPackageService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbOtaPackageService extends AbstractTbEntityService implements TbOtaPackageService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final OtaPackageService otaPackageService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `saveOtaPackageInfoRequest`：请求对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageInfo save(SaveOtaPackageInfoRequest saveOtaPackageInfoRequest, User user) throws ThingsboardException {
        ActionType actionType = saveOtaPackageInfoRequest.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = saveOtaPackageInfoRequest.getTenantId();
        try {
            OtaPackageInfo savedOtaPackageInfo = otaPackageService.saveOtaPackageInfo(new OtaPackageInfo(saveOtaPackageInfoRequest), saveOtaPackageInfoRequest.isUsesUrl());

            notificationEntityService.logEntityAction(tenantId, savedOtaPackageInfo.getId(), savedOtaPackageInfo,
                    null, actionType, user);

            return savedOtaPackageInfo;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.OTA_PACKAGE), saveOtaPackageInfoRequest,
                    actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：保存或创建数据。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * - `checksum`：`checksum` 参数。
     * - `checksumAlgorithm`：`checksumAlgorithm` 参数。
     * - `data`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageInfo saveOtaPackageData(OtaPackageInfo otaPackageInfo, String checksum, ChecksumAlgorithm checksumAlgorithm,
                                             byte[] data, String filename, String contentType, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UPDATED;
        TenantId tenantId = otaPackageInfo.getTenantId();
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            if (StringUtils.isEmpty(checksum)) {
                checksum = otaPackageService.generateChecksum(checksumAlgorithm, ByteBuffer.wrap(data));
            }
            OtaPackage otaPackage = new OtaPackage(otaPackageId);
            otaPackage.setCreatedTime(otaPackageInfo.getCreatedTime());
            otaPackage.setTenantId(tenantId);
            otaPackage.setDeviceProfileId(otaPackageInfo.getDeviceProfileId());
            otaPackage.setType(otaPackageInfo.getType());
            otaPackage.setTitle(otaPackageInfo.getTitle());
            otaPackage.setVersion(otaPackageInfo.getVersion());
            otaPackage.setTag(otaPackageInfo.getTag());
            otaPackage.setAdditionalInfo(otaPackageInfo.getAdditionalInfo());
            otaPackage.setChecksumAlgorithm(checksumAlgorithm);
            otaPackage.setChecksum(checksum);
            otaPackage.setFileName(filename);
            otaPackage.setContentType(contentType);
            otaPackage.setData(ByteBuffer.wrap(data));
            otaPackage.setDataSize((long) data.length);
            OtaPackageInfo savedOtaPackage = otaPackageService.saveOtaPackage(otaPackage);
            notificationEntityService.logEntityAction(tenantId, savedOtaPackage.getId(), savedOtaPackage, null, actionType, user);
            return savedOtaPackage;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.OTA_PACKAGE), actionType, user, e, otaPackageId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(OtaPackageInfo otaPackageInfo, User user) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = otaPackageInfo.getTenantId();
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            otaPackageService.deleteOtaPackage(tenantId, otaPackageId);
            notificationEntityService.logEntityAction(tenantId, otaPackageId, otaPackageInfo, null,
                    actionType, user, otaPackageInfo.getId().toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.OTA_PACKAGE),
                    actionType, user, e, otaPackageId.toString());
            throw e;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbOtaPackageService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
