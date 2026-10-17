# Job Search Agent --- V1 Development Plan

## 1. Project Goal

Build a personal job-search assistant that helps me:

1.  Search LinkedIn for relevant jobs.
2.  Apply search filters.
3.  Collect job information.
4.  Compare each job against my resume and professional profile.
5.  Calculate a relevance score.
6.  Rank jobs from best to worst.
7.  Save and display the best matches.
8.  Let me manually review and decide which jobs to apply for.

### V1 Boundary

V1 is **read/search/analyze only**.

The system must NOT:

-   Automatically submit applications.
-   Automatically send LinkedIn messages.
-   Automatically send connection requests.
-   Perform irreversible external actions.
-   Invent skills, experience, education, certifications, companies, or
    achievements.
-   Bypass CAPTCHA, verification, or security challenges.

The user remains responsible for reviewing and manually submitting
applications.

------------------------------------------------------------------------

# 2. V1 Architecture

``` text
                    LinkedIn
                       |
                       v
                agent-browser
                       |
             Search + Filters
                       |
                       v
                Job Information
                       |
                       v
                 Job JSON
                       |
                       +----------------------+
                       |                      |
                       v                      v
                  resume.md              profile.md
                       |                      |
                       +----------+-----------+
                                  |
                                  v
                         preferences.md
                                  |
                                  v
                         DeepSeek V4 Flash
                                  |
                                  v
                         Match Analysis
                                  |
                                  v
                           Job Score /100
                                  |
                                  v
                              Ranking
                                  |
                                  v
                              Results
                                  |
                                  v
                            USER REVIEW
```

------------------------------------------------------------------------

# 3. Technology Stack

Keep V1 simple.

## Required

-   Windows
-   OpenCode Desktop
-   DeepSeek V4 Flash
-   Node.js / npm
-   agent-browser
-   Chrome/Chromium
-   Python
-   Markdown
-   JSON

## Do NOT add yet

-   Redis
-   Kafka
-   PostgreSQL
-   MongoDB
-   Vector database
-   LangChain
-   Multiple AI agents
-   Automated application submission
-   Recruiter messaging automation
-   Email automation

These can be considered later only if there is a real requirement.

------------------------------------------------------------------------

# 4. Project Structure

Create the following:

``` text
job-search-agent/
|
├── resume.md
├── profile.md
├── preferences.md
├── AGENTS.md
|
├── jobs/
|
├── results/
|
└── src/
```

Later, the project can be expanded, but V1 should remain small and
understandable.

------------------------------------------------------------------------

# 5. Milestone 1 --- Install agent-browser

The OpenCode skill has already been installed using:

``` powershell
npx skills add https://github.com/vercel-labs/agent-browser --skill agent-browser
```

Now install the actual CLI.

Run:

``` powershell
npm install -g agent-browser
```

Then:

``` powershell
agent-browser install
```

Then:

``` powershell
agent-browser --help
```

Then:

``` powershell
agent-browser doctor
```

## Success condition

All commands should complete successfully.

Do not continue to LinkedIn until this step works.

------------------------------------------------------------------------

# 6. Milestone 2 --- Test Browser Automation

Run:

``` powershell
agent-browser open https://example.com
```

Then:

``` powershell
agent-browser snapshot -i
```

Then:

``` powershell
agent-browser close
```

## What I need to learn

Understand the basic agent-browser workflow:

``` text
OPEN
  |
  v
SNAPSHOT
  |
  v
UNDERSTAND PAGE
  |
  v
CLICK / FILL / NAVIGATE
  |
  v
SNAPSHOT AGAIN
  |
  v
EXTRACT DATA
```

The important concept is that snapshots expose the page's
accessible/interactable elements so the browser agent can work with
them.

------------------------------------------------------------------------

# 7. Milestone 3 --- Verify OpenCode Integration

Verify that OpenCode can use the installed agent-browser skill.

The agent-browser skill should be available to the coding agent.

The recommended current workflow is:

``` powershell
agent-browser skills get core
```

OpenCode should use agent-browser for browser interaction rather than
inventing a separate browser automation mechanism.

------------------------------------------------------------------------

# 8. Milestone 4 --- Create the Project

Create:

``` text
job-search-agent/
```

Then:

``` text
job-search-agent/
|
├── resume.md
├── profile.md
├── preferences.md
├── AGENTS.md
|
├── jobs/
├── results/
└── src/
```

Do not build the complete system yet.

------------------------------------------------------------------------

# 9. Milestone 5 --- Create resume.md

`resume.md` is the primary source for resume information.

It should contain only verified information.

Recommended structure:

``` markdown
# Resume

## Career Summary

...

## Professional Experience

### Company
Role:
Dates:

Responsibilities:
- ...
- ...

## Technical Skills

### Languages
- ...

### Frameworks
- ...

### Frontend
- ...

### Databases
- ...

### Tools
- ...

## Projects

### Project Name
Technologies:
- ...

Description:
...

## Education

...

## Certifications

...
```

## Important Rule

The AI must never invent information.

If a skill is not present in the resume, profile, or explicitly supplied
by the user, it must not be claimed as experience.

------------------------------------------------------------------------

# 10. Milestone 6 --- Create profile.md

`profile.md` contains the broader professional profile.

Use it to represent:

-   Professional background
-   Skills
-   Experience
-   Projects
-   Education
-   Certifications
-   Career direction

Keep professional experience separate from project experience.

Do not turn project experience into professional employment experience.

------------------------------------------------------------------------

# 11. Milestone 7 --- Create preferences.md

This file defines what kinds of jobs I actually want.

Recommended structure:

``` markdown
# Job Search Preferences

## Target Roles

- Java Developer
- Java Backend Developer
- Spring Boot Developer
- Java Full Stack Developer

## Preferred Technologies

### High Priority

- Java
- Spring Boot
- Spring MVC
- REST APIs
- SQL
- React

### Medium Priority

- Docker
- Kafka
- PostgreSQL
- MySQL

## Experience

Preferred:
1-3 years

Acceptable:
0-4 years

## Locations

Preferred:
- Pune
- Mumbai
- Bangalore
- Hyderabad

## Work Mode

Preferred:
- Hybrid
- Remote

Acceptable:
- On-site

## Employment Type

Preferred:
- Full-time

## Exclude

- Internship
- Senior
- Staff
- Principal
- Non-technical roles
```

The actual values should be updated according to the user's current
preferences.

------------------------------------------------------------------------

# 12. Milestone 8 --- Create AGENTS.md

`AGENTS.md` should instruct OpenCode about the project's purpose and
safety rules.

Core rules:

``` markdown
# Job Search Agent

## Purpose

This project is a personal job-search assistant.

It searches job listings, extracts job information, compares jobs
against verified resume/profile information, scores jobs, ranks jobs,
and generates results.

## V1 Scope

V1 is read/search/analyze only.

Allowed:

- Search job listings
- Read job pages
- Extract job information
- Analyze job descriptions
- Compare jobs with resume/profile
- Score jobs
- Rank jobs
- Save results

Not allowed:

- Submit applications
- Send messages
- Send connection requests
- Perform irreversible actions
- Invent user information

## Browser

Use agent-browser for browser interaction.

## AI

Use DeepSeek V4 Flash for reasoning, extraction, comparison, and ranking.

## Sources of Truth

- resume.md
- profile.md
- preferences.md

Never invent skills, experience, education, certifications,
companies, achievements, or technologies.

If information is unknown, mark it as unknown.

## Safety

Never submit an application automatically.

Stop before any irreversible external action.
```

------------------------------------------------------------------------

# 13. Milestone 9 --- LinkedIn Exploration

Only after the browser setup works should LinkedIn be opened.

Run:

``` powershell
agent-browser open https://www.linkedin.com
```

Then:

``` powershell
agent-browser snapshot -i
```

Login should be performed manually.

Never store LinkedIn passwords in:

-   Source files
-   `resume.md`
-   `profile.md`
-   `preferences.md`
-   `AGENTS.md`
-   Git repositories

If LinkedIn displays:

-   CAPTCHA
-   Security verification
-   Login challenge
-   Identity verification

Stop and handle it manually.

Do not attempt to bypass security controls.

------------------------------------------------------------------------

# 14. Milestone 10 --- Test LinkedIn Jobs Manually

Before implementing extraction, learn the LinkedIn UI.

Example first search:

``` text
Java Developer
```

Possible initial filters:

``` text
Location:
Pune

Date Posted:
Past 24 hours

Experience:
Entry level / Associate

Job Type:
Full-time

Work Mode:
Hybrid / Remote
```

Do not automate everything immediately.

First understand the page and inspect its snapshot.

------------------------------------------------------------------------

# 15. Milestone 11 --- First Job Extraction

Start with exactly one job.

Open the job page and inspect it.

Extract only information that is actually available.

Target fields:

``` json
{
  "id": "job-001",
  "title": "...",
  "company": "...",
  "location": "...",
  "workMode": "...",
  "experience": "...",
  "salary": null,
  "requiredSkills": [],
  "preferredSkills": [],
  "description": "...",
  "url": "...",
  "applicationType": "...",
  "easyApply": null
}
```

If information is unavailable:

``` json
null
```

or:

``` text
"Unknown"
```

Do not guess.

------------------------------------------------------------------------

# 16. Milestone 12 --- Save Jobs

Use:

``` text
jobs/
```

For example:

``` text
jobs/
└── job-001.json
```

After one job works, test with:

``` text
1 job
```

Then:

``` text
5 jobs
```

Then:

``` text
20 jobs
```

Do not immediately try to collect hundreds of jobs.

------------------------------------------------------------------------

# 17. Milestone 13 --- Duplicate Detection

The same job may appear under multiple searches.

For example:

``` text
Java Developer
Spring Boot Developer
Java Backend Developer
```

may all expose the same job.

Use the job URL as the primary identifier when available.

Target flow:

``` text
80 discovered
      |
      v
duplicate detection
      |
      v
65 unique jobs
```

------------------------------------------------------------------------

# 18. Milestone 14 --- DeepSeek Matching

DeepSeek V4 Flash should receive:

``` text
resume.md
+
profile.md
+
preferences.md
+
job.json
```

It should return structured match analysis.

Example:

``` json
{
  "score": 94,
  "category": "A",
  "technicalMatch": 95,
  "experienceMatch": 90,
  "responsibilityMatch": 95,
  "locationMatch": 100,
  "seniorityMatch": 90,
  "matchingSkills": [
    "Java",
    "Spring Boot",
    "React"
  ],
  "missingSkills": [
    "AWS"
  ],
  "concerns": [
    "AWS experience is not verified"
  ],
  "recommendation": "High priority"
}
```

------------------------------------------------------------------------

# 19. Milestone 15 --- Job Scoring

Use this initial scoring model:

``` text
Technical Skill Match       40%
Relevant Experience         25%
Job Responsibility Match    15%
Location / Work Mode        10%
Seniority Match             10%
```

Formula:

``` text
Final Score =
    Technical Match × 0.40
  + Experience Match × 0.25
  + Responsibility Match × 0.15
  + Location Match × 0.10
  + Seniority Match × 0.10
```

Categories:

``` text
90-100 → A → Strong Match
80-89  → B → Good Match
70-79  → C → Possible Match
<70    → D → Low Match
```

------------------------------------------------------------------------

# 20. Milestone 16 --- Explainable Matching

Do not output only a number.

Example:

``` text
94/100 — A — Strong Match

Technical Match: 95

Matching:
- Java
- Spring Boot
- React
- REST APIs

Missing:
- AWS

Experience Match: 90

Reason:
The job requests 1-3 years of experience and the candidate
has relevant professional experience.

Recommendation:
HIGH PRIORITY
```

Every important score should have supporting evidence.

------------------------------------------------------------------------

# 21. Milestone 17 --- Rank Jobs

Example:

``` text
Job 1 → 94
Job 2 → 88
Job 3 → 83
Job 4 → 79
Job 5 → 72
```

Sort from highest to lowest.

Then display:

``` text
TOP MATCHES

1. Java Full Stack Developer — 94
2. Java Developer — 88
3. Spring Boot Developer — 83
4. Java Developer — 79
5. Backend Developer — 72
```

------------------------------------------------------------------------

# 22. Milestone 18 --- Generate Results

Create:

``` text
results/
```

Example:

``` text
results/
└── 2026-08-21.md
```

Suggested report:

``` markdown
# Job Search Results

Date:
...

Jobs discovered:
...

Unique jobs:
...

Strong matches:
...

Good matches:
...

## Top Matches

### 1. Java Full Stack Developer

Score: 94/100
Category: A

Company:
...

Location:
...

Work Mode:
...

Matching Skills:
- Java
- Spring Boot
- React
- REST APIs

Missing:
- AWS

Recommendation:
HIGH PRIORITY

URL:
...
```

------------------------------------------------------------------------

# 23. Milestone 19 --- Final V1 Command

Eventually V1 should be runnable with a simple command such as:

``` powershell
python src/main.py
```

Expected flow:

``` text
Starting Job Search Agent...

Loading profile...
✓ resume.md
✓ profile.md
✓ preferences.md

Opening browser...
✓ agent-browser

Searching jobs...

Jobs discovered: 35
Unique jobs: 29

Analyzing jobs...

Top Matches:

1. Java Full Stack Developer — 94
2. Java Developer — 91
3. Spring Boot Developer — 88
4. Backend Developer — 83
5. Java Developer — 81

Results saved to:
results/2026-08-21.md
```

------------------------------------------------------------------------

# 24. V1 Completion Criteria

V1 is complete only when all of these work:

-   [ ] agent-browser installed
-   [ ] agent-browser doctor succeeds
-   [ ] Browser can open a test page
-   [ ] Browser snapshots work
-   [ ] OpenCode can access the agent-browser skill
-   [ ] `job-search-agent` project exists
-   [ ] `resume.md` exists
-   [ ] `profile.md` exists
-   [ ] `preferences.md` exists
-   [ ] `AGENTS.md` exists
-   [ ] LinkedIn can be opened
-   [ ] LinkedIn job search can be performed
-   [ ] Search filters can be applied
-   [ ] One job can be extracted
-   [ ] Job data can be saved as JSON
-   [ ] Multiple jobs can be collected
-   [ ] Duplicate jobs can be removed
-   [ ] DeepSeek can analyze jobs
-   [ ] Jobs can be scored
-   [ ] Jobs can be ranked
-   [ ] Results can be saved
-   [ ] Top matches can be displayed
-   [ ] No application is submitted automatically

------------------------------------------------------------------------

# 25. V1 Roadmap Summary

``` text
1. Install agent-browser
        ↓
2. Verify browser
        ↓
3. Verify OpenCode skill
        ↓
4. Create project
        ↓
5. Create resume/profile/preferences
        ↓
6. Create AGENTS.md
        ↓
7. Explore LinkedIn manually
        ↓
8. Search jobs
        ↓
9. Extract one job
        ↓
10. Save job JSON
        ↓
11. Extract multiple jobs
        ↓
12. Remove duplicates
        ↓
13. DeepSeek matching
        ↓
14. Score
        ↓
15. Rank
        ↓
16. Generate results
        ↓
17. V1 COMPLETE
```

------------------------------------------------------------------------

# 26. Future V2 --- Application Assistance

Do NOT implement this during V1.

After V1 is stable:

``` text
Select job
    ↓
Open application
    ↓
Read form
    ↓
Map verified information
    ↓
Prepare answers
    ↓
Fill appropriate fields
    ↓
STOP
    ↓
USER REVIEWS
    ↓
USER MANUALLY SUBMITS
```

The user must remain in control of final submission.

------------------------------------------------------------------------

# 27. Immediate Next Step

Do not build the whole project at once.

Start with:

``` powershell
npm install -g agent-browser
agent-browser install
agent-browser --help
agent-browser doctor
```

Then send the command output for verification.

Only after that should Milestone 2 begin.
