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

import java.time.chrono.IsoChronology;

/**
 * 中文说明：
 * 1. `TbDateTestEntity` 是 ThingsBoard Common 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 它直接协作于领域模型、实体映射器和存取实现。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
public class TbDateTestEntity {
    /**
     * `year` 字段，保存当前对象的对应属性。
     */
    private int year;
    private int month;
    /**
     * `date` 字段，保存当前对象的对应属性。
     */
    private int date;
    private int hours;
    /**
     * 功能：创建 `TbDateTestEntity` 实例，并初始化必要字段。
     * 参数：
     * - `year`：`year` 参数。
     * - `month`：`month` 参数。
     * - `date`：`date` 参数。
     * - `hours`：`hours` 参数。
     * 返回：新创建的对象实例。
     */
    public TbDateTestEntity(int year, int month, int date, int hours) {
        this.year = year;
        this.month = month;
        this.date = date;
        this.hours = hours;
        if (hours > 23) {
            if (date == 31) {
                this.year++;
                this.month = 1;
                this.date = 1;
            } else {
                this.date++;
            }
            this.hours = hours - 24;
        } else if (hours < 0) {
            if (month== 1 && date == 1) {
                this.year--;
                this.month = 12;
                this.date = 31;
            } else {
                this.date--;
            }
            this.hours = hours + 24;
        }

        if (this.date > 28) {
            int dom = 31;
            switch (month) {
                case 2:
                    dom = IsoChronology.INSTANCE.isLeapYear((long) year) ? 29 : 28;
                case 3:
                case 5:
                case 7:
                case 8:
                case 10:
                default:
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    dom = 30;
            }
            if (this.date > dom) {
                this.date = this.date - dom;
                this.month++;
            }
        }
    }
    /**
     * 功能：获取`Year`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getYear(){
        return year < 70 ? 2000 + year : year <= 99 ? 1900 + year : year;
    }

    /**
     * 功能：执行 `geMonthStr` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String geMonthStr(){
        return String.format("%02d", month);
    }

    /**
     * 功能：执行 `geDateStr` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String geDateStr(){
        return String.format("%02d", date);
    }

    /**
     * 功能：执行 `geHoursStr` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String geHoursStr(){
        return String.format("%02d", hours);
    }
}
