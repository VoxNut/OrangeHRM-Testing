const path = require('node:path');
const { defineConfig } = require('cypress');
const root = path.resolve(__dirname, '..');
module.exports = defineConfig({
  video: false,
  screenshotOnRunFailure: true,
  defaultCommandTimeout: 20000,
  requestTimeout: 30000,
  responseTimeout: 60000,
  viewportWidth: 1440,
  viewportHeight: 1000,
  retries: 0,
  e2e: {
    baseUrl: 'https://opensource-demo.orangehrmlive.com',
    specPattern: path.join(root, 'automation/22130254/cypress/e2e/**/*.cy.js'),
    supportFile: path.join(__dirname, 'cypress-support.cjs'),
    fixturesFolder: path.join(root, 'automation/22130254/cypress/fixtures'),
    testIsolation: false
  }
});
