package recruitment

import org.openqa.selenium.*
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Opt-in recovery for the thirteen ERROR cases in the 20261101_104433 baseline.
 * No credentials, browser launch, mutation, case replay or automatic identity replacement.
 * An unresolved Save always stops the current case, even after authentication recovers.
 */
class SessionRecovery {
    static final Set<String> CASES = (['RECRUIT_CAND_Create_12'] +
        ['01','02','03','05'].collect {'RECRUIT_CAND_UploadCV_'+it} +
        ['Filter_01','Search_01','Filter_02','Filter_03','Reset_01','Update_01','CancelUpdate_01','VerifyDetails_01']
            .collect {'RECRUIT_CAND_'+it}).asImmutable() as Set
    static final int MAX_ATTEMPTS=2
    static boolean eligible(String id) { CASES.contains(id) }
    final ExecutionContext context
    int allowanceSeconds=120
    boolean diagnosed=false, localizationReloaded=false
    long started=System.nanoTime()
    SessionRecovery(ExecutionContext c=ExecutionContext.current()) { context=c }

    static boolean authenticationFailure(Object error) {
        if(error instanceof PimEmployeeFixtures.Fault) return error.state=='SESSION_EXPIRED'
        if(error instanceof Map) return error.state=='SESSION_EXPIRED' || error.status==401
        false
    }
    protected WebDriver browser() { ExecutionBrowser.driver() }
    protected void navigate(String url) { browser().navigate().to(url) } // GET only; never Save/click
    protected Map state() {
        WebDriver d=browser()
        boolean login=RecruitmentUi.sameRoute(d.currentUrl,RecruitmentUi.BASE+'/web/index.php/auth/login') ||
            d.findElements(By.xpath(RecruitmentUi.LOGIN)).any {it.displayed}
        [login:login,authenticated:!login && d.findElements(By.xpath(RecruitmentUi.SIDEBAR)).any {it.displayed} &&
            d.findElements(By.xpath('//header//h6')).any {it.displayed},
         ready:((JavascriptExecutor)d).executeScript('return document.readyState')=='complete',
         loading:d.findElements(By.xpath(RecruitmentUi.LOADER)).any {it.displayed}]
    }
    protected void log(String event,Map details=[:]) {
        Map row=[event:event,caseId:context.caseId,executionId:context.executionId,
            attempt:context.authentication.recoveryAttempts ?: 0]+details
        // No cookie values, credentials, body, raw browser errors or query strings.
        println('[SESSION RECOVERY] '+new com.google.gson.Gson().toJson(row))
        new File(context.root,'logs/session-recovery.jsonl').append(new com.google.gson.Gson().toJson(row)+'\n','UTF-8')
    }
    protected Map read(Map params) {
        // The old transport stays unchanged. Temporarily bypass ONLY its cached auth fault
        // for a recovery verification GET; restore it until every invariant is proven.
        RuntimeException saved=context.pimReadFailure
        if(saved && !authenticationFailure(saved)) throw saved
        context.pimReadFailure=null
        try { return ApiReadTransport.request(browser(),'/api/v2/pim/employees',params) }
        finally { if(saved) context.pimReadFailure=saved }
    }
    protected boolean verifiedLogin() {
        Map s=state()
        if(s.login || !s.authenticated || !s.ready || s.loading) return false
        // Only after a plausible authenticated document: never poll PIM on the login page.
        try { read([model:'detailed',includeEmployees:'onlyCurrent',limit:1,offset:0]);return true }
        catch(PimEmployeeFixtures.Fault error) {
            if(authenticationFailure(error)) throw new PimEmployeeFixtures.Fault('RECOVERY_AUTHENTICATION_REJECTED',
                'Authenticated-looking document still returns HTTP 401; stop rather than repeatedly polling PIM',error)
            throw error
        }
    }
    protected void waitForLogin() {
        new WebDriverWait(browser(),Duration.ofSeconds(allowanceSeconds)).pollingEvery(Duration.ofMillis(500))
            .ignoring(StaleElementReferenceException).until { verifiedLogin() }
    }
    protected Map identities() {
        Map result=[:]
        ['managerA','managerB'].each { k ->
            Map identity=context.managerDiscovery[k]
            if(identity?.employeeId && identity.pimVerified==true) result[k]=new LinkedHashMap(identity)
        }
        result
    }
    protected void verifyIdentities(Map pinned) {
        pinned.each { key,expected ->
            assert PimEmployeeFixtures.positive(expected.employeeId) : 'Recovery needs the original numeric Employee ID'
            Map response=read([model:'detailed',includeEmployees:'onlyCurrent',empNumber:expected.employeeId,limit:50,offset:0])
            List matches=response.data.collect {PimEmployeeFixtures.employee(it)}.findAll {it.employeeId==expected.employeeId.toString()}
            if(matches.size()!=1 || !PimEmployeeFixtures.samePerson(expected,matches[0]))
                throw new PimEmployeeFixtures.Fault('RECOVERY_EMPLOYEE_CHANGED','Original active numeric Employee ID/name no longer matches; no replacement identity')
        }
    }
    void recover(String cause) {
        assert eligible(context.caseId) : 'Recovery is outside the allowed case scope'
        recoverInternal(cause)
    }
    /** Explicit read-only CAND entry points; do not expand the original eligible case set. */
    void verifyCandidateIdentities(){verifyIdentities(identities())}
    void recoverCandidateAdmission(String id){
        assert id==context.caseId && id.startsWith('RECRUIT_CAND_') : 'Wrong CAND admission recovery scope'
        recoverInternal('LOGIN_BEFORE_CANDIDATE_ADMISSION')
    }
    void recoverCandidateList(RecruitmentUi ui){
        assert ui.executionContext.is(context) && ui.caseId==context.caseId && ui.caseId.startsWith('RECRUIT_CAND_')
        currentRecords(ui) // A submitted but uncertain Save cannot be resumed/replayed.
        recoverInternal('LOGIN_DURING_CANDIDATE_LIST_GET')
        reopen(ui,RecruitmentUi.ROOT+'viewCandidates') // Same numeric owned records, then a read-only list GET.
    }
    protected void recoverInternal(String cause) {
        int attempts=(context.authentication.recoveryAttempts ?: 0) as int
        if(attempts>=MAX_ATTEMPTS) throw new PimEmployeeFixtures.Fault('RECOVERY_ATTEMPTS_EXHAUSTED','Two recovery requests already used in this execution; no loop')
        browser() // Must be the existing execution-owned Katalon driver, never a new profile.
        Map pinned=identities()
        context.authentication.recoveryAttempts=attempts+1
        context.authentication.exhausted=true
        context.authentication.recoveryFailure='IN_PROGRESS'
        context.persist()
        log('REQUESTED',[cause:cause,deadlineSeconds:allowanceSeconds,credentialPolicy:'MANUAL_LOGIN_ONLY'])
        try {
            navigate(RecruitmentUi.BASE+'/web/index.php/auth/login')
            println('[MANUAL SESSION RECOVERY] Dang nhap lai trong Chrome Katalon hien tai; toi da '+allowanceSeconds+' giay. Khong nhap mat khau vao source/log.')
            waitForLogin() // Returns immediately after UI AND read-only API proof; no fixed delay.
            verifyIdentities(pinned)
            clearAuthenticationFailure(pinned)
            log('VERIFIED',[apiStatus:200,readOnly:true,numericManagerIds:pinned.collectEntries {k,v->[(k):v.employeeId]}])
        } catch(TimeoutException error) {
            context.authentication.recoveryFailure='RECOVERY_TIMED_OUT';context.persist();log('TIMED_OUT')
            throw new PimEmployeeFixtures.Fault('RECOVERY_TIMED_OUT','Manual login/API proof did not complete within the deadline; no submit replay',error)
        } catch(RuntimeException error) {
            context.authentication.recoveryFailure=error instanceof PimEmployeeFixtures.Fault ? error.state : 'RECOVERY_FAILED'
            context.persist();log('STOPPED',[state:context.authentication.recoveryFailure])
            throw error
        }
    }
    protected void clearAuthenticationFailure(Map pinned) {
        // Reset only authentication failures AFTER successful login AND exact ID revalidation.
        if(authenticationFailure(context.pimReadFailure)) context.pimReadFailure=null
        if(authenticationFailure(context.technicalPreflightFailure)) context.technicalPreflightFailure=null
        if(authenticationFailure(context.pimFixtures.readFailure)) context.pimFixtures.remove('readFailure')
        pinned.each { k,v ->
            Map restored=v+[state:'VERIFIED']
            context.managerDiscovery[k]=restored
            String setting=k=='managerA' ? 'hiringManagerName' : 'hiringManagerAlternativeName'
            if(authenticationFailure(context.preflight.identities?.get(setting))) context.preflight.identities[setting]=restored
        }
        if(authenticationFailure(context.preflight.decisions?.get(context.caseId))) context.preflight.decisions.remove(context.caseId)
        if(authenticationFailure(context.preflightErrors[context.caseId])) context.preflightErrors.remove(context.caseId)
        context.menuVerified=false
        context.authentication.authenticated=true;context.authentication.exhausted=false
        context.authentication.reason='';context.authentication.remove('recoveryFailure');context.persist()
    }
    protected Map normalAdmission(String id,File root) { ConfigurationPreflight.admit(id,root) }
    Map admit(String id,File root) {
        if(!eligible(id)) return normalAdmission(id,root)
        try {
            boolean known=authenticationFailure(context.pimReadFailure) || authenticationFailure(context.technicalPreflightFailure)
            if(!known && context.ownedDriver && context.authentication.authenticated) known=state().login
            if(known) recover('AUTH_FAILURE_BEFORE_CASE')
            Map decision=normalAdmission(id,root)
            if(authenticationFailure(decision)) {
                recover('AUTH_FAILURE_DURING_PREFLIGHT_GET')
                decision=normalAdmission(id,root) // Read-only preflight; never reruns a case/fixture.
            }
            return decision
        } catch(RuntimeException error) {
            String code=error instanceof PimEmployeeFixtures.Fault ? error.state : 'RECOVERY_FAILED'
            Map denied=[caseId:id,caseRunId:context.caseRunId,state:code,canonicalState:code,
                problems:[[state:code,detail:'Session recovery failed; no case/fixture replay',errorType:error.class.name]]]
            context.preflightErrors[id]=error
            if(!context.preflight.decisions) context.preflight.decisions=[:]
            context.preflight.decisions[id]=denied;context.evidence.preflight=denied
            ConfigurationPreflight.persist()
            return denied // Listener exceptions alone cannot reliably suppress a Katalon case.
        }
    }
    static List<Map> currentRecords(RecruitmentUi ui) {
        Map latest=[:]
        ui.ledger.findAll {it.saveAttempted==true || it.kind=='candidate-update'}.each { row ->
            String key=row.kind=='candidate-update' ? 'update:'+row.url : row.ownership+':'+(row.name ?: row.first+':'+row.last)
            latest[key]=row
        }
        List<Map> records=latest.values().toList()
        if(records.any {!(it.state in ['confirmed','verified'])})
            throw new PimEmployeeFixtures.Fault('RECOVERY_UNCERTAIN_SAVE','A Save/Update has an unresolved outcome; reconcile exact numeric record manually. No replay')
        records=records.collect { row ->
            if(row.kind=='candidate-update') {
                Map identity=ui.ledger.reverse().find {it.kind=='candidate-saved' && it.url==row.url && it.ownership==row.ownership}
                if(!identity) throw new PimEmployeeFixtures.Fault('RECOVERY_OWNERSHIP_UNPROVEN','No verified Candidate identity for the updated numeric record')
                return identity+row
            }
            row
        }
        records.each { row ->
            boolean vacancy=row.kind in ['vacancy','vacancy-saved']
            assert row.ownerExecutionId==ui.executionContext.executionId && row.caseId==ui.caseId && row.ownership==ui.token
            assert vacancy ? CandidateChecks.vacancyRecordUrl(row.url?.toString()) : CandidateChecks.candidateRecordUrl(row.url?.toString())
        }
        records
    }
    protected void verifyRecord(RecruitmentUi ui,Map row) {
        boolean vacancy=row.kind in ['vacancy','vacancy-saved']
        navigate(row.url.toString()) // Fresh owned numeric GET, no adoption from a previous case.
        Map observed=null
        try { new WebDriverWait(browser(),Duration.ofSeconds(15)).pollingEvery(Duration.ofMillis(200)).until {
            Map s=state()
            if(s.login) throw new PimEmployeeFixtures.Fault('SESSION_EXPIRED','Authentication lost during owned-record reconciliation')
            if(!s.ready || s.loading) return false
            String switches="//div[${RecruitmentUi.cls('orangerhrm-switch-wrapper')}]"
            Map paths=vacancy ? [name:RecruitmentUi.field('Vacancy Name'),manager:RecruitmentUi.field('Hiring Manager'),
                positions:RecruitmentUi.field('Number of Positions'),description:RecruitmentUi.area('Description'),
                jobTitle:RecruitmentUi.select('Job Title'),
                active:switches+"[.//*[normalize-space()='Active']]//input[@type='checkbox']",
                published:switches+"[.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input[@type='checkbox']"] :
                [first:CandidateChecks.FIRST,middle:CandidateChecks.MIDDLE,last:CandidateChecks.LAST,email:RecruitmentUi.field('Email')]
            if(row.kind=='candidate-update') paths += [keywords:RecruitmentUi.field('Keywords'),notes:RecruitmentUi.area('Notes')]
            observed=(Map)((JavascriptExecutor)browser()).executeScript('''
                const out={};for(const [k,xp] of Object.entries(arguments[0])) {
                  const s=document.evaluate(xp,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
                  if(s.snapshotLength!==1)return null;const n=s.snapshotItem(0);
                  out[k]=(k==='active'||k==='published')?n.checked:k==='jobTitle'?n.textContent.trim():n.value;
                }return out;
            ''',paths)
            observed!=null
        } } catch(TimeoutException error) {
            throw new PimEmployeeFixtures.Fault('RECOVERY_OWNED_RECORD_MISSING','Fresh owned numeric GET has no expected form; no fixture recreation',error)
        }
        boolean matches=vacancy ? CandidateChecks.vacancyValuesMatch(row.expected as Map,observed) :
            [observed.first,observed.middle,observed.last].findAll {it}.join(' ')==row.name && observed.email==row.email
        if(row.kind=='candidate-update') matches=matches && observed.keywords==row.expectedKeywords && observed.notes==row.expectedNotes
        if(!matches || !RecruitmentUi.sameRoute(browser().currentUrl,row.url.toString()))
            throw new PimEmployeeFixtures.Fault('RECOVERY_OWNED_RECORD_CHANGED','Owned numeric record is missing or identity changed; no fixture creation/replay')
    }
    protected void reopen(RecruitmentUi ui,String url) {
        List<Map> records=currentRecords(ui)
        records.each {verifyRecord(ui,it)}
        assert url.startsWith(RecruitmentUi.ROOT)
        navigate(url) // Reacquire everything by locator in the caller; retain no WebElements.
    }
    protected Map component() {
        (Map)((JavascriptExecutor)browser()).executeScript('''
            const h=Array.from(document.querySelectorAll('h5,h6')).map(x=>x.textContent.trim());
            return {mounted:!!document.querySelector('input[name="firstName"]'),
              untranslated:h.includes('recruitment.add_candidate'),
              appGlobalPresent:typeof window.appGlobal!=='undefined'};
        ''')
    }
    /** Called only while an eligible navigation condition is NOT satisfied. */
    boolean inspectNavigation(RecruitmentUi ui,Map page,String phase) {
        if(!(phase in ['TECHNICAL_ROUTE_READY','TECHNICAL_ROUTE_AUTH','CANDIDATES_READY'])) return false
        String target=page.url?.toString()
        if(page.login) {
            // Capture the intended route separately: login URL is not a resume destination.
            String resume=phase=='CANDIDATES_READY' ? RecruitmentUi.ROOT+'viewCandidates' : ui.recoveryRoute
            recover('LOGIN_REDIRECT_DURING_NAVIGATION');reopen(ui,resume);diagnosed=false;return true
        }
        if(diagnosed || !page.domComplete || page.loading || page.challenge || page.networkError ||
            System.nanoTime()-started<Duration.ofMillis(1500).toNanos()) return false
        diagnosed=true
        Map dom=component()
        log('COMPONENT_DIAGNOSTIC',dom+[phase:phase,url:RecruitmentUi.safeUrl(target)])
        try { read([model:'detailed',includeEmployees:'onlyCurrent',limit:1,offset:0]) }
        catch(PimEmployeeFixtures.Fault error) {
            if(!authenticationFailure(error)) throw error
            recover('HTTP_401_DURING_NAVIGATION');reopen(ui,target);diagnosed=false;return true
        }
        if(dom.untranslated==true) {
            if(localizationReloaded) throw new PimEmployeeFixtures.Fault('COMPONENT_LOCALIZATION_UNAVAILABLE',
                'SaveCandidate is mounted with untranslated recruitment.add_candidate. Auth GET is 200; English business locators cannot execute. No assertion bypass')
            localizationReloaded=true
            log('LOCALIZATION_READ_ONLY_REOPEN',[mounted:dom.mounted,apiStatus:200])
            reopen(ui,target);diagnosed=false;started=System.nanoTime();return true
        }
        // API 200 with no form remains a component/readiness issue; keep bounded async wait.
        false
    }
}
