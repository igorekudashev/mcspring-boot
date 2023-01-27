package dev.alangomes.springspigot.context;

/**
 * Some beans for unknown reason cannot be configured
 * on spring context initialization (it's just stuck).
 * For bypass that, each bean which will implement this interface
 * can be initialized after context initialization
 * with custom <b>init</b> method.
 */
@FunctionalInterface
public interface AfterContextInit {

    void init();
}
