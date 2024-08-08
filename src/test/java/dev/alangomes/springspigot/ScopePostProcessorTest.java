package dev.alangomes.springspigot;

import dev.alangomes.BaseTest;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

public class ScopePostProcessorTest extends BaseTest {

    @Mock
    private ConfigurableListableBeanFactory factory;

    @Mock
    private BeanDefinition bean1, bean2, bean3;

    @InjectMocks
    private ScopePostProcessor scopePostProcessor;

    @BeforeEach
    public void setup() {
        when(factory.getBeanDefinitionNames()).thenReturn(new String[]{"bean1", "bean2", "bean3"});

        when(factory.getBeanDefinition("bean1")).thenReturn(bean1);
        when(factory.getBeanDefinition("bean2")).thenReturn(bean2);
        when(factory.getBeanDefinition("bean3")).thenReturn(bean3);

        when(factory.getType("bean1")).thenReturn((Class) Listener.class);
        when(factory.getType("bean2")).thenReturn((Class) Server.class);
        when(factory.getType("bean3")).thenReturn(null);
    }

    @Test
    public void shouldSetSingletonScopeToListeners() {
        scopePostProcessor.postProcessBeanFactory(factory);

        verify(bean1).setScope(SCOPE_SINGLETON);
        verify(bean2, never()).setScope(any());
        verify(bean3, never()).setScope(any());
    }

}