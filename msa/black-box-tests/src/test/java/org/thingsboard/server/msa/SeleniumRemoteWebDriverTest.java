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
 * 1. 类目的：`SeleniumRemoteWebDriverTest` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`SeleniumRemoteWebDriverTest` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
