package com.jobagent;

import com.jobagent.config.AgentProperties;
import com.jobagent.model.JobPosting;
import com.jobagent.model.UserContext;
import com.jobagent.service.ContextLoaderService;
import com.jobagent.service.KnowledgeBaseService;
import com.jobagent.service.LinkedInService;
import com.jobagent.service.PlaywrightService;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class JobSearchAgentApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(JobSearchAgentApplication.class);

    private final AgentProperties properties;
    private final ContextLoaderService contextLoaderService;
    private final PlaywrightService playwrightService;
    private final LinkedInService linkedinService;
    private final KnowledgeBaseService knowledgeBaseService;

    public JobSearchAgentApplication(AgentProperties properties,
                                     ContextLoaderService contextLoaderService,
                                     PlaywrightService playwrightService,
                                     LinkedInService linkedinService,
                                     KnowledgeBaseService knowledgeBaseService) {
        this.properties = properties;
        this.contextLoaderService = contextLoaderService;
        this.playwrightService = playwrightService;
        this.linkedinService = linkedinService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public static void main(String[] args) {
        SpringApplication.run(JobSearchAgentApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("================================================================================");
        log.info("      STARTING LINKEDIN HEADLESS JOB APPLICATION AGENT (SPRING BOOT CLI)        ");
        log.info("================================================================================");

        // 1. Context Loading
        log.info("Step 1: Ingesting candidate background & preferences...");
        UserContext ctx = contextLoaderService.loadContext();
        knowledgeBaseService.seedDefaultContext(ctx);
        log.info("Candidate: {} | Notice: {} | Current CTC: {}",
                ctx.getFullName(), ctx.getNoticePeriod(), ctx.getCurrentCtc());

        // 2. Playwright Initialization
        log.info("Step 2: Launching headless Playwright browser...");
        Page page = null;
        try {
            page = playwrightService.getOrCreatePage();

            // 3. Authentication
            log.info("Step 3: Authenticating with LinkedIn...");
            boolean loggedIn = linkedinService.login(page);

            if (!loggedIn) {
                log.warn("================================================================================");
                log.warn("LinkedIn login could not be completed automatically.");
                log.warn("1. Verify LINKEDIN_USERNAME and LINKEDIN_PASSWORD in application.properties or environment.");
                log.warn("2. If 2FA or CAPTCHA is required, you can temporarily set linkedin.headless=false");
                log.warn("   to perform the one-time interactive login. Session cookies will be saved to .session/");
                log.warn("================================================================================");
                return;
            }

            // 4. Job Discovery & Application Loop
            List<JobPosting> processedJobs = new ArrayList<>();
            List<String> keywords = ctx.getTargetRoles().isEmpty() ? properties.getKeywordList() : ctx.getTargetRoles();
            List<String> locations = ctx.getPreferredLocations().isEmpty() ? properties.getLocationList() : ctx.getPreferredLocations();

            log.info("Step 4: Beginning Job Discovery across {} keywords and {} locations...",
                    keywords.size(), locations.size());

            int targetApplications = properties.getSearchMaxApplications();
            int maxEvaluatedJobs = 200;

            outerLoop:
            for (String keyword : keywords) {
                for (String location : locations) {
                    long appliedCount = processedJobs.stream().filter(j -> "APPLIED".equalsIgnoreCase(j.getApplicationStatus())).count();
                    if (appliedCount >= targetApplications || processedJobs.size() >= maxEvaluatedJobs) {
                        log.info("Reached application quota ({}/{} applied). Concluding search.", appliedCount, targetApplications);
                        break outerLoop;
                    }

                    log.info("Searching for '{}' in '{}'...", keyword, location);
                    List<JobPosting> batch = linkedinService.searchJobs(page, keyword, location, ctx);

                    for (JobPosting job : batch) {
                        appliedCount = processedJobs.stream().filter(j -> "APPLIED".equalsIgnoreCase(j.getApplicationStatus())).count();
                        if (appliedCount >= targetApplications || processedJobs.size() >= maxEvaluatedJobs) {
                            log.info("Successfully reached target of {} applied jobs!", targetApplications);
                            break outerLoop;
                        }

                        linkedinService.processAndApply(page, job, ctx);
                        processedJobs.add(job);
                        playwrightService.sleepRandomDelay();
                    }
                    // Anti-bot pause between search queries
                    playwrightService.sleepRandomDelay();
                }
            }

            // 5. Execution Summary
            printExecutionSummary(processedJobs);

        } catch (Exception e) {
            log.error("Fatal error during agent execution: {}", e.getMessage(), e);
        } finally {
            log.info("Cleaning up browser context...");
            playwrightService.cleanup();
            log.info("Agent process completed.");
        }
    }

    private void printExecutionSummary(List<JobPosting> jobs) {
        log.info("================================================================================");
        log.info("                            APPLICATION SUMMARY REPORT                          ");
        log.info("================================================================================");
        log.info("Total Jobs Processed: {}", jobs.size());

        long appliedCount = jobs.stream().filter(j -> "APPLIED".equalsIgnoreCase(j.getApplicationStatus())).count();
        long skippedCount = jobs.stream().filter(j -> "SKIPPED".equalsIgnoreCase(j.getApplicationStatus())).count();
        long failedCount = jobs.stream().filter(j -> "FAILED".equalsIgnoreCase(j.getApplicationStatus())).count();

        log.info("Successfully Applied: {}", appliedCount);
        log.info("Skipped (Low Fit / Excluded): {}", skippedCount);
        log.info("Failed / Incomplete: {}", failedCount);

        for (JobPosting j : jobs) {
            log.info(" - [{}] {} @ {} (Status: {})", j.getId(), j.getTitle(), j.getCompany(), j.getApplicationStatus());
        }
        log.info("================================================================================");
    }
}
