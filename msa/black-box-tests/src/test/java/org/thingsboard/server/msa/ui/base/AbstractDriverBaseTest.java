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
package org.thingsboard.server.msa.ui.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ContainerTestSuite;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

/**
 * 中文说明：
 * 1. `AbstractDriverBaseTest` 是 ThingsBoard Microservices 中验证 `AbstractDriverBase` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractContainerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
abstract public class AbstractDriverBaseTest extends AbstractContainerTest {

    /**
     * `driver` 字段，保存当前对象的对应属性。
     */
    protected WebDriver driver;
    private final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    /**
     * `WIDTH`常量，用于统一引用固定值。
     */
    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    /**
     * 主机地址常量，用于统一引用固定值。
     */
    private static final String REMOTE_WEBDRIVER_HOST = "http://localhost:4444";
    protected final PageLink pageLink = new PageLink(30);
    private final ContainerTestSuite instance = ContainerTestSuite.getInstance();
    /**
     * `js` 字段，保存当前对象的对应属性。
     */
    private JavascriptExecutor js;
    public static final long WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private final Duration duration = Duration.ofMillis(WAIT_TIMEOUT);
    /**
     * `webStorage` 字段，保存当前对象的对应属性。
     */
    private WebStorage webStorage;

    /**
     * 功能：初始化或启动`Up`。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void startUp() throws MalformedURLException {
        log.info("===>>> Setup driver");
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("-remote-allow-origins=*"); //temporary fix after updating google chrome
        if (instance.isActive()) {
            RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL(REMOTE_WEBDRIVER_HOST), options);
            remoteWebDriver.setFileDetector(new LocalFileDetector());
            driver = remoteWebDriver;
        } else {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        driver.manage().window().setSize(dimension);
        openBaseUiUrl();
    }

    /**
     * 功能：执行 `open` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void open() {
        openBaseUiUrl();
    }

    /**
     * 功能：保存或创建`Screenshot To Report`。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void addScreenshotToReport() {
        captureScreen(driver, "After test page screenshot");
    }

    /**
     * 功能：执行 `teardown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void teardown() {
        log.info("<<<=== Teardown");
        driver.quit();
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getJwtTokenFromLocalStorage() {
        return (String) getJs().executeScript("return window.localStorage.getItem('jwt_token');");
    }

    /**
     * 功能：执行 `openBaseUiUrl` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void openBaseUiUrl() {
        driver.get(getBaseUiUrl());
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * 功能：获取`Driver`。
     * 参数：无。
     * 返回：处理结果。
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * 功能：执行 `urlContains` 对应的处理。
     * 参数：
     * - `urlPath`：文件或资源路径。
     * 返回：判断结果。
     */
    protected boolean urlContains(String urlPath) {
        WebDriverWait wait = new WebDriverWait(driver, duration);
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            return fail("URL not contains " + urlPath);
        }
        return driver.getCurrentUrl().contains(urlPath);
    }

    /**
     * 功能：执行 `jsClick` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void jsClick(WebElement element) {
        getJs().executeScript("arguments[0].click();", element);
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public RuleChain getRuleChainByName(String name) {
        return testRestClient.getRuleChains(pageLink).getData().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst().orElse(null);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public Device getDeviceByName(String name) {
        return testRestClient.getDevices(pageLink).getData().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst().orElse(null);
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `deviceNames`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    public List<Device> getDevicesByName(List<String> deviceNames) {
        List<Device> allDevices = testRestClient.getDevices(pageLink).getData();
        return allDevices.stream()
                .filter(device -> deviceNames.contains(device.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    public List<RuleChain> getRuleChainsByName(String name) {
        return testRestClient.getRuleChains(pageLink).getData().stream()
                .filter(s -> s.getName().equals(name))
                .collect(Collectors.toList());
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public Customer getCustomerByName(String name) {
        try {
            return testRestClient.getCustomers(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such customer with name: " + name);
            return null;
        }
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public DeviceProfile getDeviceProfileByName(String name) {
        return testRestClient.getDeviceProfiles(pageLink).getData().stream()
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    public AssetProfile getAssetProfileByName(String name) {
        try {
            return testRestClient.getAssetProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such asset profile with name: " + name);
            return null;
        }
    }

    /**
     * 功能：执行 `captureScreen` 对应的处理。
     * 参数：
     * - `driver`：`driver` 参数。
     * - `screenshotName`：名称。
     * 返回：无。
     */
    public void captureScreen(WebDriver driver, String screenshotName) {
        if (driver instanceof TakesScreenshot) {
            Allure.addAttachment(screenshotName,
                    new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        }
    }

    /**
     * 功能：获取`Js`。
     * 参数：无。
     * 返回：处理结果。
     */
    public JavascriptExecutor getJs() {
        return js = (JavascriptExecutor) driver;
    }

    /**
     * 功能：执行 `assertIsDisplayed` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void assertIsDisplayed(WebElement element) {
        assertThat(element.isDisplayed()).as(element + " is displayed").isTrue();
    }

    /**
     * 功能：执行 `assertIsDisable` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void assertIsDisable(WebElement element) {
        assertThat(element.isEnabled()).as(element + " is disabled").isFalse();
    }

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：无。
     */
    public void deleteRuleChainByName(String ruleChainName) {
        List<RuleChain> ruleChains = getRuleChainsByName(ruleChainName);
        if (!ruleChains.isEmpty()) {
            ruleChains.forEach(rc -> testRestClient.deleteRuleChain(rc.getId()));
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：无。
     */
    public void setRootRuleChain(String ruleChainName) {
        List<RuleChain> ruleChains = getRuleChainsByName(ruleChainName);
        if (!ruleChains.isEmpty()) {
            testRestClient.setRootRuleChain(ruleChains.stream().findFirst().get().getId());
        }
    }

    /**
     * 功能：删除或清理`Storage`。
     * 参数：无。
     * 返回：无。
     */
    public void clearStorage() {
        getJs().executeScript("window.localStorage.clear();");
        getJs().executeScript("window.sessionStorage.clear();");
    }

    /**
     * 功能：删除或清理告警ID。
     * 参数：
     * - `alarmId`：告警IDID。
     * 返回：无。
     */
    public void deleteAlarmById(AlarmId alarmId) {
        if (alarmId != null) {
            testRestClient.deleteAlarm(alarmId);
        }
    }

    /**
     * 功能：删除或清理`Alarms By Ids`。
     * 参数：
     * - `alarmIds`：`alarmIds` 参数。
     * 返回：无。
     */
    public void deleteAlarmsByIds(AlarmId... alarmIds) {
        for (AlarmId alarmId : alarmIds) {
            deleteAlarmById(alarmId);
        }
    }

    /**
     * 功能：删除或清理客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    public void deleteCustomerById(CustomerId customerId) {
        if (customerId != null) {
            testRestClient.deleteCustomer(customerId);
        }
    }

    /**
     * 功能：删除或清理客户。
     * 参数：
     * - `customerName`：名称。
     * 返回：无。
     */
    public void deleteCustomerByName(String customerName) {
        Customer customer = getCustomerByName(customerName);
        if (customer != null) {
            testRestClient.deleteCustomer(customer.getId());
        }
    }

    /**
     * 功能：删除或清理设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    public void deleteDeviceById(DeviceId deviceId) {
        if (deviceId != null) {
            testRestClient.deleteDevice(deviceId);
        }
    }

    /**
     * 功能：删除或清理资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    public void deleteAssetById(AssetId assetId) {
        if (assetId != null) {
            testRestClient.deleteAsset(assetId);
        }
    }

    /**
     * 功能：删除或清理实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * 返回：无。
     */
    public void deleteEntityView(EntityViewId entityViewId) {
        if (entityViewId != null) {
            testRestClient.deleteEntityView(entityViewId);
        }
    }

    /**
     * 功能：删除或清理仪表盘ID。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    public void deleteDashboardById(DashboardId dashboardId) {
        if (dashboardId != null) {
            testRestClient.deleteDashboard(dashboardId);
        }
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void deleteDeviceByName(String deviceName) {
        Device device = getDeviceByName(deviceName);
        if (device != null) {
            testRestClient.deleteDevice(device.getId());
        }
    }

    /**
     * 功能：删除或清理名称。
     * 参数：
     * - `deviceNames`：设备信息或设备标识。
     * 返回：无。
     */
    public void deleteDevicesByName(List<String> deviceNames) {
        List<Device> devices = getDevicesByName(deviceNames);
        for (Device device : devices) {
            if (device != null) {
                testRestClient.deleteDevice(device.getId());
            }
        }
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：
     * - `deviceProfileTitle`：设备信息或设备标识。
     * 返回：无。
     */
    public void deleteDeviceProfileByTitle(String deviceProfileTitle) {
        DeviceProfile deviceProfile = getDeviceProfileByName(deviceProfileTitle);
        if (deviceProfile != null) {
            testRestClient.deleteDeviseProfile(deviceProfile.getId());
        }
    }

    /**
     * 功能：执行 `assertInvisibilityOfElement` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void assertInvisibilityOfElement(WebElement element) {
        try {
            new WebDriverWait(driver, duration).until(ExpectedConditions.invisibilityOf(element));
        } catch (WebDriverException e) {
            fail("Element " + element.toString() + " stay visible");
        }
    }
}
