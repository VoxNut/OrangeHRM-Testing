package recruitment

import java.time.OffsetDateTime
import java.time.Duration

/** Supplementary report: statuses come exclusively from Katalon's AfterTestCase callback. */
class ExecutionReport {
    static String escape(Object value) { (value?.toString() ?: '').replace('&','&amp;').replace('<','&lt;').replace('>','&gt;').replace('"','&quot;').replace("'",'&apos;') }
    static String clean(Object input) {
        String value=input?.toString() ?: ''
        value=value.replaceAll(/(?i)(["']?(?:password|passwd|token|api[_-]?key|secret|authorization|cookie|sessionid)["']?\s*[:=]\s*)(["'])(.*?)\2/,'$1$2[REDACTED]$2')
        value=value.replaceAll(/(?i)(password|passwd|token|api[_-]?key|secret|authorization|cookie|sessionid)(\s*[=:]\s*)([^\s,;]+)/,'$1$2[REDACTED]')
        value=value.replaceAll(/(?i)Bearer\s+[^\s]+/,'Bearer [REDACTED]')
        value=value.replaceAll(/([?&](?:[^=\s]+)=)[^&\s]+/,'$1[REDACTED]')
        value
    }
    static String runKind(String input=System.getenv('ASM8_RUN_KIND')) {
        input in ['FIRST_RUN','RETEST'] ? input : 'UNCLASSIFIED'
    }
    static void start(List<String> expected) {
        ExecutionContext c=ExecutionContext.current()
        c.report=[kind:'KATALON_CALLBACK_DERIVED',module:c.module,suiteName:c.suiteId ?: 'STANDALONE',
            executionId:c.executionId,runKind:runKind(),startedAt:c.startedAt,endedAt:null,durationSeconds:null,
            katalonVersion:null,environment:[os:System.getProperty('os.name'),java:System.getProperty('java.version')],
            integrationState:'ASM8_INTEGRATION_DEFERRED',expectedCaseIds:expected,cases:[],
            limitations:['Supplementary listener report; retain the official Katalon report and console separately.',
                'Katalon version is unknown unless recorded by the official report.'],events:[]]
    }
    static void beginCase(String id) {
        ExecutionContext c=ExecutionContext.current()
        if (!c.report) start([id])
        c.report.caseStartedAt=OffsetDateTime.now().toString()
        event('CASE_STARTED '+id)
    }
    static void event(String message) {
        ExecutionContext c=ExecutionContext.peek()
        c?.report?.events?.add(OffsetDateTime.now().toString()+' '+clean(message))
    }
    static void finishCase(String id,String status,String message='') {
        ExecutionContext c=ExecutionContext.current()
        String ended=OffsetDateTime.now().toString()
        String actual=status in ['PASSED','FAILED','ERROR','SKIPPED','INCOMPLETE'] ? status : 'UNKNOWN'
        Map decision=c.preflight?.decisions?.get(id)
        c.report.cases << [caseId:id,caseRunId:c.caseRunId,result:actual,rawKatalonResult:status,
            startedAt:c.report.caseStartedAt,endedAt:ended,
            durationSeconds:c.report.caseStartedAt ? Duration.between(OffsetDateTime.parse(c.report.caseStartedAt.toString()),OffsetDateTime.parse(ended)).toMillis()/1000.0 : null,
            message:clean(message),stackTrace:clean(message),
            configurationState:decision?.state ?: 'UNKNOWN',
            executionState:actual=='SKIPPED' && decision && decision.state!='READY' ? 'CONFIGURATION_BLOCKED' : actual,
            evidence:c.evidenceFolder ? [c.evidenceDirectory().toPath().relativize(new File(c.evidenceFolder,'evidence.json').toPath()).toString().replace('\\','/')] : [],
            source:'Katalon TestCaseContext.getTestCaseStatus() after execution']
        event('CASE_FINISHED '+id+' '+actual)
    }
    static Map summary(List<Map> cases) {
        ['PASSED','FAILED','ERROR','SKIPPED','INCOMPLETE','UNKNOWN','NOT_EXECUTED'].collectEntries { state -> [(state):cases.count { it.result==state }] }
    }
    static void finish(String lifecycle) {
        ExecutionContext c=ExecutionContext.peek()
        if (!c?.report) return
        Map report=c.report
        report.endedAt=OffsetDateTime.now().toString()
        report.durationSeconds=Duration.between(OffsetDateTime.parse(c.startedAt),OffsetDateTime.parse(report.endedAt)).toMillis()/1000.0
        report.lifecycle=lifecycle
        report.expectedCaseIds.each { id ->
            if (!report.cases.any { it.caseId==id }) report.cases << [caseId:id,result:'NOT_EXECUTED',source:'No AfterTestCase callback',startedAt:null,endedAt:null,durationSeconds:null,evidence:[]]
        }
        report.summary=summary(report.cases)
        report.module=c.module
        File folder=new File(c.evidenceDirectory(),'reports/'+report.runKind)
        List events=report.remove('events') as List
        AtomicFiles.json(new File(folder,'results.json'),report,false)
        String html='<!doctype html><html><head><meta charset="utf-8"><title>Recruitment execution '+escape(c.executionId)+'</title></head><body><h1>Recruitment supplementary execution report</h1><p>'+escape(report.kind)+'; '+escape(report.integrationState)+'</p><p>'+escape(report.startedAt)+' — '+escape(report.endedAt)+'; '+escape(report.runKind)+'</p><p>'+escape(report.summary)+'</p><p>'+escape(report.limitations.join(' '))+'</p><table border="1"><tr><th>Test Case</th><th>Katalon result</th><th>Configuration</th><th>Message / stack</th><th>Evidence</th></tr>'
        report.cases.each { row ->
            html+='<tr><td>'+escape(row.caseId)+'</td><td>'+escape(row.result)+'</td><td>'+escape(row.configurationState ?: 'UNKNOWN')+'</td><td><pre>'+escape(row.message)+'</pre></td><td>'
            (row.evidence ?: []).each { path -> html+='<a href="../../'+escape(path)+'">manifest</a> ' }
            html+='</td></tr>'
        }
        html+='</table></body></html>'
        AtomicFiles.bytes(new File(folder,'results.html'),html.getBytes('UTF-8'),false)
        // Unknown/not-executed results cannot be faithfully represented by ordinary JUnit outcome fields.
        if (!report.cases.any { it.result in ['UNKNOWN','INCOMPLETE','NOT_EXECUTED'] }) {
            String xml='<?xml version="1.0" encoding="UTF-8"?><testsuite name="'+escape(report.suiteName)+'" tests="'+report.cases.size()+'" failures="'+report.summary.FAILED+'" errors="'+report.summary.ERROR+'" skipped="'+report.summary.SKIPPED+'" time="'+report.durationSeconds+'" timestamp="'+escape(report.startedAt)+'">'
            report.cases.each { row ->
                xml+='<testcase name="'+escape(row.caseId)+'" classname="'+escape(report.module)+'" time="'+row.durationSeconds+'">'
                if (row.result in ['FAILED','ERROR']) {
                    String tag=row.result=='FAILED' ? 'failure' : 'error'
                    xml+='<'+tag+' message="'+escape(row.message ?: row.result)+'">'+escape(row.stackTrace)+'</'+tag+'>'
                }
                if (row.result=='SKIPPED') xml+='<skipped message="'+escape(row.executionState ?: 'KATALON_RESULT_SKIPPED')+'"/>'
                xml+='</testcase>'
            }
            xml+='</testsuite>'
            AtomicFiles.bytes(new File(folder,'results.xml'),xml.getBytes('UTF-8'),false)
        }
        AtomicFiles.bytes(new File(folder,'execution.log'),('LISTENER LIFECYCLE LOG (not the full Katalon console)\n'+events.join('\n')+'\n').getBytes('UTF-8'),false)
        AtomicFiles.bytes(new File(folder,'README.md'),('Supplementary Katalon callback report. Keep the official Katalon console/execution report alongside it.\nASM8_INTEGRATION_DEFERRED; BROWSER NOT VERIFIED by source maintenance.\nRun classification: '+report.runKind+'. Set ASM8_RUN_KIND=FIRST_RUN or RETEST before starting Katalon.\nUnknown/incomplete/no callback results suppress JUnit output. Hard process termination may prevent finalization; retain context.json and case manifests as PARTIAL SOURCE.\n').getBytes('UTF-8'),false)
    }
    static void observe(Closure action) {
        try { action.call() } catch(Throwable error) { println('[SUPPLEMENTARY REPORT UNAVAILABLE] '+error.class.simpleName) }
    }
}
