"""Read-only configuration inspection. Never opens Katalon/browser or verifies live employees."""
from pathlib import Path
import argparse,ast,json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
FIELDS=['hiringManagerName','hiringManagerAlternativeName','interviewerA','interviewerB','cvLimitBytes']
def settings(root=ROOT):
    values={}
    for row in ET.parse(root/'Profiles/default.glbl').getroot():
        name=row.findtext('name')
        if name not in FIELDS:continue
        raw=row.findtext('initValue','')
        values[name]=ast.literal_eval(raw[:-1] if raw.endswith('L') else raw)
    return values
def inspect(root=ROOT,suite='CAND',cases=None,disable_auto_discover=False):
    plans=json.loads((root/'maintenance/preflight58-plan.json').read_text(encoding='utf-8'))
    ids=cases
    if ids is None:
        ids=[n.findtext('testCaseId').split('/')[-1] for n in ET.parse(root/'Test Suites'/f'RECRUIT_{suite}_25_ASC_001_TO_025.ts').getroot().findall('testCaseLink') if n.findtext('isRun')=='true']
    config=settings(root);entries={};missing={};auto_discover={}
    for case in ids:
        problems=[];pending=[]
        for name in plans[case]['requires']:
            value=config.get(name)
            if name=='cvLimitBytes':
                if not isinstance(value,(int,float)) or value<=0:
                    problems.append(dict(setting=name,state='MISSING_CONFIGURATION',canonicalState='CONFIG_MISSING'))
                elif value>50*1024*1024:
                    problems.append(dict(setting=name,state='INVALID_CONFIGURATION',canonicalState='INVALID_CONFIGURATION'))
            else:
                text=str(value or '').strip()
                if not text:
                    if disable_auto_discover:
                        problems.append(dict(setting=name,state='MISSING_CONFIGURATION',canonicalState='CONFIG_MISSING'))
                    else:
                        pending.append(name)
                        auto_discover.setdefault(name,[]).append(case)
                elif len(text)>=100:
                    problems.append(dict(setting=name,state='INVALID_CONFIGURATION',canonicalState='INVALID_CONFIGURATION'))
                elif name in ['hiringManagerAlternativeName','interviewerB']:
                    other='hiringManagerName' if name=='hiringManagerAlternativeName' else 'interviewerA'
                    other_text=str(config.get(other,'') or '').strip()
                    if other_text and ' '.join(text.split())==' '.join(other_text.split()):
                        problems.append(dict(setting=name,state='AMBIGUOUS_SUGGESTION',canonicalState='AMBIGUOUS',reason='A and B must differ'))
        if problems:missing.setdefault(problems[0]['setting'],[]).append(case)
        state = problems[0]['state'] if problems else ('DISCOVERY_PENDING' if pending else 'READY_FOR_UI_VERIFICATION')
        entries[case]=dict(requires=plans[case]['requires'],state=state,
            mode='AUTO_DISCOVER' if pending else 'MANUAL_OVERRIDE',
            discoveryPending=pending,problems=problems)
    return dict(suite=suite,caseCount=len(entries),missing=missing,autoDiscover=auto_discover,cases=entries,
        identityRequests={name:config[name] for name in FIELDS if name!='cvLimitBytes' and config.get(name)},
        note='No live identity verified. Empty employee settings use read-only AUTO_DISCOVER from Vacancies table + Add Vacancy/Schedule Interview autocomplete verification once per execution.')
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--suite',choices=['VAC','CAND','FLOW'],default='CAND');p.add_argument('--case',action='append');p.add_argument('--project',type=Path,default=ROOT);a=p.parse_args()
    print(json.dumps(inspect(a.project,a.suite,a.case),ensure_ascii=False,indent=2))
