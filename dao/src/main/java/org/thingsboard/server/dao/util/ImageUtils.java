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
package org.thingsboard.server.dao.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`ImageUtils` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Utility / Factory / Template。
 */
public class ImageUtils {

    private static final Map<String, String> mediaTypeMappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg",
            "x-icon", "ico"
    );

    /**
     * 方法说明：
     * 1. 职责：执行 `mediaTypeToFileExtension` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String mediaTypeToFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mediaTypeMappings.getOrDefault(subtype, subtype);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fileExtensionToMediaType` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String fileExtensionToMediaType(String extension) {
        String subtype = mediaTypeMappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType("image", subtype).toString();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processImage` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static ProcessedImage processImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (mediaTypeToFileExtension(mediaType).equals("svg")) {
            try {
                return processSvgImage(data, mediaType, thumbnailMaxDimension);
            // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
            } catch (Exception e) {
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                if (log.isDebugEnabled()) { // printing stacktrace
                    log.warn("Couldn't process SVG image, leaving preview as original image", e);
                } else {
                    log.warn("Couldn't process SVG image, leaving preview as original image: {}", ExceptionUtils.getMessage(e));
                }
                return previewAsOriginalImage(data, mediaType);
            }
        }
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
        } catch (Exception ignored) {
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (bufferedImage == null) { // means that media type is not supported by ImageIO; extracting width and height from metadata and leaving preview as original image
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));
            ProcessedImage image = previewAsOriginalImage(data, mediaType);
            String dirName = "Unknown";
            // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
            for (Directory dir : metadata.getDirectories()) {
                Tag widthTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("width"))
                        .findFirst().orElse(null);
                Tag heightTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("height"))
                        .findFirst().orElse(null);
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                if (widthTag == null || heightTag == null) {
                    continue;
                }
                int width = Integer.parseInt(dir.getObject(widthTag.getTagType()).toString());
                int height = Integer.parseInt(dir.getObject(heightTag.getTagType()).toString());
                image.setWidth(width);
                image.setHeight(height);
                image.getPreview().setWidth(width);
                image.getPreview().setHeight(height);
                dirName = dir.getName();
                break;
            }
            log.warn("Couldn't process {} ({}) with ImageIO, leaving preview as original image", mediaType, dirName);
            return image;
        }

        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setData(data);
        image.setSize(data.length);
        image.setWidth(bufferedImage.getWidth());
        image.setHeight(bufferedImage.getHeight());

        ProcessedImage preview = new ProcessedImage();
        int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension, true);
        preview.setWidth(thumbnailDimensions[0]);
        preview.setHeight(thumbnailDimensions[1]);

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (preview.getWidth() == image.getWidth() && preview.getHeight() == image.getHeight()) {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (mediaType.equals("image/png")) {
                preview.setMediaType(mediaType);
                preview.setData(null);
                preview.setSize(data.length);
                image.setPreview(preview);
                return image;
            }
        }

        BufferedImage thumbnail = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(bufferedImage, 0, 0, preview.getWidth(), preview.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "png", out);

        preview.setMediaType("image/png");
        preview.setData(out.toByteArray());
        preview.setSize(preview.getData().length);
        image.setPreview(preview);
        return image;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processSvgImage` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static ProcessedImage processSvgImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        Document document = factory.createDocument(null, new ByteArrayInputStream(data));
        Integer width = null;
        Integer height = null;
        String strWidth = document.getDocumentElement().getAttribute("width");
        String strHeight = document.getDocumentElement().getAttribute("height");
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (StringUtils.isNotEmpty(strWidth) && StringUtils.isNotEmpty(strHeight)) {
            try {
                width = (int) Double.parseDouble(strWidth);
                height = (int) Double.parseDouble(strHeight);
            // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
            } catch (NumberFormatException ignored) {} // in case width and height are in %, mm, etc.
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (width == null || height == null) {
            String viewBox = document.getDocumentElement().getAttribute("viewBox");
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (StringUtils.isNotEmpty(viewBox)) {
                String[] viewBoxValues = viewBox.split(" ");
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                if (viewBoxValues.length > 3) {
                    width = (int) Double.parseDouble(viewBoxValues[2]);
                    height = (int) Double.parseDouble(viewBoxValues[3]);
                }
            }
        }
        if (width == null) {
            UserAgent agent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(agent);
            BridgeContext context = new BridgeContext(agent, loader);
            context.setDynamic(true);
            GVTBuilder builder = new GVTBuilder();
            GraphicsNode root = builder.build(context, document);
            var bounds = root.getPrimitiveBounds();
            if (bounds != null) {
                width = (int) bounds.getWidth();
                height = (int) bounds.getHeight();
            }
        }
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setWidth(width == null ? 0 : width);
        image.setHeight(height == null ? 0 : height);
        image.setData(data);
        image.setSize(data.length);

        PNGTranscoder transcoder = new PNGTranscoder();
        if (image.getSize() < 10240) { // if SVG is smaller than 10kB (average 250x250 PNG preview size)
            return withPreviewAsOriginalImage(image);
        } else if (image.getSize() > 102400 && image.getWidth() != 0) { // considering SVG image detailed after 100kB
            // increasing preview dimensions
            thumbnailMaxDimension = 512;
            int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension, false);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) thumbnailDimensions[0]);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) thumbnailDimensions[1]);
        } else {
            transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_WIDTH, (float) thumbnailMaxDimension);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_HEIGHT, (float) thumbnailMaxDimension);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transcoder.transcode(new TranscoderInput(new ByteArrayInputStream(data)), new TranscoderOutput(out));
        byte[] pngThumbnail = out.toByteArray();

        ProcessedImage preview = new ProcessedImage();
        preview.setWidth(thumbnailMaxDimension);
        preview.setHeight(thumbnailMaxDimension);
        preview.setMediaType("image/png");
        preview.setData(pngThumbnail);
        preview.setSize(pngThumbnail.length);
        image.setPreview(preview);
        return image;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `previewAsOriginalImage` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private static ProcessedImage previewAsOriginalImage(byte[] data, String mediaType) {
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setData(data);
        image.setSize(data.length);
        image.setWidth(0);
        image.setHeight(0);
        return withPreviewAsOriginalImage(image);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `withPreviewAsOriginalImage` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static ProcessedImage withPreviewAsOriginalImage(ProcessedImage originalImage) {
        originalImage.setPreview(originalImage.withData(null));
        return originalImage;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getThumbnailDimensions` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private static int[] getThumbnailDimensions(int originalWidth, int originalHeight, int maxDimension, boolean originalIfSmaller) {
        if (originalWidth <= maxDimension && originalHeight <= maxDimension && originalIfSmaller) {
            return new int[]{originalWidth, originalHeight};
        }
        int thumbnailWidth;
        int thumbnailHeight;
        double aspectRatio = (double) originalWidth / originalHeight;
        if (originalWidth > originalHeight) {
            thumbnailWidth = maxDimension;
            thumbnailHeight = (int) (maxDimension / aspectRatio);
        } else {
            thumbnailWidth = (int) (maxDimension * aspectRatio);
            thumbnailHeight = maxDimension;
        }
        return new int[]{thumbnailWidth, thumbnailHeight};
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    /**
     * 中文说明：
     * 1. 类目的：`ProcessedImage` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
     * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Utility / Factory / Template。
     */
    public static class ProcessedImage {
        /**
         * 字段说明：
         * 1. 保存 `mediaType` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private String mediaType;
        private int width;
        /**
         * 字段说明：
         * 1. 保存 `height` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private int height;
        @With
        /**
         * 字段说明：
         * 1. 保存 `data` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private byte[] data;
        private long size;
        /**
         * 字段说明：
         * 1. 保存 `preview` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private ProcessedImage preview;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ImageUtils` 在 ThingsBoard DAO 模块 中承担DAO 工具和配置类型职责，核心目的是提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 核心流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
 * 3. 关键依赖：主要依赖或协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
