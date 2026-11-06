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
    final Map<String,String> editAttempts = [:]
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
        VacancyTable.click(runner.driver(),option,runner.seconds)
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

    /** Display formatting only for Hiring Manager; never use for IDs, email or other inputs. */
    static String normalizeManagerName(String value) {
        return (value ?: '').trim().replaceAll(/\s+/, ' ')
    }

    static void assertManagerName(String actual, String expected) {
        String name = normalizeManagerName(expected)
        assert name && normalizeManagerName(actual) == name :
            'Hiring Manager mismatch expected [' + expected + '] actual [' + actual + ']'
    }

    /** Compare the full selected/persisted name; return its verified raw value for legacy VAC callers. */
    String assertManagerValue(String expected) {
        assert normalizeManagerName(expected) : '[BLOCKED TEST DATA] Hiring Manager name is empty'
        String field = input('Hiring Manager')
        runner.step('waitForValue', field, expected)
        String actual = WebUI.getAttribute(runner.obj(field), 'value') ?: ''
        assertManagerName(actual, expected)
        println('[HIRING MANAGER VALUE] expected=[' + expected + ']; actual=[' + actual + ']')
        return actual
    }

    String chooseConfiguredManager(String setting = 'hiringManagerName') {
        assert setting in ['hiringManagerName','hiringManagerAlternativeName']
        String expected = RecruitmentUi.config(setting, '').toString().trim()
        if (!expected) {
            Map discovered = discoverAndVerifyManager(setting, null, null, null, true)
            if (discovered.state in ['VERIFIED', 'READY'] && discovered.displayName) {
                expected = discovered.displayName.toString().trim()
            }
        }
        if (!expected) {
            throw new IllegalStateException('[CONFIGURATION BLOCKED / NO_VERIFIED_HIRING_MANAGER] ' + runner.caseId +
                ' requires ' + setting + '. Neither manual override in Profiles/default nor verified auto-discovery succeeded; preflight listener prevents repeated fixture setup. RECRUIT_VAC_Create_05 intentionally omits Manager; RECRUIT_CAND_Create_05 still requires its Vacancy fixture.')
        }
        String selected = selectApprovedManager(expected)
        assertDistinctManager(setting)
        return selected
    }
    String selectApprovedManager(String expected) {
        return (String) runner.measure('AUTOCOMPLETE') { selectManagerIdentity(expected) }
    }
    protected String selectManagerIdentity(String expected) {
        assert expected?.trim() : 'Exact employee name is required'
        String field = input('Hiring Manager')
        Map initial=managerProbe(expected)
        if (initial.login) SessionGuard.lost('Hiring Manager preflight redirected to Login')
        if (initial.inputs!=1) throw new IllegalStateException('[CONFIGURATION PREFLIGHT / LOCATOR_MISMATCH] Hiring Manager input absent/ambiguous; no Save')
        String query = managerQuery(expected)
        Map last=[:]
        List<String> queries = [query]
        List<String> nameParts = normalizeManagerName(expected).tokenize(' ')
        if (nameParts.size()>2) queries << nameParts.first()+' '+nameParts.last()
        queries = queries.unique()
        for (int index=0;index<queries.size();index++) {
        query = queries[index]
        replaceField(field, query)
        try {
            waitFor().until {
                last=managerProbe(expected)
                String state=managerPreflightState(last)
                if (state=='AUTHENTICATION') SessionGuard.lost('Hiring Manager autocomplete lost Login')
                if (state in ['AMBIGUOUS','INACTIVE','LOCATOR_MISMATCH','NOT_FOUND']) throw new IllegalStateException('[CONFIGURATION PREFLIGHT / '+state+'] '+runner.caseId+': Exact active Hiring Manager not confirmed; no Save')
                state=='READY'
            }
            break
        } catch (IllegalStateException unavailable) {
            if (!(unavailable.message?.contains('CONFIGURATION PREFLIGHT / NOT_FOUND')) || index == queries.size()-1) throw unavailable
            runner.journal([kind:'manager-query-refinement',expected:expected,query:query,state:'NOT_FOUND',submitted:false])
        } catch(org.openqa.selenium.TimeoutException timeout) {
            runner.journal([kind:'manager-preflight',state:managerPreflightState(last),expected:expected,diagnostic:last,submitted:false])
            throw new IllegalStateException('[CONFIGURATION PREFLIGHT / '+managerPreflightState(last)+'] '+runner.caseId+': Hiring Manager suggestion not confirmed before timeout; no Save',timeout)
        }
        }
        String verifiedLabel = normalizeManagerName((last.matchedLabel ?: expected).toString())
        ExecutionContext c = runner.executionContext
        List pool = (c.managerDiscovery?.verifiedPool ?: []) + [c.managerDiscovery?.managerA,c.managerDiscovery?.managerB].findAll { it }
        Map cached = pool.find { it.state == 'VERIFIED' && normalizeManagerName(it.displayName?.toString()) == verifiedLabel }
        Map identity = EmployeeIdentity.resolve(runner.driver(), '/api/v2/pim/employees', query, verifiedLabel, cached?.employeeId?.toString())
        String options = group('Hiring Manager') + "//div[${token('oxd-autocomplete-option')}][normalize-space()=${literal(verifiedLabel)}]"
        runner.journal([kind:'manager-preflight',state:'READY',expected:expected,verifiedLabel:verifiedLabel,submitted:false])
        // Re-locate after preflight; one native exact suggestion click, never the first option.
        List<WebElement> matches=runner.driver().findElements(By.xpath(options)).findAll { it.displayed }
        assert matches.size()==1 : 'Hiring Manager suggestion changed after preflight'
        VacancyTable.click(runner.driver(), matches[0], runner.seconds)
        String selected = assertManagerValue(verifiedLabel)
        waitFor().until { visibleEditElements(group('Hiring Manager') + "//*[@role='listbox']").empty }
        c.managerDiscovery.lastSelection = [displayName:verifiedLabel, employeeId:identity.id.toString(), query:query, state:'VERIFIED']
        if (cached) cached.employeeId = identity.id.toString()
        c.persist()
        return selected
    }

    /** Reuse the query that yielded this exact verified full label; never weaken identity matching. */
    String managerQuery(String expected) {
        String full=normalizeManagerName(expected)
        ExecutionContext c=runner.executionContext
        List pool=(c.managerDiscovery?.verifiedPool ?: []) + [c.managerDiscovery?.managerA,c.managerDiscovery?.managerB].findAll { it }
        Map verified=pool.find { it.state=='VERIFIED' && normalizeManagerName(it.displayName?.toString())==full }
        if (verified?.searchQuery) return verified.searchQuery.toString()
        String query=normalizeManagerName(verified?.tableDisplayName?.toString())
        List parts=full.tokenize(' ')
        if (query && parts.size()>2 && query==parts.first()+' '+parts.last() && !VacancyTable.isExcludedManagerCandidate(query)) return query
        full
    }
    void assertDistinctManager(String setting) {
        if (setting == 'hiringManagerAlternativeName') {
            Map discovery = runner.executionContext.managerDiscovery
            assert discovery.managerA?.employeeId && discovery.lastSelection?.employeeId &&
                discovery.managerA.employeeId.toString() != discovery.lastSelection.employeeId.toString() :
                'Manager A/B must have verified distinct employee IDs; no Save'
        }
    }

    List<Map<String, Object>> discoverManagerCandidatesFromVacancyTable(int maxPages = 5) {
        ExecutionContext c = runner.executionContext
        runner.go('/web/index.php/recruitment/viewJobVacancy')
        List<Map<String, Object>> collected = []
        Set<String> seenNames = new LinkedHashSet<>()
        Set<String> visitedPages = new LinkedHashSet<>()
        for (int page = 1; page <= maxPages; page++) {
            waitFor().until { tableState().ready == true }
            List<Map<String, String>> rows = VacancyTable.readRows(runner.driver(), ".orangehrm-vacancy-list[role='table'], [role='table']")
            String sig = rows.collect { (it['Vacancy'] ?: '') + '|' + (it['Hiring Manager'] ?: '') }.join('\n')
            if (!visitedPages.add(sig)) break
            List<Map<String, Object>> pageCandidates = VacancyTable.extractManagerCandidates(rows, page, c.executionId)
            pageCandidates.each { Map<String, Object> cand ->
                if (seenNames.add(cand.candidateName.toString())) collected << cand
            }
            if (collected.size() >= 6) break
            List<WebElement> rowEls = runner.driver().findElements(By.xpath(ROWS))
            if (!rowEls || !nextPage(rowEls)) break
        }
        c.managerDiscovery.candidates = collected
        c.persist()
        runner.journal([kind:'manager-discovery-table', candidateCount:collected.size(), candidates:collected, submitted:false])
        return collected
    }

    Map discoverAndVerifyManager(String setting = 'hiringManagerName', String previousManager = null,
                                 Closure tableReader = null, Closure candidateVerifier = null, boolean stayOnCurrentForm = false) {
        assert setting in ['hiringManagerName', 'hiringManagerAlternativeName']
        // Production discovery is PIM-first. Keep explicit closure injection for the existing
        // offline harness; historical Vacancy rows are no longer a production precondition.
        if(tableReader==null && candidateVerifier==null)
            return new PimEmployeeFixtures(runner).manager(setting,previousManager,stayOnCurrentForm)
        ExecutionContext c = runner.executionContext
        String exclude = normalizeManagerName(previousManager ?: (setting == 'hiringManagerAlternativeName' ?
            (c.managerDiscovery?.managerA?.displayName ?: c.managerDiscovery?.managerA?.tableDisplayName ?: RecruitmentUi.config('hiringManagerName', '').toString()) : ''))
        if (setting == 'hiringManagerName' && c.managerDiscovery?.managerA?.state == 'VERIFIED' && !exclude) {
            return (Map) c.managerDiscovery.managerA
        }
        if (setting == 'hiringManagerAlternativeName' && c.managerDiscovery?.managerB?.state == 'VERIFIED') {
            String cachedB = normalizeManagerName(c.managerDiscovery.managerB.displayName?.toString())
            if (!exclude || cachedB != exclude) return (Map) c.managerDiscovery.managerB
        }
        String currentUrlBefore = ''
        if (stayOnCurrentForm && tableReader == null && candidateVerifier == null) {
            try { currentUrlBefore = runner.driver().currentUrl ?: '' } catch (Exception ignored) {}
        }
        List<Map<String, Object>> candidates = (c.managerDiscovery?.candidates as List<Map<String, Object>>) ?: []
        if (candidates.isEmpty()) {
            candidates = (List<Map<String, Object>>) (tableReader != null ? tableReader.call() : discoverManagerCandidatesFromVacancyTable())
            c.managerDiscovery.candidates = candidates
        }
        if (candidateVerifier == null) {
            if (stayOnCurrentForm && currentUrlBefore.contains('/recruitment/addJobVacancy')) {
                if (!runner.driver().currentUrl?.contains('/recruitment/addJobVacancy')) runner.go(currentUrlBefore)
            } else {
                runner.go('/web/index.php/recruitment/addJobVacancy')
                waitFor().until { !runner.driver().findElements(By.xpath("//h6[normalize-space()='Add Vacancy' or normalize-space()='Edit Vacancy']")).empty }
            }
        }
        List<Map> rejected = []
        for (Map<String, Object> cand in candidates) {
            String candidateName = normalizeManagerName(cand.candidateName?.toString())
            if (VacancyTable.isExcludedManagerCandidate(candidateName)) continue
            if (exclude && candidateName == exclude) continue
            try {
                String verifiedName = candidateVerifier != null ?
                    candidateVerifier.call(candidateName)?.toString() :
                    selectApprovedManager(candidateName)
                verifiedName = normalizeManagerName(verifiedName)
                if (!verifiedName || VacancyTable.isExcludedManagerCandidate(verifiedName)) continue
                if (exclude && verifiedName == exclude) continue
                if (exclude && c.managerDiscovery?.managerA?.employeeId && c.managerDiscovery?.lastSelection?.employeeId) {
                    assert c.managerDiscovery.managerA.employeeId.toString() != c.managerDiscovery.lastSelection.employeeId.toString() :
                        'Manager B resolves to Manager A employee ID; no Save'
                }
                Map record = [
                    state: 'VERIFIED',
                    status: 'READY',
                    displayName: verifiedName,
                    selected: verifiedName,
                    tableDisplayName: candidateName,
                    searchQuery: c.managerDiscovery?.lastSelection?.displayName == verifiedName ? c.managerDiscovery.lastSelection.query : candidateName,
                    employeeId: c.managerDiscovery?.lastSelection?.displayName == verifiedName ? c.managerDiscovery.lastSelection.employeeId : (cand.employeeId ?: null),
                    verificationMethod: 'Add Vacancy EmployeeAutocomplete exact unique active suggestion (no Save)',
                    discoverySource: cand.discoverySource ?: 'Recruitment > Vacancies table',
                    vacancyName: cand.vacancyName ?: '',
                    jobTitle: cand.jobTitle ?: '',
                    vacancyStatus: cand.vacancyStatus ?: '',
                    pageIndex: cand.pageIndex ?: 1,
                    verifiedTimestamp: java.time.OffsetDateTime.now().toString(),
                    executionId: c.executionId,
                    submitted: false
                ]
                if (setting == 'hiringManagerAlternativeName' || exclude) {
                    c.managerDiscovery.managerB = record
                } else {
                    c.managerDiscovery.managerA = record
                }
                List pool = (c.managerDiscovery.verifiedPool as List) ?: []
                if (!pool.any { normalizeManagerName(it.displayName?.toString()) == verifiedName }) pool << record
                c.managerDiscovery.verifiedPool = pool
                c.managerDiscovery.rejected = rejected
                c.managerDiscovery.status = 'VERIFIED'
                if (c.preflight?.identities != null) {
                    c.preflight.identities[setting] = [state:'READY', verificationStatus:'VERIFIED', mode:'AUTO_DISCOVER', selected:verifiedName] + record
                }
                c.persist()
                runner.journal([kind:'manager-discovered-verified', setting:setting] + record)
                return record
            } catch (Exception ex) {
                if (!(ex instanceof IllegalStateException) || !(ex.message =~ /CONFIGURATION PREFLIGHT \/ (NOT_FOUND|INACTIVE)\]/)) throw ex
                rejected << [candidateName:candidateName, reason:ex.message ?: ex.class.simpleName]
            }
        }
        c.managerDiscovery.status = 'NO_VERIFIED_HIRING_MANAGER'
        c.managerDiscovery.rejected = rejected
        c.persist()
        return [state:'NO_VERIFIED_HIRING_MANAGER', status:'NO_VERIFIED_HIRING_MANAGER', setting:setting, rejected:rejected, executionId:c.executionId, submitted:false]
    }

    static String managerPreflightState(Map s) {
        if (s.login) return 'AUTHENTICATION'
        if (s.inputs!=1) return 'LOCATOR_MISMATCH'
        if (s.loading) return 'AUTOCOMPLETE_LOADING'
        if ((s.inactive ?: 0)>0) return 'INACTIVE'
        if ((s.matches ?: 0)>1) return 'AMBIGUOUS'
        if (s.matches==1 && s.enabled==true && s.listboxes==1) return 'READY'
        if (s.noRecords) return 'NOT_FOUND'
        return 'NOT_LOADED_OR_NO_EXACT_SUGGESTION'
    }
    Map managerProbe(String expected) {
        return (Map)((org.openqa.selenium.JavascriptExecutor)runner.driver()).executeScript('''
            const xp=arguments[0], expected=arguments[1], norm=s=>(s||'').trim().replace(/\\s+/g,' ');
            const firstLast=s=>{const p=norm(s).split(' ').filter(Boolean);return p.length>1?p[0]+' '+p[p.length-1]:norm(s);};
            const snap=document.evaluate(xp,document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);
            const groups=Array.from({length:snap.snapshotLength},(_,i)=>snap.snapshotItem(i));
            const group=groups.length===1?groups[0]:null;
            const visible=n=>n.isConnected&&Array.from(n.getClientRects()).some(r=>r.width>0&&r.height>0);
            const options=group?Array.from(group.querySelectorAll('[role="option"]')).filter(visible):[];
            const labels=options.map(n=>norm(n.textContent));
            const activeOptions=options.filter(n=>!/Past Employee|Deleted|^Searching|^No Records Found$/i.test(norm(n.textContent)));
            const exact=activeOptions.filter(n=>norm(n.textContent)===expected || firstLast(n.textContent)===expected);
            const matchedLabels=exact.map(n=>norm(n.textContent));
            return {login:location.pathname.includes('/auth/')||!!document.querySelector('input[name="username"]'),
                inputs:group?group.querySelectorAll('input').length:0,listboxes:group?Array.from(group.querySelectorAll('[role="listbox"]')).filter(visible).length:0,
                loading:labels.some(s=>/^Searching(?:\\.\\.\\.|…)?$/i.test(s)),noRecords:labels.some(s=>/^No Records Found$/i.test(s)),
                matches:exact.length,enabled:exact.length===1&&exact[0].getAttribute('aria-disabled')!=='true',
                matchedLabel:matchedLabels.length===1?matchedLabels[0]:null,
                inactive:labels.filter(s=>(s.startsWith(expected)||firstLast(s)===expected)&&/Past Employee|Deleted/i.test(s)).length,labels};
        ''',group('Hiring Manager'),normalizeManagerName(expected))
    }

    /** Workbook Filter_02 requires an absent Vacancy name. The supplied page has a select, not a text search. */
    void searchMissingVacancy(String name) {
        assert name?.trim()
        runner.go('/web/index.php/recruitment/viewJobVacancy')
        ready()
        String path = group('Vacancy')
        assert runner.driver().findElements(By.xpath(path)).size() == 1 : 'Vacancy filter missing/ambiguous'
        capture('BEFORE_ACTION_MISSING_VACANCY_SEARCH')
        throw new IllegalStateException('[BLOCKED / WORKBOOK-UI MISMATCH] REQUIREMENT_DISCREPANCY: RECRUIT_VAC_Filter_02 requires searching nonexistent Vacancy [' + name +
            ']. Supplied viewJobVacancy.html and app.js use VacancyDropdown (select by existing id); no free-text Vacancy filter exists. No substitute fixture, incompatible Job Title or synthetic option was used. Requires an approved workbook/UI resolution; No Records Found remains unverified.')
    }

    /** Legacy signature retained for locked scripts; fixture identity is approved configuration or verified auto-discovery, never option order. */
    String chooseManager(String previous = null) {
        String setting=previous ? 'hiringManagerAlternativeName' : 'hiringManagerName'
        String expected=RecruitmentUi.config(setting,'').toString().trim()
        if (!expected || (previous && normalizeManagerName(expected)==normalizeManagerName(previous))) {
            Map discovered = discoverAndVerifyManager(setting, previous, null, null, true)
            if (discovered.state in ['VERIFIED', 'READY'] && discovered.displayName) {
                expected = discovered.displayName.toString().trim()
            }
        }
        ConfigurationPreflight.requireConfigured(setting,runner.caseId)
        if (previous) assert normalizeManagerName(expected)!=normalizeManagerName(previous) : 'Configured MANAGER_B must differ from MANAGER_A'
        String selected = selectApprovedManager(expected)
        assertDistinctManager(setting)
        return selected
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

    static boolean ownedFixtureName(String name,String namespace) {
        namespace && name?.startsWith('G01_23130299_'+namespace+'_') && name.length()<=50
    }

    /** Only Update_02/03 opt in. A toast is diagnostic; a fresh server-backed page is the oracle. */
    void saveEdit(Map<String, Object> expected) {
        assert runner.caseId.startsWith('RECRUIT_VAC_') : 'Vacancy edit helper'
        String fixtureUrl = runner.vars['savedVacancyUrl']
        String fixtureName = runner.vars['vacancyName']
        assert ownedFixtureName(fixtureName,runner.namespaceToken) : 'Missing own execution fixture identity'
        assert runner.ledger.any { it.name==fixtureName && it.url==RecruitmentUi.safeUrl(fixtureUrl) && it.state in ['observed','verified'] } : 'Owned numeric Vacancy was not observed by this runner'
        assert fixtureUrl == runner.driver().currentUrl && CandidateChecks.vacancyRecordUrl(fixtureUrl) :
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
        String attemptKey=fixtureUrl+'|'+new com.google.gson.Gson().toJson(new TreeMap(expected))
        assert !editAttempts.containsKey(attemptKey) : 'Edit already attempted; reconcile persisted state instead of resubmitting'
        editAttempts[attemptKey]='submitted'
        runner.journal([name:fixtureName,url:RecruitmentUi.safeUrl(fixtureUrl),state:'submitted',operation:'edit',submitted:false])
        try { VacancyTable.click(runner.driver(),button,runner.seconds) }
        catch(Throwable original) {
            editAttempts[attemptKey]='uncertain'
            runner.journal([name:fixtureName,url:RecruitmentUi.safeUrl(fixtureUrl),state:'uncertain',operation:'edit',submitted:false])
            throw original
        }
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
        Throwable primaryFailure = null
        try {
            new WebDriverWait(runner.driver(), Duration.ofSeconds(runner.seconds)).pollingEvery(Duration.ofMillis(200)).until {
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
                if (persisted == expected) return true
                if (persisted != lastPersisted) println('[SAVE NOT YET PERSISTED] expected=' + expected + '; actual=' + persisted)
                lastPersisted = persisted
                return false // WebDriverWait supplies bounded polling; success exits immediately.
            }
            runner.driver().switchTo().window(sourceWindow)
            requireNoEditErrors(editDiagnostics())
            assert persisted == expected : '[SAVE PERSISTENCE FAILED] url=' + fixtureUrl + '; expected=' + expected + '; actual=' + persisted
            editAttempts[attemptKey]='verified'
            runner.journal([name:fixtureName,url:RecruitmentUi.safeUrl(fixtureUrl),state:'verified',operation:'edit',submitted:true,verified:true])
            Map selection=runner.executionContext.managerDiscovery.lastSelection as Map
            if(selection && normalizeManagerName(selection.displayName?.toString())==normalizeManagerName(expected['Hiring Manager']?.toString()))
                PimEmployeeFixtures.dependency('vacancy',new URI(fixtureUrl).path.tokenize('/')[-1],selection.employeeId?.toString())
            println('[PERSISTENCE VERIFIED] fresh fixture page=' + fixtureUrl + '; fields=' + persisted)
        } catch (Throwable failure) {
            primaryFailure=failure
            editAttempts[attemptKey]='uncertain'
            runner.journal([name:fixtureName,url:RecruitmentUi.safeUrl(fixtureUrl),state:'uncertain',operation:'edit',submitted:true])
            println('[SAVE FAILED] expected=' + expected + '; lastFreshRead=' + persisted + '; lastSubmitDiagnostics=' + lastDiagnostic)
            throw failure // Includes timeouts: never convert a failure into Passed.
        } finally {
            try {
                if (probeWindow != null && runner.driver().windowHandles.contains(probeWindow)) {
                    runner.driver().switchTo().window(probeWindow)
                    runner.driver().close()
                }
                runner.driver().switchTo().window(sourceWindow)
                runner.driver().manage().timeouts().pageLoadTimeout(oldPageTimeout)
            } catch(Throwable cleanupFailure) {
                if (primaryFailure) primaryFailure.addSuppressed(cleanupFailure)
                else throw cleanupFailure
            }
        }

        ready()
        capture('AFTER_EDIT_SAVE')
    }

    /** Loader/row mutations plus settled results; Vue may retain every row element. */
    void tableAction(String action) {
        assert action in ['Search','Reset']
        ready()
        List<WebElement> controls=visibleEditElements("//form//button[normalize-space()=${literal(action)}]")
        assert controls.size()==1 && controls[0].enabled : 'Missing/ambiguous table action '+action
        VacancyTable.refresh(runner.driver(),controls[0],runner.seconds)
        capture('AFTER_'+action)
    }

    static List<WebElement> headers(def driver) {
        return driver.findElements(By.xpath("//div[@role='columnheader']"))
    }

    static int columnIndex(def driver, String label) {
        List<String> labels = headers(driver).collect { VacancyTable.normalizeHeaderText(it.text) }
        int index = labels.indexOf(label)
        assert index >= 0 && labels.count(label) == 1 : "Unknown/ambiguous table header ${label}: ${labels}"
        return index + 1
    }

    static String cell(String label, def driver) {
        List<WebElement> hdrs = headers(driver)
        if (!hdrs.isEmpty()) return "./div[@role='cell'][${columnIndex(driver, label)}]"
        return ".//div[${token('oxd-table-card-cell')}][./div[${token('header')} and normalize-space()=${literal(label)}]]/div[${token('data')}]"
    }

    boolean nextPage(List<WebElement> rows) {
        List<WebElement> next = runner.driver().findElements(By.xpath(
            "//ul[${token('oxd-pagination__ul')}]//button[.//i[${token('bi-chevron-right')}]]"))
            .findAll { it.displayed && it.enabled && it.getAttribute('aria-disabled') != 'true' }
        if (!next) return false
        assert next.size() == 1 : 'Ambiguous Next pagination button'
        VacancyTable.refresh(runner.driver(),next[0],runner.seconds)
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

    Map tableState() { VacancyTable.state(runner.driver()) }

    /** Negative/Cancel oracle: scan every displayed result page for this run's exact generated name. */
    void assertAbsent(String name) {
        assert ownedFixtureName(name,runner.namespaceToken) : 'Absence checks require this runner\'s generated name'
        runner.go('/web/index.php/recruitment/viewJobVacancy')
        Set<String> visited=[] as Set
        for (int page=1;page<=100;page++) {
            waitFor().until { tableState().ready==true }
            List<WebElement> rows=runner.driver().findElements(By.xpath(ROWS))
            if (!rows) {
                assert tableState().rows==0 : 'Vacancy list not settled'
                capture('AFTER_ACTION_NONCREATION_VERIFIED');return
            }
            String path=cell('Vacancy',runner.driver())
            List<String> names=rows.collect { it.findElement(By.xpath(path)).text.trim() }
            assert !names.contains(name) : 'Negative/Cancel unexpectedly persisted own Vacancy '+name
            assert visited.add(names.join('\n')) : 'Repeated page; absence not proven'
            if (!nextPage(rows)) { capture('AFTER_ACTION_NONCREATION_VERIFIED');return }
        }
        throw new AssertionError('Absence scan exceeded 100 pages; incomplete proof')
    }
}
