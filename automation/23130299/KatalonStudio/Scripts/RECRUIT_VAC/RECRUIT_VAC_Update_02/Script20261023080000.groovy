import recruitment.RecorderBridge
import recruitment.VacancyChecks
import com.kms.katalon.core.configuration.RunConfiguration
import java.security.MessageDigest

def runner = new RecorderBridge('RECRUIT_VAC_Update_02')
def checks = new VacancyChecks(runner)
// Diagnose the actual instance, not a newly compiled substitute. Fail before creating a fixture if stale.
def verifySaveContract = { String stage ->
    Class actual = checks.getClass()
    def methods = actual.getMethods().findAll { it.name == 'saveEdit' }
    File project = new File(RunConfiguration.getProjectDir()).canonicalFile
    File source = new File(project, 'Keywords/recruitment/VacancyChecks.groovy')
    println('[RUNTIME STAGE] ' + stage + '; case=' + runner.caseId)
    println('[RUNTIME PROJECT] ' + project)
    println('[RUNTIME CLASS] ' + actual.name)
    println('[RUNTIME LOADER] ' + actual.classLoader)
    println('[RUNTIME CODE SOURCE] ' + actual.protectionDomain?.codeSource?.location)
    println('[RUNTIME RESOURCE] ' + actual.getResource('/recruitment/VacancyChecks.class'))
    println('[RUNTIME RESOURCES] ' + Collections.list(actual.classLoader.getResources('recruitment/VacancyChecks.class')))
    println('[RUNTIME METHODS] ' + methods.collect { it.toGenericString() })
    println('[RUNTIME PARAMETER TYPES] ' + methods.collect { it.parameterTypes*.name })
    println('[RUNTIME SOURCE] ' + source + '; modified=' + new Date(source.lastModified()) +
        '; sha256=' + (source.isFile() ? MessageDigest.getInstance('SHA-256').digest(source.bytes).encodeHex() : 'MISSING'))
    assert actual.name == 'recruitment.VacancyChecks' && methods.any { it.parameterTypes.toList() == [Map] } :
        '[RUNTIME CLASS MISMATCH] saveEdit(java.util.Map) missing. Stop execution; refresh/reopen this project in Studio. No fallback Save.'
    println('[RUNTIME SAVE CONTRACT VERIFIED] saveEdit(java.util.Map); ' + stage)
}
try {
    runner.step('setTimeout', '30000', '')
    runner.step('echo', 'START RECRUIT_VAC_Update_02 (original TC_RECRUIT_VAC_022)', '')
    verifySaveContract('BEFORE_FIXTURE')
    runner.step('open', '/web/index.php/recruitment/viewJobVacancy', '')
    runner.step('waitForElementPresent', 'xpath=//button[normalize-space()=\'Add\']', '')
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Update_02_01_BEFORE_CREATE_ADD.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Update_02_01_BEFORE_CREATE_ADD.png', '')
    runner.step('click', 'xpath=//button[normalize-space()=\'Add\']', '')
    runner.step('waitForElementPresent', 'xpath=//h6[normalize-space()=\'Add Vacancy\']', '')
    runner.step('storeEval', '\'G01_23130299_\'+new Date().getTime()+\'_V22\'', 'vacancyName')
    runner.step('echo', 'FIXTURE ${vacancyName} / RECRUIT_VAC_Update_02', '')
    runner.step('click', 'xpath=//label[normalize-space()=\'Job Title\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-select-text-input \')]', '')
    runner.step('waitForElementPresent', 'xpath=//div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-select-option \')][.//span[normalize-space()="QA Engineer"]]', '')
    runner.step('click', 'xpath=//div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-select-option \')][.//span[normalize-space()="QA Engineer"]]', '')
    runner.step('assertText', 'xpath=//label[normalize-space()=\'Job Title\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-select-text-input \')]', 'QA Engineer')
    runner.step('type', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('type', 'xpath=//label[normalize-space()=\'Description\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//textarea', 'Before update description')
    runner.vars['managerName'] = checks.chooseManager()
    runner.step('type', 'xpath=//label[normalize-space()=\'Number of Positions\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '3')
    runner.step('ensureChecked', 'xpath=//div[contains(concat(\' \',normalize-space(@class),\' \'),\' orangerhrm-switch-wrapper \')][.//*[normalize-space()=\'Active\']]//input[@type=\'checkbox\']', '')
    runner.step('ensureUnchecked', 'xpath=//div[contains(concat(\' \',normalize-space(@class),\' \'),\' orangerhrm-switch-wrapper \')][.//*[contains(normalize-space(),\'Publish in RSS Feed\')]]//input[@type=\'checkbox\']', '')
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Update_02_02_BEFORE_SETUP_CREATE_SAVE.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Update_02_02_BEFORE_SETUP_CREATE_SAVE.png', '')
    runner.step('click', 'xpath=//form[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-form \')]//button[@type=\'submit\' and normalize-space()=\'Save\']', '')
    runner.step('waitForElementPresent', 'xpath=//h6[normalize-space()=\'Edit Vacancy\']', '')
    runner.step('waitForValue', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('assertValue', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('storeLocation', 'savedVacancyUrl', '')
    runner.step('assertText', 'xpath=//label[normalize-space()=\'Job Title\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-select-text-input \')]', 'QA Engineer')
    runner.step('waitForValue', 'xpath=//label[normalize-space()=\'Hiring Manager\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input[@placeholder=\'Type for hints...\']', '${managerName}')
    runner.step('assertValue', 'xpath=//label[normalize-space()=\'Number of Positions\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '3')
    runner.step('echo', 'SETUP CREATED: ${vacancyName} at ${savedVacancyUrl}; CLEANUP ONLY OWN RECORD AFTER EVIDENCE', '')
    runner.step('open', '${savedVacancyUrl}', '')
    runner.step('waitForElementPresent', 'xpath=//h6[normalize-space()=\'Edit Vacancy\']', '')
    runner.step('waitForValue', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('assertValue', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('storeEval', '\'Description updated \'+new Date().getTime()', 'newDescription')
    checks.replaceField('xpath=' + VacancyChecks.group('Description') + '//textarea', runner.vars['newDescription'])
    runner.step('assertValue', 'xpath=' + VacancyChecks.group('Description') + '//textarea', '${newDescription}')
    runner.step('echo', 'SCREENSHOT_CHECKPOINT RECRUIT_VAC_Update_02_03_BEFORE_UPDATE_SAVE.png', '')
    runner.step('captureEntirePageScreenshot', 'RECRUIT_VAC_Update_02_03_BEFORE_UPDATE_SAVE.png', '')
    verifySaveContract('BEFORE_SAVE')
    checks.saveEdit([
        'Vacancy Name': runner.vars['vacancyName'], 'Description': runner.vars['newDescription'],
        'Hiring Manager': runner.vars['managerName'], 'Number of Positions': '3',
        'Job Title': 'QA Engineer', 'Active': true, 'Publish in RSS Feed and Web Page': false
    ])
    runner.step('open', '${savedVacancyUrl}', '')
    runner.step('waitForElementPresent', 'xpath=//h6[normalize-space()=\'Edit Vacancy\']', '')
    runner.step('waitForValue', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('assertValue', 'xpath=//label[normalize-space()=\'Vacancy Name\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '${vacancyName}')
    runner.step('assertValue', 'xpath=//label[normalize-space()=\'Description\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//textarea', '${newDescription}')
    runner.step('assertValue', 'xpath=//label[normalize-space()=\'Number of Positions\']/ancestor::div[contains(concat(\' \',normalize-space(@class),\' \'),\' oxd-input-group \')][1]//input', '3')
    runner.step('assertValue', VacancyChecks.input('Hiring Manager'), '${managerName}')
    runner.step('assertText', VacancyChecks.select('Job Title'), 'QA Engineer')
    runner.step('assertChecked', "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' orangerhrm-switch-wrapper ')][.//*[normalize-space()='Active']]//input[@type='checkbox']", '')
    runner.step('assertNotChecked', "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' orangerhrm-switch-wrapper ')][.//*[contains(normalize-space(),'Publish in RSS Feed')]]//input[@type='checkbox']", '')
    runner.step('echo', 'ASSERTIONS COMPLETED: RECRUIT_VAC_Update_02; inspect screenshot tab and keep own fixture ledger.', '')
    runner.finished()
} catch (Throwable t) {
    runner.failed(t)
    throw t
}
