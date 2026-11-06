package recruitment

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class RecorderBridge {
    String caseId
    Map<String,String> vars = [:]
    int seconds = 30
    boolean insideIf = false
    boolean condition = false
    boolean isElse = false
    int shotCounter = 0
    String selectOwner = ''
    final String runStamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss_SSS'))
    final ExecutionContext executionContext = ExecutionContext.current()
    final File output
    final List<Map> ledger=[]
    final String namespaceToken=executionContext.tokenFor('vacancy-helper')
    static final String BASE_URL = 'https://opensource-demo.orangehrmlive.com'
    private int performanceSequence = 0
    private final String performanceScope = 'recorder:'+UUID.randomUUID().toString()
    private final List<String> performanceStack = []

    Object measure(String phase, Closure action) {
        long start = System.nanoTime(), epoch = System.currentTimeMillis()
        String id = performanceScope+':'+(++performanceSequence)
        String parent = performanceStack ? performanceStack[-1] : null
        performanceStack << id
        String outcome = 'OK'
        try { return action.call() }
        catch (Throwable failure) { outcome = failure instanceof TimeoutException ? 'TIMEOUT' : 'ERROR'; throw failure }
        finally {
            performanceStack.removeAt(performanceStack.size()-1)
            Map row = [caseId:caseId,phase:phase,operationId:id,parentId:parent,instrumentationScope:performanceScope,startEpochMs:epoch,
                elapsedMs:(System.nanoTime()-start).intdiv(1000000L),outcome:outcome]
            String json = new com.google.gson.Gson().toJson(row)
            println('[PERFORMANCE] '+json)
            try { new File(output,'performance.jsonl').append(json+'\n','UTF-8') }
            catch (IOException unavailable) { println('[PERFORMANCE LOG UNAVAILABLE] '+unavailable.class.simpleName) }
        }
    }
    WebDriverWait waitFor(int timeout = seconds) {
        new WebDriverWait(driver(),Duration.ofSeconds(timeout)).pollingEvery(Duration.ofMillis(200))
            .ignoring(StaleElementReferenceException)
    }
    boolean loginVisible() {
        driver().currentUrl.contains('/auth/') || !driver().findElements(By.xpath("//input[@name='username']")).empty
    }
    boolean vacancyNavigationReady(String url) {
        if (!RecruitmentUi.sameRoute(driver().currentUrl,url) || loginVisible() || driver().findElements(By.xpath('//aside//nav')).empty) return false
        if (driver().findElements(By.xpath(VacancyChecks.LOADING+" | //div[contains(@class,'oxd-form-loader')]")).any { it.displayed }) return false
        if (url.contains('/recruitment/addJobVacancy')) return waitForVacancyFormReady(url)
        if (url.endsWith('/recruitment/viewJobVacancy')) return VacancyTable.state(driver()).ready == true
        return !driver().findElements(By.xpath('//header//h6')).empty
    }
    boolean waitForVacancyFormReady(String url) {
        String title = url ==~ /.*\/addJobVacancy\/[1-9]\d*$/ ? 'Edit Vacancy' : 'Add Vacancy'
        return !driver().findElements(By.xpath("//h6[normalize-space()='${title}']")).empty &&
            ['Vacancy Name','Hiring Manager','Number of Positions'].every { label ->
                List<WebElement> fields=driver().findElements(By.xpath(VacancyChecks.group(label)+'//input'))
                fields.size()==1 && fields[0].displayed && fields[0].enabled
            } && (title != 'Edit Vacancy' || driver().findElement(By.xpath(VacancyChecks.group('Vacancy Name')+'//input')).getDomProperty('value'))
    }

    RecorderBridge(String caseId) { ConfigurationPreflight.guard(caseId); this.caseId = caseId; output=executionContext.caseDirectory(caseId,namespaceToken) }

    String expand(String str) {
        if (!str) return ''
        return str.replaceAll(/\$\{([^}]+)\}/) { all, key ->
            if (!vars.containsKey(key)) throw new AssertionError('Chua khai bao bien: ' + key + ' in ' + caseId)
            return vars[key] ?: ''
        }
    }
    TestObject obj(String loc) {
        String s = scopedLocator(expand(loc))
        TestObject o = new TestObject('dynamic_' + caseId)
        if (s.startsWith('xpath=')) o.addProperty('xpath', ConditionType.EQUALS, s.substring(6))
        else if (s.startsWith('css=')) o.addProperty('css', ConditionType.EQUALS, s.substring(4))
        else throw new IllegalArgumentException('Locator chua ho tro: ' + s)
        return o
    }
    String scopedLocator(String loc) {
        if (loc.startsWith('xpath=//div') && loc.contains('oxd-select-option')) {
            assert selectOwner : 'Open a label-scoped select before locating its options'
            return 'xpath='+VacancyChecks.group(selectOwner)+loc.substring(6)
        }
        return loc
    }
    String xp(String loc) { return scopedLocator(expand(loc)).replaceFirst(/^xpath=/, '') }
    WebDriver driver() {
        assert ExecutionContext.peek().is(executionContext) : 'Recorder belongs to a different execution'
        return ExecutionBrowser.driver()
    }
    boolean hasBrowser() {
        try { WebDriver d = driver(); return d != null && d.getWindowHandle() != null }
        catch (Throwable ignored) { return false }
    }
    void ensureBrowser() {
        SessionGuard.requireAllowance()
        ExecutionLease.acquire()
        if (!hasBrowser()) {
            if (executionContext.ownedDriver) SessionGuard.lost('Owned Chrome session unavailable')
            measure('BROWSER_START') { ExecutionBrowser.launch { WebUI.openBrowser(''); WebUI.maximizeWindow() } }
        }
    }
    void go(String url) {
        measure('NAVIGATION') { goInternal(url) }
    }
    protected void goInternal(String url) {
        SessionGuard.requireAllowance()
        ensureBrowser()
        String resolved = expand(url)
        if (resolved.startsWith('/')) resolved = BASE_URL + resolved
        WebUI.navigateToUrl(resolved)
        waitFor().until { loginVisible() || vacancyNavigationReady(resolved) }
        if (loginVisible()) {
            SessionGuard.requireInitialLogin()
            println('[SETUP] Vui long DANG NHAP tai Chrome Katalon mo trong 120 giay. Khong luu password vao project.')
            try { measure('LOGIN_WAIT') { waitFor(120).until { !loginVisible() && !driver().findElements(By.xpath('//aside//nav')).empty } } }
            catch (TimeoutException timeout) { SessionGuard.failedLogin(); throw timeout }
            SessionGuard.requireAllowance()
            WebUI.navigateToUrl(resolved)
            waitFor().until { if(loginVisible()) SessionGuard.lost('Navigation returned to Login'); vacancyNavigationReady(resolved) }
        }
        if (!driver().findElements(By.xpath('//aside//nav')).empty) SessionGuard.authenticated()
    }
    String screenshotDirectory() {
        String raw = ''
        try { raw = String.valueOf(GlobalVariable.evidenceDir ?: '') }
        catch (Throwable ignored) {}
        File dst = raw.trim() ? PortablePaths.resolve(raw,'evidence/screenshots') : executionContext.evidenceRoot
        dst.mkdirs()
        return dst.absolutePath
    }
    void screenshot(String label) {
        measure('SCREENSHOT') { EvidenceCapture.capture(label) }
    }

    void countCheckpoint(String label) { shotCounter++; screenshot(label) }
    void journal(Map event) {
        ledger << (event + executionContext.owner(caseId))
        AtomicFiles.json(new File(output,'fixtures.json'),ledger)
    }
    boolean selected(String loc) {
        return driver().findElement(By.xpath(xp(loc))).isSelected()
    }
    void desiredState(String loc, boolean wanted) {
        boolean now = selected(loc)
        if (now != wanted) {
            WebElement el = driver().findElement(By.xpath(xp(loc)))
            WebElement label = el.findElement(By.xpath('ancestor::label[1]'))
            label.click()
            new org.openqa.selenium.support.ui.WebDriverWait(driver(), java.time.Duration.ofSeconds(seconds)).until { selected(loc) == wanted }

        }
        assert selected(loc) == wanted : 'Checkbox cannot be set to ' + wanted + ': ' + loc
    }
    void waitValue(String loc, String expected, boolean textMode=false) {
        String actual = ''
        waitFor().until {
            List<WebElement> fields = driver().findElements(By.xpath(xp(loc)))
            assert fields.size() <= 1 : 'Ambiguous value locator: '+loc
            if (!fields) return false
            actual = textMode ? fields[0].text : fields[0].getDomProperty('value')
            // OXD employee labels may contain a blank middle name. Other input values stay exact.
            boolean displayName = loc.contains('Hiring Manager')
            return textMode || displayName ? VacancyChecks.normalizeManagerName(actual) == VacancyChecks.normalizeManagerName(expected) : (actual ?: '') == expected
        }
    }
    int xpathCount(String loc) { return driver().findElements(By.xpath(xp(loc))).size() }
    void waitCount(String loc, int expected) {
        waitFor().until { xpathCount(loc) == expected }
    }
    void waitForNavigationOutcome(String beforeUrl, String action) {
        measure(action == 'Save' ? 'SAVE' : 'NAVIGATION_OUTCOME') {
            waitFor().until {
                if (loginVisible()) SessionGuard.lost(action+' outcome lost authentication; never replay Save')
                if (driver().findElements(By.xpath(VacancyChecks.LOADING+" | //div[contains(@class,'oxd-form-loader')]")).any { it.displayed }) return false
                if (driver().currentUrl != beforeUrl) return vacancyNavigationReady(driver().currentUrl)
                if (action != 'Save') return false
                return !driver().findElements(By.xpath("//form//*[contains(@class,'oxd-input-field-error-message')] | //div[contains(@class,'oxd-toast')]")).findAll { it.displayed }.empty
            }
        }
    }
    // Only support the comparison expressions that occur in this particular .side project.
    boolean evalCondition(String input) {
        String e=expand(input).trim()
        def n=(e =~ /^Number\("([^"]+)"\)\s*(===|==|>=|<=|>|<)\s*(?:Number\("([^"]+)"\)|(-?\d+))$/)
        if (n.matches()) {
            double a=Double.parseDouble(n[0][1]); String op=n[0][2]
            double b=Double.parseDouble(n[0][3] ?: n[0][4])
            switch(op) {
                case '===': case '==': return a==b
                case '>': return a>b
                case '<': return a<b
                case '>=': return a>=b
                case '<=': return a<=b
            }
        }
        def s=(e =~ /^'([^']*)'\s*(===|!==|!=|==)\s*'([^']*)'$/)
        if (s.matches()) {
            boolean same = s[0][1] == s[0][3]
            return s[0][2] in ['===','=='] ? same : !same
        }
        throw new IllegalArgumentException('Unsupported evaluation: ' + e)
    }
    void step(String operation, String rawTarget='', String rawValue='') {
        if (operation=='if') { condition=evalCondition(rawTarget); insideIf=true; isElse=false; return }
        if (operation=='else') { isElse=true; return }
        if (operation=='endIf') { insideIf=false; isElse=false; return }
        if (insideIf && (isElse ? condition : !condition)) return
        String target=expand(rawTarget), value=expand(rawValue)
        println('[STEP] '+caseId+' : '+operation+' | '+target.take(180)+' | '+value.take(100))
        switch(operation) {
            case 'setTimeout': seconds = Math.max(10, (Integer.parseInt(target)/1000) as int); break
            case 'echo': println('[INFO] '+target); break
            case 'store': vars[value] = target; break
            case 'storeEval':
                String e=target
                // The source tests only concatenate literal fragments with new Date().getTime().
                if (!e.contains('new Date().getTime()')) throw new IllegalArgumentException('Unsupported storeEval: '+e)
                vars[value]=e.replace('new Date().getTime()', namespaceToken+'_'+(++executionContext.sequence)).replaceAll(/['+]/, '')
                assert vars[value].length()<=50 : 'Generated Vacancy name exceeds source validator length50'
                break
            case 'open': go(target); break
            case 'waitForElementPresent':
                assert WebUI.waitForElementPresent(obj(rawTarget),seconds) : 'Missing '+target;break
            case 'assertElementPresent':
                assert WebUI.verifyElementPresent(obj(rawTarget),seconds) : 'Missing '+target;break
            case 'assertElementNotPresent':
                assert WebUI.verifyElementNotPresent(obj(rawTarget),2) : 'Unexpected element '+target;break
            case 'click':
                String beforeClickUrl = driver().currentUrl
                if (target.contains('oxd-select-text-input')) {
                    WebElement control=driver().findElement(By.xpath(xp(rawTarget)))
                    List<WebElement> labels=control.findElements(By.xpath("ancestor::div["+VacancyChecks.token('oxd-input-group')+"][1]//label"))
                    assert labels.size()==1 && labels[0].text.trim() : 'Select must belong to exactly one labeled group'
                    selectOwner=labels[0].text.trim()
                    VacancyTable.click(driver(),control,seconds)
                    break
                }
                if (target.contains('oxd-select-option')) {
                    List<WebElement> options=driver().findElements(By.xpath(xp(rawTarget))).findAll { it.displayed && it.enabled }
                    assert options.size()==1 : 'Exact option missing or ambiguous inside '+selectOwner
                    String expected=options[0].text.trim()
                    VacancyTable.click(driver(),options[0],seconds)
                    waitValue(VacancyChecks.select(selectOwner),expected,true)
                    break
                }
                if (target.contains('Save') || target.contains('Search') || target.contains('Reset') || target.contains('Cancel')) screenshot(caseId+'_BEFORE_ACTION.png')
                if (target.contains('Save') && target.contains('button')) {
                    Map attempt=[attemptId:UUID.randomUUID().toString(),name:vars['vacancyName'],url:RecruitmentUi.safeUrl(driver().currentUrl),
                        state:'submitted',submitted:false,observed:false,verified:false,uncertain:false]
                    journal(new LinkedHashMap(attempt))
                    try {
                        driver().findElement(By.xpath(xp(rawTarget))).click() // exactly one native submit; no keyword fallback/retry
                        attempt.submitted=true;journal(new LinkedHashMap(attempt))
                    } catch(Throwable error) {
                        attempt.state='uncertain';attempt.uncertain=true;journal(new LinkedHashMap(attempt));throw error
                    }
                } else if (target.contains('button') && (target.contains('Search') || target.contains('Reset'))) {
                    WebElement action = waitFor().until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(By.xpath(xp(rawTarget))))
                    measure(target.contains('Reset') ? 'RESET' : 'SEARCH') { VacancyTable.refresh(driver(),action,seconds) }
                } else if (target.contains("@type='checkbox'") || target.contains('@type="checkbox"')) {
                    WebElement el=driver().findElement(By.xpath(xp(rawTarget)))
                    el.findElement(By.xpath('ancestor::label[1]')).click()
                } else {
                    WebUI.click(obj(rawTarget))
                }
                if (target.contains('Save') || target.contains('Search') || target.contains('Reset') || target.contains('Cancel')) {
                    if (!(target.contains('Search') || target.contains('Reset'))) waitForNavigationOutcome(beforeClickUrl,target.contains('Save') ? 'Save' : 'Cancel')
                    screenshot(caseId+'_'+String.format('%03d',++shotCounter)+'_AFTER_ACTION.png')
                }
                break
            case 'type': WebUI.setText(obj(rawTarget),value); break
            case 'assertValue':
                String real=WebUI.getAttribute(obj(rawTarget),'value') ?: ''
                assert real==value : 'Value mismatch expected ['+value+'] actual ['+real+']';break
            case 'assertText':
                String real=(WebUI.getText(obj(rawTarget)) ?: '').trim().replaceAll(/\s+/, ' ')
                assert real==value.trim().replaceAll(/\s+/, ' ') : 'Text mismatch expected ['+value+'] actual ['+real+']';break
            case 'waitForText': waitValue(rawTarget,value,true); break
            case 'waitForValue': waitValue(rawTarget,value,false); break
            case 'storeText': vars[value]=WebUI.getText(obj(rawTarget)).trim();break
            case 'storeValue': vars[value]=WebUI.getAttribute(obj(rawTarget),'value') ?: '';break
            case 'storeLocation':
                vars[target]=driver().getCurrentUrl()
                def identity=(vars[target] =~ /\/addJobVacancy\/([1-9]\d*)$/)
                if (identity.find()) {
                    journal([name:vars['vacancyName'],numericId:identity.group(1),url:RecruitmentUi.safeUrl(vars[target]),
                        state:'observed',submitted:true,observed:true,verified:false,uncertain:false])
                    if(VacancyChecks.ownedFixtureName(vars['vacancyName'],namespaceToken))
                        PimEmployeeFixtures.dependency('vacancy',identity.group(1),executionContext.managerDiscovery.lastSelection?.employeeId?.toString())
                }
                break
            case 'assertChecked': assert selected(rawTarget) : 'Expected checked '+target;break
            case 'assertNotChecked': assert !selected(rawTarget) : 'Expected unchecked '+target;break
            case 'ensureChecked': desiredState(rawTarget,true);break
            case 'ensureUnchecked': desiredState(rawTarget,false);break
            case 'captureEntirePageScreenshot': countCheckpoint(target);break
            case 'storeXpathCount': vars[value]=String.valueOf(xpathCount(rawTarget));break
            case 'assertXpathCount': assert xpathCount(rawTarget)==Integer.parseInt(value);break
            case 'waitForXpathCount': waitCount(rawTarget,Integer.parseInt(value));break
            case 'assertEval': assert evalCondition(rawTarget)==(value.toLowerCase()=='true') : 'Expected '+value+' for '+target;break
            case 'pause': throw new IllegalArgumentException('Fixed pause unsupported; use an operation-specific observed condition');
            default: throw new IllegalArgumentException('Command unsupported: '+operation)
        }
    }
    void finished() {
        String name=vars['vacancyName']
        if (!vars['savedVacancyUrl'] && VacancyChecks.ownedFixtureName(name,namespaceToken)) {
            // Successful create paths have a saved numeric URL; negative/cancel drafts must remain absent.
            new VacancyChecks(this).assertAbsent(name)
        }
        screenshot(caseId+'_RESULT.png')
        println('[ASSERTIONS COMPLETED] '+caseId+'; inspect actual Katalon result')
    }
    void failed(Throwable t) {
        EvidenceCapture.failure(t, "RECORDER")
        try { screenshot(caseId+'_FAILED.png') } catch(Throwable ignored) {}
        println('[TEST FAILED] '+caseId+' -> '+t.toString())
    }
}
