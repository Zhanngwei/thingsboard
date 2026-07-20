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
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `IdBased` 是 ThingsBoard Common Data 中承载 `Id Based` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `UUIDBased`、`HasId`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public abstract class IdBased<I extends UUIDBased> implements HasId<I> {
	
	/**
	 * `id`ID，用于定位对应业务对象。
	 */
	protected I id;
	
	/**
	 * 功能：创建 `IdBased` 实例，并初始化必要字段。
	 * 参数：无。
	 * 返回：新创建的对象实例。
	 */
	public IdBased() {
		super();
	}
	
	/**
	 * 功能：创建 `IdBased` 实例，并初始化必要字段。
	 * 参数：
	 * - `id`：`id`ID。
	 * 返回：新创建的对象实例。
	 */
	public IdBased(I id) {
		super();
		this.id = id;
	}

	/**
	 * 功能：更新`Id`。
	 * 参数：
	 * - `id`：`id`ID。
	 * 返回：无。
	 */
    @JsonSetter
	public void setId(I id) {
		this.id = id;
	}

	/**
	 * 功能：获取`Id`。
	 * 参数：无。
	 * 返回：处理结果。
	 */
	public I getId() {
		return id;
	}

	/**
	 * 功能：获取`Uuid Id`。
	 * 参数：无。
	 * 返回：处理结果。
	 */
	@JsonIgnore
	public UUID getUuidId() {
		if (id != null) {
			return id.getId();
		}
		return null;
	}

	/**
	 * 功能：计算当前对象的哈希值。
	 * 参数：无。
	 * 返回：数值结果。
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * 功能：比较当前对象与传入对象是否等价。
	 * 参数：
	 * - `obj`：`obj` 参数。
	 * 返回：判断结果。
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdBased other = (IdBased) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
}
