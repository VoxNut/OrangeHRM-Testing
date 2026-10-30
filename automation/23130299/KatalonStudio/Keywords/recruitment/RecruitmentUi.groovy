package recruitment

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable
import com.google.gson.GsonBuilder
import org.openqa.selenium.*
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.WheelInput
import org.openqa.selenium.support.ui.ExpectedConditions as EC
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** ASM7 only. Candidates list selectors checked against the 20261029_215823 local Time Capsule.
 * No automatic login, API writes, shared-record edits, deletion or success fallback.
 */
class RecruitmentUi {
    static final String BASE = 'https://opensource-demo.orangehrmlive.com'
    static final String ROOT = BASE + '/web/index.php/recruitment/'
    static final String DASHBOARD = BASE + '/web/index.php/dashboard/index'
    static final String LOGIN = "//input[@name='username']"
    // Accept only the sidebar module entry or its Candidates destination, never the Vacancies tab.
    // The absolute viewRecruitmentModule href is present in that captured sidebar.
    static final String SIDEBAR = '//aside//nav'
    static final String RECRUITMENT_MENU = SIDEBAR + "//a[normalize-space()='Recruitment' and (" +
        "@href='/web/index.php/recruitment/viewRecruitmentModule' or @href='${ROOT}viewRecruitmentModule' or " +
        "@href='/web/index.php/recruitment/viewCandidates' or @href='${ROOT}viewCandidates')]"
    static final String LOADER = "//*[contains(@class,'oxd-loading-spinner') or contains(@class,'oxd-form-loader')]"
    static final String ROWS = ".//div[contains(concat(' ',normalize-space(@class),' '),' oxd-table-body ')]//div[@role='row']"
    static final String EMPTY = "//div[contains(@class,'orangehrm-horizontal-padding')]//*[normalize-space()='No Records Found']"
    static final String SELECTED_PAGE = "//button[${cls('oxd-pagination-page-item--page-selected')}]"
    // Header sort menus contain hidden Ascending/Descending text. Match the header's own text node.
    static String column(String name) { return "//div[@role='columnheader'][text()[normalize-space(.)=${lit(name)}]]" }
    static final Map<String,String> CANDIDATE_MARKERS = [
        vacancy:select('Vacancy'), candidateName:field('Candidate Name'),
        search:"//form//button[normalize-space()='Search']", add:button('Add'),
        candidateHeader:column('Candidate'),
        tableBody:column('Candidate') + "/ancestor::div[${cls('oxd-table')}][1]//div[${cls('oxd-table-body')}]",
        empty:EMPTY
    ].asImmutable()
    final String caseId
    final ExecutionContext executionContext = ExecutionContext.current()
    final String token = executionContext.tokenFor('candidate-helper')
    final File output
    final List<Map> ledger = []
    int shot = 0
    int seconds = 30
    private boolean candidatesStarted = false
    private String navigationPhase = 'NOT_STARTED'
    private String navigationLocator = ''
    private static class ManualChallenge extends RuntimeException {}
    private static class SelectClosedAfterScroll extends RuntimeException {}
    private final Map<String,Integer> perfCounts = [navigation:0, vacancyFixtures:0, candidateFixtures:0, retries:0, timeouts:0, browserStarts:0]
    private final List<Integer> perfStack = []
    private final Set<Throwable> timedFailures = Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>())
    private int perfSequence = 0
    private long diagnosticWriteNs = 0L

    Object measure(String phase, Closure action) {
        int id = ++perfSequence
        Integer parent = perfStack ? perfStack[-1] : null
        long started = System.nanoTime(), epoch = System.currentTimeMillis()
        String outcome = 'OK'
        perfStack << id
        try { return action.call() }
        catch (Throwable error) {
            boolean timeout = error instanceof TimeoutException || timedFailures.contains(error)
            outcome = timeout ? 'TIMEOUT' : error instanceof IllegalStateException && error.message?.startsWith('[BLOCKED') ? 'BLOCKED' : 'ERROR'
            if (timeout && timedFailures.add(error)) perfCounts.timeouts++
            throw error
        } finally {
            perfStack.removeAt(perfStack.size() - 1)
            Map row = [caseId:caseId, phase:phase, operationId:id, parentId:parent, startEpochMs:epoch,
                elapsedMs:(System.nanoTime() - started).intdiv(1000000L), outcome:outcome, counters:new LinkedHashMap(perfCounts),
                diagnosticWriteMs:diagnosticWriteNs.intdiv(1000000L)]
            String json = new GsonBuilder().create().toJson(row)
            println('[PERFORMANCE] ' + json)
            try { new File(output, 'performance.jsonl').append(json + '\n', 'UTF-8') }
            catch (Exception unavailable) { println('[PERFORMANCE LOG UNAVAILABLE] ' + unavailable.class.simpleName) }
        }
    }
    protected void incrementPerformance(String counter) {
        assert perfCounts.containsKey(counter) : 'Unknown performance counter'
        perfCounts[counter]++
    }
    void countFixture(String kind) { perfCounts[kind == 'vacancy' ? 'vacancyFixtures' : 'candidateFixtures']++ }
    private void timeoutAlias(Throwable original, Throwable wrapper) {
        if (original instanceof TimeoutException || timedFailures.contains(original)) {
            if (timedFailures.add(original)) perfCounts.timeouts++
            timedFailures.add(wrapper) // The same failure wrapped at a boundary is not another timeout.
        }
    }
    private void errorWait(long started, TimeoutException error) {
        if (!timedFailures.add(error)) return
        perfCounts.timeouts++
        long elapsed = (System.nanoTime() - started).intdiv(1000000L)
        Map row = [caseId:caseId, operationId:++perfSequence, parentId:perfStack ? perfStack[-1] : null,
            phase:'ERROR_WAIT', startEpochMs:System.currentTimeMillis() - elapsed, elapsedMs:elapsed,
            outcome:'TIMEOUT', counters:new LinkedHashMap(perfCounts)]
        String json = new GsonBuilder().create().toJson(row)
        println('[PERFORMANCE] ' + json)
        try { new File(output, 'performance.jsonl').append(json + '\n', 'UTF-8') } catch (Exception ignored) {}
    }

    RecruitmentUi(String caseId) {
        assert caseId.startsWith('RECRUIT_CAND_') || caseId.startsWith('RECRUIT_FLOW_')
        this.caseId = caseId
        output = executionContext.caseDirectory(caseId,token)
        track([kind:'case', caseId:caseId, runToken:token, cleanup:'Manual: candidates before vacancies; retain evidence first'])
    }

    static Object config(String name, Object fallback) {
        ExecutionContext c=ExecutionContext.peek()
        Object raw = null
        if (c?.settings?.containsKey(name) && c.settings[name]!=null) raw = c.settings[name]
        else {
            try { raw = GlobalVariable."${name}" }
            catch (MissingPropertyException ignored) { raw = null }
        }
        if (name in ['hiringManagerName','hiringManagerAlternativeName','interviewerA','interviewerB']) {
            String text = raw == null ? '' : raw.toString().trim()
            if (text) return text
            String discovered = ConfigurationPreflight.resolvedIdentity(name, c)
            if (discovered) return discovered
            return fallback
        }
        return raw == null ? fallback : raw
    }
    static String lit(String s) { return VacancyChecks.literal(s) }
    static String cls(String s) { return VacancyChecks.token(s) }
    static String group(String label) { return VacancyChecks.group(label) }
    static String field(String label) { return group(label) + '//input' }
    static String area(String label) { return group(label) + '//textarea' }
    static String select(String label) { return group(label) + "//div[${cls('oxd-select-text-input')}]" }
    static String dateFilter(String edge) {
        assert edge in ['From','To']
        return "//form//div[${cls('oxd-date-input')}]//input[@placeholder=${lit(edge)}]"
    }
    Map candidateFilters() {
        Map paths = ['Job Title':select('Job Title'), Vacancy:select('Vacancy'), 'Hiring Manager':select('Hiring Manager'),
            Status:select('Status'), 'Method of Application':select('Method of Application'),
            'Candidate Name':field('Candidate Name'), Keywords:field('Keywords'), From:dateFilter('From'), To:dateFilter('To')]
        return (Map) ((JavascriptExecutor) driver()).executeScript('''
            const out = {unknown:false};
            for (const [key,xp] of Object.entries(arguments[0])) {
                const nodes=document.evaluate(xp,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
                if (nodes.snapshotLength !== 1) {out.unknown=true; continue;}
                const n=nodes.snapshotItem(0); out[key]=n.tagName === 'INPUT' ? n.value : n.textContent.trim();
            }
            const active=document.evaluate(arguments[1],document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
            out.page=active.snapshotLength === 1 ? active.snapshotItem(0).textContent.trim() : document.querySelector('.oxd-pagination__ul') ? '?' : '1';
            return out;
        ''', paths, SELECTED_PAGE)
    }
    static boolean filterInputsAreDefault(Map s) {
        return s?.unknown == false &&
            ['Job Title','Vacancy','Hiring Manager','Status','Method of Application'].every { s[it] in ['', '-- Select --'] } &&
            ['Candidate Name','Keywords','From','To'].every { s[it] == '' }
    }
    static boolean filtersAreDefault(Map s) {
        return s.unknown == false && s.page == '1' &&
            ['Job Title','Vacancy','Hiring Manager','Status','Method of Application'].every { s[it] in ['', '-- Select --'] } &&
            ['Candidate Name','Keywords','From','To'].every { s[it] == '' }
    }
    WebDriver driver() {
        assert ExecutionContext.peek().is(executionContext) : 'Helper belongs to a different execution'
        return ExecutionBrowser.driver()
    }
    WebDriverWait waitFor(int timeout = seconds) {
        return new WebDriverWait(driver(), Duration.ofSeconds(timeout)).pollingEvery(Duration.ofMillis(200))
            .ignoring(StaleElementReferenceException).ignoring(NoSuchElementException)
    }
    static boolean detachedDocument(Throwable error) {
        return error instanceof StaleElementReferenceException ||
            (error instanceof WebDriverException && error.message?.contains('Node with given id does not belong to the document'))
    }
    List<WebElement> visible(String xp) {
        // Retry only this read, reacquiring by locator on every poll. Never retry a click/Save here.
        return waitFor().until {
            try { return [elements:driver().findElements(By.xpath(xp)).findAll { it.displayed }] }
            catch (WebDriverException error) { if (!detachedDocument(error)) throw error; return null }
        }.elements
    }
    WebElement one(String xp) {
        long started = System.nanoTime()
        try { return waitFor().until {
            List<WebElement> all = visible(xp)
            assert all.size() <= 1 : '[NEEDS LIVE VERIFICATION] Ambiguous selector: ' + xp
            return all ? all[0] : null
        } } catch (TimeoutException error) { errorWait(started, error); throw error }
    }
    boolean shown(String xp) { return !visible(xp).empty }
    void ready() { waitFor().until { visible(LOADER).empty } }
    void click(String xp) {
        waitFor().until {
            if (shown(LOADER)) return false
            List<WebElement> found = visible(xp)
            assert found.size() <= 1 : 'Ambiguous click selector: ' + xp
            if (!found || !found[0].enabled || found[0].getAttribute('aria-disabled') == 'true') return false
            found[0].click()
            return true
        }
    }
    static String button(String label) { return "//button[normalize-space()=${lit(label)}]" }
    void buttonClick(String label) {
        if (label in ['Save','Cancel','Shortlist','Reject','Schedule Interview']) capture('BEFORE_ACTION_' + label)
        click(button(label))
        if (label in ['Save','Cancel','Shortlist','Reject','Schedule Interview']) capture('AFTER_ACTION_' + label)
    }
    void heading(String text) { one("//*[self::h5 or self::h6][normalize-space()=${lit(text)}]"); ready() }
    void ensureBrowser() {
        SessionGuard.requireAllowance()
        ExecutionLease.acquire()
        try { driver().windowHandle }
        catch (Exception ignored) {
            if (executionContext.ownedDriver) SessionGuard.lost('Owned Chrome session unavailable')
            measure('BROWSER_START') { launchBrowser(); incrementPerformance('browserStarts') }
        }
    }
    // Transport boundary also permits offline dispatch tests without ever launching a real browser.
    protected void launchBrowser() { ExecutionBrowser.launch { WebUI.openBrowser(''); WebUI.maximizeWindow() } }
    static boolean sameRoute(String actual, String expected) {
        try {
            URI a = new URI(actual), b = new URI(expected)
            return !a.userInfo && a.scheme == b.scheme && a.host == b.host && a.port == b.port &&
                a.path.replaceFirst('/+$', '') == b.path.replaceFirst('/+$', '')
        } catch (Exception ignored) { return false }
    }
    // Never write URL query, fragment, userinfo, page source, body text or session data to our logs.
    static String safeUrl(String raw) {
        try {
            URI u = new URI(raw)
            return new URI(u.scheme, null, u.host, u.port, u.path, null, null).toString()
        } catch (Exception ignored) { return '[URL unavailable]' }
    }
    boolean authenticated() {
        URI here
        try { here = new URI(driver().currentUrl) } catch (Exception ignored) { return false }
        return here.scheme == 'https' && here.host == new URI(BASE).host && here.port == -1 &&
            !here.userInfo && here.path.startsWith('/web/index.php/') && !here.path.startsWith('/web/index.php/auth/') &&
            !shown(LOGIN) && shown(SIDEBAR) && shown('//header//h6')
    }
    boolean documentReady() {
        return ((JavascriptExecutor) driver()).executeScript('return document.readyState') == 'complete' && visible(LOADER).empty
    }
    boolean interactive(String xp) {
        List<WebElement> elements = visible(xp)
        return elements.size() == 1 && elements[0].enabled && elements[0].getAttribute('aria-disabled') != 'true'
    }
    Map pageState() {
        // One read-only browser command, not dozens of WebDriver round trips per readiness poll.
        Map selectors = [login:LOGIN, sidebar:SIDEBAR, header:'//header//h6', loader:LOADER] + CANDIDATE_MARKERS
        Map raw = (Map) ((JavascriptExecutor) driver()).executeScript('''
            const selectors = arguments[0], result = {};
            function visible(el) {
                if (!Array.from(el.getClientRects()).some(r=>r.width>0&&r.height>0)) return false;
                for(let p=el;p;p=p.parentElement) {const s=getComputedStyle(p);if(s.visibility==='hidden'||s.display==='none'||s.opacity==='0')return false;}
                return true;
            }
            for (const [key,xp] of Object.entries(selectors)) {
                const snap = document.evaluate(xp, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
                const found = [];
                for (let i=0;i<snap.snapshotLength;i++) { const n=snap.snapshotItem(i); if (visible(n)) found.push(n); }
                result[key] = ['vacancy','candidateName','search','add'].includes(key) ?
                    found.length === 1 && !found[0].disabled && found[0].getAttribute('aria-disabled') !== 'true' : found.length > 0;
            }
            const text = (document.body ? document.body.innerText : '').toLowerCase(), title = document.title.toLowerCase();
            return {url:location.href, title:document.title, flags:result, domComplete:document.readyState === 'complete',
                challenge:/just a moment|attention required/.test(title) || /verify you are human|verifying you are human|checking your browser|performing security verification/.test(text) || !!document.querySelector('iframe[src*="challenges.cloudflare.com"]'),
                networkError:/err_connection|err_name_not_resolved|err_internet_disconnected|this site can.t be reached|502 bad gateway|503 service unavailable|504 gateway time/.test(text)};
        ''', selectors)
        String url = raw.url.toString()
        URI here = new URI(url)
        Map flags = raw.flags
        Map state = [url:safeUrl(url), title:raw.title.toString().replaceAll(/[\r\n\t]/, ' ').take(160),
            login:sameRoute(url, BASE + '/web/index.php/auth/login') || flags.login,
            dashboard:sameRoute(url, DASHBOARD), vacancies:sameRoute(url, ROOT + 'viewJobVacancy'),
            candidates:sameRoute(url, ROOT + 'viewCandidates'),
            authenticated:here.scheme == 'https' && here.host == new URI(BASE).host && here.port == -1 && !here.userInfo &&
                here.path.startsWith('/web/index.php/') && !here.path.startsWith('/web/index.php/auth/') && !flags.login && flags.sidebar && flags.header,
            domComplete:raw.domComplete == true, loading:flags.loader == true, challenge:raw.challenge == true,
            networkError:raw.networkError == true || url.startsWith('chrome-error://')]
        state.markers = CANDIDATE_MARKERS.collectEntries { key, xp -> [(key):flags[key] == true] }
        Map m = state.markers
        state.candidatesMissingMarkers = state.candidates && (!['vacancy','candidateName','search','add'].every { m[it] } || !((m.candidateHeader && m.tableBody) || m.empty))
        return state
    }
    static boolean candidateListReady(Map s) {
        Map m = s.markers ?: [:]
        return s.candidates == true && s.authenticated == true && s.login == false &&
            s.domComplete == true && s.loading == false && s.challenge == false && s.networkError == false &&
            m.vacancy == true && m.candidateName == true && m.search == true && m.add == true &&
            ((m.candidateHeader == true && m.tableBody == true) || m.empty == true)
    }
    void navigationLog(String event, Map details = [:]) {
        long writeStarted = System.nanoTime()
        Map entry = [time:new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()), caseId:caseId,
            phase:navigationPhase, event:event] + details
        String json = new GsonBuilder().disableHtmlEscaping().create().toJson(entry)
        println('[NAVIGATION] ' + json)
        try { new File(output, 'navigation.jsonl').append(json + '\n', 'UTF-8') }
        catch (Exception ex) { println('[NAVIGATION LOG UNAVAILABLE] ' + caseId + ': ' + ex.class.simpleName) }
        finally { diagnosticWriteNs += System.nanoTime() - writeStarted }
    }
    void navigationFailure(long started, Throwable error) {
        Map details = [elapsedMs:(System.nanoTime() - started).intdiv(1000000L),
            waitingFor:navigationLocator, errorType:error.class.simpleName]
        try { details.url = safeUrl(driver().currentUrl) } catch (Exception ignored) { details.url = '[unavailable]' }
        try { details.title = driver().title.replaceAll(/[\r\n\t]/, ' ').take(160) }
        catch (Exception ignored) { details.title = '[unavailable]' }
        try { details.putAll(pageState()) }
        catch (Exception unavailable) { details.browserStateUnavailable = unavailable.class.simpleName }
        navigationLog('STOPPED', details)
        try { capture('NAVIGATION_' + navigationPhase) }
        catch (Throwable unavailable) { navigationLog('SCREENSHOT_UNAVAILABLE', [errorType:unavailable.class.simpleName]) }
    }
    void navigationAction(String phase, String locator, Closure action) {
        measure(phase == 'CLICK_RECRUITMENT' || phase == 'CLICK_CANDIDATES_TAB' ? 'OPEN_CANDIDATES' : phase == 'TECHNICAL_RECRUITMENT_ROUTE' ? 'NAVIGATE_ROUTE' : phase) {
            navigationActionInternal(phase, locator, action)
        }
    }
    // Closure targets must be inherited: Groovy dispatches on CandidateChecks/WorkflowChecks at runtime.
    protected void navigationActionInternal(String phase, String locator, Closure action) {
        navigationPhase = phase; navigationLocator = locator
        long started = System.nanoTime()
        navigationLog('START', [waitingFor:locator])
        try {
            incrementPerformance('navigation')
            action.call()
            navigationLog('ACTION_COMPLETED', [url:safeUrl(driver().currentUrl)])
        } catch (Exception error) {
            navigationFailure(started, error)
            IllegalStateException stopped = new IllegalStateException('[NAVIGATION ACTION STOPPED] ' + caseId + '; phase=' + phase +
                '; ' + error.class.simpleName + '; inspect navigation.jsonl and real screenshots.')
            stopped.initCause(error); timeoutAlias(error, stopped); throw stopped
        }
    }
    void awaitNavigation(String phase, String locator, Closure condition, int timeout = seconds) {
        measure(phase in ['DETECT_LOGIN','MANUAL_LOGIN'] ? 'LOGIN_WAIT' : phase) {
            awaitNavigationInternal(phase, locator, condition, timeout)
        }
    }
    protected void awaitNavigationInternal(String phase, String locator, Closure condition, int timeout) {
        navigationPhase = phase; navigationLocator = locator
        long started = System.nanoTime()
        Map last = [:]
        boolean manualChallengeUsed = false
        navigationLog('WAIT', [waitingFor:locator, timeoutSeconds:timeout])
        try {
            while (true) {
                try {
                    waitFor(timeout).until {
                        Map state = pageState()
                        if (state.url != last.url) navigationLog('URL_OBSERVED', [url:state.url])
                        last = state
                        if (state.challenge) throw new ManualChallenge()
                        return condition.call(state)
                    }
                    navigationLog('READY', last + [elapsedMs:(System.nanoTime() - started).intdiv(1000000L)])
                    return
                } catch (ManualChallenge challenge) {
                    if (manualChallengeUsed) throw new TimeoutException('Manual verification remains unresolved/repeated')
                    manualChallengeUsed = true
                    navigationLog('MANUAL_VERIFICATION_REQUIRED', [url:last.url, allowanceSeconds:120])
                    println('[MANUAL VERIFICATION] ' + caseId + ': hoan tat kiem tra cua trang trong Chrome, toi da 120 giay. Khong tu dong giai CAPTCHA.')
                    waitFor(120).until { !pageState().challenge }
                    // A new, bounded render wait after manual verification; do not accept the challenge page.
                }
            }
        } catch (WebDriverException error) {
            navigationFailure(started, error)
            boolean authBlocked = phase == 'MANUAL_LOGIN' && last.login == true && last.challenge != true
            if (authBlocked) SessionGuard.failedLogin()
            if (authBlocked) navigationLog('AUTHENTICATION_BLOCKED', [reason:'Manual login not completed or session still unauthenticated', allowanceSeconds:timeout])
            TimeoutException stopped = new TimeoutException((authBlocked ? '[ENVIRONMENT / AUTHENTICATION BLOCKED] Manual login not completed; ' : '[NAVIGATION STOPPED] ') + caseId + '; phase=' + phase +
                '; elapsedMs=' + (System.nanoTime() - started).intdiv(1000000L) +
                '; url=' + (last.url ?: '[unavailable]') + '; inspect navigation.jsonl and real screenshots.')
            stopped.initCause(error); timeoutAlias(error, stopped); throw stopped
        }
    }
    void openDashboard() {
        SessionGuard.requireAllowance()
        navigationAction('OPEN_DASHBOARD', DASHBOARD) { ensureBrowser(); WebUI.navigateToUrl(DASHBOARD) }
        awaitNavigation('DETECT_LOGIN', 'Login username input OR authenticated document/header/sidebar') { s ->
            s.login || (s.authenticated && s.domComplete && !s.loading && !s.networkError)
        }
        if (shown(LOGIN) || sameRoute(driver().currentUrl, BASE + '/web/index.php/auth/login')) {
            SessionGuard.requireInitialLogin()
            println('[MANUAL LOGIN] ' + caseId + ': dang nhap trong Chrome do Katalon mo, trong 120 giay. Khong luu credential vao project.')
            awaitNavigation('MANUAL_LOGIN', 'Authenticated document + header/sidebar, Login absent', { s ->
                s.authenticated && !s.login && s.domComplete && !s.loading && !s.networkError
            }, 120)
        }
        navigationLog('AFTER_LOGIN', [url:safeUrl(driver().currentUrl)])
        SessionGuard.authenticated()
        // Login can redirect to a remembered module. Dashboard must be verified before clicking the menu.
        if (!sameRoute(driver().currentUrl, DASHBOARD)) {
            navigationAction('RETURN_DASHBOARD', DASHBOARD) { WebUI.navigateToUrl(DASHBOARD) }
        }
        awaitNavigation('DASHBOARD_READY', "Dashboard URL + //header//h6[normalize-space()='Dashboard'] + " + RECRUITMENT_MENU) { s ->
            s.dashboard && s.authenticated && s.domComplete && !s.loading && !s.networkError &&
                shown("//header//h6[normalize-space()='Dashboard']") && interactive(RECRUITMENT_MENU)
        }
    }
    void waitForCandidates() {
        awaitNavigation('CANDIDATES_READY', 'Exact Candidates route; interactive filters/Search/Add; Candidate table shell OR empty state; ' + CANDIDATE_MARKERS) { s ->
            if (s.login) SessionGuard.lost('Login redirect while waiting for Candidates')
            candidateListReady(s)
        }
    }
    void openCandidatesFromMenu() {
        measure('OPEN_CANDIDATES') { enterCandidates() }
    }
    protected void enterCandidates() {
        ensureBrowser()
        Map state = pageState()
        boolean sessionVerified = executionContext.menuVerified
        if (state.login || !state.authenticated) { executionContext.menuVerified=false; sessionVerified = false }
        SessionGuard.requireAllowance()
        if (!sessionVerified) openDashboard()
        else if (state.candidates) {
            if (!candidateListReady(state)) waitForCandidates()
            candidatesStarted = true
            navigationLog('CANDIDATE_LIST_REUSED', [url:state.url])
            return
        }
        // Reuse the proven native sidebar action from any authenticated Recruitment page.
        navigationAction('CLICK_RECRUITMENT', RECRUITMENT_MENU) { click(RECRUITMENT_MENU) }
        navigationLog('AFTER_RECRUITMENT_CLICK', [url:safeUrl(driver().currentUrl)])
        waitForCandidates()
        candidatesStarted = true
        executionContext.menuVerified = true // Every reuse still checks current DOM/auth.
        navigationLog('CANDIDATE_LIST_LOADED', [url:safeUrl(driver().currentUrl)])
    }
    void ensureCandidatesStarted() {
        if (!candidatesStarted) openCandidatesFromMenu()
    }
    void waitForRoute(String url, String expectedHeading = '') {
        awaitNavigation('TECHNICAL_ROUTE_READY', safeUrl(url) + '; heading=' + expectedHeading) { s ->
            if (s.login) SessionGuard.lost('Login redirect while waiting for Recruitment route')
            sameRoute(driver().currentUrl, url) && s.authenticated && s.domComplete && !s.loading && !s.networkError &&
                (expectedHeading ? shown("//*[self::h5 or self::h6][normalize-space()=${lit(expectedHeading)}]") : shown('//form'))
        }
        if (expectedHeading) heading(expectedHeading)
        assert driver().currentUrl == url && authenticated() : 'Wrong Recruitment page: ' + driver().currentUrl
    }
    void go(String route, String expectedHeading = '', boolean fresh = false) {
        measure(route == 'viewCandidates' ? 'OPEN_CANDIDATES' : route ==~ /.*\/(?:addCandidate|addJobVacancy)\/\d+/ ? 'RECORD_REOPEN' : 'NAVIGATE_ROUTE') {
            goInternal(route, expectedHeading, fresh)
        }
    }
    protected void goInternal(String route, String expectedHeading, boolean fresh) {
        SessionGuard.requireAllowance()
        String url = route.startsWith('https://') ? route : ROOT + route
        assert url.startsWith(ROOT) : 'Only Recruitment demo routes are allowed'
        if (sameRoute(url, ROOT + 'viewCandidates')) { openCandidatesFromMenu(); return }
        ensureCandidatesStarted()
        Map state = pageState()
        if (!fresh && sameRoute(driver().currentUrl, url) && state.authenticated && state.domComplete && !state.loading && !state.challenge && !state.networkError &&
            (!expectedHeading || shown("//*[self::h5 or self::h6][normalize-space()=${lit(expectedHeading)}]"))) {
            navigationLog('ROUTE_REUSED', [url:safeUrl(url)])
            return
        }
        navigationAction('TECHNICAL_RECRUITMENT_ROUTE', 'Fixture/form/owned-profile route: ' + safeUrl(url)) { WebUI.navigateToUrl(url) }
        awaitNavigation('TECHNICAL_ROUTE_AUTH', 'Login OR requested authenticated Recruitment route and ready DOM') { s ->
            s.login || (sameRoute(driver().currentUrl, url) && s.authenticated && s.domComplete && !s.loading && !s.networkError &&
                (!expectedHeading || shown("//*[self::h5 or self::h6][normalize-space()=${lit(expectedHeading)}]")))
        }
        if (sameRoute(driver().currentUrl, BASE + '/web/index.php/auth/login')) {
            SessionGuard.lost('Login redirect during Recruitment navigation')
        }
        assert sameRoute(driver().currentUrl, url) && authenticated() : 'Wrong Recruitment route'
    }
    void replace(String xp, String value, boolean exact = true) {
        long deadline = System.nanoTime() + Duration.ofSeconds(seconds).toNanos()
        WebElement el = selectionWait(deadline).until {
            List<WebElement> fields = visible(xp)
            assert fields.size() <= 1 : 'Ambiguous field: ' + xp
            return fields.size() == 1 && fields[0].enabled ? fields[0] : null
        }
        assert el.enabled : '[NEEDS LIVE VERIFICATION] Field disabled: ' + xp
        el.sendKeys(Keys.chord(Keys.CONTROL, 'a'), Keys.BACK_SPACE)
        if (value) el.sendKeys(value)
        el.sendKeys(Keys.TAB)
        if (exact) selectionWait(deadline).until {
            List<WebElement> fields = visible(xp)
            return fields.size() == 1 && fields[0].getDomProperty('value') == value
        }
    }
    String value(String xp) { return one(xp).getDomProperty('value') ?: '' }
    void equalsValue(String xp, String expected) {
        waitFor().until {
            List<WebElement> fields = visible(xp)
            assert fields.size() <= 1 : 'Ambiguous value selector: ' + xp
            return fields.size() == 1 && (fields[0].getDomProperty('value') ?: '') == expected
        }
        List<WebElement> confirmed = visible(xp)
        assert confirmed.size() == 1 && (confirmed[0].getDomProperty('value') ?: '') == expected : 'Value differs: ' + xp
    }
    void choose(String label, String expected) {
        measure('DROPDOWN_SELECT') { chooseSelect(label, expected) }
    }
    protected void chooseSelect(String label, String expected) {
        assert expected?.trim() && expected != '-- Select --' : 'An exact non-placeholder option is required'
        navigationPhase = 'SELECT_DROPDOWN'
        String boxes = selectListbox(label), options = selectOption(label, expected)
        navigationLocator = options
        long started = System.nanoTime()
        long deadline = started + Duration.ofSeconds(seconds).toNanos()
        String selectionUrl = driver().currentUrl
        Map last = [field:label, expected:expected, retry:0]
        try {
            assert visible(group(label)).size() == 1 : 'Missing/ambiguous select input group: ' + label
            assert visible(boxes).size() <= 1 : 'Multiple open listboxes in field: ' + label
            if (visible(boxes).empty) click(select(label)) // Open this field once; never toggle in a retry loop.
            for (int retry = 0; retry <= 1; retry++) {
                Map previous = null
                WebElement previousOption = null
                int scrolls = 0
                int closedPolls = 0
                String previousLog = ''
                try {
                WebElement option = selectionWait(deadline).until {
                    if (sameRoute(driver().currentUrl, BASE + '/web/index.php/auth/login')) SessionGuard.lost('Login redirect while selecting ' + label)
                    Map probe = selectProbe(boxes, selectRelativeOption(expected))
                    last = [field:label, expected:expected, retry:retry] + probe.state
                    if (last.openListboxes == 1 && last.matches == 1 && last.visible && last.enabled && !last.loading) {
                        Map geometry = selectGeometry((WebElement) probe.box, (WebElement) probe.option)
                        last.putAll(geometry)
                        last.stable = previousOption == probe.option && selectStable(previous, geometry)
                        previous = geometry; previousOption = (WebElement) probe.option
                    }
                    last.state = selectReadiness(last)
                    String signature = [last.state, last.matches, last.openListboxes, last.blocker].toString()
                    if (signature != previousLog) { navigationLog('DROPDOWN_WAIT', last + [url:safeUrl(driver().currentUrl)]); previousLog = signature }
                    if (last.state == 'closed' && scrolls > 0 && !last.loading) {
                        if (++closedPolls >= 2) throw new SelectClosedAfterScroll()
                    } else closedPolls = 0
                    if (last.state == 'ambiguous') throw new IllegalStateException('Ambiguous exact option/listbox in ' + label + ': ' + expected)
                    if (last.state == 'scroll') {
                        assert ++scrolls <= 40 : 'Dropdown scroll limit reached: ' + label
                        int delta = selectScrollDelta(last)
                        assert delta != 0 : 'Option clipped without a safe vertical scroll delta: ' + label
                        navigationLog('DROPDOWN_SCROLL', [field:label, expected:expected, retry:retry, deltaY:delta, scrolls:scrolls,
                            originX:last.originX, originY:last.originY, originSafe:last.originSafe, scrollTop:last.scrollTop, listbox:last.listbox])
                        new Actions(driver()).scrollFromOrigin(WheelInput.ScrollOrigin.fromViewport(
                            ((Number) last.originX).intValue(), ((Number) last.originY).intValue()), 0, delta).perform()
                        previous = null; previousOption = null // Reacquire and measure after wheel/re-render.
                    }
                    return last.state == 'click' ? probe.option : null
                }
                    option.click() // Native Selenium click, after clip + hit-test + stability checks.
                    break
                } catch (SelectClosedAfterScroll closed) {
                    List<WebElement> fields = visible(select(label))
                    String actual = fields.size() == 1 ? fields[0].text.trim() : ''
                    int opened = visible(boxes).size()
                    boolean samePage = sameRoute(driver().currentUrl, selectionUrl)
                    navigationLog('DROPDOWN_CLOSED_AFTER_SCROLL', last + [actual:actual, openListboxes:opened, url:safeUrl(driver().currentUrl)])
                    if (!selectReopenAllowed(retry, opened, actual, samePage, last.loading == true) || !authenticated()) {
                        throw new IllegalStateException('Dropdown closed after scroll; controlled reopen unavailable: ' + label)
                    }
                    navigationLog('DROPDOWN_REOPEN_ONCE', [field:label, expected:expected, retry:retry + 1, actual:actual])
                    incrementPerformance('retries')
                    click(select(label)) // No form submission; one reopen shares the existing retry/deadline budget.
                } catch (WebDriverException error) {
                    if (!(error instanceof ElementClickInterceptedException || error instanceof StaleElementReferenceException)) throw error
                    Map details = [:]
                    try {
                        List<WebElement> opened = visible(boxes)
                        List<WebElement> exact = opened.size() == 1 ? opened[0].findElements(By.xpath(selectRelativeOption(expected))) : []
                        if (opened.size() == 1 && exact.size() == 1) details = selectGeometry(opened[0], exact[0])
                    } catch (WebDriverException unavailable) { details.geometryUnavailable = unavailable.class.simpleName }
                    navigationLog('DROPDOWN_CLICK_BLOCKED', last + details + [errorType:error.class.simpleName, retry:retry,
                        url:safeUrl(driver().currentUrl)])
                    // One retry, only while the same field's dropdown remains open. No JS click or reopen.
                    if (!selectRetryAllowed(retry, visible(boxes).size(), true)) throw error
                    incrementPerformance('retries')
                }
            }
            selectionWait(deadline).until {
                List<WebElement> fields = visible(select(label))
                List<String> errors = visible(group(label) + "//*[${cls('oxd-input-field-error-message')}]").collect { it.text.trim() }
                if (errors) throw new IllegalStateException('Select validation in ' + label + ': ' + errors)
                last.actual = fields.size() == 1 ? fields[0].text.trim() : ''
                last.openListboxes = visible(boxes).size()
                // Actual DOM options, not guessed alternatives. Does not claim to observe the HTTP response.
                last.availableOptions = visible(boxes + "//*[@role='option']").collect { it.text.trim() }.take(100)
                return selectVerified(last.actual.toString(), expected, last.openListboxes != 0, errors)
            }
            navigationLog('DROPDOWN_SELECTED', last + [url:safeUrl(driver().currentUrl),
                elapsedMs:(System.nanoTime() - started).intdiv(1000000L)])
        } catch (Throwable error) {
            last.url = '[unavailable]'
            try {
                last.url = safeUrl(driver().currentUrl)
                List<WebElement> fields = visible(select(label))
                last.actual = fields.size() == 1 ? fields[0].text.trim() : '[unavailable]'
                last.openListboxes = visible(boxes).size()
            } catch (Exception unavailable) { last.browserStateUnavailable = unavailable.class.simpleName }
            navigationLog('DROPDOWN_STOPPED', last + [waitingFor:options, errorType:error.class.simpleName,
                elapsedMs:(System.nanoTime() - started).intdiv(1000000L)])
            try { capture('SELECT_' + label + '_ERROR') }
            catch (Throwable unavailable) { navigationLog('SCREENSHOT_UNAVAILABLE', [errorType:unavailable.class.simpleName]) }
            if (error instanceof WebDriverException) throw new IllegalStateException('[SELECT NOT CONFIRMED] ' + caseId +
                '; field=' + label + '; option=' + expected + '; state=' + last.state +
                '; inspect navigation.jsonl and real screenshot; ' + error.class.simpleName)
            throw error
        }
    }
    static String selectListbox(String label) {
        return group(label) + "//*[@role='listbox' and ${cls('oxd-select-dropdown')}]"
    }
    Map selectProbe(String boxes, String option) {
        return (Map) ((JavascriptExecutor) driver()).executeScript('''
            function visible(n) {
                if(!Array.from(n.getClientRects()).some(r=>r.width>0&&r.height>0))return false;
                for(let p=n;p;p=p.parentElement){const s=getComputedStyle(p);if(s.visibility==='hidden'||s.display==='none'||s.opacity==='0')return false;}
                return true;
            }
            function find(xp,root=document) {const s=document.evaluate(xp,root,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null),a=[];for(let i=0;i<s.snapshotLength;i++)a.push(s.snapshotItem(i));return a;}
            const boxes=find(arguments[0]).filter(visible), box=boxes.length===1?boxes[0]:null;
            const matches=box?find(arguments[1],box):[], option=matches.length===1?matches[0]:null;
            return {box:box,option:option,state:{openListboxes:boxes.length,matches:matches.length,
                allOpenListboxes:Array.from(document.querySelectorAll('[role="listbox"]')).filter(visible).length,
                visible:!!option&&visible(option),enabled:!!option&&!option.disabled&&option.getAttribute('aria-disabled')!=='true',
                loading:find(arguments[2]).some(visible)||!!box&&(box.getAttribute('aria-busy')==='true'||!!box.querySelector('[aria-busy="true"],[role="progressbar"]'))}};
        ''', boxes, option, LOADER)
    }
    static String selectOption(String label, String expected) {
        return selectListbox(label) + selectRelativeOption(expected).substring(1)
    }
    static String selectRelativeOption(String expected) {
        return ".//*[@role='option' and ${cls('oxd-select-option')}][normalize-space()=${lit(expected)}]"
    }
    WebDriverWait selectionWait(long deadline) {
        long remaining = deadline - System.nanoTime()
        if (remaining <= 0) throw new TimeoutException('Select deadline exhausted')
        return new WebDriverWait(driver(), Duration.ofNanos(remaining)).pollingEvery(Duration.ofMillis(200))
            .ignoring(StaleElementReferenceException).ignoring(NoSuchElementException)
    }
    static String selectReadiness(Map s) {
        if ((s.openListboxes ?: 0) > 1 || (s.matches ?: 0) > 1) return 'ambiguous'
        if (s.openListboxes != 1) return 'closed'
        if (s.loading == true) return 'loading'
        if (s.matches != 1) return 'missing'
        if (s.visible != true || s.enabled != true) return 'hidden-or-disabled'
        if (s.inside != true) return s.scrollable == true && s.originSafe == true ? 'scroll' : 'clipped'
        if (s.hit != true) return 'blocked'
        return s.stable == true ? 'click' : 'unstable'
    }
    static boolean selectStable(Map before, Map after) {
        return before != null && ['top', 'bottom', 'left', 'right', 'scrollTop'].every {
            before[it] instanceof Number && after[it] instanceof Number && Math.abs(before[it].doubleValue() - after[it].doubleValue()) <= 1d
        }
    }
    static boolean selectRetryAllowed(int retriesUsed, int openListboxes, boolean recoverable) {
        return recoverable && retriesUsed == 0 && openListboxes == 1
    }
    static boolean selectReopenAllowed(int retriesUsed, int openListboxes, String actual, boolean samePage, boolean loading) {
        return retriesUsed == 0 && openListboxes == 0 && actual == '-- Select --' && samePage && !loading
    }
    static int selectScrollDelta(Map g) {
        double distance = (g.top.doubleValue() + g.bottom.doubleValue() - g.clipTop.doubleValue() - g.clipBottom.doubleValue()) / 2d
        double limit = Math.max(16d, (g.clipBottom.doubleValue() - g.clipTop.doubleValue()) * 3d)
        return (int) Math.round(Math.max(-limit, Math.min(limit, distance)))
    }
    static boolean selectVerified(String actual, String expected, boolean open, List errors) {
        return expected != null && expected.trim() && expected != '-- Select --' && actual == expected && !open && errors.empty
    }
    Map selectGeometry(WebElement box, WebElement option) {
        // Read-only DOM measurements/hit-testing. Scrolling uses Selenium WheelInput; never JS click/scrollTop writes.
        return (Map) ((JavascriptExecutor) driver()).executeScript('''
            const box = arguments[0], option = arguments[1];
            const r = option.getClientRects()[0];
            if (!r || !box.contains(option)) return {inside:false, hit:false, scrollable:false};
            let left = 0, top = 0, right = innerWidth, bottom = innerHeight, scroller = null;
            for (let p = option.parentElement; p; p = p.parentElement) {
                const css = getComputedStyle(p), b = p.getBoundingClientRect();
                if (/auto|scroll|hidden|clip/.test(css.overflowY)) {
                    top = Math.max(top, b.top + p.clientTop); bottom = Math.min(bottom, b.top + p.clientTop + p.clientHeight);
                }
                if (/auto|scroll|hidden|clip/.test(css.overflowX)) {
                    left = Math.max(left, b.left + p.clientLeft); right = Math.min(right, b.left + p.clientLeft + p.clientWidth);
                }
                if (!scroller && (p === box || box.contains(p)) && /auto|scroll/.test(css.overflowY) && p.scrollHeight > p.clientHeight + 1) scroller = p;
            }
            // Match WebDriver's in-view center of the first client rect, then test ancestor clipping.
            const x = Math.floor((Math.max(0, r.left) + Math.min(innerWidth, r.right)) / 2);
            const y = Math.floor((Math.max(0, r.top) + Math.min(innerHeight, r.bottom)) / 2);
            const inside = x > left + 1 && x < right - 1 && y > top + 1 && y < bottom - 1;
            const receiver = x >= 0 && x < innerWidth && y >= 0 && y < innerHeight ? document.elementFromPoint(x, y) : null;
            let ox = 0, oy = 0, originSafe = false;
            if (scroller && right > left && bottom > top) {
                const b = scroller.getBoundingClientRect();
                ox = Math.floor((Math.max(left, b.left + scroller.clientLeft) + Math.min(right, b.left + scroller.clientLeft + scroller.clientWidth)) / 2);
                oy = Math.floor((Math.max(top, b.top + scroller.clientTop) + Math.min(bottom, b.top + scroller.clientTop + scroller.clientHeight)) / 2);
                const hit = document.elementFromPoint(ox, oy);
                originSafe = !!hit && box.contains(hit) && scroller.contains(hit);
            }
            const describe = el => el ? {tag:el.tagName.toLowerCase(), role:el.getAttribute('role'), css:(el.getAttribute('class') || '').slice(0,160)} : null;
            return {top:r.top, bottom:r.bottom, left:r.left, right:r.right, clipTop:top, clipBottom:bottom,
                inside:inside, hit:!!receiver && (receiver === option || option.contains(receiver)), centerX:x, centerY:y,
                blocker:receiver && receiver !== option && !option.contains(receiver) ? describe(receiver) : null,
                receiver:describe(receiver), option:describe(option), listbox:describe(box), scrollable:!!scroller,
                scrollTop:scroller ? scroller.scrollTop : 0, originX:ox, originY:oy, originSafe:originSafe};
        ''', box, option)
    }
    void autocomplete(String label, String expected, boolean multi = false) {
        if (label == 'Candidate Name') {
            assert !multi
            measure('SEARCH') { candidateAutocomplete(expected) }
            return
        }
        assert expected?.trim() : '[BLOCKED TEST DATA] Configure exact ' + label + ' name in Profiles/default'
        if (label == 'Hiring Manager') {
            assert !multi : 'Hiring Manager requires a single employee'
            expected = VacancyChecks.normalizeManagerName(expected)
        }
        String options = group(label) + "//div[${cls('oxd-autocomplete-option')}]"
        replace(field(label), '')
        waitFor().until { visible(options).empty }
        // Do not Tab out before selecting; autocomplete closes on blur in some versions.
        WebElement el = one(field(label))
        el.sendKeys(expected.take(Math.min(5, expected.length())))
        String option = options + "[normalize-space()=${lit(expected)}]"
        click(option)
        if (multi) {
            one(group(label) + "//*[${cls('oxd-multiselect-chips-selected')}][normalize-space()=${lit(expected)}]")
        } else if (label == 'Hiring Manager') {
            new VacancyChecks(new RecorderBridge(caseId)).assertManagerValue(expected)
        } else equalsValue(field(label), expected)
        waitFor().until { visible(options).empty }
    }
    static String candidateQuery(String expected) {
        assert expected?.trim()
        return expected.tokenize().max { it.length() } // Owned unique surname token, not a prefix crossing first/last names.
    }
    static boolean candidateNameMatches(String actual, String expected) {
        // Display spacing only; never change name tokens, case, email or numeric record identity.
        return actual != null && expected?.trim() &&
            actual.trim().replaceAll(/[\s\u00a0]+/, ' ') == expected.trim().replaceAll(/[\s\u00a0]+/, ' ')
    }
    static String candidateAutocompleteListbox() {
        return group('Candidate Name') + "//div[${cls('oxd-autocomplete-wrapper')}]//*[@role='listbox' and ${cls('oxd-autocomplete-dropdown')}]"
    }
    static String candidateAutocompleteOptions() {
        return candidateAutocompleteListbox() + "//*[@role='option' and ${cls('oxd-autocomplete-option')}]"
    }
    static boolean autocompletePlaceholder(String label) {
        String value = (label ?: '').trim().toLowerCase().replaceFirst(/[.\u2026]+$/, '')
        return value in ['searching', 'no records found']
    }
    static String autocompleteFailureStage(Map s, boolean clicked) {
        if (s.login == true) return 'authentication_required'
        if (s.inputCount > 1) return 'ambiguous_input'
        if (s.inputPresent == false) return 'input_missing_or_hidden'
        if (clicked) return s.selectionMatches == true && s.openListboxes == 0 ? 'selected' : 'selection_not_confirmed'
        if (s.loading == true) return 'searching_ui'
        if (s.presentListboxes > 0 && s.openListboxes == 0) return 'listbox_hidden'
        if (s.roleListboxes > 0 && s.presentListboxes == 0) return 'listbox_locator_mismatch'
        if (s.openListboxes > 1) return 'ambiguous_listbox'
        if (s.openListboxes == 0) return s.focused == false ? 'closed_after_blur' : 'awaiting_dropdown_after_input'
        if (s.noRecords == true) return 'empty_response_ui'
        if (s.roleOptions > 0 && s.optionCount == 0) return 'option_locator_mismatch'
        if (s.exactMatches > 1) return 'ambiguous_identity'
        if (s.exactMatches == 1) return s.exactEnabled == false ? 'exact_match_not_enabled' : 'exact_suggestion'
        return s.optionCount > 0 ? 'suggestions_without_owned_identity' : 'listbox_without_options'
    }
    protected Map candidateAutocompleteDiagnostic(String expected, String query, boolean clicked) {
        List<WebElement> fields = visible(field('Candidate Name'))
        List<WebElement> boxes = visible(candidateAutocompleteListbox())
        List<WebElement> options = visible(candidateAutocompleteOptions())
        List<String> labels = options.collect { it.text.trim().replaceAll(/[\s\u00a0]+/, ' ') }
        String actual = fields.size() == 1 ? fields[0].getDomProperty('value') ?: '' : ''
        List<String> boxText = boxes.collect { it.text.trim().toLowerCase() }
        int exactIndex = labels.findIndexOf { candidateNameMatches(it, expected) && !autocompletePlaceholder(it) }
        Map diagnostic = [inputCount:fields.size(), inputPresent:fields.size() == 1, typedQueryObserved:actual == query,
            selectionMatches:candidateNameMatches(actual, expected), actualLength:actual.length(), expectedLength:expected.length(),
            actualWhitespaceRuns:(actual =~ /[\s\u00a0]+/).collect { it.length() },
            presentListboxes:driver().findElements(By.xpath(candidateAutocompleteListbox())).size(), openListboxes:boxes.size(),
            roleListboxes:visible(group('Candidate Name') + "//*[@role='listbox']").size(),
            roleOptions:boxes.sum { it.findElements(By.xpath(".//*[@role='option']")).size() } ?: 0,
            optionCount:options.size(), exactMatches:labels.count { candidateNameMatches(it, expected) && !autocompletePlaceholder(it) },
            exactEnabled:exactIndex >= 0 && options[exactIndex].enabled,
            loading:(labels.collect { it.toLowerCase() } + boxText).any { it in ['searching...', 'searching…', 'searching'] },
            noRecords:boxText.any { it in ['no records found', 'no records found.'] },
            requestStatus:'Not captured; UI state does not prove a backend request or indexing rule']
        if (fields.size() == 1 && driver() instanceof JavascriptExecutor) {
            diagnostic.focused = ((JavascriptExecutor) driver()).executeScript('return document.activeElement === arguments[0]', fields[0])
        }
        diagnostic.login = sameRoute(driver().currentUrl, BASE + '/web/index.php/auth/login')
        diagnostic.stage = autocompleteFailureStage(diagnostic, clicked)
        return diagnostic
    }
    protected void candidateAutocomplete(String expected) {
        navigationPhase = 'CANDIDATE_NAME_SEARCH'
        String input = field('Candidate Name')
        String options = candidateAutocompleteOptions()
        navigationLocator = options + ' exact owned candidate name'
        long deadline = System.nanoTime() + Duration.ofSeconds(seconds).toNanos()
        String query = candidateQuery(expected)
        int intercepted = 0
        boolean optionClicked = false
        String actualSelection = ''
        long waitingStarted = System.nanoTime()
        try {
            WebElement control = one(input)
            control.sendKeys(Keys.chord(Keys.CONTROL, 'a'), Keys.BACK_SPACE)
            control.sendKeys(query) // Native input event; keep focus until selecting, no Tab before onSelect.
            navigationLog('CANDIDATE_NAME_INPUT', [queryStrategy:'longest-owned-name-token', queryLength:query.length(),
                debounceMsFromProvidedBundle:800, suggestionCapFromProvidedBundle:5])
            selectionWait(deadline).until {
                List<WebElement> exact = visible(options).findAll {
                    String label = it.text
                    !autocompletePlaceholder(label) && candidateNameMatches(label, expected)
                }
                assert exact.size() <= 1 : 'Duplicate exact Candidate Name suggestions; refusing arbitrary selection'
                if (!exact || !exact[0].enabled) return false
                try { exact[0].click(); optionClicked = true; return true }
                catch (ElementClickInterceptedException blocked) {
                    if (++intercepted > 1) throw blocked
                    incrementPerformance('retries'); return false
                }
            }
            selectionWait(deadline).until {
                List<WebElement> fields = visible(input)
                actualSelection = fields.size() == 1 ? fields[0].getDomProperty('value') : ''
                return fields.size() == 1 && candidateNameMatches(actualSelection, expected) && visible(options).empty
            }
            navigationLog('CANDIDATE_NAME_SELECTED', [queryStrategy:'longest-owned-name-token', url:safeUrl(driver().currentUrl)])
        } catch (Throwable error) {
            if (error instanceof TimeoutException) errorWait(waitingStarted, (TimeoutException) error)
            Map diagnostic = [stage:optionClicked ? 'selection_not_confirmed' : 'suggestion_not_confirmed']
            try { diagnostic.putAll(candidateAutocompleteDiagnostic(expected, query, optionClicked)) }
            catch (Throwable unavailable) { diagnostic.inspectionUnavailable = unavailable.class.simpleName }
            navigationLog('CANDIDATE_NAME_STOPPED', diagnostic + [queryStrategy:'longest-owned-name-token',
                optionClicked:optionClicked, selectionMatches:candidateNameMatches(actualSelection, expected),
                expectedLength:expected.length(), actualLength:actualSelection.length(),
                actualWhitespaceRuns:(actualSelection =~ /[\s\u00a0]+/).collect { it.length() },
                waitingFor:optionClicked ? 'Selected full-name display and closed popup' : 'Unique exact full-name suggestion',
                url:safeUrl(driver().currentUrl)])
            if (error instanceof TimeoutException) {
                IllegalStateException stopped = new IllegalStateException('[CANDIDATE AUTOCOMPLETE NOT CONFIRMED] ' +
                    caseId + '; stage=' + diagnostic.stage + '; optionClicked=' + optionClicked +
                    '; inspect scoped UI diagnostics; do not substitute another identity', error)
                timeoutAlias(error, stopped); throw stopped
            }
            throw error
        }
    }
    void checkbox(String xp, boolean wanted) {
        List<WebElement> all = driver().findElements(By.xpath(xp))
        assert all.size() == 1 : '[NEEDS LIVE VERIFICATION] Missing/ambiguous checkbox: ' + xp
        if (all[0].selected != wanted) {
            // Click its visible label, never inject JS to invent a UI interaction.
            click(xp + '/ancestor::label[1]')
        }
        assert driver().findElement(By.xpath(xp)).selected == wanted
    }
    void error(String groupOrField, String regex) {
        String errors = groupOrField + "/ancestor-or-self::div[${cls('oxd-input-group')}][1]//*[${cls('oxd-input-field-error-message')}]"
        String actual = one(errors).text.trim()
        assert actual ==~ regex : 'Wrong validation message: [' + actual + '], expected /' + regex + '/'
        capture('VALIDATION_AFTER_ACTION')
    }
    void noErrors() {
        assert visible("//*[${cls('oxd-input-field-error-message')}]").empty : 'Unexpected form validation'
        assert visible("//*[${cls('oxd-toast--error')}]").empty : 'Server rejected operation'
    }
    void saveSuccess(String message) {
        saveSuccess(message, [:], null)
    }
    /** A positive Save needs a matching success toast OR the caller's exact record proof.
     * The latter must be supplied explicitly; a click/loader/URL change alone is never proof.
     */
    static String saveEvidence(Map diagnostic, boolean recordProof = false) {
        if (diagnostic.validation || diagnostic.errorToasts) return 'rejected'
        if (diagnostic.submitted != true || diagnostic.loading != false) return 'pending'
        if (recordProof) return 'record'
        return diagnostic.successToast == true || diagnostic.successToastSeen == true ? 'toast' : 'pending'
    }
    void beginSavePhase(Map context) {
        navigationPhase = (context.phase ?: 'SAVE').toString()
        navigationLocator = (context.waitingFor ?: 'Matching success toast OR explicit exact-record proof').toString()
    }
    Map saveDiagnostics(String message, Map context = [:]) {
        // One synchronous DOM snapshot. No WebElements survive the document that supplied them.
        Map raw = (Map) waitFor().until {
            try { return ((JavascriptExecutor) driver()).executeScript('''
            const visible=n=>n.isConnected && Array.from(n.getClientRects()).some(r=>r.width>0&&r.height>0) &&
                getComputedStyle(n).visibility!=='hidden';
            const nodes=s=>Array.from(document.querySelectorAll(s)).filter(visible);
            const label=n=>n.closest('.oxd-input-group')?.querySelector('label')?.textContent.trim() || '(form)';
            const toasts=nodes('.oxd-toast');
            const validation=nodes('form .oxd-input-field-error-message').map(n=>({field:label(n),message:n.textContent.trim()}));
            for(const n of nodes('form input, form textarea, form select'))
                if(n.validationMessage)validation.push({field:label(n),message:n.validationMessage});
            const saves=nodes('form button').filter(n=>n.textContent.trim()==='Save');
            return {url:location.href,title:document.title,
                login:location.pathname.startsWith('/web/index.php/auth/') || !!document.querySelector('input[name="username"]'),
                validation,toasts:toasts.map(n=>n.textContent.trim()),
                errorToasts:toasts.filter(n=>['oxd-toast--error','oxd-toast--warn','oxd-toast--warning'].some(c=>n.classList.contains(c))).map(n=>n.textContent.trim()),
                successToast:toasts.some(n=>n.textContent.includes(arguments[0])),
                saveButtons:saves.map(n=>({enabled:!n.disabled,ariaDisabled:n.getAttribute('aria-disabled')})),
                loading:document.readyState!=='complete' || nodes('.oxd-loading-spinner, .oxd-form-loader').length>0};
        ''', message) }
            catch (WebDriverException error) { if (!detachedDocument(error)) throw error; return null }
        }
        if (raw.login) SessionGuard.lost(context.saveAttempted ? 'Login redirect after Save dispatch' : 'Login redirect before Save')
        return raw + [url:safeUrl(raw.url.toString()), title:raw.title.toString().take(160), fixtureName:context.fixtureName ?: '',
            urlBeforeSave:context.urlBeforeSave,
            saveAttempted:context.saveAttempted == true, submitted:context.submitted == true,
            recordObserved:context.recordObserved == true, recordConfirmed:context.recordConfirmed == true,
            successToastSeen:context.successToastSeen == true,
            validation:raw.validation.unique()] +
            (context.recordDiagnostics ?: [:])
    }
    void saveFailure(Map context, long started, Throwable error) {
        beginSavePhase(context)
        if (context.failureLogged == true) return
        context.failureLogged = true
        Map diagnostic = [fixtureName:context.fixtureName ?: '', saveAttempted:context.saveAttempted == true,
            submitted:context.submitted == true, recordConfirmed:context.recordConfirmed == true]
        try { diagnostic.putAll(saveDiagnostics((context.message ?: 'Successfully Saved').toString(), context)) }
        catch (Exception ex) { diagnostic.browserStateUnavailable = ex.class.simpleName }
        navigationLog('SAVE_STOPPED', diagnostic + [waitingFor:navigationLocator,
            elapsedMs:(System.nanoTime() - started).intdiv(1000000L), errorType:error.class.simpleName])
        try { capture(navigationPhase + '_ERROR') }
        catch (Throwable ex) { navigationLog('SCREENSHOT_UNAVAILABLE', [errorType:ex.class.simpleName]) }
    }
    Map saveSuccess(String message, Map context, Closure recordProof) {
        beginSavePhase(context)
        navigationLocator = "//*[${cls('oxd-toast')}][contains(.,${lit(message)})] OR explicit exact-record proof"
        context.waitingFor = navigationLocator
        context.message = message
        long started = System.nanoTime()
        Map last = [:]
        try {
            ready()
            last = saveDiagnostics(message, context)
            navigationLog('SAVE_PREPARED', last)
            if (saveEvidence(last) == 'rejected') throw new IllegalStateException('[SAVE REJECTED BEFORE SUBMIT] ' +
                caseId + '; validation=' + last.validation + '; errors=' + last.errorToasts)
            if (last.successToast) waitFor().until { !saveDiagnostics(message, context).successToast }
            assert context.saveAttempted != true : 'Save already attempted; reconcile the exact owned record before retrying'
            WebElement save = waitFor().until(EC.elementToBeClickable(By.xpath(button('Save'))))
            assert save.getAttribute('aria-disabled') != 'true' : 'Save is disabled'
            capture(navigationPhase + '_BEFORE_SUBMIT')
            // Journal before dispatch: if click throws, its server outcome is uncertain. Never click again here.
            context.saveAttempted = true
            if (context.beforeSubmit) context.beforeSubmit.call()
            save.click()
            context.submitted = true // Native click returned; this does NOT assert HTTP success.
            if (context.afterSubmit) context.afterSubmit.call()
            navigationLog('SAVE_CLICK_RETURNED', [url:safeUrl(driver().currentUrl), fixtureName:context.fixtureName ?: '', submitted:true])
            String previous = ''
            String evidence = ''
            waitFor().until {
                last = saveDiagnostics(message, context)
                // A toast left over from an earlier operation cannot acknowledge this Save.
                last.successToastPresent = last.successToast == true
                // Success may arrive while the SPA is still loading and disappear before the next poll.
                context.successToastSeen = context.successToastSeen == true || last.successToastPresent == true
                last.successToastSeen = context.successToastSeen
                last.successToast = context.successToastSeen
                if (saveEvidence(last) == 'rejected') throw new IllegalStateException('[SAVE REJECTED] ' + caseId +
                    '; validation=' + last.validation + '; errors=' + last.errorToasts)
                boolean proved = recordProof != null && recordProof.call(last) == true
                context.recordObserved = proved || context.recordIdentified == true
                last.recordObserved = context.recordObserved
                last.putAll(context.recordDiagnostics ?: [:])
                evidence = saveEvidence(last, proved)
                String signature = last.toString()
                if (signature != previous) { navigationLog('SAVE_OBSERVED', last); previous = signature }
                return evidence in ['toast', 'record']
            }
            ready(); noErrors()
            capture(navigationPhase + '_AFTER_ACTION')
            navigationLog('SAVE_EVIDENCE', last + [evidence:evidence, elapsedMs:(System.nanoTime() - started).intdiv(1000000L)])
            return [evidence:evidence, url:last.url]
        } catch (Throwable error) {
            saveFailure(context, started, error)
            if (error instanceof WebDriverException) {
                TimeoutException stopped = new TimeoutException('[SAVE NOT CONFIRMED] ' + caseId +
                '; phase=' + navigationPhase + '; fixture=' + (context.fixtureName ?: '') +
                '; inspect navigation.jsonl and real screenshots; do not resubmit blindly.')
                stopped.initCause(error); timeoutAlias(error, stopped); throw stopped
            }
            throw error
        }
    }
    private boolean captureUnavailableReported = false
    protected void captureUnavailable(String reason) {
        if (captureUnavailableReported) return
        captureUnavailableReported = true
        println('[CAPTURE_UNAVAILABLE] ' + caseId + ': ' + reason)
    }
    protected void captureEvidence(File destination) {
        WebUI.takeScreenshot(destination.absolutePath)
        VacancyChecks.verifyPng(destination)
    }
    void capture(String label) {
        EvidenceCapture.capture(label)
    }

    void track(Map entry) {
        Map observation=new LinkedHashMap(entry)
        if (entry.url) {
            def id=entry.url.toString() =~ /\/(?:addCandidate|addJobVacancy)\/([1-9]\d*)$/
            if (id.find()) observation.numericId=id.group(1)
        }
        observation.observed=entry.saveObserved==true || entry.recordIdentified==true || entry.state in ['observed','identified','verified','confirmed']
        observation.verified=entry.persistedVerified==true || entry.state in ['verified','confirmed']
        observation.uncertain=entry.state=='uncertain'
        ledger << (observation + executionContext.owner(caseId))
        AtomicFiles.json(new File(output,'fixtures.json'),ledger)
    }
    void blocked(String reason) { throw new IllegalStateException('[BLOCKED / NEEDS LIVE VERIFICATION] ' + reason) }
    void finish() { capture('ASSERTIONS_COMPLETED'); println('[ASSERTIONS COMPLETED] ' + caseId + '; inspect actual Katalon report.') }
    void failure(Throwable cause) {
        EvidenceCapture.failure(cause, navigationPhase)
        try { capture('STOPPED') } catch (Throwable unavailable) { captureUnavailable('failure capture: ' + unavailable.class.simpleName) }
        // WebDriverException.toString() appends capabilities and session IDs. Keep these out of helper logs.
        String description = cause instanceof WebDriverException ? cause.class.simpleName + '; phase=' + navigationPhase +
            '; inspect navigation.jsonl / Studio error stack' : cause.toString()
        println('[STOPPED] ' + caseId + ': ' + description + '; ledger=' + new File(output, 'fixtures.json'))
    }

    static String dateText(String iso, String placeholder) {
        String pattern = (placeholder ?: '').toLowerCase()
        assert pattern ==~ /(?:yyyy[-\/]mm[-\/]dd|yyyy[-\/]dd[-\/]mm|dd[-\/]mm[-\/]yyyy|mm[-\/]dd[-\/]yyyy)/ :
            '[NEEDS LIVE VERIFICATION] Unknown date placeholder: ' + placeholder
        List<String> parts = iso.split('-').toList()
        assert parts.size() == 3
        return pattern.replace('yyyy', parts[0]).replace('mm', parts[1]).replace('dd', parts[2])
    }
    static LocalDate parseDate(String text, String placeholder) {
        String pattern = placeholder.toLowerCase().replace('yyyy', 'uuuu').replace('mm', 'MM')
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT))
    }
    String enterDate(String label, String iso, boolean exact = true) {
        String formatted = dateText(iso, one(field(label)).getAttribute('placeholder'))
        replace(field(label), formatted, exact)
        return formatted
    }

    WebElement table(String header) {
        String desktop = column(header) + "/ancestor::div[${cls('oxd-table')}][1]"
        String mobile = "(//div[${cls('oxd-table-card-cell')}]/div[${cls('header')} and normalize-space()=${lit(header)}]/ancestor::div[${cls('oxd-table')}][1])[1]"
        return shown(desktop) ? one(desktop) : one(mobile)
    }
    List<Map<String,String>> pageRows(String header) {
        WebElement t = table(header)
        List<String> headers = t.findElements(By.xpath(".//div[@role='columnheader']")).collect { VacancyTable.normalizeHeaderText(it.text) }
        assert headers.findAll { it }.unique().size() == headers.findAll { it }.size() : 'Duplicate table headers'
        if (!headers.findAll { it }.empty) {
            return t.findElements(By.xpath(ROWS)).collect { row ->
                List<WebElement> cellEls = row.findElements(By.xpath("./div[@role='cell']"))
                assert cellEls.size() == headers.size() : 'Unexpected table layout'
                Map<String,String> result = [:]
                headers.eachWithIndex { String h, int i ->
                    if (h) {
                        List<WebElement> dataNodes = cellEls[i].findElements(By.xpath(".//div[${cls('oxd-table-card-cell')}]/div[${cls('data')}]"))
                        result[h] = VacancyTable.normalizeCellText(dataNodes.size() == 1 ? dataNodes[0].text : cellEls[i].text)
                    }
                }
                result
            }
        }
        return t.findElements(By.xpath(ROWS)).collect { row ->
            Map<String,String> result = [Vacancy:'', Candidate:'', 'Hiring Manager':'', 'Date of Application':'', Status:'']
            row.findElements(By.xpath(".//div[${cls('oxd-table-card-cell')}]")).each { cardCell ->
                List<WebElement> hEl = cardCell.findElements(By.xpath("./div[${cls('header')}]"))
                List<WebElement> dEl = cardCell.findElements(By.xpath("./div[${cls('data')}]"))
                String h = hEl ? VacancyTable.normalizeHeaderText(hEl[0].text) : ''
                if (h) result[h] = dEl ? VacancyTable.normalizeCellText(dEl[0].text) : ''
            }
            result
        }
    }
    boolean nextPage(String header) {
        String path = "//ul[${cls('oxd-pagination__ul')}]//button[.//i[${cls('bi-chevron-right')}]]"
        List<WebElement> next = visible(path).findAll { it.enabled && it.getAttribute('aria-disabled') != 'true' }
        if (!next) return false
        assert next.size() == 1 : 'Ambiguous pagination'
        List<WebElement> oldRows = table(header).findElements(By.xpath(ROWS))
        assert oldRows : 'Next page on empty result'
        next[0].click()
        waitFor().until(EC.stalenessOf(oldRows[0])); ready()
        return true
    }
    List<Map<String,String>> allRows(String header) {
        List<Map<String,String>> result = []
        Set<String> visited = [] as Set
        for (int page = 1; page <= 100; page++) {
            ready()
            if (shown(EMPTY)) {
                assert page == 1 && result.empty : 'Unexpected empty page during pagination'
                return result
            }
            List<Map<String,String>> rows = pageRows(header)
            assert visited.add(rows.toString()) : 'Pagination repeated a page; incomplete oracle'
            result.addAll(rows)
            if (!nextPage(header)) return result
        }
        throw new AssertionError('Pagination exceeds 100 pages; results not fully checked')
    }
    void tableAction(String action, String header = 'Candidate') {
        capture('BEFORE_ACTION_' + action)
        measure(action == 'Reset' ? 'RESET' : 'SEARCH') { tableActionInternal(action, header) }
        capture('AFTER_ACTION_' + action)
    }
    protected void tableActionInternal(String action, String header) {
        if (action == 'Reset') { resetCandidatesTable(header); return }
        ready()
        WebElement oldTable = shown(column(header)) ? table(header) : null
        List<WebElement> oldRows = oldTable ? oldTable.findElements(By.xpath(ROWS)) : []
        WebElement anchor = oldRows ? oldRows[0] : oldTable ?: one(EMPTY)
        // OrangeHRM replaces table rows during Search/Reset. Never accept the old table snapshot.
        click("//form//button[normalize-space()=${lit(action)}]")
        waitFor().until(EC.stalenessOf(anchor))
        ready()
        waitFor().until { shown(EMPTY) || shown(column(header)) }
    }
    Map resetTableState(String header) {
        Map filters = candidateFilters()
        Map state = pageState()
        List<WebElement> bodies = visible(CANDIDATE_MARKERS.tableBody)
        List<WebElement> rows = bodies ? bodies[0].findElements(By.xpath(".//div[@role='row']")) : []
        return [defaults:filtersAreDefault(filters), filterInputsDefault:filterInputsAreDefault(filters),
            page:filters.page, filters:filters, ready:candidateListReady(state) && (rows || shown(EMPTY)),
            rows:rows.size(), signature:rows.collect { it.text }.join('\n')]
    }
    static boolean resetSettled(Map previous, Map current) {
        return current.defaults == true && current.ready == true && previous?.defaults == true &&
            previous.ready == true && current.signature == previous.signature && current.rows == previous.rows
    }
    protected void centeredNativeClick(WebElement control) {
        JavascriptExecutor js = (JavascriptExecutor) driver()
        js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'center',behavior:'instant'});", control)
        Map previous = null
        int adjustments = 0
        waitFor().until {
            Map point = (Map) js.executeScript('''
                const e=arguments[0], r=e.getBoundingClientRect();
                const x=(Math.max(0,r.left)+Math.min(innerWidth,r.right))/2;
                const y=(Math.max(0,r.top)+Math.min(innerHeight,r.bottom))/2;
                const hit=document.elementFromPoint(x,y);
                return {x:x,y:y,w:r.width,h:r.height,clear:!!hit&&(hit===e||e.contains(hit)),header:!!hit&&!!hit.closest('header')};
            ''', control)
            boolean stable = point.clear && point.w > 0 && point.h > 0 && point == previous
            previous = point
            if (!point.clear && point.header==true && adjustments<2) {
                js.executeScript('window.scrollBy(0,arguments[0]-innerHeight*0.65);',point.y)
                adjustments++;previous=null
            }
            return stable
        }
        control.click() // no retry after an uncertain click; no JavaScript click
    }

    protected void resetCandidatesTable(String header) {
        assert header == 'Candidate' : 'Candidates Reset requires its own filter/result contract'
        long deadline = System.nanoTime() + Duration.ofSeconds(seconds).toNanos()
        navigationPhase = 'RESET_CANDIDATES'
        navigationLocator = 'Native Reset; default filters/page 1; ready rows OR empty state, stable across polls'
        selectionWait(deadline).until {
            if (shown(LOADER)) return false
            List<WebElement> controls = visible("//form//button[normalize-space()='Reset']")
            if (!controls) {
                List<WebElement> filterToggle = visible("//div[${cls('oxd-table-filter-header')}]//button")
                if (filterToggle.size() == 1 && filterToggle[0].enabled) {
                    centeredNativeClick(filterToggle[0])
                    return false
                }
            }
            assert controls.size() <= 1 : 'Ambiguous Reset control'
            if (!controls || !controls[0].enabled) return false
            centeredNativeClick(controls[0]); return true
        }
        Map previous = null, last = [:], firstObserved = null
        boolean pageOneClicked = false
        try {
            selectionWait(deadline).until {
                last = resetTableState(header)
                if (firstObserved == null && last.filters instanceof Map) {
                    firstObserved = new LinkedHashMap(last.filters as Map)
                    navigationLog('RESET_OBSERVED_INITIAL', [filters:firstObserved, filterInputsDefault:last.filterInputsDefault, page:last.page])
                }
                if (!pageOneClicked && last.filterInputsDefault == true && last.ready == true &&
                    previous?.filterInputsDefault == true && previous?.ready == true &&
                    last.signature == previous.signature && last.page != null && last.page != '1') {
                    String pageOnePath = "//ul[${cls('oxd-pagination__ul')}]//button[${cls('oxd-pagination-page-item--page')} and normalize-space()='1']"
                    List<WebElement> pageOne = visible(pageOnePath).findAll { it.enabled && it.getAttribute('aria-disabled') != 'true' }
                    if (pageOne.size() == 1) {
                        navigationLog('RESET_PAGE_ONE_REQUIRED', [reason:'OrangeHRM Dt composable preserves currentPage > 1 on onReset when total > 50',
                            pageBefore:last.page, filters:last.filters])
                        centeredNativeClick(pageOne[0])
                        pageOneClicked = true
                        previous = null
                        return false
                    }
                }
                boolean settled = resetSettled(previous, last)
                previous = last
                return settled
            }
            navigationLog('RESET_CONFIRMED', [filtersDefault:last.defaults, filterInputsDefault:last.filterInputsDefault,
                page:last.page, pageOneClicked:pageOneClicked, filtersAfter:last.filters, resultRows:last.rows, empty:last.rows == 0])
        } catch (Throwable error) {
            navigationLog('RESET_STOPPED', [filtersDefault:last.defaults, filterInputsDefault:last.filterInputsDefault,
                page:last.page, pageOneClicked:pageOneClicked, filtersAfter:last.filters, resultRows:last.rows, resultReady:last.ready,
                errorType:error.class.simpleName, waitingFor:navigationLocator])
            throw error
        }
    }
}
