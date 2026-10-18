package com.jobagent;

import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ManualLoginHelper {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("              LINKEDIN MANUAL LOGIN & SESSION EXPORTER HELPER                  ");
        System.out.println("================================================================================");
        System.out.println("Launching Google Chrome for manual login...");

        try (Playwright playwright = Playwright.create()) {
            Path profileDir = Paths.get("./.chrome-profile").toAbsolutePath();
            Files.createDirectories(profileDir);

            BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(false)
                    .setChannel("chrome")
                    .setViewportSize(1920, 1080)
                    .setLocale("en-US")
                    .setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9"))
                    .setArgs(List.of(
                            "--start-maximized",
                            "--lang=en-US",
                            "--no-sandbox"
                    ));

            BrowserContext context;
            try {
                context = playwright.chromium().launchPersistentContext(profileDir, options);
            } catch (Exception e) {
                System.out.println("Could not launch channel 'chrome', falling back to bundled Chromium: " + e.getMessage());
                options.setChannel((String) null);
                context = playwright.chromium().launchPersistentContext(profileDir, options);
            }

            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);

            System.out.println("Navigating to LinkedIn Login page (en-US)...");
            page.navigate("https://www.linkedin.com/login?locale=en_US");
            try { page.bringToFront(); } catch (Exception ignored) {}

            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(">>> Chrome is open! Please log into LinkedIn manually in the browser. <<<");
            System.out.println(">>> (Enter your credentials, solve any CAPTCHA/2FA as needed) <<<");
            System.out.println(">>> Waiting for you to reach your LinkedIn feed... (Up to 10 minutes) <<<");
            System.out.println("--------------------------------------------------------------------------------");

            boolean authenticated = false;
            for (int i = 0; i < 300; i++) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }

                String url = page.url();
                boolean isFeedOrJobs = url.contains("/feed") || url.contains("/jobs") || url.contains("/mynetwork");
                boolean navVisible = false;
                try {
                    navVisible = page.locator(".global-nav, nav#extended-nav, .global-nav__me").first().isVisible();
                } catch (Exception ignored) {}

                if (isFeedOrJobs || navVisible) {
                    authenticated = true;
                    break;
                }

                if (i % 15 == 0 && i > 0) {
                    System.out.println("Still waiting for login... (" + (i * 2) + "s elapsed). Current page: " + url);
                }
            }

            if (authenticated) {
                System.out.println("================================================================================");
                System.out.println("Authentication detected successfully!");

                // Save session state to .session/state.json
                Path sessionPath = Paths.get(".session/state.json").toAbsolutePath();
                Files.createDirectories(sessionPath.getParent());
                context.storageState(new BrowserContext.StorageStateOptions().setPath(sessionPath));

                System.out.println("SUCCESS! Session saved to: " + sessionPath);
                System.out.println("The job search agent can now run fully authenticated with NO login needed!");
                System.out.println("================================================================================");
            } else {
                System.out.println("Timed out waiting for manual login.");
            }

            try {
                Thread.sleep(3000);
            } catch (Exception ignored) {}
            context.close();
        } catch (Exception e) {
            System.err.println("Error during manual login helper: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
