"""Prepare frozen Katalon projects. KRE starts ONLY with the user's explicit --execute flag.

Resource admission is outside Katalon: two OS slots by default, never 25 listener errors.
No WebDriver, login, website, credential generation, or source synchronization into active workers.
"""
from pathlib import Path
import argparse, concurrent.futures, hashlib, json, os, subprocess, time, uuid
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
PROJECT='OrangeHRM_RECRUIT_VAC_KatalonStudio.prj'
FOLDERS=['Keywords','Scripts','Test Cases','Test Suites','Test Listeners','Profiles','Object Repository',
         'Data Files','Include','Drivers','Plugins','settings','.settings','Checkpoints']
ROOT_FILES=[PROJECT,'console.properties',
            'maintenance/preflight58-plan.json','maintenance/workbook75-current.json']

def atomic_json(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    staging=path.with_name('.'+path.name+'.'+uuid.uuid4().hex+'.tmp')
    try:
        with staging.open('x',encoding='utf-8') as stream:
            json.dump(data,stream,ensure_ascii=False,indent=2);stream.flush();os.fsync(stream.fileno())
        os.replace(staging,path)
    finally:
        if staging.exists():staging.unlink() # this call's staging file only

def configured_limit(root=ROOT):
    profile=ET.parse(root/'Profiles/default.glbl')
    values={v.findtext('name'):v.findtext('initValue') for v in profile.findall('GlobalVariableEntity')}
    assert values.get('executionMode')=="'ISOLATED_CONCURRENT'",'executionMode must be ISOLATED_CONCURRENT'
    limit=int(values.get('maxConcurrentSessions','2'));assert limit in [2,3], 'Supported session budget: 2 or 3'
    return limit

def prepare(suite,source=ROOT,storage=None):
    assert suite in ['VAC','CAND','FLOW']
    source=source.resolve();storage=(storage or source/'runtime/ASM7/workspaces').resolve()
    for required in [PROJECT,'Profiles/default.glbl','maintenance/preflight58-plan.json','maintenance/workbook75-current.json']:
        assert (source/required).is_file(), 'Required source dependency missing; prepare aborted: '+required
    worker=storage/str(uuid.uuid4());worker.mkdir(parents=True,exist_ok=False)
    project=worker/'project';project.mkdir()
    candidates=[source/n for n in ROOT_FILES if (source/n).is_file()]
    for folder in FOLDERS:
        candidates += [p for p in (source/folder).rglob('*') if p.is_file() and not p.is_symlink() and p.suffix not in ['.class','.pyc']]
    manifest={}
    for path in candidates:
        relative=path.relative_to(source);data=path.read_bytes()
        target=project/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
        manifest[relative.as_posix()]=hashlib.sha256(data).hexdigest()
    for relative,digest in manifest.items():
        assert hashlib.sha256((source/relative).read_bytes()).hexdigest()==digest,'Source changed during snapshot; prepare again: '+relative
    for folder in ['Reports','bin','Libs','runtime','temp']: (project/folder).mkdir(exist_ok=True)
    record=dict(kind='PREPARED ONLY - NOT EXECUTED',workspaceId=worker.name,suite=suite,source=str(source),
                project=str(project/PROJECT),reportPath=str(project/'Reports'),tempPath=str(worker/'temp'),
                eclipseWorkspace=str(worker/'eclipse-workspace'),
                coordinator=str(source/'runtime/ASM7/admission'),sourceSha256=manifest)
    (worker/'temp').mkdir()
    atomic_json(worker/'workspace.json',record)
    atomic_json(project/'asm7-worker.json',dict(source=str(source),workspaceId=worker.name,
        profileSha256={p:h for p,h in manifest.items() if p.startswith('Profiles/')},
        requiredResourceSha256={p:h for p,h in manifest.items() if p.startswith('maintenance/')}))
    return worker

class ResourceSlots:
    """Each OS lock protects one capacity slot, not the project or a test suite's business state."""
    def __init__(self,directory,limit=2):
        assert limit in [1,2,3]
        self.directory=Path(directory);self.limit=limit;self.handle=None;self.slot=None
    def try_acquire(self):
        import msvcrt
        self.directory.mkdir(parents=True,exist_ok=True)
        for slot in range(self.limit):
            handle=(self.directory/f'session-{slot}.slot').open('a+b')
            if handle.tell()==0:handle.write(b'0');handle.flush()
            handle.seek(0)
            try:msvcrt.locking(handle.fileno(),msvcrt.LK_NBLCK,1)
            except OSError:handle.close();continue
            self.handle=handle;self.slot=slot;return True
        return False
    def release(self):
        if self.handle:
            import msvcrt
            try:self.handle.seek(0);msvcrt.locking(self.handle.fileno(),msvcrt.LK_UNLCK,1)
            finally:self.handle.close();self.handle=None

def invocation(worker,engine,properties=None):
    record=json.loads((worker/'workspace.json').read_text(encoding='utf-8'))
    command=[str(engine),'-noSplash','-data',record['eclipseWorkspace'],'-runMode=console','-projectPath='+record['project'],
             '-testSuitePath=Test Suites/RECRUIT_'+record['suite']+'_25_ASC_001_TO_025',
             '-browserType=Chrome','-executionProfile=default','-retry=0',
             '-report.folderPath='+record['reportPath']]
    if properties:command += ['-propertiesFile='+str(properties)] # user's existing private KRE/license config, never copied/printed
    command += ['--config','-webui.autoUpdateDrivers=false']
    return command

def verify_worker(worker):
    record=json.loads((worker/'workspace.json').read_text(encoding='utf-8'))
    project=Path(record['project']).parent
    assert (project/'asm7-worker.json').is_file() and (project/'maintenance/preflight58-plan.json').is_file(),'STALE SNAPSHOT: worker lacks current preflight metadata; prepare again'
    for relative,digest in record['sourceSha256'].items():
        assert hashlib.sha256((project/relative).read_bytes()).hexdigest()==digest,'Prepared source changed; prepare a new workspace: '+relative
        canonical=Path(record['source'])/relative
        assert canonical.is_file() and hashlib.sha256(canonical.read_bytes()).hexdigest()==digest,'STALE SNAPSHOT: canonical source/profile changed; prepare again: '+relative
    return record

def execute(worker,engine,limit,properties=None,queue_timeout=7200):
    worker=worker.resolve();record=json.loads((worker/'workspace.json').read_text(encoding='utf-8'))
    if (worker/'runner-console.log').exists():
        raise RuntimeError('This frozen workspace was already executed. Prepare a new snapshot for a fresh KRE execution; old logs are retained.')
    verify_worker(worker) # reject known stale configuration before entering the resource queue
    # Prevent only a duplicate launch of THIS frozen workspace, before any Katalon process starts.
    workspace_lease=ResourceSlots(worker/'launch',1)
    if not workspace_lease.try_acquire():raise RuntimeError('RESOURCE LIMIT: this workspace is already running; prepare a different workspace')
    slots=ResourceSlots(record['coordinator'],limit)
    deadline=time.monotonic()+queue_timeout
    try:
        queued=False
        while not slots.try_acquire():
            if not queued:print('QUEUED before Katalon:',worker.name,flush=True);queued=True
            if time.monotonic()>=deadline:raise RuntimeError('RESOURCE LIMIT: no runner slot available before queue timeout; Katalon has NOT started')
            time.sleep(0.5)
        # Validate the frozen source before launch. No canonical source changes are synced into it.
        verify_worker(worker)
        environment=os.environ.copy();environment['TEMP']=record['tempPath'];environment['TMP']=record['tempPath']
        atomic_json(worker/'launch-status.json',dict(state='STARTING',workspaceId=worker.name,slot=slots.slot))
        with (worker/'runner-console.log').open('xb') as log:
            process=subprocess.Popen(invocation(worker,engine,properties),cwd=str(engine.parent),env=environment,stdout=log,stderr=subprocess.STDOUT)
            atomic_json(worker/'launch-status.json',dict(state='RUNNING',processId=process.pid,workspaceId=worker.name,slot=slots.slot))
            code=process.wait()
        atomic_json(worker/'launch-status.json',dict(state='EXITED',exitCode=code,workspaceId=worker.name))
        return code
    finally:slots.release();workspace_lease.release()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--suites',nargs='+',choices=['VAC','CAND','FLOW'],default=['VAC','CAND'])
    parser.add_argument('--workspace',type=Path,help='Previously prepared workspace, for explicit --execute')
    parser.add_argument('--engine',type=Path,help='Installed licensed KRE katalonc.exe; never auto-download')
    parser.add_argument('--properties-file',type=Path,help='Existing private KRE/license settings, never copied into a worker')
    parser.add_argument('--max-sessions',type=int,choices=[2,3])
    parser.add_argument('--execute',action='store_true',help='USER ONLY: starts Katalon/Chrome; default is prepare-only')
    parser.add_argument('--preflight-only',action='store_true',help='Read configuration dependencies before preparing/launching anything; no browser')
    args=parser.parse_args();limit=args.max_sessions or configured_limit()
    from preflight58 import inspect
    if args.preflight_only:
        for suite in args.suites:
            result=inspect(ROOT,suite)
            print(json.dumps({k:v for k,v in result.items() if k!='cases'},ensure_ascii=False,indent=2))
        return
    workers=[args.workspace.resolve()] if args.workspace else [prepare(s) for s in args.suites]
    for worker in workers:
        print('Prepared isolated workspace:',worker)
        verify_worker(worker)
        record=json.loads((worker/'workspace.json').read_text(encoding='utf-8'))
        plan=inspect(Path(record['project']).parent,record['suite'])
        print('CONFIGURATION PREFLIGHT (no live identity proof):',json.dumps(dict(suite=record['suite'],missing=plan['missing']),ensure_ascii=False))
    if not args.execute:
        print('PREPARED ONLY. No Katalon, Chrome, website or live test was started. Resource default:',limit)
        print('Open separate prepared .prj copies in Studio, or use this launcher with --workspace <path> --engine <licensed katalonc.exe> --execute for admission-managed KRE.')
        return
    assert args.engine and args.engine.is_file(),'Supply an existing licensed KRE executable with --engine'
    if args.properties_file:assert args.properties_file.is_file()
    with concurrent.futures.ThreadPoolExecutor(max_workers=limit) as pool:
        codes=list(pool.map(lambda w:execute(w,args.engine.resolve(),limit,args.properties_file),workers))
    raise SystemExit(max(codes,default=0))

if __name__=='__main__':main()
