package recruitment

import com.google.gson.Gson
import com.kms.katalon.core.configuration.RunConfiguration
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException

/** Configuration admission with PIM discovery and explicit opt-in employee preparation.
 * Recruitment eligibility still requires its own native autocomplete verification.
 */
class ConfigurationPreflight {
    static final List<String> EMPLOYEE_SETTINGS = ['hiringManagerName','hiringManagerAlternativeName','interviewerA','interviewerB'].asImmutable()

    static File project() { new File(RunConfiguration.getProjectDir()) }
    static Map requirements(File root) {
        File file=new File(root,'maintenance/preflight58-plan.json')
        if (!file.isFile()) throw new IllegalStateException('[PREFLIGHT RESOURCE MISSING] Prepare a new complete worker: '+file.name)
        new Gson().fromJson(file.getText('UTF-8'),Map)
    }
    static List<String> suiteCases(String id,File root) {
        String relative=(id.startsWith('Test Suites/') ? id : 'Test Suites/'+id)+'.ts'
        File suite=new File(root,relative).canonicalFile
        assert suite.toPath().startsWith(new File(root,'Test Suites').canonicalFile.toPath()) && suite.isFile() : 'Invalid suite source'
        def links=WorkerSnapshot.xml(suite).getElementsByTagName('testCaseLink')
        List<String> ids=[]
        for(int i=0;i<links.length;i++) {
            def link=links.item(i)
            if(link.getElementsByTagName('isRun').item(0).textContent=='true') ids<<link.getElementsByTagName('testCaseId').item(0).textContent.tokenize('/').last()
        }
        ids
    }
    static String resolvedIdentity(String name, ExecutionContext c = ExecutionContext.peek()) {
        if (!c) return ''
        String raw = (c.settings?.get(name) ?: '').toString().trim()
        if (raw) return raw
        if (name in ['interviewerA','interviewerB'] && c.interviewerDiscovery[name]?.state == 'VERIFIED') {
            return c.interviewerDiscovery[name].displayName.toString().trim()
        }
        Map idEntry = (Map) c.preflight?.identities?.get(name)
        if (idEntry && idEntry.state in ['READY', 'VERIFIED'] && idEntry.selected?.toString()?.trim()) {
            return idEntry.selected.toString().trim()
        }
        if (name == 'hiringManagerName' && c.managerDiscovery?.managerA?.state in ['VERIFIED', 'READY']) {
            return (c.managerDiscovery.managerA.displayName ?: '').toString().trim()
        }
        if (name == 'hiringManagerAlternativeName' && c.managerDiscovery?.managerB?.state in ['VERIFIED', 'READY']) {
            return (c.managerDiscovery.managerB.displayName ?: '').toString().trim()
        }
        if (name == 'interviewerA' && c.interviewerDiscovery?.interviewerA?.displayName) {
            return c.interviewerDiscovery.interviewerA.displayName.toString().trim()
        }
        if (name == 'interviewerB' && c.interviewerDiscovery?.interviewerB?.displayName) {
            return c.interviewerDiscovery.interviewerB.displayName.toString().trim()
        }
        return ''
    }
    static Map configuration(String caseId,Map settings,Map plans) {
        Map plan=plans[caseId]
        assert plan!=null : 'Unmapped fully qualified case ID '+caseId
        List<Map> problems=[]
        List<String> discoveryPending=[]
        boolean disableAutoDiscover = settings?._disableAutoDiscover == true
        (plan.requires as List).each { String name ->
            Object value=settings[name]
            boolean missing=name=='cvLimitBytes' ? !(value instanceof Number) || value.longValue()<=0L : !value?.toString()?.trim()
            if (missing) {
                if (name == 'cvLimitBytes' || disableAutoDiscover) {
                    problems << [setting:name,state:'MISSING_CONFIGURATION',canonicalState:'CONFIG_MISSING']
                } else {
                    discoveryPending << name
                }
            }
            else if (name!='cvLimitBytes' && value.toString().trim().length()>=100) problems << [setting:name,state:'INVALID_CONFIGURATION',canonicalState:'INVALID_CONFIGURATION',reason:'EmployeeAutocomplete source requires query length below100']
            else if (name=='cvLimitBytes' && value.longValue()>50L*1024L*1024L) problems << [setting:name,state:'INVALID_CONFIGURATION',canonicalState:'INVALID_CONFIGURATION']
            else if (name in ['hiringManagerAlternativeName','interviewerB']) {
                String other=name=='interviewerB' ? 'interviewerA' : 'hiringManagerName'
                if (value?.toString()?.trim() && settings[other]?.toString()?.trim() &&
                    VacancyChecks.normalizeManagerName(value.toString())==VacancyChecks.normalizeManagerName(settings[other]?.toString()))
                    problems << [setting:name,state:'AMBIGUOUS_SUGGESTION',canonicalState:'AMBIGUOUS',reason:'A and B must identify distinct employees']
            }
        }
        String state = problems ? problems[0].state : (discoveryPending ? 'DISCOVERY_PENDING' : 'READY')
        String canonicalState = problems ? (problems[0].canonicalState ?: problems[0].state) : (discoveryPending ? 'DISCOVERY_PENDING' : 'READY')
        [caseId:caseId,requires:plan.requires,state:state,canonicalState:canonicalState,
            mode:discoveryPending ? 'AUTO_DISCOVER' : 'MANUAL_OVERRIDE',
            discoveryPending:discoveryPending,problems:problems,
            validation:'CONFIGURATION_AND_DISCOVERY; employee suggestions require read-only UI autocomplete verification']
    }
    static void initialize(File root,List<String> ids) {
        ExecutionContext c=ExecutionContext.current()
        Map plans=requirements(root)
        Map entries=ids.collectEntries { id -> [(id):configuration(id,c.settings,plans)] }
        c.preflight=[initialized:true,project:root.canonicalPath,entries:entries,identities:[:],snapshot:WorkerSnapshot.check(root),decisions:[:]]
        if(c.preflight.snapshot.state=='READY') c.preflight.snapshot=WorkerSnapshot.runtimeSettings(root,c.settings,RunConfiguration.getExecutionProfile() ?: 'default')
        persist()
        Map missing=[:]
        Map autoDiscover=[:]
        entries.each { id,row ->
            row.problems.take(1).each { p -> missing[p.setting]=(missing[p.setting] ?: [])+[id] }
            (row.discoveryPending ?: []).each { s -> autoDiscover[s]=(autoDiscover[s] ?: [])+[id] }
        }
        println('[EXECUTION CONFIGURATION PREFLIGHT] '+new Gson().toJson([executionId:c.executionId,snapshot:c.preflight.snapshot,
            missing:missing,autoDiscover:autoDiscover,
            configurationReadyCases:entries.findAll { id,row->row.state in ['READY','DISCOVERY_PENDING'] }.keySet(),
            policy:'PIM active employee discovery; native Add Employee only with explicit ASM7_ALLOW_PIM_FIXTURE_CREATE=true, maximum two owned slots; actual Recruitment selection remains required. No historical Vacancy repair.']))
    }
    static Map admit(String caseId,File root,Closure verifier=null) {
        ExecutionContext c=ExecutionContext.current()
        if(c.preflight.decisions?.containsKey(caseId) && c.preflight.decisions[caseId].caseRunId==c.caseRunId)
            return c.preflight.decisions[caseId]
        c.admissionInProgress=true
        try {
            if(verifier==null) {
                new CandidateNavigationRecovery(c).beforeAdmission(caseId)
                new WorkflowRouteRecovery().beforeAdmission(caseId)
            }
            return admitInternal(caseId,root,verifier)
        }
        catch(RuntimeException error) {
            // Even an unexpected framework/transport exception must become a canonical
            // admission denial before Katalon is allowed to invoke the case body.
            String state=error instanceof PimEmployeeFixtures.Fault ? error.state : 'PREFLIGHT_FRAMEWORK_ERROR'
            RuntimeException canonical=error instanceof PimEmployeeFixtures.Fault ? error :
                new PimEmployeeFixtures.Fault(state,'Unexpected preflight exception; no fixture',error)
            Map stopped=[caseId:caseId,caseRunId:c.caseRunId,state:state,canonicalState:state,
                problems:[[errorType:error.class.name,detail:ExecutionReport.clean(error.message)]]]
            c.preflightErrors[caseId]=canonical
            if(!c.preflight.decisions)c.preflight.decisions=[:]
            c.preflight.decisions[caseId]=stopped
            c.technicalPreflightFailure=stopped
            c.evidence.preflight=stopped
            persist();return stopped
        }
        finally {c.admissionInProgress=false}
    }
    private static Map admitInternal(String caseId,File root,Closure verifier) {
        ExecutionContext c=ExecutionContext.current()
        if (!c.preflight.initialized) initialize(root,[caseId])
        Map result=c.preflight.entries[caseId]
        if (!result) {
            result=configuration(caseId,c.settings,requirements(root));c.preflight.entries[caseId]=result
        }
        if(c.preflight.snapshot.state!='READY') result=result+[state:'STALE_SNAPSHOT',canonicalState:'STALE_SNAPSHOT',problems:[c.preflight.snapshot]]
        boolean needsEmployee=(result.requires as List).any {it!='cvLimitBytes'}
        if(result.state in ['READY','DISCOVERY_PENDING'] && needsEmployee && c.technicalPreflightFailure) {
            result=result+c.technicalPreflightFailure+[caseId:caseId,reusedExecutionFailure:true]
        }
        if(result.state in ['READY','DISCOVERY_PENDING']) {
            boolean allVerified = true
            for(String name in (result.requires as List).findAll { it!='cvLimitBytes' }) {
                Map identity=c.preflight.identities[name]
                if (!identity || (verifier==null && identity.pimVerified==true && name in ['hiringManagerName','hiringManagerAlternativeName'])) {
                    identity=(Map)EvidenceCapture.fixture { verifier ? verifier.call(name,caseId) : probeIdentity(name,caseId) }
                    c.preflight.identities[name]=identity
                    syncDiscoveredIdentity(c, name, identity)
                    println('[EMPLOYEE PREFLIGHT] '+new Gson().toJson([setting:name,caseId:caseId]+identity))
                }
                if(!(identity.state in ['READY','VERIFIED'])) {
                    allVerified = false
                    result=result+[state:identity.state,canonicalState:normalizeCanonicalState(identity.state),problems:[[setting:name]+identity]]
                    break
                }
            }
            if (allVerified) {
                boolean pending=(result.requires as List).any { c.preflight.identities[it]?.verificationStatus=='SCHEDULE_INTERVIEW_AUTOCOMPLETE_REQUIRED' }
                result = result + [state:'READY', canonicalState:pending ? 'DISCOVERY_PENDING' : 'VERIFIED', verificationState:pending ? 'RECRUITMENT_INTERVIEWER_VERIFICATION_PENDING' : 'VERIFIED', problems:[]]
            }
        }
        result=result+[caseId:caseId,caseRunId:c.caseRunId]
        c.preflight.decisions[caseId]=result
        if(result.state!='READY' && !maySkip(result) && needsEmployee && !c.technicalPreflightFailure)
            c.technicalPreflightFailure=result
        if(result.reusedExecutionFailure && c.technicalPreflightFailure?.caseId)
            c.preflightErrors[caseId]=c.preflightErrors[c.technicalPreflightFailure.caseId]
        if(result.state!='READY' && !c.preflightErrors[caseId]) {
            String detail=(result.problems ?: result).toString()
            c.preflightErrors[caseId]=new PimEmployeeFixtures.Fault(result.state,'Preflight admission: '+detail)
        }
        if(result.state!='READY') {
            println('[PREFLIGHT STOPPED / '+(maySkip(result) ? 'SKIPPED_CONFIGURATION' : 'ERROR')+'] '+caseId+'; '+new Gson().toJson(result.problems))
            c.evidence.preflight=result
        }
        persist();result
    }
    static boolean maySkip(Map admission) {
        admission.state in ['MISSING_CONFIGURATION','INVALID_CONFIGURATION','NO_VERIFIED_HIRING_MANAGER'] || PimEmployeeFixtures.SKIPPABLE.contains(admission.state)
    }
    static void syncDiscoveredIdentity(ExecutionContext c, String setting, Map identity) {
        if (!c || !identity || !(identity.state in ['READY','VERIFIED'])) return
        String selected = (identity.selected ?: identity.displayName ?: '').toString().trim()
        if (!selected) return
        Map record = [
            state: 'VERIFIED',
            status: 'READY',
            displayName: selected,
            selected: selected,
            employeeId: identity.employeeId ?: null,
            verificationMethod: identity.verificationMethod ?: identity.validation ?: 'Add Vacancy EmployeeAutocomplete exact active suggestion',
            discoverySource: identity.discoverySource ?: (c.settings[setting]?.toString()?.trim() ? 'Profiles/default manual override' : 'Recruitment > Vacancies table'),
            verifiedTimestamp: identity.verifiedTimestamp ?: java.time.OffsetDateTime.now().toString(),
            executionId: c.executionId,
            submitted: false
        ] + identity
        record.state = identity.verificationStatus == 'SCHEDULE_INTERVIEW_AUTOCOMPLETE_REQUIRED' ? 'READY' : 'VERIFIED'
        if (setting == 'hiringManagerName') {
            c.managerDiscovery.managerA = record
            c.managerDiscovery.status = 'VERIFIED'
        } else if (setting == 'hiringManagerAlternativeName') {
            c.managerDiscovery.managerB = record
            c.managerDiscovery.status = 'VERIFIED'
        } else if (setting == 'interviewerA') {
            c.interviewerDiscovery.interviewerA = record
        } else if (setting == 'interviewerB') {
            c.interviewerDiscovery.interviewerB = record
        }
        c.persist()
    }
    static Map probeIdentity(String setting,String caseId) {
        ExecutionContext c=ExecutionContext.current()
        String expected=(c.settings[setting] ?: '').toString().trim()
        if (expected && setting in ['interviewerA','interviewerB']) {
            return [state:'READY',verificationStatus:'SCHEDULE_INTERVIEW_AUTOCOMPLETE_REQUIRED',canonicalState:'DISCOVERY_PENDING',
                mode:'MANUAL_OVERRIDE',expected:expected,selected:expected,displayName:expected,
                verificationMethod:'Pending actual Recruitment InterviewerAutocomplete; PIM alone is not eligibility proof',submitted:false]
        }
        PimEmployeeFixtures fixtures=new PimEmployeeFixtures(new RecorderBridge(caseId))
        try {
            if(setting in ['hiringManagerName','hiringManagerAlternativeName']) {
                Map verified=fixtures.manager(setting)
                return verified+[mode:expected ? 'MANUAL_OVERRIDE' : 'PIM_AUTO_DISCOVER',submitted:false]
            }
            return fixtures.interviewerSeed(setting)+[mode:'PIM_DISCOVERY_PENDING',submitted:false]
        } catch(PimEmployeeFixtures.Fault stopped) {
            c.preflightErrors[caseId]=stopped
            return [state:stopped.state,canonicalState:stopped.state,setting:setting,
                mode:'PIM_PRECONDITION',provisioningAllowed:c.allowPimFixtureCreate,detail:stopped.message,
                submitted:false,errorType:stopped.class.name,rootExceptionType:(stopped.cause ?: stopped).class.name,rootExceptionMessage:ExecutionReport.clean((stopped.cause ?: stopped).message)]
        } catch(TimeoutException timeout) {
            return [state:'UI_LOCATOR_FAILURE',canonicalState:'UI_LOCATOR_FAILURE',detail:'PIM/Recruitment readiness timed out; no configuration skip',errorType:timeout.class.name]
        } catch(IllegalStateException error) {
            if(!error.message?.contains('AUTHENTICATION'))throw error
            return [state:'SESSION_EXPIRED',canonicalState:'SESSION_EXPIRED',detail:'Execution authentication lost; no skip',errorType:error.class.name]
        }
    }
    static String normalizeState(String state) {
        [AMBIGUOUS:'AMBIGUOUS_SUGGESTION',INACTIVE:'NOT_ACTIVE',CONFIG_MISSING:'MISSING_CONFIGURATION',AUTH_LOST:'AUTHENTICATION'].get(state,state)
    }
    static String normalizeCanonicalState(String state) {
        [MISSING_CONFIGURATION:'CONFIG_MISSING',AMBIGUOUS_SUGGESTION:'AMBIGUOUS',AUTHENTICATION:'AUTH_LOST',READY:'VERIFIED'].get(state,state)
    }
    /** Admission is enforced at the shared entry paths because Katalon listener exceptions
     * are logged by its event executor and do not reliably prevent the TC body invocation.
     */
    static void guard(String caseId) {
        ExecutionContext c=ExecutionContext.peek()
        if(!c || c.admissionInProgress)return
        Map decision=c.preflight?.decisions?.get(caseId)
        if(!decision || decision.state=='READY')return
        RuntimeException original=c.preflightErrors[caseId]
        if(!original) {
            original=new PimEmployeeFixtures.Fault(decision.state,'Canonical preflight: '+decision.problems)
            c.preflightErrors[caseId]=original
        }
        throw original
    }
    static void requireConfigured(String name,String caseId) {
        guard(caseId)
        ExecutionContext c=ExecutionContext.current()
        String value=resolvedIdentity(name,c)
        if(value) {
            if(value.length()>=100)throw new IllegalStateException('[CONFIGURATION BLOCKED / INVALID_CONFIGURATION] '+caseId+' invalid '+name)
            return
        }
        Map identity=c.preflight?.identities?.get(name)
        if(identity && !(identity.state in ['READY','VERIFIED','DISCOVERY_PENDING']))
            throw new PimEmployeeFixtures.Fault(identity.state,'Identity preflight: '+identity.detail)
        // No admission yet: the PIM orchestrator owns lazy discovery at the manager helper.
        if(!(name in ['hiringManagerName','hiringManagerAlternativeName']))
            throw new IllegalStateException('[CONFIGURATION BLOCKED / MISSING_CONFIGURATION] '+caseId+' requires '+name)
    }
    static void persist() {
        ExecutionContext c=ExecutionContext.current()
        AtomicFiles.json(new File(c.root,'configuration-preflight.json'),c.preflight)
    }
}
