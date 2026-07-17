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
 * 1. `BaseDataWithAdditionalInfo` 是 ThingsBoard Common Data 中承载 `With Additional` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `UUIDBased`、`HasAdditionalInfo`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
