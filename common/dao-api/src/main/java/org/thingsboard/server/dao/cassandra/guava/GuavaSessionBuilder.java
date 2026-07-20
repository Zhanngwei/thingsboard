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
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.api.core.session.SessionBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * 中文说明：
 * 1. `GuavaSessionBuilder` 是 ThingsBoard Common 中创建或提供会话对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `SessionBuilder`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public class GuavaSessionBuilder extends SessionBuilder<GuavaSessionBuilder, GuavaSession> {

    /**
     * 功能：构建上下文。
     * 参数：
     * - `configLoader`：配置对象。
     * - `programmaticArguments`：`programmaticArguments` 参数。
     * 返回：处理结果。
     */
    @Override
    protected DriverContext buildContext(DriverConfigLoader configLoader, ProgrammaticArguments programmaticArguments) {
        return new GuavaDriverContext(configLoader, programmaticArguments);
    }

    /**
     * 功能：执行 `wrap` 对应的处理。
     * 参数：
     * - `defaultSession`：会话对象。
     * 返回：处理结果。
     */
    @Override
    protected GuavaSession wrap(@NonNull CqlSession defaultSession) {
        return new DefaultGuavaSession(defaultSession);
    }
}
