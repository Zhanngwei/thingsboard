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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.UUIDBased;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `BaseData` 是 ThingsBoard Common Data 中承载 `Base Data` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `UUIDBased`、`Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public abstract class BaseData<I extends UUIDBased> extends IdBased<I> implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 5422817607129962637L;
    public static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * 创建时间，用于记录当前对象首次生成的时间。
     */
    protected long createdTime;
    
    /**
     * 功能：创建 `BaseData` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public BaseData() {
        super();
    }

    /**
     * 功能：创建 `BaseData` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public BaseData(I id) {
        super(id);
    }
    
    /**
     * 功能：创建 `BaseData` 实例，并初始化必要字段。
     * 参数：
     * - `data`：待处理数据。
     * 返回：新创建的对象实例。
     */
    public BaseData(BaseData<I> data) {
        super(data.getId());
        this.createdTime = data.getCreatedTime();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * 功能：更新创建时间。
     * 参数：
     * - `createdTime`：`createdTime` 参数。
     * 返回：无。
     */
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (createdTime ^ (createdTime >>> 32));
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
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        BaseData other = (BaseData) obj;
        return createdTime == other.createdTime;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BaseData [createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
