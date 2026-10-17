package com.jobagent.model;

import java.util.ArrayList;
import java.util.List;

public class UserContext {
    private String rawResume;
    private String rawProfile;
    private String rawPreferences;

    private String fullName = "Kashyap Rathod";
    private String email = "kashyaprathod@example.com";
    private String phone = "+919876543210";
    private String location = "Anand, Gujarat, India";
    private String linkedInUrl = "https://www.linkedin.com/in/kashyap-rathod-90620b25a/";

    private List<String> targetRoles = new ArrayList<>();
    private List<String> preferredTechnologies = new ArrayList<>();
    private List<String> preferredLocations = new ArrayList<>();

    private String experienceSummary = "1-3 years experience in Java, Spring Boot, REST APIs, React.js";
    private String noticePeriod = "60 days (negotiable / buyout option available)";
    private String currentCtc = "5.7 LPA";
    private String expectedCtcRule = "Minimum 8 LPA for outside Gujarat (or 1-2 LPA lower than disclosed max). Within Gujarat based on disclosed salary.";
    private boolean willingToRelocate = true;

    // Getters and Setters
    public String getRawResume() {
        return rawResume;
    }

    public void setRawResume(String rawResume) {
        this.rawResume = rawResume;
    }

    public String getRawProfile() {
        return rawProfile;
    }

    public void setRawProfile(String rawProfile) {
        this.rawProfile = rawProfile;
    }

    public String getRawPreferences() {
        return rawPreferences;
    }

    public void setRawPreferences(String rawPreferences) {
        this.rawPreferences = rawPreferences;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    public List<String> getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(List<String> targetRoles) {
        this.targetRoles = targetRoles;
    }

    public List<String> getPreferredTechnologies() {
        return preferredTechnologies;
    }

    public void setPreferredTechnologies(List<String> preferredTechnologies) {
        this.preferredTechnologies = preferredTechnologies;
    }

    public List<String> getPreferredLocations() {
        return preferredLocations;
    }

    public void setPreferredLocations(List<String> preferredLocations) {
        this.preferredLocations = preferredLocations;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public void setExperienceSummary(String experienceSummary) {
        this.experienceSummary = experienceSummary;
    }

    public String getNoticePeriod() {
        return noticePeriod;
    }

    public void setNoticePeriod(String noticePeriod) {
        this.noticePeriod = noticePeriod;
    }

    public String getCurrentCtc() {
        return currentCtc;
    }

    public void setCurrentCtc(String currentCtc) {
        this.currentCtc = currentCtc;
    }

    public String getExpectedCtcRule() {
        return expectedCtcRule;
    }

    public void setExpectedCtcRule(String expectedCtcRule) {
        this.expectedCtcRule = expectedCtcRule;
    }

    public boolean isWillingToRelocate() {
        return willingToRelocate;
    }

    public void setWillingToRelocate(boolean willingToRelocate) {
        this.willingToRelocate = willingToRelocate;
    }
}
