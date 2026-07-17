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
package org.thingsboard.server.transport.snmp.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.util.TypeCastUtil;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `PduService` 是 ThingsBoard Common Transport 中负责 SNMP PDU 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@TbSnmpTransportComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class PduService {

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${transport.snmp.max_request_oids:100}")
    private int maxRequestOids;

    /**
     * 是否忽略对应检查。
     */
    @Value("${transport.snmp.response.ignore_type_cast_errors:false}")
    private boolean ignoreTypeCastErrors;

    /**
     * 功能：保存或创建`Pdus`。
     * 参数：
     * - `sessionContext`：处理上下文。
     * - `communicationConfig`：配置对象。
     * - `values`：值。
     * 返回：匹配的数据集合。
     */
    public List<PDU> createPdus(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        List<PDU> pdus = new ArrayList<>();
        List<SnmpMapping> allMappings = communicationConfig.getAllMappings();

        for (List<SnmpMapping> mappings : Lists.partition(allMappings, maxRequestOids)) {
            PDU pdu = setUpPdu(sessionContext);
            pdu.setType(communicationConfig.getMethod().getCode());
            pdu.addAll(mappings.stream()
                    .filter(mapping -> values.isEmpty() || values.containsKey(mapping.getKey()))
                    .map(mapping -> Optional.ofNullable(values.get(mapping.getKey()))
                            .map(value -> {
                                Variable variable = toSnmpVariable(value, mapping.getDataType());
                                return new VariableBinding(new OID(mapping.getOid()), variable);
                            })
                            .orElseGet(() -> new VariableBinding(new OID(mapping.getOid()))))
                    .collect(Collectors.toList()));
            if (pdu.size() > 0) {
                pdus.add(pdu);
            }
        }

        return pdus;
    }

    /**
     * 功能：保存或创建`Single Variable Pdu`。
     * 参数：
     * - `sessionContext`：处理上下文。
     * - `snmpMethod`：`snmpMethod` 参数。
     * - `oid`：`oid`ID。
     * - `value`：值。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public PDU createSingleVariablePdu(DeviceSessionContext sessionContext, SnmpMethod snmpMethod, String oid, String value, DataType dataType) {
        PDU pdu = setUpPdu(sessionContext);
        pdu.setType(snmpMethod.getCode());

        Variable variable = value == null ? Null.instance : toSnmpVariable(value, dataType);
        pdu.add(new VariableBinding(new OID(oid), variable));

        return pdu;
    }

    /**
     * 功能：执行 `toSnmpVariable` 对应的处理。
     * 参数：
     * - `value`：值。
     * - `dataType`：待处理数据。
     * 返回：处理结果。
     */
    private Variable toSnmpVariable(String value, DataType dataType) {
        dataType = dataType == null ? DataType.STRING : dataType;
        Variable variable;
        switch (dataType) {
            case LONG:
                try {
                    variable = new Integer32(Integer.parseInt(value));
                    break;
                } catch (NumberFormatException ignored) {
                }
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case JSON:
            default:
                variable = new OctetString(value);
        }
        return variable;
    }

    /**
     * 功能：更新`Up Pdu`。
     * 参数：
     * - `sessionContext`：处理上下文。
     * 返回：处理结果。
     */
    private PDU setUpPdu(DeviceSessionContext sessionContext) {
        PDU pdu;
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        SnmpProtocolVersion snmpVersion = deviceTransportConfiguration.getProtocolVersion();
        switch (snmpVersion) {
            case V1:
            case V2C:
                pdu = new PDU();
                break;
            case V3:
                ScopedPDU scopedPdu = new ScopedPDU();
                scopedPdu.setContextName(new OctetString(deviceTransportConfiguration.getContextName()));
                scopedPdu.setContextEngineID(new OctetString(deviceTransportConfiguration.getEngineId()));
                pdu = scopedPdu;
                break;
            default:
                throw new UnsupportedOperationException("SNMP version " + snmpVersion + " is not supported");
        }
        return pdu;
    }


    /**
     * 功能：处理`Pdus`。
     * 参数：
     * - `pdus`：数据列表。
     * - `responseMappings`：响应对象。
     * 返回：处理结果。
     */
    public JsonObject processPdus(List<PDU> pdus, List<SnmpMapping> responseMappings) {
        Map<OID, String> values = processPdus(pdus);

        Map<OID, SnmpMapping> mappings = new HashMap<>();
        if (responseMappings != null) {
            for (SnmpMapping mapping : responseMappings) {
                OID oid = new OID(mapping.getOid());
                mappings.put(oid, mapping);
            }
        }

        JsonObject data = new JsonObject();
        values.forEach((oid, value) -> {
            log.trace("Processing variable binding: {} - {}", oid, value);

            SnmpMapping mapping = mappings.get(oid);
            if (mapping == null) {
                log.debug("No SNMP mapping for oid {}", oid);
                return;
            }

            processValue(mapping.getKey(), mapping.getDataType(), value, data);
        });

        return data;
    }

    /**
     * 功能：处理`Pdus`。
     * 参数：
     * - `pdus`：数据列表。
     * 返回：处理结果。
     */
    public Map<OID, String> processPdus(List<PDU> pdus) {
        return pdus.stream()
                .flatMap(pdu -> pdu.getVariableBindings().stream())
                .filter(Objects::nonNull)
                .filter(variableBinding -> !(variableBinding.getVariable() instanceof Null))
                .collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::toValueString));
    }

    /**
     * 功能：处理值。
     * 参数：
     * - `key`：键。
     * - `dataType`：待处理数据。
     * - `value`：值。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    public void processValue(String key, DataType dataType, String value, JsonObject result) {
        try {
            switch (dataType) {
                case STRING:
                case JSON:
                    result.addProperty(key, value);
                    break;
                case LONG:
                case DOUBLE:
                    result.addProperty(key, TypeCastUtil.castToNumber(value).getValue());
                    break;
                case BOOLEAN:
                    if (StringUtils.equalsAnyIgnoreCase(value, "true", "false")) {
                        result.addProperty(key, Boolean.parseBoolean(value));
                    } else {
                        throw new IllegalArgumentException("Can't parse '" + value + "' as boolean");
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            if (ignoreTypeCastErrors) {
                log.debug("Ignoring value '{}' for key '{}' because of data type mismatch ({} required)", value, key, dataType);
            } else {
                throw e;
            }
        }
    }

}
