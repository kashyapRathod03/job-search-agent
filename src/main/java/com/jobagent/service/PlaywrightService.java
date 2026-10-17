package com.jobagent.service;

import com.jobagent.config.AgentProperties;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class PlaywrightService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightService.class);

    private final AgentProperties properties;
    private final Random random = new Random();

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public PlaywrightService(AgentProperties properties) {
        this.properties = properties;
    }

    public synchronized Page getOrCreatePage() {
        if (page != null && !page.isClosed()) {
            return page;
        }

        log.info("Launching installed Google Chrome browser (channel=chrome, headless={})...", properties.isLinkedinHeadless());
        playwright = Playwright.create();

        Path profileDir = Paths.get("./.chrome-profile").toAbsolutePath();
        try {
            Files.createDirectories(profileDir);
        } catch (Exception ignored) {}

        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(properties.isLinkedinHeadless())
                .setChannel("chrome") // Uses installed Google Chrome
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
                .setLocale("en-US")
                .setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9"))
                .setTimezoneId("Asia/Kolkata")
                .setIgnoreDefaultArgs(List.of("--enable-automation"))
                .setArgs(List.of(
                        "--disable-blink-features=AutomationControlled",
                        "--no-sandbox",
                        "--disable-infobars",
                        "--disable-dev-shm-usage",
                        "--start-maximized",
                        "--lang=en-US"
                ));

        try {
            context = playwright.chromium().launchPersistentContext(profileDir, options);
        } catch (Exception e) {
            log.warn("Failed to launch Chrome with channel 'chrome': {}. Falling back to default Chromium...", e.getMessage());
            options.setChannel((String) null);
            context = playwright.chromium().launchPersistentContext(profileDir, options);
        }

        // Force LinkedIn English language cookie
        try {
            context.addCookies(List.of(
                    new com.microsoft.playwright.options.Cookie("lang", "v=2&lang=en-us")
                            .setDomain(".linkedin.com")
                            .setPath("/")
            ));
        } catch (Exception ignored) {}

        // Inject stealth and language scripts
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");
        context.addInitScript("window.chrome = { runtime: {} };");
        context.addInitScript("Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});");
        context.addInitScript("Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});");
        context.addInitScript("document.cookie = 'lang=\"v=2&lang=en-us\"; domain=.linkedin.com; path=/; max-age=31536000';");

        page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        try { page.bringToFront(); } catch (Exception ignored) {}
        return page;
    }

    public void saveSessionState() {
        try {
            if (context != null) {
                Path sessionPath = Paths.get(properties.getLinkedinSessionPath());
                Files.createDirectories(sessionPath.getParent());
                context.storageState(new BrowserContext.StorageStateOptions().setPath(sessionPath));
                log.info("Session state saved successfully to {}", sessionPath);
            }
        } catch (Exception e) {
            log.warn("Failed to save session state: {}", e.getMessage());
        }
    }

    public void sleepRandomDelay() {
        long min = properties.getDelayMinMs();
        long max = properties.getDelayMaxMs();
        long delay = min + (long) (random.nextDouble() * (max - min));
        try {
            log.debug("Anti-bot sleep for {} ms...", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sleepShort() {
        try {
            Thread.sleep(1000 + random.nextInt(1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void humanType(Locator locator, String text) {
        locator.click();
        sleepShort();
        locator.fill(""); // clear existing text
        for (char c : text.toCharArray()) {
            locator.pressSequentially(String.valueOf(c));
            try {
                Thread.sleep(40 + random.nextInt(110)); // realistic typing jitter
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        sleepShort();
    }

    public void humanClick(Locator locator) {
        locator.scrollIntoViewIfNeeded();
        sleepShort();
        locator.click();
        sleepRandomDelay();
    }

    public void navigateWithDelay(String url) {
        Page p = getOrCreatePage();
        log.info("Navigating to: {}", url);
        try {
            p.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        } catch (PlaywrightException pe) {
            if (pe.getMessage() != null && pe.getMessage().contains("ERR_ABORTED")) {
                log.info("Navigation to {} aborted (in-flight redirect to: {}).", url, p.url());
            } else {
                throw pe;
            }
        }
        sleepRandomDelay();
    }

    @PreDestroy
    public synchronized void cleanup() {
        try {
            if (page != null && !page.isClosed()) {
                page.close();
            }
            page = null;

            if (context != null) {
                context.close();
                context = null;
            }

            if (browser != null && browser.isConnected()) {
                browser.close();
                browser = null;
            }

            if (playwright != null) {
                playwright.close();
                playwright = null;
            }
            log.info("Playwright browser context cleanly shut down.");
        } catch (Exception e) {
            log.debug("Playwright shutdown notice: {}", e.getMessage());
        }
    }
}
