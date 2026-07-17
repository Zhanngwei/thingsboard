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

import com.fasterxml.jackson.annotation.JsonValue;
import org.mvel2.ConversionException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * 中文说明：
 * 1. `TbDate` 是 ThingsBoard Common 中围绕 `Tb Date` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `Serializable`、`Cloneable`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbDate implements Serializable, Cloneable {

    /**
     * `instant` 字段，保存当前对象的对应属性。
     */
    private Instant instant;

    private static final ZoneId zoneIdUTC = ZoneId.of("UTC");
    private static final Locale localeUTC = Locale.forLanguageTag("UTC");

    private static final DateTimeFormatter isoDateFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd[[ ]['T']HH:mm[:ss[.SSS]][ ][XXX][Z][z][VV][O]]").withZone(ZoneId.systemDefault());

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbDate() {
        this.instant = Instant.now();
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDate(String s) {
        this.instant = parseInstant(s);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `s`：`s` 参数。
     * - `pattern`：`pattern` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDate(String s, String pattern) {
        this.instant = parseInstant(s, Locale.getDefault().toLanguageTag(), pattern);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `s`：`s` 参数。
     * - `pattern`：`pattern` 参数。
     * - `locale`：`locale` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDate(String s, String pattern, String locale) {
        this.instant = parseInstant(s, locale, pattern);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `s`：`s` 参数。
     * - `pattern`：`pattern` 参数。
     * - `locale`：`locale` 参数。
     * - `zoneId`：`zoneId`ID。
     * 返回：新创建的对象实例。
     */
    public TbDate(String s, String pattern, String locale, String zoneId) {
       this.instant = parseInstant(s, pattern, locale, zoneId);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `dateMilliSecond`：`dateMilliSecond` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDate(long dateMilliSecond) {
        instant = Instant.ofEpochMilli(dateMilliSecond);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date) {
        this(year, month, date, 0, 0, 0, 0, null);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `tz`：`tz` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, String tz) {
        this(year, month, date, 0, 0, 0, 0, tz);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0, 0, null);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, int hrs, int min, String tz) {
        this(year, month, date, hrs, min, 0, 0, tz);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, int hrs, int min, int second) {
        this(year, month, date, hrs, min, second, 0, null);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, int hrs, int min, int second, String tz) {
        this(year, month, date, hrs, min, second, 0, tz);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, int hrs, int min, int second, int milliSecond) {
        this(year, month, date, hrs, min, second, milliSecond, null);
    }

    /**
     * 功能：创建 `TbDate` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbDate(int year, int month, int date, int hrs, int min, int second, int milliSecond, String tz) {
        ZoneId zoneId = tz != null && tz.length() > 0 ? ZoneId.of(tz) : ZoneId.systemDefault();
        instant = parseInstant(year, month, date, hrs, min, second,  milliSecond, zoneId);
    }

    /**
     * 功能：获取`Instant`。
     * 参数：无。
     * 返回：处理结果。
     */
    public Instant getInstant() {
        return instant;
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：处理结果。
     */
    public ZonedDateTime getZonedDateTime() {
        return getZonedDateTime(zoneIdUTC);
    }
    /**
     * 功能：获取时间。
     * 参数：
     * - `zoneId`：`zoneId`ID。
     * 返回：处理结果。
     */
    public ZonedDateTime getZonedDateTime(ZoneId zoneId) {
        return instant.atZone(zoneId);
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：处理结果。
     */
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(this.instant,  ZoneId.systemDefault());
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：处理结果。
     */
    public LocalDateTime getUTCDateTime() {
        return LocalDateTime.ofInstant(this.instant,  zoneIdUTC);
    }

    /**
     * 功能：执行 `toDateString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toDateString() {
        return toDateString(localeUTC.getLanguage());
    }
    /**
     * 功能：执行 `toDateString` 对应的处理。
     * 参数：
     * - `locale`：`locale` 参数。
     * 返回：文本结果。
     */
    public String toDateString(String locale) {
        return toDateString(locale, ZoneId.systemDefault().toString());
    }
    /**
     * 功能：执行 `toDateString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：文本结果。
     */
    public String toDateString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
    }
    /**
     * 功能：执行 `toTimeString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toTimeString() {
        return toTimeString(Locale.getDefault().getLanguage());
    }
    /**
     * 功能：执行 `toTimeString` 对应的处理。
     * 参数：
     * - `locale`：`locale` 参数。
     * 返回：文本结果。
     */
    public String toTimeString(String locale) {
        return toTimeString(locale, ZoneId.systemDefault().toString());
    }
    /**
     * 功能：执行 `toTimeString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：文本结果。
     */
    public String toTimeString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(locale));
    }

    /**
     * 功能：执行 `toISOString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toISOString() {
        return instant.toString();
    }
    /**
     * 功能：执行 `toJSON` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toJSON() {
        return toISOString();
    }
    /**
     * 功能：执行 `toUTCString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toUTCString() {
        return toUTCString(localeUTC.getLanguage());
    }

    /**
     * 功能：执行 `toUTCString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * 返回：文本结果。
     */
    public String toUTCString(String localeStr) {
        return toLocaleString(localeStr, zoneIdUTC.getId(), (locale, options) -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM).withLocale(locale));
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonValue
    public String toString() {
        return toString(Locale.getDefault().getLanguage());
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：
     * - `locale`：`locale` 参数。
     * 返回：文本结果。
     */
    public String toString(String locale) {
        return toString(locale, ZoneId.systemDefault().toString());
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：文本结果。
     */
    public String toString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL).withLocale(locale));
    }

    /**
     * 功能：执行 `toLocaleDateString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toLocaleDateString() {
        return toLocaleDateString(localeUTC.getLanguage());
    }

    /**
     * 功能：执行 `toLocaleDateString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * 返回：文本结果。
     */
    public String toLocaleDateString(String localeStr) {
        return toLocaleString(localeStr, ZoneId.systemDefault().toString(), (locale, options) -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
    }

    /**
     * 功能：执行 `toLocaleDateString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：文本结果。
     */
    public String toLocaleDateString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDate(options.getDateStyle()).withLocale(locale));
    }

    /**
     * 功能：执行 `toLocaleTimeString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toLocaleTimeString() {
        return toLocaleTimeString(Locale.getDefault().getLanguage());
    }

    /**
     * 功能：执行 `toLocaleTimeString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * 返回：文本结果。
     */
    public String toLocaleTimeString(String localeStr) {
        return toLocaleTimeString(localeStr, ZoneId.systemDefault().toString());
    }

    /**
     * 功能：执行 `toLocaleTimeString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：文本结果。
     */
    public String toLocaleTimeString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedTime(options.getTimeStyle()).withLocale(locale));
    }

    /**
     * 功能：执行 `toLocaleString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String toLocaleString() {
        return toLocaleString(localeUTC.getLanguage(), ZoneId.systemDefault().toString());
    }

    /**
     * 功能：执行 `toLocaleString` 对应的处理。
     * 参数：
     * - `locale`：`locale` 参数。
     * 返回：文本结果。
     */
    public String toLocaleString(String locale) {
        return toLocaleString(locale, ZoneId.systemDefault().toString());
    }

    /**
     * 功能：执行 `toLocaleString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：文本结果。
     */
    public String toLocaleString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) ->
                DateTimeFormatter.ofLocalizedDateTime(options.getDateStyle(), options.getTimeStyle()).withLocale(locale));
    }

    /**
     * 功能：执行 `toLocaleString` 对应的处理。
     * 参数：
     * - `localeStr`：`localeStr` 参数。
     * - `optionsStr`：`optionsStr` 参数。
     * - `formatterBuilder`：`formatterBuilder` 参数。
     * 返回：文本结果。
     */
    public String toLocaleString(String localeStr, String optionsStr, BiFunction<Locale, DateTimeFormatOptions, DateTimeFormatter> formatterBuilder) {
        Locale locale = StringUtils.isNotEmpty(localeStr) ? Locale.forLanguageTag(localeStr) : Locale.getDefault();
        DateTimeFormatOptions options = getDateFormattingOptions(optionsStr);
        ZonedDateTime zdt = this.getInstant().atZone(options.getTimeZone().toZoneId());
        DateTimeFormatter formatter;
        if (StringUtils.isNotEmpty(options.getPattern())) {
            formatter = new DateTimeFormatterBuilder().appendPattern(options.getPattern()).toFormatter(locale);
        } else {
            formatter = formatterBuilder.apply(locale, options);
        }
        return formatter.format(zdt);
    }

    /**
     * 功能：获取`Date Formatting Options`。
     * 参数：
     * - `optionsStr`：`optionsStr` 参数。
     * 返回：处理结果。
     */
    private DateTimeFormatOptions getDateFormattingOptions(String optionsStr) {
        DateTimeFormatOptions opt = null;
        if (StringUtils.isNotEmpty(optionsStr)) {
            try {
                opt = JacksonUtil.fromString(optionsStr, DateTimeFormatOptions.class);
            } catch (IllegalArgumentException iae) {
                opt = new DateTimeFormatOptions(optionsStr);
            }
        }
        if (opt == null) {
            opt = new DateTimeFormatOptions();
        }
        return opt;
    }

    /**
     * 功能：执行 `now` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public static long now() {
        return Instant.now().toEpochMilli();
    }
    /**
     * 功能：解析`Second`。
     * 参数：无。
     * 返回：数值结果。
     */
    public long parseSecond() {
        return instant.getEpochSecond();
    }

    /**
     * 功能：解析`Second Milli`。
     * 参数：无。
     * 返回：数值结果。
     */
    public long parseSecondMilli() {
        return instant.toEpochMilli();
    }

   /**
    * 功能：执行 `UTC` 对应的处理。
    * 参数：
    * - `year`：`year` 参数。
    * 返回：数值结果。
    */
   public static long UTC(int year) {
        return UTC(year, 0, 0, 0, 0, 0, 0);
    }
    /**
     * 功能：执行 `UTC` 对应的处理。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * 返回：数值结果。
     */
    public static long UTC(int year, int month) {
       return UTC(year, month, 0, 0, 0, 0, 0);
    }
    /**
     * 功能：执行 `UTC` 对应的处理。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * 返回：数值结果。
     */
    public static long UTC(int year, int month, int date) {
        return UTC(year, month, date, 0, 0, 0, 0);
    }
    /**
     * 功能：执行 `UTC` 对应的处理。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * 返回：数值结果。
     */
    public static long UTC(int year, int month, int date, int hrs) {
        return UTC(year, month, date, hrs, 0, 0, 0);
    }
    /**
     * 功能：执行 `UTC` 对应的处理。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：数值结果。
     */
    public static long UTC(int year, int month, int date, int hrs, int min) {
       return UTC(year, month, date, hrs, min, 0, 0);
    }
    /**
     * 功能：执行 `UTC` 对应的处理。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：数值结果。
     */
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        return UTC(year, month, date, hrs, min, sec, 0);
    }
    /**
     * 功能：执行 `UTC` 对应的处理。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：数值结果。
     */
    public static long UTC(int year, int month, int date, int hrs, int min, int sec, int ms) {
        year = year == 0 ? year = 1899 : year;
        month = month == 0 ? month = 12 : month;
        date = date == 0 ? date = 31 : date;
        return parseInstant(year, month, date, hrs, min, sec, ms, zoneIdUTC).toEpochMilli();
    }
    /**
     * 功能：获取`UTC Full Year`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCFullYear() {
        return getUTCDateTime().getYear();
    }

    /**
     * 功能：获取`UTC Month`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCMonth() {
        return getUTCDateTime().getMonthValue();
    }
    // day in month
    /**
     * 功能：获取`UTC Date`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCDate() {
        return getUTCDateTime().getDayOfMonth();
    }
    // day in week
    /**
     * 功能：获取`UTC Day`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCDay() {
       return getUTCDateTime().getDayOfWeek().getValue();
    }

    /**
     * 功能：获取`UTC Hours`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCHours() {
       return getUTCDateTime().getHour();
    }

    /**
     * 功能：获取`UTC Minutes`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCMinutes() {
       return getZonedDateTime().getMinute();
    }

    /**
     * 功能：获取`UTC Seconds`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCSeconds() {
       return getUTCDateTime().getSecond();
    }
    /**
     * 功能：获取`UTC Milliseconds`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getUTCMilliseconds() {
       return getUTCDateTime().getNano()/1000000;
    }

    /**
     * 功能：更新`UTC Full Year`。
     * 参数：
     * - `year`：`year` 参数。
     * 返回：无。
     */
    public void setUTCFullYear(int year) {
        if (getUTCDate() > 28) {
            long time = getZonedDateTime().withYear(year).withDayOfMonth(1).toInstant().toEpochMilli() + (getUTCDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getZonedDateTime().withYear(year).toInstant();
        }
    }
    /**
     * 功能：更新`UTC Full Year`。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * 返回：无。
     */
    public void setUTCFullYear(int year, int month) {
        if (getUTCDate() > 28) {
            long time = getZonedDateTime().withYear(year).withMonth(month).withDayOfMonth(1).toInstant().toEpochMilli() + (getUTCDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getZonedDateTime().withYear(year).withMonth(month).toInstant();
        }
    }
    /**
     * 功能：更新`UTC Full Year`。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * 返回：无。
     */
    public void setUTCFullYear(int year, int month, int date) {
        this.instant = getZonedDateTime().withYear(year).withMonth(month).withDayOfMonth(date).toInstant();
    }
    /**
     * 功能：更新`UTC Month`。
     * 参数：
     * - `month`：`month` 参数。
     * 返回：无。
     */
    public void setUTCMonth(int month) {
        if (getUTCDate() > 28) {
            long time = getZonedDateTime().withMonth(month).withDayOfMonth(1).toInstant().toEpochMilli() + (getUTCDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getZonedDateTime().withMonth(month).toInstant();
        }
    }
    /**
     * 功能：更新`UTC Month`。
     * 参数：
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * 返回：无。
     */
    public void setUTCMonth(int month, int date) {
        this.instant = getZonedDateTime().withMonth(month).withDayOfMonth(date).toInstant();
    }
    /**
     * 功能：更新`UTC Date`。
     * 参数：
     * - `date`：`date` 参数。
     * 返回：无。
     */
    public void setUTCDate(int date) {
        this.instant = getZonedDateTime().withDayOfMonth(date).toInstant();
    }
    /**
     * 功能：更新`UTC Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * 返回：无。
     */
    public void setUTCHours(int hrs) {
        this.instant = getZonedDateTime().withHour(hrs).toInstant();
    }
    /**
     * 功能：更新`UTC Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * - `minutes`：`minutes` 参数。
     * 返回：无。
     */
    public void setUTCHours(int hrs, int minutes) {
        this.instant = getZonedDateTime().withHour(hrs).withMinute(minutes).toInstant();
    }
    /**
     * 功能：更新`UTC Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * 返回：无。
     */
    public void setUTCHours(int hrs, int minutes, int seconds) {
        this.instant = getZonedDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).toInstant();
    }
    /**
     * 功能：更新`UTC Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setUTCHours(int hrs, int minutes, int seconds, int ms) {
        this.instant = getZonedDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).withNano(ms*1000000).toInstant();
    }
    /**
     * 功能：更新`UTC Minutes`。
     * 参数：
     * - `minutes`：`minutes` 参数。
     * 返回：无。
     */
    public void setUTCMinutes(int minutes) {
        this.instant = getZonedDateTime().withMinute(minutes).toInstant();
    }
    /**
     * 功能：更新`UTC Minutes`。
     * 参数：
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * 返回：无。
     */
    public void setUTCMinutes(int minutes, int seconds) {
        this.instant = getZonedDateTime().withMinute(minutes).withSecond(seconds).toInstant();    }
    /**
     * 功能：更新`UTC Minutes`。
     * 参数：
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setUTCMinutes(int minutes, int seconds, int ms) {
        this.instant = parseInstant(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), minutes, seconds, ms, zoneIdUTC);
    }
    /**
     * 功能：更新`UTC Seconds`。
     * 参数：
     * - `seconds`：`seconds` 参数。
     * 返回：无。
     */
    public void setUTCSeconds(int seconds) {
        this.instant = getZonedDateTime().withSecond(seconds).toInstant();
    }
    /**
     * 功能：更新`UTC Seconds`。
     * 参数：
     * - `seconds`：`seconds` 参数。
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setUTCSeconds(int seconds, int ms) {
        this.instant = getZonedDateTime().withSecond(seconds).withNano(ms*1000000).toInstant();
    }
    /**
     * 功能：更新`UTC Milliseconds`。
     * 参数：
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setUTCMilliseconds(int ms) {
        this.instant = getZonedDateTime().withNano(ms*1000000).toInstant();
    }
    /**
     * 功能：获取`Full Year`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getFullYear() {
        return getLocalDateTime().getYear();
    }

    /**
     * 功能：获取`Month`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getMonth() {
        return getLocalDateTime().getMonthValue();
    }
    // day in month
    /**
     * 功能：获取`Date`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getDate() {
        return getLocalDateTime().getDayOfMonth();
    }
    // day in week
    /**
     * 功能：获取`Day`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getDay() {
       return getLocalDateTime().getDayOfWeek().getValue();
    }

    /**
     * 功能：获取`Hours`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getHours() {
       return getLocalDateTime().getHour();
    }

    /**
     * 功能：获取`Minutes`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getMinutes() {
       return getLocalDateTime().getMinute();
    }

    /**
     * 功能：获取`Seconds`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getSeconds() {
       return getLocalDateTime().getSecond();
    }
    /**
     * 功能：获取`Milliseconds`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getMilliseconds() {
        return getLocalDateTime().getNano()/1000000;
    }
    // Milliseconds since Jan 1, 1970, 00:00:00.000 GMT
     /**
      * 功能：获取时间。
      * 参数：无。
      * 返回：数值结果。
      */
     public long getTime() {
        return instant.toEpochMilli();
    }
    /**
     * 功能：执行 `valueOf` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public long valueOf(){
        return getTime() ;
    }
    /**
     * 功能：更新`Full Year`。
     * 参数：
     * - `year`：`year` 参数。
     * 返回：无。
     */
    public void setFullYear(int year) {
        Instant instantEpochWithYear = getZonedDateTime().withYear(year).toInstant();
        if (getDate() > 28) {
            long time = getLocalDateTime().withYear(year).withDayOfMonth(1).toInstant(getLocaleZoneOffset(instantEpochWithYear)).toEpochMilli() + (getDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getLocalDateTime().withYear(year).toInstant(getLocaleZoneOffset(instantEpochWithYear));
        }
    }
    /**
     * 功能：更新`Full Year`。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * 返回：无。
     */
    public void setFullYear(int year, int month) {
        Instant instantEpochWithYear = getZonedDateTime().withYear(year).withMonth(month).toInstant();
        if (getDate() > 28) {
            long time = getLocalDateTime().withYear(year).withMonth(month).withDayOfMonth(1).toInstant(getLocaleZoneOffset(instantEpochWithYear)).toEpochMilli() + (getDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getLocalDateTime().withYear(year).withMonth(month).toInstant(getLocaleZoneOffset(instantEpochWithYear));
        }
    }
    /**
     * 功能：更新`Full Year`。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * 返回：无。
     */
    public void setFullYear(int year, int month, int date) {
        Instant instantEpochWithYearMonthDate = getZonedDateTime().withYear(year).withMonth(month).withDayOfMonth(date).toInstant();
        this.instant = getLocalDateTime().withYear(year).withMonth(month).withDayOfMonth(date).toInstant(getLocaleZoneOffset(instantEpochWithYearMonthDate));
    }
    /**
     * 功能：更新`Month`。
     * 参数：
     * - `month`：`month` 参数。
     * 返回：无。
     */
    public void setMonth(int month) {
        Instant instantEpochWithYear = getZonedDateTime().withMonth(month).toInstant();
        if (getDate() > 28) {
            long time = getLocalDateTime().withMonth(month).withDayOfMonth(1).toInstant(getLocaleZoneOffset(instantEpochWithYear)).toEpochMilli() + (getDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getLocalDateTime().withMonth(month).toInstant(getLocaleZoneOffset(instantEpochWithYear));
        }

    }
    /**
     * 功能：更新`Month`。
     * 参数：
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * 返回：无。
     */
    public void setMonth(int month, int date) {
        Instant instantEpochWithMonthDate = getZonedDateTime().withMonth(month).withDayOfMonth(date).toInstant();
        this.instant = getLocalDateTime().withMonth(month).withDayOfMonth(date).toInstant(getLocaleZoneOffset(instantEpochWithMonthDate));
    }
    /**
     * 功能：更新`Date`。
     * 参数：
     * - `date`：`date` 参数。
     * 返回：无。
     */
    public void setDate(int date) {
        Instant instantEpochWithDate = getZonedDateTime().withDayOfMonth(date).toInstant();
        this.instant = getLocalDateTime().withDayOfMonth(date).toInstant(getLocaleZoneOffset(instantEpochWithDate));
    }
    /**
     * 功能：更新`Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * 返回：无。
     */
    public void setHours(int hrs) {
        this.instant = getLocalDateTime().withHour(hrs).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * - `minutes`：`minutes` 参数。
     * 返回：无。
     */
    public void setHours(int hrs, int minutes) {
        this.instant = getLocalDateTime().withHour(hrs).withMinute(minutes).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * 返回：无。
     */
    public void setHours(int hrs, int minutes, int seconds) {
        this.instant = getLocalDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Hours`。
     * 参数：
     * - `hrs`：`hrs` 参数。
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setHours(int hrs, int minutes, int seconds, int ms) {
        this.instant = getLocalDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Minutes`。
     * 参数：
     * - `minutes`：`minutes` 参数。
     * 返回：无。
     */
    public void setMinutes(int minutes) {
        this.instant = getLocalDateTime().withMinute(minutes).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Minutes`。
     * 参数：
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * 返回：无。
     */
    public void setMinutes(int minutes, int seconds) {
        this.instant = getLocalDateTime().withMinute(minutes).withSecond(seconds).toInstant(getLocaleZoneOffset(this.instant));

    }
    /**
     * 功能：更新`Minutes`。
     * 参数：
     * - `minutes`：`minutes` 参数。
     * - `seconds`：`seconds` 参数。
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setMinutes(int minutes, int seconds, int ms) {
        this.instant = getLocalDateTime().withMinute(minutes).withSecond(seconds).withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Seconds`。
     * 参数：
     * - `seconds`：`seconds` 参数。
     * 返回：无。
     */
    public void setSeconds(int seconds) {
        this.instant = getLocalDateTime().withSecond(seconds).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Seconds`。
     * 参数：
     * - `seconds`：`seconds` 参数。
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setSeconds(int seconds, int ms) {
        this.instant = getLocalDateTime().withSecond(seconds).withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }
    /**
     * 功能：更新`Milliseconds`。
     * 参数：
     * - `ms`：`ms` 参数。
     * 返回：无。
     */
    public void setMilliseconds(int ms) {
        this.instant = getLocalDateTime().withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }

    // Milliseconds since Jan 1, 1970, 00:00:00.000 GMT
     /**
      * 功能：更新时间。
      * 参数：
      * - `dateMilliSecond`：`dateMilliSecond` 参数。
      * 返回：无。
      */
     public void setTime(long dateMilliSecond) {
         instant = Instant.ofEpochMilli(dateMilliSecond);
    }

    /**
     * 功能：获取偏移量。
     * 参数：
     * - `instants`：`instants` 参数。
     * 返回：匹配的数据集合。
     */
    public ZoneOffset getLocaleZoneOffset(Instant... instants){
        return ZoneId.systemDefault().getRules().getOffset(instants.length > 0 ? instants[0] : this.instant);
    }

    /**
     * 功能：执行 `parse` 对应的处理。
     * 参数：
     * - `value`：值。
     * - `format`：`format` 参数。
     * 返回：数值结果。
     */
    public static long parse(String value, String format) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(value).getTime();
        } catch (Exception e) {
            return -1;
        }
    }
    /**
     * 功能：执行 `parse` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：数值结果。
     */
    public static long parse(String value) {
        try {
            TemporalAccessor accessor = isoDateFormatter.parseBest(value,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from);
            Instant instant = Instant.from(accessor);
            return Instant.EPOCH.until(instant, ChronoUnit.MILLIS);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 功能：解析`Instant`。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：处理结果。
     */
    private static Instant parseInstant(String s) {
        boolean isIsoFormat = s.length() > 0 && Character.isDigit(s.charAt(0));
        if (isIsoFormat) {
            return getInstant_ISO_OFFSET_DATE_TIME(s);
        } else {
            return getInstant_RFC_1123(s);
        }
    }

    /**
     * 功能：解析`Instant`。
     * 参数：
     * - `s`：`s` 参数。
     * - `localeStr`：`localeStr` 参数。
     * - `pattern`：`pattern` 参数。
     * 返回：处理结果。
     */
    private static Instant parseInstant(String s, String localeStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(localeStr));
            return Instant.from(formatter.parse(s));
        } catch (Exception ex) {
            try {
                return parseInstant(s, pattern, localeStr, ZoneId.systemDefault().getId());
            } catch (final DateTimeParseException e) {
                final ConversionException exception = new ConversionException("Cannot parse value [" + s + "] as instant", ex);
                throw exception;
            }
        }
    }

    /**
     * 功能：解析`Instant`。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hrs`：`hrs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private static Instant parseInstant(int year, int month, int date, int hrs, int min, int second, int secondMilli, ZoneId zoneId) {
        year = year < 70 ? 2000 + year : year <= 99 ? 1900 + year : year;
        ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, date, hrs, min, second, secondMilli*1000000, zoneId);
        return zonedDateTime.toInstant();
    }
    /**
     * 功能：解析`Instant`。
     * 参数：
     * - `s`：`s` 参数。
     * - `pattern`：`pattern` 参数。
     * - `localeStr`：`localeStr` 参数。
     * - `zoneIdStr`：`zoneIdStr` 参数。
     * 返回：处理结果。
     */
    private static Instant parseInstant(String s, String pattern, String localeStr, String zoneIdStr) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(localeStr));
        LocalDateTime localDateTime = LocalDateTime.parse(s, dateTimeFormatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(zoneIdStr));
        return zonedDateTime.toInstant();
    }

    /**
     * 功能：获取时间。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：处理结果。
     */
    private static Instant getInstant_ISO_OFFSET_DATE_TIME(String s) {
        // assuming  "2007-12-03T10:15:30.00Z"  UTC instant
        // assuming  "2007-12-03T10:15:30.00"  ZoneId.systemDefault() instant
        // assuming  "2007-12-03T10:15:30.00-04:00"  TZ instant
        // assuming  "2007-12-03T10:15:30.00+04:00"  TZ instant
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        try {
            return Instant.from(formatter.parse(s));
        } catch (DateTimeParseException ex) {
            try {
                long timeMS = parse(s);
                if (timeMS != -1) {
                    return Instant.ofEpochMilli(timeMS);
                } else {
                    throw new ConversionException("Cannot parse value [" + s + "] as instant");
                }
            } catch (final DateTimeParseException e) {
                throw new ConversionException("Cannot parse value [" + s + "] as instant");
            }
        }
    }
    /**
     * 功能：获取`Instant RFC 1123`。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：处理结果。
     */
    private static Instant getInstant_RFC_1123(String s) {
            // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 GMT"
            // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 GMT-02:00"
            // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 -0200"
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        try {
            return Instant.from(formatter.parse(s));
        } catch (DateTimeParseException ex) {
            try {
                return getInstantWithLocalZoneOffsetId_RFC_1123(s);
            } catch (final DateTimeParseException e) {
                throw new ConversionException("Cannot parse value [" + s + "] as instant");
            }
        }
    }
    /**
     * 功能：获取偏移量。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    private static Instant getInstantWithLocalZoneOffsetId_RFC_1123(String value) {
        String s = value.trim() + " GMT";
        Instant instant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
        ZoneId systemZone = ZoneId.systemDefault(); // my timezone
        String id =  systemZone.getRules().getOffset(instant).getId();
        value =  value.trim() + " " + id.replaceAll(":", "");
        return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value));
    }
}
