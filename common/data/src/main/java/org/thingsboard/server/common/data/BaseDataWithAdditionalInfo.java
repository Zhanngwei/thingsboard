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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by ashvayka on 19.02.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseDataWithAdditionalInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Slf4j
public abstract class BaseDataWithAdditionalInfo<I extends UUIDBased> extends BaseData<I> implements HasAdditionalInfo {

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @NoXss
    private transient JsonNode additionalInfo;
    /**
     * 扩展信息列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] additionalInfoBytes;

    /**
     * 功能：创建 `BaseDataWithAdditionalInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public BaseDataWithAdditionalInfo() {
        super();
    }

    /**
     * 功能：创建 `BaseDataWithAdditionalInfo` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public BaseDataWithAdditionalInfo(I id) {
        super(id);
    }

    /**
     * 功能：创建 `BaseDataWithAdditionalInfo` 实例，并初始化必要字段。
     * 参数：
     * - `baseData`：待处理数据。
     * 返回：新创建的对象实例。
     */
    public BaseDataWithAdditionalInfo(BaseDataWithAdditionalInfo<I> baseData) {
        super(baseData);
        setAdditionalInfo(baseData.getAdditionalInfo());
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public JsonNode getAdditionalInfo() {
        return getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    /**
     * 功能：更新扩展信息。
     * 参数：
     * - `addInfo`：`addInfo` 参数。
     * 返回：无。
     */
    public void setAdditionalInfo(JsonNode addInfo) {
        setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BaseDataWithAdditionalInfo<?> that = (BaseDataWithAdditionalInfo<?>) o;
        return Arrays.equals(additionalInfoBytes, that.additionalInfoBytes);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), additionalInfoBytes);
    }

    /**
     * 功能：获取JSON。
     * 参数：
     * - `jsonData`：待处理数据。
     * - `binaryData`：待处理数据。
     * 返回：处理结果。
     */
    public static JsonNode getJson(Supplier<JsonNode> jsonData, Supplier<byte[]> binaryData) {
        JsonNode json = jsonData.get();
        if (json != null) {
            return json;
        } else {
            byte[] data = binaryData.get();
            if (data != null) {
                try {
                    return mapper.readTree(new ByteArrayInputStream(data));
                } catch (IOException e) {
                    log.warn("Can't deserialize json data: ", e);
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * 功能：更新JSON。
     * 参数：
     * - `json`：`json` 参数。
     * - `jsonConsumer`：`jsonConsumer` 参数。
     * - `bytesConsumer`：`bytesConsumer` 参数。
     * 返回：无。
     */
    public static void setJson(JsonNode json, Consumer<JsonNode> jsonConsumer, Consumer<byte[]> bytesConsumer) {
        jsonConsumer.accept(json);
        try {
            bytesConsumer.accept(mapper.writeValueAsBytes(json));
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize json data: ", e);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseDataWithAdditionalInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
