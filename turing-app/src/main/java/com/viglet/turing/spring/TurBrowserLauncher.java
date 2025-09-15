package com.viglet.turing.spring;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurBrowserLauncher {
    @Value("${turing.url:'http://localhost:2700'}")
    private String turingUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        System.setProperty("java.awt.headless", "false");
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(turingUrl));
            } catch (IOException | URISyntaxException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.error("Desktop is not supported, cannot open browser automatically.");
        }
    }
}
