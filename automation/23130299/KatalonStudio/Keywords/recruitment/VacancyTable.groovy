package recruitment

import org.openqa.selenium.*
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Observe Vue refresh without requiring the table or row nodes to be replaced. */
class VacancyTable {
    static Map state(WebDriver driver) {
        (Map) ((JavascriptExecutor)driver).executeScript('''
            const visible=e=>!!e&&e.getClientRects().length>0;
            const rows=Array.from(document.querySelectorAll('.oxd-table-body [role="row"]')).filter(visible);
            const busy=Array.from(document.querySelectorAll('.oxd-loading-spinner,.oxd-form-loader')).some(visible);
            const empty=Array.from(document.querySelectorAll('.orangehrm-horizontal-padding')).some(e=>visible(e)&&e.textContent.includes('No Records Found'));
            const selected=document.querySelector('.oxd-pagination-page-item--page-selected');
            return {login:location.pathname.includes('/auth/login'),busy:busy,ready:!busy&&(rows.length>0||empty),
                rows:rows.length,signature:rows.map(e=>e.textContent).join('\\n'),page:selected?selected.textContent.trim():'1'};
        ''')
    }
    static boolean settled(Map before, Map previous, Map now, boolean observed) {
        now.ready == true && previous?.ready == true && now.signature == previous.signature && now.page == previous.page &&
            (observed || now.signature != before.signature || now.page != before.page)
    }
    static void click(WebDriver driver, WebElement element, int seconds) {
        JavascriptExecutor js=(JavascriptExecutor)driver
        js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest',behavior:'instant'});",element)
        Map previous=null
        new WebDriverWait(driver,Duration.ofSeconds(seconds)).until {
            Map point=(Map)js.executeScript('''
                const e=arguments[0],r=e.getBoundingClientRect(),x=(Math.max(0,r.left)+Math.min(innerWidth,r.right))/2,y=(Math.max(0,r.top)+Math.min(innerHeight,r.bottom))/2;
                const hit=document.elementFromPoint(x,y);
                return {x:x,y:y,w:r.width,h:r.height,clear:!!hit&&(hit===e||e.contains(hit))};
            ''',element)
            boolean clear=point.clear && point.w>0 && point.h>0 && previous==point
            previous=point;clear
        }
        element.click() // one dispatch; never replay an uncertain click
    }
    static void refresh(WebDriver driver, WebElement control, int seconds) {
        Map before=state(driver),previous=null
        String key='__asm7Table_'+UUID.randomUUID().toString().replace('-','')
        JavascriptExecutor js=(JavascriptExecutor)driver
        // Observer sees short-lived loaders and child mutations even between WebDriver polls.
        js.executeScript('''
            const key=arguments[0],root=document.querySelector('.oxd-layout-context');
            if(!root)throw Error('Recruitment context absent');
            const record={changed:false};
            record.observer=new MutationObserver(changes=>{
                for(const m of changes){
                    const target=m.target.nodeType===1?m.target:m.target.parentElement;
                    if(target&&target.closest('.oxd-table-body,.oxd-pagination__ul,.oxd-form'))record.changed=true;
                    for(const n of [...m.addedNodes,...m.removedNodes])if(n.nodeType===1&&(n.matches('.oxd-loading-spinner,.oxd-form-loader')||n.querySelector('.oxd-loading-spinner,.oxd-form-loader')))record.changed=true;
                }
            });
            record.observer.observe(root,{childList:true,subtree:true,characterData:true,attributes:true,attributeFilter:['disabled','class']});
            window[key]=record;
        ''',key)
        try {
            click(driver,control,seconds)
            new WebDriverWait(driver,Duration.ofSeconds(seconds)).until {
                Map now=state(driver)
                if(now.login) SessionGuard.lost('Vacancy table refresh redirected to Login')
                boolean observed=js.executeScript('return !!window[arguments[0]]?.changed;',key)==true
                boolean done=settled(before,previous,now,observed);previous=now;done
            }
        } finally {
            try { js.executeScript('window[arguments[0]]?.observer.disconnect();delete window[arguments[0]];',key) }
            catch(WebDriverException ignored) { /* preserve original refresh failure */ }
        }
    }

    static String normalizeHeaderText(String raw) {
        return (raw ?: '').replaceAll(/(?i)\b(?:Ascending|Descending)\b/, '').replaceAll(/[\s\u00a0]+/, ' ').trim()
    }

    static String normalizeCellText(String raw) {
        return (raw ?: '').replaceAll(/[\s\u00a0]+/, ' ').trim()
    }

    static boolean isExcludedManagerCandidate(String raw) {
        String value = normalizeCellText(raw)
        if (!value || value.length() >= 100) return true
        if (!(value =~ /[A-Za-zÀ-ỹ]/)) return true
        if (value in ['-- Select --', '--', 'N/A', '(Deleted)', 'Deleted', 'No Records Found', 'Searching...', 'Searching…', 'Searching']) return true
        if (value =~ /(?i)(?:\(\s*Deleted\s*\)|\bDeleted\b|\bPast\s+Employee\b|\bPurged\s+Employee\b|^No\s+Records\s+Found$|^Searching)/) return true
        return false
    }

    static String ownHeaderText(org.w3c.dom.Node headerNode) {
        if (headerNode == null) return ''
        StringBuilder sb = new StringBuilder()
        def children = headerNode.childNodes
        for (int i = 0; i < children.length; i++) {
            def child = children.item(i)
            if (child.nodeType == org.w3c.dom.Node.TEXT_NODE) {
                sb.append(child.nodeValue ?: '')
            } else if (child.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                String cls = child.attributes?.getNamedItem('class')?.nodeValue ?: ''
                if (!cls.contains('oxd-table-header-sort')) sb.append(' ').append(ownHeaderText(child))
            }
        }
        return normalizeHeaderText(sb.toString())
    }

    static Map<String, Integer> mapDesktopHeaders(org.w3c.dom.Node docOrTable) {
        def xp = javax.xml.xpath.XPathFactory.newInstance().newXPath()
        def headers = (org.w3c.dom.NodeList) xp.evaluate(".//div[@role='columnheader']", docOrTable, javax.xml.xpath.XPathConstants.NODESET)
        Map<String, Integer> mapping = new LinkedHashMap<>()
        for (int i = 0; i < headers.length; i++) {
            String label = ownHeaderText(headers.item(i))
            if (label) {
                assert !mapping.containsKey(label) : "Duplicate table columnheader: ${label}"
                mapping[label] = i
            }
        }
        return mapping
    }

    static List<Map<String, String>> parseDomRows(org.w3c.dom.Node docOrTable, String tableClassFilter = null) {
        def xp = javax.xml.xpath.XPathFactory.newInstance().newXPath()
        org.w3c.dom.Node table = docOrTable
        String tableQuery = tableClassFilter ?
            ".//div[@role='table' and contains(concat(' ',normalize-space(@class),' '),' ${tableClassFilter} ')]" :
            ".//div[@role='table']"
        def tables = (org.w3c.dom.NodeList) xp.evaluate(tableQuery, docOrTable, javax.xml.xpath.XPathConstants.NODESET)
        if (tables.length > 0) table = tables.item(0)
        Map<String, Integer> headerMap = mapDesktopHeaders(table)
        def rowNodes = (org.w3c.dom.NodeList) xp.evaluate(".//div[contains(concat(' ',normalize-space(@class),' '),' oxd-table-body ')]//div[@role='row']", table, javax.xml.xpath.XPathConstants.NODESET)
        List<Map<String, String>> result = []
        for (int r = 0; r < rowNodes.length; r++) {
            org.w3c.dom.Node row = rowNodes.item(r)
            Map<String, String> rowMap = new LinkedHashMap<>()
            if (!headerMap.isEmpty()) {
                def cells = (org.w3c.dom.NodeList) xp.evaluate("./div[@role='cell']", row, javax.xml.xpath.XPathConstants.NODESET)
                headerMap.each { String h, Integer idx ->
                    if (idx < cells.length) {
                        org.w3c.dom.Node cell = cells.item(idx)
                        def dataNodes = (org.w3c.dom.NodeList) xp.evaluate(".//div[contains(concat(' ',normalize-space(@class),' '),' oxd-table-card-cell ')]/div[contains(concat(' ',normalize-space(@class),' '),' data ')]", cell, javax.xml.xpath.XPathConstants.NODESET)
                        String val = dataNodes.length == 1 ? dataNodes.item(0).textContent : cell.textContent
                        rowMap[h] = normalizeCellText(val)
                    }
                }
            } else {
                def cardCells = (org.w3c.dom.NodeList) xp.evaluate(".//div[contains(concat(' ',normalize-space(@class),' '),' oxd-table-card-cell ')]", row, javax.xml.xpath.XPathConstants.NODESET)
                for (int c = 0; c < cardCells.length; c++) {
                    org.w3c.dom.Node cardCell = cardCells.item(c)
                    String h = normalizeHeaderText((String) xp.evaluate("./div[contains(concat(' ',normalize-space(@class),' '),' header ')]", cardCell, javax.xml.xpath.XPathConstants.STRING))
                    String d = normalizeCellText((String) xp.evaluate("./div[contains(concat(' ',normalize-space(@class),' '),' data ')]", cardCell, javax.xml.xpath.XPathConstants.STRING))
                    if (h) rowMap[h] = d
                }
            }
            def icons = (org.w3c.dom.NodeList) xp.evaluate(".//button//i/@class", row, javax.xml.xpath.XPathConstants.NODESET)
            List<String> actions = []
            for (int a = 0; a < icons.length; a++) {
                String ic = icons.item(a).nodeValue ?: ''
                if (ic.contains('bi-eye')) actions << 'view'
                if (ic.contains('bi-pencil')) actions << 'edit'
                if (ic.contains('bi-trash')) actions << 'delete'
                if (ic.contains('bi-download')) actions << 'download'
            }
            if (actions) rowMap['_actions'] = actions.unique().join(',')
            String candidateIdAttr = normalizeCellText((String) xp.evaluate(".//@data-candidate-id | .//a[contains(@href,'/addCandidate/')]/@href", row, javax.xml.xpath.XPathConstants.STRING))
            if (candidateIdAttr) {
                def m = candidateIdAttr =~ /(\d+)$/
                if (m.find()) rowMap['_candidateId'] = m.group(1)
            }
            result << rowMap
        }
        return result
    }

    static List<Map<String, String>> readRows(WebDriver driver, String tableSelector = ".orangehrm-vacancy-list[role='table'], [role='table']") {
        List<Map> raw = (List<Map>) ((JavascriptExecutor) driver).executeScript('''
            const norm = s => (s || '').replace(/[\\s\\u00a0]+/g, ' ').trim();
            const normHeader = el => {
                if (!el) return '';
                const clone = el.cloneNode(true);
                clone.querySelectorAll('.oxd-table-header-sort').forEach(n => n.remove());
                return norm(clone.textContent).replace(/\\b(?:Ascending|Descending)\\b/gi, '').trim();
            };
            const visible = el => !!el && Array.from(el.getClientRects()).some(r => r.width > 0 && r.height > 0);
            const tables = Array.from(document.querySelectorAll(arguments[0])).filter(visible);
            const table = tables[0] || document.querySelector(arguments[0]);
            if (!table) return [];
            const headerEls = Array.from(table.querySelectorAll('[role="columnheader"]')).filter(visible);
            const headerMap = {};
            headerEls.forEach((el, idx) => {
                const h = normHeader(el);
                if (h) headerMap[h] = idx;
            });
            const hasDesktopHeaders = Object.keys(headerMap).length > 0;
            const rows = Array.from(table.querySelectorAll('.oxd-table-body [role="row"]')).filter(visible);
            return rows.map(row => {
                const out = {};
                if (hasDesktopHeaders) {
                    const cells = Array.from(row.children).filter(c => c.getAttribute('role') === 'cell');
                    for (const [h, idx] of Object.entries(headerMap)) {
                        if (idx < cells.length) {
                            const dataEl = cells[idx].querySelector('.oxd-table-card-cell .data');
                            out[h] = norm(dataEl ? dataEl.textContent : cells[idx].textContent);
                        }
                    }
                } else {
                    row.querySelectorAll('.oxd-table-card-cell').forEach(cardCell => {
                        const hEl = cardCell.querySelector('.header');
                        const dEl = cardCell.querySelector('.data');
                        const h = hEl ? normHeader(hEl) : '';
                        if (h) out[h] = norm(dEl ? dEl.textContent : '');
                    });
                }
                return out;
            });
        ''', tableSelector)
        return (raw ?: []).collect { Map r ->
            Map<String, String> clean = new LinkedHashMap<>()
            r.each { k, v -> clean[k.toString()] = v == null ? '' : v.toString() }
            clean
        }
    }

    static List<Map<String, Object>> extractManagerCandidates(List<Map<String, String>> rows, int pageIndex = 1, String executionId = '') {
        List<Map<String, Object>> candidates = []
        Set<String> seen = new LinkedHashSet<>()
        (rows ?: []).eachWithIndex { Map<String, String> row, int rowIdx ->
            String rawManager = row['Hiring Manager']
            String normalized = VacancyChecks.normalizeManagerName(rawManager)
            if (!isExcludedManagerCandidate(normalized) && seen.add(normalized)) {
                candidates << [
                    candidateName: normalized,
                    vacancyName: normalizeCellText(row['Vacancy']),
                    jobTitle: normalizeCellText(row['Job Title']),
                    hiringManager: normalized,
                    vacancyStatus: normalizeCellText(row['Status']),
                    pageIndex: pageIndex,
                    rowIndex: rowIdx,
                    executionId: executionId,
                    statusIsVacancyOnly: true,
                    employeeActiveVerified: false
                ]
            }
        }
        return candidates
    }
}
