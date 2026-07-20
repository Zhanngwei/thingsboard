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
package org.thingsboard.server.service.edge.rpc.constructor.user;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `UserMsgConstructorV1` 是 ThingsBoard Application 中创建或提供用户对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseUserMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class UserMsgConstructorV1 extends BaseUserMsgConstructor {

    /**
     * 功能：执行 `constructUserUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserUpdateMsg constructUserUpdatedMsg(UpdateMsgType msgType, User user) {
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(user.getId().getId().getMostSignificantBits())
                .setIdLSB(user.getId().getId().getLeastSignificantBits())
                .setEmail(user.getEmail())
                .setAuthority(user.getAuthority().name());
        if (user.getCustomerId() != null) {
            builder.setCustomerIdMSB(user.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(user.getCustomerId().getId().getLeastSignificantBits());
        }
        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }
        if (user.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(user.getAdditionalInfo()));
        }
        return builder.build();
    }

    /**
     * 功能：执行 `constructUserCredentialsUpdatedMsg` 对应的处理。
     * 参数：
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentialsUpdateMsg constructUserCredentialsUpdatedMsg(UserCredentials userCredentials) {
        return UserCredentialsUpdateMsg.newBuilder()
                .setUserIdMSB(userCredentials.getUserId().getId().getMostSignificantBits())
                .setUserIdLSB(userCredentials.getUserId().getId().getLeastSignificantBits())
                .setEnabled(userCredentials.isEnabled())
                .setPassword(userCredentials.getPassword()).build();
    }
}
