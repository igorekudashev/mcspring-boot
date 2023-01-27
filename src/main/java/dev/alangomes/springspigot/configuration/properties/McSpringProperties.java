package dev.alangomes.springspigot.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("mcspring")
public class McSpringProperties {

    private Logging logging = new Logging();
    private Registration registration = new Registration();

    @Data
    public static class Logging {

        private boolean onRegisterListener = true;
    }

    @Data
    public static class Registration {

        private boolean registerListeners = true;
    }
}
