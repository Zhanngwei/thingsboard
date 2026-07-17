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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * 中文说明：
 * 1. `DeviceProfileServiceImpl` 是 ThingsBoard DAO 中负责设备配置的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCachedEntityService`、`DeviceProfileService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("DeviceProfileDaoService")
@Slf4j
public class DeviceProfileServiceImpl extends AbstractCachedEntityService<DeviceProfileCacheKey, DeviceProfile, DeviceProfileEvictEvent> implements DeviceProfileService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String INCORRECT_DEVICE_PROFILE_NAME = "Incorrect deviceProfileName ";
    private static final String INCORRECT_PROVISION_DEVICE_KEY = "Incorrect provisionDeviceKey ";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Device profile with such name already exists!";

    /**
     * 设备配置，用于读取或保存对应领域对象。
     */
    @Autowired
    private DeviceProfileDao deviceProfileDao;

    /**
     * 设备，用于读取或保存对应领域对象。
     */
    @Autowired
    private DeviceDao deviceDao;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceService deviceService;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Autowired
    private DataValidator<DeviceProfile> deviceProfileValidator;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private QueueService queueService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = DeviceProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(DeviceProfileEvictEvent event) {
        List<DeviceProfileCacheKey> keys = new ArrayList<>(2);
        keys.add(DeviceProfileCacheKey.fromName(event.getTenantId(), event.getNewName()));
        if (event.getDeviceProfileId() != null) {
            keys.add(DeviceProfileCacheKey.fromId(event.getDeviceProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(DeviceProfileCacheKey.defaultProfile(event.getTenantId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(DeviceProfileCacheKey.fromName(event.getTenantId(), event.getOldName()));
        }
        if (StringUtils.isNotEmpty(event.getProvisionDeviceKey())) {
            keys.add(DeviceProfileCacheKey.fromProvisionDeviceKey(event.getProvisionDeviceKey()));
        }
        cache.evict(keys);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return findDeviceProfileById(tenantId, deviceProfileId, true);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `putInCache`：`putInCache` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId, boolean putInCache) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return cache.getOrFetchFromDB(DeviceProfileCacheKey.fromId(deviceProfileId),
                () -> deviceProfileDao.findById(tenantId, deviceProfileId.getId()), true, putInCache);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName) {
        return findDeviceProfileByName(tenantId, profileName, true);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * - `putInCache`：`putInCache` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName, boolean putInCache) {
        log.trace("Executing findDeviceProfileByName [{}][{}]", tenantId, profileName);
        validateString(profileName, INCORRECT_DEVICE_PROFILE_NAME + profileName);
        return cache.getOrFetchFromDB(DeviceProfileCacheKey.fromName(tenantId, profileName),
                () -> deviceProfileDao.findByName(tenantId, profileName), true, putInCache);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `provisionDeviceKey`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDeviceProfileByProvisionDeviceKey(String provisionDeviceKey) {
        log.trace("Executing findDeviceProfileByProvisionDeviceKey provisionKey [{}]", provisionDeviceKey);
        validateString(provisionDeviceKey, INCORRECT_PROVISION_DEVICE_KEY + provisionDeviceKey);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.fromProvisionDeviceKey(provisionDeviceKey),
                () -> deviceProfileDao.findByProvisionDeviceKey(provisionDeviceKey), false);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return toDeviceProfileInfo(findDeviceProfileById(tenantId, deviceProfileId));
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile, boolean doValidate) {
        return doSaveDeviceProfile(deviceProfile, doValidate);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        return doSaveDeviceProfile(deviceProfile, true);
    }

    /**
     * 功能：执行 `doSaveDeviceProfile` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    private DeviceProfile doSaveDeviceProfile(DeviceProfile deviceProfile, boolean doValidate) {
        log.trace("Executing saveDeviceProfile [{}]", deviceProfile);
        if (deviceProfile.getProfileData() != null && deviceProfile.getProfileData().getProvisionConfiguration() instanceof X509CertificateChainProvisionConfiguration) {
            X509CertificateChainProvisionConfiguration x509Configuration = (X509CertificateChainProvisionConfiguration) deviceProfile.getProfileData().getProvisionConfiguration();
            if (x509Configuration.getProvisionDeviceSecret() != null) {
                formatDeviceProfileCertificate(deviceProfile, x509Configuration);
            }
        }
        DeviceProfile oldDeviceProfile = null;
        if (doValidate) {
            oldDeviceProfile = deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
        } else if (deviceProfile.getId() != null) {
            oldDeviceProfile = findDeviceProfileById(deviceProfile.getTenantId(), deviceProfile.getId(), false);
        }
        DeviceProfile savedDeviceProfile;
        try {
            imageService.replaceBase64WithImageUrl(deviceProfile, "device profile");
            savedDeviceProfile = deviceProfileDao.saveAndFlush(deviceProfile.getTenantId(), deviceProfile);
            publishEvictEvent(new DeviceProfileEvictEvent(savedDeviceProfile.getTenantId(), savedDeviceProfile.getName(),
                    oldDeviceProfile != null ? oldDeviceProfile.getName() : null, savedDeviceProfile.getId(), savedDeviceProfile.isDefault(),
                    oldDeviceProfile != null ? oldDeviceProfile.getProvisionDeviceKey() : null));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedDeviceProfile.getTenantId())
                    .entityId(savedDeviceProfile.getId()).created(oldDeviceProfile == null).build());
        } catch (Exception t) {
            handleEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(),
                    oldDeviceProfile != null ? oldDeviceProfile.getName() : null, null, deviceProfile.isDefault(),
                    oldDeviceProfile != null ? oldDeviceProfile.getProvisionDeviceKey() : null));
            String unqProvisionKeyErrorMsg = DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(deviceProfile.getProvisionType())
                    ? "Device profile with such certificate already exists!"
                    : "Device profile with such provision device key already exists!";
            checkConstraintViolation(t,
                    Map.of("device_profile_name_unq_key", DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS,
                            "device_provision_key_unq_key", unqProvisionKeyErrorMsg,
                            "device_profile_external_id_unq_key", "Device profile with such external id already exists!"));
            throw t;
        }
        if (oldDeviceProfile != null && !oldDeviceProfile.getName().equals(deviceProfile.getName())) {
            PageLink pageLink = new PageLink(100);
            PageData<Device> pageData;
            do {
                pageData = deviceDao.findDevicesByTenantIdAndProfileId(deviceProfile.getTenantId().getId(), deviceProfile.getUuidId(), pageLink);
                for (Device device : pageData.getData()) {
                    device.setType(deviceProfile.getName());
                    deviceService.saveDevice(device);
                }
                pageLink = pageLink.nextPageLink();
            } while (pageData.hasNext());
        }
        return savedDeviceProfile;
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing deleteDeviceProfile [{}]", deviceProfileId);
        validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (deviceProfile != null && deviceProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Device Profile is prohibited!");
        }
        this.removeDeviceProfile(tenantId, deviceProfile);
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    private void removeDeviceProfile(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            deleteEntityRelations(tenantId, deviceProfileId);
            deviceProfileDao.removeById(tenantId, deviceProfileId.getId());
            publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(),
                    null, deviceProfile.getId(), deviceProfile.isDefault(),
                    deviceProfile.getProvisionDeviceKey()));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(deviceProfileId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_device_profile")) {
                throw new DataValidationException("The device profile referenced by the devices cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfiles(tenantId, pageLink);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * - `transportType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfileInfos(tenantId, pageLink, transportType);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateDefaultDeviceProfile");
        DeviceProfile deviceProfile = findDeviceProfileByName(tenantId, name, false);
        if (deviceProfile == null) {
            try {
                deviceProfile = this.doCreateDefaultDeviceProfile(tenantId, name, name.equals("default"));
            } catch (DataValidationException e) {
                if (DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                    deviceProfile = findDeviceProfileByName(tenantId, name, false);
                } else {
                    throw e;
                }
            }
        }
        return deviceProfile;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile createDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing createDefaultDeviceProfile tenantId [{}]", tenantId);
        return doCreateDefaultDeviceProfile(tenantId, "default", true);
    }

    /**
     * 功能：执行 `doCreateDefaultDeviceProfile` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * - `defaultProfile`：`defaultProfile` 参数。
     * 返回：处理结果。
     */
    private DeviceProfile doCreateDefaultDeviceProfile(TenantId tenantId, String profileName, boolean defaultProfile) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setDefault(defaultProfile);
        deviceProfile.setName(profileName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription("Default device profile");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        return saveDeviceProfile(deviceProfile);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfile tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.defaultProfile(tenantId),
                () -> deviceProfileDao.findDefaultDeviceProfile(tenantId), true);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return toDeviceProfileInfo(findDefaultDeviceProfile(tenantId));
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：判断结果。
     */
    @Override
    public boolean setDefaultDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing setDefaultDeviceProfile [{}]", deviceProfileId);
        validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (!deviceProfile.isDefault()) {
            deviceProfile.setDefault(true);
            DeviceProfile previousDefaultDeviceProfile = findDefaultDeviceProfile(tenantId);
            boolean changed = false;
            if (previousDefaultDeviceProfile == null) {
                deviceProfileDao.save(tenantId, deviceProfile);
                publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(), null, deviceProfile.getId(), true, deviceProfile.getProvisionDeviceKey()));
                changed = true;
            } else if (!previousDefaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                previousDefaultDeviceProfile.setDefault(false);
                deviceProfileDao.save(tenantId, previousDefaultDeviceProfile);
                deviceProfileDao.save(tenantId, deviceProfile);
                publishEvictEvent(new DeviceProfileEvictEvent(previousDefaultDeviceProfile.getTenantId(), previousDefaultDeviceProfile.getName(), null, previousDefaultDeviceProfile.getId(), false, deviceProfile.getProvisionDeviceKey()));
                publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(), null, deviceProfile.getId(), true, deviceProfile.getProvisionDeviceKey()));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteDeviceProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDeviceProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDeviceProfilesRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDeviceProfileById(tenantId, new DeviceProfileId(entityId.getId())));
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteDeviceProfile(tenantId, (DeviceProfileId) id);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activeOnly`：`activeOnly` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityInfo> findDeviceProfileNamesByTenantId(TenantId tenantId, boolean activeOnly) {
        log.trace("Executing findDeviceProfileNamesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return deviceProfileDao.findTenantDeviceProfileNames(tenantId.getId(), activeOnly)
                .stream().sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());
    }

    private final PaginatedRemover<TenantId, DeviceProfile> tenantDeviceProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<DeviceProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return deviceProfileDao.findDeviceProfiles(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, DeviceProfile entity) {
                    removeDeviceProfile(tenantId, entity);
                }
            };

    /**
     * 功能：执行 `toDeviceProfileInfo` 对应的处理。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：处理结果。
     */
    private DeviceProfileInfo toDeviceProfileInfo(DeviceProfile profile) {
        return profile == null ? null : new DeviceProfileInfo(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(),
                profile.getDefaultDashboardId(), profile.getType(), profile.getTransportType());
    }

    /**
     * 功能：执行 `formatDeviceProfileCertificate` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `x509Configuration`：配置对象。
     * 返回：无。
     */
    private void formatDeviceProfileCertificate(DeviceProfile deviceProfile, X509CertificateChainProvisionConfiguration x509Configuration) {
        String formattedCertificateValue = formatCertificateValue(x509Configuration.getProvisionDeviceSecret());
        String cert = fetchLeafCertificateFromChain(formattedCertificateValue);
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);
        DeviceProfileData deviceProfileData = deviceProfile.getProfileData();
        x509Configuration.setProvisionDeviceSecret(formattedCertificateValue);
        deviceProfileData.setProvisionConfiguration(x509Configuration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(sha3Hash);
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    private String fetchLeafCertificateFromChain(String value) {
        String regex = "-----BEGIN CERTIFICATE-----\\s*.*?\\s*-----END CERTIFICATE-----";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            // if the method receives a chain it fetches the leaf (end-entity) certificate, else if it gets a single certificate, it returns the single certificate
            return matcher.group(0);
        }
        return value;
    }

    /**
     * 功能：执行 `formatCertificateValue` 对应的处理。
     * 参数：
     * - `certificateValue`：值。
     * 返回：文本结果。
     */
    private String formatCertificateValue(String certificateValue) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream inputStream = new ByteArrayInputStream(certificateValue.getBytes());
            Certificate[] certificates = cf.generateCertificates(inputStream).toArray(new Certificate[0]);
            if (certificates.length > 1) {
                return EncryptionUtil.certTrimNewLinesForChainInDeviceProfile(certificateValue);
            }
        } catch (CertificateException ignored) {
        }
        return EncryptionUtil.certTrimNewLines(certificateValue);
    }

}
