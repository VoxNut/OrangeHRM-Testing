package recruitment

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.WebDriver
import org.openqa.selenium.HasCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.decorators.Decorated

/** Katalon remains the driver owner/factory. No raw Selenium threads, cookie copies, or global driver. */
class ExecutionBrowser {
    static WebDriver driver() {
        ExecutionContext c=ExecutionContext.current()
        WebDriver d=DriverFactory.getWebDriver()
        if (!c.ownedDriver || !c.ownedDriver.is(d)) throw new IllegalStateException('[ENVIRONMENT / DRIVER OWNERSHIP] The Katalon driver is not bound to this execution. Use a fresh isolated runner; no other browser will be reused or closed.')
        d
    }
    static String sessionId(WebDriver d) {
        if (d instanceof Decorated) return sessionId((WebDriver)d.original)
        d instanceof RemoteWebDriver ? d.sessionId?.toString() ?: '' : 'UNVERIFIED-'+System.identityHashCode(d)
    }
    static Map options(ExecutionContext c,Map previous=[:]) {
        Map result=new LinkedHashMap(previous)
        List args=new ArrayList((previous.args ?: []) as List)
        args=args.findAll { !(it.toString() ==~ /-*(user-data-dir|profile-directory|remote-debugging-port|remote-debugging-address)(=.*)?/) }
        args << '--user-data-dir='+new File(c.root,'chrome-profile').absolutePath
        result.args=args
        result.remove('debuggerAddress') // never attach to a different execution's browser
        result.prefs=new LinkedHashMap((previous.prefs ?: [:]) as Map)+[
            'download.default_directory':new File(c.root,'downloads').absolutePath,
            'download.prompt_for_download':false,'download.directory_upgrade':true]
        result
    }
    static void launch(Closure katalonLaunch) {
        ExecutionContext c=ExecutionContext.current()
        assert !c.ownedDriver : 'Browser already belongs to this execution; do not start a second one'
        // Scope is only the Katalon preferences + launch operation, never a test/suite or cross-JVM lock.
        synchronized(RunConfiguration.class) {
            WebDriver existing=null
            try { existing=DriverFactory.getWebDriver() } catch(Exception absent) {}
            if (existing) throw new IllegalStateException('[ENVIRONMENT / DRIVER OWNERSHIP] A driver is already bound before this execution starts. Use an isolated workspace/process; it will not be adopted or closed.')
            Map prior=new LinkedHashMap(RunConfiguration.getDriverPreferencesProperties('WebUI') ?: [:])
            Map configured=options(c,prior)
            configured.each { key,value -> RunConfiguration.setWebDriverPreferencesProperty(key.toString(),value) }
            katalonLaunch.call() // WebUI.openBrowser, not new ChromeDriver
            WebDriver created=DriverFactory.getWebDriver()
            c.ownedDriver=created;c.browserSessionId=sessionId(created)
            Map chrome=created instanceof HasCapabilities ? (Map)((HasCapabilities)created).capabilities.getCapability('chrome') : null
            String actual=chrome?.userDataDir?.toString()
            if (!actual || new File(actual).canonicalFile!=new File(c.root,'chrome-profile').canonicalFile) {
                c.authentication.exhausted=true;c.authentication.reason='CHROME_PROFILE_NOT_CONFIRMED';c.persist()
                closeOwned()
                throw new IllegalStateException('[ENVIRONMENT / CHROME PROFILE NOT CONFIRMED] Katalon did not return this execution\'s configured user-data-dir. No login/fixture permitted; inspect Chrome preferences in the isolated runner.')
            }
            c.browserConfigured=true;c.persist()
        }
    }
    static void closeOwned() {
        ExecutionContext c=ExecutionContext.peek()
        if (!c?.ownedDriver) return
        try {
            WebDriver bound=DriverFactory.getWebDriver()
            if (bound.is(c.ownedDriver)) DriverFactory.closeWebDriver()
            else println('[DRIVER CLEANUP UNAVAILABLE] Bound driver differs; no foreign driver closed')
        } catch(Exception unavailable) { println('[DRIVER CLEANUP UNAVAILABLE] '+unavailable.class.simpleName) }
        finally { c.ownedDriver=null;c.menuVerified=false;c.persist() }
    }
}
