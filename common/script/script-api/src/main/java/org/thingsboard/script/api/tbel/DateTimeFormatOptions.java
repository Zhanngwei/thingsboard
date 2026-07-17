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
package org.thingsboard.script.api.tbel;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;

import java.time.format.FormatStyle;
import java.util.TimeZone;

/**
 * 中文说明：
 * 1. `DateTimeFormatOptions` 是 ThingsBoard Common 中围绕 `Date Time Format` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@NoArgsConstructor
@Data
class DateTimeFormatOptions {
    private static final TimeZone DEFAULT_TZ = TimeZone.getDefault();

    /**
     * 时间，用于控制时间范围或等待时长。
     */
    private String timeZone;
    private String dateStyle;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    private String timeStyle;
    /**
     * `pattern` 字段，保存当前对象的对应属性。
     */
    @Getter
    private String pattern;

    /**
     * 功能：创建 `DateTimeFormatOptions` 实例，并初始化必要字段。
     * 参数：
     * - `timeZone`：`timeZone` 参数。
     * 返回：新创建的对象实例。
     */
    public DateTimeFormatOptions(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：处理结果。
     */
    TimeZone getTimeZone() {
        return StringUtils.isNotEmpty(timeZone) ? TimeZone.getTimeZone(timeZone) : TimeZone.getDefault();
    }

    /**
     * 功能：获取`Date Style`。
     * 参数：无。
     * 返回：处理结果。
     */
    FormatStyle getDateStyle() {
        return getFormatStyle(dateStyle, FormatStyle.SHORT);
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：处理结果。
     */
    FormatStyle getTimeStyle() {
        return getFormatStyle(timeStyle, FormatStyle.MEDIUM);
    }

    /**
     * 功能：获取`Format Style`。
     * 参数：
     * - `style`：`style` 参数。
     * - `defaultStyle`：`defaultStyle` 参数。
     * 返回：处理结果。
     */
    private static FormatStyle getFormatStyle(String style, FormatStyle defaultStyle) {
        if (StringUtils.isNotEmpty(style)) {
            try {
                return FormatStyle.valueOf(style.toUpperCase());
            } catch (IllegalArgumentException e) {
                return defaultStyle;
            }
        } else {
            return defaultStyle;
        }
    }

}
