package dev.alangomes.test;

import dev.alangomes.BaseTest;
import dev.alangomes.springspigot.context.Context;
import dev.alangomes.springspigot.event.SpringEventExecutor;
import dev.alangomes.springspigot.security.Audit;
import dev.alangomes.test.util.SpringSpigotTestInitializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
@ContextConfiguration(
        classes = {TestApplication.class, ListenerTest.TestListener.class},
        initializers = SpringSpigotTestInitializer.class
)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ListenerTest extends BaseTest {

    @Autowired
    private TestListener testListener;

    @Autowired
    private SpringEventExecutor springEventExecutor;

    @Autowired
    private Server server;

    @Autowired
    private Plugin plugin;

    @SpyBean
    private Context context;

    @Mock
    private Player player;

    @BeforeEach
    public void setup() {
        when(player.getName()).thenReturn("player");
        when(server.getPlayer("player")).thenReturn(player);
    }

    @Test
    public void shouldRegisterAllEventsInTheListener() {
        verify(server.getPluginManager()).registerEvent(eq(PlayerJoinEvent.class), eq(testListener),
                eq(EventPriority.NORMAL), notNull(), eq(plugin), eq(true));
        verify(server.getPluginManager()).registerEvent(eq(PlayerQuitEvent.class), eq(testListener),
                eq(EventPriority.HIGHEST), notNull(), eq(plugin), eq(false));
    }

    @Test
    @SneakyThrows
    public void shouldExecuteEventOnListener() {
        springEventExecutor
                .create(TestListener.class.getDeclaredMethod("onJoin", PlayerJoinEvent.class))
                .execute(testListener, new PlayerJoinEvent(player, ""));

        verify(player).sendMessage("join");
        verify(player, never()).sendMessage("quit");
    }

    @Test
    @SneakyThrows
    public void shouldInferSenderFromGetters() {
        springEventExecutor
                .create(TestListener.class.getDeclaredMethod("onTest", TestEvent.class))
                .execute(testListener, new TestEvent(player));

        verify(context).runWithSender(eq(player), any(Runnable.class));
    }

    @Component
    @Audit
    static class TestListener implements Listener {

        @Autowired
        private Context context;

        @EventHandler(ignoreCancelled = true)
        public void onJoin(PlayerJoinEvent event) {
            context.getPlayer().sendMessage("join");
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onQuit(PlayerQuitEvent event) {
            context.getPlayer().sendMessage("quit");
        }

        @EventHandler
        public void onTest(TestEvent event) {

        }

    }

    @AllArgsConstructor
    @Getter
    static class TestEvent extends Event {

        private CommandSender commandSender;

        @Override
        public HandlerList getHandlers() {
            return null;
        }
    }

}