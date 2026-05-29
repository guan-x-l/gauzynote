package com.gauzynote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.gauzynote.*.mapper")
@EnableCaching
@EnableTransactionManagement
public class GauzyNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(GauzyNoteApplication.class, args);
    }

}
