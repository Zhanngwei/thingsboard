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
package org.thingsboard.server.common.data.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 中文说明：
 * 1. `BasePageDataIterable` 是 ThingsBoard Common Data 中承载 `Page Iterable` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Iterable`、`Iterator`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public abstract class BasePageDataIterable<T> implements Iterable<T>, Iterator<T> {

    /**
     * `fetchSize` 字段，保存当前对象的对应属性。
     */
    private final int fetchSize;

    /**
     * `currentItems`列表，用于保存一组待处理对象。
     */
    private List<T> currentItems;
    private int currentIdx;
    /**
     * 是否包含`next pack`。
     */
    private boolean hasNextPack;
    private PageLink nextPackLink;
    /**
     * 是否满足`initialized`条件。
     */
    private boolean initialized;

    /**
     * 功能：创建 `BasePageDataIterable` 实例，并初始化必要字段。
     * 参数：
     * - `fetchSize`：`fetchSize` 参数。
     * 返回：新创建的对象实例。
     */
    public BasePageDataIterable(int fetchSize) {
        super();
        this.fetchSize = fetchSize;
    }

    /**
     * 功能：执行 `iterator` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Iterator<T> iterator() {
        return this;
    }

    /**
     * 功能：判断`Next`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean hasNext() {
        if (!initialized) {
            fetch(new PageLink(fetchSize));
            initialized = true;
        }
        if (currentIdx == currentItems.size()) {
            if (hasNextPack) {
                fetch(nextPackLink);
            }
        }
        return currentIdx < currentItems.size();
    }

    /**
     * 功能：执行 `next` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return currentItems.get(currentIdx++);
    }

    /**
     * 功能：执行 `fetch` 对应的处理。
     * 参数：
     * - `link`：`link` 参数。
     * 返回：无。
     */
    private void fetch(PageLink link) {
        PageData<T> pageData = fetchPageData(link);
        currentIdx = 0;
        currentItems = pageData != null ? pageData.getData() : new ArrayList<>();
        hasNextPack = pageData != null && pageData.hasNext();
        nextPackLink = link.nextPageLink();
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `link`：`link` 参数。
     * 返回：匹配的数据集合。
     */
    abstract PageData<T> fetchPageData(PageLink link);
}
