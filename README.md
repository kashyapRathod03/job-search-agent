# LinkedIn Job Application Agent (Headless Spring Boot & Playwright)

An autonomous, headless LinkedIn job discovery and application agent built from scratch using **Spring Boot 3**, **Java 22**, **Gradle 8.10**, **Microsoft Playwright for Java**, and **Google Gemini AI**.

---

## Architecture Overview

* **CLI Runner:** Pure headless Spring Boot execution via `CommandLineRunner` (no web servlet overhead).
* **Context Ingestion:** Reads ground truth from local `preferences.md`, `profile.md`, and `resume.md`.
* **Playwright Automation:** Headless Chromium with anti-bot stealth flags, session persistence (`.session/state.json`), and randomized action delays (2 to 6 seconds).
* **AI Engine:** Google Gemini integration for job matching evaluation, truthful resume tailoring, application question answering, and DOM diagnostic self-healing.
* **Resume Generation:** Dynamically generates tailored PDF resumes using OpenPDF and attaches them to Easy Apply forms.
* **Self-Healing Recovery:** Wraps Playwright actions in try-catch blocks, passes error and DOM snippets to Gemini for fallback selectors/actions, executes them, and logs all recovery events to `playwright_recovery_logs.md`.

---

## Prerequisites

* **Java:** JDK 22 installed (System default `22.0.2`).
* **Gradle:** Included via `./gradlew` / `gradlew.bat`.

---

## Configuration

Set your credentials in `src/main/resources/application.properties` or as environment variables:

```properties
# LinkedIn Authentication
LINKEDIN_USERNAME=your_email@example.com
LINKEDIN_PASSWORD=your_password

# Google Gemini API
GEMINI_API_KEY=your_gemini_api_key
```

### Key Application Properties

| Property | Default | Description |
| :--- | :--- | :--- |
| `linkedin.headless` | `true` | Runs Chromium headlessly (set to `false` if manual 2FA verification is needed) |
| `linkedin.session.path` | `./.session/state.json` | Stores session cookies so repeated logins are not required |
| `agent.delay.min-ms` | `2000` | Minimum anti-bot randomized delay between actions (ms) |
| `agent.delay.max-ms` | `6000` | Maximum anti-bot randomized delay between actions (ms) |
| `gemini.model` | `gemini-2.5-flash` | Gemini model used for matching and self-healing |
| `search.keywords` | `Java Developer, Spring Boot Developer` | Target search keywords |
| `search.easy-apply-only` | `true` | Restricts search to Easy Apply jobs |
| `search.date-posted` | `past-24h` | Filters jobs posted in the last 24 hours |

---

## Running the Agent

### Run All Tests
```bash
./gradlew test
```

### Launch the Application Agent
```bash
./gradlew bootRun
```
Or on Windows:
```cmd
gradlew.bat bootRun
```

---

## Self-Healing Logs

Any DOM selector errors or timeout recoveries are captured and logged to:
* `playwright_recovery_logs.md`
