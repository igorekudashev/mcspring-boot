package dev.alangomes.springspigot;

import dev.alangomes.BaseTest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
public class SpringSpigotInitializerTest extends BaseTest {

    private static final String PLUGIN_NAME = "TestPlugin";

    @Mock
    private Plugin plugin;

    @Mock
    private ConfigurableApplicationContext context;

    @Mock
    private MutablePropertySources propertySources;

    @Captor
    private ArgumentCaptor<PropertiesPropertySource> propertySourceCaptor;

    private SpringSpigotInitializer initializer;

    @BeforeEach
    public void setup() {
        initializer = new SpringSpigotInitializer(plugin);

        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getPropertySources()).thenReturn(propertySources);

        when(plugin.getConfig()).thenReturn(mock(FileConfiguration.class));
        when(plugin.getName()).thenReturn(PLUGIN_NAME);
    }

    @Test
    public void shouldRegisterPluginConfigurationPropertySource() {
        initializer.initialize(context);

        verify(propertySources).addLast(any(ConfigurationPropertySource.class));
    }

    @Test
    public void shouldRegisterHookProperties() {
        initializer.initialize(context);

        verify(propertySources, times(1)).addLast(propertySourceCaptor.capture());

        PropertiesPropertySource propertySource = propertySourceCaptor.getValue();
        Map<String, Object> props = propertySource.getSource();

        assertEquals(PLUGIN_NAME, props.get("spigot.plugin.name"));
    }

}