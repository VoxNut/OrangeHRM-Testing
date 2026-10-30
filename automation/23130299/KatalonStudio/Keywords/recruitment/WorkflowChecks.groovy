package recruitment

import org.openqa.selenium.*
import java.time.LocalDate

/** State-specific workflow setup and history assertions. No candidate is borrowed from the demo. */
class WorkflowChecks extends CandidateChecks {
    static final String INITIAL = 'Application Initiated'
    static final String SHORT = 'Shortlisted'
    static final String REJECTED = 'Rejected'
    static final String SCHEDULED = 'Interview Scheduled'
    final Map<String,Map> workflowAttempts = [:]

    WorkflowChecks(String caseId) { super(caseId) }

    Map fixture(String state = INITIAL, String suffix = 'A') {
        Map record = (Map) EvidenceCapture.fixture { prepareWorkflowFixture(state, suffix) }
        capture('BEFORE_ACTION_WORKFLOW') // business starting state after setup has released screenshot quota
        return record
    }
    protected Map prepareWorkflowFixture(String state, String suffix) {
        Map vac = vacancy()
        Map d = create(draft(vac, suffix, [first:'Flow', last:'Nguyen' + token + suffix]))
        d.fixtureVacancy = vac
        stage(INITIAL); historySequence([INITIAL])
        if (state in [SHORT, SCHEDULED]) transition(d, 'Shortlist', SHORT, 'Shortlist ' + token)
        if (state == REJECTED) transition(d, 'Reject', REJECTED, 'Reject initial ' + token)
        if (state == SCHEDULED) {
            Map interview = interviewData('25')
            schedule(d, interview)
            d.interview = interview
        }
        // State-specific setup is complete. Re-enter the main module through Dashboard/Recruitment
        // before opening the exact owned profile required by the existing workflow assertions.
        list()
        reopen(d); stage(state)
        return d
    }
    String status() {
        String text = one("//p[starts-with(normalize-space(),'Status:')]").text.trim()
        return text.replaceFirst(/^Status:\s*/, '').trim()
    }
    void stage(String expected) {
        waitFor().until { status() == expected }
        assert status() == expected : 'Wrong workflow state'
    }
    List<Map<String,String>> history() {
        heading('Candidate History')
        List<Map<String,String>> rows = allRows('Description')
        assert rows && rows.every { it.Description?.trim() && it['Performed Date']?.trim() } : 'History event/date missing'
        return rows
    }
    List<String> historyEvents() { return history().collect { it.Description } }
    static String historyEvent(String description) {
        String text = (description ?: '').toLowerCase().replaceAll(/\s+/, ' ').trim()
        if (text =~ /\bshortlist(?:ed)?\b/) return SHORT
        if (text =~ /\breject(?:ed)?\b/) return REJECTED
        if ((text =~ /\binterview\b/) && (text =~ /\bschedul(?:e|ed)\b/)) return SCHEDULED
        if (text =~ /\b(added|assigned|applied|initiated)\b/) return INITIAL
        return null // Unknown translations/events require review; never classify by current status.
    }
    static List<String> historyChain(List<String> descriptions) {
        List<String> classified = descriptions.collect { historyEvent(it) }
        assert classified && classified.every { it != null } : 'Unrecognized History description; inspect actual translated event'
        // Add Candidate (16) and Assign Vacancy (1) can be separate initial events in source.
        List<String> chain = []
        classified.each { value -> if (!(value == INITIAL && chain && chain[-1] == INITIAL)) chain << value }
        return chain
    }
    static boolean historyAppended(List<String> before, List<String> after, String target) {
        List<String> added = new ArrayList<String>(after)
        for (String previous : before) { if (!added.remove(previous)) return false }
        return !added.empty && added.every { historyEvent(it) == target }
    }
    void historySequence(List<String> expected) {
        List<String> actual = historyChain(historyEvents())
        assert actual == expected || actual.reverse() == expected : 'Wrong History event chain: expected=' + expected + '; actual=' + actual
    }
    void noTransition(Map d, String oldState, List<String> before) {
        reopen(d); stage(oldState)
        assert historyEvents() == before : 'Cancelled/invalid action persisted a history event'
    }
    void actionForm(Map d, String action) {
        requireOwned(d); reopen(d)
        String actionButton = "//div[${cls('orangehrm-recruitment-actions')}]" + button(action).substring(1)
        ready()
        List<WebElement> allowed = visible(actionButton)
        assert allowed.size() == 1 && allowed[0].enabled : 'Server-rendered allowed action missing/disabled: ' + action
        allowed[0].click() // Native UI; source doWorkflow navigates to a dedicated action page.
        heading(action == 'Schedule Interview' ? action : action + ' Candidate')
        assert actionCandidateId(driver().currentUrl) == candidateId(d.url.toString()) : 'Action page Candidate ID differs'
        assert candidateNameMatches(value(field('Candidate')), fullName(d)) : 'Action Candidate name differs'
        equalsValue(field('Vacancy'), d.vacancy.toString())
    }
    static String actionCandidateId(String url) {
        try {
            URI uri = new URI(url)
            if (uri.scheme != 'https' || uri.host != new URI(BASE).host || uri.port != -1 || uri.userInfo ||
                uri.path != '/web/index.php/recruitment/changeCandidateVacancyStatus') return null
            List<String> ids = (uri.rawQuery ?: '').split('&').findAll { it.startsWith('candidateId=') }.collect { it.substring(12) }
            return ids.size() == 1 && ids[0] ==~ /[1-9]\d*/ ? ids[0] : null
        } catch (Exception ignored) { return null }
    }
    protected boolean workflowRecordProof(Map d, String target) {
        Map state = pageState()
        if (state.login) throw new IllegalStateException('[AUTH_SESSION_LOST_AFTER_SUBMIT] Workflow outcome uncertain; never replay action')
        if (!sameRoute(driver().currentUrl, d.url.toString()) || !state.authenticated || !state.domComplete || state.loading || state.challenge || state.networkError) return false
        if (!shown(field('Email'))) return false
        return value(field('Email')) == d.email && candidateNameMatches(value(FIRST) + ' ' + value(MIDDLE) + ' ' + value(LAST), fullName(d)) && status() == target
    }
    protected void saveWorkflowAction(Map d, String operation, String target, Closure postcondition) {
        requireOwned(d)
        assert postcondition != null : 'Workflow verification requires operation-specific assertions'
        String key = d.url.toString() + ':' + operation
        assert !workflowAttempts.containsKey(key) : 'Workflow action already attempted; reconcile the owned record, never resubmit'
        Map auth = pageState()
        if (auth.login || !auth.authenticated) throw new IllegalStateException('[ENVIRONMENT / AUTHENTICATION BLOCKED] Workflow form lost authentication before Save; no submit sent. Complete manual login and rerun independently.')
        Map attempt = [kind:'workflow-action', operation:operation, candidateId:candidateId(d.url.toString()),
            url:d.url, ownership:token, state:'prepared', submitted:false]
        workflowAttempts[key] = attempt
        track(new LinkedHashMap(attempt))
        Map context = [phase:'WORKFLOW_' + operation.toUpperCase().replace(' ', '_')]
        context.beforeSubmit = { attempt.state = 'submitted'; track(new LinkedHashMap(attempt)) }
        context.afterSubmit = { attempt.submitted = true; track(new LinkedHashMap(attempt)) }
        try {
            saveSuccess('Successfully Updated', context) { diagnostic -> workflowRecordProof(d, target) }
            attempt.state = 'observed'; track(new LinkedHashMap(attempt))
            attempt.state = 'identified'; track(new LinkedHashMap(attempt))
            reopen(d); verifyDetails(d); stage(target) // Fresh GET, same Candidate ID and immutable fields.
            postcondition.call() // History/interview assertions must complete before marking this action verified.
            attempt.state = 'verified'; track(new LinkedHashMap(attempt))
        } catch (Throwable failure) {
            attempt.state = context.saveAttempted == true ? 'uncertain' : 'prepared'
            track(new LinkedHashMap(attempt)); throw failure
        }
    }
    void transition(Map d, String action, String target, String note) {
        requireOwned(d)
        reopen(d)
        String previous = status()
        assert (action == 'Shortlist' && previous == INITIAL) || (action == 'Reject' && previous in [INITIAL, SHORT])
        List<String> before = historyEvents()
        actionForm(d, action)
        boolean hasNotes = shown(area('Notes'))
        if (hasNotes) replace(area('Notes'), note)
        capture('BEFORE_' + action.toUpperCase())
        saveWorkflowAction(d, action, target) {
            List<String> after = historyEvents()
            assert historyAppended(before, after, target) : 'Previous history lost or new target event absent'
            List<String> chain = previous == SHORT ? [INITIAL, SHORT, target] : [INITIAL, target]
            historySequence(chain)
        }
        if (hasNotes) d[action.toLowerCase() + 'Note'] = note
        track([kind:'candidate-state', url:d.url, email:d.email, from:previous, to:target, notes:hasNotes ? note : '(optional field not present)'])
    }
    void cancelTransition(Map d, String action, String expectedState, String note) {
        reopen(d); stage(expectedState)
        List<String> before = historyEvents()
        actionForm(d, action)
        if (shown(area('Notes'))) replace(area('Notes'), note)
        buttonClick('Cancel')
        noTransition(d, expectedState, before)
    }
    Map interviewData(String suffix, Map overrides = [:]) {
        String interviewer = config('interviewerA', '').toString().trim()
        if (!overrides.containsKey('interviewers') && !interviewer) blocked('Set interviewerA to an exact, active employee suggestion before scheduling.')
        if (config('timeFormat', '').toString() == '24h') blocked('Configured 24h display contradicts supplied OxdTimeInput (hh:mm a); inspect deployment before proceeding.')
        LocalDate original = LocalDate.parse(suffix == '12' ? '2026-11-12' : '2026-11-10')
        LocalDate resolved = original.isAfter(LocalDate.now()) ? original : LocalDate.now().plusDays(14)
        Map data = [title:'Interview ' + token + suffix, interviewers:[interviewer], date:resolved.toString(),
            time:suffix == '12' ? '14:00' : '10:30', notes:suffix == '12' ? 'Đánh giá kỹ năng QA' : 'First round']
        data.putAll(overrides)
        track([kind:'interview-input', workbookDate:original.toString(), resolved:data,
            dateRule:'Test Data DATE_WINDOW allows a future valid date at execution; invalid dates are never adjusted'])
        return data
    }
    void scheduleForm(Map d) {
        requireOwned(d); reopen(d); stage(SHORT)
        actionForm(d, 'Schedule Interview')
        equalsValue(field('Current Status'), SHORT)
    }
    String timeText(String time24) {
        assert time24 ==~ /(?:[01]\d|2[0-3]):[0-5]\d/ : 'Invalid internal 24h interview time'
        int hour = Integer.parseInt(time24.substring(0, 2))
        return String.format('%02d', hour % 12 == 0 ? 12 : hour % 12) + time24.substring(2) + (hour < 12 ? ' AM' : ' PM')
    }
    void setTime(String time24, boolean invalid = false) {
        String display = invalid ? time24 : timeText(time24)
        replace(field('Time'), display, !invalid)
        if (!invalid) equalsValue(field('Time'), display)
    }
    static String interviewerInput(int index) {
        assert index >= 0 && index < 5
        return "(//div[${cls('orangehrm-recruitment-interviewer-input')}])[${index + 1}]//div[${cls('oxd-autocomplete-wrapper')}]//input"
    }
    static String interviewerOptions(int index) {
        return interviewerInput(index) + "/ancestor::div[${cls('oxd-autocomplete-wrapper')}][1]" +
            "//*[@role='listbox' and ${cls('oxd-autocomplete-dropdown')}]//*[@role='option' and ${cls('oxd-autocomplete-option')}]"
    }
    static boolean interviewerOptionMatches(String optionText, String expected) {
        if (autocompletePlaceholder(optionText)) return false
        String normOpt = VacancyChecks.normalizeManagerName(optionText)
        if (VacancyTable.isExcludedManagerCandidate(normOpt)) return false
        String normExp = VacancyChecks.normalizeManagerName(expected)
        if (candidateNameMatches(normOpt, normExp)) return true
        List<String> parts = normOpt.tokenize(' ')
        String firstLast = parts.size() > 1 ? parts.first() + ' ' + parts.last() : normOpt
        return candidateNameMatches(firstLast, normExp)
    }
    String selectInterviewer(int index, String expected) {
        assert expected?.trim() && expected.length() < 100 : 'EmployeeAutocomplete requires a nonblank name shorter than 100 characters'
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(seconds).toNanos()
        WebElement input = selectionWait(deadline).until {
            List<WebElement> fields = visible(interviewerInput(index))
            assert fields.size() <= 1 : 'Ambiguous indexed interviewer field'
            return fields && fields[0].enabled ? fields[0] : null
        }
        input.sendKeys(Keys.chord(Keys.CONTROL, 'a'), Keys.BACK_SPACE)
        input.sendKeys(expected) // Full configured or discovered identity narrows the source's maximum five suggestions.
        boolean clicked = false
        String selectedLabel = expected
        selectionWait(deadline).until {
            List<WebElement> matches = visible(interviewerOptions(index)).findAll { interviewerOptionMatches(it.text, expected) }
            assert matches.size() <= 1 : 'Ambiguous employee identity; configure a uniquely identifiable interviewer'
            if (!matches || !matches[0].enabled) return false
            selectedLabel = VacancyChecks.normalizeManagerName(matches[0].text)
            matches[0].click(); clicked = true; return true
        }
        selectionWait(deadline).until {
            List<WebElement> fields = visible(interviewerInput(index))
            String actualVal = fields.size() == 1 ? fields[0].getDomProperty('value') : ''
            if (clicked && fields.size() == 1 && (candidateNameMatches(actualVal, selectedLabel) || candidateNameMatches(actualVal, expected)) &&
                visible(interviewerOptions(index)).empty &&
                visible(interviewerInput(index) + "/ancestor::div[${cls('oxd-input-group')}][1]//*[${cls('oxd-input-field-error-message')}]").empty) {
                selectedLabel = VacancyChecks.normalizeManagerName(actualVal ?: selectedLabel)
                return true
            }
            return false
        }
        String settingKey = index == 0 ? 'interviewerA' : 'interviewerB'
        Map verifiedRecord = [
            state: 'VERIFIED',
            displayName: selectedLabel,
            selected: selectedLabel,
            verificationMethod: 'Schedule Interview InterviewerAutocomplete (/api/v2/recruitment/interviewers)',
            verifiedTimestamp: java.time.OffsetDateTime.now().toString(),
            executionId: executionContext.executionId
        ]
        executionContext.interviewerDiscovery[settingKey] = verifiedRecord
        executionContext.interviewerDiscovery.status = 'VERIFIED'
        executionContext.persist()
        return selectedLabel
    }
    void fillInterview(Map data) {
        replace(field('Interview Title'), data.title.toString())
        List<String> names = new ArrayList<String>((data.interviewers as List<String>) ?: [])
        assert names.size() <= 5 && names.collect { it.trim().replaceAll(/\s+/, ' ') }.unique(false).size() == names.size() : 'One to five distinct employee identities only'
        Set<String> chosenNormalized = new LinkedHashSet<>()
        names.eachWithIndex { String name, int index ->
            if (index > 0) buttonClick('Add another')
            assert visible("//div[${cls('orangehrm-recruitment-interviewer-input')}]").size() == index + 1 : 'Add another did not create exactly one interviewer field'
            String settingKey = index == 0 ? 'interviewerA' : 'interviewerB'
            boolean isManual = (executionContext.settings?.get(settingKey) ?: '').toString().trim() != ''
            List<String> pool = [name]
            if (!isManual) {
                List<Map> discovered = (executionContext.interviewerDiscovery?.candidates ?: executionContext.managerDiscovery?.candidates ?: []) as List<Map>
                discovered.each { Map c ->
                    String cn = VacancyChecks.normalizeManagerName(c.candidateName?.toString())
                    if (cn && !pool.any { VacancyChecks.normalizeManagerName(it) == cn }) pool << cn
                }
            }
            String verified = null
            Throwable lastErr = null
            for (String candidate in pool) {
                String normCand = VacancyChecks.normalizeManagerName(candidate)
                if (chosenNormalized.contains(normCand)) continue
                try {
                    verified = selectInterviewer(index, candidate)
                    if (!chosenNormalized.contains(VacancyChecks.normalizeManagerName(verified))) {
                        chosenNormalized << VacancyChecks.normalizeManagerName(verified)
                        names[index] = verified
                        break
                    }
                } catch (Throwable err) {
                    lastErr = err
                    if (isManual) throw err
                }
            }
            if (!verified) throw (lastErr ?: new IllegalStateException('[BLOCKED / NO_VERIFIED_INTERVIEWER] Could not verify active interviewer on Schedule Interview'))
        }
        data.interviewers = names
        if (data.date) enterDate('Date', data.date.toString())
        else replace(field('Date'), '')
        setTime(data.time.toString())
        if (data.notes) replace(area('Notes'), data.notes.toString())
    }
    void schedule(Map d, Map data) {
        scheduleForm(d); fillInterview(data)
        List<String> before = [INITIAL, SHORT]
        capture('BEFORE_SCHEDULE_SAVE')
        saveWorkflowAction(d, 'Schedule Interview', SCHEDULED) {
            assert status() != SHORT
            historySequence(before + [SCHEDULED])
            verifyInterview(d, data)
        }
        track([kind:'candidate-state', url:d.url, email:d.email, from:SHORT, to:SCHEDULED, interview:data])
    }
    void openEvent(Map d, String description) {
        reopen(d)
        history()
        WebElement t = table('Description')
        List<String> headers = t.findElements(By.xpath(".//div[@role='columnheader']")).collect { it.text.trim() }
        int index = headers.indexOf('Description') + 1
        List<WebElement> rows = t.findElements(By.xpath(ROWS)).findAll {
            historyEvent(it.findElement(By.xpath("./div[@role='cell'][${index}]")).text.trim()) == description
        }
        assert rows.size() == 1 : 'Event is not unique on own Candidate History: ' + description
        List<WebElement> view = rows[0].findElements(By.xpath(".//button[.//i[${cls('bi-pencil-fill')}]]")).findAll { it.displayed && it.enabled }
        if (view.size() != 1) blocked('History Edit/details unavailable for ' + description + '; source gates pencil by event.editable. No event assertion is omitted.')
        view[0].click(); heading('View Action History')
        assert historyCandidateId(driver().currentUrl) == candidateId(d.url.toString()) : 'History details opened a different Candidate'
        assert candidateNameMatches(value(field('Candidate')), fullName(d))
        equalsValue(field('Vacancy'), d.vacancy.toString())
        Map labels = [(SHORT):'Shortlist', (REJECTED):'Reject', (SCHEDULED):'Schedule Interview']
        equalsValue(field('Performed Action'), labels[description].toString())
        assert value(field('Performed By')).trim() && value(field('Performed Date')).trim() : 'History actor/date absent'
    }
    static String historyCandidateId(String url) {
        try {
            URI uri = new URI(url)
            if (uri.scheme != 'https' || uri.host != new URI(BASE).host || uri.port != -1 || uri.userInfo) return null
            def match = uri.path =~ /^\/web\/index\.php\/recruitment\/candidateHistory\/([1-9]\d*)\/([1-9]\d*)$/
            return match.matches() ? match.group(1) : null
        } catch (Exception ignored) { return null }
    }
    void verifyInterview(Map d, Map data) {
        openEvent(d, SCHEDULED)
        equalsValue(field('Interview Title'), data.title.toString())
        String date = dateText(data.date.toString(), one(field('Date')).getAttribute('placeholder'))
        equalsValue(field('Date'), date)
        equalsValue(field('Time'), timeText(data.time.toString()))
        if (data.notes) equalsValue(area('Notes'), data.notes.toString())
        List<String> people = visible("//div[${cls('orangehrm-recruitment-interviewer-input')}]//div[${cls('oxd-autocomplete-wrapper')}]//input").collect { it.getDomProperty('value') }
        List<String> expectedPeople = data.interviewers as List<String>
        assert people.size() == expectedPeople.size() && people.every { person -> expectedPeople.count { candidateNameMatches(person, it) } == 1 } &&
            people.collect { it.trim().replaceAll(/\s+/, ' ') }.unique().size() == people.size() :
            'Persisted interviewer identities differ: ' + people
        // The event came from the exact owned candidate's history; recheck numeric candidate URL and identity afterwards.
        capture('INTERVIEW_DETAILS_VERIFIED')
        reopen(d); verifyDetails(d); stage(SCHEDULED)
    }
    void verifyEventNote(Map d, String event, String expected) {
        openEvent(d, event)
        equalsValue(area('Notes'), expected)
        capture('HISTORY_NOTE_VERIFIED')
        reopen(d)
    }
    void scheduleNegative(Map d, Map data, String problem) {
        reopen(d); stage(SHORT)
        List<String> before = historyEvents()
        scheduleForm(d)
        fillInterview(data)
        String path
        String regex
        switch (problem) {
            case 'title': path = field('Interview Title'); regex = 'Required'; break
            case 'interviewer': path = field('Interviewer'); regex = 'Required'; break
            case 'date': path = field('Date'); regex = 'Required'; break
            case 'invalidDate':
                path = field('Date'); regex = '(?i).*(valid date|invalid date|invalid format|yyyy|dd.mm).*'
                String invalid = dateText('2027-02-31', one(path).getAttribute('placeholder'))
                replace(path, invalid, false)
                if (value(path) != invalid) {
                    assert value(path) == '' : 'Invalid date was coerced into another nonempty value: ' + value(path)
                    capture('DATE_INPUT_BLOCKED'); buttonClick('Cancel'); noTransition(d, SHORT, before); return
                }
                break
            case 'invalidTime':
                path = field('Time'); regex = '(?i).*(valid (?:date|time)|invalid time|invalid format|hh.mm).*'
                setTime('25:99', true)
                if (value(path) != '25:99') {
                    assert value(path) == '' : 'Invalid time was coerced into another nonempty value: ' + value(path)
                    capture('TIME_INPUT_BLOCKED'); buttonClick('Cancel'); noTransition(d, SHORT, before); return
                }
                break
            case 'unknownInterviewer':
                path = field('Interviewer'); regex = '(?i)(Required|.*invalid.*|.*valid.*)'
                String missing = 'DELETED_INTERVIEWER_' + token
                one(path).sendKeys(missing)
                waitFor().until {
                    List<WebElement> options = visible(interviewerOptions(0))
                    return options.size() == 1 && options[0].text.trim().equalsIgnoreCase('No records found')
                }
                one(path).sendKeys(Keys.TAB) // Deliberately do not select an employee.
                break
            default: throw new IllegalArgumentException('Unknown negative schedule target: ' + problem)
        }
        String key = d.url.toString() + ':invalid-' + problem
        assert !workflowAttempts.containsKey(key) : 'Invalid schedule already attempted; never replay'
        Map attempt = [kind:'workflow-validation', operation:problem, ownership:token, url:d.url, state:'submitted']
        workflowAttempts[key] = attempt; track(new LinkedHashMap(attempt))
        try {
            buttonClick('Save'); error(path, regex)
            assert shown(field('Interview Title')) && actionCandidateId(driver().currentUrl) == candidateId(d.url.toString()) : 'Invalid schedule unexpectedly navigated away'
            noTransition(d, SHORT, before)
            attempt.state = 'verified'; attempt.rejected = true; track(new LinkedHashMap(attempt))
        } catch (Throwable failure) {
            attempt.state = 'uncertain'; track(new LinkedHashMap(attempt)); throw failure
        }
    }
    void inspectActionMenus() {
        String xp = "//button[@aria-haspopup='menu'] | //button[.//i[${cls('bi-three-dots-vertical')} or ${cls('bi-three-dots')}]]"
        List<WebElement> menus = visible(xp)
        if (menus.size() > 1) blocked('Multiple action menus; scope the live Application Stage menu before validating forbidden actions.')
        if (menus) menus[0].click()
    }
    void forbiddenAction(String text) {
        assert owned.containsKey(driver().currentUrl) : 'Allowed-action assertion requires this case\'s exact owned Profile'
        ready()
        String action = "//div[${cls('orangehrm-recruitment-actions')}]//button[normalize-space()=${lit(text)}]"
        assert visible(action).empty : 'Forbidden action is visible in this state: ' + text
    }
}
