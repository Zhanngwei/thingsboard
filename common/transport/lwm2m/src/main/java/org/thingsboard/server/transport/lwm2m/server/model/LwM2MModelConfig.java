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
package org.thingsboard.server.transport.lwm2m.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.util.CollectionsUtil.diffSets;

/**
 * 中文说明：
 * 1. `LwM2MModelConfig` 是 ThingsBoard Common Transport 中描述 LwM2M 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@NoArgsConstructor
@ToString(exclude = "toCancelRead")
@Slf4j
public class LwM2MModelConfig {
    /**
     * `endpoint` 字段，保存当前对象的对应属性。
     */
    private String endpoint;
    private Map<String, ObjectAttributes> attributesToAdd;
    /**
     * `attributesToRemove`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<String> attributesToRemove;
    private Set<String> toObserve;
    /**
     * `toCancelObserve`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<String> toCancelObserve;
    private Set<String> toRead;
    /**
     * `toCancelRead`集合，用于去重保存或快速判断对象是否存在。
     */
    @JsonIgnore
    private Set<String> toCancelRead;

    /**
     * 功能：创建 `LwM2MModelConfig` 实例，并初始化必要字段。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2MModelConfig(String endpoint) {
        this.endpoint = endpoint;
        this.attributesToAdd = new ConcurrentHashMap<>();
        this.attributesToRemove = ConcurrentHashMap.newKeySet();
        this.toObserve = ConcurrentHashMap.newKeySet();
        this.toCancelObserve = ConcurrentHashMap.newKeySet();
        this.toRead = ConcurrentHashMap.newKeySet();
        this.toCancelRead = new HashSet<>();
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `modelConfig`：配置对象。
     * 返回：无。
     */
    public void merge(LwM2MModelConfig modelConfig) {
        if (modelConfig.isEmpty() && modelConfig.getToCancelRead().isEmpty()) {
            return;
        }

        modelConfig.getAttributesToAdd().forEach((k, v) -> {
            if (this.attributesToRemove.contains(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToAdd.put(k, v);
            }
        });

        modelConfig.getAttributesToRemove().forEach(k -> {
            if (this.attributesToAdd.containsKey(k)) {
                this.attributesToRemove.remove(k);
            } else {
                this.attributesToRemove.add(k);
            }
        });

        this.toObserve.addAll(diffSets(this.toCancelObserve, modelConfig.getToObserve()));
        this.toCancelObserve.addAll(diffSets(this.toObserve, modelConfig.getToCancelObserve()));

        this.toObserve.removeAll(modelConfig.getToCancelObserve());
        this.toCancelObserve.removeAll(modelConfig.getToObserve());

        this.toRead.removeAll(modelConfig.getToObserve());
        this.toRead.removeAll(modelConfig.getToCancelRead());
        this.toRead.addAll(modelConfig.getToRead());
    }

    /**
     * 功能：判断`Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isEmpty() {
        return attributesToAdd.isEmpty() && toObserve.isEmpty() && toCancelObserve.isEmpty() && toRead.isEmpty();
    }
}
