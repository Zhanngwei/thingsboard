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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.common.util.JacksonUtil.newArrayNode;

/**
 * Provides utility methods for SparkplugB MQTT Payload Metric.
 */
/**
 * 中文说明：
 * 1. 类目的：`SparkplugMetricUtil` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class SparkplugMetricUtil {

    /**
     * 功能：执行 `fromSparkplugBMetricToKeyValueProto` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `protoMetric`：`protoMetric` 参数。
     * 返回：可能存在的结果。
     */
    public static Optional<TransportProtos.KeyValueProto> fromSparkplugBMetricToKeyValueProto(String key, SparkplugBProto.Payload.Metric protoMetric) throws ThingsboardException {
        // Check if the null flag has been set indicating that the value is null
        if (protoMetric.getIsNull()) {
            return Optional.empty();
        }
        // Otherwise convert the value based on the type
        int metricType = protoMetric.getDatatype();
        TransportProtos.KeyValueProto.Builder builderProto = TransportProtos.KeyValueProto.newBuilder();
        ArrayNode nodeArray = newArrayNode();
        MetricDataType metricDataType = MetricDataType.fromInteger(metricType);
        try {
            switch (metricDataType) {
                case Boolean:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.BOOLEAN_V)
                            .setBoolV(protoMetric.getBooleanValue()).build());
                case DateTime:
                case Int64:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(protoMetric.getLongValue()).build());
                case Float:
                    var f = new BigDecimal(String.valueOf(protoMetric.getFloatValue()));
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.DOUBLE_V)
                            .setDoubleV(f.doubleValue()).build());
                case Double:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(Double.valueOf(protoMetric.getDoubleValue()).longValue()).build());
                case Int8:
                case UInt8:
                case Int16:
                case Int32:
                case UInt16:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                            .setLongV(protoMetric.getIntValue()).build());
                case UInt32:
                case UInt64:
                    if (protoMetric.hasIntValue()) {
                        return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                                .setLongV(protoMetric.getIntValue()).build());
                    } else if (protoMetric.hasLongValue()) {
                        return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.LONG_V)
                                .setLongV(protoMetric.getLongValue()).build());
                    } else {
                        log.error("Invalid value for UInt32 datatype");
                        throw new ThingsboardException("Invalid value for " + MetricDataType.fromInteger(metricType).name() + " datatype " + metricType, ThingsboardErrorCode.INVALID_ARGUMENTS);
                    }
                case String:
                case Text:
                case UUID:
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                            .setStringV(protoMetric.getStringValue()).build());
                // byte[]
                case Bytes:
                    ByteBuffer byteBuffer = ByteBuffer.wrap(protoMetric.getBytesValue().toByteArray());
                    while (byteBuffer.hasRemaining()) {
                        nodeArray.add(byteBuffer.get());
                    }
                    return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.JSON_V)
                            .setJsonV(nodeArray.toString()).build());
                case DataSet:
                case Template:
                case File:
                    //TODO
                    // Build the and create the DataSet
                    /**
                     SparkplugBProto.Payload.DataSet protoDataSet = protoMetric.getDatasetValue();
                     return new SparkplugBProto.Payload.DataSet.Builder(protoDataSet.getNumOfColumns()).addColumnNames(protoDataSet.getColumnsList())
                     .addTypes(convertDataSetDataTypes(protoDataSet.getTypesList()))
                     .addRows(convertDataSetRows(protoDataSet.getRowsList(), protoDataSet.getTypesList()))
                     .createDataSet();
                     return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                     .setStringV(protoDataSet.toString()).build());
                     **/
                    //TODO
                    // Build the and create the Template
                    /**
                     SparkplugBProto.Payload.Template protoTemplate = protoMetric.getTemplateValue();
                     return Optional.of(builderProto.setKey(key).setType(TransportProtos.KeyValueType.STRING_V)
                     .setStringV( protoTemplate.toString()).build());
                     **/
                    //TODO
                    // Build the and create the File
                    /**
                     String filename = protoMetric.getMetadata().getFileName();
                     return Optional.of(builderPrbyteValueoto.setKey(key + "_" + filename).setType(TransportProtos.KeyValueType.STRING_V)
                     .setStringV(Hex.encodeHexString((protoMetric.getBytesValue().toByteArray()))).build());
                     **/
                    return Optional.empty();
                case Unknown:
                default:
                    throw new ThingsboardException("Failed to decode: Unknown MetricDataType " + metricType, ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        } catch (Exception e) {
            log.error("", e);
            return Optional.empty();
        }
    }

    /**
     * 功能：保存或创建`Metric`。
     * 参数：
     * - `value`：值。
     * - `ts`：时间戳。
     * - `key`：键。
     * - `metricDataType`：待处理数据。
     * 返回：处理结果。
     */
    public static SparkplugBProto.Payload.Metric createMetric(Object value, long ts, String key, MetricDataType metricDataType) throws ThingsboardException {
        SparkplugBProto.Payload.Metric metric = SparkplugBProto.Payload.Metric.newBuilder()
                .setTimestamp(ts)
                .setName(key)
                .setDatatype(metricDataType.toIntValue())
                .build();
        switch (metricDataType) {
            case Int8:      //  (byte)
                return metric.toBuilder().setIntValue(((Byte) value).intValue()).build();
            case Int16:     // (short)
            case UInt8:
                return metric.toBuilder().setIntValue(((Short) value).intValue()).build();
            case UInt16:     //  (int)
            case Int32:
                return metric.toBuilder().setIntValue(((Integer) value).intValue()).build();
            case UInt32:     // (long)
            case Int64:
            case UInt64:
            case DateTime:
                return metric.toBuilder().setLongValue(((Long) value).longValue()).build();
            case Float:     // (float)
                return metric.toBuilder().setFloatValue(((Float) value).floatValue()).build();
            case Double:     // (double)
                return metric.toBuilder().setDoubleValue(((Double) value).doubleValue()).build();
            case Boolean:      // (boolean)
                return metric.toBuilder().setBooleanValue(((Boolean) value).booleanValue()).build();
            case String:        // String)
            case Text:
            case UUID:
                return metric.toBuilder().setStringValue((String) value).build();
            case Bytes:
                ByteString byteString = ByteString.copyFrom((byte[]) value);
                return metric.toBuilder().setBytesValue(byteString).build();
            case DataSet:
                return metric.toBuilder().setDatasetValue((SparkplugBProto.Payload.DataSet) value).build();
            case File:
                SparkplugMetricUtil.File file = (SparkplugMetricUtil.File) value;
                ByteString byteFileString = ByteString.copyFrom(file.getBytes());
                return metric.toBuilder().setBytesValue(byteFileString).build();
            case Template:
                return metric.toBuilder().setTemplateValue((SparkplugBProto.Payload.Template) value).build();
            case Unknown:
                throw new ThingsboardException("Invalid value for MetricDataType " + metricDataType.name(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return metric;
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    public static TransportProtos.TsKvProto getTsKvProto(String key, Object value, long ts) throws ThingsboardException {
        try {
            TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
            TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
            keyValueProtoBuilder.setKey(key);
            if (value instanceof String) {
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.STRING_V);
                keyValueProtoBuilder.setStringV((String) value);
            } else if (value instanceof Integer) {
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                keyValueProtoBuilder.setLongV((Integer) value);
            } else if (value instanceof Long) {
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                keyValueProtoBuilder.setLongV((Long) value);
            } else if (value instanceof Boolean) {
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.BOOLEAN_V);
                keyValueProtoBuilder.setBoolV((Boolean) value);
            } else if (value instanceof Double) {
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.DOUBLE_V);
                keyValueProtoBuilder.setDoubleV((Double) value);
            } else if (value instanceof List) {
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.JSON_V);
                ArrayNode arrayNodeBytes = JacksonUtil.convertValue(value, ArrayNode.class);
                keyValueProtoBuilder.setJsonV(arrayNodeBytes.toString());
            } else {
                throw new ThingsboardException("Failed to convert device/node RPC command to TsKvProto for Sparkplug MQT msg: value [" + value + "]", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            tsKvProtoBuilder.setKv(keyValueProtoBuilder.build());
            tsKvProtoBuilder.setTs(ts);
            return tsKvProtoBuilder.build();
        } catch (Exception e) {
            throw new ThingsboardException("Failed to convert device/node RPC command to TsKvProto for Sparkplug MQT msg: value [" + value + "]", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }

    /**
     * 功能：执行 `validatedValueByTypeMetric` 对应的处理。
     * 参数：
     * - `kv`：`kv` 参数。
     * - `metricDataType`：待处理数据。
     * 返回：可能存在的结果。
     */
    public static Optional<Object> validatedValueByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) throws ThingsboardException {
        if (kv.getTypeValue() <= 3) {
            return validatedValuePrimitiveByTypeMetric(kv, metricDataType);
        } else if (kv.getTypeValue() == 4) {
            JsonNode arrayNode = JacksonUtil.fromString(kv.getJsonV(), JsonNode.class);
            if (arrayNode.isArray()) {
                return validatedValueJsonByTypeMetric(kv.getJsonV(), metricDataType);
            }
        } else {
            throw new ThingsboardException("Invalid type KeyValueProto " + kv.toString() + " for MetricDataType " + metricDataType.name(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return Optional.empty();
    }

    /**
     * 功能：执行 `validatedValuePrimitiveByTypeMetric` 对应的处理。
     * 参数：
     * - `kv`：`kv` 参数。
     * - `metricDataType`：待处理数据。
     * 返回：可能存在的结果。
     */
    public static Optional<Object> validatedValuePrimitiveByTypeMetric(TransportProtos.KeyValueProto kv, MetricDataType metricDataType) throws ThingsboardException {
        Optional<String> valueOpt = getValueKvProtoPrimitive(kv);
        if (valueOpt.isPresent()) {
            try {
                switch (metricDataType) {
                    // int
                    case Int8:
                    case Int16:
                    case UInt8:
                    case UInt16:
                    case Int32:
                        Optional<Integer> boolInt8 = booleanStringToInt(valueOpt.get());
                        if (boolInt8.isPresent()) {
                            return Optional.of(boolInt8.get());
                        }
                        try {
                            return Optional.of(Integer.valueOf(valueOpt.get()));
                        } catch (NumberFormatException eInt) {
                            var i = new BigDecimal(valueOpt.get());
                            if (i.longValue() <= Integer.MAX_VALUE) {
                                return Optional.of(i.intValue());
                            }
                            throw new ThingsboardException("Invalid type value " + kv.toString() + " for MetricDataType "
                                    + metricDataType.name(), eInt, ThingsboardErrorCode.INVALID_ARGUMENTS);
                        }
                        // long
                    case UInt32:
                    case Int64:
                    case UInt64:
                    case DateTime:
                        Optional<Integer> boolInt64 = booleanStringToInt(valueOpt.get());
                        if (boolInt64.isPresent()) {
                            return Optional.of(Long.valueOf(boolInt64.get()));
                        }
                        var l = new BigDecimal(valueOpt.get());
                        return Optional.of(l.longValue());
                    // float
                    case Float:
                        Optional<Integer> boolFloat = booleanStringToInt(valueOpt.get());
                        if (boolFloat.isPresent()) {
                            var fb = new BigDecimal(boolFloat.get());
                            return Optional.of(fb.floatValue());
                        }
                        var f = new BigDecimal(valueOpt.get());
                        return Optional.of(f.floatValue());
                    // double
                    case Double:
                        Optional<Integer> boolDouble = booleanStringToInt(valueOpt.get());
                        if (boolDouble.isPresent()) {
                            return Optional.of(Double.valueOf(boolDouble.get()));
                        }
                        var dd = new BigDecimal(valueOpt.get());
                        return Optional.of(dd.doubleValue());
                    case Boolean:
                        if ("true".equals(valueOpt.get())) {
                            return Optional.of(true);
                        } else if ("false".equals(valueOpt.get())) {
                            return Optional.of(false);
                        } else {
                            Number number = NumberFormat.getInstance().parse(valueOpt.get());
                            if (StringUtils.isBlank(number.toString()) || "0".equals(number.toString())) { // ok 0
                                return Optional.of(false);
                            } else {
                                return Optional.of(true);
                            }
                        }
                    case String:
                    case Text:
                    case UUID:
                        return Optional.of(valueOpt.get());
                }
            } catch (Exception e) {
                log.trace("Invalid type value [{}] for MetricDataType [{}] [{}]", kv, metricDataType.name(), e.getMessage());
                throw new ThingsboardException("Invalid type value " + kv.toString() + " for MetricDataType " + metricDataType.name(), e, ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
        }
        return Optional.empty();
    }

    /**
     * 功能：执行 `validatedValueJsonByTypeMetric` 对应的处理。
     * 参数：
     * - `arrayNodeStr`：`arrayNodeStr` 参数。
     * - `metricDataType`：待处理数据。
     * 返回：可能存在的结果。
     */
    public static Optional<Object> validatedValueJsonByTypeMetric(String arrayNodeStr, MetricDataType metricDataType) {
        try {
            Optional<Object> valueOpt;
            switch (metricDataType) {
                // byte[]
                case Bytes:
                    List<Byte> listBytes = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {
                    });
                    byte[] bytes = new byte[listBytes.size()];
                    for (int i = 0; i < listBytes.size(); i++) {
                        bytes[i] = listBytes.get(i).byteValue();
                    }
                    return Optional.of(bytes);
                case DataSet:
                case File:
                case Template:
                    log.error("Invalid type value [{}] for MetricDataType [{}]", arrayNodeStr, metricDataType.name());
                    return Optional.empty();
                case Unknown:
                default:
                    log.error("Invalid MetricDataType [{}] type,  value [{}]", arrayNodeStr, metricDataType.name());
                    return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Invalid type value [{}] for MetricDataType [{}] [{}]", arrayNodeStr, metricDataType.name(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `kv`：`kv` 参数。
     * 返回：可能存在的结果。
     */
    private static Optional<String> getValueKvProtoPrimitive(TransportProtos.KeyValueProto kv) {
        if (kv.getTypeValue() == 0) {         // boolean
            return Optional.of(String.valueOf(kv.getBoolV()));
        } else if (kv.getTypeValue() == 1) {   // kvLong
            return Optional.of(String.valueOf(kv.getLongV()));
        } else if (kv.getTypeValue() == 2) {   // kvDouble/float
            return Optional.of(String.valueOf(kv.getDoubleV()));
        } else if (kv.getTypeValue() == 3) {   // kvString
            return Optional.of(kv.getStringV());
        } else {
            return Optional.empty();
        }
    }

    /**
     * 功能：执行 `booleanStringToInt` 对应的处理。
     * 参数：
     * - `booleanStr`：`booleanStr` 参数。
     * 返回：可能存在的结果。
     */
    private static Optional<Integer> booleanStringToInt(String booleanStr) {
        if ("true".equals(booleanStr)) {
            return Optional.of(1);
        } else if ("false".equals(booleanStr)) {
            return Optional.of(0);
        } else {
            return Optional.empty();
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`File` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    @JsonIgnoreProperties(
            value = {"fileName"})
    @JsonSerialize(
            using = FileSerializer.class)
    public class File {

        /**
         * 名称，用于标识或展示当前对象。
         */
        private String fileName;
        private byte[] bytes;

        /**
         * Default Constructor
         */
        /**
         * 功能：创建 `SparkplugMetricUtil` 实例，并初始化必要字段。
         * 参数：无。
         * 返回：新创建的对象实例。
         */
        public File() {
            super();
        }

        /**
         * Constructor
         *
         * @param fileName the full file name path
         * @param bytes    the array of bytes that represent the contents of the file
         */
        /**
         * 功能：创建 `SparkplugMetricUtil` 实例，并初始化必要字段。
         * 参数：
         * - `fileName`：名称。
         * - `bytes`：`bytes` 参数。
         * 返回：新创建的对象实例。
         */
        public File(String fileName, byte[] bytes) {
            super();
            this.fileName = fileName == null
                    ? null
                    : fileName.replace("/", System.getProperty("file.separator")).replace("\\",
                    System.getProperty("file.separator"));
            this.bytes = Arrays.copyOf(bytes, bytes.length);
        }

        /**
         * Gets the full filename path
         *
         * @return the full filename path
         */
        /**
         * 功能：获取名称。
         * 参数：无。
         * 返回：文本结果。
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Sets the full filename path
         *
         * @param fileName the full filename path
         */
        /**
         * 功能：更新名称。
         * 参数：
         * - `fileName`：名称。
         * 返回：无。
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Gets the bytes that represent the contents of the file
         *
         * @return the bytes that represent the contents of the file
         */
        /**
         * 功能：获取`Bytes`。
         * 参数：无。
         * 返回：处理结果。
         */
        public byte[] getBytes() {
            return bytes;
        }

        /**
         * Sets the bytes that represent the contents of the file
         *
         * @param bytes the bytes that represent the contents of the file
         */
        /**
         * 功能：更新`Bytes`。
         * 参数：
         * - `bytes`：`bytes` 参数。
         * 返回：无。
         */
        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        /**
         * 功能：生成当前对象的文本表示。
         * 参数：无。
         * 返回：文本结果。
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("File [fileName=");
            builder.append(fileName);
            builder.append(", bytes=");
            builder.append(Arrays.toString(bytes));
            builder.append("]");
            return builder.toString();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SparkplugMetricUtil` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
