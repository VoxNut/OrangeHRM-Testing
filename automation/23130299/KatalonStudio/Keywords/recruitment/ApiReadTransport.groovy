package recruitment

import org.openqa.selenium.*
import java.time.Duration

/** Read-only authenticated transport. Route contract is captured in pages/current-page.html
 * and api.service.ts: /web/index.php is the Axios base, /api/v2/... its relative path.
 * Never depends on page bootstrap globals (navigation/new tabs may not have them).
 */
class ApiReadTransport {
    static final Set<String> ENDPOINTS=['/api/v2/pim/employees','/api/v2/recruitment/interviewers'] as Set
    static String resolve(String currentUrl,String endpoint,String configuredBase=RecruitmentUi.BASE) {
        try {
            URI current=new URI(currentUrl),configured=new URI(configuredBase)
            if (!ENDPOINTS.contains(endpoint) || current.scheme!='https' || current.userInfo || configured.userInfo ||
                current.scheme!=configured.scheme || current.host!=configured.host || current.port!=configured.port ||
                configured.rawQuery || configured.rawFragment || !(configured.path in ['', '/', '/web/index.php']))
                throw new IllegalArgumentException('Untrusted origin/base/endpoint')
            if (current.path.startsWith('/web/index.php/auth/'))
                throw new PimEmployeeFixtures.Fault('SESSION_EXPIRED','Current document is the login route; no API read')
            if (!knownDocumentPath(current.path))
                throw new IllegalArgumentException('Current document has no verified OrangeHRM route context')
            new URI(current.scheme,null,current.host,current.port,'/web/index.php'+endpoint,null,null).toASCIIString()
        } catch(PimEmployeeFixtures.Fault error) {throw error}
        catch(Exception error) {throw new PimEmployeeFixtures.Fault('PIM_API_BASE_URL_MISSING_OR_INVALID','Cannot resolve same-origin API URL from current OrangeHRM route',error)}
    }
    static boolean knownDocumentPath(String path) {
        path in ['/web/index.php/dashboard/index','/web/index.php/recruitment/viewRecruitmentModule',
            '/web/index.php/recruitment/viewCandidates','/web/index.php/recruitment/viewJobVacancy',
            '/web/index.php/recruitment/addCandidate','/web/index.php/recruitment/addJobVacancy',
            '/web/index.php/recruitment/changeCandidateVacancyStatus','/web/index.php/pim/viewEmployeeList',
            '/web/index.php/pim/addEmployee'] ||
            path==~ /^\/web\/index\.php\/recruitment\/(addCandidate|addJobVacancy)\/[1-9]\d*$/ ||
            path==~ /^\/web\/index\.php\/recruitment\/candidateHistory\/[1-9]\d*\/[1-9]\d*$/ ||
            path==~ /^\/web\/index\.php\/pim\/viewPersonalDetails\/empNumber\/[1-9]\d*$/
    }
    static final String SCRIPT='''
        const absolute=arguments[0],params=arguments[1],requireTotal=arguments[2],done=arguments[arguments.length-1];
        let finished=false,timer,abort;
        const finish=value=>{if(finished)return;finished=true;if(timer!==undefined)clearTimeout(timer);done(value);};
        (async()=>{
          try {
            const url=new URL(absolute);
            if(url.origin!==location.origin||url.username||url.password){
              finish({error:'ORIGIN',documentOrigin:location.origin,documentPath:location.pathname,
                detail:'Endpoint origin differs from effective document origin; WebDriver URL alone is insufficient'});return;
            }
            if(!/^\\/web\\/index\\.php\\/api\\/v2\\/(pim\\/employees|recruitment\\/interviewers)$/.test(url.pathname)){
              finish({error:'BASE_URL'});return;
            }
            for(const [k,v] of Object.entries(params))if(v!==null&&v!==undefined)url.searchParams.set(k,String(v));
            abort=new AbortController();
            timer=setTimeout(()=>{abort.abort();finish({error:'READ_TIMEOUT',errorType:'AbortError'});},15000);
            const r=await fetch(url,{method:'GET',credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'},signal:abort.signal});
            if(r.redirected||r.status===401){finish({status:401,error:'SESSION_EXPIRED'});return;}
            if(!r.ok){finish({status:r.status,error:'HTTP_'+r.status});return;}
            let body;
            try{body=await r.json();}catch(e){finish({status:r.status,error:'JSON_PARSE',errorType:e.name});return;}
            if(!body||!Array.isArray(body.data)||(requireTotal&&(!Number.isInteger(body.meta?.total)||body.meta.total<0))){
              finish({status:r.status,error:'RESPONSE_SCHEMA'});return;
            }
            const valid=e=>e&&Number.isSafeInteger(e.empNumber)&&e.empNumber>0&&typeof e.firstName==='string'&&
              typeof e.lastName==='string'&&Object.prototype.hasOwnProperty.call(e,'terminationId')&&
              (e.terminationId===null||(Number.isSafeInteger(e.terminationId)&&e.terminationId>0))&&
              (e.middleName==null||typeof e.middleName==='string');
            if(!body.data.every(valid)){finish({status:r.status,error:'RESPONSE_SCHEMA'});return;}
            finish({status:r.status,total:body.meta?.total,data:body.data.map(e=>({empNumber:e.empNumber,
              firstName:e.firstName,middleName:e.middleName,lastName:e.lastName,employeeId:e.employeeId,terminationId:e.terminationId}))});
          }catch(e){finish({error:e.name==='AbortError'?'READ_TIMEOUT':'TRANSPORT',errorType:e.name,detail:e.message});}
        })();
    '''
    static Map request(WebDriver driver,String endpoint,Map params,boolean requireTotal=true) {
        ExecutionContext c=ExecutionContext.peek()
        if(c?.pimReadFailure)throw c.pimReadFailure
        try {return requestInternal(driver,endpoint,params,requireTotal)}
        catch(PimEmployeeFixtures.Fault error) {
            // A denied/failed read never becomes missing identity. Recovery requires a
            // new operator-started execution, never a speculative retry or employee Save.
            if(c) {
                c.pimReadFailure=error
                c.pimFixtures.readFailure=[state:error.state,errorType:error.class.name,
                    rootExceptionType:(error.cause ?: error).class.name,detail:error.message]
                c.persist()
            }
            throw error
        }
    }
    private static Map requestInternal(WebDriver driver,String endpoint,Map params,boolean requireTotal) {
        String url=resolve(driver.currentUrl,endpoint)
        Duration original=driver.manage().timeouts().scriptTimeout
        Map response
        Throwable primary=null
        try {
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20))
            response=(Map)((JavascriptExecutor)driver).executeAsyncScript(SCRIPT,url,params,requireTotal)
        } catch(WebDriverException error) {
            primary=new PimEmployeeFixtures.Fault('PIM_API_TRANSPORT_ERROR','Browser script/transport failed; no provisioning fallback',error)
            throw primary
        } finally {
            try {driver.manage().timeouts().scriptTimeout(original)}
            catch(WebDriverException restore) {if(primary)primary.addSuppressed(restore);else throw new PimEmployeeFixtures.Fault('PIM_API_TRANSPORT_ERROR','Cannot restore Selenium script timeout',restore)}
        }
        validate(response,requireTotal)
    }
    static Map validate(Map response,boolean requireTotal=true) {
        try {return validateInternal(response,requireTotal)}
        catch(PimEmployeeFixtures.Fault error){error.diagnostic=[status:response?.status,error:response?.error,errorType:response?.errorType,
            documentOrigin:response?.documentOrigin,documentPath:response?.documentPath];throw error}
    }
    private static Map validateInternal(Map response,boolean requireTotal) {
        if(response?.status==401) {SessionGuard.failedLogin();throw new PimEmployeeFixtures.Fault('SESSION_EXPIRED','PIM/Recruitment GET HTTP 401 or authentication redirect')}
        if(response?.status==403)throw new PimEmployeeFixtures.Fault('PIM_PERMISSION_DENIED','PIM/Recruitment GET HTTP 403; no write')
        if(response?.status && response.status!=200)throw new PimEmployeeFixtures.Fault('PIM_API_HTTP_ERROR','PIM/Recruitment GET HTTP '+response.status+'; no write')
        String code=response?.error?.toString()
        if(code || !response || !(response.data instanceof List) || (requireTotal&&(!(response.total instanceof Number)||response.total<0))) {
            String state=code in ['ORIGIN','BASE_URL']?'PIM_API_BASE_URL_MISSING_OR_INVALID':code=='JSON_PARSE'?'PIM_API_JSON_PARSE_ERROR':
                code=='READ_TIMEOUT'?'PIM_API_TIMEOUT':code=='TRANSPORT'?'PIM_API_TRANSPORT_ERROR':'PIM_API_RESPONSE_SCHEMA_INVALID'
            throw new PimEmployeeFixtures.Fault(state,'Read failed: '+(code ?: 'RESPONSE_SCHEMA')+'; rootType='+(response?.errorType ?: 'ResponseValidation')+'; '+ExecutionReport.clean(response?.detail ?: '')+'; no write')
        }
        response
    }
}
