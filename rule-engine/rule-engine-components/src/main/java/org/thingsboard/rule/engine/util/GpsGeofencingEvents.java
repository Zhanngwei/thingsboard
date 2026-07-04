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
package org.thingsboard.rule.engine.util;

/**
 * GPS 地理围栏事件名称常量集合。
 * 本类仅定义字符串常量，不直接访问数据库、缓存、MQTT、Actor 或 Rule Engine 消息流。
 */
public class GpsGeofencingEvents {
    /**
     * 进入围栏事件。
     */
    public static final String ENTERED = "Entered";
    /**
     * 处于围栏内部事件。
     */
    public static final String INSIDE = "Inside";
    /**
     * 离开围栏事件。
     */
    public static final String LEFT = "Left";
    /**
     * 处于围栏外部事件。
     */
    public static final String OUTSIDE = "Outside";
}

/*
 * 本类总结：
 * 本类集中保存地理围栏事件字符串，属于无状态常量类；具体事件判断、缓存读取和规则链消息分发由调用方实现。
 */
