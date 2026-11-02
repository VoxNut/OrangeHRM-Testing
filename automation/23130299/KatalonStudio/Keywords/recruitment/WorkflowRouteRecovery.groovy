package recruitment

import org.openqa.selenium.*
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** A failed action-page GET must not poison the next case's read-only admission.
 * Never retries that action or its Save. Only an owned, journaled route may recover.
 */
class WorkflowRouteRecovery {
    // Explicit opt-in: the ten official PASSED cases retain their original action path.
    static final Set<String> REPAIR_CASES = (['ScheduleInterview_06','ScheduleInterview_07',
        'SelectInterviewer_01','SelectInterviewer_02','SelectInterviewer_03','ValidateAction_01',
        'ValidateAction_02','Status_02','ValidateAction_03','CancelReject_01','VerifyDetails_01',
        'History_01','History_02','History_03','History_04'].collect {'RECRUIT_FLOW_'+it}) as Set
    static boolean repairable(String id) { REPAIR_CASES.contains(id) }
    int restorationSeconds=20
    static Map copy(Map value) { new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(value),Map) as Map }
    static void noteFlow(WorkflowChecks ui,Map d,String action,String status) {
        assert repairable(ui.caseId)
        ui.requireOwned(d)
        String id=CandidateChecks.candidateId(d.url.toString())
        String selected=[Shortlist:'2',Reject:'3','Schedule Interview':'4'][action]
        assert id && selected
        // Capture complete original ownership/operation context, never a replacement fixture.
        Map c=[flowScoped:true,caseId:ui.caseId,candidateId:id,action:action,beforeStatus:status,
            url:RecruitmentUi.ROOT+'changeCandidateVacancyStatus?candidateId='+id+'&selectedAction='+selected,
            executionId:ui.executionContext.executionId,record:copy(d),ledger:ui.ledger.collect {copy(it)},
            operations:ui.workflowAttempts.values().collect {copy(it)},
            managers:copy(ui.executionContext.managerDiscovery.findAll {k,v->k in ['managerA','managerB'] && v?.pimVerified==true}),
            saveReplayed:false,noSaveSent:true]
        ui.executionContext.authentication.pendingActionDocument=c
        ui.executionContext.persist()
    }
    static void noteUncertain(WorkflowChecks ui,Map d,String action) {
        assert repairable(ui.caseId)
        noteFlow(ui,d,action,action=='Schedule Interview' ? WorkflowChecks.SHORT : WorkflowChecks.INITIAL)
        Map pending=ui.executionContext.authentication.pendingActionDocument as Map
        pending.noSaveSent=false;pending.writeUncertain=true
        ui.executionContext.persist()
    }
    protected ExecutionContext context() { ExecutionContext.current() }
    protected String ownedWindow() {
        WebDriver d=browser();ExecutionContext c=context()
        assert c.ownedDriver?.is(d) && c.browserSessionId==ExecutionBrowser.sessionId(d) : 'FLOW recovery driver/session ownership changed'
        assert d.windowHandle && d.windowHandles.contains(d.windowHandle) : 'FLOW owned window unavailable'
        d.windowHandle
    }
    protected Map inspectDocument() { CandidateNavigationRecovery.inspect(browser()) }
    protected void get(String url) {
        assert trusted(url) : 'FLOW GET target must be trusted HTTPS'
        browser().navigate().to(url)
    }
    static boolean trusted(String url) {
        try {
            URI u=new URI(url),base=new URI(RecruitmentUi.BASE)
            u.scheme=='https' && u.host==base.host && u.port==base.port && !u.userInfo && !u.rawFragment &&
                (ApiReadTransport.knownDocumentPath(u.path) || u.path=='/web/index.php/auth/login')
        } catch(Exception ignored) { false }
    }
    protected void logFlow(String event,Map detail=[:]) {
        String text=new com.google.gson.Gson().toJson([event:event,caseId:context().caseId,
            executionId:context().executionId,readOnly:true,noSubmitReplay:true]+detail)
        println('[FLOW DOCUMENT RECOVERY] '+text)
        new File(context().root,'logs/workflow-document-recovery.jsonl').append(text+'\n','UTF-8')
    }
    protected void invariantWindow(String window) { assert ownedWindow()==window : 'FLOW recovery window changed' }
    protected void checkDocument(Map s) {
        if (!trusted(s.url?.toString()) || s.origin!=RecruitmentUi.BASE)
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_ORIGIN','Untrusted effective document; no login/API/write')
        if (s.challenge==true) throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_CHALLENGE','Manual challenge requires operator; no bypass')
    }
    protected Map waitApplication(String window,boolean allowLogin=true) {
        Map result
        new WebDriverWait(browser(),Duration.ofSeconds(restorationSeconds)).pollingEvery(Duration.ofMillis(200)).until {
            invariantWindow(window);Map s=inspectDocument();checkDocument(s)
            if(s.login==true) {
                if(!CandidateNavigationRecovery.confirmedLogin(s) || !allowLogin)
                    throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_AUTH_REDIRECT','Trusted GET returned login; no submit replay')
                result=s;return true
            }
            if(CandidateNavigationRecovery.errorDocument(s))
                throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_DOCUMENT_ERROR','Read-only GET returned browser/server error; inspect HTTP evidence')
            if(CandidateNavigationRecovery.application(s)){result=s;return true}
            false
        }
        result
    }
    /** Calls existing verified auth/identity APIs without changing CAND implementation or scope. */
    protected static class FlowSession extends SessionRecovery {
        final WebDriver owned;final String window
        FlowSession(ExecutionContext c,WebDriver d,String w){super(c);owned=d;window=w}
        protected WebDriver browser() {
            assert context.ownedDriver?.is(owned) && context.browserSessionId==ExecutionBrowser.sessionId(owned)
            assert owned.windowHandle==window && owned.windowHandles.contains(window)
            owned
        }
        protected Map state() {
            Map s=CandidateNavigationRecovery.inspect(browser())
            assert WorkflowRouteRecovery.trusted(s.url?.toString()) && s.origin==RecruitmentUi.BASE : 'Manual login left trusted origin'
            [login:s.login,authenticated:CandidateNavigationRecovery.application(s),ready:s.ready,loading:s.loading]
        }
        void loginFlow(){recoverInternal('FLOW_ACTION_DOCUMENT_LOGIN_REDIRECT')}
        void pinsFlow(Map pinned){verifyIdentities(pinned)}
        void recordFlow(Map row){verifyRecord(null,row)}
    }
    protected FlowSession sessionFlow(String window) { new FlowSession(context(),browser(),window) }
    protected static class FlowReadback extends CandidateReadback {
        final WebDriver owned
        FlowReadback(ExecutionContext c,WebDriver d){super(c);owned=d}
        protected WebDriver browser(){owned} // read() still checks exact execution/session/window ownership.
        Map record(Map d) {
            Map r=read('/api/v2/recruitment/candidates/'+d.candidateId)
            if(!(r.data instanceof Map))throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_SCHEMA','Exact Candidate GET has no data object')
            compare(d,r.data as Map);r.data as Map
        }
    }
    protected Map readRecord(Map d) { new FlowReadback(context(),browser()).record(d) }
    protected void verifyPins(Map j,String window) {
        Map a=j.managers?.managerA
        if(a?.pimVerified!=true || !PimEmployeeFixtures.positive(a?.employeeId) ||
            context().managerDiscovery.managerA?.employeeId?.toString()!=a.employeeId.toString())
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_MANAGER','Original numeric active Hiring Manager proof missing/changed')
        sessionFlow(window).pinsFlow(j.managers as Map)
    }
    protected void verifyVacancy(Map row,String window) { sessionFlow(window).recordFlow(row) }
    protected void manualLogin(String window) {
        logFlow('MANUAL_LOGIN_REQUIRED',[deadlineSeconds:120])
        try {sessionFlow(window).loginFlow();invariantWindow(window);waitApplication(window,false)}
        catch(RuntimeException e) {
            String code=e instanceof PimEmployeeFixtures.Fault ? e.state : e.class.simpleName
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_AUTH_'+code,'Manual login verification failed; no replay',e)
        }
    }
    protected void admissionFatal(Map j) {
        ExecutionContext c=context()
        if(c.pimReadFailure && !SessionRecovery.authenticationFailure(c.pimReadFailure)) throw c.pimReadFailure
        Map f=c.technicalPreflightFailure as Map,own=c.authentication.workflowRecoveryDenial as Map
        if(f && !(own && own.executionId==c.executionId && own.pendingCaseId==j.caseId &&
            own.caseId==f.caseId && own.caseRunId==f.caseRunId && own.state==f.state &&
            f.state?.toString()?.startsWith('FLOW_RECOVERY_'))) {
            RuntimeException original=c.preflightErrors[f.caseId]
            if(original)throw original
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_UNRELATED_FATAL','Unrelated admission failure retained')
        }
        if(c.authentication.exhausted==true && c.authentication.reason &&
            c.authentication.reason!='LOGIN_TIMEOUT_OR_LOST')
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_UNRELATED_FATAL','Unrelated exhausted circuit retained')
    }
    protected void requestBudget(Map j,String window) {
        assert j.executionId==context().executionId && repairable(j.caseId?.toString()) && j.noSaveSent instanceof Boolean
        assert WorkflowChecks.actionCandidateId(j.url?.toString())==j.candidateId?.toString()
        if(j.window && j.window!=window)throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_WINDOW','Journal window differs')
        j.window=window
        int attempts=(context().authentication.workflowRecoveryAttempts ?: 0) as int
        if(attempts>=3)throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_BUDGET_EXHAUSTED','Three FLOW restoration attempts used; no loop')
        context().authentication.workflowRecoveryAttempts=attempts+1;context().persist()
    }
    protected void establish(Map j,String window) {
        invariantWindow(window);Map s=inspectDocument();checkDocument(s)
        boolean action=WorkflowChecks.actionCandidateId(s.url?.toString())==j.candidateId?.toString() &&
            WorkflowChecks.actionSelection(s.url?.toString())==WorkflowChecks.actionSelection(j.url.toString())
        boolean dashboard=RecruitmentUi.sameRoute(s.url?.toString(),RecruitmentUi.DASHBOARD)
        if(!action && !dashboard && !CandidateNavigationRecovery.confirmedLogin(s))
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_CONTEXT','Browser left journaled action/Dashboard/login; no speculative adoption')
        logFlow('DOCUMENT_PROBE',[candidateId:j.candidateId,mounted:s.mounted,authenticated:s.authenticated,
            loading:s.loading,login:s.login,url:RecruitmentUi.safeUrl(s.url?.toString()),resourceFailures:s.resourceFailures])
        if(s.login==true)manualLogin(window)
        else if(!CandidateNavigationRecovery.application(s)) {
            get(RecruitmentUi.DASHBOARD);s=waitApplication(window)
            if(s.login==true)manualLogin(window)
        }
    }
    protected void reconcile(Map j,String window) {
        Map d=j.record as Map
        if(!d || d.ownerExecutionId!=context().executionId || d.ownership==null || d.recordVerified!=true ||
            d.candidateId?.toString()!=j.candidateId?.toString() || CandidateChecks.candidateId(d.url?.toString())!=j.candidateId?.toString())
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_OWNERSHIP','Original case-owned Candidate proof missing')
        verifyPins(j,window)
        Map row=readRecord(d) // Full original fields + numeric Vacancy association, no alternate Candidate.
        logFlow('CANDIDATE_GET_RECONCILED',[candidateId:j.candidateId,statusId:row.status?.id,originalCase:j.caseId])
        if(j.writeUncertain==true || (j.operations as List).any {it.state!='verified'})
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_UNCERTAIN_WRITE','GET observed original Candidate but pending operation/history remains unverified; no replay/admission')
        Map statuses=[(WorkflowChecks.INITIAL):1,(WorkflowChecks.SHORT):2,(WorkflowChecks.REJECTED):3,(WorkflowChecks.SCHEDULED):4]
        if(row.status?.id!=statuses[j.beforeStatus])
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_STATUS_CHANGED','Persisted workflow state differs; do not replay previous action')
        List<Map> records=(j.ledger as List<Map>).findAll {it.ownerExecutionId==context().executionId &&
            it.caseId==j.caseId && it.ownership==d.ownership}
        List<Map> vacancies=records.findAll {it.kind in ['vacancy','vacancy-saved'] && it.name==d.vacancy}
        List<Map> candidates=records.findAll {it.kind in ['candidate-save','candidate-saved'] && it.url==d.url}
        if(!vacancies || !candidates)throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_OWNERSHIP','Original case ledger lacks Candidate/Vacancy proof')
        Map vacancy=vacancies.last(),candidate=candidates.last()
        if(!(vacancy.state in ['confirmed','verified']) || vacancy.url!=d.vacancyUrl ||
            candidate.state!='verified' || candidate.persistedVerified!=true)
            throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_UNCERTAIN_WRITE','Original Candidate/Vacancy create outcome remains unresolved')
        verifyVacancy(vacancy,window) // Existing fresh numeric Vacancy UI GET and complete expected field proof.
        get(d.url.toString());waitApplication(window,false)
        // Recheck server-rendered status and allowed action on exact original profile, never click it again.
        new WebDriverWait(browser(),Duration.ofSeconds(restorationSeconds)).pollingEvery(Duration.ofMillis(200)).until {
            invariantWindow(window);Map s=inspectDocument();checkDocument(s)
            if(s.login)throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_AUTH_REDIRECT','Owned Profile GET returned login')
            if(!CandidateNavigationRecovery.application(s) || !RecruitmentUi.sameRoute(s.url?.toString(),d.url.toString()))return false
            Map markers=profileMarkers(j)
            markers.identity==true && markers.status==true && markers.allowed==true
        }
    }
    protected Map profileMarkers(Map j) {
        Map d=j.record as Map
        String button="//div[${RecruitmentUi.cls('orangehrm-recruitment-actions')}]"+RecruitmentUi.button(j.action.toString()).substring(1)
        (Map)((JavascriptExecutor)browser()).executeScript('''
            const first=x=>{const r=document.evaluate(x,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
              return r.snapshotLength===1?r.snapshotItem(0):null;};
            const paths=arguments[0],v=arguments[1],a=first(paths.action),s=first(paths.status);
            const value=x=>first(x)?.value;
            return {identity:value(paths.first)===v.first&&value(paths.last)===v.last&&value(paths.email)===v.email,
              status:s?.textContent.trim()==='Status: '+v.status,
              allowed:!!a&&a.getClientRects().length>0&&!a.disabled};
        ''',[first:CandidateChecks.FIRST,last:CandidateChecks.LAST,email:RecruitmentUi.field('Email'),
            status:"//p[starts-with(normalize-space(),'Status:')]",action:button],
            [first:d.first,last:d.last,email:d.email,status:j.beforeStatus])
    }
    protected void clearOwnDenial(Map j) {
        ExecutionContext c=context();Map own=c.authentication.workflowRecoveryDenial as Map
        if(own && c.technicalPreflightFailure?.caseId==own.caseId && c.technicalPreflightFailure?.caseRunId==own.caseRunId &&
            c.technicalPreflightFailure?.state==own.state && own.pendingCaseId==j.caseId && own.executionId==c.executionId)
            c.technicalPreflightFailure=null // Historical case decisions/errors/ledger remain immutable evidence.
        c.authentication.remove('workflowRecoveryDenial')
    }
    void restoreAction(WorkflowChecks ui,Map d,String action,String status,Closure fullMarkers) {
        assert repairable(ui.caseId) && ui.executionContext.is(context())
        ui.requireOwned(d);Map j=context().authentication.pendingActionDocument as Map
        assert j.caseId==ui.caseId && j.action==action && j.beforeStatus==status && j.noSaveSent==true
        if(j.actionRestorationAttempted==true)throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_BUDGET_EXHAUSTED','Only one action-form restoration per case')
        j.actionRestorationAttempted=true
        admissionFatal(j);String window=ownedWindow();requestBudget(j,window)
        establish(j,window)
        try {reconcile(j,window)}
        catch(PimEmployeeFixtures.Fault e) {
            if(!SessionRecovery.authenticationFailure(e))throw e
            manualLogin(window);reconcile(j,window) // One authenticated GET reconciliation, never a write retry.
        }
        get(j.url.toString()) // Exact source-backed action GET remount, not another native action click.
        waitApplication(window,false)
        new WebDriverWait(browser(),Duration.ofSeconds(restorationSeconds)).pollingEvery(Duration.ofMillis(200)).until {
            invariantWindow(window);Map s=inspectDocument();checkDocument(s)
            if(s.login)throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_AUTH_REDIRECT','Action GET returned login')
            CandidateNavigationRecovery.application(s) && WorkflowChecks.actionCandidateId(s.url?.toString())==j.candidateId?.toString() &&
                WorkflowChecks.actionSelection(s.url?.toString())==WorkflowChecks.actionSelection(j.url.toString()) && fullMarkers.call()
        }
        clearOwnDenial(j);context().authentication.remove('pendingActionDocument');context().persist()
        logFlow('ACTION_FORM_RESTORED',[candidateId:j.candidateId,fullMarkers:true])
    }
    protected void restoreAdmission(String nextCase,Map j) {
        if(!repairable(nextCase))throw new PimEmployeeFixtures.Fault('FLOW_RECOVERY_SCOPE','Pending failed action must be reconciled before a protected case is admitted')
        try {
            admissionFatal(j);String window=ownedWindow();requestBudget(j,window)
            // Failed restoration may have ended on an owned record; return only through known safe Dashboard.
            Map s=inspectDocument();checkDocument(s)
            boolean ownedRecord=RecruitmentUi.sameRoute(s.url?.toString(),j.record?.url?.toString()) ||
                RecruitmentUi.sameRoute(s.url?.toString(),j.record?.vacancyUrl?.toString())
            if(ownedRecord){get(RecruitmentUi.DASHBOARD);s=waitApplication(window)}
            establish(j,window)
            try {reconcile(j,window)}
            catch(PimEmployeeFixtures.Fault e) {
                if(!SessionRecovery.authenticationFailure(e))throw e
                manualLogin(window);reconcile(j,window)
            }
            get(RecruitmentUi.DASHBOARD);waitApplication(window,false)
            clearOwnDenial(j);context().authentication.remove('pendingActionDocument');context().persist()
            logFlow('NEXT_CASE_ADMITTED',[originalCase:j.caseId,candidateId:j.candidateId,previousCaseResultUnchanged:true])
        } catch(RuntimeException e) {
            String cause=e instanceof PimEmployeeFixtures.Fault ? e.state : e.class.simpleName
            String state=cause.startsWith('FLOW_RECOVERY_') ? cause : 'FLOW_RECOVERY_'+cause
            RuntimeException exact=e instanceof PimEmployeeFixtures.Fault && e.state==state ? e :
                new PimEmployeeFixtures.Fault(state,'FLOW read-only restoration failed; no fixture/action replay',e)
            if(state.startsWith('FLOW_RECOVERY_'))context().authentication.workflowRecoveryDenial=[caseId:nextCase,
                caseRunId:context().caseRunId,state:state,executionId:context().executionId,pendingCaseId:j.caseId]
            context().persist();throw exact
        }
    }


    static void note(ExecutionContext c, String candidateId, String url) {
        if (WorkflowChecks.actionCandidateId(url) != candidateId) return
        c.authentication.pendingActionDocument = [caseId:c.caseId, candidateId:candidateId, url:url,
            executionId:c.executionId, saveReplayed:false]
        c.persist()
    }
    protected WebDriver browser() { ExecutionBrowser.driver() }
    protected void navigate() { browser().navigate().to(RecruitmentUi.DASHBOARD) }
    protected Map document() {
        (Map)((JavascriptExecutor)browser()).executeScript('''
            const visible=n=>n&&n.getClientRects().length>0;
            return {url:location.href,ready:document.readyState==='complete',
                login:!!document.querySelector('input[name="username"]'),
                authenticated:visible(document.querySelector('aside nav'))&&visible(document.querySelector('header h6')),
                loading:!!document.querySelector('.oxd-loading-spinner,.oxd-form-loader')};
        ''')
    }
    void beforeAdmission(String nextCase) {
        ExecutionContext c=ExecutionContext.current()
        Map pending=c.authentication.pendingActionDocument as Map
        if (!pending || !nextCase.startsWith('RECRUIT_FLOW_') || pending.caseId==nextCase) return
        assert pending.executionId==c.executionId && WorkflowChecks.actionCandidateId(pending.url.toString())==pending.candidateId
        if (pending.flowScoped==true) { restoreAdmission(nextCase,pending); return }
        // Both the original action and its HTTP outcome remain failed in their own case.
        // Fatal read/auth faults are never cleared here, and wrong/unknown routes are never adopted.
        if(c.pimReadFailure) throw c.pimReadFailure
        SessionGuard.requireAllowance()
        assert WorkflowChecks.actionCandidateId(browser().currentUrl)==pending.candidateId : 'Browser left the journaled action route; no speculative recovery'
        navigate() // One safe GET; no candidate state mutation, automatic login or action replay.
        new WebDriverWait(browser(),Duration.ofSeconds(30)).pollingEvery(Duration.ofMillis(200)).until {
            Map s=document()
            if(s.login || s.url?.toString()?.contains('/auth/')) SessionGuard.lost('Safe route recovery redirected to login')
            RecruitmentUi.sameRoute(s.url?.toString(),RecruitmentUi.DASHBOARD) && s.ready && s.authenticated && !s.loading
        }
        println('[WORKFLOW ROUTE RECOVERY] Safe authenticated Dashboard verified before fresh identity admission; no action/Save replay')
        c.authentication.remove('pendingActionDocument'); c.persist()
    }
}
