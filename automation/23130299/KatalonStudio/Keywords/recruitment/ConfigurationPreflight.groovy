package recruitment

import com.google.gson.Gson
import com.kms.katalon.core.configuration.RunConfiguration
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException

/** Configuration admission and read-only employee auto-discovery. Business assertions, fixture ownership and Save paths are unchanged. */
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
            policy:'Empty employee settings use read-only AUTO_DISCOVER from Vacancies table + Add Vacancy/Schedule Interview autocomplete verification; missing cvLimitBytes is officially SKIPPED before testcase script/setup.']))
    }
    static Map admit(String caseId,File root,Closure verifier=null) {
        ExecutionContext c=ExecutionContext.current()
        if (!c.preflight.initialized) initialize(root,[caseId])
        Map result=c.preflight.entries[caseId]
        if (!result) {
            result=configuration(caseId,c.settings,requirements(root));c.preflight.entries[caseId]=result
        }
        if(c.preflight.snapshot.state!='READY') result=result+[state:'STALE_SNAPSHOT',canonicalState:'STALE_SNAPSHOT',problems:[c.preflight.snapshot]]
        if(result.state in ['READY','DISCOVERY_PENDING']) {
            boolean allVerified = true
            for(String name in (result.requires as List).findAll { it!='cvLimitBytes' }) {
                Map identity=c.preflight.identities[name]
                if (!identity) {
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
                result = result + [state:'READY', canonicalState:'VERIFIED', verificationState:'VERIFIED', problems:[]]
            }
        }
        c.preflight.decisions[caseId]=result
        if(result.state!='READY') {
            println('[CONFIGURATION BLOCKED -> OFFICIAL SKIP] '+caseId+'; '+new Gson().toJson(result.problems))
            c.evidence.preflight=result
        }
        persist();result
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
        ExecutionContext c = ExecutionContext.current()
        String expected=(c.settings[setting] ?: '').toString().trim()
        RecorderBridge runner=new RecorderBridge(caseId)
        VacancyChecks checks=new VacancyChecks(runner)
        if (!expected) {
            try {
                if (setting in ['hiringManagerName', 'hiringManagerAlternativeName']) {
                    Map discovered = checks.discoverAndVerifyManager(setting)
                    if (discovered.state in ['VERIFIED', 'READY'] && discovered.displayName) {
                        return [state:'READY', verificationStatus:'VERIFIED', canonicalState:'VERIFIED', mode:'AUTO_DISCOVER',
                            expected:'', selected:discovered.displayName] + discovered
                    }
                    return [state:discovered.state ?: 'NO_VERIFIED_HIRING_MANAGER', canonicalState:'NO_VERIFIED_HIRING_MANAGER',
                        mode:'AUTO_DISCOVER', expected:'', submitted:false] + discovered
                }
                // For interviewerA / interviewerB when blank: discover candidate employees from Vacancies table;
                // actual Interviewer eligibility is verified on Schedule Interview's InterviewerAutocomplete (/api/v2/recruitment/interviewers).
                List<Map<String, Object>> candidates = (c.managerDiscovery?.candidates as List<Map<String, Object>>) ?: []
                if (candidates.isEmpty()) candidates = checks.discoverManagerCandidatesFromVacancyTable()
                c.interviewerDiscovery.candidates = candidates
                String other = setting == 'interviewerB' ? resolvedIdentity('interviewerA', c) : ''
                Map chosen = candidates.find { Map cand ->
                    String n = VacancyChecks.normalizeManagerName(cand.candidateName?.toString())
                    !VacancyTable.isExcludedManagerCandidate(n) && (!other || n != VacancyChecks.normalizeManagerName(other))
                }
                if (!chosen) {
                    return [state:'NO_VERIFIED_HIRING_MANAGER', canonicalState:'NO_VERIFIED_HIRING_MANAGER',
                        mode:'AUTO_DISCOVER', setting:setting, submitted:false]
                }
                String candName = VacancyChecks.normalizeManagerName(chosen.candidateName.toString())
                Map record = [state:'READY', verificationStatus:'SCHEDULE_INTERVIEW_AUTOCOMPLETE_REQUIRED', canonicalState:'DISCOVERY_PENDING',
                    mode:'AUTO_DISCOVER', displayName:candName, selected:candName,
                    discoverySource:'Recruitment > Vacancies table (requires Schedule Interview InterviewerAutocomplete verification)',
                    executionId:c.executionId, submitted:false]
                if (setting == 'interviewerA') c.interviewerDiscovery.interviewerA = record
                else c.interviewerDiscovery.interviewerB = record
                c.persist()
                return record
            } catch(IllegalStateException error) {
                if (!(error.message?.contains('CONFIGURATION') || error.message?.contains('AUTHENTICATION'))) throw error
                def state=error.message =~ /\/\s*([A-Z_]+)\]/
                String value=state.find() ? state.group(1) : 'AUTHENTICATION'
                return [state:normalizeState(value),canonicalState:normalizeCanonicalState(value),mode:'AUTO_DISCOVER',expected:'',errorType:error.class.name,submitted:false]
            } catch(TimeoutException error) {
                return [state:'NO_VERIFIED_HIRING_MANAGER',canonicalState:'NO_VERIFIED_HIRING_MANAGER',phase:'AUTO_DISCOVER_PREFLIGHT',mode:'AUTO_DISCOVER',expected:'',errorType:error.class.name,submitted:false]
            }
        }
        try {
            runner.go('/web/index.php/recruitment/addJobVacancy')
            checks.waitFor().until { !runner.driver().findElements(By.xpath("//h6[normalize-space()='Add Vacancy']")).empty }
            String selected=checks.selectApprovedManager(expected) // generic EmployeeAutocomplete; no Save
            return [state:'READY',verificationStatus:'VERIFIED',canonicalState:'VERIFIED',mode:'MANUAL_OVERRIDE',
                expected:expected,selected:selected,displayName:selected,
                validation:'Exact unique active EmployeeAutocomplete selection; actual interviewer form still verifies eligibility']
        } catch(IllegalStateException error) {
            if (!(error.message?.contains('CONFIGURATION') || error.message?.contains('AUTHENTICATION'))) throw error
            def state=error.message =~ /\/\s*([A-Z_]+)\]/
            String value=state.find() ? state.group(1) : 'AUTHENTICATION'
            return [state:normalizeState(value),canonicalState:normalizeCanonicalState(value),mode:'MANUAL_OVERRIDE',expected:expected,errorType:error.class.name,submitted:false]
        } catch(TimeoutException error) {
            return [state:'LOCATOR_MISMATCH',canonicalState:'LOCATOR_MISMATCH',phase:'ADD_VACANCY_PREFLIGHT_FORM',mode:'MANUAL_OVERRIDE',expected:expected,errorType:error.class.name,submitted:false]
        }
    }
    static String normalizeState(String state) {
        [AMBIGUOUS:'AMBIGUOUS_SUGGESTION',INACTIVE:'NOT_ACTIVE',CONFIG_MISSING:'MISSING_CONFIGURATION',AUTH_LOST:'AUTHENTICATION'].get(state,state)
    }
    static String normalizeCanonicalState(String state) {
        [MISSING_CONFIGURATION:'CONFIG_MISSING',AMBIGUOUS_SUGGESTION:'AMBIGUOUS',AUTHENTICATION:'AUTH_LOST',READY:'VERIFIED'].get(state,state)
    }
    static void requireConfigured(String name,String caseId) {
        ExecutionContext c=ExecutionContext.current()
        String value=resolvedIdentity(name, c)
        if (value) {
            if (value.length()>80) throw new IllegalStateException('[CONFIGURATION BLOCKED / INVALID_CONFIGURATION] '+caseId+' invalid '+name)
            return
        }
        String decisionState=c.preflight?.decisions?.get(caseId)?.state?.toString()
        String identityState=c.preflight?.identities?.get(name)?.state?.toString()
        String discoveryStatus=c.managerDiscovery?.status?.toString()
        if ((decisionState && decisionState != 'READY') ||
            (identityState && !(identityState in ['READY', 'VERIFIED', 'DISCOVERY_PENDING'])) ||
            discoveryStatus == 'NO_VERIFIED_HIRING_MANAGER' ||
            !(name in ['hiringManagerName', 'hiringManagerAlternativeName'])) {
            throw new IllegalStateException('[CONFIGURATION BLOCKED / NO_VERIFIED_HIRING_MANAGER] '+caseId+
                ' requires '+name+'. Neither manual override in Profiles/default nor verified auto-discovery succeeded; preflight listener prevents repeated fixture setup. RECRUIT_VAC_Create_05 intentionally omits Manager; RECRUIT_CAND_Create_05 still requires its Vacancy fixture.')
        }
    }
    static void persist() {
        ExecutionContext c=ExecutionContext.current()
        AtomicFiles.json(new File(c.root,'configuration-preflight.json'),c.preflight)
    }
}
