"""Review task scope/security and reference preservation; read-only except review JSON.

python final_review.py --source <reference-project> --group-reference <original-checkout>
No Git mutation, browser, API, Katalon execution, secret value or source rewrite.
"""
from pathlib import Path
import argparse, collections, hashlib, json, re, subprocess
import team_feedback as t

def main(source,group):
    audit=t.read(t.OUT/'audit.json')
    original_branch=subprocess.check_output(['git','-c','safe.directory='+group.as_posix(),'-C',str(group),'branch','--show-current'],text=True).strip()
    original_status=subprocess.check_output(['git','-c','safe.directory='+group.as_posix(),'-C',str(group),'status','--porcelain=v1','-uall'],text=True)
    assert original_branch==audit['originalGroupBranch'] and original_status==audit['originalGroupStatus']
    source_checks={}
    for row in audit['sourceComparison']:
        if row['sourceSha256'] is None: continue # release-only documentation
        source_checks[row['path']]=t.sha(source/row['path'])==row['sourceSha256']
    assert all(source_checks.values())
    official={};evidence={}
    for run in t.runs():
        leaf=(t.REPO/run['officialHtml']).parent
        for p in leaf.iterdir():
            if not p.is_file():continue
            relative=p.relative_to(t.REPORT/'UNCLASSIFIED')
            original=source/relative
            official[relative.as_posix()]=original.is_file() and t.sha(p)==t.sha(original)
        for case in run['cases']:
            for shot in case['screenshots']:
                p=t.REPO/shot['path'];relative=p.relative_to(t.REPO/'Envidence - Group01/23130299')
                originals=[source/relative,group/relative]
                evidence[relative.as_posix()]=any(q.is_file() and t.sha(q)==shot['sha256'] for q in originals)
    assert all(official.values()) and all(evidence.values())
    names=set(t.git('diff','--name-only','HEAD').splitlines())
    names.update(filter(None,t.git('ls-files','--others','--exclude-standard','-z').split('\0')))
    allowed_exact={'README.md','Automation Testing Project - Group 01/README.md',
        'Test Report - Group 01/README.md','Envidence - Group01/README.md',
        'evidence/executions/README.md','automation/23130299/KatalonStudio/README.md',
        'docs/automation/recruit-checklist.md','docs/automation/ASM8_RECRUITMENT_TEAM_LEADER_FIX_REPORT.md'}
    new_report_names={'ANOMALY_SCREENSHOTS.json','ANOMALY_SCREENSHOTS.md','RESULTS75.json','RESULTS75.md',
        'REPORT_LINKS.md','index.html','recruitment-failure-analysis.md'}
    unexpected=[n for n in names if n not in allowed_exact and not n.startswith('maintenance/asm8-23130299/')
        and not (n.startswith('Test Report - Group 01/Individual/23130299/') and Path(n).name in new_report_names)]
    assert not unexpected,unexpected
    patterns={
        'private-key':r'-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----',
        'github-token':r'\b(?:gh[pousr]_[A-Za-z0-9]{25,}|github_pat_[A-Za-z0-9_]{25,})\b',
        'aws-access-key':r'\bAKIA[A-Z0-9]{16}\b',
        'jwt':r'\beyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\b',
        'unmasked-sensitive-assignment':r'(?i)\b(?:password|passwd|authorization|cookie|sessionid)\s*[=:]\s*[\"\x27]?(?!\*|\[REDACTED\]|null|false|\"\"|\x27\x27)[A-Za-z0-9_+/-]{12,}',
        'browser-session-id':r'(?im)^\s*Session ID:\s*[a-f0-9]{20,}'
    }
    secrets=[];large=[];conflicts=[];categories=collections.Counter()
    for n in sorted(names):
        p=t.REPO/n
        if not p.is_file():continue
        categories['documentation' if p.suffix=='.md' else 'derived-report' if n.startswith('Test Report') else 'offline-tooling-and-proof']+=1
        if p.stat().st_size>5*1024*1024:large.append(dict(path=n,size=p.stat().st_size))
        text=p.read_text(encoding='utf8',errors='replace')
        for kind,pattern in patterns.items():
            # Record filenames/type only, never matched secret values.
            if re.search(pattern,text):secrets.append(dict(path=n,kind=kind))
        if re.search(r'(?m)^(?:<<<<<<< |=======\s*$|>>>>>>> )',text):conflicts.append(n)
    assert not secrets,secrets
    assert not large,large
    assert not conflicts,conflicts
    previous=t.read(t.OUT/'validation.json')
    result=dict(status='PASS',branch=t.git('branch','--show-current').strip(),releaseBase=t.BASE,
        originalGroupBranchAndStatus='UNCHANGED',downloadsSourceFilesChecked=len(source_checks),
        downloadsSourceDrift=[],officialOriginalFilesChecked=len(official),officialOriginalMismatches=[],
        latestPngsMatchedToOriginals=len(evidence),latestPngMismatches=[],
        scopeUnexpectedFiles=unexpected,changedFiles=sorted(names),categories=dict(categories),
        largeNewFiles=large,secretFindings=secrets,conflictMarkers=conflicts,
        linkCheck=previous['checkedLinks'],runtimeSourceChanges='NONE',
        sharedHelperChanges='NONE',workbookExpectedResultChanges='NONE',
        caveat='Pattern scan and scoped file review; not a claim of exhaustive secret detection.')
    t.dump(t.OUT/'final-review.json',result)
    print('FINAL REVIEW PASS',len(names),'files;',len(source_checks),'source references;',len(official),'original report files;',len(evidence),'PNGs;',previous['checkedLinks'],'links')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,required=True);p.add_argument('--group-reference',type=Path,required=True)
    a=p.parse_args();main(a.source.resolve(),a.group_reference.resolve())
