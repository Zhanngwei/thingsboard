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
package org.thingsboard.server.common.data.transport.snmp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.DataType;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `SnmpMapping` 是 ThingsBoard Common Data 中承载 SNMP 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SnmpMapping implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2042438869374145944L;

    /**
     * `oid`ID，用于定位对应业务对象。
     */
    private String oid;
    private String key;
    /**
     * 数据，用于区分不同处理分支。
     */
    private DataType dataType;

    private static final Pattern OID_PATTERN = Pattern.compile("^\\.?([0-2])((\\.0)|(\\.[1-9][0-9]*))*$");

    /**
     * 功能：判断`Valid`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotEmpty(oid) && OID_PATTERN.matcher(oid).matches() && StringUtils.isNotBlank(key);
    }
}
