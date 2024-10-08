package dev.alangomes.springspigot;

import dev.alangomes.springspigot.configuration.properties.SpringSpigotProperties;
import dev.alangomes.springspigot.context.Context;
import dev.alangomes.springspigot.event.EventService;
import dev.alangomes.springspigot.scope.SenderContextScope;
import dev.alangomes.springspigot.util.scheduler.SchedulerService;
import dev.alangomes.springspigot.util.scheduler.SpigotScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfiguration;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@AutoConfiguration
@ComponentScan("dev.alangomes.springspigot")
@ConditionalOnClass({Bukkit.class})
@RequiredArgsConstructor
class SpringSpigotAutoConfiguration {

    private final ConfigurableApplicationContext applicationContext;
    private final SpringSpigotProperties properties;

    private boolean initialized = false;

    @EventListener
    public void onStartup(ContextRefreshedEvent event) {
        if (initialized) return;
        initialized = true;
        if (properties.getRegistration().isListeners()) {
            val beans = applicationContext.getBeansOfType(Listener.class).values();
            val eventService = applicationContext.getBean(EventService.class);
            beans.forEach(eventService::registerEvents);
        }
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    @ConditionalOnBean(SchedulingConfiguration.class)
    public TaskScheduler taskScheduler(Context context, SchedulerService scheduler, @Value("${spigot.scheduler.poolSize:1}") int poolSize) {
        val taskScheduler = new SpigotScheduler(scheduler, context);
        taskScheduler.setPoolSize(poolSize);
        taskScheduler.initialize();
        return taskScheduler;
    }

    @Bean(destroyMethod = "")
    Server serverBean(Plugin plugin) {
        return plugin.getServer();
    }

    @Bean(destroyMethod = "")
    Plugin pluginBean(Environment environment) {
        return environment.getProperty("spigot.plugin.instance", Plugin.class);
    }

    @Bean(destroyMethod = "")
    BukkitScheduler schedulerBean(Server server) {
        return server.getScheduler();
    }

    @Bean
    public static BeanFactoryPostProcessor scopeBeanFactoryPostProcessor(SenderContextScope scope) {
        return new ScopePostProcessor(scope);
    }
}