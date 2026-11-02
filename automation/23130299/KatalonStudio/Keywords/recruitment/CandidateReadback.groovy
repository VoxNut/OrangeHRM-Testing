package recruitment

import org.openqa.selenium.*
import java.time.Duration

/** Exact GET reconciliation only. Separate schema from the unchanged PIM transport.
 * Source contracts: ViewCandidateProfile.onCandidateUpdate, VacancyDropdown.setup.
 */
class CandidateReadback {
    final ExecutionContext context
    CandidateReadback(ExecutionContext c=ExecutionContext.current()){context=c}
    protected WebDriver browser(){ExecutionBrowser.driver()}
    static final String SCRIPT='''
        const absolute=arguments[0],params=arguments[1],done=arguments[arguments.length-1];
        let finished=false,timer,abort;const finish=x=>{if(finished)return;finished=true;clearTimeout(timer);done(x);};
        (async()=>{try{
          const u=new URL(absolute);
          if(u.origin!==location.origin||u.username||u.password||
             !/^\\/web\\/index\\.php\\/api\\/v2\\/recruitment\\/(candidates\\/[1-9][0-9]*|vacancies)$/.test(u.pathname)){
            finish({error:'ORIGIN_OR_ENDPOINT'});return;
          }
          for(const [k,v] of Object.entries(params))if(v!==null&&v!==undefined)u.searchParams.set(k,String(v));
          abort=new AbortController();timer=setTimeout(()=>{abort.abort();finish({error:'TIMEOUT'});},15000);
          const r=await fetch(u,{method:'GET',credentials:'same-origin',redirect:'error',cache:'no-store',headers:{Accept:'application/json'},signal:abort.signal});
          if(!r.ok){finish({status:r.status,error:'HTTP'});return;}
          let body;try{body=await r.json();}catch(e){finish({status:r.status,error:'JSON_PARSE'});return;}
          finish({status:r.status,data:body?.data});
        }catch(e){finish({error:e.name==='AbortError'?'TIMEOUT':'TRANSPORT',errorType:e.name});}})();
    '''
    static String endpoint(String path){
        assert path=='/api/v2/recruitment/vacancies' || path==~ /^\/api\/v2\/recruitment\/candidates\/[1-9]\d*$/ : 'Read endpoint not allowed'
        path
    }
    protected Map read(String path,Map params=[:]){
        WebDriver d=browser()
        assert context.ownedDriver?.is(d) && context.browserSessionId==ExecutionBrowser.sessionId(d) : 'Readback driver/session ownership changed'
        assert d.windowHandles.contains(d.windowHandle) : 'Readback window unavailable'
        Map doc=CandidateNavigationRecovery.inspect(d)
        if(!CandidateNavigationRecovery.application(doc))throw new PimEmployeeFixtures.Fault('CANDIDATE_DOCUMENT_CONTEXT_INVALID','Readback requires a real mounted authenticated same-origin document')
        // Reuse the trusted route/origin resolver, never window.appGlobal or document.baseURI.
        String base=ApiReadTransport.resolve(d.currentUrl,'/api/v2/pim/employees')
        String url=base.substring(0,base.indexOf('/api/v2/'))+endpoint(path)
        Duration previous=d.manage().timeouts().scriptTimeout
        Map result;Throwable primary=null
        try {
            d.manage().timeouts().scriptTimeout(Duration.ofSeconds(20))
            result=(Map)((JavascriptExecutor)d).executeAsyncScript(SCRIPT,url,params)
        } catch(WebDriverException e){primary=e;throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_TRANSPORT_ERROR','Exact GET transport failed; no write replay',e)}
        finally {try{d.manage().timeouts().scriptTimeout(previous)}catch(WebDriverException e){if(primary)primary.addSuppressed(e);else throw e}}
        validate(result)
    }
    static Map validate(Map r){
        if(r?.status==401)throw new PimEmployeeFixtures.Fault('SESSION_EXPIRED','Candidate/Vacancy GET HTTP401')
        if(r?.status==403)throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_PERMISSION_DENIED','Candidate/Vacancy GET HTTP403')
        if(r?.error || r?.status!=200)throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_'+(r?.error ?: 'INVALID_RESPONSE'),'Read-only request failed; status='+r?.status+'; error='+r?.error+'; no Save replay')
        r
    }
    static Map expected(Map d){
        Map fields=[:]
        ['first','middle','last','email','phone','keywords','notes','applicationIso','vacancy','consent'].each{k->if(d.containsKey(k))fields[k]=d[k]}
        fields+[url:d.url,candidateId:d.candidateId,vacancyUrl:d.vacancyUrl,ownership:d.ownership]
    }
    static Map compare(Map d,Map row){
        assert CandidateChecks.candidateId(d.url?.toString())==d.candidateId?.toString() && d.candidateId : 'Original numeric Candidate ID required'
        if(!(row?.id instanceof Number) || row.id.toString()!=d.candidateId.toString())
            throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_ID_MISMATCH','Exact Candidate GET returned a different/missing numeric ID')
        Map mapping=[first:'firstName',middle:'middleName',last:'lastName',email:'email',phone:'contactNumber',keywords:'keywords',notes:'comment',applicationIso:'dateOfApplication',consent:'consentToKeepData']
        Map actual=[:]
        mapping.each{k,v->if(row.containsKey(v))actual[k]=row[v]==null && k in ['middle','phone','keywords','notes'] ? '' : row[v]}
        if(row.containsKey('vacancy'))actual.vacancy=row.vacancy?.name ?: ''
        Map comparison=CandidateChecks.candidateComparison(d,actual)
        if(comparison.missingFields || comparison.mismatchedFields)
            throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_FIELDS_MISMATCH','Fresh exact GET fields differ: '+comparison)
        if(d.containsKey('consent') && row.consentToKeepData!=d.consent)
            throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_FIELDS_MISMATCH','Fresh exact GET consent differs/missing')
        if(d.vacancy){
            assert CandidateChecks.vacancyRecordUrl(d.vacancyUrl?.toString()) : 'Original numeric Vacancy URL required'
            String vid=new URI(d.vacancyUrl.toString()).path.tokenize('/')[-1]
            if(!(row.vacancy?.id instanceof Number) || row.vacancy.id.toString()!=vid || row.vacancy.name!=d.vacancy)
                throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_VACANCY_MISMATCH','Candidate association differs from exact owned numeric Vacancy')
            if(row.vacancy.status!=true)
                throw new PimEmployeeFixtures.Fault('RECOVERY_OWNED_DEPENDENCY_CHANGED','Original active owned Vacancy is closed or status unverified; no replacement')
        } else if(row.vacancy!=null)throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_VACANCY_MISMATCH','Unexpected Vacancy association')
        comparison+[candidateId:d.candidateId,vacancyId:row.vacancy?.id,persistedReadback:true]
    }
    Map candidate(Map d){
        assert d.ownership && d.last?.toString()?.contains(d.ownership.toString()) : 'Owned fixture marker required'
        Map r=read('/api/v2/recruitment/candidates/'+d.candidateId)
        if(!(r.data instanceof Map))throw new PimEmployeeFixtures.Fault('CANDIDATE_READ_SCHEMA_INVALID','Candidate GET requires data object')
        compare(d,r.data as Map)
    }
    Map vacancyOptions(boolean add){
        Map params=[model:'summary',limit:0,excludeInterviewers:add];if(add)params.status=true
        Map r=read('/api/v2/recruitment/vacancies',params)
        if(!(r.data instanceof List) || !r.data.every{it instanceof Map && it.id instanceof Number && it.name instanceof String && it.status instanceof Boolean})
            throw new PimEmployeeFixtures.Fault('VACANCY_OPTIONS_SCHEMA_INVALID','Summary requires data array of numeric id/name/status')
        [params:params,status:r.status,data:r.data]
    }
    void pending(){
        Map pending=context.authentication.candidatePending as Map
        if(!pending || pending.persistedReadback==true)return
        if(!pending.candidateId)throw new PimEmployeeFixtures.Fault('RECOVERY_UNCERTAIN_SAVE','Original Save has no numeric ID; no cross-case write admission')
        Map proof=candidate(pending)
        pending.persistedReadback=true;context.persist()
        new File(context.root,'logs/candidate-readback.jsonl').append(new com.google.gson.Gson().toJson(
            [event:'PENDING_WRITE_RECONCILED',originalCase:pending.caseId,executionId:context.executionId,
             currentCase:context.caseId,noSubmitReplay:true,uiAssertionsComplete:false]+proof)+'\n','UTF-8')
    }
}
