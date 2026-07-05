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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ImageUtils {

    private static final Map<String, String> mediaTypeMappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg",
            "x-icon", "ico"
    );

    /**
     * 功能：执行 `mediaTypeToFileExtension` 对应的处理。
     * 参数：
     * - `mimeType`：类型。
     * 返回：文本结果。
     */
    public static String mediaTypeToFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mediaTypeMappings.getOrDefault(subtype, subtype);
    }

    /**
     * 功能：执行 `fileExtensionToMediaType` 对应的处理。
     * 参数：
     * - `extension`：`extension` 参数。
     * 返回：文本结果。
     */
    public static String fileExtensionToMediaType(String extension) {
        String subtype = mediaTypeMappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType("image", subtype).toString();
    }

    /**
     * 功能：处理图片资源。
     * 参数：
     * - `data`：待处理数据。
     * - `mediaType`：类型。
     * - `thumbnailMaxDimension`：`thumbnailMaxDimension` 参数。
     * 返回：处理结果。
     */
    public static ProcessedImage processImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        if (mediaTypeToFileExtension(mediaType).equals("svg")) {
            try {
                return processSvgImage(data, mediaType, thumbnailMaxDimension);
            } catch (Exception e) {
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
        } catch (Exception ignored) {
        }
        if (bufferedImage == null) { // means that media type is not supported by ImageIO; extracting width and height from metadata and leaving preview as original image
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));
            ProcessedImage image = previewAsOriginalImage(data, mediaType);
            String dirName = "Unknown";
            for (Directory dir : metadata.getDirectories()) {
                Tag widthTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("width"))
                        .findFirst().orElse(null);
                Tag heightTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("height"))
                        .findFirst().orElse(null);
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

        if (preview.getWidth() == image.getWidth() && preview.getHeight() == image.getHeight()) {
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
     * 功能：处理图片资源。
     * 参数：
     * - `data`：待处理数据。
     * - `mediaType`：类型。
     * - `thumbnailMaxDimension`：`thumbnailMaxDimension` 参数。
     * 返回：处理结果。
     */
    public static ProcessedImage processSvgImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        Document document = factory.createDocument(null, new ByteArrayInputStream(data));
        Integer width = null;
        Integer height = null;
        String strWidth = document.getDocumentElement().getAttribute("width");
        String strHeight = document.getDocumentElement().getAttribute("height");
        if (StringUtils.isNotEmpty(strWidth) && StringUtils.isNotEmpty(strHeight)) {
            try {
                width = (int) Double.parseDouble(strWidth);
                height = (int) Double.parseDouble(strHeight);
            } catch (NumberFormatException ignored) {} // in case width and height are in %, mm, etc.
        }
        if (width == null || height == null) {
            String viewBox = document.getDocumentElement().getAttribute("viewBox");
            if (StringUtils.isNotEmpty(viewBox)) {
                String[] viewBoxValues = viewBox.split(" ");
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
     * 功能：执行 `previewAsOriginalImage` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * - `mediaType`：类型。
     * 返回：处理结果。
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
     * 功能：执行 `withPreviewAsOriginalImage` 对应的处理。
     * 参数：
     * - `originalImage`：`originalImage` 参数。
     * 返回：处理结果。
     */
    public static ProcessedImage withPreviewAsOriginalImage(ProcessedImage originalImage) {
        originalImage.setPreview(originalImage.withData(null));
        return originalImage;
    }

    /**
     * 功能：获取`Thumbnail Dimensions`。
     * 参数：
     * - `originalWidth`：`originalWidth` 参数。
     * - `originalHeight`：`originalHeight` 参数。
     * - `maxDimension`：`maxDimension` 参数。
     * - `originalIfSmaller`：`originalIfSmaller` 参数。
     * 返回：处理结果。
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
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessedImage {
        /**
         * 类型，用于区分不同处理分支。
         */
        private String mediaType;
        private int width;
        /**
         * `height` 字段，保存当前对象的对应属性。
         */
        private int height;
        /**
         * 数据列表，用于保存一组待处理对象。
         */
        @With
        private byte[] data;
        private long size;
        /**
         * `preview` 字段，保存当前对象的对应属性。
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
