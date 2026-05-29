package com.gauzynote.common.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {
    private String name;
    private String version;
    private String description;

    // 嵌套对象，对应 YAML 中的 env 节点
    private Env env;
    // 对应 YAML 中的 upload 节点
    private Upload upload;
    private Cors cors;

    @Data
    public static class Env {
        private String envName;
        private String logLevel;
        private Map<String, Boolean> featureFlags;
    }

    @Data
    public static class Upload {
        private long maxTotalCapacity;
        private String dir;
        private Image image;
        private OtherFile otherFile;

        public String getUploadImageDir() {
            return this.dir + image.getDir();
        }

        public String getUploadOtherFileDir() {
            return this.dir + otherFile.getDir();
        }

        @Data
        public static class Image {
            private String dir;
            private String[] allowedContentTypes;
            private int maxAge;
        }
        @Data
        public static class OtherFile {
            private String dir;
        }
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
        private Boolean allowCredentials;
    }

}
