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

import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`ContactBased` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@EqualsAndHashCode(callSuper = true)
public abstract class ContactBased<I extends UUIDBased> extends BaseDataWithAdditionalInfo<I> implements HasEmail {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 5047448057830660988L;

    /**
     * `country` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "country")
    @NoXss
    protected String country;
    /**
     * 状态，表示当前对象所处状态。
     */
    @Length(fieldName = "state")
    @NoXss
    protected String state;
    /**
     * `city` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "city")
    @NoXss
    protected String city;
    /**
     * `address` 字段，保存当前对象的对应属性。
     */
    @NoXss
    protected String address;
    /**
     * `address2` 字段，保存当前对象的对应属性。
     */
    @NoXss
    protected String address2;
    /**
     * `zip` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "zip or postal code")
    @NoXss
    protected String zip;
    /**
     * `phone` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "phone")
    @NoXss
    protected String phone;
    /**
     * 邮箱，用于展示或标识当前对象。
     */
    @Length(fieldName = "email")
    @NoXss
    protected String email;

    /**
     * 功能：创建 `ContactBased` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ContactBased() {
        super();
    }

    /**
     * 功能：创建 `ContactBased` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public ContactBased(I id) {
        super(id);
    }

    /**
     * 功能：创建 `ContactBased` 实例，并初始化必要字段。
     * 参数：
     * - `contact`：`contact` 参数。
     * 返回：新创建的对象实例。
     */
    public ContactBased(ContactBased<I> contact) {
        super(contact);
        this.country = contact.getCountry();
        this.state = contact.getState();
        this.city = contact.getCity();
        this.address = contact.getAddress();
        this.address2 = contact.getAddress2();
        this.zip = contact.getZip();
        this.phone = contact.getPhone();
        this.email = contact.getEmail();
    }

    /**
     * 功能：获取`Country`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCountry() {
        return country;
    }

    /**
     * 功能：更新`Country`。
     * 参数：
     * - `country`：`country` 参数。
     * 返回：无。
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getState() {
        return state;
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 功能：获取`City`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getCity() {
        return city;
    }

    /**
     * 功能：更新`City`。
     * 参数：
     * - `city`：`city` 参数。
     * 返回：无。
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 功能：获取`Address`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getAddress() {
        return address;
    }

    /**
     * 功能：更新`Address`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 功能：获取`Address2`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getAddress2() {
        return address2;
    }

    /**
     * 功能：更新`Address2`。
     * 参数：
     * - `address2`：`address2` 参数。
     * 返回：无。
     */
    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    /**
     * 功能：获取`Zip`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getZip() {
        return zip;
    }

    /**
     * 功能：更新`Zip`。
     * 参数：
     * - `zip`：`zip` 参数。
     * 返回：无。
     */
    public void setZip(String zip) {
        this.zip = zip;
    }

    /**
     * 功能：获取`Phone`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 功能：更新`Phone`。
     * 参数：
     * - `phone`：`phone` 参数。
     * 返回：无。
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 功能：获取邮箱。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 功能：更新邮箱。
     * 参数：
     * - `email`：`email` 参数。
     * 返回：无。
     */
    public void setEmail(String email) {
        this.email = email;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ContactBased` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
