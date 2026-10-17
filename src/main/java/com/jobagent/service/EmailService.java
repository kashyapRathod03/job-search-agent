package com.jobagent.service;

import com.jobagent.config.AgentProperties;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String TWO_FA_FILE = "2fa_code.txt";

    private final AgentProperties properties;
    private final GeminiService geminiService;

    public EmailService(AgentProperties properties, GeminiService geminiService) {
        this.properties = properties;
        this.geminiService = geminiService;
    }

    public String fetchLinkedIn2FACode() {
        log.info("Attempting to automatically retrieve LinkedIn 2FA verification code...");

        // 1. Try automated IMAP inbox check first
        String code = fetchCodeFromImap();
        if (code != null && !code.isBlank()) {
            log.info("Successfully retrieved 2FA code via Gmail IMAP: {}", code);
            return code;
        }

        // 2. Fallback: Wait and poll 2fa_code.txt file (up to 90 seconds)
        log.info("Waiting for 2FA code in '{}' or terminal input...", TWO_FA_FILE);
        ensureFileExists(TWO_FA_FILE);

        for (int i = 0; i < 45; i++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            String fileCode = readCodeFromFile();
            if (fileCode != null && !fileCode.isBlank()) {
                log.info("Detected 2FA verification code from {}: {}", TWO_FA_FILE, fileCode);
                return fileCode;
            }
        }

        log.warn("Timed out waiting for 2FA code.");
        return null;
    }

    public String checkPinNow() {
        return readCodeFromFile();
    }

    private String fetchCodeFromImap() {
        String username = properties.getLinkedinUsername();
        String password = properties.getLinkedinPassword();

        if (username == null || !username.endsWith("@gmail.com")) {
            return null;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "10000");

        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Get last 10 messages
            int total = inbox.getMessageCount();
            int start = Math.max(1, total - 10);
            Message[] messages = inbox.getMessages(start, total);

            for (int i = messages.length - 1; i >= 0; i--) {
                Message msg = messages[i];
                String subject = msg.getSubject() != null ? msg.getSubject() : "";
                String from = msg.getFrom() != null && msg.getFrom().length > 0 ? msg.getFrom()[0].toString() : "";

                if (subject.toLowerCase().contains("pin") ||
                    subject.toLowerCase().contains("verification") ||
                    subject.toLowerCase().contains("security code") ||
                    from.toLowerCase().contains("linkedin")) {

                    String body = getTextFromMessage(msg);
                    String pin = extractPin(subject + " " + body);
                    if (pin != null) {
                        inbox.close(false);
                        store.close();
                        return pin;
                    }
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            log.info("IMAP direct access not available (standard Gmail password requires App Password): {}", e.getMessage());
        }
        return null;
    }

    private String extractPin(String content) {
        // Look for 6-digit pin
        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Ask Gemini to extract pin from email content
        return geminiService.extractPinFromEmail(content);
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private void ensureFileExists(String filename) {
        try {
            Path path = Paths.get(filename);
            if (!Files.exists(path)) {
                Files.writeString(path, "# Write your 6-digit LinkedIn verification code on the line below\n");
            }
        } catch (IOException ignored) {}
    }

    private String readCodeFromFile() {
        try {
            Path path = Paths.get(TWO_FA_FILE);
            if (Files.exists(path)) {
                for (String line : Files.readAllLines(path)) {
                    line = line.trim();
                    if (line.matches("^\\d{6}$")) {
                        return line;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
