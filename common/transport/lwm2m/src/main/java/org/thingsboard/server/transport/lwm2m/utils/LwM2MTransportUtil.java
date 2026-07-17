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
package org.thingsboard.server.transport.lwm2m.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.transport.lwm2m.config.TbLwM2mVersion;
import org.thingsboard.server.transport.lwm2m.server.LwM2mOtaConvert;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;
import org.thingsboard.server.transport.lwm2m.server.downlink.HasVersionedId;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.leshan.core.attributes.Attribute.DIMENSION;
import static org.eclipse.leshan.core.attributes.Attribute.GREATER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.LESSER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.OBJECT_VERSION;
import static org.eclipse.leshan.core.attributes.Attribute.STEP;
import static org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
import static org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
import static org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
import static org.eclipse.leshan.core.model.ResourceModel.Type.TIME;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SW_STATE_ID;

/**
 * 中文说明：
 * 1. `LwM2MTransportUtil` 是 ThingsBoard Common Transport 中处理 LwM2M 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class LwM2MTransportUtil {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    public static final String LWM2M_OBJECT_VERSION_DEFAULT = "1.0";

    /**
     * 遥测常量，用于统一引用固定值。
     */
    public static final String LOG_LWM2M_TELEMETRY = "transportLog";
    public static final String LOG_LWM2M_INFO = "info";
    /**
     * 错误信息常量，用于统一引用固定值。
     */
    public static final String LOG_LWM2M_ERROR = "error";
    public static final String LOG_LWM2M_WARN = "warn";
    /**
     * `BOOTSTRAP_DEFAULT_SHORT_ID`常量，用于统一引用固定值。
     */
    public static final int BOOTSTRAP_DEFAULT_SHORT_ID = 0;

    /**
     * 中文说明：
     * 1. `LwM2MClientStrategy` 是 ThingsBoard Common Transport 中定义 LwM2M 固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
     */
    public enum LwM2MClientStrategy {
        CLIENT_STRATEGY_1(1, "Read only resources marked as observation"),
        CLIENT_STRATEGY_2(2, "Read all client resources");

        /**
         * 编码，表示当前对象的对应属性。
         */
        public int code;
        public String type;

        LwM2MClientStrategy(int code, String type) {
            this.code = code;
            this.type = type;
        }

        /**
         * 功能：执行 `fromStrategyClientByType` 对应的处理。
         * 参数：
         * - `type`：类型。
         * 返回：处理结果。
         */
        public static LwM2MClientStrategy fromStrategyClientByType(String type) {
            for (LwM2MClientStrategy to : LwM2MClientStrategy.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client Strategy type  : %s", type));
        }

        /**
         * 功能：执行 `fromStrategyClientByCode` 对应的处理。
         * 参数：
         * - `code`：`code` 参数。
         * 返回：处理结果。
         */
        public static LwM2MClientStrategy fromStrategyClientByCode(int code) {
            for (LwM2MClientStrategy to : LwM2MClientStrategy.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client Strategy code : %s", code));
        }
    }

    /**
     * 功能：执行 `equalsResourceValue` 对应的处理。
     * 参数：
     * - `valueOld`：值。
     * - `valueNew`：值。
     * - `type`：类型。
     * - `resourcePath`：文件或资源路径。
     * 返回：判断结果。
     */
    public static boolean equalsResourceValue(Object valueOld, Object valueNew, ResourceModel.Type type, LwM2mPath
            resourcePath) throws CodecException {
        switch (type) {
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
                return String.valueOf(valueOld).equals(String.valueOf(valueNew));
            case TIME:
                return ((Date) valueOld).getTime() == ((Date) valueNew).getTime();
            case STRING:
            case OBJLNK:
                return valueOld.equals(valueNew);
            case OPAQUE:
                return Arrays.equals(Hex.decodeHex(((String) valueOld).toCharArray()), Hex.decodeHex(((String) valueNew).toCharArray()));
            default:
                throw new CodecException("Invalid value type for resource %s, type %s", resourcePath, type);
        }
    }

    /**
     * 功能：转换值。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * - `value`：值。
     * - `currentType`：类型。
     * 返回：处理结果。
     */
    public static LwM2mOtaConvert convertOtaUpdateValueToString(String pathIdVer, Object value, ResourceModel.Type currentType) {
        String path = fromVersionedIdToObjectId(pathIdVer);
        LwM2mOtaConvert lwM2mOtaConvert = new LwM2mOtaConvert();
        if (path != null) {
            if (FW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(FirmwareUpdateState.fromStateFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (FW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(FirmwareUpdateResult.fromUpdateResultFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (SW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(SoftwareUpdateState.fromUpdateStateSwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (SW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(SoftwareUpdateResult.fromUpdateResultSwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            }
        }
        lwM2mOtaConvert.setCurrentType(currentType);
        lwM2mOtaConvert.setValue(value);
        return lwM2mOtaConvert;
    }

    /**
     * 功能：执行 `toLwM2MClientProfile` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public static Lwm2mDeviceProfileTransportConfiguration toLwM2MClientProfile(DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.LWM2M)) {
            return (Lwm2mDeviceProfileTransportConfiguration) transportConfiguration;
        } else {
            log.info("[{}] Received profile with invalid transport configuration: {}", deviceProfile.getId(), deviceProfile.getProfileData().getTransportConfiguration());
            throw new IllegalArgumentException("Received profile with invalid transport configuration: " + transportConfiguration.getType());
        }
    }

    /**
     * 功能：获取参数集合。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    public static List<LwM2MBootstrapServerCredential> getBootstrapParametersFromThingsboard(DeviceProfile deviceProfile) {
        return toLwM2MClientProfile(deviceProfile).getBootstrap();
    }

    /**
     * 功能：执行 `fromVersionedIdToObjectId` 对应的处理。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * 返回：文本结果。
     */
    public static String fromVersionedIdToObjectId(String pathIdVer) {
        try {
            if (pathIdVer == null) {
                return null;
            }
            String[] keyArray = pathIdVer.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LWM2M_SEPARATOR_KEY).length == 2) {
                keyArray[1] = keyArray[1].split(LWM2M_SEPARATOR_KEY)[0];
                return StringUtils.join(keyArray, LWM2M_SEPARATOR_PATH);
            } else {
                return pathIdVer;
            }
        } catch (Exception e) {
            log.debug("Issue converting path with version [{}] to path without version: ", pathIdVer, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path - pathId or pathIdVer
     * @return
     */
    /**
     * 功能：获取路径。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：文本结果。
     */
    public static String getVerFromPathIdVerOrId(String path) {
        try {
            String[] keyArray = path.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1) {
                String[] keyArrayVer = keyArray[1].split(LWM2M_SEPARATOR_KEY);
                return keyArrayVer.length == 2 ? keyArrayVer[1] : LWM2M_OBJECT_VERSION_DEFAULT;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 功能：执行 `validPathIdVer` 对应的处理。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * - `registration`：`registration` 参数。
     * 返回：文本结果。
     */
    public static String validPathIdVer(String pathIdVer, Registration registration) throws
            IllegalArgumentException {
        if (!pathIdVer.contains(LWM2M_SEPARATOR_PATH)) {
            throw new IllegalArgumentException(String.format("Error:"));
        } else {
            String[] keyArray = pathIdVer.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LWM2M_SEPARATOR_KEY).length == 2) {
                return pathIdVer;
            } else {
                return convertObjectIdToVersionedId(pathIdVer, registration);
            }
        }
    }

    /**
     * 功能：转换`Object Id To Versioned Id`。
     * 参数：
     * - `path`：文件或资源路径。
     * - `registration`：`registration` 参数。
     * 返回：文本结果。
     */
    public static String convertObjectIdToVersionedId(String path, Registration registration) {
        String ver = registration.getSupportedObject().get(new LwM2mPath(path).getObjectId());
        ver = ver != null ? ver : TbLwM2mVersion.VERSION_1_0.getVersion().toString();
        try {
            String[] keyArray = path.split(LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1) {
                keyArray[1] = keyArray[1] + LWM2M_SEPARATOR_KEY + ver;
                return StringUtils.join(keyArray, LWM2M_SEPARATOR_PATH);
            } else {
                return path;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 功能：校验键。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    public static String validateObjectVerFromKey(String key) {
        try {
            return (key.split(LWM2M_SEPARATOR_PATH)[1].split(LWM2M_SEPARATOR_KEY)[1]);
        } catch (Exception e) {
            return ObjectModel.DEFAULT_VERSION;
        }
    }

    /**
     * As example:
     * a)Write-Attributes/3/0/9?pmin=1 means the Battery Level value will be notified
     * to the Server with a minimum interval of 1sec;
     * this value is set at theResource level.
     * b)Write-Attributes/3/0/9?pmin means the Battery Level will be notified
     * to the Server with a minimum value (pmin) given by the default one
     * (resource 2 of Object Server ID=1),
     * or with another value if this Attribute has been set at another level
     * (Object or Object Instance: see section5.1.1).
     * c)Write-Attributes/3/0?pmin=10 means that all Resources of Instance 0 of the Object ‘Device (ID:3)’
     * will be notified to the Server with a minimum interval of 10 sec;
     * this value is set at the Object Instance level.
     * d)Write-Attributes /3/0/9?gt=45&st=10 means the Battery Level will be notified to the Server
     * when:
     * a.old value is 20 and new value is 35 due to step condition
     * b.old value is 45 and new value is 50 due to gt condition
     * c.old value is 50 and new value is 40 due to both gt and step conditions
     * d.old value is 35 and new value is 20 due to step conditione)
     * Write-Attributes /3/0/9?lt=20&gt=85&st=10 means the Battery Level will be notified to the Server
     * when:
     * a.old value is 17 and new value is 24 due to lt condition
     * b.old value is 75 and new value is 90 due to both gt and step conditions
     * String uriQueries = "pmin=10&pmax=60";
     * AttributeSet attributes = AttributeSet.parse(uriQueries);
     * WriteAttributesRequest request = new WriteAttributesRequest(target, attributes);
     * Attribute gt = new Attribute(GREATER_THAN, Double.valueOf("45"));
     * Attribute st = new Attribute(LESSER_THAN, Double.valueOf("10"));
     * Attribute pmax = new Attribute(MAXIMUM_PERIOD, "60");
     * Attribute [] attrs = {gt, st};
     */
    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `target`：`target` 参数。
     * - `params`：`params` 参数。
     * - `serviceImpl`：服务对象。
     * 返回：处理结果。
     */
    public static SimpleDownlinkRequest createWriteAttributeRequest(String target, Object params, LwM2mUplinkMsgHandler serviceImpl) {
        AttributeSet attrSet = new AttributeSet(createWriteAttributes(params, serviceImpl, target));
        return attrSet.getAttributes().size() > 0 ? new WriteAttributesRequest(target, attrSet) : null;
    }

    /**
     * 功能：保存或创建`Write Attributes`。
     * 参数：
     * - `params`：`params` 参数。
     * - `serviceImpl`：服务对象。
     * - `target`：`target` 参数。
     * 返回：处理结果。
     */
    private static Attribute[] createWriteAttributes(Object params, LwM2mUplinkMsgHandler serviceImpl, String target) {
        List<Attribute> attributeLists = new ArrayList<>();
        Map<String, Object> map = JacksonUtil.convertValue(params, new TypeReference<>() {
        });
        map.forEach((k, v) -> {
            if (StringUtils.trimToNull(v.toString()) != null) {
                Object attrValue = convertWriteAttributes(k, v, serviceImpl, target);
                if (attrValue != null) {
                    Attribute attribute = createAttribute(k, attrValue);
                    if (attribute != null) {
                        attributeLists.add(new Attribute(k, attrValue));
                    }
                }
            }
        });
        return attributeLists.toArray(Attribute[]::new);
    }

    /**
     * "UNSIGNED_INTEGER":  // Number -> Integer Example:
     * Alarm Timestamp [32-bit unsigned integer]
     * Short Server ID, Object ID, Object Instance ID, Resource ID, Resource Instance ID
     * "CORELINK": // String used in Attribute
     */
    /**
     * 功能：执行 `equalsResourceTypeGetSimpleName` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    public static ResourceModel.Type equalsResourceTypeGetSimpleName(Object value) {
        switch (value.getClass().getSimpleName()) {
            case "Double":
                return FLOAT;
            case "Integer":
                return INTEGER;
            case "String":
                return STRING;
            case "Boolean":
                return BOOLEAN;
            case "byte[]":
                return OPAQUE;
            case "Date":
                return TIME;
            case "ObjectLink":
                return OBJLNK;
            default:
                return null;
        }
    }

    /**
     * 功能：校验`Versioned Id`。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * 返回：无。
     */
    public static void validateVersionedId(LwM2mClient client, HasVersionedId request) {
        String msgExceptionStr = "";
        if (request.getObjectId() == null) {
            msgExceptionStr = "Specified object id is null!";
        } else {
            msgExceptionStr = client.isValidObjectVersion(request.getVersionedId());
        }
        if (!msgExceptionStr.isEmpty()) {
            throw new IllegalArgumentException(msgExceptionStr);
        }
    }

    /**
     * 功能：转换RPC。
     * 参数：
     * - `value`：值。
     * - `type`：类型。
     * - `versionedId`：`versionedId`ID。
     * 返回：处理结果。
     */
    public static Map<Integer, Object> convertMultiResourceValuesFromRpcBody(Object value, ResourceModel.Type type, String versionedId) throws Exception {
            String valueJsonStr = JacksonUtil.toString(value);
            JsonElement element = JsonUtils.parse(valueJsonStr);
            return convertMultiResourceValuesFromJson(element, type, versionedId);
    }

    /**
     * 功能：转换JSON。
     * 参数：
     * - `newValProto`：`newValProto` 参数。
     * - `type`：类型。
     * - `versionedId`：`versionedId`ID。
     * 返回：处理结果。
     */
    public static Map<Integer, Object> convertMultiResourceValuesFromJson(JsonElement newValProto, ResourceModel.Type type, String versionedId) {
        Map<Integer, Object> newValues = new HashMap<>();
        newValProto.getAsJsonObject().entrySet().forEach((obj) -> {
            newValues.put(Integer.valueOf(obj.getKey()), LwM2mValueConverterImpl.getInstance().convertValue(obj.getValue().getAsString(),
                    STRING, type, new LwM2mPath(fromVersionedIdToObjectId(versionedId))));
        });
        return newValues;
    }

    /**
     * 功能：转换`Write Attributes`。
     * 参数：
     * - `type`：类型。
     * - `value`：值。
     * - `serviceImpl`：服务对象。
     * - `target`：`target` 参数。
     * 返回：处理结果。
     */
    public static Object convertWriteAttributes(String type, Object value, LwM2mUplinkMsgHandler serviceImpl, String target) {
        switch (type) {
            /** Integer [0:255]; */
            case DIMENSION:
                Long dim = (Long) serviceImpl.getConverter().convertValue(value, equalsResourceTypeGetSimpleName(value), INTEGER, new LwM2mPath(target));
                return dim >= 0 && dim <= 255 ? dim : null;
            /**String;*/
            case OBJECT_VERSION:
                return serviceImpl.getConverter().convertValue(value, equalsResourceTypeGetSimpleName(value), STRING, new LwM2mPath(target));
            /**INTEGER */
            case MINIMUM_PERIOD:
            case MAXIMUM_PERIOD:
                return serviceImpl.getConverter().convertValue(value, equalsResourceTypeGetSimpleName(value), INTEGER, new LwM2mPath(target));
            /**Float; */
            case GREATER_THAN:
            case LESSER_THAN:
            case STEP:
                if (value.getClass().getSimpleName().equals("String")) {
                    value = Double.valueOf((String) value);
                }
                return serviceImpl.getConverter().convertValue(value, equalsResourceTypeGetSimpleName(value), FLOAT, new LwM2mPath(target));
            default:
                return null;
        }
    }

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `key`：键。
     * - `attrValue`：值。
     * 返回：处理结果。
     */
    private static Attribute createAttribute(String key, Object attrValue) {
        try {
            return new Attribute(key, attrValue);
        } catch (Exception e) {
            log.error("CreateAttribute, not valid parameter key: [{}], attrValue: [{}], error: [{}]", key, attrValue, e.getMessage());
            return null;
        }
    }

    /**
     * @param lwM2MClient -
     * @param path        -
     * @return - return value of Resource by idPath
     */
    /**
     * 功能：获取值。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `path`：文件或资源路径。
     * 返回：处理结果。
     */
    public static LwM2mResource getResourceValueFromLwM2MClient(LwM2mClient lwM2MClient, String path) {
        LwM2mResource lwm2mResourceValue = null;
        ResourceValue resourceValue = lwM2MClient.getResources().get(path);
        if (resourceValue != null) {
            if (new LwM2mPath(fromVersionedIdToObjectId(path)).isResource()) {
                lwm2mResourceValue = lwM2MClient.getResources().get(path).getLwM2mResource();
            }
        }
        return lwm2mResourceValue;
    }

    /**
     * 功能：执行 `contentToString` 对应的处理。
     * 参数：
     * - `content`：`content` 参数。
     * 返回：可能存在的结果。
     */
    @SuppressWarnings("unchecked")
    public static Optional<String> contentToString(Object content) {
        try {
            String value = null;
            LwM2mResource resource = null;
            String key = null;
            if (content instanceof Map) {
                Map<Object, Object> contentAsMap = (Map<Object, Object>) content;
                if (contentAsMap.size() == 1) {
                    for (Map.Entry<Object, Object> kv : contentAsMap.entrySet()) {
                        if (kv.getValue() instanceof LwM2mResource) {
                            key = kv.getKey().toString();
                            resource = (LwM2mResource) kv.getValue();
                        }
                    }
                }
            } else if (content instanceof LwM2mResource) {
                resource = (LwM2mResource) content;
            }
            if (resource != null && resource.getType() == OPAQUE) {
                value = opaqueResourceToString(resource, key);
            }
            value = value == null ? content.toString() : value;
            return Optional.of(value);
        } catch (Exception e) {
            log.debug("Failed to convert content " + content + " to string", e);
            return Optional.ofNullable(content != null ? content.toString() : null);
        }
    }

    /**
     * 功能：执行 `opaqueResourceToString` 对应的处理。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `key`：键。
     * 返回：文本结果。
     */
    private static String opaqueResourceToString(LwM2mResource resource, String key) {
        String value = null;
        StringBuilder builder = new StringBuilder();
        if (resource instanceof LwM2mSingleResource) {
            builder.append("LwM2mSingleResource");
            if (key == null) {
                builder.append(" id=").append(String.valueOf(resource.getId()));
            } else {
                builder.append(" key=").append(key);
            }
            builder.append(" value=").append(opaqueToString((byte[]) resource.getValue()));
            builder.append(" type=").append(OPAQUE.toString());
            value = builder.toString();
        } else if (resource instanceof LwM2mMultipleResource) {
            builder.append("LwM2mMultipleResource");
            if (key == null) {
                builder.append(" id=").append(String.valueOf(resource.getId()));
            } else {
                builder.append(" key=").append(key);
            }
            builder.append(" values={");
            if (resource.getInstances().size() > 0) {
                builder.append(multiInstanceOpaqueToString((LwM2mMultipleResource) resource));
            }
            builder.append("}");
            builder.append(" type=").append(OPAQUE.toString());
            value = builder.toString();
        }
        return value;
    }

    /**
     * 功能：执行 `multiInstanceOpaqueToString` 对应的处理。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：文本结果。
     */
    private static String multiInstanceOpaqueToString(LwM2mMultipleResource resource) {
        StringBuilder builder = new StringBuilder();
        resource.getInstances().values()
                .forEach(v -> builder.append(" id=").append(v.getId()).append(" value=").append(Hex.encodeHexString((byte[]) v.getValue())).append(", "));
        int startInd = builder.lastIndexOf(", ");
        if (startInd > 0) {
            builder.delete(startInd, startInd + 2);
        }
        return builder.toString();
    }

    /**
     * 功能：执行 `opaqueToString` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    private static String opaqueToString(byte[] value) {
        String opaque = Hex.encodeHexString(value);
        return opaque.length() > 1024 ? opaque.substring(0, 1024) : opaque;
    }

    /**
     * 功能：保存或创建`Models Default`。
     * 参数：无。
     * 返回：处理结果。
     */
    public static LwM2mModel createModelsDefault() {
        return new StaticModel(ObjectLoader.loadDefault());
    }

    /**
     * 功能：执行 `compareAttNameKeyOta` 对应的处理。
     * 参数：
     * - `attrName`：名称。
     * 返回：判断结果。
     */
    public static boolean compareAttNameKeyOta(String attrName) {
        for (OtaPackageKey value : OtaPackageKey.values()) {
            if (attrName.contains(value.getValue())) return true;
        }
        return false;
    }

    /**
     * 功能：执行 `valueEquals` 对应的处理。
     * 参数：
     * - `newValue`：值。
     * - `oldValue`：值。
     * 返回：判断结果。
     */
    public static boolean valueEquals(Object newValue, Object oldValue) {
        String newValueStr;
        String oldValueStr;
        if (oldValue instanceof byte[]) {
            oldValueStr = Hex.encodeHexString((byte[]) oldValue);
        } else {
            oldValueStr = oldValue.toString();
        }
        if (newValue instanceof byte[]) {
            newValueStr = Hex.encodeHexString((byte[]) newValue);
        } else {
            newValueStr = newValue.toString();
        }
        return newValueStr.equals(oldValueStr);
    }
}
