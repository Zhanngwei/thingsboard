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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;

/**
 * 中文说明：
 * 1. `TwoFactorAuthConfigController` 是 ThingsBoard Application 中处理认证请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@RequestMapping("/api/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFactorAuthConfigController extends BaseController {

    /**
     * 配置，负责处理对应任务或消息。
     */
    private final TwoFaConfigManager twoFaConfigManager;
    private final TwoFactorAuthService twoFactorAuthService;


    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get account 2FA settings (getAccountTwoFaSettings)",
            notes = "Get user's account 2FA configuration. Configuration contains configs for different 2FA providers." + NEW_LINE +
                    "Example:\n" +
                    "```\n{\n  \"configs\": {\n" +
                    "    \"EMAIL\": {\n      \"providerType\": \"EMAIL\",\n      \"useByDefault\": true,\n      \"email\": \"tenant@thingsboard.org\"\n    },\n" +
                    "    \"TOTP\": {\n      \"providerType\": \"TOTP\",\n      \"useByDefault\": false,\n      \"authUrl\": \"otpauth://totp/TB%202FA:tenant@thingsboard.org?issuer=TB+2FA&secret=P6Z2TLYTASOGP6LCJZAD24ETT5DACNNX\"\n    },\n" +
                    "    \"SMS\": {\n      \"providerType\": \"SMS\",\n      \"useByDefault\": false,\n      \"phoneNumber\": \"+380501253652\"\n    }\n" +
                    "  }\n}\n```" +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @GetMapping("/account/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings getAccountTwoFaSettings() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFaConfigManager.getAccountTwoFaSettings(user.getTenantId(), user.getId()).orElse(null);
    }


    /**
     * 功能：执行 `generateTwoFaAccountConfig` 对应的处理。
     * 参数：
     * - `for`：`for` 参数。
     * - `TOTP`：`TOTP` 参数。
     * - `providerType`：类型。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Generate 2FA account config (generateTwoFaAccountConfig)",
            notes = "Generate new 2FA account config template for specified provider type. " + NEW_LINE +
                    "For TOTP, this will return a corresponding account config template " +
                    "with a generated OTP auth URL (with new random secret key for each API call) that can be then " +
                    "converted to a QR code to scan with an authenticator app. Example:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"TOTP\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"authUrl\": \"otpauth://totp/TB%202FA:tenant@thingsboard.org?issuer=TB+2FA&secret=PNJDNWJVAK4ZTUYT7RFGPQLXA7XGU7PX\"\n" +
                    "}\n```" + NEW_LINE +
                    "For EMAIL, the generated config will contain email from user's account:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"EMAIL\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"email\": \"tenant@thingsboard.org\"\n" +
                    "}\n```" + NEW_LINE +
                    "For SMS 2FA this method will just return a config with empty/default values as there is nothing to generate/preset:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"SMS\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"phoneNumber\": null\n" +
                    "}\n```" + NEW_LINE +
                    "Will throw an error (Bad Request) if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config/generate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public TwoFaAccountConfig generateTwoFaAccountConfig(@ApiParam(value = "2FA provider type to generate new account config for", defaultValue = "TOTP", required = true)
                                                         @RequestParam TwoFaProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        return twoFactorAuthService.generateNewAccountConfig(user, providerType);
    }

    /**
     * 功能：发送或提交配置。
     * 参数：
     * - `accountConfig`：配置对象。
     * 返回：无。
     */
    @ApiOperation(value = "Submit 2FA account config (submitTwoFaAccountConfig)",
            notes = "Submit 2FA account config to prepare for a future verification. " +
                    "Basically, this method will send a verification code for a given account config, if this has " +
                    "sense for a chosen 2FA provider. This code is needed to then verify and save the account config." + NEW_LINE +
                    "Example of EMAIL 2FA account config:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"EMAIL\",\n" +
                    "  \"useByDefault\": true,\n" +
                    "  \"email\": \"separate-email-for-2fa@thingsboard.org\"\n" +
                    "}\n```" + NEW_LINE +
                    "Example of SMS 2FA account config:\n" +
                    "```\n{\n" +
                    "  \"providerType\": \"SMS\",\n" +
                    "  \"useByDefault\": false,\n" +
                    "  \"phoneNumber\": \"+38012312321\"\n" +
                    "}\n```" + NEW_LINE +
                    "For TOTP this method does nothing." + NEW_LINE +
                    "Will throw an error (Bad Request) if submitted account config is not valid, " +
                    "or if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config/submit")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void submitTwoFaAccountConfig(@Valid @RequestBody TwoFaAccountConfig accountConfig) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, accountConfig, false);
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `accountConfig`：配置对象。
     * - `verificationCode`：`verificationCode` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Verify and save 2FA account config (verifyAndSaveTwoFaAccountConfig)",
            notes = "Checks the verification code for submitted config, and if it is correct, saves the provided account config. " + NEW_LINE +
                    "Returns whole account's 2FA settings object.\n" +
                    "Will throw an error (Bad Request) if the provider is not configured for usage. " +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PostMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings verifyAndSaveTwoFaAccountConfig(@Valid @RequestBody TwoFaAccountConfig accountConfig,
                                                                @RequestParam(required = false) String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();
        if (twoFaConfigManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig.getProviderType()).isPresent()) {
            throw new IllegalArgumentException("2FA provider is already configured");
        }

        boolean verificationSuccess;
        if (accountConfig.getProviderType() != TwoFaProviderType.BACKUP_CODE) {
            verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, accountConfig, false);
        } else {
            verificationSuccess = true;
        }
        if (verificationSuccess) {
            return twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
        } else {
            throw new IllegalArgumentException("Verification code is incorrect");
        }
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `providerType`：类型。
     * - `updateRequest`：请求对象。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Update 2FA account config (updateTwoFaAccountConfig)", notes =
            "Update config for a given provider type. \n" +
                    "Update request example:\n" +
                    "```\n{\n  \"useByDefault\": true\n}\n```\n" +
                    "Returns whole account's 2FA settings object.\n" +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PutMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings updateTwoFaAccountConfig(@RequestParam TwoFaProviderType providerType,
                                                         @RequestBody TwoFaAccountConfigUpdateRequest updateRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        TwoFaAccountConfig accountConfig = twoFaConfigManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType)
                .orElseThrow(() -> new IllegalArgumentException("Config for " + providerType + " 2FA provider not found"));
        accountConfig.setUseByDefault(updateRequest.isUseByDefault());
        return twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
    }

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `providerType`：类型。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Delete 2FA account config (deleteTwoFaAccountConfig)", notes =
            "Delete 2FA config for a given 2FA provider type. \n" +
                    "Returns whole account's 2FA settings object.\n" +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @DeleteMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public AccountTwoFaSettings deleteTwoFaAccountConfig(@RequestParam TwoFaProviderType providerType) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return twoFaConfigManager.deleteTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType);
    }


    /**
     * 功能：获取`Available Two Fa Providers`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get available 2FA providers (getAvailableTwoFaProviders)", notes =
            "Get the list of provider types available for user to use (the ones configured by tenant or sysadmin).\n" +
                    "Example of response:\n" +
                    "```\n[\n  \"TOTP\",\n  \"EMAIL\",\n  \"SMS\"\n]\n```" +
                    ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER
    )
    @GetMapping("/providers")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<TwoFaProviderType> getAvailableTwoFaProviders() throws ThingsboardException {
        return twoFaConfigManager.getPlatformTwoFaSettings(getTenantId(), true)
                .map(PlatformTwoFaSettings::getProviders).orElse(Collections.emptyList()).stream()
                .map(TwoFaProviderConfig::getProviderType)
                .collect(Collectors.toList());
    }


    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get platform 2FA settings (getPlatformTwoFaSettings)",
            notes = "Get platform settings for 2FA. The settings are described for savePlatformTwoFaSettings API method. " +
                    "If 2FA is not configured, then an empty response will be returned." +
                    ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    public PlatformTwoFaSettings getPlatformTwoFaSettings() throws ThingsboardException {
        return twoFaConfigManager.getPlatformTwoFaSettings(getTenantId(), false).orElse(null);
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `value`：值。
     * - `twoFaSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Save platform 2FA settings (savePlatformTwoFaSettings)",
            notes = "Save 2FA settings for platform. The settings have following properties:\n" +
                    "- `providers` - the list of 2FA providers' configs. Users will only be allowed to use 2FA providers from this list. \n\n" +
                    "- `minVerificationCodeSendPeriod` - minimal period in seconds to wait after verification code send request to send next request. \n" +
                    "- `verificationCodeCheckRateLimit` - rate limit configuration for verification code checking.\n" +
                    "The format is standard: 'amountOfRequests:periodInSeconds'. The value of '1:60' would limit verification " +
                    "code checking requests to one per minute.\n" +
                    "- `maxVerificationFailuresBeforeUserLockout` - maximum number of verification failures before a user gets disabled.\n" +
                    "- `totalAllowedTimeForVerification` - total amount of time in seconds allotted for verification. " +
                    "Basically, this property sets a lifetime for pre-verification token. If not set, default value of 30 minutes is used.\n" + NEW_LINE +
                    "TOTP 2FA provider config has following settings:\n" +
                    "- `issuerName` - issuer name that will be displayed in an authenticator app near a username. Must not be blank.\n\n" +
                    "For SMS 2FA provider:\n" +
                    "- `smsVerificationMessageTemplate` - verification message template.  Available template variables " +
                    "are ${code} and ${userEmail}. It must not be blank and must contain verification code variable.\n" +
                    "- `verificationCodeLifetime` - verification code lifetime in seconds. Required to be positive.\n\n" +
                    "For EMAIL provider type:\n" +
                    "- `verificationCodeLifetime` - the same as for SMS." + NEW_LINE +
                    "Example of the settings:\n" +
                    "```\n{\n" +
                    "  \"providers\": [\n" +
                    "    {\n" +
                    "      \"providerType\": \"TOTP\",\n" +
                    "      \"issuerName\": \"TB\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"providerType\": \"EMAIL\",\n" +
                    "      \"verificationCodeLifetime\": 60\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"providerType\": \"SMS\",\n" +
                    "      \"verificationCodeLifetime\": 60,\n" +
                    "      \"smsVerificationMessageTemplate\": \"Here is your verification code: ${code}\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"minVerificationCodeSendPeriod\": 60,\n" +
                    "  \"verificationCodeCheckRateLimit\": \"3:900\",\n" +
                    "  \"maxVerificationFailuresBeforeUserLockout\": 10,\n" +
                    "  \"totalAllowedTimeForVerification\": 600\n" +
                    "}\n```" +
                    ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    public PlatformTwoFaSettings savePlatformTwoFaSettings(@ApiParam(value = "Settings value", required = true)
                                          @RequestBody PlatformTwoFaSettings twoFaSettings) throws ThingsboardException {
        return twoFaConfigManager.savePlatformTwoFaSettings(getTenantId(), twoFaSettings);
    }


    /**
     * 中文说明：
     * 1. `TwoFaAccountConfigUpdateRequest` 是 ThingsBoard Application 中承载请求信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    public static class TwoFaAccountConfigUpdateRequest {
        /**
         * 当前对象是否为默认项。
         */
        private boolean useByDefault;
    }

}
