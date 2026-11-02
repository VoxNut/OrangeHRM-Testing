import recruitment.*
import org.openqa.selenium.*
import java.lang.reflect.Proxy

// Uses the compiled production fillInterview/buttonClick/click and XPath, with native element doubles.
class LatestInterviewProbe extends WorkflowChecks {
    int fields=1,clicks=0,saveCalls=0
    List selected=[],locators=[]
    boolean ambiguous=false,disabled=false,removed=false,staleDispatch=false
    WebDriver ownedDouble=Proxy.newProxyInstance(LatestInterviewProbe.classLoader,[WebDriver] as Class[],{o,m,a->
        if(m.name=='toString')return 'Offline native control driver'
        if(m.name=='getCurrentUrl')return ROOT+'changeCandidateVacancyStatus?candidateId=94&selectedAction=4'
        throw new AssertionError('Forbidden driver operation '+m.name)
    } as java.lang.reflect.InvocationHandler) as WebDriver
    LatestInterviewProbe(){super('RECRUIT_FLOW_SelectInterviewer_02');seconds=0}
    WebDriver driver(){ownedDouble}
    void replace(String path,String value){}
    String enterDate(String label,String value){value}
    void setTime(String value,boolean invalid=false){}
    boolean shown(String path){assert path==LOADER;false}
    void capture(String label){throw new AssertionError('No screenshot/browser expected for Add Another')}
    List<WebElement> visible(String path){
        if(path.contains('orangehrm-recruitment-interviewer-input'))return (1..fields).collect{element(false)}
        locators<<path
        assert path==RecruitmentUi.button('Add Another') : 'Wrong source-shaped native button locator: '+path
        if(removed)return []
        ambiguous ? [element(true),element(true)] : [element(true)]
    }
    WebElement element(boolean add){
        Proxy.newProxyInstance(getClass().classLoader,[WebElement] as Class[],{o,m,a->
            switch(m.name){
                case 'isEnabled': return !disabled
                case 'getAttribute': assert a[0]=='aria-disabled';return disabled?'true':null
                case 'click':
                    assert add;clicks++
                    if(staleDispatch)throw new StaleElementReferenceException('Uncertain native click; never replay')
                    fields++;return null
                default:throw new AssertionError('Forbidden element method '+m.name)
            }
        } as java.lang.reflect.InvocationHandler) as WebElement
    }
    String selectInterviewer(int index,String name){
        assert index<fields && name in ['Interviewer Alpha','Interviewer Beta']
        selected<<[index:index,name:name,id:index==0?'86':'87'];name
    }
}
int groups=0
def check={String name,Closure task->task();println('LATEST FLOW PASS '+(++groups)+': '+name)}
def rejects={Closure task->boolean caught=false;try{task()}catch(AssertionError|WebDriverException expected){caught=true};assert caught}
def data={List names->[title:'Owned',interviewers:names,date:'',time:'10:30',notes:'']}
ExecutionContext c=ExecutionContext.current();assert c.ownedDriver==null
check('two native interviewer fields use exact Add Another spelling once'){
    def ui=new LatestInterviewProbe();ui.fillInterview(data(['Interviewer Alpha','Interviewer Beta']))
    assert ui.fields==2 && ui.clicks==1 && ui.selected*.id==['86','87'] && ui.saveCalls==0
}
check('one interviewer canary never enters changed branch'){
    def ui=new LatestInterviewProbe();ui.fillInterview(data(['Interviewer Alpha']))
    assert ui.clicks==0 && ui.selected*.id==['86']
}
check('zero interviewer negative path never adds a field'){
    def ui=new LatestInterviewProbe();ui.fillInterview(data([]));assert ui.clicks==0 && ui.selected.empty
}
check('duplicate employee labels remain rejected before any native addition'){
    def ui=new LatestInterviewProbe();rejects{ui.fillInterview(data(['Interviewer Alpha','Interviewer Alpha']))};assert ui.clicks==0
}
check('more than five entries remain rejected'){
    def ui=new LatestInterviewProbe();rejects{ui.fillInterview(data(['a','b','c','d','e','f']))};assert ui.clicks==0
}
check('ambiguous Add Another native controls fail closed'){
    def ui=new LatestInterviewProbe();ui.ambiguous=true;rejects{ui.fillInterview(data(['Interviewer Alpha','Interviewer Beta']))};assert ui.clicks==0
}
check('disabled Add Another never dispatches'){
    def ui=new LatestInterviewProbe();ui.disabled=true;rejects{ui.fillInterview(data(['Interviewer Alpha','Interviewer Beta']))};assert ui.clicks==0
}
check('missing Add Another remains ERROR; never Save'){
    def ui=new LatestInterviewProbe();ui.removed=true;rejects{ui.fillInterview(data(['Interviewer Alpha','Interviewer Beta']))};assert ui.clicks==0 && ui.saveCalls==0
}
check('stale uncertain click dispatch is not replayed'){
    def ui=new LatestInterviewProbe();ui.staleDispatch=true;rejects{ui.fillInterview(data(['Interviewer Alpha','Interviewer Beta']))};assert ui.clicks==1 && ui.fields==1
}
println('LATEST FLOW OFFLINE PASS '+groups+'; actual production methods with native boundary doubles only')
evaluate(new File('maintenance/asm8-full-audit-20261102/check_latest_portability.groovy'))
