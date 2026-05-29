package com.gauzynote.common.utils.file;

import com.gauzynote.common.exception.ServiceException;
import com.gauzynote.common.utils.MessageUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static com.gauzynote.common.utils.TikaUtils.getExtensionByTika;
import static com.gauzynote.common.utils.file.FileUtils.*;

/**
 * 网络文件转MultipartFile工具类
 * 完整链路：URL → HttpURLConnection → InputStream → File → MultipartFile
 */
public class NetworkFileConverter {
    // 临时文件存储目录（可配置到application.yml）
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/network_files/";

    /**
     * 核心方法：从网络地址获取文件并转换为MultipartFile
     * @param fileUrl 网络文件地址（如https://example.com/test.jpg）
     * @param fieldName 表单字段名（如"file"）
     * @return MultipartFile
     * @throws Exception 网络请求/文件操作异常
     */
    public static MultipartFile convertUrlToMultipartFile(String fileUrl, String fieldName) throws Exception {
        // 1. 第一步：从URL获取HttpURLConnection和InputStream
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        File tempFile = null;

        try {
            // 创建URL对象并打开连接
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 连接超时5秒
            connection.setReadTimeout(10000);  // 读取超时10秒
            connection.setDoInput(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");

            // 校验响应码（200表示成功）
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("请求失败，responseCode：" + responseCode);
            }
            // 从响应头获取文件名和Content-Type
            String contentTypeFromHeader = connection.getContentType();
            // 处理Content-Type可能带参数的情况（如text/html;charset=utf-8）
            if (contentTypeFromHeader != null && contentTypeFromHeader.contains(";")) {
                contentTypeFromHeader = contentTypeFromHeader.split(";")[0].trim();
            }
            System.out.println(contentTypeFromHeader);
            // 获取网络文件的输入流
            inputStream = connection.getInputStream();

            // 2. 第二步：InputStream转本地临时File
            tempFile = inputStreamToFile(inputStream, fileUrl, contentTypeFromHeader);

            // 3. 第三步：File转MultipartFile
            return fileToMultipartFile(tempFile, fieldName, contentTypeFromHeader);

        } finally {
            // 释放资源：关闭流和连接
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
            // 可选：删除临时文件（若不需要保留）
            if (tempFile != null && tempFile.exists()) {
                // tempFile.deleteOnExit(); // JVM退出时删除
                // 或立即删除（转换完成后即可删除）
                 Files.deleteIfExists(tempFile.toPath());
            }
        }
    }

    /**
     * 重载方法：默认表单字段名为"file"
     */
    public static MultipartFile convertUrlToMultipartFile(String fileUrl) throws Exception {
        return convertUrlToMultipartFile(fileUrl, "file");
    }

    /**
     * InputStream 转 本地File（生成临时文件）
     * @param inputStream 网络文件输入流
     * @return 本地临时File
     * @throws IOException IO异常
     */
    private static File inputStreamToFile(InputStream inputStream, String fileUrl, String contentTypeFromHeader) throws IOException {
        // 1. 创建临时目录（不存在则创建）
        Path tempDirPath = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDirPath)) {
            Files.createDirectories(tempDirPath);
        }

        // 根据Content-Type补充扩展名
        String ext = getExtensionByTika(contentTypeFromHeader);

        // 计算输入流的哈希值 + 暂存流内容（解决流只能读一次的问题）
        // 先把输入流读入字节数组（既用于计算哈希，也用于后续写入文件）
        byte[] fileBytes = null;
        String hashFileName;
        try {
            fileBytes = readInputStreamToBytes(inputStream);
            // 输入流计算 SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            hashFileName = bytesToHash(md.digest(fileBytes));
        } catch (NoSuchAlgorithmException e) {
            // 哈希计算失败时用短UUID兜底（8位）
            hashFileName = UUID.randomUUID().toString().substring(0, 8);
        }

        // 生成最终的哈希文件名（哈希值 + 扩展名）
        String uniqueFileName = hashFileName + ext;
        System.out.println(uniqueFileName);
        // 创建临时文件并写入流内容
        File file = new File(TEMP_DIR + uniqueFileName);
        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(fileBytes); // 直接写入暂存的字节数组，效率更高
        }
        return file;
    }
    /**
     * 辅助方法：将InputStream读取为字节数组（解决流只能读一次的问题）
     * @param inputStream 输入流
     * @return 字节数组 缓冲区大小为8192
     * @throws IOException IO异常
     */
    private static byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        baos.close();
        inputStream.close();
        return baos.toByteArray();
    }

    /**
     * File 转 MultipartFile（自动识别MIME类型）
     */
    private static MultipartFile fileToMultipartFile(File file, String fieldName, String contentTypeFromHeader) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("无效的文件：" + file.getAbsolutePath());
        }
        System.out.println(file.getName());
        // 自动识别MIME类型
        String contentType = contentTypeFromHeader;
        // 若识别失败，使用通用二进制类型
        if (contentTypeFromHeader == null) {
            contentType = "application/octet-stream";
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return new MockMultipartFile(
                    fieldName,
                    file.getName(),
                    contentType,
                    fileInputStream
            );
        }
    }
}
