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
package org.thingsboard.monitoring.util;

import lombok.SneakyThrows;
import org.thingsboard.common.util.JacksonUtil;

import java.io.InputStream;

/**
 * 中文说明：
 * 1. `ResourceUtils` 是 ThingsBoard Monitoring 中处理资源通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class ResourceUtils {

    /**
     * 功能：获取`Resource`。
     * 参数：
     * - `path`：文件或资源路径。
     * - `type`：类型。
     * 返回：处理结果。
     */
    @SneakyThrows
    public static <T> T getResource(String path, Class<T> type) {
        InputStream resource = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found for path " + path);
        }
        return JacksonUtil.OBJECT_MAPPER.readValue(resource, type);
    }

    /**
     * 功能：获取`Resource As Stream`。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：处理结果。
     */
    public static InputStream getResourceAsStream(String path) {
        InputStream resource = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found for path " + path);
        }
        return resource;
    }

}
