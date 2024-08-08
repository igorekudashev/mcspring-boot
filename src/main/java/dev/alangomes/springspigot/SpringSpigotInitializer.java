package dev.alangomes.springspigot;

import lombok.val;
import org.bukkit.plugin.Plugin;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.List;
import java.util.Properties;

/**
 * Initializer that set core properties and adds config yml source
 */
public class SpringSpigotInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private final Plugin plugin;
    private final List<PropertySource<?>> additionalProperties;

    public SpringSpigotInitializer(Plugin plugin) {
        this(plugin, List.of());
    }

    public SpringSpigotInitializer(Plugin plugin, List<PropertySource<?>> additionalProperties) {
        this.plugin = plugin;
        this.additionalProperties = additionalProperties;
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        val propertySources = context.getEnvironment().getPropertySources();
        additionalProperties.forEach(propertySources::addLast);
        propertySources.addLast(new ConfigurationPropertySource(plugin.getConfig()));

        val props = new Properties();
        props.put("spigot.plugin.instance", plugin);
        props.put("spigot.plugin.name", plugin.getName());
        propertySources.addLast(new PropertiesPropertySource("spring-bukkit", props));
    }
}