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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.model.mfa.account.BackupCodeTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.BackupCodeTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. `BackupCodeTwoFaProvider` 是 ThingsBoard Application 中创建或提供 `Backup Code Two` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TwoFaProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Service
@TbCoreComponent
public class BackupCodeTwoFaProvider implements TwoFaProvider<BackupCodeTwoFaProviderConfig, BackupCodeTwoFaAccountConfig> {

    /**
     * 配置，负责处理对应任务或消息。
     */
    @Autowired @Lazy
    private TwoFaConfigManager twoFaConfigManager;

    /**
     * 功能：执行 `generateNewAccountConfig` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerConfig`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public BackupCodeTwoFaAccountConfig generateNewAccountConfig(User user, BackupCodeTwoFaProviderConfig providerConfig) {
        BackupCodeTwoFaAccountConfig config = new BackupCodeTwoFaAccountConfig();
        config.setCodes(generateCodes(providerConfig.getCodesQuantity(), 8));
        config.setSerializeHiddenFields(true);
        return config;
    }

    /**
     * 功能：执行 `generateCodes` 对应的处理。
     * 参数：
     * - `count`：`count` 参数。
     * - `length`：`length` 参数。
     * 返回：匹配的数据集合。
     */
    private static Set<String> generateCodes(int count, int length) {
        return Stream.generate(() -> StringUtils.random(length, "0123456789abcdef"))
                .distinct().limit(count)
                .collect(Collectors.toSet());
    }

    /**
     * 功能：校验编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `code`：`code` 参数。
     * - `providerConfig`：配置对象。
     * - `accountConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean checkVerificationCode(SecurityUser user, String code, BackupCodeTwoFaProviderConfig providerConfig, BackupCodeTwoFaAccountConfig accountConfig) {
        if (CollectionsUtil.contains(accountConfig.getCodes(), code)) {
            accountConfig.getCodes().remove(code);
            twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.BACKUP_CODE;
    }

}
