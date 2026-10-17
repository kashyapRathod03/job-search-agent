package com.jobagent.model;

public class JobPosting {
    private String id;
    private String title;
    private String company;
    private String location;
    private String jobUrl;
    private boolean easyApply;
    private String description;
    private int matchScore = 0;
    private String matchReason;
    private boolean shouldApply = false;
    private String applicationStatus = "PENDING"; // PENDING, APPLIED, SKIPPED, FAILED

    public JobPosting() {
    }

    public JobPosting(String id, String title, String company, String location, String jobUrl, boolean easyApply) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.jobUrl = jobUrl;
        this.easyApply = easyApply;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public boolean isEasyApply() {
        return easyApply;
    }

    public void setEasyApply(boolean easyApply) {
        this.easyApply = easyApply;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public boolean isShouldApply() {
        return shouldApply;
    }

    public void setShouldApply(boolean shouldApply) {
        this.shouldApply = shouldApply;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s (%s) - EasyApply: %s - Status: %s",
                id, title, company, location, easyApply, applicationStatus);
    }
}
