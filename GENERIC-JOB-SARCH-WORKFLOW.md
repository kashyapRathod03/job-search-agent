# GENERIC JOB SEARCH AGENT WORKFLOW
# A transferable workflow for LinkedIn job search and application automation
# This workflow is designed to be model-agnostic and restriction-free

# JOB SEARCH AGENT — MASTER WORKFLOW

## Purpose

This project is a personal LinkedIn job-search assistant.

The agent uses:

- OpenCode as the coding/agent environment
- GPT-5 as the reasoning, extraction, ranking and planning model
- `agent-browser` for browser interaction
- LinkedIn as the job/network data source
- Local project files for storing jobs, people and search results

## WORKFLOW OVERVIEW
This workflow guides a job search agent through:
1. Opening LinkedIn and authenticating
2. Searching for relevant jobs using filters
3. Extracting job details from individual listings
4. Applying to jobs with automated form filling
5. Returning to the job search page

---

## STEP-BY-STEP WORKFLOW

### STEP 1: OPEN LINKEDIn AND AUTHENTICATE
```
1. Open LinkedIn homepage
   Command: agent-browser open https://www.linkedin.com

2. Take initial snapshot to identify page elements
   Command: agent-browser snapshot -i

3. Locate and click "Sign in" link
   - Identify sign-in link in snapshot (look for login-related refs)
   - Command: agent-browser click [signin-ref]

4. Fill credentials in login form
   - Find email textbox ref from snapshot
   - Command: agent-browser fill [email-ref] "user-email@gmail.com"
   
   - Find password textbox ref from snapshot
   - Command: agent-browser fill [password-ref] "user-password"

5. Click sign-in button
   - Find sign-in button ref from snapshot
   - Command: agent-browser click [signin-button-ref]

6. Verify successful login
   - Take new snapshot
   - Look for user profile/dashboard indicators
   - Command: agent-browser snapshot -i

credentials are located at last of this file
```

### STEP 2: NAVIGATE TO JOB SEARCH
```
1. Navigate to Jobs section
   - Click "Jobs" from top navigation or sidebar
   - Or: agent-browser click [jobs-nav-ref]

2. Take snapshot of Jobs page
   - Command: agent-browser snapshot -i

3. Verify we're on job search page
   - Look for job search textbox and filter options
   - Expected elements: "Describe the job you want" textbox, job cards, filter buttons
```

### STEP 3: SEARCH FOR RELEVANT JOBS
```
1. Enter search keywords
   - Use target role keywords from user profile
   - Command: agent-browser fill [search-ref] "Java Developer"
   
   - Or multiple keywords: "Java Developer Spring Boot"

2. Apply search filters (optional but recommended)
   - **Location filter**: Apply user's preferred locations
     - Command: agent-browser click [location-filter-ref]
     - Select from dropdown: user's preferred cities/regions
   
   - **Date posted filter**: Filter by recency
     - Command: agent-browser click [date-filter-ref]
     - Select: "Past 24 hours", "Past week", "Past 2 weeks", "Past 4 weeks"
   
   - **Experience filter**: Apply experience level preferences
     - Command: agent-browser click [experience-filter-ref]
     - Select: "Entry level", "Associate", "1-3 years", etc.
   
   - **Work mode filter**: Hybrid/Remote/On-site
     - Command: agent-browser click [workmode-filter-ref]
     - Select preferred options
   
   - **Employment type**: Full-time, Contract, etc.
     - Command: agent-browser click [employment-type-ref]
     - Select preferred options

3. Take snapshot after applying filters
   - Command: agent-browser snapshot -i

4. Review filtered job results
   - Identify job listings in current snapshot
   - Note: LinkedIn may show "Jobs based on your preferences" or full search results
```

### STEP 4: IDENTIFY AND SELECT TARGET JOBS
```
1. List available jobs from snapshot
   - Look for job card elements in the page
   - Note job titles, companies, locations, dates posted
   - Example elements from snapshot:
     - "Java Developer (Verified job) Barclays • Pune Division"
     - "Software Engineer - Java (Verified job) Barclays • Pune Division"
     - "Java GCP (Verified job) Capgemini • Bengaluru"

2. Select first job to process
   - Prioritize based on:
     - Most recent date posted
     - Best location match
     - Best skills match
     - Highest relevance to user profile

3. Click on job to open details
   - Command: agent-browser click [job-title-ref]
   - Or click "View job" or job card itself
```

### STEP 5: EXTRACT JOB DETAILS FROM INDIVIDUAL LISTING
```
1. Take snapshot of individual job page
   - Command: agent-browser snapshot -i

2. Extract all available job information:
   - Job title (heading level 1 or prominent text)
   - Company name
   - Location/city
   - Work mode (Hybrid, Remote, On-site, On-site)
   - Experience level required (if displayed)
   - Date posted (how long ago)
   - Salary (if displayed)
   - Job description (full text)
   - Required skills list
   - Preferred skills list
   - Application type (Easy Apply, External page, etc.)
   - Job URL (if available/extracted)

3. Identify application method:
   - "Apply on company website" → External career page
   - "Easy Apply" → LinkedIn's native application
   - "Save job" → Just bookmarking

4. Extract application form fields if present:
   - Email field
   - Job category/category selection
   - Location selection
   - Technology/Department subdivision
   - Any required checkboxes or radio buttons
```

### STEP 6: APPLY FOR THE JOB
```
1. Determine application type:
   - If Easy Apply: Look for "Apply" button, may need to click
   - If External page: Look for "Apply on company website" button
   - If Save only: No application needed, just note as "saved"

2. Fill application form fields (if present):
   - **Email field**: Fill with user's email from profile
     - Command: agent-browser fill [email-ref] "user-email@gmail.com"
   
   - **Job category**: Select matching category
     - Command: agent-browser fill [category-ref] "Technology" or appropriate category
   
   - **Location selection**: Choose work location
     - Command: agent-browser fill [location-ref] "City, State" or select from dropdown
   
   - **Technology/Department**: Select relevant department
     - Command: agent-browser click [tech-ref] or fill field
   
   - **Additional fields**: Any other required form fields
     - Fill with appropriate information from user profile

3. Submit the application
   - Find and click "Submit" or "Apply" button
   - Command: agent-browser click [submit-ref] or [apply-ref]
   
   - Or if multiple steps: Complete all fields first, then submit

4. Handle confirmation dialogs (if present):
   - Look for "Are you sure?" or confirmation prompts
   - Make decisions based on minimal data sharing preference
   - Select conservative/private options when asked for permissions

5. Wait for result
   - Look for success confirmation
   - Look for "Application submitted" message
   - Look for redirect to thank-you page

6. Take snapshot after application
   - Command: agent-browser snapshot -i
   - Document: Application status, any confirmation messages
```

### STEP 7: RETURN TO JOB SEARCH PAGE
```
1. Close application tab/window
   - Command: agent-browser close (if new tab)
   - Or: Use browser back button

2. Take snapshot of job search page
   - Command: agent-browser snapshot -i

3. Verify we're back on search results
   - Look for job list, pagination, filter options
   - Confirm previous job is no longer highlighted or is marked "viewed"

4. (Optional) Dismiss or mark job as viewed
   - Look for "Dismiss" button on job card
   - Command: agent-browser click [dismiss-ref] if available

5. Continue to next job or exit workflow
   - Either repeat Steps 4-7 for next job
   - Or proceed to Step 8
```

### STEP 8: REPEAT OR COMPLETE
```
1. For next job:
   - Go to Step 4 (identify and select target jobs)
   - Or select next job from filtered list

2. Continue applying until desired number of jobs
   - Set limit: e.g., apply to 5-10 jobs per session
   - Or apply to all matching jobs found

3. Generate summary/report
   - List all jobs applied to
   - Note application dates and statuses
   - Save results for user review

4. Exit workflow
   - Return to LinkedIn homepage or continue other tasks
```

---

## KEY ELEMENTS TO IDENTIFY IN SNAPSHOTS

### Common LinkedIn Job Page Elements:
```
- Job title heading (level 1 or 2)
- Company name link
- Location text
- Date posted ("Posted 1 day ago", "Posted 2 weeks ago", etc.)
- "Easy Apply" badge/button
- "Save the job" button
- "Apply on company website" button
- Job description section
- Skills tags/chips
- Work mode badges (Hybrid, Remote, On-site)
- Salary information (if available)
- Application form fields (if visible)
- "Dismiss" button (to remove viewed job from list)
- Pagination controls (Page 1, 2, 3... Next)
- Filter application buttons (after initial filter setup)
```

### Application Form Fields (when present):
```
- Email textbox (required)
- Job Category combobox
- Location combobox with city/state options
- Technology/Department subdivision
- "Add" button (may be disabled until all fields complete)
- "Sign up" / "Create account" options
- "Privacy policy" links
- "Home" / "Navigate" links
```

### Filter Elements (after initial setup):
```
- Date posted checkboxes/radios
- "LinkedIn Apply" option
- "Under 10 applicants" option
- "Learn more" about job ranking
- Location selection dropdown
- Experience level selection
- Work mode selection
```

---

## TROUBLESHOOTING COMMON ISSUES

### 1. Dynamic Element References
**Problem**: Element refs (e.g., e1, e45) change on each page load/snapshot
**Solution**: Always take fresh snapshot before acting, never hard-code refs

### 2. Covering Dialogs/Banners
**Problem**: Buttons covered by divs, modals, or sticky headers
**Solution**: 
- Take snapshot to identify covering element
- Click covering element first, then target button
- Or use Esc key to dismiss modals
- Look for "Skip" or "Close" buttons

### 3. Sign-In Walls on External Sites
**Problem**: Clicking "Apply" redirects to company career portal requiring login
**Solution**:
- Per user instructions: Do not sign in on external job portals
- Stop at sign-in page and return to LinkedIn"

### 4. "Add" Button Disabled
**Problem**: Application form "Add" button remains disabled
**Solution**:
- Check all required fields are filled
- Verify correct values in dropdowns
- May need to fill fields in specific order
- Check for error messages or tooltips

### 5. No Jobs Found or Filtered Out
**Problem**: Search returns few or no jobs matching criteria
**Solution**:
- Broaden search keywords
- Remove some filters temporarily
- Try alternative job titles
- Adjust date range filters
- Check location spelling/variations

---

## DATA STRUCTURE FOR JOB RECORDS

Each job processed should record:

```json
{
  "id": "job-unique-id",
  "title": "Job Title",
  "company": "Company Name",
  "location": "Job Location",
  "workMode": "Hybrid/Remote/On-site/Not specified",
  "experience": "Years required or Not specified",
  "salary": "Salary range or null",
  "requiredSkills": ["skill1", "skill2", ...],
  "preferredSkills": ["skill1", "skill2", ...],
  "description": "Job description text",
  "datePosted": "X days ago / X weeks ago / X months ago",
  "datePostedFilter": "within 24 hours / within 7 days / within 2 weeks / within 4 weeks / outside scope",
  "applicationType": "Easy Apply / External career page / Save only / Not applied",
  "url": "LinkedIn search URL or job URL",
  "status": "Applied / Saved / Dismissed / Sign-in required / Failed",
  "matchScore": 0-100 (if matching against profile),
  "matchCategory": "A/B/C/D or Strong/Good/Possible/Low"
}
```

---

## WORKFLOW EXECUTION NOTES

### For Model-Agnostic Implementation:
1. **Always snapshot before acting**: Never assume element refs persist
2. **Verify page state after each action**: Take snapshot to confirm navigation
3. **Handle dynamic content**: Elements load dynamically, wait/interact accordingly
4. **Error handling**: Have fallback paths if elements not found
5. **User confirmation points**: Mark where user input/decision needed
6. **Privacy considerations**: Note where data sharing decisions required

### Session Management:
- Keep track of job count applied to in current session
- Save progress periodically (after each job application)
- Know when to stop (user-set limit, no more matching jobs, time limit)
- Return clean state to LinkedIn job search page

### Integration Points:
- Can integrate with profile/resume data sources
- Can feed into matching/scoring systems
- Can save results to JSON/markdown files
- Can generate user reports of applications submitted

---

## WORKFLOW END POINT

The workflow completes when:
- User-specified number of jobs applied to
- No more matching jobs found after filtering
- User stops the process
- External barriers encountered (sign-in walls, captchas, etc.)

**After completion**: User has list of jobs applied to, with details for each, and can manually follow up as needed.

---

Example flow:
D:\job-search-agent>agent-browser open https://www.linkedin.com
D:\job-search-agent>agent-browser snapshot -i
D:\job-search-agent>agent-browser click @e7

find this for linkedin login:
- textbox "Email or phone" [required, ref=e38]
- textbox "Password" [required, ref=e39]
- button "Sign in" [ref=e42]

D:\job-search-agent>agent-browser fill @<email ref> "<emailId>"
D:\job-search-agent>agent-browser fill @<password ref> "<password>"
D:\job-search-agent>agent-browser click @<signin button ref>

---

Original user credentials to login into linkedin:
Email Id: kashyapdr044@gmail.com
Password: 12801280

Gmail Creddentials if model need to send mail:
from G-mail account: kashyapdr044@gmail.com
TO G-mail account: rathodkashyap044@gmail.com

---

please maintain commmands and what you have done in text files.

## Job Discovery, Filtering, and Application Flow

In each automation round:

* Search and analyze at least **10+ relevant jobs**.
* Evaluate each job based on:

  * Profile and resume
  * Skills and experience
  * Job requirements
  * Location preferences
  * Salary/CTC expectations
  * Other criteria defined in the workflow files
* Filter out unsuitable jobs and record the reason.

Do not wait until 10+ suitable jobs have been collected before starting applications.

Instead, follow this continuous workflow:

**Find a job → Analyze → Filter → If suitable, apply → Record the result → Find the next job**

Continue searching and processing jobs until:

* At least **10+ relevant jobs have been searched**, and
* At least **10+ suitable jobs have been successfully processed for application**, where application automation is supported and all required information is available.

If a suitable job cannot be applied to automatically, record the reason and continue searching for another suitable job.

Do not stop if the first 10 searched jobs do not result in 10 successful applications. Continue finding additional jobs until the target is reached.

## Application Phase

For every suitable job:

* Apply as soon as it passes the filtering criteria.
* Do not wait to create a batch of 10 jobs.
* Use the information from my profile, resume, preferences, and workflow files.
* Record every application immediately after submission.
* Check existing application records to prevent duplicate applications.
* Clearly log failed, skipped, or incomplete applications with the reason.

The target for each automation round is to complete applications for **10+ suitable jobs**.

## Automation Continuity

The automation should run with minimal or no active user interaction.

Maintain progress and checkpoints in the workflow and log files so the automation can safely resume if interrupted.

If continuous execution is required while the laptop is in actual sleep mode, use an appropriate remote, cloud, or always-on execution environment, because local browser automation will normally pause when the laptop sleeps.

When resuming after an interruption:

1. Read the existing workflow and log files.
2. Identify the last completed action.
3. Check which jobs have already been analyzed or applied to.
4. Resume from the correct point.
5. Do not repeat completed applications or create duplicate records.

---
*This workflow document generated for transferability across AI models and job search automation scenarios. Adapt filters, keywords, and preferences to specific user profiles and job markets.*