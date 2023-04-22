package dev.alangomes.springspigot.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring-spigot")
public class SpringSpigotProperties {

    private Logging logging = new Logging();
    private Registration registration = new Registration();

    @Data
    public static class Logging {

        private boolean listeners = true;
    }

    @Data
    public static class Registration {

        private boolean listeners = true;
    }
}
