package com.jobagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgentProperties {

    @Value("${linkedin.username:}")
    private String linkedinUsername;

    @Value("${linkedin.password:}")
    private String linkedinPassword;

    @Value("${linkedin.headless:true}")
    private boolean linkedinHeadless;

    @Value("${linkedin.session.path:./.session/state.json}")
    private String linkedinSessionPath;

    @Value("${agent.delay.min-ms:2000}")
    private long delayMinMs;

    @Value("${agent.delay.max-ms:6000}")
    private long delayMaxMs;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.8-flash}")
    private String geminiModel;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${search.keywords:Java Developer, Spring Boot Developer}")
    private String searchKeywords;

    @Value("${search.locations:Ahmedabad, Pune, Bangalore}")
    private String searchLocations;

    @Value("${search.easy-apply-only:true}")
    private boolean searchEasyApplyOnly;

    @Value("${search.date-posted:past-24h}")
    private String searchDatePosted;

    @Value("${search.max-applications:10}")
    private int searchMaxApplications;

    @Value("${applicant.name:Kashyap Rathod}")
    private String applicantName;

    @Value("${applicant.email:kashyaprathod@example.com}")
    private String applicantEmail;

    @Value("${applicant.phone:+919876543210}")
    private String applicantPhone;

    @Value("${applicant.location:Anand, Gujarat, India}")
    private String applicantLocation;

    // Getters and Setters
    public String getLinkedinUsername() {
        return linkedinUsername;
    }

    public void setLinkedinUsername(String linkedinUsername) {
        this.linkedinUsername = linkedinUsername;
    }

    public String getLinkedinPassword() {
        return linkedinPassword;
    }

    public void setLinkedinPassword(String linkedinPassword) {
        this.linkedinPassword = linkedinPassword;
    }

    public boolean isLinkedinHeadless() {
        return linkedinHeadless;
    }

    public void setLinkedinHeadless(boolean linkedinHeadless) {
        this.linkedinHeadless = linkedinHeadless;
    }

    public String getLinkedinSessionPath() {
        return linkedinSessionPath;
    }

    public void setLinkedinSessionPath(String linkedinSessionPath) {
        this.linkedinSessionPath = linkedinSessionPath;
    }

    public long getDelayMinMs() {
        return delayMinMs;
    }

    public void setDelayMinMs(long delayMinMs) {
        this.delayMinMs = delayMinMs;
    }

    public long getDelayMaxMs() {
        return delayMaxMs;
    }

    public void setDelayMaxMs(long delayMaxMs) {
        this.delayMaxMs = delayMaxMs;
    }

    public String getGeminiApiKey() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiApiKey.trim();
        }
        String envVal = System.getenv("GEMINI_API_KEY");
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        java.io.File dotEnv = new java.io.File(".env");
        if (dotEnv.exists() && dotEnv.isFile()) {
            try {
                for (String line : java.nio.file.Files.readAllLines(dotEnv.toPath())) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("GEMINI_API_KEY=")) {
                        String val = trimmed.substring("GEMINI_API_KEY=".length()).trim();
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (!val.isBlank()) {
                            return val;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public String getGeminiBaseUrl() {
        return geminiBaseUrl;
    }

    public void setGeminiBaseUrl(String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public List<String> getKeywordList() {
        if (searchKeywords == null || searchKeywords.isBlank()) {
            return List.of("Java Developer");
        }
        return Arrays.stream(searchKeywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public String getSearchLocations() {
        return searchLocations;
    }

    public void setSearchLocations(String searchLocations) {
        this.searchLocations = searchLocations;
    }

    public List<String> getLocationList() {
        if (searchLocations == null || searchLocations.isBlank()) {
            return List.of("India");
        }
        return Arrays.stream(searchLocations.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isSearchEasyApplyOnly() {
        return searchEasyApplyOnly;
    }

    public void setSearchEasyApplyOnly(boolean searchEasyApplyOnly) {
        this.searchEasyApplyOnly = searchEasyApplyOnly;
    }

    public String getSearchDatePosted() {
        return searchDatePosted;
    }

    public void setSearchDatePosted(String searchDatePosted) {
        this.searchDatePosted = searchDatePosted;
    }

    public int getSearchMaxApplications() {
        return searchMaxApplications;
    }

    public void setSearchMaxApplications(int searchMaxApplications) {
        this.searchMaxApplications = searchMaxApplications;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getApplicantLocation() {
        return applicantLocation;
    }

    public void setApplicantLocation(String applicantLocation) {
        this.applicantLocation = applicantLocation;
    }
}
