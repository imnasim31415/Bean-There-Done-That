package com.beanthere;

import com.beanthere.listener.EarlyStartupCapture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeanThereDoneThatApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BeanThereDoneThatApplication.class);
        app.addListeners(new EarlyStartupCapture());
        app.run(args);
    }
}
