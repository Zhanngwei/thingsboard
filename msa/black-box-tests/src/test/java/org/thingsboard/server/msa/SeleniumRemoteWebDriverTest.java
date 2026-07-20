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
package org.thingsboard.server.msa;

import com.google.common.io.Files;
import io.qameta.allure.Attachment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 中文说明：
 * 1. `SeleniumRemoteWebDriverTest` 是 ThingsBoard Microservices 中验证 `SeleniumRemoteWebDriver` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public class SeleniumRemoteWebDriverTest {

    /**
     * `WIDTH`常量，用于统一引用固定值。
     */
    static final int WIDTH = 1680;
    static final int HEIGHT = 1050;
    final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    /**
     * `driver` 字段，保存当前对象的对应属性。
     */
    WebDriver driver;

    /**
     * 功能：执行 `captureScreen` 对应的处理。
     * 参数：
     * - `driver`：`driver` 参数。
     * - `dirPath`：文件或资源路径。
     * 返回：处理结果。
     */
    @SneakyThrows
    @Attachment(value = "Page screenshot", type = "image/png")
    public static byte[] captureScreen(WebDriver driver, String dirPath) {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(screenshot, new File("./target/allure-results/screenshots/" + dirPath + "//" + screenshot.getName()));
        return Files.toByteArray(screenshot);
    }


    /**
     * Requirement:
     * docker run --name=chrome --rm --network=host -p 4444:4444 -p 7900:7900 --shm-size="2g" -e SE_NODE_MAX_SESSIONS=8 -e SE_NODE_OVERRIDE_MAX_SESSIONS=true -e SE_NODE_SESSION_TIMEOUT=90 -e SE_SCREEN_WIDTH=1920 -e SE_SCREEN_HEIGHT=1080 -e SE_SCREEN_DEPTH=24 -e SE_SCREEN_DPI=74 selenium/standalone-chrome
     * */
    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() throws MalformedURLException {
        log.info("Requirement:");
        log.info("docker run --name=chrome --rm --network=host -p 4444:4444 -p 7900:7900 --shm-size=\"2g\" -e SE_NODE_MAX_SESSIONS=8 -e SE_NODE_OVERRIDE_MAX_SESSIONS=true -e SE_NODE_SESSION_TIMEOUT=90 -e SE_SCREEN_WIDTH=1920 -e SE_SCREEN_HEIGHT=1080 -e SE_SCREEN_DEPTH=24 -e SE_SCREEN_DPI=74 selenium/standalone-chrome");
        log.info("*----------------------* Setup driver *----------------------*");
        ChromeOptions options = new ChromeOptions();
        RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL("http://127.0.0.1:4444"), options);
        remoteWebDriver.setFileDetector(new LocalFileDetector());
        driver = remoteWebDriver;
        driver.manage().window().setSize(dimension);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    void tearDown() {
        log.info("*----------------------* Teardown *----------------------*");
        driver.quit();
    }

    /**
     * 功能：验证`Selenium Connection`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testSeleniumConnection() {
        driver.get("https://thingsboard.io/");
        captureScreen(driver, "success");
        log.info("Check the screenshot on target/allure-results/screenshots/success/screenshot???????????????.png");
        //Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }

}
