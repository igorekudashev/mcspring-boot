package dev.alangomes.springspigot.event;

import dev.alangomes.springspigot.configuration.properties.SpringSpigotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final SpringEventExecutor eventExecutor;
    private final Server server;

    private final Plugin plugin;
    private final SpringSpigotProperties properties;

    public void registerEvents(Listener listener) {
        getListenerMethods(listener).forEach(method -> registerEvents(listener, method));
        if (properties.getLogging().isListeners()) {
            log.info("Listener {} successfully registered!", listener.getClass().getSimpleName());
        }
    }

    private void registerEvents(Listener listener, Method method) {
        val handler = method.getAnnotation(EventHandler.class);
        val eventType = (Class<? extends Event>) method.getParameters()[0].getType();
        server.getPluginManager().registerEvent(eventType, listener, handler.priority(), eventExecutor.create(method), plugin, handler.ignoreCancelled());
    }

    private Stream<Method> getListenerMethods(Listener listener) {
        val target = AopUtils.getTargetClass(listener);
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(target))
                .filter(method -> method.isAnnotationPresent(EventHandler.class))
                .filter(method -> method.getParameters().length == 1)
                .filter(method -> Event.class.isAssignableFrom(method.getParameters()[0].getType()));
    }

}
