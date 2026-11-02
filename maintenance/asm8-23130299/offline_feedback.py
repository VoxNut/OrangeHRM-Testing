"""Compile Recruitment with real Studio jars and run unchanged portability harness.

Usage: python offline_feedback.py --katalon-home <KS-home> --reference <Downloads-project>
All mutable work is under a fresh OS temporary directory. No Katalon CLI, test case,
browser, login or network is executed. Logs are new files under team-feedback/offline.
"""
from pathlib import Path
import argparse, hashlib, json, re, shutil, subprocess, tempfile
import xml.etree.ElementTree as ET

REPO=Path(__file__).resolve().parents[2]
PROJECT=REPO/'automation/23130299/KatalonStudio'
OUT=Path(__file__).resolve().parent/'team-feedback/offline'

def main(home,reference,output=None):
    global OUT
    if output: OUT=output
    assert not OUT.exists(), 'Retain previous verification; use a fresh output directory'
    OUT.mkdir(parents=True)
    groovy=next((home/'plugins').glob('org.codehaus.groovy_3.0.25.*/lib'))
    java=home/'jre/bin/java.exe'
    jars=[groovy/'groovy-3.0.25-indy.jar']+[p for d in [home/'plugins',home/'configuration/resources/lib',groovy]
        for p in d.glob('*.jar') if not re.search(r'-sources|-javadoc|groovy-3\.0\.17',p.name)]
    cp=';'.join(map(str,jars)); results=[]
    def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
    protected={p.relative_to(PROJECT).as_posix():digest(p) for folder in ['Keywords','Scripts','Test Cases','Test Suites','Test Listeners','Profiles','settings','maintenance'] for p in (PROJECT/folder).rglob('*') if p.is_file()}
    with tempfile.TemporaryDirectory(prefix='recruit-feedback-') as temporary:
        temp=Path(temporary);src=temp/'src';src.mkdir()
        profile=ET.parse(PROJECT/'Profiles/default.glbl')
        gv=src/'GlobalVariable.groovy'
        gv.write_text('package internal\nclass GlobalVariable {\n'+'\n'.join('static Object '+x.findtext('name')+' = '+x.findtext('initValue') for x in profile.findall('GlobalVariableEntity'))+'\n}',encoding='utf8')
        units=list((PROJECT/'Keywords').rglob('*.groovy'))+[gv]
        for p in sorted((PROJECT/'Scripts').glob('*/*/*.groovy')):
            target=src/(p.parent.name+'.groovy');target.write_bytes(p.read_bytes());units.append(target)
        units+=list((PROJECT/'Test Suites').glob('*.groovy'))+list((PROJECT/'Test Listeners').glob('*.groovy'))
        def run(arguments,name,cwd=temp):
            argfile=temp/'java.args'
            argfile.write_text('\n'.join('"'+str(a).replace('\\','/').replace('"','\\"')+'"' for a in arguments),encoding='utf8')
            r=subprocess.run([str(java),'@'+str(argfile)],cwd=cwd,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=180)
            captured=(r.stdout+'\n'+r.stderr).rstrip('\r\n')
            (OUT/name).write_text(captured+('\n' if captured else ''),encoding='utf8',newline='\n')
            return r
        for parser in ['antlr2','parrot']:
            classes=temp/parser;classes.mkdir()
            r=run(['-Dfile.encoding=UTF-8','-Dgroovy.antlr4='+str(parser=='parrot').lower(),'-cp',cp,
                'org.codehaus.groovy.tools.FileSystemCompiler','--encoding','UTF-8','-d',classes,*units],parser+'-production.log')
            results.append(dict(check=parser+'-production',status='PASS' if r.returncode==0 else 'FAIL',units=len(units)))
            print(results[-1],flush=True)
            if r.returncode: raise RuntimeError(r.stderr[-4000:])
        check_root=Path(__file__).resolve().parent/'checks'
        original=check_root/'check_latest_portability.groovy'
        if reference:
            assert digest(original)==digest(reference/'maintenance/asm8-full-audit-20261102'/original.name)
        wrapper=temp/'RunCheck.groovy'
        wrapper.write_text("""recruitment.ExecutionBrowser.metaClass.static.launch={Closure ignored -> throw new IllegalStateException('[OFFLINE_BROWSER_GUARD] Browser forbidden')}
com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords.metaClass.static.openBrowser={String ignored -> throw new IllegalStateException('[OFFLINE_BROWSER_GUARD] Browser forbidden')}
recruitment.ExecutionContext.start('OFFLINE',true,new File(args[0],'guard-runtime'),new File(args[0],'guard-evidence'))
try { evaluate(new File(args[1])) } finally { recruitment.ExecutionLease.release() }
""",encoding='utf8')
        r=run(['-Dfile.encoding=UTF-8','-Dgroovy.antlr4=true','-cp',str(classes)+';'+cp,'groovy.ui.GroovyMain',wrapper,temp,original],'portability-original.log')
        passed=len(re.findall(r'PORTABILITY PASS \d+:',r.stdout))
        results.append(dict(check='unchanged-10-group-portability',status='PASS' if r.returncode==0 and passed==10 else 'FAIL',
            groupsPassed=passed,exitCode=r.returncode,harnessSha256=digest(original),temporaryDirectoryFresh=True))
        print(results[-1],flush=True)
        if r.returncode:print((r.stdout+r.stderr)[-3500:],flush=True)
        # The FLOW harness invokes portability itself. Give it a separate fresh sandbox;
        # running portability twice in the same sandbox correctly refuses the old file.
        flow_temp=temp/'flow-canary';flow_temp.mkdir()
        flow_checks=flow_temp/'maintenance/asm8-full-audit-20261102';flow_checks.mkdir(parents=True)
        for name in ['check_latest_portability.groovy','check_latest_flow.groovy']:
            shutil.copyfile(check_root/name,flow_checks/name)
            if reference: assert digest(check_root/name)==digest(reference/'maintenance/asm8-full-audit-20261102'/name)
        r=run(['-Dfile.encoding=UTF-8','-Dgroovy.antlr4=true','-cp',str(classes)+';'+cp,'groovy.ui.GroovyMain',wrapper,flow_temp,flow_checks/'check_latest_flow.groovy'],'flow-canary-original.log',cwd=flow_temp)
        results.append(dict(check='unchanged-flow-canary-plus-nested-portability',status='PASS' if r.returncode==0 else 'FAIL',
            groupsPassed=len(re.findall(r'LATEST FLOW PASS \d+:',r.stdout)),exitCode=r.returncode,
            harnessSha256=digest(check_root/'check_latest_flow.groovy')))
        print(results[-1],flush=True)
        # Additional real member-repo placement, listener lifecycle, UUID and evidence mapping.
        extra=temp/'MemberLayout.groovy'
        extra.write_text("""import recruitment.*
import java.nio.file.*
Path root=Paths.get(args[0]).resolve('clone with spaces');Files.createDirectories(root)
Files.writeString(root.resolve('.git'),'gitdir: synthetic offline member layout')
Path project=root.resolve('automation/23130299/KatalonStudio');Files.createDirectories(project)
Path sink=root.resolve('evidence/executions/23130299')
assert PortablePaths.evidence(project,[:]).toPath()==sink
def first=ExecutionContext.start('RECRUIT_FLOW',true,root.resolve('runtime').toFile(),sink.toFile())
EvidenceCapture.startCase('RECRUIT_FLOW_OFFLINE_01')
assert first.evidenceFolder.toPath().startsWith(sink)
assert first.evidence.executionId==first.executionId && first.evidence.caseRunId==first.caseRunId
File manifest=new File(first.evidenceFolder,'evidence.json')
EvidenceCapture.finish('SKIPPED')
assert manifest.isFile()
String before=manifest.text
def second=ExecutionContext.start('RECRUIT_FLOW',true,root.resolve('runtime').toFile(),sink.toFile())
EvidenceCapture.startCase('RECRUIT_FLOW_OFFLINE_01')
assert first.executionId!=second.executionId && first.caseRunId!=second.caseRunId
assert second.evidenceFolder!=manifest.parentFile
EvidenceCapture.finish('SKIPPED');ExecutionLease.release()
assert manifest.text==before && ExecutionContext.peek()==null
assert first.ownedDriver==null && second.ownedDriver==null
println('MEMBER LAYOUT / LISTENER HELPERS / UUID / MANIFEST PRESERVATION PASS; no browser')
""",encoding='utf8')
        r=run(['-Dfile.encoding=UTF-8','-cp',str(classes)+';'+cp,'groovy.ui.GroovyMain',extra,temp],'member-layout.log')
        results.append(dict(check='member-layout-evidence-lifecycle',status='PASS' if r.returncode==0 else 'FAIL',exitCode=r.returncode))
        print(results[-1],flush=True)
        # Launcher preflight only, in a copied project; no --execute, no engine invocation.
        isolated=temp/'standalone-project';isolated.mkdir()
        for relative in protected:
            target=isolated/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(PROJECT/relative,target)
        shutil.copyfile(PROJECT/'OrangeHRM_RECRUIT_VAC_KatalonStudio.prj',isolated/'OrangeHRM_RECRUIT_VAC_KatalonStudio.prj')
        import sys
        r=subprocess.run([sys.executable,'-X','utf8',str(isolated/'maintenance/run_isolated.py'),'--preflight-only','--suites','VAC','CAND','FLOW'],cwd=isolated,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=60)
        captured=(r.stdout+'\n'+r.stderr).rstrip('\r\n')
        (OUT/'preflight-only.log').write_text(captured+('\n' if captured else ''),encoding='utf8',newline='\n')
        results.append(dict(check='standalone-launcher-preflight-only',status='PASS' if r.returncode==0 else 'FAIL',exitCode=r.returncode))
        print(results[-1],flush=True)
        assert all(digest(PROJECT/p)==h for p,h in protected.items())
    payload=dict(checks=results,scope='OFFLINE ONLY; no browser, KRE, API or demo writes',
        sourcePreserved=True,fullLegacyRegression='PARTIAL; previous failures are not rebaselined',
        status='PASS' if all(x['status']=='PASS' for x in results) else 'PARTIAL')
    (OUT/'results.json').write_text(json.dumps(payload,indent=2)+'\n',encoding='utf8')
    return 0 if payload['status']=='PASS' else 1

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--katalon-home',required=True,type=Path);p.add_argument('--reference',type=Path);p.add_argument('--output',type=Path)
    a=p.parse_args();raise SystemExit(main(a.katalon_home.resolve(),a.reference.resolve() if a.reference else None,a.output.resolve() if a.output else None))
