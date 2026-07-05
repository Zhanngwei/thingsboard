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
 * `GpsGeofencingEvents` 类，封装当前模块中的一组相关职责。
 */
public class GpsGeofencingEvents {
    /**
     * `ENTERED`常量，用于统一引用固定值。
     */
    public static final String ENTERED = "Entered";
    /**
     * `INSIDE`常量，用于统一引用固定值。
     */
    public static final String INSIDE = "Inside";
    /**
     * `LEFT`常量，用于统一引用固定值。
     */
    public static final String LEFT = "Left";
    /**
     * `OUTSIDE`常量，用于统一引用固定值。
     */
    public static final String OUTSIDE = "Outside";
}

/*
 * 本类总结：
 * 本类集中保存地理围栏事件字符串，属于无状态常量类；具体事件判断、缓存读取和规则链消息分发由调用方实现。
 */
