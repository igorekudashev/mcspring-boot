package dev.alangomes.springspigot.util;

import dev.alangomes.BaseTest;
import dev.alangomes.springspigot.util.scheduler.SchedulerService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UtilAspectTest extends BaseTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private Server server;

    @InjectMocks
    private UtilAspect utilAspect;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Test
    public void shouldIgnoreOnMainThread() throws Throwable {
        when(server.isPrimaryThread()).thenReturn(true);

        utilAspect.synchronizeCall(joinPoint);

        verify(joinPoint).proceed();
        verify(schedulerService, never()).scheduleSyncDelayedTask(any(Runnable.class), anyLong());
    }

    @Test
    public void shouldScheduleExecutionToNextTick() throws Throwable {
        when(server.isPrimaryThread()).thenReturn(false);

        utilAspect.synchronizeCall(joinPoint);

        verify(joinPoint, never()).proceed();
        verify(schedulerService).scheduleSyncDelayedTask(runnableCaptor.capture(), eq(0L));

        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

        verify(joinPoint).proceed();
    }

}