package dev.alangomes.springspigot;

import dev.alangomes.springspigot.context.AfterContextInitializer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.switchyard.common.type.CompoundClassLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Slf4j
public final class SpringSpigotBootstrapper {

    private final JavaPlugin plugin;
    private final SpringApplicationBuilder builder;
    private final List<String> additionalYamlProperties = new ArrayList<>();

    private ClassLoader classLoader;

    public SpringSpigotBootstrapper(JavaPlugin plugin, Class<?> applicationClass) {
        this(plugin, new SpringApplicationBuilder(applicationClass));
    }

    public SpringSpigotBootstrapper(JavaPlugin plugin, SpringApplicationBuilder builder) {
        this.plugin = plugin;
        this.builder = builder;
        this.classLoader = new CompoundClassLoader(plugin.getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
    }

    public SpringSpigotBootstrapper setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    // List of yaml files in plugin folder e.g. application.yml
    public SpringSpigotBootstrapper addAdditionalYamlProperties(String ... files) {
        additionalYamlProperties.addAll(Arrays.asList(files));
        return this;
    }

    public ConfigurableApplicationContext initialize() throws ExecutionException, InterruptedException {
        val executor = Executors.newSingleThreadExecutor();
        try {
            Future<ConfigurableApplicationContext> contextFuture = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(classLoader);

                if (builder.application().getResourceLoader() == null) {
                    val loader = new DefaultResourceLoader(classLoader);
                    builder.resourceLoader(loader);
                }
                return builder
                        .initializers(new SpringSpigotInitializer(plugin, getPropertySources()))
                        .run();
            });
            val context = contextFuture.get();
            // TODO: придумать получше
            context.getBean(AfterContextInitializer.class).initializeAll();
            return context;
        } finally {
            executor.shutdown();
        }
    }

    private List<PropertySource<?>> getPropertySources() {
        return additionalYamlProperties.stream()
                .peek(fileName -> {
                    try {
                        plugin.saveResource(fileName, false);
                    } catch (IllegalArgumentException e) {
                        log.error("Error on loading resource {}. Message: {}", fileName, e.getMessage());
                    }
                })
                .map(fileName -> new File(plugin.getDataFolder() + "/" + fileName))
                .filter(File::exists)
                .flatMap(file -> {
                    try {
                        return new YamlPropertySourceLoader().load("appProperties", new FileSystemResource(file)).stream();
                    } catch (IOException e) {
                        log.error("Unexpected error while creating YamlPropertySourceLoader for {}: {}", file.getPath(), e.getMessage());
                        return Stream.empty();
                    }
                })
                .toList();
    }
}
