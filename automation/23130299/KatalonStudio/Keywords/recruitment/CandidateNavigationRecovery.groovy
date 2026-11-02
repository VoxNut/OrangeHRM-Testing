package recruitment

import org.openqa.selenium.*
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Source proof: SaveCandidate.onCancel -> viewCandidates; OXD navigation performs
 * full document GETs. Only recover a failed read document, never a business action.
 * See maintenance/cand-navigation-repair/frontend-source-proof.md (pre-edit audit).
 */
class CandidateNavigationRecovery {
    static final String LIST=RecruitmentUi.ROOT+'viewCandidates'
    static final int MAX_EXECUTION_ATTEMPTS=3
    static final Set<String> DOCUMENT_FAILURES=['CANDIDATE_DOCUMENT_UNAVAILABLE','CANDIDATE_DOCUMENT_RECOVERY_EXHAUSTED','CANDIDATE_DOCUMENT_CONTEXT_INVALID'] as Set
    final ExecutionContext context
    int seconds=20
    CandidateNavigationRecovery(ExecutionContext c=ExecutionContext.current()){context=c}
    protected WebDriver browser(){ExecutionBrowser.driver()}
    protected SessionRecovery session(){new SessionRecovery(context)}
    static Map inspect(WebDriver driver) {
        (Map)((JavascriptExecutor)driver).executeScript('''
            const visible=n=>!!n&&n.isConnected&&Array.from(n.getClientRects()).some(r=>r.width>0&&r.height>0);
            const text=(document.body?.innerText||'').toLowerCase();
            return {url:location.href,documentUrl:document.URL,origin:location.origin,
                mounted:!!document.querySelector('#app[data-v-app]'),ready:document.readyState==='complete',
                profileFields:!!document.querySelector('input[name="firstName"]'),appGlobalPresent:typeof window.appGlobal!=='undefined',
                vueAppPresent:!!document.querySelector('#app')?.__vue_app__,
                resourceFailures:typeof performance==='undefined'?[]:performance.getEntriesByType('resource')
                    .filter(e=>e.responseStatus>=400).map(e=>({path:new URL(e.name,location.href).pathname,status:e.responseStatus,kind:e.initiatorType})).slice(-12),
                runtimeExceptionEvidence:'Not captured by document probe; requires browser console evidence',
                login:location.pathname.startsWith('/web/index.php/auth/')||visible(document.querySelector('input[name="username"]')),
                authenticated:visible(document.querySelector('aside nav'))&&visible(document.querySelector('header h6')),
                loading:Array.from(document.querySelectorAll('.oxd-loading-spinner,.oxd-form-loader')).some(visible),
                chromeError:location.protocol==='chrome-error:',
                challenge:/verify you are human|checking your browser|just a moment/.test(text),
                visibleHttpError:(text.match(/http error ([1-5][0-9][0-9])/)||[])[1]||null,
                networkError:/http error 500|this page isn.t working|err_connection|err_name_not_resolved|err_internet_disconnected|this site can.t be reached|502 bad gateway|503 service unavailable|504 gateway time/.test(text),
                untranslated:Array.from(document.querySelectorAll('h5,h6')).some(n=>['general.candidates','recruitment.candidate_profile','recruitment.add_candidate'].includes(n.textContent.trim()))};
        ''')
    }
    protected Map document(){inspect(browser())}
    protected Map diagnosticDocument(){
        try {return document()}
        catch(Exception unavailable){return [unavailable:unavailable.class.simpleName]}
    }
    static boolean errorDocument(Map s){s.chromeError==true || s.url?.toString()?.startsWith('chrome-error://') ||
        (s.networkError==true && !s.mounted && s.authenticated!=true)}
    static boolean trustedTarget(String url){
        try {ApiReadTransport.resolve(url,'/api/v2/pim/employees');return url.startsWith(RecruitmentUi.ROOT)}
        catch(PimEmployeeFixtures.Fault ignored){return false}
    }
    static boolean application(Map s){
        s.origin==RecruitmentUi.BASE && s.mounted==true && s.authenticated==true && s.ready==true &&
            s.login!=true && s.loading!=true && s.challenge!=true && !errorDocument(s)
    }
    static boolean confirmedLogin(Map s){s.login==true && s.origin==RecruitmentUi.BASE &&
        s.url?.toString()?.startsWith(RecruitmentUi.BASE+'/web/index.php/auth/')}
    protected void log(String event,Map detail=[:]){
        Map row=[event:event,caseId:context.caseId,executionId:context.executionId,readOnly:true]+detail
        if(event in ['REQUESTED','STOPPED'])row.browserConsole=consoleEvidence()
        if(row.document instanceof Map)row.document=new LinkedHashMap(row.document)+[
            url:RecruitmentUi.safeUrl(row.document.url?.toString()),documentUrl:RecruitmentUi.safeUrl(row.document.documentUrl?.toString())]
        String text=new com.google.gson.Gson().toJson(row)
        println('[CANDIDATE DOCUMENT RECOVERY] '+text)
        new File(context.root,'logs/candidate-document-recovery.jsonl').append(text+'\n','UTF-8')
    }
    protected Map consoleEvidence(){
        try {
            List entries=browser().manage().logs().get(org.openqa.selenium.logging.LogType.BROWSER).all
            List failures=entries.findAll{it.level.intValue()>=java.util.logging.Level.WARNING.intValue()}.takeRight(20).collect { entry ->
                String message=entry.message ?: ''
                String category=message.toLowerCase().contains('vue') ? 'VUE_RUNTIME' :
                    message==~ /(?s).*(TypeError|ReferenceError|SyntaxError|RangeError|uncaught|Uncaught).*/ ? 'JAVASCRIPT_EXCEPTION' :
                    message.toLowerCase().contains('failed to load resource') ? 'RESOURCE_LOAD' : 'OTHER_BROWSER_WARNING'
                [category:category,level:entry.level.name,
                    errorType:['TypeError','ReferenceError','SyntaxError','RangeError'].find{message.contains(it)}]
            }
            return [available:true,failures:failures,rawMessagesOmitted:true]
        } catch(Exception unavailable){return [available:false,errorType:unavailable.class.simpleName,rawMessagesOmitted:true]}
    }
    protected String ownedWindow(){
        WebDriver d=browser()
        assert context.ownedDriver?.is(d) && context.browserSessionId==ExecutionBrowser.sessionId(d) : 'Candidate recovery driver/session ownership differs'
        String handle=d.windowHandle
        assert handle && d.windowHandles.contains(handle) : 'Candidate recovery needs the existing owned window'
        handle
    }
    protected void navigate(){browser().navigate().to(LIST)}
    private static class LoginDocument extends RuntimeException {}
    protected void waitForList(RecruitmentUi ui,String window){
        new WebDriverWait(browser(),Duration.ofSeconds(seconds)).pollingEvery(Duration.ofMillis(200)).until {
            assert ownedWindow()==window : 'Window changed during read-only Candidates recovery'
            Map s=document()
            if(s.login){
                if(!confirmedLogin(s))throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_CONTEXT_INVALID','Login markers on an untrusted document; no authentication bypass')
                throw new LoginDocument()
            }
            if(errorDocument(s))throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_UNAVAILABLE',
                'Safe Candidates GET still returned a browser/server error document; no API or Save replay')
            if(!application(s) || !RecruitmentUi.sameRoute(s.url?.toString(),LIST))return false
            // Reuse exactly the original semantic filter/table-or-empty condition.
            Map page=ui ? ui.pageState() : listPage()
            boolean ready=RecruitmentUi.candidateListReady(page)
            if(!ready && s.untranslated==true)throw new PimEmployeeFixtures.Fault('COMPONENT_LOCALIZATION_UNAVAILABLE',
                'Candidates is mounted with raw translation keys but mandatory business controls are unavailable; assertions unchanged')
            ready
        }
    }
    protected void waitAfterManualLogin(RecruitmentUi ui,String window){
        try {waitForList(ui,window)}
        catch(LoginDocument expired){
            SessionGuard.failedLogin()
            throw new PimEmployeeFixtures.Fault('SESSION_EXPIRED','Candidates GET returned login after manual recovery; no Save replay')
        }
    }
    protected Map listPage(){
        Map flags=(Map)((JavascriptExecutor)browser()).executeScript('''
            const flags={};for(const [k,xp] of Object.entries(arguments[0])){
                const s=document.evaluate(xp,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null),a=[];
                for(let i=0;i<s.snapshotLength;i++){const n=s.snapshotItem(i);if(n.getClientRects().length)a.push(n);}
                flags[k]=['vacancy','candidateName','search','add'].includes(k)?a.length===1&&!a[0].disabled:a.length>0;
            }return flags;
        ''',RecruitmentUi.CANDIDATE_MARKERS)
        Map s=document()
        [candidates:RecruitmentUi.sameRoute(s.url?.toString(),LIST),authenticated:application(s),login:s.login==true,
            domComplete:s.ready==true,loading:s.loading==true,challenge:s.challenge==true,networkError:errorDocument(s),markers:flags]
    }
    protected void verifyPinned(){
        if(context.managerDiscovery.managerA?.pimVerified!=true || !PimEmployeeFixtures.positive(context.managerDiscovery.managerA?.employeeId))
            throw new PimEmployeeFixtures.Fault('RECOVERY_EMPLOYEE_UNVERIFIED','Original numeric active Manager proof missing; recovery cannot select a replacement')
        session().verifyCandidateIdentities()
    }
    protected void verifyPending(){
        if(context.authentication.candidatePending && context.pimReadFailure)throw context.pimReadFailure
        Map fatal=context.technicalPreflightFailure
        if(context.authentication.candidatePending && fatal && !DOCUMENT_FAILURES.contains(fatal.state?.toString()) &&
            !SessionRecovery.authenticationFailure(fatal)){
            RuntimeException canonical=context.preflightErrors[fatal.caseId]
            if(canonical)throw canonical
            throw new PimEmployeeFixtures.Fault(fatal.state.toString(),'Original non-document admission failure remains; no repeated reconciliation read')
        }
        new CandidateReadback(context).pending()
    }
    static boolean restorable(Map s,String target){
        trustedTarget(target) && s.origin==RecruitmentUi.BASE && s.ready==true && s.challenge!=true &&
            s.login!=true && sameDocument(s,target) && !errorDocument(s) &&
            (s.mounted!=true || (CandidateChecks.candidateRecordUrl(target) && s.profileFields==false))
    }
    private static boolean sameDocument(Map s,String target){
        RecruitmentUi.sameRoute(s.url?.toString(),target) && RecruitmentUi.sameRoute(s.documentUrl?.toString(),target)
    }
    private boolean cachedChromeOrigin(RuntimeException fault){
        fault instanceof PimEmployeeFixtures.Fault && fault.state=='PIM_API_BASE_URL_MISSING_OR_INVALID' &&
            fault.diagnostic?.error=='ORIGIN' && fault.diagnostic?.documentOrigin=='null' &&
            fault.diagnostic?.documentPath in ['/','/chromewebdata/']
    }
    protected void recover(RecruitmentUi ui,String phase){
        String window=ownedWindow(),target=browser().currentUrl
        if(!trustedTarget(target))throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_CONTEXT_INVALID','Untrusted/unknown WebDriver route; no speculative navigation')
        if(ui)SessionRecovery.currentRecords(ui) // Uncertain Save blocks even read-navigation resumption.
        Map budget=context.authentication.candidateNavigation ?: [attempts:0,cases:[:]]
        context.authentication.candidateNavigation=budget
        int used=(budget.cases[context.caseRunId] ?: 0) as int
        if(used>=1 || (budget.attempts as int)>=MAX_EXECUTION_ATTEMPTS)
            throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_RECOVERY_EXHAUSTED','One attempt per case / three per execution exhausted; no loop or write replay')
        RuntimeException saved=context.pimReadFailure
        if(saved && !cachedChromeOrigin(saved) && !SessionRecovery.authenticationFailure(saved))throw saved
        budget.cases[context.caseRunId]=used+1;budget.attempts=(budget.attempts as int)+1;context.persist()
        log('REQUESTED',[phase:phase,target:RecruitmentUi.safeUrl(target),document:document(),attempt:budget.attempts,
            navigationDeadlineSeconds:seconds,manualLoginDeadlineSeconds:120,noSubmitReplay:true])
        try {
            if(SessionRecovery.authenticationFailure(saved)){
                if(ui)session().recoverCandidateList(ui)
                else session().recoverCandidateAdmission(context.caseId)
                saved=context.pimReadFailure
            }
            navigate() // Explicit source-backed route, one read-only retry, never the failing menu/action.
            try {waitForList(ui,window)}
            catch(LoginDocument expired){
                if(ui)session().recoverCandidateList(ui)
                else {session().recoverCandidateAdmission(context.caseId);navigate()}
                waitAfterManualLogin(ui,window)
            }
            // Bypass only a proven cached Chrome-origin fault for verification reads;
            // retain any NEW failure. Never clears HTTP403/500/transport/identity failures.
            if(cachedChromeOrigin(saved))context.pimReadFailure=null
            verifyPinned()
            verifyPending() // An uncertain write must be read-reconciled before any subsequent case is admitted.
            assert ownedWindow()==window && application(document()) : 'Recovery lost verified application context'
            if(cachedChromeOrigin(saved) && context.pimReadFailure==null){
                context.pimFixtures.remove('readFailure')
                if(context.technicalPreflightFailure?.state==saved.state &&
                    context.preflightErrors[context.technicalPreflightFailure.caseId]?.is(saved))context.technicalPreflightFailure=null
            }
            Map technical=context.technicalPreflightFailure
            if(technical && DOCUMENT_FAILURES.contains(technical.state?.toString()) && technical.caseId==budget.lastFailedCase)
                context.technicalPreflightFailure=null
            context.menuVerified=true;context.persist()
            log('VERIFIED',[phase:phase,target:LIST,attempt:budget.attempts,remainingAssertions:'Original list, exact owned numeric candidate reopen and full profile checks still required'])
        } catch(RuntimeException failure){
            if(saved && context.pimReadFailure==null)context.pimReadFailure=saved
            budget.lastFailedCase=context.caseId;context.persist()
            log('STOPPED',[phase:phase,errorType:failure.class.name,document:diagnosticDocument(),
                state:failure instanceof PimEmployeeFixtures.Fault ? failure.state : 'NAVIGATION_TIMEOUT',noSubmitReplay:true])
            if(failure instanceof PimEmployeeFixtures.Fault)throw failure
            throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_UNAVAILABLE','Read-only Candidates recovery did not verify real application/list; no business assertions completed',failure)
        }
    }
    boolean inspectNavigation(RecruitmentUi ui,Map page,String phase){
        if(!ui.caseId.startsWith('RECRUIT_CAND_') || phase!='CANDIDATES_READY')return false
        Map s=document()
        if(errorDocument(s)){recover(ui,phase);return true}
        if(s.login){
            assert confirmedLogin(s) : 'Untrusted login document'
            String window=ownedWindow();session().recoverCandidateList(ui);waitAfterManualLogin(ui,window);return true
        }
        false
    }
    void beforeAdmission(String id){
        if(!id.startsWith('RECRUIT_CAND_') || !context.ownedDriver || context.authentication.authenticated!=true)return
        Map s=document()
        boolean knownAuth=SessionRecovery.authenticationFailure(context.pimReadFailure) || SessionRecovery.authenticationFailure(context.technicalPreflightFailure)
        if(knownAuth){
            if(!confirmedLogin(s) && !application(s) && !(errorDocument(s)&&trustedTarget(browser().currentUrl)))
                throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_CONTEXT_INVALID','Cached authentication fault on untrusted document; no bypass')
            ownedWindow();session().recoverCandidateAdmission(id);s=document()
        }
        if(errorDocument(s) || restorable(s,browser().currentUrl)){recover(null,'BEFORE_PIM');return}
        if(s.login){
            if(!confirmedLogin(s))throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_CONTEXT_INVALID','Untrusted login document before PIM')
            ownedWindow();session().recoverCandidateAdmission(id);s=document()
        }
        if(!application(s)){
            // about:blank/foreign/unmounted contexts cannot dispatch a PIM request.
            throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_CONTEXT_INVALID','Before PIM: current document is not a mounted authenticated same-origin OrangeHRM page')
        }
        verifyPending()
        // A failed navigation can be followed by an already restored app document.
        // Clear ONLY this helper's previous document denial after fresh pinned-ID proof.
        Map technical=context.technicalPreflightFailure,budget=context.authentication.candidateNavigation as Map
        if(technical && DOCUMENT_FAILURES.contains(technical.state?.toString()) && technical.caseId==budget?.lastFailedCase){
            String window=ownedWindow();verifyPinned()
            assert ownedWindow()==window && application(document()) : 'Document changed during identity revalidation'
            context.technicalPreflightFailure=null;context.persist()
            log('EXISTING_DOCUMENT_VERIFIED',[previousCase:technical.caseId,noNavigation:true,noSubmitReplay:true])
        }
    }
}
