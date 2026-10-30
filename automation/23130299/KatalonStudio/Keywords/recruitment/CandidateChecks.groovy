package recruitment

import org.openqa.selenium.*
import java.time.LocalDate
import java.security.MessageDigest

/** Independent Candidates fixtures and persistence oracles; never selects a demo candidate for mutation. */
class CandidateChecks extends RecruitmentUi {
    static final String FIRST = "//input[@name='firstName']"
    static final String MIDDLE = "//input[@name='middleName']"
    static final String LAST = "//input[@name='lastName']"
    static final String CONSENT = group('Consent to keep data') + "//input[@type='checkbox']"
    static final String EMAIL_FORMAT_PATTERN = '(?i)(?:.*valid.*email.*|Expected format: admin@example\\.com)'
    // app.js go validator, used by SaveCandidate and CandidateProfile; no backend claim.
    static final String PHONE_FORMAT_PATTERN = '(?i).*(?:phone|number).*'
    static boolean sourcePhoneAccepts(String value) {
        return !value || (value.length() <= 25 && value ==~ /^[0-9+\-\/() ]+$/)
    }
    final Map<String, Map> owned = [:]
    // Cache attempts before submission as well as confirmed records. Never replace an uncertain attempt.
    private final Map<String, Map> preparedVacancies = [:]

    CandidateChecks(String caseId) { super(caseId) }

    void preflightResume(String extension, boolean supported = true, boolean boundary = false) {
        measure('ASSERTION') {
            ResumeFiles assets = new ResumeFiles(this)
            if (boundary) assets.requireBoundaryConfig() // No browser or server fixture if known config is absent.
            list(); buttonClick('Add'); waitForRoute(ROOT + 'addCandidate', 'Add Candidate')
            Map rule = assets.readRule()
            assert rule.extensions.contains(extension) == supported : 'Resume format rule disagrees with case input'
            if (boundary) assets.limit()
            track([kind:'resume-preflight', extension:extension, supported:supported,
                exactLimitVerified:boundary, lowerBoundBytes:rule.lowerBoundBytes])
        }
    }

    Map vacancy(String suffix = 'A') {
        return (Map) EvidenceCapture.fixture { measure('VACANCY_FIXTURE') { prepareVacancy(suffix) } }
    }
    protected Map prepareVacancy(String suffix) {
        ConfigurationPreflight.requireConfigured('hiringManagerName',caseId)
        ensureCandidatesStarted()
        String name = 'VAC_23130299_' + token + '_' + suffix
        Map fixture = preparedVacancies[suffix]
        if (!fixture) {
            fixture = [name:name, url:null, ownership:token, ownerExecutionId:executionContext.executionId, saveAttempted:false, submitted:false,
                confirmed:false, cleanup:'Pending manual cleanup; verify exact URL/name/ownership first']
            preparedVacancies[suffix] = fixture
            recordVacancy(fixture, 'prepared')
        }
        assert fixture.ownership == token && fixture.name == name : 'Only this case\'s exact Vacancy attempt may be reused'
        Map context = [phase:'FIXTURE_PREPARE', fixtureName:name, saveAttempted:fixture.saveAttempted,
            submitted:fixture.submitted, recordConfirmed:fixture.confirmed]
        long started = System.nanoTime()
        try {
            if (fixture.saveAttempted || fixture.confirmed) {
                // Recovery is read-only. No second Save, even if the previous click had an unknown outcome.
                fixture.confirmed = false
                fixture.persistedVerified = false
                context.recordConfirmed = false
                if (!fixture.url) vacancyRecordProof(fixture)
                if (!fixture.url) blocked('Uncertain Vacancy submission: ' + name +
                    '. No exact owned numeric URL is known. Inspect ledger/list manually; this helper will not submit again.')
                context.phase = 'FIXTURE_SAVE'
                verifyVacancy(fixture, context)
            } else {
                println('[FIXTURE SETUP] ' + caseId + ': create own Vacancy ' + suffix + '; return to Candidates afterwards.')
                go('addJobVacancy', 'Add Vacancy')
                String job = config('jobTitle', 'QA Engineer').toString()
                String description = 'ASM7 disposable fixture ' + caseId
                choose('Job Title', job)
                replace(field('Vacancy Name'), name)
                replace(area('Description'), description)
                replace(field('Number of Positions'), '3')
                String manager = new VacancyChecks(new RecorderBridge(caseId)).chooseConfiguredManager()
                String switches = "//div[${cls('orangerhrm-switch-wrapper')}]"
                checkbox(switches + "[.//*[normalize-space()='Active']]//input[@type='checkbox']", true)
                checkbox(switches + "[.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input[@type='checkbox']", false)
                fixture.manager = manager
                fixture.expected = [name:name, jobTitle:job, description:description, manager:manager,
                    positions:'3', active:true, published:false].asImmutable()
                recordVacancy(fixture, 'prepared')
                context.phase = 'FIXTURE_SAVE'
                context.beforeSubmit = {
                    fixture.saveAttempted = true
                    recordVacancy(fixture, 'submitted') // Attempt journaled before click; submitted=false until click returns.
                }
                context.afterSubmit = { fixture.submitted = true; recordVacancy(fixture, 'submitted') }
                saveSuccess('Successfully Saved', context) { diagnostic -> vacancyRecordProof(fixture) }
                // Even a toast is not enough to mark a fixture confirmed: require numeric URL + all expected fields.
                awaitVacancyRecord(fixture, context)
                verifyVacancy(fixture, context) // Fresh GET and all seven fields, not merely the submitting DOM.
            }
            fixture.confirmed = true
            context.recordConfirmed = true
            if (fixture.state != 'confirmed') countFixture('vacancy')
            recordVacancy(fixture, 'confirmed')
            navigationLog('FIXTURE_CONFIRMED', saveDiagnostics('Successfully Saved', context))
        } catch (Throwable error) {
            if (fixture.confirmed != true) recordVacancy(fixture, fixture.saveAttempted ? 'uncertain' : 'prepared')
            saveFailure(context, started, error)
            throw error
        }
        openCandidatesFromMenu()
        return fixture
    }
    static boolean vacancyRecordUrl(String url) {
        return url != null && url ==~ /https:\/\/opensource-demo\.orangehrmlive\.com\/web\/index.php\/recruitment\/addJobVacancy\/\d+/
    }
    static boolean vacancyValuesMatch(Map expected, Map actual) {
        List<String> keys = ['name', 'jobTitle', 'description', 'positions', 'active', 'published']
        return expected != null && actual != null && keys.every { expected.containsKey(it) && actual.containsKey(it) && expected[it] == actual[it] } &&
            expected.manager?.toString()?.trim() && VacancyChecks.normalizeManagerName(expected.manager.toString()) ==
                VacancyChecks.normalizeManagerName(actual.manager?.toString() ?: '')
    }
    static Map vacancyLedgerEntry(Map fixture, String state) {
        assert state in ['prepared', 'submitted', 'confirmed', 'uncertain']
        assert fixture.ownership && fixture.name?.toString()?.startsWith('VAC_23130299_' + fixture.ownership + '_')
        if (state == 'confirmed') assert fixture.confirmed == true && fixture.persistedVerified == true && vacancyRecordUrl(fixture.url?.toString()) :
            'Cannot confirm a fixture without exact persisted-record verification'
        if (state == 'submitted' || state == 'uncertain') assert fixture.saveAttempted == true
        return new LinkedHashMap(fixture) + [kind:state == 'confirmed' ? 'vacancy-saved' : 'vacancy',
            state:state, time:java.time.OffsetDateTime.now().toString()]
    }
    protected void recordVacancy(Map fixture, String state) {
        track(vacancyLedgerEntry(fixture, state)) // Snapshot each transition; do not mutate historical ledger entries.
        fixture.state = state
    }
    private Map vacancyValues() {
        String switches = "//div[${cls('orangerhrm-switch-wrapper')}]"
        Map paths = [name:field('Vacancy Name'), jobTitle:select('Job Title'), description:area('Description'),
            manager:field('Hiring Manager'), positions:field('Number of Positions'),
            active:switches + "[.//*[normalize-space()='Active']]//input[@type='checkbox']",
            published:switches + "[.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input[@type='checkbox']"]
        return (Map) ((JavascriptExecutor) driver()).executeScript('''
            const out={};
            for(const [key,xp] of Object.entries(arguments[0])) {
                const s=document.evaluate(xp,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
                if(s.snapshotLength!==1)return null;
                const n=s.snapshotItem(0);
                if(key==='active'||key==='published')out[key]=n.checked;
                else if(key==='jobTitle')out[key]=n.textContent.trim();
                else out[key]=n.value||'';
            }
            return out;
        ''', paths)
    }
    protected boolean vacancyRecordProof(Map fixture) {
        assert fixture.ownership == token : 'Record proof requires this case\'s fixture'
        String url = driver().currentUrl
        if (!vacancyRecordUrl(url) || (fixture.url && fixture.url != url)) return false
        // Retain observed numeric identity even while the replacement form is loading.
        // This is an uncertain attempt until every field and a fresh GET are verified below.
        if (!fixture.url) { fixture.url = url; recordVacancy(fixture, 'submitted') }
        Map state = pageState()
        if (!state.authenticated || !state.domComplete || state.loading || state.challenge || state.networkError ||
            !shown("//h6[normalize-space()='Edit Vacancy']") || !vacancyValuesMatch(fixture.expected as Map, vacancyValues())) return false
        return true
    }
    private void verifyVacancy(Map fixture, Map context) {
        assert fixture.ownership == token && fixture.url && fixture.name?.toString()?.startsWith('VAC_23130299_' + token + '_') :
            'Only this case\'s exact Vacancy fixture may be verified'
        assert fixture.url ==~ /https:\/\/opensource-demo\.orangehrmlive\.com\/web\/index.php\/recruitment\/addJobVacancy\/\d+/
        String name = fixture.name.toString()
        String manager = fixture.manager.toString()
        String switches = "//div[${cls('orangerhrm-switch-wrapper')}]"
        fixture.persistedVerified = false
        go(fixture.url.toString(), 'Edit Vacancy', true)
        // go() is a fresh GET of this owned numeric record. Restore the actual verification phase.
        context.waitingFor = 'Fresh owned numeric Vacancy URL + name/job/description/manager/positions/Active/Publish'
        beginSavePhase(context)
        navigationLog('FIXTURE_RECORD_REOPENED', [fixtureName:name, url:safeUrl(driver().currentUrl)])
        awaitVacancyRecord(fixture, context)
        equalsValue(field('Vacancy Name'), name)
        equalsValue(field('Number of Positions'), '3')
        new VacancyChecks(new RecorderBridge(caseId)).assertManagerValue(manager)
        equalsValue(area('Description'), fixture.expected.description.toString())
        assert one(select('Job Title')).text.trim() == fixture.expected.jobTitle
        assert driver().findElement(By.xpath(switches + "[.//*[normalize-space()='Active']]//input")).selected
        assert !driver().findElement(By.xpath(switches + "[.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input")).selected
        fixture.persistedVerified = true
    }
    private void awaitVacancyRecord(Map fixture, Map context) {
        waitFor().until {
            Map diagnostic = saveDiagnostics('Successfully Saved', context)
            if (saveEvidence(diagnostic) == 'rejected') throw new IllegalStateException('[FIXTURE RECORD REJECTED] ' +
                diagnostic.validation + '; ' + diagnostic.errorToasts)
            return vacancyRecordProof(fixture)
        }
    }

    Map draft(Map vac, String suffix = 'A', Map overrides = [:]) {
        Map d = [kind:'candidate', first:'An', middle:'', last:'Nguyen' + token + suffix,
            email:('an.' + token + '.' + suffix.toLowerCase() + '@example.com'), phone:'', keywords:'', notes:'',
            vacancy:vac?.name ?: '', vacancyUrl:vac?.url, consent:false, resume:null, url:null, ownership:token,ownerExecutionId:executionContext.executionId]
        d.putAll(overrides)
        assert d.email.endsWith('@example.com') || d.email in ['', 'quality.example.com', 'quality@']
        return d
    }
    static String fullName(Map d) { return [d.first, d.middle, d.last].findAll { it }.join(' ') }
    void openAdd(Map d) {
        track([kind:'candidate-intent', name:fullName(d), email:d.email, vacancy:d.vacancy, ownership:token])
        // Only this helper's already-started list may be reused. Add does not depend on list filters.
        ensureCandidatesStarted()
        if (sameRoute(driver().currentUrl, ROOT + 'viewCandidates')) {
            waitForCandidates() // Retains authentication, loading and interactive-control checks.
            navigationLog('CANDIDATE_LIST_REUSED', [url:safeUrl(driver().currentUrl)])
        } else list()
        buttonClick('Add')
        waitForRoute(ROOT + 'addCandidate', 'Add Candidate')
        replace(FIRST, d.first.toString()); replace(MIDDLE, d.middle.toString()); replace(LAST, d.last.toString())
        replace(field('Email'), d.email.toString())
        if (d.vacancy) choose('Vacancy', d.vacancy.toString())
        replace(field('Contact Number'), d.phone.toString())
        replace(field('Keywords'), d.keywords.toString())
        replace(area('Notes'), d.notes.toString())
        if (d.consent) {
            if (driver().findElements(By.xpath(CONSENT)).size() != 1) blocked('Consent checkbox location/capability must be checked on this demo.')
            checkbox(CONSENT, true)
        }
        if (d.applicationIso) enterDate('Date of Application', d.applicationIso.toString())
        String date = value(field('Date of Application'))
        d.datePlaceholder = one(field('Date of Application')).getAttribute('placeholder')
        d.applicationIso = parseDate(date, d.datePlaceholder.toString()).toString()
        if (d.resume) upload(d.resume as File)
    }
    Map create(Map d) {
        Closure createOnce = {
            measure('CANDIDATE_FIXTURE') {
                assert candidateMaySubmit(d) : 'Candidate already submitted; inspect/reuse the exact owned record, never resubmit'
                openAdd(d); return saveNew(d)
            }
        }
        boolean setup = caseId ==~ /RECRUIT_CAND_(?:Filter|Search|Reset|Update|CancelUpdate|VerifyDetails)_.+/
        return (Map) (setup ? EvidenceCapture.fixture(createOnce) : createOnce.call())
    }
    Map saveNew(Map d) {
        return (Map) measure('CANDIDATE_SAVE') { saveCandidateDraft(d) }
    }
    protected Map saveCandidateDraft(Map d) {
        assert d.ownership == token && candidateMaySubmit(d) : 'Only a new owned draft may be submitted once'
        assert sameRoute(driver().currentUrl, ROOT + 'addCandidate') : 'Save requires the Add Candidate form'
        Map context = [phase:'SAVE_CANDIDATE', urlBeforeSave:safeUrl(driver().currentUrl), recordReopened:false]
        long started = System.nanoTime()
        recordCandidate(d, 'prepared')
        context.beforeSubmit = { d.saveAttempted = true; recordCandidate(d, 'submitted') }
        context.afterSubmit = { d.submitted = true; recordCandidate(d, 'submitted') }
        try {
            capture('BEFORE_CANDIDATE_SAVE')
            saveSuccess('Successfully Saved', context) { diagnostic -> observeCandidate(d, context) }
            context.waitingFor = 'Numeric /addCandidate/{id}, Candidate Profile, exact fields and Vacancy'
            awaitCandidateRecord(d, context)
            // Identity matched on the returned profile. Reopen that exact URL before confirming persistence.
            assert candidateRecordUrl(d.url?.toString()) && d.recordIdentified == true
            context.waitingFor = 'Fresh GET of identified Candidate Profile; exact saved fields and Vacancy'
            go(d.url.toString(), 'Candidate Profile', true)
            context.recordReopened = true
            beginSavePhase(context)
            navigationLog('CANDIDATE_RECORD_REOPENED', [url:safeUrl(driver().currentUrl), candidateId:d.candidateId])
            awaitCandidateRecord(d, context)
            verifyCandidateFields(d) // All original field/date/Vacancy/consent assertions remain mandatory.
            d.persistedVerified = true; d.recordVerified = true
            context.recordConfirmed = true
            context.recordDiagnostics.recordConfirmed = true
            context.recordDiagnostics.reasonNotConfirmed = ''
            owned[d.url.toString()] = d
            recordCandidate(d, 'verified')
            countFixture('candidate')
            navigationLog('CANDIDATE_CONFIRMED', context.recordDiagnostics + [url:safeUrl(driver().currentUrl)])
            return d
        } catch (Throwable error) {
            if (d.saveAttempted == true) {
                try { observeCandidate(d, context) }
                catch (Throwable unavailable) { context.inspectionUnavailable = unavailable.class.simpleName }
            }
            if (d.saveAttempted == true && d.recordVerified != true) recordCandidate(d, 'uncertain')
            if (!context.recordDiagnostics) context.recordDiagnostics = [urlBeforeSave:context.urlBeforeSave,
                recordReopened:context.recordReopened == true, reasonNotConfirmed:'Candidate Save/verification stopped before profile inspection']
            if (!(context.recordDiagnostics.missingFields || context.recordDiagnostics.mismatchedFields)) {
                context.recordDiagnostics.reasonNotConfirmed = 'Verification stopped before completion: ' + error.class.simpleName
            }
            saveFailure(context, started, error)
            throw error
        }
    }
    private void awaitCandidateRecord(Map d, Map context) {
        waitFor().until {
            Map diagnostic = saveDiagnostics('Successfully Saved', context)
            if (saveEvidence(diagnostic) == 'rejected') throw new IllegalStateException('[CANDIDATE VALIDATION] ' +
                diagnostic.validation + '; ' + diagnostic.errorToasts)
            return observeCandidate(d, context)
        }
    }
    static boolean candidateMaySubmit(Map d) { return d.saveAttempted != true && d.recordVerified != true }
    static String candidateId(String url) {
        if (url == null) return null
        def match = url =~ /^https:\/\/opensource-demo\.orangehrmlive\.com\/web\/index\.php\/recruitment\/addCandidate\/([1-9]\d*)$/
        return match.matches() ? match.group(1) : null
    }
    static boolean candidateRecordUrl(String url) { return candidateId(url) != null }
    static Map candidateComparison(Map expected, Map actual) {
        List<String> keys = ['first', 'middle', 'last', 'email', 'phone', 'keywords', 'notes', 'applicationIso', 'vacancy']
        if (expected.consent == true) keys << 'consent'
        List<String> verified = [], mismatch = [], missing = []
        keys.each { key ->
            if (!actual.containsKey(key)) missing << key
            else {
                boolean matches = key == 'vacancy' && !expected.vacancy ? actual[key] in ['', '--', '-- Select --', 'N/A'] : actual[key] == expected[key]
                if (matches) verified << key
                else mismatch << key
            }
        }
        return [verifiedFields:verified, mismatchedFields:mismatch, missingFields:missing]
    }
    static Map candidateLedgerEntry(Map d, String state) {
        assert state in ['prepared', 'submitted', 'observed', 'identified', 'verified', 'uncertain']
        assert d.ownership && d.last?.toString()?.contains(d.ownership.toString()) : 'Candidate ownership marker required'
        if (state != 'prepared') assert d.saveAttempted == true
        if (state in ['identified', 'verified']) assert candidateRecordUrl(d.url?.toString()) && d.recordIdentified == true
        if (state == 'verified') assert d.recordVerified == true && d.persistedVerified == true : 'Fresh persistence verification required'
        return [kind:state == 'verified' ? 'candidate-saved' : 'candidate-save', state:state,
            name:fullName(d), email:d.email, vacancy:d.vacancy, url:d.url, candidateId:d.candidateId,
            applicationIso:d.applicationIso, ownership:d.ownership, saveAttempted:d.saveAttempted == true,
            submitted:d.submitted == true, saveObserved:d.saveObserved == true, recordIdentified:d.recordIdentified == true,
            recordVerified:d.recordVerified == true, persistedVerified:d.persistedVerified == true,
            successToastSeen:d.successToastSeen == true, cleanup:'Manual only after exact owned URL/name verification',
            time:java.time.OffsetDateTime.now().toString()]
    }
    protected void recordCandidate(Map d, String state) { track(candidateLedgerEntry(d, state)); d.saveState = state }
    private Map candidateProfileValues() {
        Map paths = [first:FIRST, middle:MIDDLE, last:LAST, email:field('Email'), phone:field('Contact Number'),
            keywords:field('Keywords'), notes:area('Notes'), applicationIso:field('Date of Application'),
            vacancy:select('Job Vacancy'), consent:CONSENT]
        Map result = (Map) ((JavascriptExecutor) driver()).executeScript('''
            const out={};
            for(const [key,xp] of Object.entries(arguments[0])) {
                const s=document.evaluate(xp,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
                if(s.snapshotLength!==1)continue;
                const n=s.snapshotItem(0);
                if(key==='consent')out[key]=n.checked;
                else if(key==='vacancy')out[key]=n.textContent.trim();
                else {out[key]=n.value||'';if(key==='applicationIso')out.datePlaceholder=n.getAttribute('placeholder');}
            }
            return out;
        ''', paths)
        if (result.containsKey('applicationIso')) {
            try { result.applicationIso = parseDate(result.applicationIso.toString(), result.datePlaceholder?.toString()).toString() }
            catch (java.time.DateTimeException ignored) { result.applicationIso = '[invalid date]' }
        }
        result.remove('datePlaceholder')
        return result
    }
    protected boolean observeCandidate(Map d, Map context) {
        String url = driver().currentUrl, id = candidateId(url)
        d.successToastSeen = d.successToastSeen == true || context.successToastSeen == true
        if ((d.successToastSeen || id) && d.saveObserved != true) {
            d.saveObserved = true; recordCandidate(d, 'observed')
        }
        if (id) {
            assert !d.url || d.url == url : 'Candidate ID changed during verification; no second submission permitted'
            if (d.recordIdentified != true) {
                d.url = url; d.candidateId = id; d.recordIdentified = true; recordCandidate(d, 'identified')
            }
        }
        Map state = pageState()
        boolean loaded = id != null && state.authenticated && state.domComplete && !state.loading && !state.challenge && !state.networkError && shown("//h6[normalize-space()='Candidate Profile']")
        Map comparison = candidateComparison(d, loaded ? candidateProfileValues() : [:])
        context.recordIdentified = id != null
        context.recordDiagnostics = [urlBeforeSave:context.urlBeforeSave, urlAfterSave:safeUrl(url),
            successToastSeen:d.successToastSeen, saveObserved:d.saveObserved == true, numericCandidateId:id,
            candidateProfileLoaded:loaded, recordIdentified:d.recordIdentified == true,
            recordReopened:context.recordReopened == true, recordConfirmed:d.recordVerified == true] + comparison +
            [reasonNotConfirmed:!id ? 'No numeric Candidate route' : !loaded ? 'Candidate Profile not ready' :
                comparison.missingFields || comparison.mismatchedFields ? 'Profile fields missing or mismatched' : 'Fresh read/assertions required']
        return loaded && comparison.missingFields.empty && comparison.mismatchedFields.empty
    }
    void requireOwned(Map d) {
        assert ExecutionContext.peek().is(executionContext) : 'Fixture helper belongs to a different execution'
        assert d.ownership == token && candidateRecordUrl(d.url?.toString()) && d.recordVerified == true &&
            owned[d.url.toString()]?.email == d.email : 'Only a verified candidate created by this case may be changed'
    }
    void reopen(Map d) {
        requireOwned(d)
        go(d.url.toString(), 'Candidate Profile', true)
        equalsValue(FIRST, d.first.toString()); equalsValue(LAST, d.last.toString())
        equalsValue(field('Email'), d.email.toString())
        assert driver().currentUrl == d.url
    }
    void vacancyValue(String expected) {
        if (shown(select('Job Vacancy'))) {
            String actual = one(select('Job Vacancy')).text.trim()
            assert expected ? actual == expected : actual in ['', '--', '-- Select --', 'N/A'] : 'Profile Job Vacancy differs'
            return
        }
        if (shown(select('Vacancy'))) {
            String actual = one(select('Vacancy')).text.trim()
            assert expected ? actual == expected : actual in ['', '-- Select --'] : 'Vacancy differs: ' + actual
            return
        }
        // Some demo versions show Vacancy as a labelled value in Application Stage.
        String stage = "//*[self::h5 or self::h6][normalize-space()='Application Stage']/parent::div"
        String container = stage + '/parent::div'
        if (expected) one(container + "//*[self::a or self::p][normalize-space()=${lit(expected)}]")
        else {
            String label = container + "//*[self::p or self::label][normalize-space()='Vacancy']"
            String text = one(label + '/following-sibling::*[1]').text.trim()
            assert text in ['', '--', '-- Select --', 'N/A'] : 'Optional Vacancy is not blank: ' + text
        }
    }
    void verifyDetails(Map d) {
        requireOwned(d)
        if (caseId=='RECRUIT_CAND_VerifyDetails_01') capture('BEFORE_ACTION_VERIFY_DETAILS')
        verifyCandidateFields(d)
        if (caseId=='RECRUIT_CAND_VerifyDetails_01') capture('AFTER_ACTION_VERIFY_DETAILS')
    }
    private void verifyCandidateFields(Map d) {
        measure('ASSERTION') { verifyCandidateFieldValues(d) }
    }
    protected void verifyCandidateFieldValues(Map d) {
        equalsValue(FIRST, d.first.toString()); equalsValue(MIDDLE, d.middle.toString()); equalsValue(LAST, d.last.toString())
        equalsValue(field('Email'), d.email.toString())
        equalsValue(field('Contact Number'), d.phone.toString())
        equalsValue(field('Keywords'), d.keywords.toString()); equalsValue(area('Notes'), d.notes.toString())
        String date = value(field('Date of Application'))
        LocalDate persistedDate = parseDate(date, one(field('Date of Application')).getAttribute('placeholder'))
        assert persistedDate.toString() == d.applicationIso : 'Date of Application was not persisted as entered'
        vacancyValue(d.vacancy.toString())
        if (d.consent) {
            List<WebElement> checkbox = driver().findElements(By.xpath(CONSENT))
            if (checkbox.size() != 1) blocked('Cannot verify persisted consent on Candidate Profile. Keep expected; inspect DOM/storage UI.')
            assert checkbox[0].selected : 'Consent was not persisted'
        }
    }
    void list() {
        go('viewCandidates', 'Candidates')
        // Reuse this list only with default filters and page 1, matching the former full navigation contract.
        if (!filtersAreDefault(candidateFilters())) tableAction('Reset')
    }
    List<Map<String,String>> byVacancy(Map vac) {
        list(); choose('Vacancy', vac.name.toString()); tableAction('Search')
        return allRows('Candidate')
    }
    void emptyVacancy(Map vac) {
        assert byVacancy(vac).empty : 'Invalid/cancelled form created a candidate in our initially empty vacancy'
        one(EMPTY)
    }
    void noCandidate(Map d, Map vac) {
        if (d.vacancy) { emptyVacancy(vac); return }
        list()
        List<Map<String,String>> rows = allRows('Candidate')
        assert !rows.any { it.Candidate == fullName(d) } : 'Cancelled draft was persisted'
    }
    void verifyListAndReopen(Map d, Map vac) {
        List<Map<String,String>> rows
        if (d.vacancy) rows = byVacancy(vac)
        else { list(); rows = allRows('Candidate') }
        List<Map<String,String>> matches = rows.findAll { it.Candidate == fullName(d) }
        if (matches.size() != 1) {
            navigationLog('CANDIDATE_LIST_IDENTITY_MISMATCH', [candidateId:d.candidateId,
                ownedUrl:safeUrl(d.url.toString()), expectedName:fullName(d), vacancy:d.vacancy,
                exactNameCount:matches.size(), totalRows:rows.size(), matchingRows:matches,
                note:'No record chosen. Known ID was fresh-read before list lookup; reconcile duplicates/missing rows without resubmitting.'])
        }
        assert matches.size() == 1 : 'Candidate not uniquely present in list: ' + fullName(d)
        assert d.vacancy ? matches[0].Vacancy == d.vacancy : matches[0].Vacancy in ['', '--', 'N/A'] : 'List/profile Vacancy mismatch'
        // Search by the exact owned name, then use the scoped View icon and compare the numeric URL.
        list(); autocomplete('Candidate Name', fullName(d)); tableAction('Search')
        List<Map<String,String>> exact = allRows('Candidate')
        assert exact.size() == 1 && exact[0].Candidate == fullName(d)
        String row = "//div[@role='row'][./div[@role='cell'][normalize-space()=${lit(fullName(d))}]]"
        click(row + "//button[.//i[${cls('bi-eye-fill')} or ${cls('bi-eye')}]]")
        heading('Candidate Profile')
        assert driver().currentUrl == d.url : 'List opened another candidate'
        verifyDetails(d)
    }
    void verifyEmailLookup(Map d) {
        list()
        if (!shown(field('Email'))) {
            blocked('Workbook explicitly asks to search by email, but the Email filter selector is unverified/unavailable. Exact-name lookup plus numeric URL/email verification is prepared separately; it does not silently satisfy this step. Review UI/design for Create_01 / FLOW_VerifyDetails_01.')
        }
        replace(field('Email'), d.email.toString()); tableAction('Search')
        List<Map<String,String>> result = allRows('Candidate')
        assert result.size() == 1 && result[0].Candidate == fullName(d) : 'Email search did not return exactly the owned candidate'
        String row = "//div[@role='row'][./div[@role='cell'][normalize-space()=${lit(fullName(d))}]]"
        click(row + "//button[.//i[${cls('bi-eye-fill')} or ${cls('bi-eye')}]]")
        heading('Candidate Profile')
        assert driver().currentUrl == d.url
        verifyDetails(d)
    }
    void enableEdit(Map d) {
        requireOwned(d)
        String toggle = "//*[self::h5 or self::h6][normalize-space()='Candidate Profile']/parent::div//input[@type='checkbox']"
        if (!one(field('Email')).enabled) {
            List<WebElement> controls = driver().findElements(By.xpath(toggle))
            if (controls.size() != 1) blocked('Candidate Profile Edit toggle needs live DOM verification.')
            checkbox(toggle, true)
        }
        waitFor().until { one(field('Email')).enabled }
    }
    void updateNotes(Map d) {
        requireOwned(d)
        assert d.updateAttempted != true : 'Update already attempted; reconcile the owned record, never resubmit'
        reopen(d); enableEdit(d)
        replace(field('Keywords'), 'playwright,regression') // Literal workbook data; tool remains Katalon.
        replace(area('Notes'), 'Updated ' + token)
        Map expected = new LinkedHashMap(d) + [keywords:'playwright,regression', notes:'Updated ' + token]
        Map context = [phase:'UPDATE_CANDIDATE', urlBeforeSave:safeUrl(driver().currentUrl), recordReopened:false]
        recordUpdate(d, expected, context, 'prepared')
        context.beforeSubmit = { d.updateAttempted = true; recordUpdate(d, expected, context, 'submitted') }
        context.afterSubmit = { recordUpdate(d, expected, context, 'submitted') }
        Throwable saveError = null
        try {
            try {
                saveSuccess('Successfully Updated', context) { diagnostic -> candidateUpdateProof(d, expected, diagnostic) }
            } catch (Throwable failure) {
                saveError = failure
                recordUpdate(d, expected, context, 'uncertain')
                if (context.submitted != true || !pageState().login) throw failure
                navigationLog('AUTH_SESSION_LOST_AFTER_SUBMIT', [submitted:true, recordConfirmed:false,
                    httpStatus:'NOT_OBSERVED', candidateId:candidateId(d.url.toString())])
                navigationLog('UPDATE_REAUTHENTICATION_REQUIRED', [submitted:true, recordConfirmed:false,
                    candidateId:candidateId(d.url.toString()), recovery:'Manual login then GET only; never replay Save'])
                println('[MANUAL LOGIN] ' + caseId + ': session expired during Update. Complete manual login; recovery only reads this owned record, no second Save.')
                openCandidatesFromMenu() // Existing Dashboard/manual login/native menu contract, unchanged 120 s allowance.
            }
            reopen(d) // Fresh GET of exactly the original owned ID, including after authentication recovery.
            context.recordReopened = true
            verifyDetails(expected) // Changed fields AND every original immutable field/date/Vacancy/consent assertion.
            context.recordConfirmed = true
            recordUpdate(d, expected, context, 'verified')
            d.keywords = expected.keywords; d.notes = expected.notes
        } catch (Throwable failure) {
            if (d.updateAttempted == true) recordUpdate(d, expected, context, 'uncertain')
            if (saveError != null && failure.is(saveError) == false) {
                saveError.addSuppressed(failure)
                boolean login = false
                try { login = pageState().login == true } catch (Exception ignored) {}
                throw new IllegalStateException(login ? '[ENVIRONMENT / AUTHENTICATION BLOCKED] Update submitted; manual login incomplete; owned record remains uncertain' :
                    '[UPDATE NOT CONFIRMED] Read-only recovery did not verify all expected fields; never resubmit', saveError)
            }
            throw failure
        }
    }
    protected boolean candidateUpdateProof(Map d, Map expected, Map diagnostic) {
        requireOwned(d)
        Map state = pageState()
        if (state.login) throw new IllegalStateException('[UPDATE AUTHENTICATION REQUIRED] Submitted result is uncertain; do not replay Save')
        // The existing ID and freshly typed fields also exist BEFORE Update is persisted.
        // Never accept that dirty form alone as an acknowledgement of this submission.
        if (diagnostic.successToastSeen != true && diagnostic.successToast != true) return false
        if (!sameRoute(driver().currentUrl, d.url.toString()) || !state.authenticated || !state.domComplete ||
            state.loading || state.challenge || state.networkError) return false
        Map comparison = candidateComparison(expected, candidateProfileValues())
        return comparison.missingFields.empty && comparison.mismatchedFields.empty
    }
    protected void recordUpdate(Map d, Map expected, Map context, String state) {
        requireOwned(d)
        assert state in ['prepared','submitted','uncertain','verified']
        if (state == 'verified') assert context.recordReopened == true && context.recordConfirmed == true
        track([kind:'candidate-update', state:state, ownership:token, url:d.url, candidateId:candidateId(d.url.toString()),
            saveAttempted:context.saveAttempted == true, submitted:context.submitted == true,
            expectedKeywords:expected.keywords, expectedNotes:expected.notes, recordReopened:context.recordReopened == true,
            recordVerified:state == 'verified', time:java.time.OffsetDateTime.now().toString()])
        d.updateState = state
    }
    void cancelEdit(Map d) {
        reopen(d); enableEdit(d)
        String baseline = value(field('Email'))
        replace(field('Email'), 'unsaved@example.com')
        capture('BEFORE_CANCEL_EDIT')
        // Source_Aligned requires leaving via Recruitment > Candidates. Profile has Edit/Save only.
        openCandidatesFromMenu()
        reopen(d); equalsValue(field('Email'), baseline); verifyDetails(d)
    }
    void upload(File file) {
        assert file.isFile() && file.length() > 0
        List<WebElement> inputs = driver().findElements(By.xpath("//input[@type='file']"))
        if (inputs.size() != 1) blocked('Resume file input missing/ambiguous. Inspect live DOM.')
        inputs[0].sendKeys(file.absolutePath)
        capture('RESUME_SELECTED')
    }
    String resumeText() { return one(group('Resume')).text }
    void resumePersisted(Map d, File file, boolean download = false) {
        reopen(d)
        assert resumeText().contains(file.name) : 'Resume filename not persisted'
        if (download) downloadResume(file, d)
    }
    static String sha256(File file) { return MessageDigest.getInstance('SHA-256').digest(file.bytes).encodeHex().toString() }
    static String resumeListDownload(String name, String vacancy) {
        return "//div[@role='row'][./div[@role='cell'][normalize-space()=${lit(name)}]]" +
            "[./div[@role='cell'][normalize-space()=${lit(vacancy)}]]//button[.//i[${cls('bi-download')}]]"
    }
    static String candidateAttachmentUrl(String ownedProfileUrl) {
        String id = candidateId(ownedProfileUrl)
        assert id : 'Attachment requires an identified positive Candidate ID'
        return ROOT + 'viewCandidateAttachment/candidateId/' + id
    }
    static String attachmentCandidateId(String url) {
        try {
            URI uri = new URI(url)
            if (uri.scheme != 'https' || uri.host != new URI(BASE).host || uri.port != -1 || uri.userInfo) return null
            def match = uri.path =~ /^\/web\/index\.php\/recruitment\/viewCandidateAttachment\/candidateId\/([1-9]\d*)$/
            return match.matches() ? match.group(1) : null
        } catch (Exception ignored) { return null }
    }
    static String resumeProfileDownload(String ownedProfileUrl) {
        String absolute = candidateAttachmentUrl(ownedProfileUrl)
        String relative = new URI(absolute).path
        return group('Resume') + "/ancestor::div[${cls('orangehrm-file-input')}][1]//a[" +
            "@href=${lit(absolute)} or @href=${lit(relative)}]"
    }
    static String resumeProfilePreview(String filename) {
        assert filename?.trim()
        // FileUploadInput Uo renders an onClick DIV, not an anchor or button.
        return group('Resume') + "//div[${cls('orangehrm-file-current')}]" +
            "/div[${cls('orangehrm-file-preview')}][.//p[${cls('orangehrm-file-name')} and @title=${lit(filename)}]]" +
            "[.//i[${cls('orangehrm-file-download')}]]"
    }
    protected Map inspectResumeWindows(Set<String> before, String original, String expectedId) {
        Map observed = [newWindows:0, inspectedWindows:0, expectedAttachmentObserved:false,
            foreignCandidateObserved:false, inlinePdfObserved:false]
        try {
            List<String> added = (driver().windowHandles - before).toList()
            observed.newWindows = added.size()
            added.take(2).each { handle ->
                driver().switchTo().window(handle)
                observed.inspectedWindows++
                String foundId = attachmentCandidateId(driver().currentUrl)
                if (foundId) {
                    if (foundId == expectedId) observed.expectedAttachmentObserved = true
                    else observed.foreignCandidateObserved = true
                }
                observed.inlinePdfObserved = observed.inlinePdfObserved ||
                    !driver().findElements(By.xpath("//embed[@type='application/pdf'] | //object[@type='application/pdf']")).empty
            }
        } catch (Exception unavailable) { observed.inspectionUnavailable = unavailable.class.simpleName }
        finally {
            try { if (driver().windowHandles.contains(original)) driver().switchTo().window(original) }
            catch (Exception unavailable) { observed.restoreUnavailable = unavailable.class.simpleName }
        }
        return observed
    }
    void downloadResume(File source, Map d = [:]) {
        beginSavePhase([phase:'RESUME_DOWNLOAD', waitingFor:'Native verified Resume download, completed local file and SHA-256'])
        requireOwned(d)
        String expectedAttachment = candidateAttachmentUrl(d.url.toString())
        Set<String> windowsBefore = new HashSet<String>(driver().windowHandles)
        String originalWindow = driver().windowHandle
        File folder = new File(output, 'downloads'); assert folder.mkdirs() || folder.isDirectory()
        try { driver().executeCdpCommand('Browser.setDownloadBehavior', [behavior:'allow', downloadPath:folder.absolutePath]) }
        catch (MissingMethodException ex) { blocked('Chrome download control unavailable; configure Katalon Chrome download preferences and inspect helper. ' + ex.message) }
        String control = group('Resume') + "//button[.//i[${cls('bi-download')}]]"
        String profileLink = resumeProfileDownload(d.url.toString())
        String preview = resumeProfilePreview(source.name)
        if (shown(preview)) {
            assert sameRoute(driver().currentUrl, d.url.toString()) : 'Resume preview must belong to the exact owned Profile'
            assert d.resume == source && visible(preview).size() == 1 : 'Missing/ambiguous owned uploaded Resume preview'
            navigationLog('RESUME_DOWNLOAD_ROUTE', [route:'Profile FileUploadInput native preview click', candidateId:candidateId(d.url.toString())])
            click(preview)
        } else if (shown(profileLink)) {
            assert visible(profileLink).size() == 1 : 'Ambiguous owned Profile attachment link'
            navigationLog('RESUME_DOWNLOAD_ROUTE', [route:'Profile native attachment link', candidateId:candidateId(d.url.toString())])
            click(profileLink)
        } else if (shown(control)) {
            click(control)
        } else {
            // Latest local Candidates capture has a native Download button on the owned row.
            // Do not invent a Profile anchor or click any unscoped download icon.
            requireOwned(d)
            assert source.isFile() && d.resume == source && d.vacancy : 'Download requires the owned uploaded fixture'
            navigationLog('RESUME_DOWNLOAD_ROUTE', [route:'Owned Candidates row native Download', profileControlMissing:true])
            list(); choose('Vacancy', d.vacancy.toString()); tableAction('Search')
            List<Map<String,String>> matches = allRows('Candidate').findAll { it.Candidate == fullName(d) }
            assert matches.size() == 1 && matches[0].Vacancy == d.vacancy : 'Download row ownership/uniqueness mismatch'
            String downloadControl = resumeListDownload(fullName(d), d.vacancy.toString())
            assert visible(downloadControl).size() == 1 : 'Missing/ambiguous exact owned Resume download button'
            beginSavePhase([phase:'RESUME_DOWNLOAD', waitingFor:downloadControl])
            click(downloadControl)
        }
        File saved = new File(folder, source.name)
        Map attachmentWindow = [:]
        int inspections = 0
        try {
            waitFor().until {
                if (inspections < 3 && driver().windowHandles.size() > windowsBefore.size()) {
                    inspections++
                    attachmentWindow = inspectResumeWindows(windowsBefore, originalWindow, attachmentCandidateId(expectedAttachment))
                    assert !attachmentWindow.foreignCandidateObserved : 'Download opened an attachment for another Candidate'
                }
                return saved.isFile() && saved.length() == source.length() && !new File(folder, source.name + '.crdownload').exists()
            }
        } catch (TimeoutException failure) {
            attachmentWindow = inspectResumeWindows(windowsBefore, originalWindow, attachmentCandidateId(expectedAttachment))
            navigationLog('RESUME_DOWNLOAD_STOPPED', [expectedFilename:source.name, expectedBytes:source.length(),
                completedFiles:folder.listFiles().findAll { !it.name.endsWith('.crdownload') }.collect { it.name },
                attachmentWindow:attachmentWindow, previewIsNotDownload:true,
                waitingFor:'Completed file with original fixture size, before SHA-256 assertion; an attachment tab/preview is insufficient'])
            throw failure
        }
        attachmentWindow = inspectResumeWindows(windowsBefore, originalWindow, attachmentCandidateId(expectedAttachment))
        assert !attachmentWindow.foreignCandidateObserved : 'Attachment Candidate ID mismatch'
        assert sha256(saved) == sha256(source) : 'Downloaded CV bytes differ from uploaded fixture'
        track([kind:'resume-download-verified', filename:source.name, sha256:sha256(saved),
            candidateId:candidateId(d.url.toString()), attachmentWindow:attachmentWindow])
    }
    void invalidCreate(Map d, Map vac, String fieldPath, String pattern) {
        emptyVacancy(vac); openAdd(d); buttonClick('Save')
        error(fieldPath, pattern)
        assert driver().currentUrl.endsWith('/recruitment/addCandidate') : 'Invalid candidate navigated away from Add'
        noCandidate(d, vac)
    }
    void invalidResume(Map d, Map vac, File file, String pattern) {
        emptyVacancy(vac); d.resume = file; openAdd(d); buttonClick('Save')
        error(group('Resume'), pattern)
        assert driver().currentUrl.endsWith('/recruitment/addCandidate')
        noCandidate(d, vac)
    }

    /** Positive and negative controls are created within this case, never from a preceding test. */
    Map controls(boolean dates = false, boolean distinctVacancies = true) {
        Map va = vacancy('A'); Map vb = distinctVacancies ? vacancy('B') : va
        String keyword = 'run_keyword_a_' + token
        Map a = create(draft(va, 'A', [keywords:keyword]))
        Map b = create(draft(vb, 'B', [keywords:'run_keyword_b_' + token]))
        Map c = null
        if (dates) c = create(draft(va, 'C', [keywords:'date_control_' + token,
            applicationIso:LocalDate.parse(a.applicationIso.toString()).minusDays(10).toString()]))
        return [va:va, vb:vb, a:a, b:b, c:c, keyword:keyword]
    }
    void filterCase(String mode) {
        measure(mode == 'reset' ? 'RESET' : mode == 'name' ? 'SEARCH' : 'FILTER') { runFilterCase(mode) }
    }
    protected void runFilterCase(String mode) {
        Map f = controls(mode == 'date', mode in ['vacancy','date','reset'])
        list()
        if (mode in ['vacancy', 'date', 'reset']) choose('Vacancy', f.va.name.toString())
        if (mode == 'name') autocomplete('Candidate Name', fullName(f.a))
        if (mode in ['keyword', 'reset']) replace(field('Keywords'), f.keyword.toString())
        String datePattern = ''
        LocalDate from = LocalDate.parse(f.a.applicationIso.toString()).minusDays(1)
        LocalDate to = LocalDate.parse(f.a.applicationIso.toString()).plusDays(1)
        if (mode == 'date') {
            datePattern = f.a.datePlaceholder.toString() // Format observed on this case's actual Candidate date field.
            replace(dateFilter('From'), dateText(from.toString(), datePattern))
            replace(dateFilter('To'), dateText(to.toString(), datePattern))
        }
        tableAction('Search')
        List<Map<String,String>> results = allRows('Candidate')
        assert results : 'Positive control was filtered out'
        assert results.any { it.Candidate == fullName(f.a) } : 'Positive control missing'
        assert !results.any { it.Candidate == fullName(f.b) } : 'Negative control leaked into results'
        results.each { row ->
            if (mode in ['vacancy', 'date', 'reset']) assert row.Vacancy == f.va.name
            if (mode in ['name', 'keyword', 'reset']) assert row.Candidate == fullName(f.a)
            if (mode == 'date') {
                LocalDate date = parseDate(row['Date of Application'], datePattern)
                assert !date.isBefore(from) && !date.isAfter(to) : 'Date outside AND filter range'
            }
        }
        if (mode == 'date') assert !results.any { it.Candidate == fullName(f.c) } : 'Wrong-date control leaked into results'
        if (mode == 'keyword') { reopen(f.a); equalsValue(field('Keywords'), f.keyword.toString()) }
        if (mode == 'reset') {
            // The exact one-record result above is already on page 1; preserve/check that query before Reset.
            assert results.size() == 1 && results[0].Candidate == fullName(f.a) : 'Reset requires the exact owned filtered result'
            assert one(select('Vacancy')).text.trim() == f.va.name
            equalsValue(field('Keywords'), f.keyword.toString())
            tableAction('Reset')
            assert one(select('Vacancy')).text.trim() == '-- Select --'
            equalsValue(field('Keywords'), '')
            ['Job Title', 'Hiring Manager', 'Status', 'Method of Application'].each { label ->
                assert one(select(label)).text.trim() == '-- Select --'
            }
            equalsValue(field('Candidate Name'), '')
            equalsValue(dateFilter('From'), ''); equalsValue(dateFilter('To'), '')
            List<Map<String,String>> reset = allRows('Candidate')
            assert reset.any { it.Candidate == fullName(f.a) } && reset.any { it.Candidate == fullName(f.b) } : 'Reset did not restore both own controls'
        }
        capture('RESULT_SET_VERIFIED')
    }
}
