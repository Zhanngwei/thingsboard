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
     * 中文说明：邮件发件人地址，来源于系统邮件配置、租户邮件配置或规则节点配置；生命周期与单次邮件载荷一致。
     * 设计为字段是为了让异步发送流程无需再次读取配置即可获得发件人。
     */
    private final String from;
    /**
     * 中文说明：主收件人地址列表字符串，来源于规则消息元数据、节点配置或通知目标；生命周期与单次发送请求一致。
     * 设计为字段是为了固定邮件投递目标，避免异步发送期间目标被重新计算。
     */
    private final String to;
    /**
     * 中文说明：抄送地址，来源于规则节点配置或通知模板；生命周期与单次邮件载荷一致。
     * 设计为字段是为了完整表达邮件协议语义，而不是把抄送信息拼入正文。
     */
    private final String cc;
    /**
     * 中文说明：密送地址，来源于规则节点配置或通知模板；生命周期与单次邮件载荷一致。
     * 设计为字段是为了支持邮件投递层直接处理 BCC 语义。
     */
    private final String bcc;
    /**
     * 中文说明：邮件主题，来源于规则节点模板、消息元数据或通知模板渲染结果；生命周期与单次邮件载荷一致。
     * 设计为字段是为了让发送服务无需理解 Rule Engine 模板上下文。
     */
    private final String subject;
    /**
     * 中文说明：邮件正文，来源于规则消息数据、模板渲染或通知内容；生命周期与单次邮件载荷一致。
     * 设计为字段是为了把内容生成与外部邮件发送解耦。
     */
    private final String body;
    /**
     * 中文说明：邮件内联图片映射，key 通常是内容引用标识，value 是图片资源位置或编码；来源于节点配置或模板处理。
     * 生命周期与邮件载荷一致，设计为字段是为了支持 HTML 邮件中的资源引用。
     */
    private final Map<String, String> images;
    /**
     * 中文说明：标记正文是否按 HTML 发送，来源于节点配置或通知模板；生命周期与邮件载荷一致。
     * 设计为字段是为了让邮件发送服务选择正确的 MIME 构造方式。
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
