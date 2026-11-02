import recruitment.RecorderBridge
import recruitment.VacancyChecks

// Workbook: search a nonexistent Vacancy. Never replace with incompatible existing filters.
def runner = new RecorderBridge('RECRUIT_VAC_Filter_02')
try {
    runner.step('setTimeout', '30000', '')
    String missingName = 'VAC_NOT_EXIST_' + UUID.randomUUID().toString().replace('-', '').take(12)
    new VacancyChecks(runner).searchMissingVacancy(missingName)
    // Retain the required noncreation/no-results oracle. The supplied select-only UI blocks above.
    runner.step('storeXpathCount', 'xpath=' + VacancyChecks.ROWS, 'filteredRows')
    assert Integer.parseInt(runner.vars['filteredRows']) == 0 : 'Nonexistent Vacancy search returned records'
    runner.step('assertElementPresent', "xpath=//*[normalize-space()='No Records Found']", '')
    runner.finished()
} catch (Throwable failure) {
    runner.failed(failure)
    throw failure
}
