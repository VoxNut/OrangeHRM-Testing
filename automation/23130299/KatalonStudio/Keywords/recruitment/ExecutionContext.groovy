package recruitment

import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable
import org.openqa.selenium.WebDriver
import java.security.MessageDigest

/** One suite execution, sequential cases. Independent Katalon JVMs have independent state. */
class ExecutionContext {
    private static final ThreadLocal<ExecutionContext> binding=new ThreadLocal<ExecutionContext>()
    static final File EVIDENCE_ROOT=new File('C:/Users/Admin/IdeaProjects/OrangeHRM-Testing/evidence/executions/23130299')
    final String executionId=UUID.randomUUID().toString()
    final String startedAt=java.time.OffsetDateTime.now().toString()
    final long processId=ProcessHandle.current().pid()
    final String workerId=Thread.currentThread().name+'-'+Thread.currentThread().id
    final File root,evidenceRoot
    final boolean suiteExecution
    String suiteId,module,caseId='',caseRunId='',lifecycle='STARTED',browserSessionId=''
    transient WebDriver ownedDriver
    boolean browserConfigured=false,menuVerified=false
    Map authentication=[authenticated:false,exhausted:false,reason:'']
    Map preflight=[:]
    Map managerDiscovery=[candidates:[],verifiedPool:[],managerA:null,managerB:null,status:'DISCOVERY_PENDING']
    Map interviewerDiscovery=[candidates:[],verifiedPool:[],interviewerA:null,interviewerB:null,status:'DISCOVERY_PENDING']
    Map evidence=[:]
    File evidenceFolder
    int fixtureDepth=0,sequence=0
    final Map settings

    private ExecutionContext(String suite,boolean suiteRun,File runtime,File screenshots) {
        suiteId=suite;suiteExecution=suiteRun;module=moduleOf(suite)
        root=new File(runtime,'executions/'+executionId);evidenceRoot=screenshots
        settings=['executionMode','maxConcurrentSessions','jobTitle','hiringManagerName','hiringManagerAlternativeName','interviewerA','interviewerB','cvLimitBytes','timeFormat','phoneValidationConfirmed']
            .collectEntries { name -> [(name):profile(name,null)] }.asImmutable()
        if (!root.mkdirs()) throw new IllegalStateException('Execution directory already exists or unavailable: '+root)
        ['fixtures','logs','temp','downloads','chrome-profile'].each { new File(root,it).mkdirs() }
        persist()
    }
    static Object profile(String name,Object fallback) {
        try { Object v=GlobalVariable."${name}";v==null ? fallback : v }
        catch(MissingPropertyException ignored) { fallback }
    }
    static String moduleOf(String id) {
        def match=(id ?: '') =~ /RECRUIT_(VAC|CAND|FLOW)/
        match.find() ? 'RECRUIT_'+match.group(1) : 'RECRUIT_UNKNOWN'
    }
    static ExecutionContext peek() { binding.get() }
    static ExecutionContext start(String suite='',boolean suiteRun=false,File runtime=null,File screenshots=EVIDENCE_ROOT) {
        ExecutionContext previous=binding.get()
        if (previous) previous.end('SUPERSEDED') // metadata only, never another context's driver
        File base=runtime
        if (!base) {
            String configured=profile('asm7OutputDir','').toString().trim()
            base=configured ? new File(configured) : new File(RunConfiguration.getProjectDir(),'runtime/ASM7')
        }
        ExecutionContext created=new ExecutionContext(suite,suiteRun,base,screenshots)
        binding.set(created);created
    }
    static ExecutionContext current() { binding.get() ?: start() }
    static void detach() { binding.remove() }
    void beginCase(String id) {
        caseId=id.tokenize('/').last();module=moduleOf(caseId);caseRunId=UUID.randomUUID().toString()
        evidence=[:];evidenceFolder=null;fixtureDepth=0;lifecycle='RUNNING';persist()
    }
    String tokenFor(String id) {
        // 80 random bits, compact enough for Last<=30 and Vacancy Name<=50.
        MessageDigest.getInstance('SHA-256').digest((executionId+':'+id+':'+UUID.randomUUID()).getBytes('UTF-8')).encodeHex().toString().take(20)
    }
    File caseDirectory(String id,String token) {
        File path=new File(root,'fixtures/'+id+'/'+token)
        assert path.mkdirs() : 'Case output already exists or unavailable: '+path
        path
    }
    Map owner(String id) { [ownerExecutionId:executionId,caseId:id,processId:processId,workerId:workerId,runNamespace:'G01_23130299_'+executionId] }
    void persist() {
        AtomicFiles.json(new File(root,'context.json'),[executionId:executionId,suiteId:suiteId,module:module,startedAt:startedAt,
            executionMode:settings.executionMode ?: 'ISOLATED_CONCURRENT',maxConcurrentSessions:settings.maxConcurrentSessions ?: 2,
            processId:processId,workerId:workerId,caseId:caseId,caseRunId:caseRunId,browserSessionId:browserSessionId,
            runNamespace:'G01_23130299_'+executionId,fixtureLedgerPath:new File(root,'fixtures').absolutePath,
            evidencePath:new File(evidenceRoot,module+'_'+executionId).absolutePath,downloadPath:new File(root,'downloads').absolutePath,
            tempPath:new File(root,'temp').absolutePath,authentication:new LinkedHashMap(authentication),
            managerDiscovery:new LinkedHashMap(managerDiscovery),interviewerDiscovery:new LinkedHashMap(interviewerDiscovery),
            screenshotQuota:[normalTotal:3,beforeMaximum:2,terminalReserved:2,totalMaximum:5],lifecycle:lifecycle])
    }
    void end(String state='FINISHED') { lifecycle=state;persist() } // no other run's artifacts are removed
}
