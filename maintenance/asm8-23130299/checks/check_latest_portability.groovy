import recruitment.*
import java.nio.file.*

int portabilityGroups=0
def verify={String name,Closure task->task();println('PORTABILITY PASS '+(++portabilityGroups)+': '+name)}
Path sandbox=new File(args[0],'latest-portability').toPath();Files.createDirectories(sandbox)
Path moved=sandbox.resolve('classroom export/unrelated-student-path/KatalonStudio');Files.createDirectories(moved)
verify('relocated project uses project evidence directory, not shell cwd'){
    assert PortablePaths.evidence(moved,[:]).toPath()==moved.resolve('evidence/executions/23130299')
}
verify('relative runtime output resolves against supplied project'){
    assert PortablePaths.resolve('runtime/ASM7','unused',moved).toPath()==moved.resolve('runtime/ASM7')
}
verify('absolute dedicated evidence override is retained'){
    Path dedicated=sandbox.resolve('dedicated-output')
    assert PortablePaths.evidence(moved,[ASM7_EVIDENCE_ROOT:dedicated.toString()]).toPath()==dedicated
}
verify('foreign member namespace is rejected'){
    boolean stopped=false
    try{PortablePaths.evidence(moved,[ASM7_EVIDENCE_ROOT:sandbox.resolve('evidence/executions/99999999').toString()])}
    catch(IllegalArgumentException expected){stopped=true};assert stopped
}
verify('relative override is rejected'){
    boolean stopped=false;try{PortablePaths.evidence(moved,[ASM7_EVIDENCE_ROOT:'../evidence'])}
    catch(IllegalArgumentException expected){stopped=true};assert stopped
}
verify('project root cannot become shared evidence sink'){
    boolean stopped=false;try{PortablePaths.evidence(moved,[ASM7_EVIDENCE_ROOT:moved.toString()])}
    catch(IllegalArgumentException expected){stopped=true};assert stopped
}
verify('another drive path can be resolved without touching that drive'){
    Path other=Paths.get('Q:/classroom/recruitment/KatalonStudio')
    assert PortablePaths.evidence(other,[:]).toPath()==other.resolve('evidence/executions/23130299')
}
verify('first run and retest classifications never inferred from execution order'){
    assert ExecutionReport.runKind('FIRST_RUN')=='FIRST_RUN'
    assert ExecutionReport.runKind('RETEST')=='RETEST'
    assert ExecutionReport.runKind('')=='UNCLASSIFIED'
}
verify('new contexts isolate runtime UUID and evidence namespaces'){
    def first=ExecutionContext.start('RECRUIT_FLOW',true,sandbox.resolve('runtime').toFile(),sandbox.resolve('evidence').toFile())
    def second=ExecutionContext.start('RECRUIT_FLOW',true,sandbox.resolve('runtime').toFile(),sandbox.resolve('evidence').toFile())
    assert first.executionId!=second.executionId && first.root!=second.root && first.evidenceDirectory()!=second.evidenceDirectory()
    assert first.ownedDriver==null && second.ownedDriver==null
    ExecutionContext.detach()
}
verify('atomic screenshot-like file output refuses overwrite'){
    File artifact=sandbox.resolve('synthetic-atomic-check.txt').toFile()
    AtomicFiles.bytes(artifact,'offline'.bytes,false)
    boolean stopped=false;try{AtomicFiles.bytes(artifact,'replacement'.bytes,false)}catch(Exception expected){stopped=true}
    assert stopped && artifact.text=='offline'
}
println('LATEST PORTABILITY OFFLINE PASS '+portabilityGroups+'; temporary files only; no browser/network/real screenshot')
