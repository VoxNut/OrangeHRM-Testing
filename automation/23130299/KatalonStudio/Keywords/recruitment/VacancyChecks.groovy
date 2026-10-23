package recruitment

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.WindowType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Opt-in helpers for the eight repaired cases; RecorderBridge commands stay unchanged. */
class VacancyChecks {
    final RecorderBridge runner
    static final String ROWS = "//div[contains(concat(' ',normalize-space(@class),' '),' oxd-table-body ')]//div[@role='row']"
    static final String LOADING = "//*[contains(concat(' ',normalize-space(@class),' '),' oxd-loading-spinner ')]"

    VacancyChecks(RecorderBridge runner) { this.runner = runner }

    static String literal(String value) {
        if (!value.contains("'")) return "'" + value + "'"
        if (!value.contains('"')) return '"' + value + '"'
        return 'concat(' + value.split("'", -1).collect { "'" + it + "'" }.join(',"\'",') + ')'
    }

    static String token(String name) {
        return "contains(concat(' ',normalize-space(@class),' '),' ${name} ')"
    }

    static String group(String label) {
        return "//label[normalize-space()=${literal(label)}]/ancestor::div[${token('oxd-input-group')}][1]"
    }

    static String input(String label) { return 'xpath=' + group(label) + '//input' }
    static String select(String label) { return 'xpath=' + group(label) + "//div[${token('oxd-select-text-input')}]" }

    WebDriverWait waitFor() {
        return new WebDriverWait(runner.driver(), Duration.ofSeconds(runner.seconds))
            .pollingEvery(Duration.ofMillis(200)).ignoring(StaleElementReferenceException)
    }

    void ready() {
        waitFor().until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(LOADING)))
    }

    void capture(String label) {
        String fileName = runner.caseId + '_' + String.format('%03d', ++runner.shotCounter) + '_' + label + '.png'
        runner.screenshot(fileName)
        File png = new File(runner.screenshotDirectory(), runner.caseId + '/' + runner.runStamp + '/' + fileName)
        verifyPng(png)
    }

    static void verifyPng(File png) {
        assert png.isFile() && png.length() > 24 : 'Screenshot was not written: ' + png
        png.withInputStream { stream ->
            DataInputStream data = new DataInputStream(stream)
            assert data.readLong() == 0x89504E470D0A1A0AL : 'Invalid PNG signature: ' + png
            data.readInt() // IHDR length
            assert data.readInt() == 0x49484452 : 'PNG has no IHDR: ' + png
            assert data.readInt() > 0 && data.readInt() > 0 : 'PNG has invalid dimensions: ' + png
        }
    }

    /** Keyboard replacement only for explicitly requested input/textarea; never a global type override. */
    void replaceField(String locator, String value) {
        By by = By.xpath(runner.xp(locator))
        ready()
        WebElement field = waitFor().until(ExpectedConditions.elementToBeClickable(by))
        assert field.tagName in ['input', 'textarea'] : 'Replacement requires input/textarea'
        field.sendKeys(Keys.chord(Keys.CONTROL, 'a'), Keys.BACK_SPACE)
        waitFor().until { runner.driver().findElement(by).getAttribute('value') == '' }
        if (value) runner.driver().findElement(by).sendKeys(value)
        waitFor().until { runner.driver().findElement(by).getAttribute('value') == value }
        assert runner.driver().findElement(by).getAttribute('value') == value : 'Replacement did not reach Vue field'
    }

    void chooseFilter(String label, String expected) {
        ready()
        String field = select(label)
        runner.step('click', field, '')
        String options = group(label) + "//div[${token('oxd-select-option')}][.//span[normalize-space()=${literal(expected)}]]"
        WebElement option = waitFor().until(ExpectedConditions.elementToBeClickable(By.xpath(options)))
        assert runner.driver().findElements(By.xpath(options)).findAll { it.displayed }.size() == 1 :
            "Ambiguous ${label} option: ${expected}"
        option.click()
        runner.step('waitForText', field, expected)
        runner.step('assertText', field, expected)
    }

    /** Bind the list label to our exact vacancy; the list may omit the employee's middle name. */
    void resolveFixtureManager() {
        String fixture = runner.vars['vacancyName']
        String fullName = runner.vars['managerName']?.trim()?.replaceAll(/\s+/, ' ')
        assert fixture && fullName : '[BLOCKED TEST DATA] Fixture identity/manager is missing'
        Set<String> visited = new HashSet<>()
        for (int page = 1; page <= 100; page++) {
            ready()
            List<WebElement> rows = waitFor().until {
                List<WebElement> current = runner.driver().findElements(By.xpath(ROWS))
                return current ? current : null
            }
            String vacancyPath = cell('Vacancy', runner.driver())
            List<WebElement> ownRows = rows.findAll { it.findElement(By.xpath(vacancyPath)).text.trim() == fixture }
            assert ownRows.size() <= 1 : 'Duplicate generated vacancy names: ' + fixture
            if (ownRows) {
                String label = ownRows[0].findElement(By.xpath(cell('Hiring Manager', runner.driver()))).text.trim().replaceAll(/\s+/, ' ')
                List<String> parts = fullName.tokenize(' ')
                String firstLast = parts.size() > 1 ? parts.first() + ' ' + parts.last() : fullName
                assert label && label in [fullName, firstLast] :
                    "[BLOCKED TEST DATA] Fixture manager differs between persisted edit form [${fullName}] and own list row [${label}]"
                runner.vars['managerFilterName'] = label
                println('[FIXTURE MANAGER IDENTITY] vacancy=' + fixture + '; edit=' + fullName + '; list/filter=' + label)
                capture('FIXTURE_MANAGER_IDENTITY_PAGE_' + page)
                if (page > 1) {
                    // Return to a fresh unfiltered list before the scenario sets its target filters.
                    runner.go('/web/index.php/recruitment/viewJobVacancy')
                    waitFor().until { !runner.driver().findElements(By.xpath(ROWS)).isEmpty() }
                    ready()
                }
                return
            }
            String signature = rows.collect { it.findElement(By.xpath(vacancyPath)).text.trim() }.join('\n')
            assert visited.add(signature) : 'Pagination repeated while locating own fixture'
            if (!nextPage(rows)) break
        }
        throw new AssertionError('Persisted fixture not found in list across pages: ' + fixture +
            '. Check shared-demo reset/data deletion before assuming a filter defect.')
    }

    /** Select the exact label taken from this fixture's row, never a first-name substring. */
    void filterFixtureManager() {
        String expected = runner.vars['managerFilterName']
        assert expected?.trim() : '[BLOCKED TEST DATA] Resolve fixture manager from the list before filtering'
        chooseFilter('Hiring Manager', expected)
        runner.vars['selectedManagerFilter'] = expected
        println('[FIXTURE MANAGER FILTER] ' + expected)
    }

    /** Any valid manager may establish a fixture; an update must select a different real full name. */
    String chooseManager(String previous = null) {
        String field = input('Hiring Manager')
        String options = group('Hiring Manager') + "//div[${token('oxd-autocomplete-option')}]"
        for (String query : ['a', 'e', 'i', 'o', 'u']) {
            replaceField(field, '')
            waitFor().until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(options)))
            replaceField(field, query)
            // The previous query's options were removed before entering this query.
            List<WebElement> choices = waitFor().until {
                List<WebElement> shown = runner.driver().findElements(By.xpath(options)).findAll { it.displayed }
                if (!shown || shown.any { it.text.contains('Searching') }) return null
                return shown
            }
            List<String> names = choices.collect { it.text.trim().replaceAll(/\s+/, ' ') }
                .findAll { it && it != 'No Records Found' && it != previous }.unique().sort()
            for (String name : names) {
                String exact = options + "[normalize-space()=${literal(name)}]"
                List<WebElement> matches = runner.driver().findElements(By.xpath(exact)).findAll { it.displayed }
                if (matches.size() != 1) continue // Duplicate display names cannot identify an employee safely.
                waitFor().until(ExpectedConditions.elementToBeClickable(By.xpath(exact))).click()
                runner.step('waitForValue', field, name)
                runner.step('assertValue', field, name)
                assert previous == null || name != previous : 'Manager must change'
                println('[FIXTURE MANAGER] ' + (previous ?: '(new fixture)') + ' -> ' + name)
                return name
            }
        }
        throw new IllegalStateException('[BLOCKED TEST DATA] No unambiguous ' +
            (previous ? 'different Hiring Manager from ' + previous : 'Hiring Manager') +
            ' in autocomplete queries a/e/i/o/u. No Save performed.')
    }

    /** Exact values, including unchanged fields; never normalize textarea/input contents. */
    Map<String, Object> editState() {
        Map<String, Object> state = [:]
        ['Vacancy Name', 'Description', 'Hiring Manager', 'Number of Positions'].each { label ->
            String tag = label == 'Description' ? 'textarea' : 'input'
            state[label] = runner.driver().findElement(By.xpath(group(label) + '//' + tag)).getDomProperty('value')
        }
        state['Job Title'] = runner.driver().findElement(By.xpath(runner.xp(select('Job Title')))).text.trim()
        state['Active'] = runner.driver().findElement(By.xpath(
            "//div[${token('orangerhrm-switch-wrapper')}][.//*[normalize-space()='Active']]//input[@type='checkbox']")).selected
        state['Publish in RSS Feed and Web Page'] = runner.driver().findElement(By.xpath(
            "//div[${token('orangerhrm-switch-wrapper')}][.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input[@type='checkbox']")).selected
        return state
    }

    List<WebElement> visibleEditElements(String path) {
        return runner.driver().findElements(By.xpath(path)).findAll { it.displayed }
    }

    /** Diagnostics are read-only and restricted to the form, Save button and visible toasts. */
    Map editDiagnostics() {
        List<WebElement> toasts = visibleEditElements("//*[${token('oxd-toast')}]")
        List<String> errors = visibleEditElements("//form//*[${token('oxd-input-field-error-message')} or @aria-invalid='true']")
            .collect { it.text ?: (it.getDomProperty('validationMessage') ?: 'aria-invalid=true') }
        visibleEditElements('//form//input | //form//textarea | //form//select').each { field ->
            String message = field.getDomProperty('validationMessage')
            if (message) errors << message
        }
        List<String> toastErrors = toasts.findAll {
            (it.getAttribute('class') ?: '').tokenize().any { it in ['oxd-toast--error', 'oxd-toast--warn', 'oxd-toast--warning'] }
        }.collect { it.text }
        return [url:runner.driver().currentUrl, validation:errors.unique(), toastErrors:toastErrors,
            toasts:toasts.collect { [text:it.text, css:it.getAttribute('class')] },
            loading:!visibleEditElements(LOADING).empty,
            save:visibleEditElements("//form//button[@type='submit' and normalize-space()='Save']")
                .collect { [enabled:it.enabled, disabled:it.getAttribute('aria-disabled')] }]
    }

    void requireNoEditErrors(Map diagnostic) {
        assert diagnostic.validation.empty && diagnostic.toastErrors.empty : '[SAVE REJECTED] ' + diagnostic
    }

    /** Only Update_02/03 opt in. A toast is diagnostic; a fresh server-backed page is the oracle. */
    void saveEdit(Map<String, Object> expected) {
        assert runner.caseId in ['RECRUIT_VAC_Update_02', 'RECRUIT_VAC_Update_03'] : 'Update-only Save helper'
        String fixtureUrl = runner.vars['savedVacancyUrl']
        String fixtureName = runner.vars['vacancyName']
        assert fixtureName ==~ /G01_23130299_\d+_V(22|23)/ : 'Missing own fixture identity'
        assert fixtureUrl == runner.driver().currentUrl && fixtureUrl ==~ /https:\/\/opensource-demo\.orangehrmlive\.com\/web\/index\.php\/recruitment\/addJobVacancy\/\d+/ :
            'Save requires the exact verified fixture URL'
        ready()
        Map before = editState()
        assert expected.keySet() == before.keySet() && expected['Vacancy Name'] == fixtureName : 'Complete expected fixture state required'
        assert before == expected : '[BEFORE SAVE MISMATCH] expected=' + expected + '; actual=' + before
        assert visibleEditElements(group('Hiring Manager') + "//div[${token('oxd-autocomplete-option')}]").empty :
            'Hiring Manager autocomplete must be selected and closed before Save'
        Map diagnostic = editDiagnostics()
        println('[SAVE BEFORE] fields=' + before + '; diagnostics=' + diagnostic)
        requireNoEditErrors(diagnostic)
        By save = By.xpath(group('Vacancy Name') + "/ancestor::form[1]//button[@type='submit' and normalize-space()='Save']")
        assert runner.driver().findElements(save).size() == 1 : 'Ambiguous vacancy Save button'
        WebElement button = waitFor().until(ExpectedConditions.elementToBeClickable(save))
        assert button.getAttribute('aria-disabled') != 'true' : 'Vacancy Save is disabled'
        capture('BEFORE_EDIT_SAVE')
        button.click() // Native WebDriver click; no DOM mutation, JS event or API write.
        println('[SAVE CLICK RETURNED] ' + fixtureUrl + '; persistence not yet verified')

        // Keep the submitting document alive. Reloading it immediately could abort an in-flight Save.
        // The temporary tab only GETs our verified fixture, sharing the authenticated browser session.
        String sourceWindow = runner.driver().windowHandle
        String probeWindow = null
        Duration oldPageTimeout = runner.driver().manage().timeouts().pageLoadTimeout
        long deadline = System.nanoTime() + Duration.ofSeconds(runner.seconds).toNanos()
        Map persisted = [:]
        Map lastPersisted = null
        Map lastDiagnostic = null
        try {
            while (System.nanoTime() < deadline) {
                runner.driver().switchTo().window(sourceWindow)
                diagnostic = editDiagnostics()
                if (diagnostic != lastDiagnostic) println('[SAVE PROCESSING] ' + diagnostic)
                lastDiagnostic = diagnostic
                requireNoEditErrors(diagnostic)
                long remaining = Math.max(1L, deadline - System.nanoTime())
                runner.driver().manage().timeouts().pageLoadTimeout(Duration.ofNanos(remaining))
                if (probeWindow == null) {
                    runner.driver().switchTo().newWindow(WindowType.TAB)
                    probeWindow = runner.driver().windowHandle
                    runner.driver().get(fixtureUrl)
                } else {
                    runner.driver().switchTo().window(probeWindow)
                    runner.driver().navigate().refresh()
                }
                remaining = Math.max(1L, deadline - System.nanoTime())
                new WebDriverWait(runner.driver(), Duration.ofNanos(remaining))
                    .pollingEvery(Duration.ofMillis(200)).ignoring(StaleElementReferenceException).until {
                        requireNoEditErrors(editDiagnostics())
                        if (visibleEditElements("//h6[normalize-space()='Edit Vacancy']").empty || !visibleEditElements(LOADING).empty) return false
                        persisted = editState()
                        return persisted['Vacancy Name'] && persisted['Hiring Manager'] &&
                            persisted['Job Title'] && persisted['Job Title'] != '-- Select --'
                    }
                assert runner.driver().currentUrl == fixtureUrl && persisted['Vacancy Name'] == fixtureName :
                    '[FIXTURE IDENTITY MISMATCH] URL=' + runner.driver().currentUrl + '; actual=' + persisted
                if (persisted == expected) break
                if (persisted != lastPersisted) println('[SAVE NOT YET PERSISTED] expected=' + expected + '; actual=' + persisted)
                lastPersisted = persisted
                Thread.sleep(200L) // Bounded polling cadence, never a success condition.
            }
            runner.driver().switchTo().window(sourceWindow)
            requireNoEditErrors(editDiagnostics())
            assert persisted == expected : '[SAVE PERSISTENCE FAILED] url=' + fixtureUrl + '; expected=' + expected + '; actual=' + persisted
            println('[PERSISTENCE VERIFIED] fresh fixture page=' + fixtureUrl + '; fields=' + persisted)
        } catch (Throwable failure) {
            println('[SAVE FAILED] expected=' + expected + '; lastFreshRead=' + persisted + '; lastSubmitDiagnostics=' + lastDiagnostic)
            throw failure // Includes timeouts: never convert a failure into Passed.
        } finally {
            if (probeWindow != null && runner.driver().windowHandles.contains(probeWindow)) {
                runner.driver().switchTo().window(probeWindow)
                runner.driver().close()
            }
            runner.driver().switchTo().window(sourceWindow)
            runner.driver().manage().timeouts().pageLoadTimeout(oldPageTimeout)
        }
        ready()
        capture('AFTER_EDIT_SAVE')
    }

    /** Wait for replacement of old rows, then completion of the request, rather than a fixed pause. */
    void tableAction(String action) {
        ready()
        List<WebElement> oldRows = runner.driver().findElements(By.xpath(ROWS))
        assert oldRows : 'Expected nonempty table before ' + action
        WebUI.click(runner.obj("xpath=//form//button[normalize-space()=${literal(action)}]"))
        waitFor().until(ExpectedConditions.stalenessOf(oldRows[0]))
        ready()
        waitFor().until { !runner.driver().findElements(By.xpath(ROWS)).isEmpty() }
        capture('AFTER_' + action)
    }

    static List<WebElement> headers(def driver) {
        return driver.findElements(By.xpath("//div[@role='columnheader']"))
    }

    static int columnIndex(def driver, String label) {
        List<String> labels = headers(driver).collect { it.text.trim().replaceAll(/\s+/, ' ') }
        int index = labels.indexOf(label)
        assert index >= 0 && labels.count(label) == 1 : "Unknown/ambiguous table header ${label}: ${labels}"
        return index + 1
    }

    static String cell(String label, def driver) {
        return "./div[@role='cell'][${columnIndex(driver, label)}]"
    }

    boolean nextPage(List<WebElement> rows) {
        List<WebElement> next = runner.driver().findElements(By.xpath(
            "//ul[${token('oxd-pagination__ul')}]//button[.//i[${token('bi-chevron-right')}]]"))
            .findAll { it.displayed && it.enabled && it.getAttribute('aria-disabled') != 'true' }
        if (!next) return false
        assert next.size() == 1 : 'Ambiguous Next pagination button'
        next[0].click()
        waitFor().until(ExpectedConditions.stalenessOf(rows[0]))
        return true
    }

    /** Check every result page. Exact-vacancy cases retain exact-name existence assertions. */
    void assertResults(Map<String, String> expected, List<String> requiredVacancies = []) {
        Set<String> visited = new HashSet<>()
        Set<String> found = new HashSet<>()
        int total = 0
        for (int page = 1; page <= 100; page++) {
            ready()
            List<WebElement> rows = waitFor().until {
                List<WebElement> current = runner.driver().findElements(By.xpath(ROWS))
                return current ? current : null
            }
            Map<String, String> paths = expected.collectEntries { key, value -> [(key): cell(key, runner.driver())] }
            String vacancyPath = cell('Vacancy', runner.driver())
            List<String> names = []
            rows.each { row ->
                String vacancy = row.findElement(By.xpath(vacancyPath)).text.trim()
                names.add(vacancy)
                found.add(vacancy)
                expected.each { label, wanted ->
                    String actual = row.findElement(By.xpath(paths[label])).text.trim().replaceAll(/\s+/, ' ')
                    assert actual == wanted : "${label} mismatch on page ${page}, Vacancy ${vacancy}: expected [${wanted}], actual [${actual}]"
                }
            }
            assert visited.add(names.join('\n')) : 'Pagination repeated a page; refusing an endless loop'
            total += rows.size()
            capture('RESULT_PAGE_' + page)
            if (!nextPage(rows)) {
                assert total > 0 : 'No search results'
                assert found.containsAll(requiredVacancies) : 'Required vacancies missing from all result pages: ' + (requiredVacancies - found)
                println('[RESULTS VERIFIED] rows=' + total + '; filters=' + expected + '; requiredVacancies=' + requiredVacancies)
                return
            }
        }
        throw new AssertionError('Pagination exceeded 100 pages; validation incomplete')
    }

    String firstDifferentVacancy(String fixtureName) {
        ready()
        waitFor().until { !runner.driver().findElements(By.xpath(ROWS)).isEmpty() }
        String path = cell('Vacancy', runner.driver())
        List<String> names = runner.driver().findElements(By.xpath(ROWS))
            .collect { it.findElement(By.xpath(path)).text.trim() }.findAll { it && it != fixtureName }
        assert names : '[BLOCKED TEST DATA] Reset needs a visible control vacancy distinct from own fixture'
        return names[0] // Read-only control from the unfiltered list; never edited.
    }

    void assertResetDefaults() {
        ['Job Title', 'Vacancy', 'Hiring Manager', 'Status'].each { label ->
            runner.step('waitForText', select(label), '-- Select --')
            runner.step('assertText', select(label), '-- Select --')
        }
    }
}
