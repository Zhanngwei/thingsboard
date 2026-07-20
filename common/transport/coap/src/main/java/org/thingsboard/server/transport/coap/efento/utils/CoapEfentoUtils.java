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
package org.thingsboard.server.transport.coap.efento.utils;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 中文说明：
 * 1. `CoapEfentoUtils` 是 ThingsBoard Common Transport 中处理 CoAP 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class CoapEfentoUtils {

    /**
     * 功能：转换`Byte Array To String`。
     * 参数：
     * - `a`：`a` 参数。
     * 返回：文本结果。
     */
    public static String convertByteArrayToString(byte[] a) {
        StringBuilder out = new StringBuilder();
        for (byte b : a) {
            out.append(String.format("%02X", b));
        }
        return out.toString();
    }

    /**
     * 功能：转换时间戳。
     * 参数：
     * - `timestampInMillis`：`timestampInMillis` 参数。
     * 返回：文本结果。
     */
    public static String convertTimestampToUtcString(long timestampInMillis) {
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String utcZone = "UTC";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(utcZone));
        return String.format("%s UTC", simpleDateFormat.format(new Date(timestampInMillis)));
    }

    /**
     * 功能：更新`Default Measurements`。
     * 参数：
     * - `serialNumber`：`serialNumber` 参数。
     * - `batteryStatus`：`batteryStatus` 参数。
     * - `measurementPeriod`：`measurementPeriod` 参数。
     * - `nextTransmissionAtMillis`：`nextTransmissionAtMillis` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static JsonObject setDefaultMeasurements(String serialNumber, boolean batteryStatus, long measurementPeriod, long nextTransmissionAtMillis, long signal, long startTimestampMillis) {
        JsonObject values = new JsonObject();
        values.addProperty("serial", serialNumber);
        values.addProperty("battery", batteryStatus ? "ok" : "low");
        values.addProperty("measured_at", convertTimestampToUtcString(startTimestampMillis));
        values.addProperty("next_transmission_at", convertTimestampToUtcString(nextTransmissionAtMillis));
        values.addProperty("signal", signal);
        values.addProperty("measurement_interval", measurementPeriod);
        return values;
    }

}
