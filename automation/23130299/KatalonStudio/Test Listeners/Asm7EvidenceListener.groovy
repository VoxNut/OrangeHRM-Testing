import com.kms.katalon.core.annotation.*
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext
import recruitment.*

class Asm7EvidenceListener {
    @BeforeTestSuite
    def beforeSuite(TestSuiteContext context) {
        // No cross-execution admission or lock in a test listener.
        ExecutionBrowser.closeOwned()
        ExecutionContext.start(context.getTestSuiteId(),true)
        SessionGuard.startSuite()
        List<String> cases=ConfigurationPreflight.suiteCases(context.getTestSuiteId(),ConfigurationPreflight.project())
        ExecutionReport.observe { ExecutionReport.start(cases) }
        ConfigurationPreflight.initialize(ConfigurationPreflight.project(),cases)
    }
    @BeforeTestCase
    def beforeCase(TestCaseContext context) {
        if (!ExecutionContext.peek()) ExecutionContext.start('',false)
        SessionGuard.startCase()
        EvidenceCapture.startCase(context.getTestCaseId())
        ExecutionReport.observe { ExecutionReport.beginCase(context.getTestCaseId().tokenize('/').last()) }
        String caseId=context.getTestCaseId().tokenize('/').last()
        Map admission=SessionRecovery.eligible(caseId) ? new SessionRecovery().admit(caseId,ConfigurationPreflight.project()) :
            ConfigurationPreflight.admit(caseId,ConfigurationPreflight.project())
        if (admission.state!='READY') {
            if (ConfigurationPreflight.maySkip(admission)) {
                context.skipThisTestCase() // Explicit missing configuration/test data; never a locator or framework error.
            } else {
                // Event listener exceptions do not guarantee TC suppression. The shared
                // RecorderBridge/RecruitmentUi entry guard throws the canonical cause once
                // inside the TC, before browser navigation/fixtures/business assertions.
                println('[PREFLIGHT ADMISSION DENIED / '+admission.state+'] deferred to TC admission guard')
            }
        }
    }
    @AfterTestCase
    def afterCase(TestCaseContext context) {
        ExecutionContext c=ExecutionContext.peek()
        if (!c || c.caseId!=context.getTestCaseId().tokenize('/').last()) return
        ExecutionReport.observe {
            ExecutionReport.finishCase(c.caseId,context.getTestCaseStatus(),context.getMessage())
        }
        EvidenceCapture.finish(context.getTestCaseStatus())
        if (!c.suiteExecution) finishExecution()
    }
    @AfterTestSuite
    def afterSuite(TestSuiteContext context) {
        ExecutionContext c=ExecutionContext.peek()
        if (c && c.suiteId==context.getTestSuiteId()) finishExecution()
    }
    protected void finishExecution() {
        try {
            ExecutionBrowser.closeOwned()
            ExecutionContext c=ExecutionContext.peek()
            if(c && c.pimFixtures.employees) AtomicFiles.json(new File(c.root,'pim-cleanup-plan.json'),
                [executionId:c.executionId,employees:PimEmployeeFixtures.cleanupPlan(c),deletionImplemented:false,
                 policy:'Operator confirmation required; keep dependencies/evidence first; never delete borrowed employees'])
            boolean blocked=c?.preflight?.decisions?.values()?.any { it.state!='READY' }
            String state=blocked ? 'CONFIGURATION_BLOCKED' : 'FINISHED'
            ExecutionReport.observe { ExecutionReport.finish(state) }
            c?.end(state)
        }
        catch(Throwable unavailable) { println('[EXECUTION CLEANUP UNAVAILABLE] '+unavailable.class.simpleName) }
        finally { ExecutionContext.detach() }
    }
}
