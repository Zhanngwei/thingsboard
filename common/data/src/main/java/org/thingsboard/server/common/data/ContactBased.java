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
 * 1. `ContactBased` 是 ThingsBoard Common Data 中承载 `Contact Based` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `UUIDBased`、`HasEmail`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
