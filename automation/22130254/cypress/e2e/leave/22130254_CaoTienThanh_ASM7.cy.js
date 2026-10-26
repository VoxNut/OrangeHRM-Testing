/// <reference types="cypress" />

// Author: 22130254 – Cao Tiến Thành.
// Module: LEAVE_CONFIG → LEAVE_SELF → LEAVE_ADMIN.
// Lab 7: ASM7
const runId = Date.now().toString(36);
const createdTypes = new Set();
const createdEmployees = [];
const ctx = {
  runId, dates: [], shiftsCreated: [],
  annualType: `ASM7_22130254_${runId}`,
};

function dateObject(date) {
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return { year, month, day, iso: `${year}-${month}-${day}` };
}

// Chọn một ngày làm việc trong kỳ kế tiếp, loại trừ cuối tuần và ngày lễ.
function buildDates(holidays) {
  const { from, to } = ctx.targetPeriod;
  const date = new Date(Number(from.year), Number(from.month) - 1, Number(from.day), 12);
  while (dateObject(date).iso <= to.iso) {
    const candidate = dateObject(date);
    if (![0, 6].includes(date.getDay()) && !holidays.has(candidate.iso)) return [candidate];
    date.setDate(date.getDate() + 1);
  }
  throw new Error('Không có ngày làm việc trong kỳ phép kiểm thử');
}

function formatDate(date, placeholder) {
  return placeholder.replace(/yyyy|mm|dd/gi, (token) => date[token.toLowerCase() === 'yyyy'
    ? 'year' : token.toLowerCase() === 'mm' ? 'month' : 'day']);
}

ctx.input = (label, value) => {
  return cy.field(label).find('input').clear().type(String(value), { delay: 0 }).blur();
};
ctx.enterDate = (label, date) => cy.field(label).find('input').invoke('attr', 'placeholder').then((placeholder) => {
  expect(placeholder, 'Định dạng ngày từ UI').to.match(/yyyy/i);
  const formatted = formatDate(date, placeholder);
  return cy.field(label).find('input').type(`{selectall}{backspace}${formatted}`, { delay: 0 })
    .blur().should('have.value', formatted);
});
ctx.save = () => cy.contains('button', /^\s*Save\s*$/).click();
ctx.success = (message) => cy.contains('.oxd-toast', message).should('be.visible');

// Đổi tài khoản bằng Logout/Login; ghi trạng thái sau khi UI xác nhận đăng nhập thành công.
function login(username, password) {
  return cy.then(() => {
    if (ctx.currentUser === username) return;
    return cy.switchUserUI(username, password).then(() => { ctx.currentUser = username; });
  });
}
ctx.loginAdmin = () => login(ctx.data.adminUsername, ctx.data.adminPassword);
ctx.loginEmployee = (employee = ctx.employee) => login(employee.username, ctx.data.employeePassword);

ctx.trackType = (name) => { createdTypes.add(name); };
ctx.openTypes = () => cy.leavePage('Configure', 'Leave Types');
ctx.typeRow = (name) => cy.ownRow(name);
ctx.deleteType = (name) => {
  ctx.openTypes();
  cy.get('body').then(($body) => {
    if ($body.find('.oxd-table-cell').toArray().some((cell) => cell.innerText.trim() === name)) {
      ctx.typeRow(name).find('button').filter(':has(.bi-trash)').click();
      cy.contains('.oxd-dialog-sheet button', /Yes, Delete/).click();
      ctx.success('Successfully Deleted');
      cy.ready();
      cy.contains('.oxd-table-cell', name).should('not.exist');
    }
  });
};

ctx.selectPeriod = (year) => {
  cy.field('Leave Period').find('.oxd-select-text').click();
  return cy.get('.oxd-select-option').then(($options) => {
    const matches = $options.filter((_, option) => option.innerText.trim().split(/\s+-\s+/)[0].includes(String(year)));
    expect(matches.length, 'Một kỳ có năm bắt đầu đúng dữ liệu kiểm thử').to.eq(1);
    return cy.wrap(matches).click();
  });
};
ctx.grant = (employeeName, type, year, days) => {
  cy.leavePage('Entitlements', 'Add Entitlements');
  cy.employeeUI(employeeName);
  cy.choose('Leave Type', type);
  ctx.selectPeriod(year);
  ctx.input('Entitlement', days);
  ctx.save();
  cy.contains('.oxd-dialog-sheet', 'Updating Entitlement').should('be.visible');
  cy.contains('.oxd-dialog-sheet button', /^\s*Confirm\s*$/).click();
  ctx.success('Successfully Saved');
  return cy.ready();
};
let searchSequence = 0;
function searchLeave() {
  const alias = `searchLeave${++searchSequence}`;
  cy.intercept('GET', /\/api\/v2\/leave\/(?:employees\/)?leave-requests(?:\?|$)/).as(alias);
  cy.contains('button', /^\s*Search\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
  return cy.ready();
}
ctx.openEntitlements = (employeeName, type, year) => {
  cy.leavePage('Entitlements', 'Employee Entitlements');
  cy.employeeUI(employeeName);
  cy.choose('Leave Type', type);
  ctx.selectPeriod(year);
  const alias = `entitlementsSearch${++searchSequence}`;
  cy.intercept('GET', '**/api/v2/leave/leave-entitlements?*').as(alias);
  cy.contains('button', /^\s*Search\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
  return cy.ready();
};

ctx.prepareApply = (from, to = from, type = ctx.annualType, comment) => {
  cy.leavePage('Apply');
  ctx.enterDate('From Date', from);
  ctx.enterDate('To Date', to);
  cy.choose('Leave Type', type);
  if (comment !== undefined) cy.field('Comments').find('textarea').clear().type(comment, { delay: 0 });
  cy.contains('h6', /^\s*Apply Leave\s*$/).click();
  return cy.ready();
};

// Xóa lần lượt các chip trạng thái qua icon hiển thị trước khi chọn danh sách mới.
ctx.selectStatuses = (statuses) => {
  function clear() {
    cy.field('Show Leave with Status').then(($group) => {
      const icons = $group.find('.oxd-multiselect-chips-selected .bi-x');
      if (icons.length) {
        cy.wrap(icons.first()).click();
        clear();
      }
    });
  }
  clear();
  statuses.forEach((status) => cy.choose('Show Leave with Status', status));
};
ctx.myLeave = (options = {}) => {
  cy.leavePage('My Leave');
  if (options.from) ctx.enterDate('From Date', options.from);
  if (options.to) ctx.enterDate('To Date', options.to);
  if (options.type) cy.choose('Leave Type', options.type);
  ctx.selectStatuses(options.statuses || ['Rejected', 'Cancelled', 'Pending Approval', 'Scheduled', 'Taken']);
  return searchLeave();
};
ctx.filterAdmin = (options = {}) => {
  cy.leavePage('Leave List');
  if (options.from) ctx.enterDate('From Date', options.from);
  if (options.to) ctx.enterDate('To Date', options.to);
  if (options.employee) cy.employeeUI(typeof options.employee === 'string' ? options.employee : options.employee.name);
  if (options.type) cy.choose('Leave Type', options.type);
  if (options.statuses) ctx.selectStatuses(options.statuses);
  return searchLeave();
};

// Tìm đúng đơn theo ngày và loại phép của lần chạy này.
ctx.requestRow = (date, options = {}) => cy.field('From Date').find('input')
  .invoke('attr', 'placeholder').then((placeholder) => {
    const start = formatDate(date, placeholder);
    const end = options.to && formatDate(options.to, placeholder);
    const type = options.type || ctx.annualType;
    return cy.get('.oxd-table-body .oxd-table-row').filter((_, row) => {
      const cells = Cypress.$(row).find('.oxd-table-cell');
      const text = cells.eq(1).text().trim();
      return text.startsWith(start) && (!end || text.includes(end)) && cells.eq(3).text().trim() === type;
    }).should('have.length', 1);
  });

// Đọc kỳ phép thật từ dropdown; demo có thể dùng kỳ qua hai năm dương lịch.
function preparePeriods() {
  cy.leavePage('Entitlements', 'Add Entitlements');
  cy.field('Leave Period').find('.oxd-select-text').click();
  cy.get('.oxd-select-option').then(($options) => {
    const tokens = ctx.datePlaceholder.match(/yyyy|mm|dd/gi).map((token) => token.toLowerCase());
    const parse = (value) => {
      const numbers = value.match(/\d+/g);
      const parts = Object.fromEntries(tokens.map((token, index) => [token, numbers[index]]));
      const date = { year: parts.yyyy, month: parts.mm.padStart(2, '0'), day: parts.dd.padStart(2, '0') };
      date.iso = `${date.year}-${date.month}-${date.day}`;
      return date;
    };
    const periods = $options.toArray().map((option) => option.innerText.trim())
      .filter((value) => value.match(/\d{4}/)).map((label) => {
        const dates = label.split(/\s+-\s+/);
        expect(dates, 'Hai ngày của kỳ phép').to.have.length(2);
        return { label, from: parse(dates[0]), to: parse(dates[1]) };
      }).sort((a, b) => a.from.iso.localeCompare(b.from.iso));
    const today = dateObject(new Date()).iso;
    const index = periods.findIndex((period) => period.from.iso <= today && period.to.iso >= today);
    expect(index, 'Kỳ phép chứa ngày hiện tại').to.be.at.least(0);
    expect(index, 'Có kỳ kế tiếp cho dữ liệu tương lai').to.be.lessThan(periods.length - 1);
    ctx.currentPeriod = periods[index];
    ctx.targetPeriod = periods[index + 1];
    ctx.currentYear = ctx.currentPeriod.from.year;
    ctx.targetYear = ctx.targetPeriod.from.year;
  });
  cy.field('Entitlement').find('input').click();
}

// Đọc ngày lễ của kỳ kế tiếp để chọn ngày xin nghỉ hợp lệ.
function prepareCalendar() {
  const holidays = new Set();
  cy.then(() => {
  [...new Set([ctx.targetPeriod.from.year, ctx.targetPeriod.to.year])].forEach((year) => {
    cy.leavePage('Configure', 'Holidays');
    ctx.enterDate('From', { year, month: '01', day: '01' });
    ctx.enterDate('To', { year, month: '12', day: '31' });
    const alias = `holidaysSearch${++searchSequence}`;
    cy.intercept({ method: 'GET', url: '**/api/v2/leave/holidays*',
      query: { limit: '50', offset: '0', fromDate: `${year}-01-01`, toDate: `${year}-12-31` } }).as(alias);
    cy.contains('button', /^\s*Search\s*$/).click();
    cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
    cy.ready();
    cy.field('From').find('input').invoke('attr', 'placeholder').then((placeholder) => {
      const tokens = placeholder.match(/yyyy|mm|dd/gi).map((token) => token.toLowerCase());
      cy.get('body').then(($body) => {
        const headers = $body.find('.oxd-table-header-cell').toArray().map((cell) => cell.innerText.trim());
        const dateColumn = headers.indexOf('Date');
        $body.find('.oxd-table-body .oxd-table-row').each((_, row) => {
          const values = Cypress.$(row).find('.oxd-table-cell').eq(dateColumn).text().trim().match(/\d+/g);
          if (values && values.length === 3) {
            const parts = Object.fromEntries(tokens.map((token, index) => [token, values[index]]));
            expect(parts.yyyy, 'Ngày lễ hiển thị thuộc kỳ đã Search').to.eq(year);
            holidays.add(`${parts.yyyy}-${parts.mm.padStart(2, '0')}-${parts.dd.padStart(2, '0')}`);
          }
        });
        const totalText = $body.text();
        const total = totalText.match(/\((\d+)\) Records Found/);
        if (total) {
          expect(Number(total[1]), 'Ngày nghỉ lễ một kỳ nằm trong trang đã đọc').to.be.at.most(50);
          expect($body.find('.oxd-table-body .oxd-table-row').length, 'Đã đọc đủ ngày lễ trong kỳ').to.eq(Number(total[1]));
        }
      });
    });
  });
  cy.then(() => {
    ctx.dates = buildDates(holidays);
  });
  });
}

// Ghi dữ liệu thực tế để dọn đúng fixture nếu lần chạy bị gián đoạn.
function writeRunData(cleanupCompleted = false) {
  return cy.then(() => cy.writeFile('evidence/executions/22130254/run-data.json', {
    runId,
    recordedAt: new Date().toISOString(),
    cleanupAttempted: Boolean(ctx.cleanupAttempted),
    cleanupCompleted,
    fixtures: [ctx.employee, ctx.employeeB].filter(Boolean).map((employee) => ({
      id: employee.id, name: employee.name, username: employee.username,
      empNumber: employee.empNumber, createdDuringRun: Boolean(employee.created),
    })),
    trackedTypes: [...createdTypes],
    shifts: ctx.shiftsCreated,
    periods: { current: ctx.currentPeriod, target: ctx.targetPeriod },
    dates: ctx.dates,
  }, { log: false }));
}

describe('22130254_CaoTienThanh_ASM7 – Kiểm thử Leave qua giao diện', () => {
  before(() => {
    // Chuẩn bị nhân viên chính A và nhân viên B đối chứng hạn mức.
    cy.fixture('leave').then((data) => {
      ctx.data = data;
      ['employee', 'employeeB'].forEach((key, index) => {
        const employee = {
          id: String((Number(Date.now().toString().slice(-9)) + index) % 1000000000).padStart(9, '0'),
          firstName: data.employeeFirstName,
          middleName: data.employeeMiddleName,
          lastName: `T${index}${runId}`,
          username: `thanh_22130254_${index}_${runId}`,
        };
        employee.name = `${employee.firstName} ${employee.middleName} ${employee.lastName}`;
        ctx[key] = employee;
      });
      ctx.loginAdmin();
      [ctx.employee, ctx.employeeB].forEach((employee) => {
        createdEmployees.push(employee);
        createEmployee(ctx, employee);
      });
      cy.field('Date of Birth').find('input').invoke('attr', 'placeholder').then((placeholder) => {
        ctx.datePlaceholder = placeholder;
      });
      verify8hShift(ctx, ctx.employee);
      preparePeriods();
      prepareCalendar();
      writeRunData();
    });
  });

  afterEach(function () {
    // Minh chứng – Chỉ chụp ảnh PASS khi toàn bộ các assertion của case đã hoàn tất.
    if (this.currentTest.state === 'passed') {
      const id = this.currentTest.title.match(/^TC_LEAVE_[A-Z]+_\d{3}/)[0];
      cy.screenshot(id, { capture: 'runner', overwrite: true });
    } else if (this.currentTest.state === 'failed') {
      // Phục hồi UI – Đóng modal của case lỗi để case độc lập kế tiếp vẫn có thể thao tác.
      cy.closeDialogUI();
    }
  });

  registerConfigCases(ctx);
  registerSelfCases(ctx);
  registerAdminCases(ctx);

  after(() => {
    // Dọn dữ liệu – Đối chiếu cả mã và họ tên, chỉ xóa những fixture của đúng lần chạy.
    if (!ctx.data) return;
    cy.then(() => { ctx.cleanupAttempted = true; });
    writeRunData();
    cy.closeDialogUI();
    ctx.loginAdmin();
    createdEmployees.forEach((employee) => {
      const alias = `cleanupEmployee${++searchSequence}`;
      cy.intercept('GET', '**/api/v2/pim/employees?*').as(alias);
      cy.location('pathname').then((pathname) => {
        if (!pathname.includes('/viewEmployeeList')) {
          cy.contains('.oxd-main-menu-item', /^\s*PIM\s*$/).click();
          cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
        }
      });
      cy.ready();
      ctx.input('Employee Id', employee.id);
      cy.choose('Include', 'Current and Past Employees');
      cy.contains('button', /^\s*Search\s*$/).click();
      cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
      cy.ready();
      cy.get('body').then(($body) => {
        if ($body.find('.oxd-table-cell').toArray().some((cell) => cell.innerText.trim() === employee.id)) {
          cy.ownRow(employee.id).should('contain.text', employee.firstName).and('contain.text', employee.lastName);
          cy.ownRow(employee.id).find('button').filter(':has(.bi-trash)').click();
          cy.contains('.oxd-dialog-sheet button', /Yes, Delete/).click();
          ctx.success('Successfully Deleted');
          cy.ready();
          cy.contains('.oxd-table-cell', employee.id).should('not.exist');
        }
      });
    });
    cy.then(() => {
      [...createdTypes].forEach((name) => ctx.deleteType(name));
    });
    cleanupShifts(ctx);
    writeRunData(true);
    cy.logoutUI();
  });
});

let pimSetupNavigation = 0;

function createEmployee(ctx, employee) {
  // Arrange – Tạo hồ sơ riêng, chuẩn bị ESS khi fixture có username.
  const firstName = employee.firstName || ctx.data.employeeFirstName;
  const middleName = employee.middleName ?? ctx.data.employeeMiddleName;
  employee.firstName = firstName;
  employee.middleName = middleName;
  employee.name = `${firstName} ${middleName} ${employee.lastName}`;
  const alias = `pimCreateList${++pimSetupNavigation}`;
  cy.intercept('GET', '**/api/v2/pim/employees?*').as(alias);
  cy.contains('.oxd-main-menu-item', /^\s*PIM\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
  cy.ready();
  cy.contains('button', /^\s*Add\s*$/).click();
  cy.contains('h6', /^\s*Add Employee\s*$/).should('be.visible');
  cy.ready();
  cy.get('input[name="firstName"]').type(firstName);
  if (middleName) cy.get('input[name="middleName"]').type(middleName);
  cy.get('input[name="lastName"]').type(employee.lastName);
  cy.field('Employee Id').find('input').clear().type(String(employee.id));
  if (employee.username) {
    cy.get('.user-form-header .oxd-switch-input').click();
    cy.field('Username').find('input').type(employee.username);
    cy.contains('.oxd-radio-wrapper', /^\s*Enabled\s*$/).find('input').should('be.checked');
    cy.field('Password').find('input').type(employee.password || ctx.data.employeePassword, { log: false });
    cy.field('Confirm Password').find('input').type(employee.password || ctx.data.employeePassword, { log: false });
  }
  // Đăng ký quyền sở hữu trước Save để cleanup xử lý cả trường hợp tạo hồ sơ rồi tạo ESS lỗi.
  cy.then(() => {
    employee.attempted = true;
  });

  // Act – Save qua biểu mẫu Add Employee.
  cy.contains('button', /^\s*Save\s*$/).click();

  // Assert – Lưu thành công; mã hồ sơ và tên khớp fixture trên Personal Details.
  cy.contains('.oxd-toast', 'Successfully Saved').should('be.visible');
  cy.contains('h6', /^\s*Personal Details\s*$/).should('be.visible');
  cy.ready();
  cy.field('Employee Id').find('input').should('have.value', String(employee.id));
  cy.get('input[name="firstName"]').should('have.value', firstName);
  cy.get('input[name="lastName"]').should('have.value', employee.lastName);
  return cy.location('pathname').then((pathname) => {
    const match = pathname.match(/\/empNumber\/(\d+)/);
    expect(match, 'URL hồ sơ vừa tạo').not.to.eq(null);
    employee.empNumber = match[1];
    employee.created = true;
  });
}

function openWorkShiftsUI() {
  const initialAlias = `asm7AdminInitial${++pimSetupNavigation}`;
  const targetAlias = `asm7WorkShiftList${pimSetupNavigation}`;
  cy.intercept('GET', '**/api/v2/admin/users?*').as(initialAlias);
  cy.intercept('GET', '**/api/v2/admin/work-shifts?*').as(targetAlias);
  cy.contains('.oxd-main-menu-item', /^\s*Admin\s*$/).click();
  cy.wait(`@${initialAlias}`).its('response.statusCode').should('eq', 200);
  cy.ready();
  cy.contains('.oxd-topbar-body-nav-tab', /^\s*Job\s*$/).click();
  cy.contains('.oxd-topbar-body-nav-tab-link', /^\s*Work Shifts\s*$/).click();
  cy.wait(`@${targetAlias}`).its('response.statusCode').should('eq', 200);
  cy.contains('h6', /^\s*Work Shifts\s*$/).should('be.visible');
  return cy.ready();
}

function verify8hShift(ctx, employee) {
  // Arrange – Tạo ca riêng và gán nhân viên qua Admin > Job > Work Shifts.
  const shiftName = `ASM7_8h_${ctx.runId}_${employee.id}`;
  openWorkShiftsUI();
  cy.contains('button', /^\s*Add\s*$/).click();
  cy.contains('h6', /^\s*Add Work Shift\s*$/).should('be.visible');
  cy.field('Shift Name').find('input').type(shiftName);
  // OXD TimeInput nhận định dạng 12 giờ có AM/PM, rồi chuyển model sang HH:mm.
  cy.field('From').find('.oxd-time-input input').clear().type('09:00 AM').blur();
  cy.field('To').find('.oxd-time-input input').clear().type('05:00 PM').blur();
  cy.contains('h6', /^\s*Add Work Shift\s*$/).click();
  cy.field('Duration Per Day').find('p').should('have.text', '8.00');
  cy.field('Assigned Employees').find('input').type(employee.name);
  cy.contains('.oxd-autocomplete-option', new RegExp(`^\\s*${Cypress._.escapeRegExp(employee.name)}\\s*$`))
    .should('be.visible').click();
  cy.field('Assigned Employees').should('contain.text', employee.name);
  cy.then(() => {
    ctx.shiftsCreated ||= [];
    if (!ctx.shiftsCreated.includes(shiftName)) ctx.shiftsCreated.push(shiftName);
  });

  // Act – Save ca 09:00–17:00 và mở lại bản ghi riêng qua icon Edit.
  cy.contains('button', /^\s*Save\s*$/).click();
  cy.contains('.oxd-toast', 'Successfully Saved').should('be.visible');
  cy.ownRow(shiftName).should('contain.text', '8.00');
  const editAlias = `asm7WorkShiftEdit${++pimSetupNavigation}`;
  cy.intercept('GET', '**/api/v2/admin/work-shifts/*').as(editAlias);
  cy.ownRow(shiftName).find('.bi-pencil-fill').click();
  cy.wait(`@${editAlias}`).its('response.statusCode').should('eq', 200);
  cy.contains('h6', /^\s*Edit Work Shift\s*$/).should('be.visible');
  cy.ready();

  // Assert – Ca đã lưu có đúng tám giờ/ngày, giờ làm và nhân viên được gán.
  cy.field('From').find('.oxd-time-input input').should('have.value', '09:00 AM');
  cy.field('To').find('.oxd-time-input input').should('have.value', '05:00 PM');
  cy.field('Duration Per Day').find('p').should('have.text', '8.00');
  cy.field('Assigned Employees').should('contain.text', employee.name);
  cy.contains('button', /^\s*Cancel\s*$/).click();
  return cy.then(() => {
    employee.shiftName = shiftName;
    employee.shiftHours = 8;
    ctx.shiftHours = 8;
  });
}

// Cleanup – Sau khi xóa nhân viên, chỉ xóa ca có đúng tên được đăng ký của lần chạy này.
function deleteOwnedWorkShiftUI(name) {
  openWorkShiftsUI();
  cy.get('body').then(($body) => {
    const matches = $body.find('.oxd-table-cell').filter((_, cell) => cell.innerText.trim() === name);
    if (matches.length) {
      cy.ownRow(name).find('.bi-trash').click();
      cy.contains('.oxd-dialog-sheet button', /^\s*Yes, Delete\s*$/).click();
      cy.contains('.oxd-toast', 'Successfully Deleted').should('be.visible');
      cy.ready();
      cy.contains('.oxd-table-cell', name).should('not.exist');
    }
  });
}

function cleanupShifts(ctx) {
  return cy.then(() => {
    const names = [...new Set((ctx.shiftsCreated || []).map((shift) =>
      typeof shift === 'string' ? shift : shift.name))];
    names.forEach((name) => {
      expect(name, 'Chỉ dọn ca mang runId của lần chạy này').to.include(ctx.runId);
      deleteOwnedWorkShiftUI(name);
    });
  });
}

function registerConfigCases(ctx) {
  describe('LEAVE_CONFIG – Leave Types & Entitlements', () => {

    // Mở biểu mẫu mới để dữ liệu chưa lưu của case trước không ảnh hưởng validation.
    function openAddType() {
      ctx.openTypes();
      cy.contains('button', /^\s*Add\s*$/).click();
      cy.contains('h6', /^\s*Add Leave Type\s*$/).should('be.visible');
    }

    function openAddEntitlements() {
      cy.leavePage('Entitlements', 'Add Entitlements');
      cy.contains('.oxd-radio-wrapper', /^\s*Individual Employee\s*$/)
        .find('input').should('be.checked');
    }

    function prepareEntitlement(days, year = ctx.targetYear) {
      openAddEntitlements();
      cy.employeeUI(ctx.employee.name);
      cy.choose('Leave Type', ctx.annualType);
      ctx.selectPeriod(year);
      if (days !== undefined) ctx.input('Entitlement', days);
    }

    function confirmEntitlement() {
      ctx.save();
      cy.contains('.oxd-dialog-sheet', 'Updating Entitlement').should('be.visible');
      cy.contains('.oxd-dialog-sheet button', /^\s*Confirm\s*$/).click();
    }

    function assertTotal(expected) {
      return cy.contains('.orangehrm-paper-container .orangehrm-header-container', /Total\s+[\d.]+\s+Day\(s\)/)
        .invoke('text').then((text) => {
          const match = text.match(/Total\s+([\d.]+)\s+Day\(s\)/);
          expect(Number(match[1]), 'Tổng hạn mức của tổ hợp đang lọc').to.eq(expected);
        });
    }

    // Lấy vị trí cột từ nhãn hiển thị, tránh phụ thuộc vào cột checkbox/hành động.
    function assertEntitlements(type, year, expected) {
      cy.field('Leave Period').find('.oxd-select-text').invoke('text').then((text) => {
        const period = text.trim().split(/\s+-\s+/);
        expect(period, 'Hai mốc ngày của kỳ phép được chọn').to.have.length(2);
        expect(period[0]).to.include(String(year));
        cy.get('.oxd-table-header-cell').then(($headers) => {
          const labels = [...$headers].map((header) => header.innerText.trim());
          const columns = {
            type: labels.indexOf('Leave Type'),
            from: labels.indexOf('Valid From'),
            to: labels.indexOf('Valid To'),
            days: labels.indexOf('Days'),
          };
          Object.values(columns).forEach((index) => expect(index, 'Cột bảng hạn mức').to.be.at.least(0));
          cy.get('.oxd-table-body .oxd-table-row').should('have.length', 1).then(($rows) => {
            let total = 0;
            [...$rows].forEach((row) => {
              const cells = [...row.querySelectorAll('.oxd-table-cell')].map((cell) => cell.innerText.trim());
              expect(cells[columns.type], 'Loại phép đúng bộ dữ liệu').to.eq(type);
              expect(cells[columns.from], 'Ngày bắt đầu đúng kỳ').to.eq(period[0]);
              expect(cells[columns.to], 'Ngày kết thúc đúng kỳ').to.eq(period[1]);
              total += Number(cells[columns.days]);
            });
            expect(total, 'Tổng Days trên các dòng').to.eq(expected);
          });
        });
      });
      assertTotal(expected);
    }

    it('TC_LEAVE_CONFIG_001 – Thêm mới Leave Type với các trường bắt buộc', () => {
      // Arrange – Nhập tên duy nhất; kiểm tra Situational mặc định là No.
      openAddType();
      ctx.input('Name', ctx.annualType);
      cy.contains('.oxd-radio-wrapper', /^\s*No\s*$/).find('input').should('be.checked');
      ctx.trackType(ctx.annualType);

      // Act – Lưu loại phép dùng chung cho các case phía sau.
      ctx.save();

      // Assert – Có thông báo lưu thành công và đúng tên trong danh sách.
      ctx.success('Successfully Saved');
      cy.then(() => { ctx.annualTypeCreated = true; });
      cy.ready();
      ctx.typeRow(ctx.annualType).should('be.visible');
      cy.get('.oxd-table-header-cell').then(($headers) => {
        const labels = [...$headers].map((cell) => cell.innerText.trim()).filter(Boolean);
        expect(labels, 'Danh sách có hai cột dữ liệu').to.deep.eq(['Name', 'Actions']);
      });
    });

    it('TC_LEAVE_CONFIG_016 – Thêm Leave Entitlement hợp lệ với số ngày nguyên', () => {
      // Arrange – Chọn ESS riêng, Annual và kỳ năm sau chưa có hạn mức.
      prepareEntitlement('20', ctx.targetYear);

      // Act – Save rồi Confirm ở Updating Entitlement.
      confirmEntitlement();

      // Assert – Lưu thành công; tổ hợp nhân viên, loại phép, kỳ có đúng 20 ngày.
      ctx.success('Successfully Saved');
      cy.then(() => { ctx.targetGranted = true; });
      ctx.openEntitlements(ctx.employee.name, ctx.annualType, ctx.targetYear);
      assertEntitlements(ctx.annualType, ctx.targetYear, 20);
    });

    it('TC_LEAVE_CONFIG_008 – Lọc Employee Entitlements theo nhân viên, loại phép và kỳ', () => {
      // Arrange – A có 20 ngày kỳ hiện tại và 20 ngày năm sau; B có dữ liệu đối chứng ở cả hai kỳ.
      ctx.grant(ctx.employee.name, ctx.annualType, ctx.currentYear, '20');
      cy.then(() => { ctx.currentGranted = true; });
      ctx.grant(ctx.employeeB.name, ctx.annualType, ctx.currentYear, '3');
      ctx.grant(ctx.employeeB.name, ctx.annualType, ctx.targetYear, '4');

      // Act – Mở Employee Entitlements, chọn A, Annual, kỳ hiện tại và Search qua UI.
      ctx.openEntitlements(ctx.employee.name, ctx.annualType, ctx.currentYear);

      // Assert – Chỉ đúng tổ hợp A/Annual/kỳ hiện tại, mốc ngày chính xác và tổng Days bằng 20.
      cy.field('Employee Name').find('input').should('have.value', ctx.employee.name);
      assertEntitlements(ctx.annualType, ctx.currentYear, 20);
    });

  });
}

function registerSelfCases(ctx) {
  // Đọc ngày khi case chạy để nhận lịch làm việc và ngày nghỉ lễ đã chuẩn bị qua UI.
  const day = (index) => ctx.dates[index];
  const apply = () => cy.contains('button', /^\s*Apply\s*$/).click();

  // Đối chiếu đúng cột Number of Days, tránh nhầm với số dư hoặc số ngày trong Status.
  function assertDays(date, expected, options = {}) {
    ctx.requestRow(date, options).find('.oxd-table-cell').eq(5)
      .invoke('text').then((value) => {
        expect(Number(value.trim()), 'Số ngày nghỉ trên đúng dòng đơn').to.eq(expected);
      });
  }

  describe('LEAVE_SELF – Apply Leave và My Leave', () => {
    before(() => {
      // Chuẩn bị – ESS riêng đã có entitlement kỳ hiện tại và kỳ chứa các ngày kiểm thử.
      ctx.loginEmployee();
    });

    it('TC_LEAVE_SELF_001 – Nộp đơn nghỉ một ngày hợp lệ', () => {
      // Arrange – Chọn loại phép riêng, một ngày làm việc và comment riêng của lần chạy.
      ctx.prepareApply(day(0), day(0), ctx.annualType,
        `${ctx.data.applicationComment} ${ctx.runId}`);
      cy.field('Duration').should('contain.text', 'Full Day');

      // Act – Nộp đơn Full Day và mở danh sách My Leave với đúng ngày/loại phép.
      apply();
      ctx.success('Successfully Saved');
      ctx.myLeave({ from: day(0), to: day(0), type: ctx.annualType });

      // Assert – Chỉ có một đơn của ESS, Pending Approval và đúng 1.00 ngày.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length', 1);
      ctx.requestRow(day(0)).should('contain.text', ctx.employee.name)
        .and('contain.text', ctx.annualType).and('contain.text', 'Pending Approval');
      assertDays(day(0), 1);
    });

  });
}

function registerAdminCases(ctx) {
  // Dùng lại ngày và đơn nghỉ đã được tạo trong nhóm LEAVE_SELF.
  let coreApprove;

  function rows() {
    return cy.get('.oxd-table-body .oxd-table-row');
  }

  // Cột được xác định theo tiêu đề để đối chiếu số liệu thật trên bảng.
  function cell(date, title, options) {
    return cy.get('.oxd-table-header-cell').then(($headers) => {
      const index = Array.from($headers).findIndex((header) => header.innerText.trim() === title);
      expect(index, `Có cột ${title} trên Leave List`).to.be.at.least(0);
      return ctx.requestRow(date, options).find('.oxd-table-cell').eq(index);
    });
  }

  describe('LEAVE_ADMIN – Danh sách phép và xử lý yêu cầu', () => {
    before(() => {
      // Duyệt chính đơn nghỉ do SELF_001 vừa tạo, không chuẩn bị thêm đơn khác.
      coreApprove = ctx.dates[0];
      ctx.loginAdmin();
    });

    it('TC_LEAVE_ADMIN_010 – Approve một đơn Pending Approval', () => {
      // Arrange – Lọc và đối chiếu đúng đơn Pending dành riêng cho nhánh Approve.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreApprove, to: coreApprove, statuses: ['Pending Approval'] });
      ctx.requestRow(coreApprove).should('contain.text', ctx.employee.name).and('contain.text', 'Pending Approval');

      // Act – Approve trên dòng của đúng đơn rồi tìm lại trạng thái Scheduled.
      ctx.requestRow(coreApprove).contains('button', /^\s*Approve\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreApprove, to: coreApprove, statuses: ['Scheduled'] });

      // Assert – Đơn chuyển Scheduled, thời lượng vẫn một ngày và hết nút Approve/Reject.
      ctx.requestRow(coreApprove).should('contain.text', 'Scheduled');
      cell(coreApprove, 'Number of Days').invoke('text').then((value) => { expect(Number(value.trim())).to.eq(1); });
      ctx.requestRow(coreApprove).contains('button', /^\s*Approve\s*$/).should('not.exist');
      ctx.requestRow(coreApprove).contains('button', /^\s*Reject\s*$/).should('not.exist');
    });

    it('TC_LEAVE_ADMIN_011 – Scheduled đồng bộ sang My Leave của nhân viên', () => {
      // Arrange – Chuyển từ Admin sang đúng ESS đã tạo đơn được duyệt.
      ctx.loginEmployee();

      // Act – Tìm đúng loại phép và ngày của đơn đã Approve trên My Leave.
      ctx.myLeave({ from: coreApprove, to: coreApprove, type: ctx.annualType, statuses: ['Scheduled'] });

      // Assert – Dòng ESS có đúng ngày, loại phép, Scheduled và một ngày nghỉ.
      ctx.requestRow(coreApprove).should('contain.text', ctx.annualType).and('contain.text', 'Scheduled');
      cell(coreApprove, 'Number of Days').invoke('text').then((value) => { expect(Number(value.trim())).to.eq(1); });
      rows().should('have.length', 1);
    });

  });
}
