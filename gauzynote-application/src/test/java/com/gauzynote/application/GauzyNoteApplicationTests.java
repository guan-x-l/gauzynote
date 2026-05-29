package com.gauzynote.application;

import com.gauzynote.GauzyNoteApplication;
import com.gauzynote.common.utils.file.NetworkFileConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(classes = GauzyNoteApplication.class)
class GauzyNoteApplicationTests {

    @Test
    void contextLoads() {
    }
    @Test
    public void test(){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("123456"));
        System.out.println(encoder.encode("admin123"));
        System.out.println(encoder.encode("admin123"));
    }
    @Test
    public void downloadImage(){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("123456"));
        System.out.println(encoder.encode("admin123"));
        System.out.println(encoder.encode("admin123"));
    }

    // 测试方法
    @Test
    public void NetworkFileConverter() {
        NetworkFileConverter converter = new NetworkFileConverter();
        try {
            // 测试网络文件地址（替换为实际可访问的地址）
            String testUrl = "https://www.example.com/aaa.webp";
            MultipartFile multipartFile = converter.convertUrlToMultipartFile(testUrl);

            // 打印结果验证
            System.out.println("转换成功！");
            System.out.println("文件名：" + multipartFile.getOriginalFilename());
            System.out.println("文件类型：" + multipartFile.getContentType());
            System.out.println("文件大小：" + multipartFile.getSize() + " 字节");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
