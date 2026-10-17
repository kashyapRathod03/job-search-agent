package com.jobagent.service;

import com.jobagent.config.AgentProperties;
import com.jobagent.model.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContextLoaderService {

    private static final Logger log = LoggerFactory.getLogger(ContextLoaderService.class);

    private final AgentProperties properties;

    public ContextLoaderService(AgentProperties properties) {
        this.properties = properties;
    }

    public UserContext loadContext() {
        UserContext ctx = new UserContext();

        // 1. Read files
        String preferencesContent = readFileIfExists("preferences.md");
        String profileContent = readFileIfExists("profile.md");
        String resumeContent = readFileIfExists("resume.md");

        ctx.setRawPreferences(preferencesContent);
        ctx.setRawProfile(profileContent);
        ctx.setRawResume(resumeContent);

        // 2. Parse Profile details (Name, Location, LinkedIn)
        if (profileContent != null && !profileContent.isBlank()) {
            extractProfileMetadata(profileContent, ctx);
        } else {
            ctx.setFullName(properties.getApplicantName());
            ctx.setEmail(properties.getApplicantEmail());
            ctx.setPhone(properties.getApplicantPhone());
            ctx.setLocation(properties.getApplicantLocation());
        }

        // 3. Parse Preferences details (Target roles, preferred tech, locations, notice period, CTC)
        if (preferencesContent != null && !preferencesContent.isBlank()) {
            extractPreferencesMetadata(preferencesContent, ctx);
        }

        log.info("Loaded user context: Candidate: {}, Location: {}, Target Roles: {}",
                ctx.getFullName(), ctx.getLocation(), ctx.getTargetRoles());

        return ctx;
    }

    private void extractProfileMetadata(String content, UserContext ctx) {
        Matcher nameMatcher = Pattern.compile("\\*\\*Name:\\*\\*\\s*(.+)", Pattern.CASE_INSENSITIVE).matcher(content);
        if (nameMatcher.find()) {
            ctx.setFullName(nameMatcher.group(1).trim());
        } else {
            ctx.setFullName(properties.getApplicantName());
        }

        Matcher locMatcher = Pattern.compile("\\*\\*Location:\\*\\*\\s*(.+)", Pattern.CASE_INSENSITIVE).matcher(content);
        if (locMatcher.find()) {
            ctx.setLocation(locMatcher.group(1).trim());
        } else {
            ctx.setLocation(properties.getApplicantLocation());
        }

        Matcher linkedinMatcher = Pattern.compile("\\*\\*LinkedIn:\\*\\*\\s*(.+)", Pattern.CASE_INSENSITIVE).matcher(content);
        if (linkedinMatcher.find()) {
            ctx.setLinkedInUrl(linkedinMatcher.group(1).trim());
        }

        ctx.setEmail(properties.getApplicantEmail());
        ctx.setPhone(properties.getApplicantPhone());
    }

    private void extractPreferencesMetadata(String content, UserContext ctx) {
        // Target Roles
        List<String> targetRoles = new ArrayList<>();
        Pattern rolePattern = Pattern.compile("## Target Roles[\\r\\n]+(([\\s]*-[\\s]*[^\\r\\n]+[\\r\\n]*)+)");
        Matcher roleMatcher = rolePattern.matcher(content);
        if (roleMatcher.find()) {
            String listBlock = roleMatcher.group(1);
            for (String line : listBlock.split("[\\r\\n]+")) {
                String role = line.replaceFirst("^[\\s]*-[\\s]*", "").trim();
                if (!role.isEmpty()) {
                    targetRoles.add(role);
                }
            }
        }
        if (targetRoles.isEmpty()) {
            targetRoles.addAll(properties.getKeywordList());
        }
        ctx.setTargetRoles(targetRoles);

        // Preferred Locations
        List<String> locations = new ArrayList<>();
        Pattern locPattern = Pattern.compile("## Location[\\s\\S]*?Preferred:[\\r\\n]+(([\\s]*-[\\s]*[^\\r\\n]+[\\r\\n]*)+)");
        Matcher locMatcher = locPattern.matcher(content);
        if (locMatcher.find()) {
            String listBlock = locMatcher.group(1);
            for (String line : listBlock.split("[\\r\\n]+")) {
                String loc = line.replaceFirst("^[\\s]*-[\\s]*", "").trim();
                if (!loc.isEmpty()) {
                    locations.add(loc);
                }
            }
        }
        if (locations.isEmpty()) {
            locations.addAll(properties.getLocationList());
        }
        ctx.setPreferredLocations(locations);

        // Notice period
        Matcher npMatcher = Pattern.compile("\\*\\*Notice period:\\*\\*\\s*`?([^`\\r\\n]+)`?", Pattern.CASE_INSENSITIVE).matcher(content);
        if (npMatcher.find()) {
            ctx.setNoticePeriod(npMatcher.group(1).trim());
        }

        // Current CTC
        Matcher ctcMatcher = Pattern.compile("\\*\\*Current CTC:\\*\\*\\s*`?([^`\\r\\n]+)`?", Pattern.CASE_INSENSITIVE).matcher(content);
        if (ctcMatcher.find()) {
            ctx.setCurrentCtc(ctcMatcher.group(1).trim());
        }
    }

    private String readFileIfExists(String filename) {
        try {
            Path path = Paths.get(filename);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
            Path rootPath = Paths.get("..", filename);
            if (Files.exists(rootPath)) {
                return Files.readString(rootPath);
            }
        } catch (IOException e) {
            log.warn("Could not read file {}: {}", filename, e.getMessage());
        }
        return "";
    }
}
