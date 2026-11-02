"""Offline Recruitment audit and derived handoff indexes. Never executes Katalon cases.

python team_feedback.py audit --source <reference-project> --group-reference <checkout>
python team_feedback.py build
python team_feedback.py validate
Existing official reports, supplementary reports, screenshots and source are read-only.
"""
from pathlib import Path
import argparse, collections, hashlib, html, json, os, re, subprocess
import xml.etree.ElementTree as ET
from urllib.parse import quote, unquote, urlsplit

REPO = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / 'team-feedback'
PROJECT = REPO / 'automation/23130299/KatalonStudio'
REPORT = REPO / 'Test Report - Group 01/Individual/23130299'
EVIDENCE = REPO / 'Envidence - Group01/23130299/evidence/executions/23130299'
BASE = 'e7c89705625cb4809ae6a8e5da81fb6871ab011d'

def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def rel(p): return p.relative_to(REPO).as_posix()
def write(p, text):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8', newline='\n')
def dump(p, data): write(p, json.dumps(data, ensure_ascii=False, indent=2) + '\n')
def git(*args):
    return subprocess.check_output(['git', '-c', 'safe.directory='+REPO.as_posix(),
        '-c', 'core.longpaths=true', '-C', str(REPO), *args], encoding='utf-8')
def link(origin, target, label):
    assert target.exists(), target
    href = quote(os.path.relpath(target, origin.parent).replace('\\', '/'), safe='/')
    return f'[{label}]({href})'
def walk(value):
    if isinstance(value, dict):
        yield value
        for item in value.values(): yield from walk(item)
    elif isinstance(value, list):
        for item in value: yield from walk(item)
def entities(p):
    text = p.read_text(encoding='utf-8-sig'); found = {}
    for m in re.finditer(r"loadExecutionData\((?:'[^']*'|null),\s*", text):
        try: data = json.JSONDecoder().raw_decode(text[m.end():])[0]
        except json.JSONDecodeError: continue
        for row in walk(data):
            if row.get('type') == 'TEST_CASE' and row.get('entityId'):
                cid = row['entityId'].split('/')[-1]
                if cid in found: assert found[cid]['result'] == row['result']
                found[cid] = row
    return found

def diagnostic(text):
    # Keep the error/stack diagnosis; omit historical machine/session diagnostics in new summaries.
    return '\n'.join(line for line in text.splitlines() if not re.match(
        r'(?i)^(Capabilities\s|Session ID:|System info:|Driver info:)',line.strip()))

def runs():
    result = []
    for junit in sorted(REPORT.glob('UNCLASSIFIED/Reports/**/JUnit_Report.xml')):
        leaf = junit.parent; stamp = leaf.name
        official = leaf / (stamp + '.html'); cases = entities(official)
        nodes = ET.parse(junit).findall('.//testcase')
        assert len(cases) == len(nodes) == 25
        module = nodes[0].get('name').split('/')[-1].split('_')[1]
        matches = list(EVIDENCE.glob('RECRUIT_'+module+'*/reports/UNCLASSIFIED/results.json'))
        assert len(matches) == 1
        supplemental = read(matches[0]); callback = {c['caseId']:c for c in supplemental['cases']}
        derived_xml={c.get('name'):('ERROR' if c.find('error') is not None else 'FAILED' if c.find('failure') is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASSED')
            for c in ET.parse(matches[0].parent/'results.xml').findall('testcase')}
        runtime = supplemental['executionId']
        evidence_dir = matches[0].parents[2]
        assert runtime[:12] in evidence_dir.name
        rows = []
        for node in nodes:
            cid = node.get('name').split('/')[-1]; c = cases[cid]; cb = callback[cid]
            assert c['result'] == node.get('status') == cb['result'], cid
            assert c['result'] == derived_xml[cid], cid
            manifests = list(evidence_dir.glob(cid+'/*/evidence.json'))
            images = []; manifest_paths = []
            for p in manifests:
                m = read(p)
                assert m['executionId'] == runtime and m['caseId'] == cid
                assert m['caseRunId'] == cb['caseRunId'] and m['finalResult'] == c['result']
                manifest_paths.append(rel(p))
                for checkpoint in m['checkpoints']:
                    image = p.parent / checkpoint['screenshotPath']
                    assert image.is_file() and sha(image) == checkpoint['sha256']
                    images.append(dict(path=rel(image), sha256=sha(image),
                        checkpoint=checkpoint['checkpoint'], timestamp=checkpoint['timestamp'],
                        caseRunId=m['caseRunId']))
            assert images or c['result'] == 'SKIPPED', cid
            errors = [n.text or n.get('message','') for n in node if n.tag in ('error','failure','skipped')]
            rows.append(dict(caseId=cid, module=module, executionId=stamp, runtimeUuid=runtime,
                caseRunUuid=cb['caseRunId'], runKind='UNCLASSIFIED', status=c['result'],
                htmlStatus=c['result'], junitStatus=node.get('status'),
                startedAt=c.get('startTime'), endedAt=c.get('endTime'),
                errorMessage=diagnostic('\n'.join(errors) or (c.get('message','') if c['result'] != 'PASSED' else '')),
                screenshots=images, evidenceManifests=manifest_paths,
                officialHtml=rel(official), officialJunit=rel(junit),
                consoleLog=rel(leaf/'console0.log'), executionLog=rel(leaf/'execution0.log'),
                supplementary={n:rel(matches[0].parent/n) for n in ['results.html','results.json','results.xml','execution.log']},
                provenance={rel(p):sha(p) for p in [official,junit]}))
        result.append(dict(module=module, executionId=stamp, runtimeUuid=runtime,
            counts=dict(collections.Counter(c['status'] for c in rows)), cases=rows,
            officialHtml=rel(official), officialJunit=rel(junit), consoleLog=rel(leaf/'console0.log'),
            executionLog=rel(leaf/'execution0.log'), supplementary=rows[0]['supplementary'], evidence=rel(evidence_dir)))
    assert len(result) == 3
    return sorted(result, key=lambda r: ['VAC','CAND','FLOW'].index(r['module']))

def audit(source, group):
    assert not (OUT/'audit.json').exists(), 'Preserve previous audit; use its snapshot'
    tracked = git('ls-files','-z').split('\0')
    freeze = {n:sha(REPO/n) for n in tracked if n and (REPO/n).is_file()}
    comparisons = []
    for n in tracked:
        if n.startswith('automation/23130299/KatalonStudio/') and not n.endswith(('README.md','.gitattributes')):
            relative = n.split('KatalonStudio/',1)[1]; p = source/relative
            comparisons.append(dict(path=relative, repositorySha256=sha(REPO/n),
                sourceSha256=sha(p) if p.is_file() else None,
                match=p.is_file() and sha(p)==sha(REPO/n)))
    r = runs()
    latest = []
    for p in sorted((source/'Reports').glob('**/JUnit_Report.xml')):
        names = [n.get('name','') for n in ET.parse(p).findall('.//testcase')]
        module = re.search(r'RECRUIT_(VAC|CAND|FLOW)', ' '.join(names))
        if module: latest.append(dict(module=module[1],executionId=p.parent.name,path=p.relative_to(source).as_posix(),sha256=sha(p)))
    # Source/metadata scans intentionally exclude browser profiles, session context and cache.
    runtime = []
    for folder in ['Keywords','Scripts','Test Listeners','Profiles','settings','maintenance']:
        for p in (PROJECT/folder).rglob('*'):
            if p.is_file():
                text = p.read_text(encoding='utf8', errors='replace')
                if re.search(r'(?i)[A-Z]:[/\\]+Users[/\\]+|user\.home|user\.dir',text):
                    runtime.append(rel(p))
    dump(OUT/'audit.json', dict(base=BASE,branch=git('branch','--show-current').strip(),
        sourceComparison=comparisons, officialExecutions=latest, latestRuns=[{k:v for k,v in x.items() if k!='cases'} for x in r],
        hardcodedRuntimeFindings=runtime, preservedTrackedSha256=freeze,
        originalGroupBranch=subprocess.check_output(['git','-c','safe.directory='+group.as_posix(),'-C',str(group),'branch','--show-current'],text=True).strip(),
        originalGroupStatus=subprocess.check_output(['git','-c','safe.directory='+group.as_posix(),'-C',str(group),'status','--porcelain=v1','-uall'],text=True)))
    gap = '''# Gap analysis before repair — 2026-11-02

Baseline e7c89705625cb4809ae6a8e5da81fb6871ab011d; working checkout NVThang is a different history. Release has already handed over the files. No source recopy is justified.

| Area | Observed before repair | Necessary action |
| --- | --- | --- |
| Runtime paths | PortablePaths + ASM7_EVIDENCE_ROOT present; no personal runtime path found | Rerun unchanged 10-group harness in fresh temporary directory; preserve source |
| Prior portability claim | Archived log has 9 PASS then FileAlreadyExistsException, despite handoff saying PASS | Preserve old log; publish actual fresh result separately |
| Reports | All three latest official HTML/JUnit/log leaves already present | Reuse unchanged, add member-level entry point |
| Supplementary | JSON/HTML/XML and 288 original PNGs already present | Link; no duplicate evidence |
| RESULTS75 | Existing derived JSON/CSV embeds original layout paths | Preserve original; generate new official-derived JSON/Markdown with portable repository links |
| Failure analysis | Existing analysis uses ../Reports and ../evidence paths that no longer resolve in report package | Preserve archived analysis; add corrected linked analysis and anomaly index |
| Checklist | Still says CAND/FLOW unverified and old VAC only | Update to latest provenance and explicit group pending |
| Runner | Release has staging README but no run.mjs/summarize.mjs; NVThang has Node adapters and explicit Recruitment skip | Document both branch states; do not copy/rewrite shared runner |
| Standalone | Source/Profile/plan/runtime workbook present | Offline compile/preflight; browser remains NOT VERIFIED |
| Integration | Lead decision is SKIPPED; no shared Recruitment execution | Keep SKIPPED_BY_GROUP_DECISION / GROUP_INTEGRATION_PENDING |
'''
    write(OUT/'GAP_ANALYSIS.md',gap)
    print('AUDIT',len(comparisons),'source/config compared;',sum(not x['match'] for x in comparisons),'differences;',len(latest),'official executions;',len(runtime),'runtime findings')

def reference_evidence(source, group):
    inventory=[]; historical=[]
    for junit in sorted((source/'Reports').glob('**/JUnit_Report.xml')):
        leaf=junit.parent; official=leaf/(leaf.name+'.html'); console=leaf/'console0.log'
        if not official.exists() or not console.exists(): continue
        rows=entities(official)
        runtime_ids=set(re.findall(r'"executionId"\s*:\s*"([a-f0-9-]{36})"',console.read_text(encoding='utf8',errors='replace')))
        if len(runtime_ids)!=1: continue
        runtime=next(iter(runtime_ids))
        for node in ET.parse(junit).findall('.//testcase'):
            cid=node.get('name','').split('/')[-1]; c=rows.get(cid)
            if not c or node.get('status')!=c.get('result'): continue
            historical.append(dict(caseId=cid,runtimeUuid=runtime,executionId=leaf.name,
                status=c['result'],startedAt=c.get('startTime'),endedAt=c.get('endTime'),
                report=official.relative_to(source).as_posix(),reportSha256=sha(official),
                junit=junit.relative_to(source).as_posix(),junitSha256=sha(junit),
                log=console.relative_to(source).as_posix(),logSha256=sha(console),
                observation=diagnostic('\n'.join(n.text or '' for n in node if n.tag in ['error','failure','skipped']))[:900]))
    for origin,base in [('DOWNLOADS_PROJECT',source),('ORIGINAL_GROUP_CHECKOUT',group)]:
        root=base/'evidence/executions/23130299'
        for p in sorted(root.rglob('*.png')):
            if re.search(r'_(ERROR|STOPPED|FAILED)\.png$',p.name,re.I):
                inventory.append(dict(origin=origin,referencePath=p.relative_to(base).as_posix(),sha256=sha(p)))
    dump(OUT/'reference-evidence.json',dict(scope='Read-only anomaly inventory; runtime browser profiles excluded',
        records=inventory,historicalOfficialCases=historical))
    print('REFERENCE ANOMALY INVENTORY',len(inventory))

DIAGNOSES = {
 'RECRUIT_VAC_Filter_02': ('REQUIREMENT DISCREPANCY', 'Workbook requests a nonexistent Vacancy; UI selects an existing numeric ID. Helper stops before substituting input.', 'Lead must reconcile requirement with UI; preserve Expected Result.', 'Approved requirement decision; browser retest pending.'),
 'RECRUIT_CAND_UploadCV_04': ('CONFIGURATION BLOCKED', 'Exact CV maximum in bytes is unconfirmed; cvLimitBytes=0L causes official preflight SKIPPED.', 'Verify authoritative exact maxFileSize bytes before boundary upload.', 'Exact byte configuration and approved browser retest pending.'),
 'RECRUIT_FLOW_Status_01': ('UNKNOWN', 'Candidate81 Save yielded toast/route but profile showed No Records Found; persistence/readback unverified. Underlying backend cause UNKNOWN.', 'Read-only reconciliation of exact Candidate81 when authorized; never replay uncertain Save/Shortlist or pending write.', 'Readback reconciliation and approved browser retest pending.'),
 'RECRUIT_FLOW_SelectInterviewer_02': ('VERIFIED ROOT CAUSE / RETEST PENDING', 'Exact locator Add another differs from visible Add Another. Release source already corrected; original execution remains ERROR.', 'Retest corrected case plus single-interviewer canary only after write approval and active numeric A/B confirmation.', 'Offline check does not prove browser recovery; official browser retest pending.')
}

def build():
    read(OUT/'audit.json') # audit must precede any handoff edit
    execution_runs = runs(); rows = [c for r in execution_runs for c in r['cases']]
    assert len(rows)==75 and len({c['caseId'] for c in rows})==75
    assert collections.Counter(c['status'] for c in rows)==dict(PASSED=71,ERROR=3,SKIPPED=1)
    for c in rows:
        diag = DIAGNOSES.get(c['caseId'])
        c.update(classification=diag[0] if diag else 'NONE', rootCause=diag[1] if diag else None,
            action=diag[2] if diag else 'Preserve official result.',remainingBlocker=diag[3] if diag else None)
    dump(REPORT/'RESULTS75.json',dict(kind='DERIVED_FROM_OFFICIAL_KATALON_HTML_AND_JUNIT',
        groupIntegration='SKIPPED_BY_GROUP_DECISION / GROUP_INTEGRATION_PENDING',runKind='UNCLASSIFIED',
        totals=dict(PASSED=71,FAILED=0,ERROR=3,SKIPPED=1),cases=rows))
    p=REPORT/'REPORT_LINKS.md'
    text='# Original report and evidence locations\n\nThree local UNCLASSIFIED executions. Every official file remains unchanged; supplementary files are callback-derived.\n\n| Module / execution / runtime UUID | Official | Supplementary | Logs / evidence |\n| --- | --- | --- | --- |\n'
    for r in execution_runs:
        official=' · '.join(link(p,REPO/r[k],label) for k,label in [('officialHtml','HTML'),('officialJunit','JUnit XML')])
        supplementary=' · '.join(link(p,REPO/v,k) for k,v in r['supplementary'].items())
        logs=' · '.join(link(p,REPO/r[k],label) for k,label in [('consoleLog','Console'),('executionLog','Execution log'),('evidence','Evidence directory')])
        text+='| '+r['module']+' / '+r['executionId']+' / '+r['runtimeUuid']+' | '+official+' | '+supplementary+' | '+logs+' |\n'
    write(p,text)
    p=REPORT/'RESULTS75.md'
    text='# Recruitment — 75 official local case results\n\n71 PASSED / 0 FAILED / 3 ERROR / 1 SKIPPED. Three separate local executions, UNCLASSIFIED. Not a shared ASM8 run. JSON includes exact timestamps, messages, provenance hashes, runtime/case UUIDs and blockers.\n\n'
    text+=link(p,REPORT/'RESULTS75.json','Machine-readable results')+'\n\n| Case | Execution | Final official | Start / end (+07:00) | Evidence / reports / logs | Analysis |\n| --- | --- | --- | --- | --- | --- |\n'
    for c in rows:
        targets=[(c['officialHtml'],'HTML'),(c['officialJunit'],'JUnit'),(c['consoleLog'],'Console'),(c['executionLog'],'Execution log')]
        targets += [(s['path'],f'PNG {i+1}') for i,s in enumerate(c['screenshots'])]
        if not c['screenshots']: targets += [(c['supplementary']['execution.log'],'Preflight log (no PNG)')]
        text+='| '+c['caseId']+' | '+c['executionId']+' | '+c['status']+' | '+str(c['startedAt'])+' / '+str(c['endedAt'])+' | '+' · '.join(link(p,REPO/t,l) for t,l in targets)+' | '+(c['classification'] if c['status']!='PASSED' else '—')+' |\n'
    write(p,text)
    p=REPORT/'index.html'; esc=html.escape
    def a(target,label):
        assert target.exists(),target
        return '<a href="'+quote(os.path.relpath(target,p.parent).replace('\\','/'),safe='/')+'">'+esc(label)+'</a>'
    text='<!doctype html>\n<html lang="vi"><meta charset="utf-8"><title>Recruitment 23130299 — local reports</title><h1>Recruitment — 23130299</h1><p>71 PASSED / 0 FAILED / 3 ERROR / 1 SKIPPED. 75 local cases, three separate UNCLASSIFIED executions. GROUP_INTEGRATION_PENDING / SKIPPED_BY_GROUP_DECISION.</p><p>'+a(REPORT/'RESULTS75.md','75 case results')+' · '+a(REPORT/'RESULTS75.json','JSON')+'</p><table border="1"><tr><th>Module</th><th>Execution / runtime UUID</th><th>Total / PASSED / FAILED / ERROR / SKIPPED</th><th>Reports and logs</th></tr>\n'
    for r in execution_runs:
        links=[(r['officialHtml'],'Official HTML'),(r['officialJunit'],'Official JUnit'),(r['consoleLog'],'Console'),(r['executionLog'],'Official execution log'),(r['evidence'],'Evidence')]
        links += [(v,'Supplementary '+k) for k,v in r['supplementary'].items()]
        counts=' / '.join(str(r['counts'].get(s,0)) for s in ['PASSED','FAILED','ERROR','SKIPPED'])
        text+='<tr><td>'+r['module']+'</td><td>'+r['executionId']+'<br>'+r['runtimeUuid']+'</td><td>25 / '+counts+'</td><td>'+' · '.join(a(REPO/t,l) for t,l in links)+'</td></tr>\n'
    text+='</table><p>Supplementary reports are callback-derived; final status is cross-checked against official HTML/JUnit. Screenshot suffix does not determine final status. Original reports are immutable.</p></html>\n'
    write(p,text)
    p=REPORT/'recruitment-failure-analysis.md'
    text='# Recruitment failure analysis\n\nOfficial HTML, JUnit and callback statuses agree for all 75 cases. These are local runs, not group results. ERROR remains distinct from FAILED. No inferred HTTP status or backend diagnosis. All browser retests are pending.\n\n| Case | Execution | Final status | Evidence | Root cause | Action | Retest |\n| --- | --- | --- | --- | --- | --- | --- |\n'
    for c in rows:
        if c['status']=='PASSED': continue
        refs=[(s['path'],Path(s['path']).name) for s in c['screenshots']]
        refs += [(c['officialHtml'],'HTML'),(c['officialJunit'],'JUnit'),(c['consoleLog'],'Console'),(c['supplementary']['execution.log'],'Supplementary/preflight log')]
        text+='| '+c['caseId']+' | '+c['executionId']+' / '+c['runtimeUuid']+' | '+c['status']+' | '+' · '.join(link(p,REPO/t,l) for t,l in refs)+' | '+c['classification']+': '+c['rootCause']+' | '+c['action']+' | '+c['remainingBlocker']+' |\n'
    text+='\nSTOPPED records a framework/helper stop before the final callback; ERROR records the final error capture. BEFORE/AFTER/CHECK/RESULT are checkpoints. A suffix is never promoted to an assertion. Latest non-passed cases are not a demonstrated cascade. Historical cascades remain UNKNOWN without their exact originating log.\n\n[Searchable ERROR/STOPPED/FAILED screenshot index](ANOMALY_SCREENSHOTS.md)\n'
    # Written after anomaly index below, so link target must exist first.
    anomalies=[]; by_sha=collections.defaultdict(list)
    for c in rows:
        for s in c['screenshots']: by_sha[s['sha256']].append((c,s))
    for origin,base in [('REPOSITORY',REPO/'evidence/executions/23130299'),('SUBMITTED_LATEST',EVIDENCE)]:
        for image in sorted(base.rglob('*.png')):
            if not re.search(r'_(ERROR|STOPPED|FAILED)\.png$',image.name,re.I): continue
            cid=next((x for x in image.parts if re.fullmatch(r'RECRUIT_(?:VAC|CAND|FLOW)_[A-Za-z][A-Za-z0-9]*_\d\d',x)),None)
            candidates=[(c,s) for c,s in by_sha[sha(image)] if c['caseId']==cid and c['runtimeUuid'][:12] in image.as_posix()]
            c=candidates[0][0] if candidates else None
            anomalies.append(dict(origin=origin,path=rel(image),sha256=sha(image),caseId=cid,
                classification='LATEST_EXECUTION_CHECKPOINT' if c else 'HISTORICAL_EVIDENCE_UNMATCHED',
                officialStatus=c['status'] if c else 'UNVERIFIED',executionId=c['executionId'] if c else None,
                runtimeUuid=c['runtimeUuid'] if c else None,consoleLog=c['consoleLog'] if c else None,
                rootCause=c['rootCause'] if c else 'UNKNOWN; exact original final report not uniquely linked'))
    inventory=OUT/'reference-evidence.json'
    for x in read(inventory)['records'] if inventory.exists() else []:
        # Already submitted historical files need only one canonical link.
        if any(a['sha256']==x['sha256'] and a['path']==x['referencePath'] for a in anomalies): continue
        cid_match=re.search(r'RECRUIT_(?:VAC|CAND|FLOW)_[A-Za-z][A-Za-z0-9]*_\d\d',x['referencePath'])
        cid=cid_match[0] if cid_match else None
        candidates=[(c,s) for c,s in by_sha[x['sha256']] if c['caseId']==cid and c['runtimeUuid'][:12] in x['referencePath']]
        c,s=candidates[0] if candidates else (None,None)
        anomalies.append(dict(origin=x['origin'],referencePath=x['referencePath'],path=s['path'] if s else None,
            sha256=x['sha256'],caseId=cid,classification='LATEST_EXECUTION_CHECKPOINT' if c else 'HISTORICAL_EVIDENCE_UNMATCHED',
            officialStatus=c['status'] if c else 'UNVERIFIED',executionId=c['executionId'] if c else None,
            runtimeUuid=c['runtimeUuid'] if c else None,consoleLog=c['consoleLog'] if c else None,
            rootCause=c['rootCause'] if c else 'UNKNOWN; reference-only file, exact final report not uniquely linked'))
    historical=read(inventory).get('historicalOfficialCases',[]) if inventory.exists() else []
    for x in anomalies:
        if x['classification']!='HISTORICAL_EVIDENCE_UNMATCHED': continue
        path=x.get('referencePath') or x.get('path') or ''
        timestamp=re.search(r'(20\d{6})_(\d{6})',Path(path).name)
        matches=[]
        for c in historical:
            if c['caseId']!=x['caseId']: continue
            uuid_match=c['runtimeUuid'][:12] in path
            interval=False
            if timestamp and c['startedAt'] and c['endedAt']:
                stamp=timestamp[1]+timestamp[2]
                compact=lambda t:re.sub(r'[-:T]','',t[:19])
                interval=compact(c['startedAt'])<=stamp<=compact(c['endedAt'])
            if uuid_match or interval:matches.append(c)
        if len(matches)==1:
            c=matches[0]
            x.update(classification='HISTORICAL_OFFICIAL_VERIFIED_REFERENCE_ONLY',officialStatus=c['status'],
                executionId=c['executionId'],runtimeUuid=c['runtimeUuid'],
                referenceOfficialReport=c['report'],referenceOfficialLog=c['log'],
                referenceProvenance={c[k]:c[k+'Sha256'] for k in ['report','junit','log']},
                verifiedObservation=c['observation'],
                rootCause='UNKNOWN; exact final HTML/JUnit verified in source reference. '+('Preflight/configuration stop observed.' if 'PREFLIGHT' in c['observation'] or 'CONFIGURATION' in c['observation'] else 'See original exception; causal claim requires original execution review.'))
    dump(REPORT/'ANOMALY_SCREENSHOTS.json',anomalies)
    q=REPORT/'ANOMALY_SCREENSHOTS.md'
    table='# ERROR / STOPPED / FAILED screenshots\n\nSearch by Case ID. Latest images are matched by runtime UUID, case ID and SHA-256. Historical unmatched images do not establish a final FAILED/ERROR result. Originals are unchanged.\n\n| Case | Image | Execution | Official status | Classification / cause | Log |\n| --- | --- | --- | --- | --- | --- |\n'
    for x in anomalies:
        image_link=link(q,REPO/x['path'],Path(x['path']).name) if x['path'] else 'REFERENCE ONLY: '+x['referencePath']
        log_link=link(q,REPO/x['consoleLog'],'Console') if x['consoleLog'] else ('SOURCE REFERENCE ONLY: '+x['referenceOfficialLog'] if x.get('referenceOfficialLog') else 'MISSING / UNKNOWN')
        table+='| '+str(x['caseId'])+' | '+image_link+' | '+str(x['executionId'] or 'UNKNOWN')+' | '+x['officialStatus']+' | '+x['classification']+': '+x['rootCause']+' | '+log_link+' |\n'
    write(q,table)
    write(p,text)
    # Add links only to the new, non-historical index.
    index=REPORT/'index.html'
    write(index,index.read_text(encoding='utf8').replace('</html>','<p><a href="recruitment-failure-analysis.md">Failure analysis</a> · <a href="ANOMALY_SCREENSHOTS.md">Anomaly screenshot index</a></p></html>'))
    print('BUILD 75 cases;',sum(len(c['screenshots']) for c in rows),'screenshots;',len(anomalies),'anomaly rows')

def validate():
    audit=read(OUT/'audit.json'); matrix=read(REPORT/'RESULTS75.json'); current=runs()
    assert [(c['caseId'],c['status']) for c in matrix['cases']]==[(c['caseId'],c['status']) for r in current for c in r['cases']]
    preserved={n:sha(REPO/n)==h for n,h in audit['preservedTrackedSha256'].items()
        if n.startswith(('automation/23130299/KatalonStudio/Keywords/','automation/23130299/KatalonStudio/Scripts/',
            'automation/23130299/KatalonStudio/Test Cases/','automation/23130299/KatalonStudio/Test Suites/',
            'automation/23130299/KatalonStudio/Test Listeners/','automation/23130299/KatalonStudio/Profiles/',
            'automation/23130299/KatalonStudio/settings/','automation/23130299/KatalonStudio/maintenance/',
            'docs/test-cases/','Test Report - Group 01/Individual/','Envidence - Group01/23130299/','evidence/executions/23130299/'))}
    assert all(preserved.values()),[n for n,v in preserved.items() if not v]
    scripts=list((PROJECT/'Scripts').glob('*/*/*.groovy')); cases=list((PROJECT/'Test Cases').glob('*/*.tc'))
    suites=list((PROJECT/'Test Suites').glob('*.ts'))
    guids=[ET.parse(p).findtext('testCaseGuid') for p in cases]
    assert len(scripts)==len(cases)==75 and len(suites)==3 and len(set(guids))==75
    order={p.name:[x.findtext('testCaseId') for x in ET.parse(p).findall('testCaseLink')] for p in suites}
    assert all(len(v)==25 and len(set(v))==25 for v in order.values())
    fqcn=[]
    for p in (PROJECT/'Keywords').rglob('*.groovy'):
        s=p.read_text(encoding='utf8'); package=re.search(r'(?m)^package\s+(\w+(?:\.\w+)*)',s)
        fqcn += [(package[1]+'.' if package else '')+c for c in re.findall(r'(?m)^(?:public\s+)?(?:class|enum|interface)\s+(\w+)',s)]
    assert len(set(fqcn))==len(fqcn)
    broken=[]; checked=0
    new_files=[REPORT/n for n in ['index.html','REPORT_LINKS.md','RESULTS75.md','recruitment-failure-analysis.md','ANOMALY_SCREENSHOTS.md']]
    new_files += [REPO/n for n in ['README.md','Test Report - Group 01/README.md','Envidence - Group01/README.md','evidence/executions/README.md','Automation Testing Project - Group 01/README.md','automation/23130299/KatalonStudio/README.md','docs/automation/recruit-checklist.md','docs/automation/ASM8_RECRUITMENT_TEAM_LEADER_FIX_REPORT.md']]
    new_files += [REPO/v for r in current for k,v in r['supplementary'].items() if k=='results.html']
    for p in new_files:
        if not p.exists(): continue
        s=p.read_text(encoding='utf8')
        for href in re.findall(r'href="([^"]+)"|\]\(([^)]+)\)',s):
            u=html.unescape(href[0] or href[1]); parts=urlsplit(u)
            if parts.scheme or not parts.path: continue
            target=(p.parent/unquote(parts.path)).resolve(); checked+=1
            if not target.exists(): broken.append(dict(file=rel(p),target=u))
    assert not broken,broken
    all_png={rel(p) for p in EVIDENCE.rglob('*.png')}
    mapped={s['path'] for c in matrix['cases'] for s in c['screenshots']}
    assert all_png==mapped and len(mapped)==288
    dump(OUT/'validation.json',dict(status='PASS_OFFLINE',scripts=len(scripts),testCases=len(cases),
        suites=len(suites),uniqueGuids=len(set(guids)),uniqueFqcn=len(fqcn),suiteOrder=order,
        preservedFiles=len(preserved),preservation=preserved,checkedLinks=checked,brokenLinks=broken,
        mappedScreenshots=len(mapped),officialHtmlJunitCallbackCaseMatches=75,
        businessRetest='NOT_EXECUTED',groupIntegration='GROUP_INTEGRATION_PENDING'))
    print('VALIDATE PASS',len(preserved),'immutable files;',checked,'links;',len(mapped),'PNGs; 75 official statuses')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=['audit','references','build','validate'])
    parser.add_argument('--source',type=Path);parser.add_argument('--group-reference',type=Path)
    args=parser.parse_args()
    if args.command=='audit': audit(args.source.resolve(),args.group_reference.resolve())
    elif args.command=='references': reference_evidence(args.source.resolve(),args.group_reference.resolve())
    elif args.command=='build': build()
    else: validate()
