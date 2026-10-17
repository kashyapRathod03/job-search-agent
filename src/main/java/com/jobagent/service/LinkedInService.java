package com.jobagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.config.AgentProperties;
import com.jobagent.model.FormField;
import com.jobagent.model.JobPosting;
import com.jobagent.model.UserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LinkedInService {

    private static final Logger log = LoggerFactory.getLogger(LinkedInService.class);
    private static final String APPLIED_JOBS_FILE = "applied_jobs.json";

    private final Set<String> appliedJobIds = new HashSet<>();
    private final AgentProperties properties;
    private final PlaywrightService playwrightService;
    private final GeminiService geminiService;
    private final EmailService emailService;
    private final KnowledgeBaseService knowledgeBaseService;

    public LinkedInService(AgentProperties properties,
                           PlaywrightService playwrightService,
                           GeminiService geminiService,
                           EmailService emailService,
                           KnowledgeBaseService knowledgeBaseService) {
        this.properties = properties;
        this.playwrightService = playwrightService;
        this.geminiService = geminiService;
        this.emailService = emailService;
        this.knowledgeBaseService = knowledgeBaseService;
        loadAppliedJobs();
    }

    // =========================================================================
    // 1. Applied Jobs Persistence
    // =========================================================================

    private void loadAppliedJobs() {
        try {
            Path path = Paths.get(APPLIED_JOBS_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                List<Map<String, Object>> list = new ObjectMapper().readValue(content, List.class);
                for (Map<String, Object> map : list) {
                    if (map.containsKey("id")) {
                        appliedJobIds.add(String.valueOf(map.get("id")));
                    }
                }
                log.info("Loaded {} previously applied job ID(s) from {}", appliedJobIds.size(), APPLIED_JOBS_FILE);
            }
        } catch (Exception e) {
            log.warn("Could not load {}: {}", APPLIED_JOBS_FILE, e.getMessage());
        }
    }

    private synchronized void recordAppliedJob(JobPosting job) {
        appliedJobIds.add(job.getId());
        try {
            Path path = Paths.get(APPLIED_JOBS_FILE);
            List<Map<String, Object>> list = new ArrayList<>();
            if (Files.exists(path)) {
                try {
                    list = new ObjectMapper().readValue(Files.readString(path), List.class);
                } catch (Exception ignored) {}
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", job.getId());
            entry.put("title", job.getTitle());
            entry.put("company", job.getCompany());
            entry.put("status", job.getApplicationStatus());
            entry.put("appliedAt", LocalDateTime.now().toString());
            list.add(entry);

            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), list);
        } catch (Exception e) {
            log.warn("Failed to write to {}: {}", APPLIED_JOBS_FILE, e.getMessage());
        }
    }

    // =========================================================================
    // 2. Authentication & Session
    // =========================================================================

    public boolean login(Page page) {
        log.info("Checking LinkedIn authentication status...");
        playwrightService.navigateWithDelay("https://www.linkedin.com/feed/?locale=en_US");

        // Allow any immediate server redirect to settle
        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(3000));
        } catch (Exception ignored) {}

        if (isLoggedIn(page)) {
            log.info("Already authenticated via saved session.");
            return true;
        }

        // Check if navigation immediately redirected to checkpoint/challenge
        if (page.url().contains("checkpoint") || page.url().contains("challenge") || is2FAPage(page)) {
            log.info("Security challenge / checkpoint detected: {}", page.url());
            handle2FA(page);
            if (isLoggedIn(page)) {
                log.info("Successfully authenticated after challenge!");
                playwrightService.saveSessionState();
                return true;
            }
            return false;
        }

        // Navigate to login page
        log.info("No active session found. Navigating to LinkedIn login...");
        playwrightService.navigateWithDelay("https://www.linkedin.com/login?locale=en_US");

        if (page.url().contains("checkpoint") || page.url().contains("challenge") || is2FAPage(page)) {
            log.info("Redirected to checkpoint / challenge from login: {}", page.url());
            handle2FA(page);
            if (isLoggedIn(page)) {
                log.info("Successfully authenticated after challenge!");
                playwrightService.saveSessionState();
                return true;
            }
            return false;
        }

        String username = properties.getLinkedinUsername();
        String password = properties.getLinkedinPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.error("LinkedIn credentials missing in configuration.");
            return false;
        }

        log.info("Current landing page URL: {}, Title: {}", page.url(), page.title());
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("login_page.png")));
        } catch (Exception ignored) {}

        // Dismiss cookie banner if present
        try {
            Locator cookieBtn = page.locator("button:has-text('Accept'), button:has-text('Reject'), button[action-type='ACCEPT']").first();
            if (cookieBtn.count() > 0 && cookieBtn.isVisible()) {
                cookieBtn.click();
                playwrightService.sleepShort();
            }
        } catch (Exception ignored) {}

        // If on homepage with a 'Sign in' link, click it
        try {
            Locator navSignIn = page.locator("a.nav__button-secondary:has-text('Sign in'), a[href*='login']").first();
            if (navSignIn.count() > 0 && navSignIn.isVisible()) {
                log.info("Clicking landing page 'Sign in' link...");
                navSignIn.click();
                playwrightService.sleepRandomDelay();
            }
        } catch (Exception ignored) {}

        // Try auto-filling credentials
        try {
            Locator userField = page.getByLabel("Email or phone");
            if (userField.count() == 0 || !userField.first().isVisible()) {
                userField = page.locator("#username:visible, input[name='session_key']:visible, input#session_key:visible, input[aria-label*='Email' i]:visible, input[type='email']:visible").first();
            } else {
                userField = userField.first();
            }

            if (userField.count() > 0 && userField.isVisible()) {
                log.info("Typing username into login form...");
                playwrightService.humanType(userField, username);

                Locator passField = page.getByLabel("Password");
                if (passField.count() == 0 || !passField.first().isVisible()) {
                    passField = page.locator("#password:visible, input[name='session_password']:visible, input#session_password:visible, input[type='password']:visible").first();
                } else {
                    passField = passField.first();
                }

                if (passField.count() > 0 && passField.isVisible()) {
                    log.info("Typing password into login form...");
                    playwrightService.humanType(passField, password);
                    passField.press("Enter");
                    playwrightService.sleepRandomDelay();
                }

                // Click explicit Sign in button if still visible
                Locator signInBtn = page.locator("button:text-is('Sign in'):visible, button.btn__primary--large:visible, button[type='submit']:has-text('Sign in'):visible").first();
                if (signInBtn.count() > 0 && signInBtn.isVisible()) {
                    log.info("Clicking 'Sign in' submit button...");
                    playwrightService.humanClick(signInBtn);
                }
            }
        } catch (Exception e) {
            log.warn("Credential auto-fill notice: {}", e.getMessage());
        }

        // Check if 2FA or security challenge is presented
        if (page.url().contains("checkpoint") || page.url().contains("challenge") || is2FAPage(page)) {
            log.info("Security challenge / 2FA detected. Waiting for resolution...");
            handle2FA(page);
        }

        if (isLoggedIn(page)) {
            log.info("Successfully authenticated with LinkedIn!");
            playwrightService.saveSessionState();
            return true;
        }

        // If not yet authenticated in non-headless mode, wait for user in browser
        if (!properties.isLinkedinHeadless()) {
            log.info(">>> Please complete login in the open browser window. Waiting up to 180s... <<<");
            for (int w = 0; w < 90; w++) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                if (isLoggedIn(page)) {
                    log.info("Successfully authenticated after user login in browser!");
                    playwrightService.saveSessionState();
                    return true;
                }
                if (w % 10 == 0) {
                    log.info("Waiting for authentication in browser... ({}s / 180s elapsed)", w * 2);
                }
            }
        }

        return false;
    }

    private boolean is2FAPage(Page page) {
        try {
            Locator pin = page.locator("#input__email_verification_pin, input[name='pin']").first();
            return pin.count() > 0 && pin.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    private void handle2FA(Page page) {
        log.info(">>> Security verification required. Current URL: {}. Waiting up to 300s for CAPTCHA/2FA completion... <<<", page.url());
        try {
            page.bringToFront();
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("security_checkpoint.png")));
        } catch (Exception ignored) {}
        for (int i = 0; i < 150; i++) {
            if (isLoggedIn(page)) {
                log.info("Authenticated successfully!");
                return;
            }

            // 1. Auto-click reCAPTCHA anchor if found inside iframe
            try {
                com.microsoft.playwright.FrameLocator rFrame = page.frameLocator("iframe[title*='reCAPTCHA' i], iframe[src*='recaptcha' i]").first();
                if (rFrame != null) {
                    Locator cb = rFrame.locator("#recaptcha-anchor, .recaptcha-checkbox").first();
                    if (cb.count() > 0 && cb.isVisible() && !cb.isChecked()) {
                        cb.click();
                    }
                }
            } catch (Exception ignored) {}

            // 2. Select email option if offered
            try {
                Locator emailOpt = page.locator("button:has-text('Send code via email'), button:has-text('Email a code')").first();
                if (emailOpt.count() > 0 && emailOpt.isVisible()) {
                    emailOpt.click();
                }
            } catch (Exception ignored) {}

            // 3. Check for 2FA PIN from 2fa_code.txt
            String pin = emailService.checkPinNow();
            if (pin != null && !pin.isBlank()) {
                try {
                    Locator pinInput = page.locator("#input__email_verification_pin, input[name='pin']").first();
                    if (pinInput.count() > 0 && pinInput.isVisible()) {
                        log.info("Entering 2FA PIN: {}", pin);
                        playwrightService.humanType(pinInput, pin);
                        Locator submit = page.locator("#email-pin-submit-button, button[type='submit']").first();
                        if (submit.count() > 0 && submit.isVisible()) {
                            submit.click();
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (i % 5 == 0) {
                log.info("Waiting for CAPTCHA/2FA verification in browser or 2fa_code.txt... ({}s / 300s elapsed)", i * 2);
            }

            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }
    }

    private boolean isLoggedIn(Page page) {
        try {
            String url = page.url();
            if (url.contains("/feed") || url.contains("/jobs") || url.contains("/mynetwork")) {
                return true;
            }
            Locator nav = page.locator(".global-nav, nav#extended-nav, .global-nav__me").first();
            return nav.count() > 0 && nav.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // 3. Job Search
    // =========================================================================

    public List<JobPosting> searchJobs(Page page, String keyword, String location, UserContext ctx) {
        List<JobPosting> jobs = new ArrayList<>();
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

            String searchUrl = String.format(
                    "https://www.linkedin.com/jobs/search/?keywords=%s&location=%s&f_AL=true&f_TPR=r604800&f_JT=F&locale=en_US",
                    encodedKeyword, encodedLocation
            );

            log.info("Searching LinkedIn: {}", searchUrl);
            playwrightService.navigateWithDelay(searchUrl);

            try {
                page.waitForSelector("ul.scaffold-layout__list-container > li, .jobs-search-results-list li, .job-card-container",
                        new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception ignored) {}

            Locator cards = page.locator("ul.scaffold-layout__list-container > li, .jobs-search-results-list li, .job-card-container");
            int total = cards.count();
            log.info("Found {} job cards on search page.", total);

            for (int i = 0; i < total; i++) {
                try {
                    Locator card = cards.nth(i);
                    String jobId = card.getAttribute("data-occludable-job-id");

                    Locator link = card.locator("a.job-card-list__title--link, a.job-card-container__link, a[href*='/jobs/view/']").first();
                    if (link.count() == 0) continue;

                    String jobUrl = link.getAttribute("href");
                    if (jobId == null || jobId.isBlank()) {
                        jobId = extractJobId(jobUrl);
                    }
                    if (jobId == null || appliedJobIds.contains(jobId)) {
                        continue;
                    }

                    String title = link.innerText().trim();
                    Locator comp = card.locator(".artdeco-entity-lockup__subtitle, .job-card-container__primary-description").first();
                    String company = comp.count() > 0 ? comp.innerText().trim() : "Unknown Company";

                    JobPosting job = new JobPosting(jobId, title, company, location, jobUrl, true);
                    jobs.add(job);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("Error during job search: {}", e.getMessage());
        }
        return jobs;
    }

    private String extractJobId(String url) {
        if (url == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/jobs/view/([0-9]+)").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    // =========================================================================
    // 4. Job Application Wizard
    // =========================================================================

    public void processAndApply(Page page, JobPosting job, UserContext ctx) {
        log.info("--------------------------------------------------------------------------------");
        log.info("Processing Job: {} @ {} (ID: {})", job.getTitle(), job.getCompany(), job.getId());
        log.info("--------------------------------------------------------------------------------");

        try {
            // Navigate to job view
            String viewUrl = "https://www.linkedin.com/jobs/view/" + job.getId() + "/?locale=en_US";
            playwrightService.navigateWithDelay(viewUrl);

            // Find visible Easy Apply button
            Locator applyBtn = page.locator("button.jobs-apply-button:has-text('Easy Apply'), button:has-text('Easy Apply')");
            Locator visibleBtn = null;
            for (int i = 0; i < applyBtn.count(); i++) {
                if (applyBtn.nth(i).isVisible()) {
                    visibleBtn = applyBtn.nth(i);
                    break;
                }
            }

            if (visibleBtn == null) {
                log.info("No active 'Easy Apply' button found for job {}. Skipping.", job.getId());
                job.setApplicationStatus("SKIPPED");
                return;
            }

            log.info("Clicking 'Easy Apply' button...");
            playwrightService.humanClick(visibleBtn);

            // Run Form Wizard
            boolean applied = runApplyWizard(page, job, ctx);
            if (applied) {
                job.setApplicationStatus("APPLIED");
                recordAppliedJob(job);
                log.info("SUCCESS: Applied to {} @ {}", job.getTitle(), job.getCompany());
            } else {
                job.setApplicationStatus("FAILED");
                dismissModal(page);
            }
        } catch (Exception e) {
            log.error("Failed to apply for job {}: {}", job.getId(), e.getMessage());
            job.setApplicationStatus("FAILED");
            dismissModal(page);
        }
    }

    private boolean runApplyWizard(Page page, JobPosting job, UserContext ctx) {
        int maxSteps = 10;

        for (int step = 1; step <= maxSteps; step++) {
            playwrightService.sleepShort();
            log.info("Application wizard step {} for job: {}", step, job.getId());

            // 1. Check for Submit Application button
            Locator submitBtn = page.locator("button:has-text('Submit application'), button[aria-label*='Submit application' i]").first();
            if (submitBtn.count() > 0 && submitBtn.isVisible()) {
                log.info("Submit application button found on step {}.", step);

                // Uncheck follow company checkbox if present
                try {
                    Locator followBox = page.locator("input[id*='follow-company'], label:has-text('Follow')").first();
                    if (followBox.count() > 0 && followBox.isChecked()) {
                        followBox.uncheck();
                    }
                } catch (Exception ignored) {}

                playwrightService.humanClick(submitBtn);
                playwrightService.sleepRandomDelay();

                // Close post-apply confirmation dialog
                closeConfirmationModal(page);
                return true;
            }

            // 2. Scan and answer all form fields on current step in ONE batch
            fillCurrentFormStep(page, job, ctx);

            // 3. Advance to next step (Next or Review)
            Locator nextBtn = page.locator("button:has-text('Next'), button[aria-label*='Continue to next step' i]").first();
            Locator reviewBtn = page.locator("button:has-text('Review'), button[aria-label*='Review your application' i]").first();

            if (nextBtn.count() > 0 && nextBtn.isVisible()) {
                log.info("Clicking 'Next' button...");
                playwrightService.humanClick(nextBtn);
            } else if (reviewBtn.count() > 0 && reviewBtn.isVisible()) {
                log.info("Clicking 'Review' button...");
                playwrightService.humanClick(reviewBtn);
            } else {
                log.warn("Neither Next, Review, nor Submit button found on step {}.", step);
                return false;
            }

            // Check if blocked by validation error
            try {
                Locator err = page.locator(".artdeco-inline-feedback--error").first();
                if (err.count() > 0 && err.isVisible()) {
                    log.warn("Step {} validation error: {}", step, err.innerText());
                    return false;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    private void fillCurrentFormStep(Page page, JobPosting job, UserContext ctx) {
        Locator modal = page.locator("div.jobs-easy-apply-modal, [role='dialog']").first();
        if (modal.count() == 0) return;

        List<FormFieldHolder> stepFields = new ArrayList<>();

        // 1. Collect Text / Number / Textarea inputs
        Locator textInputs = modal.locator("input[type='text'], input[type='number'], input:not([type]), textarea");
        for (int i = 0; i < textInputs.count(); i++) {
            Locator field = textInputs.nth(i);
            if (!field.isVisible()) continue;
            String label = extractLabel(field);
            stepFields.add(new FormFieldHolder(new FormField(label, "TEXT", "", true), field));
        }

        // 2. Collect Dropdown / Select elements
        Locator selects = modal.locator("select");
        for (int i = 0; i < selects.count(); i++) {
            Locator field = selects.nth(i);
            if (!field.isVisible()) continue;
            String label = extractLabel(field);
            List<String> options = field.locator("option").allInnerTexts();
            FormField ff = new FormField(label, "SELECT", "", true);
            ff.setOptions(options);
            stepFields.add(new FormFieldHolder(ff, field));
        }

        // 3. Collect Radio button groups
        Locator radioGroups = modal.locator("fieldset[data-test-form-builder-radio-button-form-component='true'], fieldset");
        for (int i = 0; i < radioGroups.count(); i++) {
            Locator group = radioGroups.nth(i);
            if (!group.isVisible()) continue;
            Locator legend = group.locator("legend, span.t-14").first();
            String label = legend.count() > 0 ? legend.innerText().trim() : "Radio Question";
            List<String> options = group.locator("label").allInnerTexts();
            FormField ff = new FormField(label, "RADIO", "", true);
            ff.setOptions(options);
            stepFields.add(new FormFieldHolder(ff, group));
        }

        // 4. Collect Checkboxes
        Locator checkboxes = modal.locator("input[type='checkbox']");
        for (int i = 0; i < checkboxes.count(); i++) {
            Locator cb = checkboxes.nth(i);
            if (!cb.isVisible()) continue;
            String label = extractLabel(cb);
            FormField ff = new FormField(label, "CHECKBOX", "", false);
            stepFields.add(new FormFieldHolder(ff, cb));
        }

        // 5. Handle File Upload (Resume)
        Locator fileInputs = modal.locator("input[type='file']");
        for (int i = 0; i < fileInputs.count(); i++) {
            try {
                Locator fi = fileInputs.nth(i);
                Path resumePath = Paths.get("Kashyap_Rathod_SDE_Resume.pdf").toAbsolutePath();
                if (Files.exists(resumePath)) {
                    fi.setInputFiles(resumePath);
                    log.info("Attached resume: {}", resumePath);
                }
            } catch (Exception ignored) {}
        }

        // 6. Resolve answers (KnowledgeBase first, then batch Gemini)
        List<FormField> missingForGemini = new ArrayList<>();
        for (FormFieldHolder holder : stepFields) {
            String ans = knowledgeBaseService.lookupAnswer(holder.formField.getLabel(), holder.formField.getOptions());
            if (ans != null && !ans.isBlank()) {
                holder.resolvedAnswer = ans;
            } else {
                missingForGemini.add(holder.formField);
            }
        }

        // Single batch call to Gemini for all missing fields
        if (!missingForGemini.isEmpty()) {
            log.info("Requesting Gemini batch answer for {} missing question(s)...", missingForGemini.size());
            Map<String, String> geminiAnswers = geminiService.answerQuestionsBatch(missingForGemini, ctx, job);
            for (FormFieldHolder holder : stepFields) {
                if (holder.resolvedAnswer == null && geminiAnswers.containsKey(holder.formField.getLabel())) {
                    holder.resolvedAnswer = geminiAnswers.get(holder.formField.getLabel());
                    knowledgeBaseService.saveAnswer(holder.formField.getLabel(), holder.resolvedAnswer);
                }
            }
        }

        // 7. Fill values into form elements
        for (FormFieldHolder holder : stepFields) {
            String value = holder.resolvedAnswer;
            if (value == null || value.isBlank()) continue;

            try {
                switch (holder.formField.getFieldType()) {
                    case "TEXT":
                        holder.locator.click();
                        holder.locator.fill("");
                        holder.locator.fill(value);
                        holder.locator.press("Tab"); // triggers blur/validation
                        break;

                    case "SELECT":
                        String matching = matchDropdownOption(holder.locator, value);
                        if (matching != null) {
                            holder.locator.selectOption(matching);
                        }
                        break;

                    case "RADIO":
                        Locator radioToClick = holder.locator.locator("label:has-text('" + value + "'), input[value='" + value + "']").first();
                        if (radioToClick.count() > 0) {
                            radioToClick.click();
                        } else {
                            // Fallback to first radio
                            holder.locator.locator("input[type='radio'], label").first().click();
                        }
                        break;

                    case "CHECKBOX":
                        if ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || holder.formField.isRequired()) {
                            if (!holder.locator.isChecked()) holder.locator.check();
                        }
                        break;
                }
            } catch (Exception e) {
                log.debug("Error filling field '{}': {}", holder.formField.getLabel(), e.getMessage());
            }
        }
    }

    private String matchDropdownOption(Locator selectLocator, String targetValue) {
        try {
            List<String> options = selectLocator.locator("option").allInnerTexts();
            String targetLower = targetValue.toLowerCase().trim();

            for (String opt : options) {
                String optLower = opt.toLowerCase().trim();
                if (optLower.equals(targetLower)) return opt;
            }
            for (String opt : options) {
                String optLower = opt.toLowerCase().trim();
                if (optLower.contains(targetLower) || targetLower.contains(optLower)) return opt;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractLabel(Locator field) {
        try {
            String id = field.getAttribute("id");
            if (id != null && !id.isBlank()) {
                Locator label = field.page().locator("label[for='" + id + "']").first();
                if (label.count() > 0 && !label.innerText().isBlank()) return label.innerText().trim();
            }
            String ariaLabel = field.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.isBlank()) return ariaLabel.trim();

            Locator parent = field.locator("xpath=ancestor::*[contains(@class, 'fb-dash-form-element') or contains(@class, 'jobs-easy-apply-form-section') or self::fieldset or self::label]").first();
            if (parent.count() > 0) {
                Locator lbl = parent.locator("label, legend, span.t-14").first();
                if (lbl.count() > 0 && !lbl.innerText().isBlank()) return lbl.innerText().trim();
            }
        } catch (Exception ignored) {}
        return "Application Field";
    }

    private void closeConfirmationModal(Page page) {
        try {
            Locator doneBtn = page.locator("button:has-text('Done'), button[aria-label*='Dismiss' i]").first();
            if (doneBtn.count() > 0 && doneBtn.isVisible()) {
                playwrightService.humanClick(doneBtn);
            }
        } catch (Exception ignored) {}
    }

    private void dismissModal(Page page) {
        try {
            Locator dismissBtn = page.locator("button[aria-label*='Dismiss' i], button.artdeco-modal__dismiss").first();
            if (dismissBtn.count() > 0 && dismissBtn.isVisible()) {
                dismissBtn.click();
                playwrightService.sleepShort();
                Locator discardBtn = page.locator("button:has-text('Discard'), button[data-control-name='discard_application_confirm_btn']").first();
                if (discardBtn.count() > 0 && discardBtn.isVisible()) {
                    discardBtn.click();
                }
            }
        } catch (Exception ignored) {}
    }

    private static class FormFieldHolder {
        final FormField formField;
        final Locator locator;
        String resolvedAnswer;

        FormFieldHolder(FormField formField, Locator locator) {
            this.formField = formField;
            this.locator = locator;
        }
    }
}
