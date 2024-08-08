package dev.alangomes.springspigot.command;

import dev.alangomes.BaseTest;
import dev.alangomes.springspigot.configuration.Instance;
import dev.alangomes.springspigot.picocli.CommandLineDefinition;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
public class DefaultCommandExecutorTest extends BaseTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private CommandLineDefinition commandLineDefinition;

    @Mock
    private Runnable commandRunnable;

    @Mock
    private Callable commandCallable;

    @Mock
    private CommandLine command, commandLine;

    @Mock
    private CommandLine.Model.ArgSpec argument;

    @InjectMocks
    private DefaultCommandExecutor commandExecutor;

    @BeforeEach
    public void setup() {
        Instance<Boolean> cacheEnabled = mock(Instance.class);
        when(cacheEnabled.get()).thenReturn(false);
        commandExecutor.setCacheEnabled(cacheEnabled);

        when(commandLineDefinition.build(applicationContext)).thenReturn(commandLine);
        when(commandLine.parse(any(String[].class))).thenReturn(Collections.singletonList(command));

        when(command.getCommand()).thenReturn(commandRunnable);
        CommandSpec commandSpec = mock(CommandSpec.class);
        when(commandLine.getCommandSpec()).thenReturn(commandSpec);

        when(argument.paramLabel()).thenReturn("<parameter>");

    }

    @Test
    public void shouldRunExistingCommandSuccessfully() {
        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLineDefinition).build(applicationContext);
        verify(commandLine).parse("say", "hello");
        verify(commandRunnable).run();
    }

    @Test
    public void shouldRunExistingCommandSuccessfullyFromConsole() {
        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        verify(commandRunnable).run();
    }

    @Test
    public void shouldIgnoreUnknownCommand() {
        when(commandLine.parse(any())).thenReturn(Collections.emptyList());
        CommandResult result = commandExecutor.execute("say", "hello");

        assertFalse(result.isExists());
        verify(commandLine).parse("say", "hello");
    }

    @Test
    public void shouldIgnoreInvalidCommand() {
        when(commandLine.parse(any())).thenThrow(new CommandLine.UnmatchedArgumentException(commandLine, ""));

        CommandResult result = commandExecutor.execute("say", "hello");

        assertFalse(result.isExists());
        verify(commandLine).parse("say", "hello");
    }

    @Test
    public void shouldSendCallableStringOutput() throws Exception {
        when(commandCallable.call()).thenReturn("hello world");
        when(command.getCommand()).thenReturn(commandCallable);

        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        verify(commandCallable).call();
        assertEquals("hello world", result.getOutput().get(0));
    }

    @Test
    public void shouldSendCallableListOutput() throws Exception {
        when(commandCallable.call()).thenReturn(Arrays.asList("hello", "world"));
        when(command.getCommand()).thenReturn(commandCallable);

        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        verify(commandCallable).call();
        assertEquals("hello", result.getOutput().get(0));
        assertEquals("world", result.getOutput().get(1));
    }

    @Test
    public void shouldSendMissingParameterError() {
        Instance<String> instance = mock(Instance.class);
        when(instance.get()).thenReturn("&amissing parameter: %s");
        commandExecutor.setMissingParameterErrorMessage(instance);
        when(commandLine.parse(any())).thenThrow(new CommandLine.MissingParameterException(commandLine, argument, ""));

        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        assertEquals(ChatColor.GREEN + "missing parameter: <parameter>", result.getOutput().get(0));
    }

    @Test
    public void shouldSendInvalidParameterError() {
        Instance<String> instance = mock(Instance.class);
        when(instance.get()).thenReturn("&binvalid parameter: %s");
        commandExecutor.setParameterErrorMessage(instance);
        when(commandLine.parse(any())).thenThrow(new CommandLine.ParameterException(commandLine, "", argument, ""));

        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        assertEquals(ChatColor.AQUA + "invalid parameter: <parameter>", result.getOutput().get(0));
    }

    @Test
    public void shouldSendCommandErrorMessage() {
        when(commandLine.parse(any())).thenThrow(new CommandException("generic error"));

        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        assertEquals(ChatColor.RED + "generic error", result.getOutput().get(0));
    }

    @Test
    public void shouldSendGenericErrorMessage() {
        Instance<String> instance = mock(Instance.class);
        when(instance.get()).thenReturn("&cunexpected error");
        commandExecutor.setCommandErrorMessage(instance);
        when(commandLine.parse(any())).thenThrow(new RuntimeException("ignored message"));

        CommandResult result = commandExecutor.execute("say", "hello");

        assertTrue(result.isExists());
        verify(commandLine).parse("say", "hello");
        assertEquals(ChatColor.RED + "unexpected error", result.getOutput().get(0));
    }

    @Test
    public void shouldStoreCommandLineCache() {
        when(commandExecutor.getCacheEnabled().get()).thenReturn(true);

        commandExecutor.execute("say", "hello");
        commandExecutor.execute("say", "hello");

        verify(commandLineDefinition).build(applicationContext);
    }

}