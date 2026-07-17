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
package org.thingsboard.server.common.data.kv;

import lombok.Data;

/**
 * 中文说明：
 * 1. `BaseTsKvQuery` 是 ThingsBoard Common Data 中承载 `Ts Kv Query` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TsKvQuery`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class BaseTsKvQuery implements TsKvQuery {

    private static final ThreadLocal<Integer> idSeq = ThreadLocal.withInitial(() -> 0);

    /**
     * `id`ID，用于定位对应业务对象。
     */
    private final int id;
    private final String key;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long startTs;
    private final long endTs;

    /**
     * 功能：创建 `BaseTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    public BaseTsKvQuery(String key, long startTs, long endTs) {
        this(idSeq.get(), key, startTs, endTs);
        idSeq.set(id + 1);
    }

    /**
     * 功能：创建 `BaseTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    protected BaseTsKvQuery(int id, String key, long startTs, long endTs) {
        this.id = id;
        this.key = key;
        this.startTs = startTs;
        this.endTs = endTs;
    }

}
