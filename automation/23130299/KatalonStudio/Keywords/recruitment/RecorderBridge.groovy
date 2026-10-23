package recruitment

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor

class RecorderBridge {
    String caseId
    Map<String,String> vars = [:]
    int seconds = 30
    boolean insideIf = false
    boolean condition = false
    boolean isElse = false
    int shotCounter = 0
    final String runStamp = new Date().format('yyyyMMdd_HHmmss_SSS')
    static final String BASE_URL = 'https://opensource-demo.orangehrmlive.com'

    RecorderBridge(String caseId) { this.caseId = caseId }

    String expand(String str) {
        if (!str) return ''
        return str.replaceAll(/\$\{([^}]+)\}/) { all, key ->
            if (!vars.containsKey(key)) throw new AssertionError('Chua khai bao bien: ' + key + ' in ' + caseId)
            return vars[key] ?: ''
        }
    }
    TestObject obj(String loc) {
        String s = expand(loc)
        TestObject o = new TestObject('dynamic_' + caseId)
        if (s.startsWith('xpath=')) o.addProperty('xpath', ConditionType.EQUALS, s.substring(6))
        else if (s.startsWith('css=')) o.addProperty('css', ConditionType.EQUALS, s.substring(4))
        else throw new IllegalArgumentException('Locator chua ho tro: ' + s)
        return o
    }
    String xp(String loc) { return expand(loc).replaceFirst(/^xpath=/, '') }
    WebDriver driver() { return DriverFactory.getWebDriver() }
    boolean hasBrowser() {
        try { WebDriver d = DriverFactory.getWebDriver(); return d != null && d.getWindowHandle() != null }
        catch (Throwable ignored) { return false }
    }
    void ensureBrowser() {
        if (!hasBrowser()) { WebUI.openBrowser(''); WebUI.maximizeWindow() }
    }
    void go(String url) {
        ensureBrowser()
        String resolved = expand(url)
        if (resolved.startsWith('/')) resolved = BASE_URL + resolved
        WebUI.navigateToUrl(resolved)
        WebUI.waitForPageLoad(15, FailureHandling.OPTIONAL)
        // The Recorder Chrome profile is not shared. Give the SPA time to redirect to login.
        long detectUntil = System.currentTimeMillis() + 7000L
        boolean loginPage = false
        while (System.currentTimeMillis() < detectUntil) {
            String here = driver().getCurrentUrl()
            if (here.contains('/auth/login') || driver().findElements(By.xpath("//input[@name='username']")).size()>0) {
                loginPage = true; break
            }
            if (driver().findElements(By.xpath("//button[normalize-space()='Add']")).size()>0 ||
                here.contains('/recruitment/addJobVacancy/')) break
            Thread.sleep(500L)
        }
        if (loginPage) {
            println('[SETUP] Vui long DANG NHAP tai Chrome Katalon mo trong 120 giay. Khong luu password vao project.')
            long deadline = System.currentTimeMillis() + 120000L
            while (System.currentTimeMillis() < deadline) {
                String here = driver().getCurrentUrl()
                if (!here.contains('/auth/login') && driver().findElements(By.xpath("//input[@name='username']")).isEmpty()) break
                Thread.sleep(700L)
            }
            assert !driver().getCurrentUrl().contains('/auth/login') : 'Login required: mo Chrome do Studio khoi dong va dang nhap OrangeHRM.'
            WebUI.navigateToUrl(resolved)
            WebUI.waitForPageLoad(15, FailureHandling.OPTIONAL)
        }
    }
    String screenshotDirectory() {
        String raw = ''
        try { raw = String.valueOf(GlobalVariable.evidenceDir ?: '') }
        catch (Throwable ignored) {}
        File dst = raw.trim() ? new File(raw.trim()) : new File(RunConfiguration.getProjectDir(), 'evidence/screenshots')
        dst.mkdirs()
        return dst.absolutePath
    }
    void screenshot(String label) {
        if (!hasBrowser()) { println('[CAPTURE] SKIP - chua co Chrome'); return }
        String safe = label.replaceAll(/[^A-Za-z0-9._-]/, '_')
        if (!safe.toLowerCase().endsWith('.png')) safe += '.png'
        File dir = new File(screenshotDirectory(), caseId + '/' + runStamp)
        dir.mkdirs()
        String file = new File(dir, safe).absolutePath
        WebUI.takeScreenshot(file, FailureHandling.OPTIONAL)
        if (new File(file).isFile()) println('[CAPTURE] SAVED ' + file)
        else println('[CAPTURE WARNING] Khong thay PNG tai ' + file)
    }
    void countCheckpoint(String label) { shotCounter++; screenshot(label) }
    boolean selected(String loc) {
        return driver().findElement(By.xpath(xp(loc))).isSelected()
    }
    void desiredState(String loc, boolean wanted) {
        boolean now = selected(loc)
        if (now != wanted) {
            WebElement el = driver().findElement(By.xpath(xp(loc)))
            // OrangeHRM renders hidden input checkboxes. Click via DOM to trigger React's change event.
            ((JavascriptExecutor) driver()).executeScript('arguments[0].click();', el)
            Thread.sleep(300L)
        }
        assert selected(loc) == wanted : 'Checkbox cannot be set to ' + wanted + ': ' + loc
    }
    void waitValue(String loc, String expected, boolean textMode=false) {
        long until = System.currentTimeMillis() + seconds*1000L
        String actual = ''
        while (System.currentTimeMillis()<until) {
            try {
                TestObject to = obj(loc)
                actual = textMode ? WebUI.getText(to) : WebUI.getAttribute(to, 'value')
                if ((actual ?: '').trim().replaceAll(/\s+/, ' ') == expected.trim().replaceAll(/\s+/, ' ')) return
            } catch(Throwable ignored) {}
            Thread.sleep(450L)
        }
        throw new AssertionError('Timeout checking '+loc+': expected ['+expected+'] but actual ['+actual+']')
    }
    int xpathCount(String loc) { return driver().findElements(By.xpath(xp(loc))).size() }
    void waitCount(String loc, int expected) {
        long until = System.currentTimeMillis() + seconds*1000L
        while (System.currentTimeMillis() < until) {
            if (xpathCount(loc) == expected) return
            Thread.sleep(500L)
        }
        throw new AssertionError('XPath count mismatch for '+loc+': expected '+expected+' actual '+xpathCount(loc))
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
                vars[value]=e.replace('new Date().getTime()', String.valueOf(System.currentTimeMillis())).replaceAll(/['+]/, '')
                break
            case 'open': go(target); break
            case 'waitForElementPresent':
                assert WebUI.waitForElementPresent(obj(rawTarget),seconds) : 'Missing '+target;break
            case 'assertElementPresent':
                assert WebUI.verifyElementPresent(obj(rawTarget),seconds) : 'Missing '+target;break
            case 'assertElementNotPresent':
                assert WebUI.verifyElementNotPresent(obj(rawTarget),2) : 'Unexpected element '+target;break
            case 'click':
                if (target.contains("@type='checkbox'") || target.contains('@type="checkbox"')) {
                    WebElement el=driver().findElement(By.xpath(xp(rawTarget)))
                    ((JavascriptExecutor) driver()).executeScript('arguments[0].click();', el)
                } else {
                    WebUI.click(obj(rawTarget))
                }
                if (target.contains('Save') || target.contains('Search') || target.contains('Reset') || target.contains('Cancel')) {
                    Thread.sleep(850L)
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
            case 'storeLocation': vars[target]=driver().getCurrentUrl();break
            case 'assertChecked': assert selected(rawTarget) : 'Expected checked '+target;break
            case 'assertNotChecked': assert !selected(rawTarget) : 'Expected unchecked '+target;break
            case 'ensureChecked': desiredState(rawTarget,true);break
            case 'ensureUnchecked': desiredState(rawTarget,false);break
            case 'captureEntirePageScreenshot': countCheckpoint(target);break
            case 'storeXpathCount': vars[value]=String.valueOf(xpathCount(rawTarget));break
            case 'assertXpathCount': assert xpathCount(rawTarget)==Integer.parseInt(value);break
            case 'waitForXpathCount': waitCount(rawTarget,Integer.parseInt(value));break
            case 'assertEval': assert evalCondition(rawTarget)==(value.toLowerCase()=='true') : 'Expected '+value+' for '+target;break
            case 'pause': Thread.sleep(Long.parseLong(target));break
            default: throw new IllegalArgumentException('Command unsupported: '+operation)
        }
    }
    void finished() { screenshot(caseId+'_RESULT.png');println('[PASS ASSERTIONS] '+caseId+' (not verified until actual Studio run)') }
    void failed(Throwable t) {
        try { screenshot(caseId+'_FAILED.png') } catch(Throwable ignored) {}
        println('[TEST FAILED] '+caseId+' -> '+t.toString())
    }
}
