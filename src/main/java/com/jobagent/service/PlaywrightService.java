package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.config.AgentProperties;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PlaywrightService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightService.class);

    private final AgentProperties properties;
    private final Random random = new Random();

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private boolean isCdpConnection = false;

    public PlaywrightService(AgentProperties properties) {
        this.properties = properties;
    }

    public synchronized Page getOrCreatePage() {
        if (page != null && !page.isClosed()) {
            return page;
        }

        if (playwright == null) {
            playwright = Playwright.create();
        }

        // 1. Check if user opened Chrome manually with --remote-debugging-port=9222
        try {
            log.info("Checking for manually opened Chrome instance on http://localhost:9222...");
            browser = playwright.chromium().connectOverCDP("http://localhost:9222");
            isCdpConnection = true;
            log.info("Successfully connected to manually opened Chrome via CDP (port 9222)!");

            List<BrowserContext> contexts = browser.contexts();
            context = contexts.isEmpty() ? browser.newContext() : contexts.get(0);

            // Find existing LinkedIn tab or use active tab
            for (Page p : context.pages()) {
                if (p.url().contains("linkedin.com")) {
                    page = p;
                    log.info("Attached to existing LinkedIn tab in manually opened Chrome: {}", p.url());
                    break;
                }
            }
            if (page == null) {
                page = context.newPage();
            }
            try { page.bringToFront(); } catch (Exception ignored) {}

            saveSessionState();
            return page;
        } catch (Exception cdpEx) {
            log.info("No Chrome instance detected on port 9222 ({}). Launching browser...", cdpEx.getMessage());
            isCdpConnection = false;
        }

        // 2. Launch persistent Chrome instance
        log.info("Launching installed Google Chrome browser (channel=chrome, headless={})...", properties.isLinkedinHeadless());
        Path profileDir = Paths.get("./.chrome-profile").toAbsolutePath();
        try {
            Files.createDirectories(profileDir);
        } catch (Exception ignored) {}

        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(properties.isLinkedinHeadless())
                .setChannel("chrome") // Uses installed Google Chrome
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

        // Load cookies from .session folder (cookies.json, state.json, cookies.txt, li_at.txt)
        loadCookiesFromSessionFolder(context);

        // Inject stealth scripts
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");
        context.addInitScript("window.chrome = { runtime: {} };");
        context.addInitScript("Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});");
        context.addInitScript("Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});");

        page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        try { page.bringToFront(); } catch (Exception ignored) {}
        return page;
    }

    private void loadCookiesFromSessionFolder(BrowserContext ctx) {
        Path stateJson = Paths.get(properties.getLinkedinSessionPath());
        Path cookiesJson = Paths.get(".session/cookies.json");
        Path cookiesTxt = Paths.get(".session/cookies.txt");
        Path liAtTxt = Paths.get(".session/li_at.txt");

        ObjectMapper mapper = new ObjectMapper();

        // 1. .session/cookies.json (Cookie-Editor format or JSON array)
        if (Files.exists(cookiesJson)) {
            try {
                JsonNode root = mapper.readTree(cookiesJson.toFile());
                JsonNode array = root.isArray() ? root : root.get("cookies");
                if (array != null && array.isArray()) {
                    List<Cookie> cookies = parseJsonCookies(array);
                    if (!cookies.isEmpty()) {
                        ctx.addCookies(cookies);
                        log.info("Loaded {} cookies from .session/cookies.json", cookies.size());
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed reading .session/cookies.json: {}", e.getMessage());
            }
        }

        // 2. .session/state.json (Playwright storage state format)
        if (Files.exists(stateJson)) {
            try {
                JsonNode root = mapper.readTree(stateJson.toFile());
                JsonNode array = root.get("cookies");
                if (array != null && array.isArray()) {
                    List<Cookie> cookies = parseJsonCookies(array);
                    if (!cookies.isEmpty()) {
                        ctx.addCookies(cookies);
                        log.info("Loaded {} cookies from saved state {}", cookies.size(), stateJson);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed reading {}: {}", stateJson, e.getMessage());
            }
        }

        // 3. .session/cookies.txt (Raw header or key=value string)
        if (Files.exists(cookiesTxt)) {
            try {
                String content = Files.readString(cookiesTxt).trim();
                List<Cookie> cookies = parseRawCookieString(content);
                if (!cookies.isEmpty()) {
                    ctx.addCookies(cookies);
                    log.info("Loaded {} cookies from .session/cookies.txt", cookies.size());
                    return;
                }
            } catch (Exception e) {
                log.warn("Failed reading .session/cookies.txt: {}", e.getMessage());
            }
        }

        // 4. .session/li_at.txt
        if (Files.exists(liAtTxt)) {
            try {
                String content = Files.readString(liAtTxt).trim();
                List<Cookie> cookies = parseRawCookieString(content);
                if (!cookies.isEmpty()) {
                    ctx.addCookies(cookies);
                    log.info("Loaded {} cookie(s) from .session/li_at.txt", cookies.size());
                }
            } catch (Exception e) {
                log.warn("Failed reading .session/li_at.txt: {}", e.getMessage());
            }
        }
    }

    private List<Cookie> parseJsonCookies(JsonNode array) {
        List<Cookie> result = new ArrayList<>();
        for (JsonNode node : array) {
            String name = node.has("name") ? node.get("name").asText() : null;
            String value = node.has("value") ? node.get("value").asText() : null;
            if (name == null || value == null || name.isBlank()) continue;

            String domain = node.has("domain") ? node.get("domain").asText() : ".linkedin.com";
            String path = node.has("path") ? node.get("path").asText() : "/";

            Cookie c = new Cookie(name, value)
                    .setDomain(domain)
                    .setPath(path.isBlank() ? "/" : path);

            if (node.has("secure") && node.get("secure").asBoolean()) {
                c.setSecure(true);
            }
            if (node.has("httpOnly") && node.get("httpOnly").asBoolean()) {
                c.setHttpOnly(true);
            }
            result.add(c);
        }
        return result;
    }

    private List<Cookie> parseRawCookieString(String raw) {
        List<Cookie> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;

        if (raw.toLowerCase().startsWith("cookie:")) {
            raw = raw.substring(raw.indexOf(':') + 1).trim();
        }

        if (raw.contains("=") && (raw.contains(";") || raw.contains("\n"))) {
            String[] pairs = raw.split("[;\n]");
            for (String pair : pairs) {
                String trimmed = pair.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    String name = trimmed.substring(0, eq).trim();
                    String val = trimmed.substring(eq + 1).trim();
                    result.add(new Cookie(name, val).setDomain(".linkedin.com").setPath("/").setSecure(true));
                    result.add(new Cookie(name, val).setDomain(".www.linkedin.com").setPath("/").setSecure(true));
                }
            }
        } else {
            String token = raw.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .findFirst()
                    .orElse("");
            if (!token.isBlank()) {
                result.add(new Cookie("li_at", token).setDomain(".linkedin.com").setPath("/").setSecure(true).setHttpOnly(true));
                result.add(new Cookie("li_at", token).setDomain(".www.linkedin.com").setPath("/").setSecure(true).setHttpOnly(true));
            }
        }
        return result;
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

    public void clearCookies() {
        try {
            if (context != null) {
                context.clearCookies();
                log.info("Cleared browser cookies.");
            }
        } catch (Exception e) {
            log.warn("Failed to clear cookies: {}", e.getMessage());
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
            com.microsoft.playwright.Response resp = p.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            if (resp != null) {
                log.info("HTTP Status {} for {}", resp.status(), url);
            }
        } catch (PlaywrightException pe) {
            if (pe.getMessage() != null && pe.getMessage().contains("ERR_ABORTED")) {
                log.info("Navigation to {} aborted (in-flight redirect to: {}).", url, p.url());
            } else if (pe.getMessage() != null && pe.getMessage().contains("ERR_HTTP_RESPONSE_CODE_FAILURE")) {
                log.warn("HTTP response code failure navigating to {}: {}. Backing off...", url, pe.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            } else {
                log.warn("Navigation notice for {}: {}", url, pe.getMessage());
            }
        }
        sleepRandomDelay();
    }

    @PreDestroy
    public synchronized void cleanup() {
        try {
            if (!isCdpConnection) {
                if (page != null && !page.isClosed()) {
                    page.close();
                }
                if (context != null) {
                    context.close();
                }
                if (browser != null && browser.isConnected()) {
                    browser.close();
                }
            } else {
                log.info("Detached from manually opened Chrome (keeping your browser window open).");
            }
            page = null;
            context = null;
            browser = null;

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
