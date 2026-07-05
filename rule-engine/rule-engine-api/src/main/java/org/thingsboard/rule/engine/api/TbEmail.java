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
package org.thingsboard.rule.engine.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 中文说明：
 * 1. 职责：封装 Rule Engine 邮件节点或通知流程发送邮件时需要的完整邮件载荷。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的外部通知数据模型。
 * 3. 协作对象：与 {@link MailService}、邮件规则节点、通知中心以及外部邮件发送实现协作。
 * 4. 生命周期：由规则节点或通知流程在单次发送前构建，发送完成后即可释放，不持有连接资源。
 * 5. 设计原因：用不可变 Builder DTO 汇总收件人、正文、内联图片和 HTML 标志，避免方法参数过长且便于异步传递。
 * 6. 设计模式：Builder，用 Lombok 生成构建器以提升调用方可读性。
 * 7. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；作为 Rule Engine 调用邮件服务的载荷。
 */
@Data
@Builder
public class TbEmail {

    /**
     * `from` 字段，保存当前对象的对应属性。
     */
    private final String from;
    /**
     * `to` 字段，保存当前对象的对应属性。
     */
    private final String to;
    /**
     * `cc` 字段，保存当前对象的对应属性。
     */
    private final String cc;
    /**
     * `bcc` 字段，保存当前对象的对应属性。
     */
    private final String bcc;
    /**
     * `subject` 字段，保存当前对象的对应属性。
     */
    private final String subject;
    /**
     * `body` 字段，保存当前对象的对应属性。
     */
    private final String body;
    /**
     * `images`映射关系，用于按键查找对应值。
     */
    private final Map<String, String> images;
    /**
     * 是否满足`html`条件。
     */
    private final boolean html;

}

/*
 * 本类总结：
 * 1. 核心职责：承载 Rule Engine 邮件发送所需的不可变邮件数据。
 * 2. 核心流程：规则节点或通知流程构建 TbEmail，邮件服务读取字段并调用外部邮件系统。
 * 3. 关键依赖：MailService、通知模板、规则消息元数据和 Lombok Builder。
 * 4. 学习重点：Rule Engine 通过 DTO 固定异步外部调用的输入，降低节点和发送实现之间的耦合。
 */
