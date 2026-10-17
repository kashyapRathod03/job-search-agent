package com.jobagent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jobagent.model.UserContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final String KB_FILE_PATH = "qa_knowledge_base.json";

    private final ObjectMapper objectMapper;
    private final Map<String, String> knowledgeBase = new ConcurrentHashMap<>();

    public KnowledgeBaseService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    public synchronized void seedDefaultContext(UserContext ctx) {
        if (ctx == null) return;

        // Seed core candidate identity facts if missing
        putIfAbsent("first name", "Kashyap");
        putIfAbsent("last name", "Rathod");
        putIfAbsent("full name", ctx.getFullName() != null ? ctx.getFullName() : "Kashyap Rathod");
        putIfAbsent("email", ctx.getEmail() != null ? ctx.getEmail() : "kashyapdr044@gmail.com");
        putIfAbsent("email address", ctx.getEmail() != null ? ctx.getEmail() : "kashyapdr044@gmail.com");
        putIfAbsent("phone", ctx.getPhone() != null ? ctx.getPhone() : "9054397202");
        putIfAbsent("mobile phone number", ctx.getPhone() != null ? ctx.getPhone() : "9054397202");
        putIfAbsent("phone country code", "India (+91)");

        // Location
        String loc = ctx.getLocation() != null ? ctx.getLocation() : "Anand, Gujarat, India";
        putIfAbsent("location", loc);
        putIfAbsent("current location", loc);
        putIfAbsent("city", "Anand");
        putIfAbsent("state", "Gujarat");
        putIfAbsent("country", "India");

        // Preferences & Compensation
        putIfAbsent("notice period", "60");
        putIfAbsent("notice period in days", "60");
        putIfAbsent("current ctc", "570000");
        putIfAbsent("current ctc in inr", "570000");
        putIfAbsent("expected ctc", "800000");
        putIfAbsent("expected ctc in inr", "800000");

        // Experience
        putIfAbsent("years of experience", "2");
        putIfAbsent("total years of experience", "2");
        putIfAbsent("total years of professional experience", "2");
        putIfAbsent("total additional months of experience", "0");
        putIfAbsent("additional months of experience", "0");

        // Authorization & Logistics
        putIfAbsent("legally authorized to work in india", "Yes");
        putIfAbsent("authorized to work in india", "Yes");
        putIfAbsent("require sponsorship", "No");
        putIfAbsent("will you require sponsorship", "No");
        putIfAbsent("visa sponsorship", "No");
        putIfAbsent("comfortable commuting", "Yes");
        putIfAbsent("willing to relocate", "Yes");
        putIfAbsent("relocate", "Yes");

        // Education
        putIfAbsent("highest level of education", "Bachelor's Degree");
        putIfAbsent("degree", "Bachelor of Technology in Information Technology");
        putIfAbsent("major", "Information Technology");
        putIfAbsent("gpa", "8.8");

        saveKnowledgeBase();
    }

    private void putIfAbsent(String key, String value) {
        String normalized = normalizeKey(key);
        if (!knowledgeBase.containsKey(normalized)) {
            knowledgeBase.put(normalized, value);
        }
    }

    public String lookupAnswer(String rawQuestion, List<String> options) {
        if (rawQuestion == null || rawQuestion.isBlank()) {
            return matchFromOptions("Yes", options);
        }

        String normalized = normalizeKey(rawQuestion);

        // 1. Prioritized specific rules (Notice period, CTC, INR amounts, Experience, Months)
        if (normalized.contains("notice") || normalized.contains("notice period")) {
            return matchFromOptions("60", options);
        }

        if (normalized.contains("current ctc") || normalized.contains("current annual") || normalized.contains("current salary")) {
            if (normalized.contains("inr") || normalized.contains("annual") || normalized.contains("numeric") || normalized.contains("amount")) {
                return matchFromOptions("570000", options);
            }
            if (normalized.contains("lpa") || normalized.contains("lakh")) {
                return matchFromOptions("5.7", options);
            }
            return matchFromOptions("570000", options);
        }

        if (normalized.contains("expected ctc") || normalized.contains("expected annual") || normalized.contains("expected salary") || normalized.contains("desired salary") || normalized.contains("compensation")) {
            if (normalized.contains("inr") || normalized.contains("annual") || normalized.contains("numeric") || normalized.contains("amount")) {
                return matchFromOptions("800000", options);
            }
            if (normalized.contains("lpa") || normalized.contains("lakh")) {
                return matchFromOptions("8", options);
            }
            return matchFromOptions("800000", options);
        }

        if (normalized.contains("additional month") || normalized.contains("months of experience")) {
            return matchFromOptions("0", options);
        }

        if (normalized.contains("year") && (normalized.contains("experience") || normalized.contains("professional"))) {
            return matchFromOptions("2", options);
        }

        if (normalized.equals("month") || normalized.contains("from month") || normalized.contains("start month")) {
            return matchFromOptions("January", options);
        }
        if (normalized.contains("to month") || normalized.contains("end month")) {
            return matchFromOptions("December", options);
        }
        if (normalized.equals("year") || normalized.contains("from year") || normalized.contains("start year")) {
            return matchFromOptions("2024", options);
        }
        if (normalized.contains("to year") || normalized.contains("end year")) {
            return matchFromOptions("2026", options);
        }

        if (normalized.contains("sponsorship") || normalized.contains("visa")) {
            return matchFromOptions("No", options);
        }

        if (normalized.contains("authorized") || normalized.contains("eligible to work") || normalized.contains("relocate") || normalized.contains("commute") || normalized.contains("comfortable commuting")) {
            return matchFromOptions("Yes", options);
        }

        if (normalized.contains("country code") || normalized.contains("phone country")) {
            return matchFromOptions("India (+91)", options);
        }

        if (normalized.contains("phone") || normalized.contains("mobile")) {
            return matchFromOptions("+919054397202", options);
        }

        if (normalized.contains("city") || normalized.contains("current location")) {
            return matchFromOptions("Anand, Gujarat, India", options);
        }

        if (normalized.contains("email")) {
            return matchFromOptions("kashyapdr044@gmail.com", options);
        }

        if (normalized.contains("highest level of education") || normalized.contains("degree") || normalized.contains("education")) {
            return matchFromOptions("Bachelor's Degree", options);
        }

        // 2. Direct exact key match
        if (knowledgeBase.containsKey(normalized)) {
            return matchFromOptions(knowledgeBase.get(normalized), options);
        }

        // 3. Longest Key Fuzzy match (sort keys by descending length)
        List<String> sortedKeys = new ArrayList<>(knowledgeBase.keySet());
        sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String key : sortedKeys) {
            if (normalized.contains(key) || key.contains(normalized)) {
                return matchFromOptions(knowledgeBase.get(key), options);
            }
        }

        return null; // Not found in Knowledge Base -> requires model inference
    }

    public synchronized void saveAnswer(String rawQuestion, String answer) {
        if (rawQuestion == null || answer == null) return;
        knowledgeBase.put(normalizeKey(rawQuestion), answer.trim());
        saveKnowledgeBase();
    }

    public synchronized void saveAll(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) return;
        answers.forEach((k, v) -> {
            if (k != null && v != null) {
                knowledgeBase.put(normalizeKey(k), v.trim());
            }
        });
        saveKnowledgeBase();
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(knowledgeBase);
    }

    public String matchFromOptions(String bestValue, List<String> options) {
        if (options == null || options.isEmpty()) {
            return bestValue;
        }

        String bestLower = bestValue.toLowerCase().trim();

        // 1. Exact match
        for (String opt : options) {
            if (opt.trim().equalsIgnoreCase(bestValue.trim())) {
                return opt.trim();
            }
        }

        // 2. Number extraction match for numeric bestValue (e.g. "0" matches "0" or "0 months", but NOT "10" or "20")
        if (bestLower.matches("\\d+")) {
            for (String opt : options) {
                String optTrimmed = opt.trim();
                String optLower = optTrimmed.toLowerCase();
                if (isPlaceholderOption(optLower)) {
                    continue;
                }
                if (optLower.matches("^" + Pattern.quote(bestLower) + "(\\s+.*)?$") ||
                    optLower.matches(".*\\b" + Pattern.quote(bestLower) + "\\b.*")) {
                    return optTrimmed;
                }
            }
        }

        // 3. Substring match for non-numeric or descriptive values, filtering out placeholders
        for (String opt : options) {
            String optTrimmed = opt.trim();
            String optLower = optTrimmed.toLowerCase();
            if (isPlaceholderOption(optLower)) {
                continue;
            }
            if (!optLower.isEmpty() && (optLower.contains(bestLower) || bestLower.contains(optLower))) {
                return optTrimmed;
            }
        }

        // 4. Fallback: return the first NON-PLACEHOLDER option
        for (String opt : options) {
            String optTrimmed = opt.trim();
            if (!isPlaceholderOption(optTrimmed.toLowerCase())) {
                return optTrimmed;
            }
        }

        return options.get(0);
    }

    public boolean isPlaceholderOption(String optLower) {
        return optLower.isEmpty() ||
                optLower.equals("select") ||
                optLower.equals("select an option") ||
                optLower.equals("select option") ||
                optLower.equals("please select") ||
                optLower.equals("choose") ||
                optLower.equals("month") ||
                optLower.equals("year") ||
                optLower.equals("--");
    }

    private String normalizeKey(String key) {
        return key.toLowerCase()
                .replaceAll("[*?:\"'\\[\\]()]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void loadKnowledgeBase() {
        File file = new File(KB_FILE_PATH);
        if (file.exists()) {
            try {
                Map<String, String> loaded = objectMapper.readValue(file, new TypeReference<Map<String, String>>() {});
                knowledgeBase.putAll(loaded);
                log.info("Loaded {} question-answer pairs from {}", knowledgeBase.size(), KB_FILE_PATH);
            } catch (IOException e) {
                log.warn("Could not read {}: {}", KB_FILE_PATH, e.getMessage());
            }
        } else {
            log.info("{} does not exist yet. Will create once seeded.", KB_FILE_PATH);
        }
    }

    private void saveKnowledgeBase() {
        try {
            objectMapper.writeValue(new File(KB_FILE_PATH), knowledgeBase);
        } catch (IOException e) {
            log.error("Failed to write to {}: {}", KB_FILE_PATH, e.getMessage());
        }
    }
}
