package dev.alangomes.springspigot;

import dev.alangomes.springspigot.context.AfterContextInitializer;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.switchyard.common.type.CompoundClassLoader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public final class SpringSpigotBootstrapper {

    private static final String ADDITIONAL_CONFIG_LOCATION = "spring.config.import";

    private final JavaPlugin plugin;
    private final SpringApplicationBuilder builder;
    private final Set<String> yamlPropertySources = new HashSet<>() {{ add("application.yml"); }};
    private final List<ClassLoader> classLoaders = new ArrayList<>();

    public SpringSpigotBootstrapper(JavaPlugin plugin, Class<?> applicationClass) {
        this(plugin, new SpringApplicationBuilder(applicationClass));
    }

    public SpringSpigotBootstrapper(JavaPlugin plugin, SpringApplicationBuilder builder) {
        this.plugin = plugin;
        this.builder = builder;
    }

    public SpringSpigotBootstrapper addClassLoader(ClassLoader classLoader) {
        this.classLoaders.add(classLoader);
        return this;
    }

    // List of yaml files in plugin folder e.g. application.yml
    public SpringSpigotBootstrapper addYamlPropertySource(String... files) {
        yamlPropertySources.addAll(Arrays.asList(files));
        return this;
    }

    public ConfigurableApplicationContext initialize() {
        val classLoader = new CompoundClassLoader(classLoaders);
        Thread.currentThread().setContextClassLoader(classLoader);

        val context = builder
                .resourceLoader(new DefaultResourceLoader(classLoader))
                .initializers(new SpringSpigotInitializer(plugin, getAdditionalPropertySources()))
                .run();

        context.getBean(AfterContextInitializer.class).initializeAll();
        return context;
    }

    private String getAdditionalYamlSourcesProperty() {
        // TODO: Тут может сломаться если yamlPropertySources пустой (удалится последний символ в конце)
        Path pluginFolder = plugin.getDataFolder().getAbsoluteFile().toPath();

        StringBuilder propertyValueBuilder = new StringBuilder(ADDITIONAL_CONFIG_LOCATION).append("=");
        for (String source : yamlPropertySources) {
            Path sourcePath = pluginFolder.resolve(source);
            boolean isSourceOk = createSourceFileIfNeeded(source, sourcePath);

            if (isSourceOk) {
                propertyValueBuilder.append("file:").append(sourcePath).append(",");
            }
        }
        propertyValueBuilder.deleteCharAt(propertyValueBuilder.length() - 1);
        return propertyValueBuilder.toString();
    }

    private boolean createSourceFileIfNeeded(String source, Path sourcePath) {
        if (Files.exists(sourcePath)) return true;

        try {
            plugin.saveResource(source, false);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Property source '{}' not found in plugin classpath (plugin jar) or plugin folder. Skipping", sourcePath);
            return false;
        }
    }

    private List<PropertySource<?>> getAdditionalPropertySources() {
        return yamlPropertySources.stream()
                .peek(fileName -> {
                    try {
                        plugin.saveResource(fileName, false);
                    } catch (IllegalArgumentException e) {
                        log.error("Error on loading resource {}. Message: {}", fileName, e.getMessage());
                    }
                })
                .map(fileName -> new File(plugin.getDataFolder() + "/" + fileName))
                .filter(File::exists)
                .flatMap(file -> loadYamlPropertySource(file).stream())
//                .flatMap(file -> {
//                    try {
//                        return new YamlPropertySourceLoader().load(file.getAbsolutePath(), new FileSystemResource(file)).stream();
//                    } catch (IOException e) {
//                        log.error("Unexpected error while creating YamlPropertySourceLoader for {}: {}", file.getPath(), e.getMessage());
//                        return Stream.empty();
//                    }
//                })
                .toList();
    }

    // TODO: Привести код в порядок и сделать так чтобы имена папок где лежит конфиг тоже прибавлялись в качестве префикса пропертей
    private List<PropertySource<?>> loadYamlPropertySource(File file) {
        try {
            String fileName = StringUtils.substringBeforeLast(file.getName(), ".");
            if (fileName.equals("application")) {
                return new YamlPropertySourceLoader().load(file.getAbsolutePath(), new FileSystemResource(file));
            }

            Yaml yaml = new Yaml();
            Properties properties = new Properties();
            try (InputStream input = new FileSystemResource(file).getInputStream()) {
                Map<String, Object> yamlMap = yaml.load(input);
                Map<String, Object> transformedMap = new HashMap<>();

                for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
                    transformedMap.put(fileName + entry.getKey(), entry.getValue());
                }
                properties.putAll(flattenMap(transformedMap));
            }

            return List.of(new MapPropertySource(file.getAbsolutePath(), (Map) properties));
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> flattenMap(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> subMap = flattenMap((Map<String, Object>) entry.getValue());
                for (Map.Entry<String, Object> subEntry : subMap.entrySet()) {
                    result.put(entry.getKey() + "." + subEntry.getKey(), subEntry.getValue());
                }
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
