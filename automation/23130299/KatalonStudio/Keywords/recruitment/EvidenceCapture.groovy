package recruitment

import org.openqa.selenium.*
import java.security.MessageDigest

/** Observational only. Every mutable field lives in ExecutionContext. */
class EvidenceCapture {
    static final File ROOT=ExecutionContext.EVIDENCE_ROOT
    static String getSuiteId() { ExecutionContext.peek()?.suiteId ?: '' }
    static String getRunId() { ExecutionContext.peek()?.executionId ?: '' }
    static Map getCurrent() { ExecutionContext.peek()?.evidence ?: [:] }
    static void setCurrent(Map value) { ExecutionContext.current().evidence=value }
    static File getFolder() { ExecutionContext.peek()?.evidenceFolder }
    static void setFolder(File value) { ExecutionContext.current().evidenceFolder=value }
    static Object fixture(Closure action) {
        ExecutionContext c=ExecutionContext.current(); c.fixtureDepth++
        try { return action.call() } finally { c.fixtureDepth-- }
    }
    static String stamp() { java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss_SSS'))+'_'+UUID.randomUUID().toString() }
    static void startSuite(String id) {
        if (!ExecutionContext.peek()) ExecutionContext.start(id,true)
    }
    static void endSuite() {
        ExecutionContext c=ExecutionContext.peek()
        if (c) { c.evidence=[:];c.evidenceFolder=null }
    }
    static void startCase(String id) {
        ExecutionContext c=ExecutionContext.current();c.beginCase(id)
        c.evidenceFolder=new File(c.evidenceRoot,c.module+'_'+c.executionId+'/screenshots/'+c.caseId+'/'+c.caseRunId)
        c.evidence=[suiteId:c.suiteId ?: 'STANDALONE',caseId:c.caseId,runId:c.executionId,executionId:c.executionId,
            caseRunId:c.caseRunId,processId:c.processId,workerId:c.workerId,timestamp:java.time.OffsetDateTime.now().toString(),
            finalResult:null,checkpoints:[]]
    }
    static boolean safePage(String url,boolean passwordPresent) {
        try { URI u=new URI(url);return u.scheme=='https' && !u.userInfo && u.port == -1 && u.host=='opensource-demo.orangehrmlive.com' && u.path.startsWith('/web/index.php/recruitment/') && !passwordPresent }
        catch(Exception ignored) { return false }
    }
    static boolean allowCheckpoint(List rows,String kind,boolean terminal) {
        if (rows.size()>=5) return false
        if (terminal) return true
        if (rows.count { it.kind!='terminal' }>=3) return false
        return rows.count { it.kind==kind } < (kind=='before' ? 2 : 1)
    }
    static String checkpointLabel(String phase, boolean finalStatus) {
        String label=phase.replaceAll(/[^A-Za-z0-9_-]/,'_')
        // Legacy helpers may request *_FAILED before Katalon has classified ERROR versus FAILED.
        finalStatus ? label : label.replaceAll(/(?:^|_)(?:FAILED|ERROR)(?=_|$)/,'_STOPPED')
    }
    static String expectedFor(String id) {
        try {
            File snapshot=new File(com.kms.katalon.core.configuration.RunConfiguration.getProjectDir(),'maintenance/workbook75-current.json')
            Map requirements=new com.google.gson.Gson().fromJson(snapshot.getText('UTF-8'),Map)
            return requirements[id]?.expected ?: 'Workbook '+id
        } catch(Exception ignored) { return 'Workbook '+id }
    }
    static void capture(String phase,boolean terminal=false) {
        ExecutionContext c=ExecutionContext.peek()
        if (!c?.evidence || !c.evidenceFolder) return
        if (!terminal && (c.fixtureDepth>0 || phase.contains('FIXTURE'))) return
        List rows=c.evidence.checkpoints as List
        String kind=terminal ? 'terminal' : phase.contains('BEFORE') ? 'before' : phase.contains('AFTER') || phase.contains('VALIDATION') ? 'after' : 'observation'
        if (!allowCheckpoint(rows,kind,terminal)) return
        long started=System.nanoTime()
        try {
            if (!c.ownedDriver) { println('[EVIDENCE UNAVAILABLE] No browser owned by this execution');return }
            WebDriver d=ExecutionBrowser.driver()
            if (!d.windowHandle || !(d instanceof TakesScreenshot)) { println('[EVIDENCE UNAVAILABLE] Screenshot capability unavailable');return }
            String url=d.currentUrl
            if (!safePage(url,!d.findElements(By.cssSelector('input[type=password]')).empty)) return
            byte[] bytes=((TakesScreenshot)d).getScreenshotAs(OutputType.BYTES)
            // Guard a navigation to Login occurring while the screenshot command was in flight.
            if (!safePage(d.currentUrl,!d.findElements(By.cssSelector('input[type=password]')).empty)) return
            String label=checkpointLabel(phase,terminal && c.evidence.finalResult in ['FAILED','ERROR'])
            File target=new File(c.evidenceFolder,stamp()+'_'+label+'.png')
            AtomicFiles.bytes(target,bytes,false)
            String browser=d instanceof HasCapabilities ? ((HasCapabilities)d).capabilities.browserName : d.class.simpleName
            URI u=new URI(url)
            rows << [timestamp:java.time.OffsetDateTime.now().toString(),browser:browser,url:u.scheme+'://'+u.host+u.path,
                checkpoint:phase,kind:kind,expected:expectedFor(c.caseId),observed:[phase:phase,url:u.path,assertions:'Final result supplied by Katalon AfterTestCase'],elapsedMs:(System.nanoTime()-started).intdiv(1000000L),
                screenshotPath:target.absolutePath,sha256:MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString()]
        } catch(Throwable unavailable) { println('[EVIDENCE UNAVAILABLE] '+unavailable.class.simpleName) }
    }
    static void failure(Throwable error,String phase) {
        ExecutionContext c=ExecutionContext.peek()
        if (!c?.evidence) return
        c.evidence.exception=[type:error.class.name,phase:phase]
        capture('STOPPED',true)
    }
    static void finish(String result) {
        ExecutionContext c=ExecutionContext.peek()
        try {
            if (!c?.evidence || !c.evidenceFolder) return
            c.evidence.finalResult=result
            if (!(result=='SKIPPED' && c.evidence.preflight?.state!='READY'))
                capture(result in ['FAILED','ERROR'] ? result : 'RESULT',true)
            AtomicFiles.json(new File(c.evidenceFolder,'evidence.json'),c.evidence,false)
        } catch(Throwable unavailable) { println('[EVIDENCE MANIFEST UNAVAILABLE] '+unavailable.class.simpleName) }
        finally { if(c) { c.evidence=[:];c.evidenceFolder=null } }
    }
}
