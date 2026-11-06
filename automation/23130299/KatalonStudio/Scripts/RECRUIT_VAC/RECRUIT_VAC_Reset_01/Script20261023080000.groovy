
import recruitment.RecorderBridge
import recruitment.VacancyChecks

def runner = new RecorderBridge('RECRUIT_VAC_Reset_01')

def checks = new VacancyChecks(runner)
try {
    runner.step('setTimeout', '30000', '')
    runner.step('echo', 'START RECRUIT_VAC_Reset_01 (original TC_RECRUIT_VAC_021)', '')

    runner.step('open', '/web/index.php/recruitment/viewJobVacancy', '')
    runner.step('waitForElementPresent', "xpath=//button[normalize-space()='Add']", '')

    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_01_BEFORE_CREATE_ADD.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_01_BEFORE_CREATE_ADD.png', '')

    runner.step('click', "xpath=//button[normalize-space()='Add']", '')
    runner.step('waitForElementPresent', "xpath=//h6[normalize-space()='Add Vacancy']", '')

    runner.step('storeEval', "'G01_23130299_'+new Date().getTime()+'_V21'", 'vacancyName')
    runner.step('echo', 'FIXTURE ${vacancyName} / RECRUIT_VAC_Reset_01', '')

    def jobTitleField = "xpath=//label[normalize-space()='Job Title']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//div[contains(concat(' ',normalize-space(@class),' '),' oxd-select-text-input ')]"

    def jobTitleOption = "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' oxd-select-option ')][.//span[normalize-space()='QA Engineer']]"

    def vacancyNameField = "xpath=//label[normalize-space()='Vacancy Name']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input"

    def descriptionField = "xpath=//label[normalize-space()='Description']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//textarea"

    def hiringManagerInput = "xpath=//label[normalize-space()='Hiring Manager']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input[@placeholder='Type for hints...']"

    def positionField = "xpath=//label[normalize-space()='Number of Positions']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' oxd-input-group ')][1]//input"

    def firstManagerSuggestion = "xpath=(//div[contains(concat(' ',normalize-space(@class),' '),' oxd-autocomplete-option ')][.//span[normalize-space()!='' and not(contains(.,'No Records Found')) and not(contains(.,'Searching'))]])[1]"

    runner.step('click', jobTitleField, '')
    runner.step('waitForElementPresent', jobTitleOption, '')
    runner.step('click', jobTitleOption, '')
    runner.step('assertText', jobTitleField, 'QA Engineer')

    runner.step('type', vacancyNameField, '${vacancyName}')
    runner.step('type', descriptionField, 'Filter fixture QA / unique own run')

    runner.vars['managerName'] = checks.chooseManager()

    runner.step('type', positionField, '3')

    runner.step('ensureChecked', "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' orangerhrm-switch-wrapper ')][.//*[normalize-space()='Active']]//input[@type='checkbox']", '')

    runner.step('ensureUnchecked', "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' orangerhrm-switch-wrapper ')][.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input[@type='checkbox']", '')

    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_02_BEFORE_SETUP_CREATE_SAVE.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_02_BEFORE_SETUP_CREATE_SAVE.png', '')

    runner.step('click', "xpath=//form[contains(@class,'oxd-form')]//button[@type='submit' and normalize-space()='Save']", '')

    runner.step('waitForElementPresent', "xpath=//h6[normalize-space()='Edit Vacancy']", '')
    runner.step('waitForValue', vacancyNameField, '${vacancyName}')
    runner.step('assertValue', vacancyNameField, '${vacancyName}')

    runner.step('storeLocation', 'savedVacancyUrl', '')
    runner.step('assertText', jobTitleField, 'QA Engineer')
    runner.step('waitForValue', hiringManagerInput, '${managerName}')
    runner.step('assertValue', positionField, '3')

    runner.step('assertChecked', "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' orangerhrm-switch-wrapper ')][.//*[normalize-space()='Active']]//input[@type='checkbox']", '')
    runner.step('echo', 'SETUP CREATED: ${vacancyName} at ${savedVacancyUrl}; CLEANUP ONLY OWN RECORD AFTER EVIDENCE', '')

    runner.step('store', '${vacancyName}', 'filterVac')
    runner.step('store', '${managerName}', 'filterManagerFull')

    runner.step('open', '/web/index.php/recruitment/viewJobVacancy', '')
    runner.step('waitForElementPresent', "xpath=//h5[normalize-space()='Vacancies']", '')
    checks.ready()
    checks.assertResetDefaults()
    checks.resolveFixtureManager()
    String controlVacancy = checks.firstDifferentVacancy(runner.vars['vacancyName'])
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_03_BEFORE_RESET.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_03_BEFORE_RESET.png', '')
    checks.tableAction('Reset')
    checks.assertResetDefaults()
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_04_BEFORE_FILTER_JobTitle.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_04_BEFORE_FILTER_JobTitle.png', '')
    checks.chooseFilter('Job Title', 'QA Engineer')
    checks.chooseFilter('Vacancy', runner.vars['vacancyName'])
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_05_BEFORE_FILTER_HiringManager.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_05_BEFORE_FILTER_HiringManager.png', '')
    checks.filterFixtureManager()
    checks.chooseFilter('Status', 'Active')
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_06_BEFORE_SEARCH.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_06_BEFORE_SEARCH.png', '')
    checks.tableAction('Search')
    checks.assertResults(['Job Title':'QA Engineer', 'Vacancy':runner.vars['vacancyName'],
        'Hiring Manager':runner.vars['selectedManagerFilter'], 'Status':'Active'], [runner.vars['vacancyName']])
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Reset_01_07_BEFORE_RESET.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Reset_01_07_BEFORE_RESET.png', '')
    checks.tableAction('Reset')
    checks.assertResetDefaults()
    // Both own fixture and a previously observed unrelated record must be restored, across pages.
    checks.assertResults([:], [runner.vars['vacancyName'], controlVacancy])
    runner.step('echo', 'ASSERTIONS COMPLETED: RECRUIT_VAC_Reset_01; inspect screenshot tab and keep own fixture ledger.', '')

    runner.finished()

} catch (Throwable t) {
    runner.failed(t)
    throw t
}
