package recruitment

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import java.time.Duration

/** Read-only identity proof during an owner-run UI test. Never creates or changes an employee.
 * Endpoint/query contracts come from EmployeeAutocomplete and InterviewerAutocomplete.
 * A native suggestion click and selected input assertion are still mandatory at the caller.
 */
class EmployeeIdentity {
    static class NotFound extends IllegalStateException {
        NotFound(String reason) { super(reason) }
    }
    static final Set<String> ENDPOINTS = ['/api/v2/pim/employees', '/api/v2/recruitment/interviewers'] as Set

    static Map exact(List<Map> employees, String selectedLabel, String previousId = null) {
        String name = VacancyChecks.normalizeManagerName(selectedLabel)
        List<Map> matches = employees.findAll { VacancyChecks.normalizeManagerName(it.label?.toString()) == name }
        assert name && matches.size() == 1 : '[IDENTITY AMBIGUOUS OR MISSING] Exact selected employee must resolve once'
        Map identity = matches[0]
        assert identity.id?.toString() ==~ /[1-9]\d*/ : '[IDENTITY INVALID] Numeric employee empNumber required'
        assert identity.active == true : '[IDENTITY NOT ACTIVE] Selected employee is terminated/deleted'
        assert !previousId || identity.id.toString() == previousId : '[IDENTITY CHANGED] Cached employee was deleted/replaced; no Save'
        return new LinkedHashMap(identity)
    }

    static Map resolve(WebDriver driver, String endpoint, String query, String selectedLabel, String previousId = null) {
        assert ENDPOINTS.contains(endpoint) && query?.trim() && query.length() < 100
        Map response=ApiReadTransport.request(driver,endpoint,
            [nameOrId:query.trim()]+(endpoint=='/api/v2/pim/employees'?[includeEmployees:'onlyCurrent']:[:]),false)
        List<Map> employees=response.data.collect {Map e ->
            [id:e.empNumber,label:[e.firstName,e.middleName,e.lastName].findAll {it}.join(' '),active:e.terminationId==null]
        }
        return exact(employees,selectedLabel,previousId)+[query:query,endpoint:endpoint]
    }
}
