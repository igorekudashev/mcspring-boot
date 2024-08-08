package dev.alangomes.springspigot;

import dev.alangomes.BaseTest;
import dev.alangomes.springspigot.configuration.properties.SpringSpigotProperties;
import dev.alangomes.springspigot.event.EventService;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpringSpigotAutoConfigurationTest extends BaseTest {
    
    @Mock
    private ConfigurableApplicationContext context;

    @Mock
    private EventService eventService;

    @Spy
    private SpringSpigotProperties properties;

    @InjectMocks
    private SpringSpigotAutoConfiguration startupHook;

    @BeforeEach
    public void setup() {
        Map<String, Listener> beans = new HashMap<>();
        beans.put("b1", mock(Listener.class));
        beans.put("b2", mock(Listener.class));
        when(context.getBeansOfType(Listener.class)).thenReturn(beans);
        when(context.getBean(EventService.class)).thenReturn(eventService);
    }

    @Test
    public void shouldRegisterAllListeners() {
        startupHook.onStartup(null);

        verify(eventService, times(2)).registerEvents(notNull());
    }

}