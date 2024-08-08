package dev.alangomes.test;

import dev.alangomes.BaseTest;
import dev.alangomes.springspigot.context.Context;
import dev.alangomes.springspigot.context.SessionService;
import dev.alangomes.springspigot.exception.PermissionDeniedException;
import dev.alangomes.springspigot.exception.PlayerNotFoundException;
import dev.alangomes.springspigot.security.Audit;
import dev.alangomes.springspigot.security.Authorize;
import dev.alangomes.springspigot.security.GuardService;
import dev.alangomes.springspigot.security.HasPermission;
import dev.alangomes.springspigot.security.PlayerOnly;
import dev.alangomes.test.util.SpringSpigotTestInitializer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@Disabled
@ContextConfiguration(
        classes = {TestApplication.class, SecurityTest.TestService.class, SecurityTest.GuardServiceImpl.class},
        initializers = SpringSpigotTestInitializer.class
)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class SecurityTest extends BaseTest {

    @Autowired
    private TestService testService;

    @Autowired
    private Context context;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private Server server;

    @Mock
    private Player player;

    @BeforeEach
    public void setup() {
        when(player.getName()).thenReturn("test_player");
        when(server.getPlayer("test_player")).thenReturn(player);
    }

    @Test
    public void shouldThrowExceptionIfNoPlayerInContext() {
        assertThrows(PlayerNotFoundException.class, () -> testService.sum(2, 2));
    }

    @Test
    public void shouldThrowExceptionIfPermissionWasDenied() {
        when(player.hasPermission("server.kill")).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> context.runWithSender(player, () -> testService.sum(2, 2)));
    }

    @Test
    public void shouldPassCallThroughIfPermissionWasGranted() {
        when(player.hasPermission("server.kill")).thenReturn(true);

        int result = context.runWithSender(player, () -> testService.sum(2, 2));
        assertEquals("The call was not successful", 4, result);
    }

    @Test
    public void shouldEvaluateMethodParameters() {
        when(player.hasPermission(any(String.class))).thenReturn(false);
        when(player.hasPermission("resource.test.create")).thenReturn(true);

        String result = context.runWithSender(player, () -> testService.create("test"));
        assertEquals("The call was not successful", "test", result);
    }

    @Test
    public void shouldPassCallThroughIfMethodIsNotAnnotated() {
        when(player.hasPermission("server.kill")).thenReturn(false);

        int result = context.runWithSender(player, () -> testService.multiply(2, 5));
        assertEquals("The call was not successful", 10, result);
    }

    @Test
    public void shouldCallGuardMethod() {
        when(player.hasPermission("test.admin")).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> context.runWithSender(player, testService::testGuard));
    }

    @Test
    public void shouldThrowExceptionWithCustomMessageIfPermissionWasDenied() {
        when(player.hasPermission("server.shutdown")).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> {
            try {
                context.runWithSender(player, testService::testMessage);
            } catch (PermissionDeniedException exception) {
                assertEquals("The call was not successful", "You cannot shutdown the server!", exception.getMessage());
                throw exception;
            }
        });
    }

    @Test
    public void shouldProvideSessionAccess() {
        context.runWithSender(player, () -> {
            sessionService.current().put("test key", 2);
            testService.assertTwo("test key");
        });
    }

    @Test
    public void shouldThrowExceptionWithCustomMessageIfSenderIsNotPlayer() {
        ConsoleCommandSender sender = mock(ConsoleCommandSender.class);
        when(server.getConsoleSender()).thenReturn(sender);

        assertThrows(PermissionDeniedException.class, () -> {
            try {
                context.runWithSender(sender, testService::testPlayer);
            } catch (PermissionDeniedException exception) {
                assertEquals("The call was not successful", "Only players can run this method!", exception.getMessage());
                throw exception;
            }
        });
    }

    @Service
    static class TestService {
        @Audit(senderOnly = false)
        @HasPermission("server.kill")
        public int sum(int num1, int num2) {
            return num1 + num2;
        }

        @Audit
        @Authorize("hasPermission('resource.' + #arg0 + '.create')")
        public String create(String resourceName) {
            return resourceName;
        }

        @Authorize("#session[#arg0] == 2")
        public void assertTwo(String key) {

        }

        @Authorize("#guard.isAdmin()")
        public void testGuard() {

        }

        @PlayerOnly(message = "Only players can run this method!")
        public void testPlayer() {

        }

        @Authorize(value = "hasPermission('server.shutdown')", message = "    You cannot shutdown the server!   ")
        public void testMessage() {

        }

        public int multiply(int num1, int num2) {
            return num1 * num2;
        }
    }

    @Service
    static class GuardServiceImpl implements GuardService {
        @Autowired
        private Context context;

        public boolean isAdmin() {
            return context.getSender().hasPermission("test.admin");
        }
    }

}