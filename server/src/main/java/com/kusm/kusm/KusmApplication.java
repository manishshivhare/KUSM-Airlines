package com.kusm.kusm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@ComponentScan(basePackages = "com.kusm")
@EnableJpaRepositories(basePackages = "com.kusm.repository")
@EntityScan(basePackages = "com.kusm.model")
public class KusmApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .filename(".env")
                .load();
        dotenv.entries().forEach(entry
                -> System.setProperty(entry.getKey(), entry.getValue())
        );
        SpringApplication.run(KusmApplication.class, args);
    }

}
