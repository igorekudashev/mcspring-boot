package dev.alangomes.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = "dev.alangomes.test")
@EnableScheduling
public class TestApplication {

}
