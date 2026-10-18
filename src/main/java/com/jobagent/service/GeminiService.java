package com.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobagent.config.AgentProperties;
import com.jobagent.model.FormField;
import com.jobagent.model.JobPosting;
import com.jobagent.model.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final AgentProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService(AgentProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void evaluateJobMatch(JobPosting job, UserContext ctx) {
        if (!hasApiKey()) {
            log.warn("Gemini API key not configured. Using rule-based heuristic match evaluation.");
            heuristicJobMatch(job, ctx);
            return;
        }

        String prompt = String.format("""
                You are a senior technical recruiter and career advisor.
                Evaluate if this job listing matches the candidate's profile and preferences.
                
                RULES:
                1. Never invent or hallucinate candidate skills. Use only verified information.
                2. Avoid Senior/Staff/Principal roles, pure frontend, pure QA, or sales roles.
                3. High priority technologies: Java, Spring Boot, REST APIs, React, SQL.
                
                JOB DETAILS:
                Title: %s
                Company: %s
                Location: %s
                Description:
                %s
                
                CANDIDATE TARGET PREFERENCES:
                Roles: %s
                Locations: %s
                Experience: %s
                
                Respond in valid JSON with this exact schema:
                {
                  "matchScore": <number 0-100>,
                  "shouldApply": <true/false>,
                  "reason": "<short explanation>"
                }
                """,
                job.getTitle(), job.getCompany(), job.getLocation(),
                truncate(job.getDescription(), 3000),
                ctx.getTargetRoles(), ctx.getPreferredLocations(), ctx.getExperienceSummary()
        );

        try {
            String responseText = callGemini(prompt);
            JsonNode root = parseJsonFromResponse(responseText);
            if (root != null) {
                job.setMatchScore(root.path("matchScore").asInt(70));
                job.setShouldApply(root.path("shouldApply").asBoolean(true));
                job.setMatchReason(root.path("reason").asText("Matched via Gemini analysis"));
                log.info("Gemini Job Evaluation: Score={}, Apply={}, Reason={}",
                        job.getMatchScore(), job.isShouldApply(), job.getMatchReason());
                return;
            }
        } catch (Exception e) {
            log.error("Failed to evaluate job via Gemini: {}", e.getMessage());
        }

        heuristicJobMatch(job, ctx);
    }

    public String tailorResume(JobPosting job, UserContext ctx) {
        if (!hasApiKey()) {
            log.info("Gemini API key not provided; using base resume markdown.");
            return ctx.getRawResume();
        }

        String prompt = String.format("""
                You are a technical resume optimization expert.
                Tailor the candidate's resume for this specific job posting.
                
                CRITICAL CONSTRAINTS:
                - Do NOT invent, exaggerate, or fabricate any experience, skill, certification, or project.
                - Only emphasize and highlight technologies and accomplishments that ALREADY exist in the verified resume.
                - Reorganize keywords and summary to directly match the target job description.
                
                TARGET JOB:
                Title: %s at %s
                Description:
                %s
                
                VERIFIED RESUME:
                %s
                
                Generate a clean, professional, concise Markdown resume ready to convert into a PDF.
                """,
                job.getTitle(), job.getCompany(),
                truncate(job.getDescription(), 3000),
                truncate(ctx.getRawResume(), 6000)
        );

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.error("Failed to tailor resume with Gemini: {}", e.getMessage());
            return ctx.getRawResume();
        }
    }

    public String answerFormQuestion(String questionText, String fieldType, List<String> options, UserContext ctx, JobPosting job) {
        String lowerQ = questionText.toLowerCase();

        // Direct rule shortcuts for known questions
        if (lowerQ.contains("notice period") || lowerQ.contains("notice")) {
            return "60";
        }
        if (lowerQ.contains("current ctc") || lowerQ.contains("current salary")) {
            return "570000";
        }
        if (lowerQ.contains("expected ctc") || lowerQ.contains("expected salary") || lowerQ.contains("compensation")) {
            return "800000";
        }
        if (lowerQ.contains("relocate") || lowerQ.contains("willing to relocate")) {
            return options.stream().filter(o -> o.equalsIgnoreCase("yes")).findFirst().orElse("Yes");
        }
        if (lowerQ.contains("experience with java") || (lowerQ.contains("years of experience") && lowerQ.contains("java"))) {
            return "2";
        }
        if (lowerQ.contains("experience with spring") || lowerQ.contains("spring boot")) {
            return "2";
        }
        if (lowerQ.contains("additional month") || lowerQ.contains("months of experience")) {
            return "0";
        }

        if (!hasApiKey()) {
            return fallbackAnswer(fieldType, options);
        }

        String prompt = String.format("""
                You are answering an application form question on LinkedIn on behalf of the applicant.
                
                APPLICANT PROFILE:
                Name: %s
                Location: %s
                Experience: 1-3 years in Java, Spring Boot, REST APIs, React, MySQL
                Notice Period: 60 days (negotiable / buyout option available)
                Current CTC: 5.7 LPA
                Expected CTC: 8 LPA (or negotiable based on budget)
                Willing to relocate: Yes
                
                JOB: %s at %s
                
                QUESTION: %s
                FIELD TYPE: %s
                AVAILABLE OPTIONS: %s
                
                RULES:
                1. Provide ONLY the exact, concise answer string to input into the field or select.
                2. If it is a number or years of experience, output ONLY the number (e.g. '2' or '3').
                3. If options are provided, output EXACTLY one matching option string.
                4. Do NOT include markdown, punctuation, or explanation.
                """,
                ctx.getFullName(), ctx.getLocation(),
                job.getTitle(), job.getCompany(),
                questionText, fieldType, options
        );

        try {
            String answer = callGemini(prompt).trim();
            // clean up backticks or quotes if any
            answer = answer.replaceAll("^[\"']|[\"']$", "").trim();
            log.info("Gemini answered question '{}' with: '{}'", questionText, answer);
            return answer;
        } catch (Exception e) {
            log.error("Failed to answer question via Gemini: {}", e.getMessage());
            return fallbackAnswer(fieldType, options);
        }
    }

    public Map<String, String> answerQuestionsBatch(List<FormField> fields, UserContext ctx, JobPosting job) {
        Map<String, String> resultMap = new HashMap<>();
        if (fields == null || fields.isEmpty()) {
            return resultMap;
        }

        if (!hasApiKey()) {
            for (FormField f : fields) {
                resultMap.put(f.getLabel(), fallbackAnswer(f.getFieldType(), f.getOptions()));
            }
            return resultMap;
        }

        log.info("Batch answering {} question(s) via Gemini in a single request...", fields.size());

        StringBuilder questionsBuilder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            FormField f = fields.get(i);
            questionsBuilder.append(String.format("%d. Question: \"%s\" | Type: %s | Options: %s\n",
                    (i + 1), f.getLabel(), f.getFieldType(), f.getOptions()));
        }

        String prompt = String.format("""
                You are an expert career assistant. Answer the following LinkedIn application form questions for candidate %s applying for:
                Job Title: %s
                Company: %s
                
                CANDIDATE VERIFIED PROFILE:
                - Full Name: %s
                - Location: %s
                - Experience: 2 years (Core technologies: Java, Spring Boot, REST APIs, React, SQL)
                - Notice Period in days: 60
                - Current CTC / Annual Compensation: 570000
                - Expected CTC / Desired Compensation: 800000
                - Authorized to work in India: Yes
                - Require Visa Sponsorship: No
                - Willing to relocate: Yes
                - Education: Bachelor of Technology in Information Technology
                
                QUESTIONS:
                %s
                
                RULES:
                1. For numeric or salary or experience questions (notice period, current CTC, expected CTC, years of experience, months of experience), output ONLY the raw integer number (e.g. "60", "570000", "800000", "2", "0") WITHOUT any currency signs, words, units or text like 'days' or 'LPA'.
                2. If 'Options' are provided, the answer MUST EXACTLY MATCH one of the strings in the Options list.
                3. For sponsorship questions, answer "No". For authorization/work questions, answer "Yes".
                4. For additional months of experience, answer "0" or "0 month" if available.
                
                Respond in valid JSON with an "answers" array:
                {
                  "answers": [
                    {"question": "<exact question text>", "answer": "<exact option or short value>"}
                  ]
                }
                """,
                ctx.getFullName(), job.getTitle(), job.getCompany(),
                ctx.getFullName(), ctx.getLocation(),
                questionsBuilder.toString()
        );

        try {
            String response = callGemini(prompt);
            JsonNode root = parseJsonFromResponse(response);
            if (root != null && root.has("answers") && root.get("answers").isArray()) {
                ArrayNode arr = (ArrayNode) root.get("answers");
                for (JsonNode item : arr) {
                    String qText = item.path("question").asText().trim();
                    String ans = item.path("answer").asText().trim();
                    if (!qText.isBlank() && !ans.isBlank()) {
                        resultMap.put(qText, ans);
                    }
                }
            }
            log.info("Gemini batch answered {} of {} questions successfully.", resultMap.size(), fields.size());
        } catch (Exception e) {
            log.error("Failed to batch answer questions via Gemini: {}", e.getMessage());
        }

        // Fill any questions that weren't returned with fallback
        for (FormField f : fields) {
            if (!resultMap.containsKey(f.getLabel())) {
                resultMap.put(f.getLabel(), fallbackAnswer(f.getFieldType(), f.getOptions()));
            }
        }

        return resultMap;
    }

    public String diagnosePlaywrightError(String errorMessage, String domSnippet, String pageUrl) {
        if (!hasApiKey()) {
            return "Try standard fallback selector: button:has-text('Next'), button:has-text('Review'), button[aria-label='Continue to next step']";
        }

        String prompt = String.format("""
                Playwright encountered an error while automating a LinkedIn Easy Apply form.
                
                PAGE URL: %s
                ERROR: %s
                
                DOM SNIPPET:
                %s
                
                TASK:
                What is the correct CSS selector or Playwright action to proceed past this error?
                Respond in this structured format:
                SELECTOR: <css selector or text selector>
                ACTION: <click | type | scroll | wait>
                EXPLANATION: <brief note on why this resolves the issue>
                """,
                pageUrl, errorMessage, truncate(domSnippet, 3500)
        );

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.error("Failed to diagnose Playwright error via Gemini: {}", e.getMessage());
            return "SELECTOR: button.artdeco-button--primary\nACTION: click\nEXPLANATION: Fallback primary button click";
        }
    }

    public String extractPinFromEmail(String emailContent) {
        if (!hasApiKey() || emailContent == null || emailContent.isBlank()) {
            return null;
        }

        String prompt = String.format("""
                Extract the 6-digit verification code or PIN from this LinkedIn security email.
                
                EMAIL CONTENT:
                %s
                
                Output ONLY the 6 digits (e.g. 123456) with no words, markdown, or spaces. If not found, output NONE.
                """,
                truncate(emailContent, 3000)
        );

        try {
            String result = callGemini(prompt).trim();
            Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(result);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Gemini could not extract PIN: {}", e.getMessage());
        }
        return null;
    }

    private String callGemini(String prompt) throws IOException, InterruptedException {
        List<String> modelsToTry = java.util.stream.Stream.of(
                properties.getGeminiModel(),
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite"
        ).filter(m -> m != null && !m.isBlank()).distinct().toList();

        for (String modelName : modelsToTry) {
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    properties.getGeminiBaseUrl(),
                    modelName,
                    properties.getGeminiApiKey());

            ObjectNode requestRoot = objectMapper.createObjectNode();
            ArrayNode contents = requestRoot.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            String jsonPayload = objectMapper.writeValueAsString(requestRoot);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidate = root.path("candidates").get(0);
                if (candidate != null) {
                    JsonNode partsNode = candidate.path("content").path("parts").get(0);
                    if (partsNode != null) {
                        return partsNode.path("text").asText("");
                    }
                }
            } else if (response.statusCode() == 503 || response.statusCode() == 404 || response.statusCode() == 429) {
                log.warn("Gemini model '{}' returned {}. Trying fallback model...", modelName, response.statusCode());
                if (response.statusCode() == 429) {
                    Thread.sleep(1200);
                }
                continue;
            } else {
                throw new RuntimeException("Gemini API call failed with status: " + response.statusCode() + " body: " + response.body());
            }
        }

        throw new RuntimeException("All Gemini candidate models failed to respond.");
    }

    private JsonNode parseJsonFromResponse(String response) {
        try {
            String clean = response.replaceAll("(?i)```json", "").replaceAll("```", "").trim();
            Matcher matcher = Pattern.compile("(?s)\\{.*\\}").matcher(clean);
            if (matcher.find()) {
                return objectMapper.readTree(matcher.group());
            }
            return objectMapper.readTree(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private void heuristicJobMatch(JobPosting job, UserContext ctx) {
        String title = job.getTitle().toLowerCase();
        // Negative filter
        if (title.contains("senior") || title.contains("staff") || title.contains("principal") ||
            title.contains("lead") || title.contains("manager") || title.contains("intern") ||
            title.contains("sales") || title.contains("recruiter")) {
            job.setMatchScore(20);
            job.setShouldApply(false);
            job.setMatchReason("Job title contains excluded seniority or domain keyword.");
            return;
        }

        // Positive filter
        if (title.contains("java") || title.contains("spring") || title.contains("backend") || title.contains("software engineer")) {
            job.setMatchScore(85);
            job.setShouldApply(true);
            job.setMatchReason("Title matches target role profile (Java / Backend / Spring).");
            return;
        }

        job.setMatchScore(60);
        job.setShouldApply(true);
        job.setMatchReason("Heuristic match approved.");
    }

    public String fallbackAnswer(String fieldType, List<String> options) {
        if (options != null && !options.isEmpty()) {
            return options.get(0);
        }
        return "Yes";
    }

    private boolean hasApiKey() {
        return properties.getGeminiApiKey() != null && !properties.getGeminiApiKey().isBlank();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
