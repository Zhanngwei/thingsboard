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
package org.thingsboard.common.util;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;

import javax.script.ScriptException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 中文说明：
 * 1. `ExceptionUtil` 是 ThingsBoard Common 中处理 `Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class ExceptionUtil {

    /**
     * 功能：执行 `lookupException` 对应的处理。
     * 参数：
     * - `source`：`source` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    public static <T extends Exception> T lookupException(Throwable source, Class<T> clazz) {
        Exception e = lookupExceptionInCause(source, clazz);
        if (e != null) {
            return (T) e;
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `lookupExceptionInCause` 对应的处理。
     * 参数：
     * - `source`：`source` 参数。
     * - `clazzes`：`clazzes` 参数。
     * 返回：处理结果。
     */
    public static Exception lookupExceptionInCause(Throwable source, Class<? extends Exception>... clazzes) {
        while (source != null) {
            for (Class<? extends Exception> clazz : clazzes) {
                if (clazz.isAssignableFrom(source.getClass())) {
                    return (Exception) source;
                }
            }
            source = source.getCause();
        }
        return null;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：
     * - `e`：`e` 参数。
     * - `componentId`：`componentId`ID。
     * - `stackTraceEnabled`：`stackTraceEnabled` 参数。
     * 返回：文本结果。
     */
    public static String toString(Exception e, EntityId componentId, boolean stackTraceEnabled) {
        Exception exception = lookupExceptionInCause(e, ScriptException.class, JsonParseException.class);
        if (exception != null && StringUtils.isNotEmpty(exception.getMessage())) {
            return exception.getMessage();
        } else {
            if (stackTraceEnabled) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                return sw.toString();
            } else {
                log.debug("[{}] Unknown error during message processing", componentId, e);
                return "Please contact system administrator";
            }
        }
    }
}
