package dev.alangomes.test;

import dev.alangomes.BaseTest;
import dev.alangomes.springspigot.configuration.DynamicValue;
import dev.alangomes.springspigot.configuration.Instance;
import dev.alangomes.test.util.SpringSpigotTestInitializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
@ContextConfiguration(
        classes = TestApplication.class,
        initializers = SpringSpigotTestInitializer.class
)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigurationTest extends BaseTest {

    @DynamicValue("${command.message}")
    private Instance<String> commandMessage;

    @DynamicValue("${command.message_list}")
    private Instance<List<String>> commandMessageList;

    @Autowired
    private Plugin plugin;

    private FileConfiguration configuration;

    @BeforeEach
    public void setup() {
        configuration = plugin.getConfig();
    }

    @Test
    public void shouldRetrieveConfigurationFromPlugin() {
        when(configuration.get("command.message")).thenReturn("test message");

        String message = commandMessage.get();

        assertEquals("test message", message);
    }

    @Test
    public void shouldRetrieveDefaultValueSupplied() {
        when(configuration.get("command.message")).thenReturn(null);

        String message = commandMessage.orElse("default value");

        assertEquals("default value", message);
    }

    @Test
    public void shouldRetrieveDefaultValueFromSupplier() {
        when(configuration.get("command.message")).thenReturn(null);

        String message = commandMessage.orElseGet(() -> "default value");

        assertEquals("default value", message);
    }

    @Test
    public void shouldReevaluateConfigurationOnEachCall() {
        when(configuration.get("command.message")).thenReturn("test message");

        commandMessage.get();
        commandMessage.get();

        verify(configuration, times(2)).get("command.message");
    }

    @Test
    public void shouldConvertValueBasedOnGenericType() {
        when(configuration.get("command.message_list")).thenReturn("message1,message2");

        List<String> messages = commandMessageList.get();

        assertEquals(2, messages.size());
        assertEquals("message1", messages.get(0));
        assertEquals("message2", messages.get(1));
    }


}