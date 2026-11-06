package recruitment

import com.google.gson.Gson
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.WebDriverWait
import java.security.MessageDigest
import java.time.Duration

/** Execution-scoped PIM preconditions. All writes are one native Add Employee Save,
 * behind an explicit opt-in. No employee update/delete or login-account creation exists here.
 */
class PimEmployeeFixtures {
    static class Fault extends IllegalStateException {
        final String state
        Map diagnostic=[:]
        Fault(String state,String detail,Throwable cause=null){super('['+state+'] '+detail,cause);this.state=state}
    }
    static final Set<String> SKIPPABLE=['NO_ACTIVE_EMPLOYEE_AVAILABLE','PIM_PERMISSION_DENIED','EMPLOYEE_CREATION_DISABLED'] as Set
    final RecorderBridge runner
    final ExecutionContext context
    final File ledgerFile

    PimEmployeeFixtures(RecorderBridge runner) {
        this.runner=runner;context=runner.executionContext
        ledgerFile=new File(context.root,'pim-fixtures.json')
        if (ledgerFile.isFile() && context.pimFixtures.employees.isEmpty()) {
            Map saved=new Gson().fromJson(ledgerFile.getText('UTF-8'),Map)
            if (saved.executionId!=context.executionId || saved.schema!=1 || !(saved.fixtures instanceof Map) ||
                !(saved.fixtures.employees instanceof Map) || !saved.fixtures.employees.keySet().every {it in ['A','B']})
                throw new Fault('EMPLOYEE_CREATION_FAILED','Foreign/invalid PIM ledger; no write or adoption')
            context.pimFixtures=saved.fixtures
        }
    }
    static String fullName(Map e){[e.firstName,e.middleName,e.lastName].findAll {it}.join(' ')}
    static boolean positive(Object id){id?.toString()==~ /[1-9]\d*/}
    static Map draft(String executionId,String slot,String caseId='',String caseRunId='') {
        assert slot in ['A','B'] && executionId==~ /[0-9a-f-]{36}/
        String hash=MessageDigest.getInstance('SHA-256').digest((executionId+':'+slot).getBytes('UTF-8')).encodeHex().toString()
        [slot:slot,executionId:executionId,caseId:caseId,caseRunId:caseRunId,
         firstName:'ASM23130299',middleName:'Manager',lastName:'R'+executionId.replace('-','').take(16)+slot,
         employeeCode:'S'+hash.take(9),ownershipMarker:'ASM23130299:'+executionId+':'+slot,
         employeeId:null,createdByExecution:false,saveAttempted:false,state:'PREPARED',
         preparedAt:java.time.OffsetDateTime.now().toString(),cleanupStatus:'NOT_CREATED',vacancyIds:[],candidateIds:[]]
    }
    static Map employee(Map raw) {
        if (!(raw.empNumber instanceof Number) || !positive(raw.empNumber) || !(raw.firstName instanceof String) || !(raw.lastName instanceof String) ||
            !raw.containsKey('terminationId') || !(raw.terminationId==null || (raw.terminationId instanceof Number && positive(raw.terminationId)))) throw new Fault('PIM_API_RESPONSE_SCHEMA_INVALID','PIM response lacks source-declared numeric empNumber/name/current status fields')
        [employeeId:raw.empNumber.toString(),firstName:raw.firstName,middleName:raw.middleName ?: '',lastName:raw.lastName,
         employeeCode:raw.employeeId ?: '',active:!raw.terminationId,displayName:fullName(raw)]
    }
    static boolean samePerson(Map expected,Map actual) {
        actual && actual.active==true && ['firstName','middleName','lastName'].every {(expected[it] ?: '')==(actual[it] ?: '')} &&
            (!expected.employeeId || expected.employeeId.toString()==actual.employeeId?.toString()) &&
            (!expected.employeeCode || expected.employeeCode==actual.employeeCode)
    }
    void persist() {
        AtomicFiles.json(ledgerFile,[schema:1,executionId:context.executionId,allowCreate:context.allowPimFixtureCreate,
            fixtures:context.pimFixtures,cleanupPolicy:'MANUAL_CONFIRMATION_ONLY; exact IDs and dependencies; never prefix deletion'])
        context.persist()
    }
    protected void ensureSession() {
        SessionGuard.requireAllowance()
        if(runner.hasBrowser() && RecruitmentUi.sameRoute(runner.driver().currentUrl,RecruitmentUi.ROOT+'viewJobVacancy') && VacancyTable.state(runner.driver()).ready) {
            String diagnostic=vacancyManagerAvailability(VacancyTable.readRows(runner.driver()))
            if(diagnostic=='ONLY_DELETED_VACANCY_MANAGERS' && !context.pimFixtures.diagnostics.any {it.state==diagnostic}) {
                context.pimFixtures.diagnostics << [state:diagnostic,blocking:false,detail:'Observed historical Vacancy labels; PIM discovery continues']
                persist()
            }
        }
        if (!context.authentication.authenticated) runner.go('/web/index.php/dashboard/index')
        if (runner.loginVisible()) throw new Fault('SESSION_EXPIRED','Login must be completed in the execution-owned browser')
    }
    static String vacancyManagerAvailability(List<Map> rows) {
        rows && rows.every {VacancyTable.normalizeCellText(it['Hiring Manager']?.toString()) in ['(Deleted)','Deleted']} ?
            'ONLY_DELETED_VACANCY_MANAGERS' : 'PIM_DISCOVERY_REQUIRED'
    }
    protected Map read(Map params) {
        (Map)runner.measure('PIM_READ') { readInternal(params) }
    }
    protected Map readInternal(Map params) {
        SessionGuard.requireAllowance()
        assert params.keySet().every {it in ['model','includeEmployees','limit','offset','nameOrId','empNumber','employeeId','sortField','sortOrder']}
        String operationId=UUID.randomUUID().toString()
        long started=System.nanoTime()
        Map response
        Fault failure=null
        WebDriver transport=runner.driver()
        try {
            response=ApiReadTransport.request(transport,'/api/v2/pim/employees',params)
            response.operationId=operationId
            return response
        } catch(Fault error) {failure=error;throw error}
        finally {
            diagnostic([operationId:operationId,kind:'PIM_GET',currentUrl:RecruitmentUi.safeUrl(transport.currentUrl),
                endpointPath:'/web/index.php/api/v2/pim/employees',params:params,
                httpStatus:response?.status ?: failure?.diagnostic?.status,errorState:failure?.state,rootExceptionType:failure ? (failure.cause ?: failure).class.name : null,rootExceptionMessage:failure ? ExecutionReport.clean(failure.message) : null,total:response?.total,rowCount:response?.data?.size(),
                elapsedMs:(System.nanoTime()-started).intdiv(1000000L)])
        }
    }
    protected void diagnostic(Map details) {
        Map row=[executionId:context.executionId,caseId:context.caseId,caseRunId:context.caseRunId]+details
        println('[PIM DIAGNOSTIC] '+new Gson().toJson(row))
        new File(context.root,'logs/pim-read.jsonl').append(new Gson().toJson(row)+'\n','UTF-8')
    }
    /** Overlap is usable for positive identity discovery, never evidence of absence.
     * Negative decisions require two consecutive complete, duplicate-free identical scans.
     * This is read consistency evidence, not server snapshot isolation on the public demo.
     */
    protected List<Map> inventory(Closure accept=null) {
        List<Map> cached=(context.pimFixtures.inventory ?: context.pimFixtures.candidatePool ?: []) as List<Map>
        Set<String> attempted=[] as Set
        if(accept)for(Map e in cached){attempted.add(e.employeeId.toString());if(accept.call(e))return cached}
        if(context.pimFixtures.inventory instanceof List)return cached
        String previousFingerprint=null,lastState='PIM_PAGINATION_UNSTABLE'
        int verificationCount=attempted.size()
        for(int attempt=1;attempt<=3;attempt++) {
            List<Map> employees=[];Set<String> seen=[] as Set;List<String> previousIds=[]
            Integer expectedTotal=null;boolean clean=true,terminal=false
            for (int pageNumber = 0; pageNumber < 100; pageNumber++) {
                int offset = pageNumber * 50
                Map page=read([model:'detailed',includeEmployees:'onlyCurrent',limit:50,offset:offset,sortField:'employee.firstName',sortOrder:'ASC'])
                if(!(page.data instanceof List)||!(page.total instanceof Number)||page.total<0||page.data.size()>50)
                    throw new Fault('PIM_API_RESPONSE_SCHEMA_INVALID','Invalid PIM pagination envelope')
                List<Map> parsed=page.data.collect {employee(it)}
                List<String> ids=parsed.collect {it.employeeId.toString()}
                List<String> within=ids.countBy {it}.findAll {id,n->n>1}.keySet().toList()
                List<String> overlap=ids.findAll {seen.contains(it)}.unique()
                int before=seen.size()
                if(expectedTotal==null)expectedTotal=page.total as int
                boolean totalChanged=expectedTotal!=(page.total as int)
                if(totalChanged||within||overlap)clean=false
                parsed.each {e->if(seen.add(e.employeeId.toString())&&e.active&&!VacancyTable.isExcludedManagerCandidate(e.displayName.toString()))employees<<e}
                context.pimFixtures.candidatePool=employees
                String reason=page.data.empty?'EMPTY_PAGE':offset+ids.size()>=page.total?'OFFSET_REACHED_TOTAL':'CONTINUE'
                diagnostic([kind:'PIM_PAGE',operationId:page.operationId,attempt:attempt,offset:offset,limit:50,
                    sortField:'employee.firstName',sortOrder:'ASC',httpStatus:page.status,total:page.total,rowCount:ids.size(),
                    uniqueEmployeeIdCount:seen.size(),firstId:ids?ids.first():null,lastId:ids?ids.last():null,
                    duplicateWithinPage:within,duplicateAcrossPages:overlap,duplicateWithPreviousPage:ids.intersect(previousIds),
                    repeatedPage:ids&&ids==previousIds,totalChanged:totalChanged,progress:seen.size()-before,
                    sortKeys:parsed.collect {it.firstName},sortObservation:'Diagnostic only: server collation and concurrent changes unproved',terminationReason:reason])
                // Identity must pass fresh numeric GET AND exact native Recruitment selection.
                if(accept)for(Map e in employees)if(attempted.add(e.employeeId.toString())) {
                    if(++verificationCount>100)throw new Fault('PIM_PAGINATION_UNSTABLE','Candidate verification request budget reached; completeness unproved; no write')
                    if(accept.call(e)){diagnostic([kind:'PIM_SCAN_END',terminationReason:'VERIFIED_CANDIDATE',complete:false]);persist();return employees}
                }
                if(!ids.empty&&seen.size()==before){lastState='PIM_PAGINATION_NO_PROGRESS';break}
                if(page.data.empty&&offset<page.total){lastState='PIM_PAGINATION_NO_PROGRESS';break}
                if(offset+ids.size()>=page.total) {
                    terminal=true
                    if(clean&&seen.size()==expectedTotal) {
                        String fingerprint=new Gson().toJson(parsedSnapshot(employees))
                        if(fingerprint==previousFingerprint) {
                            context.pimFixtures.inventory=employees;context.pimFixtures.inventoryComplete=true
                            diagnostic([kind:'PIM_SCAN_END',attempt:attempt,terminationReason:'TWO_MATCHING_COMPLETE_SCANS',complete:true]);persist();return employees
                        }
                        previousFingerprint=fingerprint
                    } else previousFingerprint=null
                    break
                }
                previousIds=ids
            }
            if(!terminal)previousFingerprint=null
            diagnostic([kind:'PIM_SCAN_RETRY',attempt:attempt,terminationReason:lastState,complete:false])
        }
        throw new Fault(lastState,'Bounded PIM scan could not prove completeness; no absence decision or provisioning')
    }
    static List<Map> parsedSnapshot(List<Map> employees) {
        employees.sort(false){it.employeeId.toString().toLong()}.collect {new LinkedHashMap(it)}
    }
    protected Map fresh(Map e,boolean owned=false) {
        Map response=read([model:'detailed',includeEmployees:'onlyCurrent',empNumber:e.employeeId,limit:50,offset:0])
        List<Map> matches=response.data.collect {employee(it)}.findAll {it.employeeId==e.employeeId?.toString()}
        if (matches.size()!=1 || !samePerson(e,matches[0])) {
            throw new Fault(owned ? 'EMPLOYEE_CREATED_BUT_NOT_SELECTABLE' : 'NO_ACTIVE_EMPLOYEE_AVAILABLE',
                'Cached employee ID '+e.employeeId+' is absent, terminated or changed; identity invalidated, no replacement Save')
        }
        matches[0]
    }
    protected Map verifyManager(Map e) {
        openManagerForm()
        try {
            String selected=selectManagerOption(fullName(e))
            Map actual=context.managerDiscovery.lastSelection
            if (actual?.employeeId?.toString()!=e.employeeId.toString()) throw new Fault('EMPLOYEE_IDENTITY_AMBIGUOUS','PIM and selected Hiring Manager IDs differ')
            return e+[displayName:VacancyChecks.normalizeManagerName(selected),selected:selected,
                searchQuery:actual.query,successfulSearchQuery:actual.query,state:'VERIFIED',status:'READY',
                reasonCode:'EMPLOYEE_VERIFIED',pimVerified:true,verificationStatus:'VERIFIED',
                canonicalState:'EMPLOYEE_VERIFIED',executionId:context.executionId,
                verifiedTimestamp:java.time.OffsetDateTime.now().toString(),
                verificationMethod:'PIM active numeric identity + native Hiring Manager option + selected value/ID',discoverySource:'PIM']
        } catch (IllegalStateException error) {
            if (error instanceof Fault || error instanceof EmployeeIdentity.NotFound) throw error
            if (error.message?.contains('CONFIGURATION PREFLIGHT / NOT_FOUND') || error.message?.contains('CONFIGURATION PREFLIGHT / INACTIVE'))
                throw new EmployeeIdentity.NotFound('PIM employee is not an active Hiring Manager suggestion')
            throw new Fault(error.message?.contains('AUTHENTICATION') ? 'SESSION_EXPIRED' : 'HIRING_MANAGER_AUTOCOMPLETE_FAILURE','Hiring Manager verification stopped',error)
        } catch (AssertionError error) {throw new Fault('EMPLOYEE_IDENTITY_AMBIGUOUS','Hiring Manager identity could not be proved',error)}
    }
    protected void openManagerForm(){runner.go('/web/index.php/recruitment/addJobVacancy')}
    protected String selectManagerOption(String name){new VacancyChecks(runner).selectApprovedManager(name)}
    Map manager(String setting='hiringManagerName',String previous=null,boolean preserveForm=false) {
        (Map)runner.measure('PIM_MANAGER_PRECONDITION') { managerInternal(setting,previous,preserveForm) }
    }
    protected Map managerInternal(String setting,String previous,boolean preserveForm) {
        assert setting in ['hiringManagerName','hiringManagerAlternativeName']
        ConfigurationPreflight.guard(runner.caseId)
        ensureSession()
        String key=setting=='hiringManagerName' ? 'managerA' : 'managerB'
        if(key=='managerB' && !context.managerDiscovery.managerA)manager('hiringManagerName',null,preserveForm)
        Map cached=context.managerDiscovery[key] as Map
        if (cached?.pimVerified && cached.state=='VERIFIED') {
            try {fresh(cached,cached.createdByExecution==true)}
            catch(Fault error){cached.state='INVALIDATED';context.pimFixtures.status=error.state;persist();throw error}
            return cached
        }
        return (Map) preservingForm(preserveForm) { resolveManager(setting,previous) }
    }
    protected Map resolveManager(String setting,String previous) {
        String slot=setting=='hiringManagerName' ? 'A' : 'B'
        String excludedId=slot=='B' ? context.managerDiscovery.managerA?.employeeId?.toString() : null
        String excludedName=VacancyChecks.normalizeManagerName(previous ?: '')
        String configured=(context.settings[setting] ?: '').toString().trim()
        List<Map> rejected=[]
        Map selected=null
        List<Map> candidates=inventory {Map e ->
            if (e.employeeId==excludedId || (excludedName && VacancyChecks.normalizeManagerName(e.displayName.toString())==excludedName)) return false
            if (configured && VacancyChecks.normalizeManagerName(e.displayName.toString())!=VacancyChecks.normalizeManagerName(configured)) return false
            try { selected=verifyManager(fresh(e));return true }
            catch(EmployeeIdentity.NotFound unavailable) {rejected << [employeeId:e.employeeId,reason:'NOT_SELECTABLE'];return false}
        }
        context.managerDiscovery.candidates=candidates.collect {it+[candidateName:it.displayName]}
        context.interviewerDiscovery.candidates=context.managerDiscovery.candidates
        if(selected)return cache(setting,selected)
        if(context.pimFixtures.inventoryComplete!=true)throw new Fault('PIM_PAGINATION_UNSTABLE','No verified candidate and incomplete inventory; no write')
        context.pimFixtures.diagnostics << [state:'NO_ACTIVE_EMPLOYEE_AVAILABLE',setting:setting,rejected:rejected,
            detail:'Complete current PIM inventory has no remaining suitable distinct selectable employee']
        persist()
        if (configured) throw new Fault('NO_ACTIVE_EMPLOYEE_AVAILABLE','Configured exact manager unavailable; provisioning never substitutes another person')
        if (!context.allowPimFixtureCreate) throw new Fault('EMPLOYEE_CREATION_DISABLED','No suitable PIM employee; explicit opt-in is false; no write')
        Map owned=provision(slot)
        context.pimFixtures.inventory=null;context.pimFixtures.inventoryComplete=false
        try {return cache(setting,verifyManager(fresh(owned,true))+[createdByExecution:true,ownershipMarker:owned.ownershipMarker])}
        catch(EmployeeIdentity.NotFound unavailable) {
            owned.state='EMPLOYEE_CREATED_BUT_NOT_SELECTABLE';persist()
            throw new Fault('EMPLOYEE_CREATED_BUT_NOT_SELECTABLE','Owned PIM employee '+owned.employeeId+' exists but Hiring Manager suggestion is unavailable; no extra employee created',unavailable)
        }
    }
    protected Map cache(String setting,Map verified) {
        String key=setting=='hiringManagerName' ? 'managerA' : 'managerB'
        if (key=='managerB' && verified.employeeId==context.managerDiscovery.managerA?.employeeId)
            throw new Fault('EMPLOYEE_IDENTITY_AMBIGUOUS','Manager B must have a different empNumber')
        context.managerDiscovery[key]=verified;context.managerDiscovery.status='VERIFIED'
        List pool=(context.managerDiscovery.verifiedPool ?: []) as List
        pool.removeAll {it.employeeId==verified.employeeId};pool << verified;context.managerDiscovery.verifiedPool=pool
        context.pimFixtures.status='EMPLOYEE_VERIFIED';persist();verified
    }
    Map interviewerSeed(String setting) {
        assert setting in ['interviewerA','interviewerB']
        ConfigurationPreflight.guard(runner.caseId)
        ensureSession()
        String other=setting=='interviewerB' ? context.interviewerDiscovery.interviewerA?.employeeId?.toString() : null
        List<Map> pool=[]
        [context.managerDiscovery.managerA,context.managerDiscovery.managerB].findAll {it?.pimVerified}.each {pool << it}
        pool.addAll((context.pimFixtures.inventory ?: context.pimFixtures.candidatePool ?: []) as List)
        context.pimFixtures.employees.values().findAll {it.createdByExecution&&positive(it.employeeId)}.each {pool << it}
        Map seed=pool.find {it.employeeId?.toString()!=other}
        if (!seed) seed=manager(setting=='interviewerA' ? 'hiringManagerName' : 'hiringManagerAlternativeName')
        fresh(seed,seed.createdByExecution==true)
        Map record=seed+[state:'READY',canonicalState:'DISCOVERY_PENDING',selected:fullName(seed),displayName:fullName(seed),
            verificationStatus:'SCHEDULE_INTERVIEW_AUTOCOMPLETE_REQUIRED',reasonCode:'PIM_EMPLOYEE_SEED_ONLY',
            discoverySource:'Live PIM; recruitment interviewer eligibility NOT YET VERIFIED']
        context.interviewerDiscovery[setting]=record
        context.interviewerDiscovery.candidates=pool.collect {it+[candidateName:fullName(it)]}
        persist();record
    }
    protected Object preservingForm(boolean preserve,Closure action) {
        String source=runner.driver().windowHandle,temporary=null
        boolean keep=preserve && (runner.driver().currentUrl==~ /https:\/\/opensource-demo\.orangehrmlive\.com\/web\/index\.php\/recruitment\/addJobVacancy(?:\/[1-9]\d*)?/)
        Throwable primary=null
        try {
            if (keep) {
                runner.driver().switchTo().newWindow(WindowType.TAB);temporary=runner.driver().windowHandle
                // newWindow starts at about:blank. Mount an authenticated document before
                // discovery; do not run API scripts in a blank tab or change the source draft.
                runner.go('/web/index.php/recruitment/addJobVacancy')
            }
            action.call()
        } catch(Throwable failure){primary=failure;throw failure}
        finally {
            if(temporary)try{runner.driver().switchTo().window(temporary);runner.driver().close();runner.driver().switchTo().window(source)}
            catch(Throwable cleanup){if(primary)primary.addSuppressed(cleanup);else throw cleanup}
        }
    }
    protected void await(String operation,Closure condition) {
        try {new WebDriverWait(runner.driver(),Duration.ofSeconds(runner.seconds)).pollingEvery(Duration.ofMillis(200))
            .ignoring(StaleElementReferenceException).until {if(runner.loginVisible())throw new Fault('SESSION_EXPIRED',operation);condition.call()}}
        catch(TimeoutException timeout){throw new Fault('UI_LOCATOR_FAILURE',operation+' timed out; never classify this as configuration skip',timeout)}
    }
    protected Map formState() {
        (Map)((JavascriptExecutor)runner.driver()).executeScript('''
            const visible=n=>!!n&&n.isConnected&&n.getClientRects().length>0;
            const first=document.querySelector('input[name="firstName"]'),form=first?.closest('form');
            const id=document.evaluate(arguments[0],document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;
            const check=form?Array.from(form.querySelectorAll('input[type="checkbox"]')):[];
            return {url:location.href,firstName:first?.value,middleName:form?.querySelector('input[name="middleName"]')?.value,
              lastName:form?.querySelector('input[name="lastName"]')?.value,employeeCode:id?.value,
              enabled:!!first&&!first.disabled,loginSwitches:check.length,createLogin:check.some(e=>e.checked),
              loading:Array.from(document.querySelectorAll('.oxd-form-loader,.oxd-loading-spinner')).some(visible),
              errors:form?Array.from(form.querySelectorAll('.oxd-input-field-error-message')).filter(visible).map(n=>n.textContent.trim()):[],
              save:form?Array.from(form.querySelectorAll('button')).filter(e=>visible(e)&&e.type==='submit'&&e.textContent.trim()==='Save').length:0};
        ''',VacancyChecks.group('Employee Id')+'//input'+' | '+VacancyChecks.group('Employee ID')+'//input')
    }
    protected void openCreate() {
        runner.go('/web/index.php/pim/viewEmployeeList')
        Map list=[:]
        await('PIM Employee list mount') {
            list=(Map)((JavascriptExecutor)runner.driver()).executeScript('''
                const root=document.querySelector('.orangehrm-employee-list'),vis=n=>!!n&&n.getClientRects().length>0;
                return {ready:!!root&&!Array.from(document.querySelectorAll('.oxd-loading-spinner')).some(vis),
                  add:Array.from(document.querySelectorAll('.orangehrm-header-container button')).filter(e=>vis(e)&&e.textContent.trim()==='Add'&&!e.disabled).length};
            ''');list.ready
        }
        if(list.add==0)throw new Fault('PIM_PERMISSION_DENIED','Mounted PIM list has no create-authorized Add action ($can.create employee_list); no write')
        if(list.add!=1)throw new Fault('UI_LOCATOR_FAILURE','Ambiguous PIM Add action; no write')
        nativeClick("//div[${VacancyChecks.token('orangehrm-header-container')}]//button[normalize-space()='Add']")
        await('PIM Add Employee form ready') {Map s=formState();RecruitmentUi.sameRoute(s.url.toString(),RecruitmentUi.BASE+'/web/index.php/pim/addEmployee')&&s.enabled&&!s.loading&&s.save==1}
    }
    protected void nativeClick(String xpath) {
        List<WebElement> controls=runner.driver().findElements(By.xpath(xpath)).findAll {it.displayed&&it.enabled}
        if(controls.size()!=1)throw new Fault('UI_LOCATOR_FAILURE','Missing/ambiguous native control; no fallback click')
        VacancyTable.click(runner.driver(),controls[0],runner.seconds)
    }
    protected void fill(Map e) {
        VacancyChecks fields=new VacancyChecks(runner)
        fields.replaceField('xpath=//input[@name="firstName"]',e.firstName.toString())
        fields.replaceField('xpath=//input[@name="middleName"]',e.middleName.toString())
        fields.replaceField('xpath=//input[@name="lastName"]',e.lastName.toString())
        double after=((Number)((JavascriptExecutor)runner.driver()).executeScript('return performance.now();')).doubleValue()
        String idPath='('+VacancyChecks.group('Employee Id')+'//input | '+VacancyChecks.group('Employee ID')+'//input)'
        fields.replaceField('xpath='+idPath,e.employeeCode.toString())
        runner.driver().findElement(By.xpath(idPath)).sendKeys(Keys.TAB)
        boolean previous=false
        await('Employee ID asynchronous unique validation') {
            boolean finished=((JavascriptExecutor)runner.driver()).executeScript('''
                return performance.getEntriesByType('resource').some(r=>{
                  if(r.startTime<arguments[1]||r.responseEnd<=0)return false;
                  const u=new URL(r.name,location.origin);
                  return u.origin===location.origin&&u.pathname.endsWith('/api/v2/core/validation/unique')&&
                    u.searchParams.get('entityName')==='Employee'&&u.searchParams.get('attributeName')==='employeeId'&&u.searchParams.get('value')===arguments[0];
                });
            ''',e.employeeCode,after)==true
            Map state=formState()
            if(state.errors)throw new Fault('EMPLOYEE_CREATION_FAILED','PIM input validation rejected: '+state.errors)
            boolean done=finished&&!state.loading&&previous;previous=finished&&!state.loading;done
        }
    }
    protected void submitOnce(Map e) {
        Map s=formState()
        if (!context.allowPimFixtureCreate)throw new Fault('EMPLOYEE_CREATION_DISABLED','Opt-in false at dispatch; no Save')
        if(runner.loginVisible())throw new Fault('SESSION_EXPIRED','PIM authentication lost before Save; no write')
        if(!RecruitmentUi.sameRoute(s.url?.toString(),RecruitmentUi.BASE+'/web/index.php/pim/addEmployee')||!s.enabled||s.loginSwitches!=1||s.createLogin||s.errors||s.loading||s.save!=1||!sameDraft(e,s))
            throw new Fault('UI_LOCATOR_FAILURE','PIM draft/login-off/readiness contract failed; no Save')
        if(e.saveAttempted)throw new Fault('EMPLOYEE_CREATION_FAILED','Employee Save already attempted; reconcile only')
        e.saveAttempted=true;e.state='SUBMITTED';e.attemptedAt=java.time.OffsetDateTime.now().toString();persist()
        nativeClick("//input[@name='firstName']/ancestor::form[1]//button[@type='submit' and normalize-space()='Save']")
        e.clickReturned=true;persist()
    }
    static boolean sameDraft(Map e,Map s){['firstName','middleName','lastName','employeeCode'].every {e[it]==s[it]}}
    static String routeId(String url) {
        def match=(url ?: '') =~ /^https:\/\/opensource-demo\.orangehrmlive\.com\/web\/index\.php\/pim\/viewPersonalDetails\/empNumber\/([1-9]\d*)$/
        match.matches()?match.group(1):null
    }
    protected void outcome(Map e) {
        await('PIM Save numeric redirect or explicit rejection') {
            Map s=formState()
            if(s.errors)throw new Fault('EMPLOYEE_CREATION_FAILED','PIM Save rejected: '+s.errors)
            String id=routeId(s.url?.toString())
            if(id){e.employeeId=id;e.state='IDENTIFIED';persist();return true}
            false
        }
    }
    protected Map reconcile(Map e) {
        Map response=read([model:'detailed',includeEmployees:'onlyCurrent',employeeId:e.employeeCode,limit:50,offset:0])
        List<Map> matches=response.data.collect {employee(it)}
        if(response.total!=1||matches.size()!=1||!samePerson(e,matches[0]))
            throw new Fault('EMPLOYEE_CREATION_FAILED','Uncertain Employee Save not reconciled by exact employee code/full names/ID; no replay')
        e.employeeId=matches[0].employeeId
        runner.go('/web/index.php/pim/viewPersonalDetails/empNumber/'+e.employeeId)
        await('Fresh PIM Personal Details fields') {Map state=formState();!state.loading&&sameDraft(e,state)&&routeId(state.url?.toString())==e.employeeId}
        e.createdByExecution=true;e.createdAt=e.createdAt ?: java.time.OffsetDateTime.now().toString()
        e.state='PIM_VERIFIED';e.active=true;e.cleanupStatus='NEEDS_OPERATOR_CONFIRMATION';persist();e
    }
    protected Map provision(String slot) {
        (Map)runner.measure('PIM_EMPLOYEE_CREATION') { provisionInternal(slot) }
    }
    protected Map provisionInternal(String slot) {
        assert slot in ['A','B'] : 'Only two PIM employee slots per execution'
        if(!context.allowPimFixtureCreate)throw new Fault('EMPLOYEE_CREATION_DISABLED','Provisioning is OFF; no form/write')
        Map e=context.pimFixtures.employees[slot] as Map
        if(e?.saveAttempted)return reconcile(e) // Includes uncertain dispatches: never create another employee for this slot.
        if(!e){e=draft(context.executionId,slot,runner.caseId,context.caseRunId);context.pimFixtures.employees[slot]=e;persist()}
        Map collision=read([model:'detailed',includeEmployees:'onlyCurrent',employeeId:e.employeeCode,limit:50,offset:0])
        if(collision.total!=0||collision.data)throw new Fault('EMPLOYEE_IDENTITY_AMBIGUOUS','Employee code already exists before Save; do not adopt or overwrite')
        openCreate();fill(e)
        try {
            EvidenceCapture.capture('PIM_BEFORE_SAVE')
            runner.measure('PIM_SAVE') {submitOnce(e);outcome(e)}
            Map result=(Map)runner.measure('PIM_PERSISTENCE_VERIFICATION') {reconcile(e)}
            EvidenceCapture.capture('PIM_AFTER_FRESH_READ');return result
        } catch(Throwable failure) {
            if(e.saveAttempted){e.state='UNCERTAIN';e.lastFailure=failure instanceof Fault ? failure.state : failure.class.simpleName;persist()
                try {return reconcile(e)}catch(Throwable followup){failure.addSuppressed(followup)}}
            throw failure
        }
    }
    static void dependency(String kind,String id,String managerId,String vacancyId=null) {
        ExecutionContext c=ExecutionContext.peek();if(!c||!positive(id)||!(kind in ['vacancy','candidate']))return
        Set<String> managers=[] as Set
        if(positive(managerId))managers << managerId
        if(kind=='candidate'&&vacancyId)c.pimFixtures.dependencies.findAll {it.kind=='vacancy'&&it.id==vacancyId}.each {if(it.managerId)managers << it.managerId}
        managers.each {emp ->
            Map link=[kind:kind,id:id,managerId:emp,vacancyId:vacancyId,caseId:c.caseId,caseRunId:c.caseRunId,state:'RETAINED_FOR_EVIDENCE']
            if(!c.pimFixtures.dependencies.any {it.kind==kind&&it.id==id&&it.managerId==emp})c.pimFixtures.dependencies << link
            c.pimFixtures.employees.values().findAll {it.createdByExecution&&it.employeeId==emp}.each {record ->
                String field=kind=='vacancy'?'vacancyIds':'candidateIds';if(!record[field].contains(id))record[field] << id
                record.cleanupStatus='BLOCKED_BY_RETAINED_DEPENDENCIES'
            }
        }
        AtomicFiles.json(new File(c.root,'pim-fixtures.json'),[schema:1,executionId:c.executionId,allowCreate:c.allowPimFixtureCreate,fixtures:c.pimFixtures]);c.persist()
    }
    static List<Map> cleanupPlan(ExecutionContext c) {
        c.pimFixtures.employees.values().findAll {e ->
            Map expected=draft(c.executionId,e.slot?.toString() ?: 'A')
            e.executionId==c.executionId&&e.ownershipMarker==expected.ownershipMarker&&e.employeeCode==expected.employeeCode&&
                e.createdByExecution==true&&e.saveAttempted==true&&positive(e.employeeId)&&sameDraft(expected,e)
        }.collect {e -> [employeeId:e.employeeId,fullName:fullName(e),executionId:c.executionId,
            vacancyIds:e.vacancyIds,candidateIds:e.candidateIds,status:e.vacancyIds||e.candidateIds?'BLOCKED_BY_RETAINED_DEPENDENCIES':'NEEDS_OPERATOR_CONFIRMATION',
            deletionImplemented:false]}
    }
}
