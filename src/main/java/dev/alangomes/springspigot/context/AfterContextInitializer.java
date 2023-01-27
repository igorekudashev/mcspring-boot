package dev.alangomes.springspigot.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AfterContextInitializer {

    private final List<AfterContextInit> beansForInit;
    private boolean isInitialized = false;

    public void initializeAll() {
        if (!isInitialized) {
            beansForInit.forEach(AfterContextInit::init);
            isInitialized = true;
        }
    }
}
