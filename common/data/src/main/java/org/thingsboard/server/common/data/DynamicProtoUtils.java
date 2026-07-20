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

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.EnumDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.TypeElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DynamicProtoUtils` 是 ThingsBoard Common Data 中处理 `Dynamic Proto Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class DynamicProtoUtils {

    public static final Location LOCATION = new Location("", "", -1, -1);
    /**
     * Protobuf常量，用于统一引用固定值。
     */
    public static final String PROTO_3_SYNTAX = "proto3";

    /**
     * 功能：获取`Descriptor`。
     * 参数：
     * - `protoSchema`：`protoSchema` 参数。
     * - `schemaName`：名称。
     * 返回：处理结果。
     */
    public static Descriptors.Descriptor getDescriptor(String protoSchema, String schemaName) {
        try {
            DynamicMessage.Builder builder = getDynamicMessageBuilder(protoSchema, schemaName);
            return builder.getDescriptorForType();
        } catch (Exception e) {
            log.warn("Failed to get Message Descriptor due to {}", e.getMessage());
            return null;
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `protoSchema`：`protoSchema` 参数。
     * - `schemaName`：名称。
     * 返回：处理结果。
     */
    public static DynamicMessage.Builder getDynamicMessageBuilder(String protoSchema, String schemaName) {
        ProtoFileElement protoFileElement = getProtoFileElement(protoSchema);
        DynamicSchema dynamicSchema = getDynamicSchema(protoFileElement, schemaName);
        String lastMsgName = getMessageTypes(protoFileElement.getTypes()).stream()
                .map(MessageElement::getName).reduce((previous, last) -> last).get();
        return dynamicSchema.newMessageBuilder(lastMsgName);
    }

    /**
     * 功能：获取`Dynamic Schema`。
     * 参数：
     * - `protoFileElement`：`protoFileElement` 参数。
     * - `schemaName`：名称。
     * 返回：处理结果。
     */
    public static DynamicSchema getDynamicSchema(ProtoFileElement protoFileElement, String schemaName) {
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        schemaBuilder.setName(schemaName);
        schemaBuilder.setSyntax(PROTO_3_SYNTAX);
        schemaBuilder.setPackage(StringUtils.isNotEmpty(protoFileElement.getPackageName()) ?
                protoFileElement.getPackageName() : schemaName.toLowerCase());
        List<TypeElement> types = protoFileElement.getTypes();
        List<MessageElement> messageTypes = getMessageTypes(types);

        if (!messageTypes.isEmpty()) {
            List<EnumElement> enumTypes = getEnumElements(types);
            if (!enumTypes.isEmpty()) {
                enumTypes.forEach(enumElement -> {
                    EnumDefinition enumDefinition = getEnumDefinition(enumElement);
                    schemaBuilder.addEnumDefinition(enumDefinition);
                });
            }
            List<MessageDefinition> messageDefinitions = getMessageDefinitions(messageTypes);
            messageDefinitions.forEach(schemaBuilder::addMessageDefinition);
            try {
                return schemaBuilder.build();
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException("Failed to create dynamic schema due to: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Failed to get Dynamic Schema! Message types is empty for schema:" + schemaName);
        }
    }

    /**
     * 功能：获取文件。
     * 参数：
     * - `protoSchema`：`protoSchema` 参数。
     * 返回：处理结果。
     */
    public static ProtoFileElement getProtoFileElement(String protoSchema) {
        return new ProtoParser(LOCATION, protoSchema.toCharArray()).readProtoFile();
    }

    /**
     * 功能：执行 `dynamicMsgToJson` 对应的处理。
     * 参数：
     * - `descriptor`：`descriptor` 参数。
     * - `payload`：`payload` 参数。
     * 返回：文本结果。
     */
    public static String dynamicMsgToJson(Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

    /**
     * 功能：执行 `jsonToDynamicMessage` 对应的处理。
     * 参数：
     * - `builder`：`builder` 参数。
     * - `payload`：`payload` 参数。
     * 返回：处理结果。
     */
    public static DynamicMessage jsonToDynamicMessage(DynamicMessage.Builder builder, String payload) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(payload, builder);
        return builder.build();
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `types`：类型。
     * 返回：匹配的数据集合。
     */
    private static List<MessageElement> getMessageTypes(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof MessageElement)
                .map(typeElement -> (MessageElement) typeElement)
                .collect(Collectors.toList());
    }

    /**
     * 功能：获取`Enum Elements`。
     * 参数：
     * - `types`：类型。
     * 返回：匹配的数据集合。
     */
    private static List<EnumElement> getEnumElements(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof EnumElement)
                .map(typeElement -> (EnumElement) typeElement)
                .collect(Collectors.toList());
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `messageElementsList`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private static List<MessageDefinition> getMessageDefinitions(List<MessageElement> messageElementsList) {
        if (!messageElementsList.isEmpty()) {
            List<MessageDefinition> messageDefinitions = new ArrayList<>();
            messageElementsList.forEach(messageElement -> {
                MessageDefinition.Builder messageDefinitionBuilder = MessageDefinition.newBuilder(messageElement.getName());

                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        nestedEnumTypes.forEach(enumElement -> {
                            EnumDefinition nestedEnumDefinition = getEnumDefinition(enumElement);
                            messageDefinitionBuilder.addEnumDefinition(nestedEnumDefinition);
                        });
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    List<MessageDefinition> nestedMessageDefinitions = getMessageDefinitions(nestedMessageTypes);
                    nestedMessageDefinitions.forEach(messageDefinitionBuilder::addMessageDefinition);
                }
                List<FieldElement> messageElementFields = messageElement.getFields();
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    for (OneOfElement oneOfelement : oneOfs) {
                        MessageDefinition.OneofBuilder oneofBuilder = messageDefinitionBuilder.addOneof(oneOfelement.getName());
                        addMessageFieldsToTheOneOfDefinition(oneOfelement.getFields(), oneofBuilder);
                    }
                }
                if (!messageElementFields.isEmpty()) {
                    addMessageFieldsToTheMessageDefinition(messageElementFields, messageDefinitionBuilder);
                }
                messageDefinitions.add(messageDefinitionBuilder.build());
            });
            return messageDefinitions;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 功能：获取`Enum Definition`。
     * 参数：
     * - `enumElement`：`enumElement` 参数。
     * 返回：处理结果。
     */
    private static EnumDefinition getEnumDefinition(EnumElement enumElement) {
        List<EnumConstantElement> enumElementTypeConstants = enumElement.getConstants();
        EnumDefinition.Builder enumDefinitionBuilder = EnumDefinition.newBuilder(enumElement.getName());
        if (!enumElementTypeConstants.isEmpty()) {
            enumElementTypeConstants.forEach(constantElement -> enumDefinitionBuilder.addValue(constantElement.getName(), constantElement.getTag()));
        }
        return enumDefinitionBuilder.build();
    }


    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `messageElementFields`：待处理消息。
     * - `messageDefinitionBuilder`：待处理消息。
     * 返回：无。
     */
    private static void addMessageFieldsToTheMessageDefinition(List<FieldElement> messageElementFields, MessageDefinition.Builder messageDefinitionBuilder) {
        messageElementFields.forEach(fieldElement -> {
            String labelStr = null;
            if (fieldElement.getLabel() != null) {
                labelStr = fieldElement.getLabel().name().toLowerCase();
            }
            messageDefinitionBuilder.addField(
                    labelStr,
                    fieldElement.getType(),
                    fieldElement.getName(),
                    fieldElement.getTag());
        });
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `oneOfsElementFields`：数据列表。
     * - `oneofBuilder`：`oneofBuilder` 参数。
     * 返回：无。
     */
    private static void addMessageFieldsToTheOneOfDefinition(List<FieldElement> oneOfsElementFields, MessageDefinition.OneofBuilder oneofBuilder) {
        oneOfsElementFields.forEach(fieldElement -> oneofBuilder.addField(
                fieldElement.getType(),
                fieldElement.getName(),
                fieldElement.getTag()));
        oneofBuilder.msgDefBuilder();
    }

    // validation

    /**
     * 功能：校验Protobuf。
     * 参数：
     * - `schema`：`schema` 参数。
     * - `schemaName`：名称。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：无。
     */
    public static void validateProtoSchema(String schema, String schemaName, String exceptionPrefix) throws IllegalArgumentException {
        ProtoParser schemaParser = new ProtoParser(LOCATION, schema.toCharArray());
        ProtoFileElement protoFileElement;
        try {
            protoFileElement = schemaParser.readProtoFile();
        } catch (Exception e) {
            throw new IllegalArgumentException(exceptionPrefix + " failed to parse " + schemaName + " due to: " + e.getMessage());
        }
        checkProtoFileSyntax(schemaName, protoFileElement);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getOptions().isEmpty(), " Schema options don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getPublicImports().isEmpty(), " Schema public imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getImports().isEmpty(), " Schema imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getExtendDeclarations().isEmpty(), " Schema extend declarations don't support!", exceptionPrefix);
        checkTypeElements(schemaName, protoFileElement, exceptionPrefix);
    }

    /**
     * 功能：校验文件。
     * 参数：
     * - `schemaName`：名称。
     * - `protoFileElement`：`protoFileElement` 参数。
     * 返回：无。
     */
    private static void checkProtoFileSyntax(String schemaName, ProtoFileElement protoFileElement) {
        if (protoFileElement.getSyntax() == null || !protoFileElement.getSyntax().equals(Syntax.PROTO_3)) {
            throw new IllegalArgumentException("[Transport Configuration] invalid schema syntax: " + protoFileElement.getSyntax() +
                    " for " + schemaName + " provided! Only " + Syntax.PROTO_3 + " allowed!");
        }
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `schemaName`：名称。
     * - `isEmptySettings`：配置对象。
     * - `invalidSettingsMessage`：配置对象。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：无。
     */
    private static void checkProtoFileCommonSettings(String schemaName, boolean isEmptySettings, String invalidSettingsMessage, String exceptionPrefix) {
        if (!isEmptySettings) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + invalidSettingsMessage);
        }
    }

    /**
     * 功能：校验类型。
     * 参数：
     * - `schemaName`：名称。
     * - `protoFileElement`：`protoFileElement` 参数。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：无。
     */
    private static void checkTypeElements(String schemaName, ProtoFileElement protoFileElement, String exceptionPrefix) {
        List<TypeElement> types = protoFileElement.getTypes();
        if (!types.isEmpty()) {
            if (types.stream().noneMatch(typeElement -> typeElement instanceof MessageElement)) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " At least one Message definition should exists!");
            } else {
                checkEnumElements(schemaName, getEnumElements(types), exceptionPrefix);
                checkMessageElements(schemaName, getMessageTypes(types), exceptionPrefix);
            }
        } else {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Type elements is empty!");
        }
    }

    /**
     * 功能：校验字段名。
     * 参数：
     * - `schemaName`：名称。
     * - `fieldElements`：数据列表。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：无。
     */
    private static void checkFieldElements(String schemaName, List<FieldElement> fieldElements, String exceptionPrefix) {
        if (!fieldElements.isEmpty()) {
            boolean hasRequiredLabel = fieldElements.stream().anyMatch(fieldElement -> {
                Field.Label label = fieldElement.getLabel();
                return label != null && label.equals(Field.Label.REQUIRED);
            });
            if (hasRequiredLabel) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Required labels are not supported!");
            }
            boolean hasDefaultValue = fieldElements.stream().anyMatch(fieldElement -> fieldElement.getDefaultValue() != null);
            if (hasDefaultValue) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Default values are not supported!");
            }
        }
    }

    /**
     * 功能：校验`Enum Elements`。
     * 参数：
     * - `schemaName`：名称。
     * - `enumTypes`：类型。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：无。
     */
    private static void checkEnumElements(String schemaName, List<EnumElement> enumTypes, String exceptionPrefix) {
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getNestedTypes().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Nested types in Enum definitions are not supported!");
        }
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getOptions().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Enum definitions options are not supported!");
        }
    }

    /**
     * 功能：校验消息。
     * 参数：
     * - `schemaName`：名称。
     * - `messageElementsList`：待处理消息。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：无。
     */
    private static void checkMessageElements(String schemaName, List<MessageElement> messageElementsList, String exceptionPrefix) {
        if (!messageElementsList.isEmpty()) {
            messageElementsList.forEach(messageElement -> {
                checkProtoFileCommonSettings(schemaName, messageElement.getGroups().isEmpty(),
                        " Message definition groups don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getOptions().isEmpty(),
                        " Message definition options don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getExtensions().isEmpty(),
                        " Message definition extensions don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getReserveds().isEmpty(),
                        " Message definition reserved elements don't support!", exceptionPrefix);
                checkFieldElements(schemaName, messageElement.getFields(), exceptionPrefix);
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    oneOfs.forEach(oneOfElement -> {
                        checkProtoFileCommonSettings(schemaName, oneOfElement.getGroups().isEmpty(),
                                " OneOf definition groups don't support!", exceptionPrefix);
                        checkFieldElements(schemaName, oneOfElement.getFields(), exceptionPrefix);
                    });
                }
                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        checkEnumElements(schemaName, nestedEnumTypes, exceptionPrefix);
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    checkMessageElements(schemaName, nestedMessageTypes, exceptionPrefix);
                }
            });
        }
    }

    /**
     * 功能：执行 `invalidSchemaProvidedMessage` 对应的处理。
     * 参数：
     * - `schemaName`：名称。
     * - `exceptionPrefix`：`exceptionPrefix` 参数。
     * 返回：文本结果。
     */
    public static String invalidSchemaProvidedMessage(String schemaName, String exceptionPrefix) {
        return exceptionPrefix + " invalid " + schemaName + " provided!";
    }

}
