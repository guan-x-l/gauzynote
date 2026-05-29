package com.gauzynote.common.utils;

import org.apache.tika.mime.MimeTypes;

public class TikaUtils {

    public static String getExtensionByTika(String contentType) {
        try {
            return MimeTypes.getDefaultMimeTypes().forName(contentType).getExtension();
        } catch (Exception e) {
            return ".bin";
        }
    }
}
