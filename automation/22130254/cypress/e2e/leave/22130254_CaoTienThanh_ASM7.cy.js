/// <reference types="cypress" />

// MSSV: 22130254 – Cao Tiến Thành – ASM7.
// Chạy tuần tự 75 case: LEAVE_CONFIG → LEAVE_SELF → LEAVE_ADMIN.
// Thao tác nghiệp vụ và chuẩn bị dữ liệu đều qua UI; intercept chỉ quan sát việc tải.
const runId = Date.now().toString(36);
const onlyCases = String(Cypress.env('onlyCases') || '').split(',').map((s) => s.trim()).filter(Boolean);
const rawIt = globalThis.it;
const it = onlyCases.length
  ? (title, fn) => {
    const id = title.split(/\s+[–-]\s+|:/)[0].trim();
    if (onlyCases.includes(id)) return rawIt(title, fn);
  }
  : rawIt;
const currentYear = String(new Date().getFullYear());
const targetYear = String(Number(currentYear) + 1);
const createdTypes = new Set();
const createdEmployees = [];
const ctx = {
  runId, currentYear, targetYear, dates: [], shiftsCreated: [],
  annualType: `ASM7_22130254_${runId}`,
  alternateType: `ASM7_Alt_${runId}`,
  pastType: `ASM7_Past_${runId}`,
  bulkType: `ASM7_Bulk_${runId}`,
  pageType: `ASM7_Page_${runId}`,
};

function dateObject(date) {
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return { year, month, day, iso: `${year}-${month}-${day}` };
}

// Chọn trọn các tuần thứ Hai–thứ Sáu, giữ vị trí các block nhiều ngày không chồng nhau.
function buildDates(holidays = new Set()) {
  const result = [];
  const start = ctx.targetPeriod?.from || { year: targetYear, month: '01', day: '01' };
  const end = ctx.targetPeriod?.to?.iso || `${targetYear}-12-31`;
  const monday = new Date(Number(start.year), Number(start.month) - 1, Number(start.day), 12);
  while (monday.getDay() !== 1) monday.setDate(monday.getDate() + 1);
  while (result.length < 100) {
    const week = Array.from({ length: 5 }, (_, offset) =>
      dateObject(new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + offset, 12)));
    if (week.every((date) => date.iso <= end && !holidays.has(date.iso))) result.push(...week);
    monday.setDate(monday.getDate() + 7);
    if (result.length < 100 && dateObject(monday).iso > end) throw new Error('Không đủ tuần làm việc trong kỳ kiểm thử');
  }
  return result;
}
ctx.dates = buildDates();
ctx.findWeekdayBlock = (start, length) => ctx.dates.slice(start, start + length);

function formatDate(date, placeholder) {
  return placeholder.replace(/yyyy|mm|dd/gi, (token) => date[token.toLowerCase() === 'yyyy'
    ? 'year' : token.toLowerCase() === 'mm' ? 'month' : 'day']);
}

ctx.input = (label, value) => {
  const input = cy.field(label).find('input').clear().type(String(value), { delay: 0 });
  // Giữ focus của autocomplete để có thể quan sát No Records Found trước khi rời trường.
  return label === 'Employee Name' ? input : input.blur();
};
ctx.enterDate = (label, date) => cy.field(label).find('input').invoke('attr', 'placeholder').then((placeholder) => {
  expect(placeholder, 'Định dạng ngày từ UI').to.match(/yyyy/i);
  const formatted = formatDate(date, placeholder);
  return cy.field(label).find('input').type(`{selectall}{backspace}${formatted}`, { delay: 0 })
    .blur().should('have.value', formatted);
});

ctx.save = () => cy.contains('button', /^\s*Save\s*$/).click();
ctx.cancel = () => cy.contains('button', /^\s*Cancel\s*$/).click();
ctx.success = (message) => cy.contains('.oxd-toast', message, { timeout: 60000 }).should('be.visible');

// Đổi tài khoản bằng Logout/Login; ghi trạng thái sau khi UI xác nhận đăng nhập thành công.
function login(username, password) {
  return cy.location('pathname').then((pathname) => {
    if (ctx.currentUser === username && !pathname.includes('/auth/login')) return;
    ctx.currentUser = null;
    return cy.switchUserUI(username, password).then(() => { ctx.currentUser = username; });
  });
}
ctx.loginAdmin = () => login(ctx.data.adminUsername, ctx.data.adminPassword);
ctx.loginEmployee = (employee = ctx.employee) => login(employee.username, ctx.data.employeePassword);

ctx.trackType = (name) => { createdTypes.add(name); };
ctx.openTypes = () => cy.location('pathname').then((pathname) => {
  if (pathname.includes('/defineLeaveType')) {
    cy.location('pathname', { timeout: 20000 }).should('include', '/leaveTypeList');
  }
  return cy.location('pathname').then((current) => {
    if (current.includes('/leaveTypeList')) {
      cy.contains('h6', /^\s*Leave Types\s*$/).should('be.visible');
      return cy.ready();
    }
    return cy.leavePage('Configure', 'Leave Types');
  });
});
ctx.typeRow = (name) => cy.ownRow(name);
ctx.addType = (name, situational = 'No') => {
  ctx.openTypes();
  cy.contains('button', /^\s*Add\s*$/).click();
  ctx.input('Name', name);
  cy.contains('.oxd-radio-wrapper', new RegExp(`^\\s*${situational}\\s*$`)).find('.oxd-radio-input').click();
  cy.contains('.oxd-radio-wrapper', new RegExp(`^\\s*${situational}\\s*$`)).find('input').should('be.checked');
  ctx.trackType(name);
  ctx.save();
  ctx.success('Successfully Saved');
  cy.location('pathname').should('include', '/leaveTypeList');
  cy.ready();
  return ctx.typeRow(name).should('be.visible');
};
ctx.openTypeEdit = (name) => {
  ctx.openTypes();
  ctx.typeRow(name).find('button').filter(':has(.bi-pencil-fill)').click();
  return cy.contains('h6', /^\s*Edit Leave Type\s*$/).should('be.visible');
};
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
ctx.numericCell = (row, column) => row.then(($row) => {
  const headers = Cypress.$('.oxd-table-header-cell').toArray().map((header) => header.innerText.trim());
  const index = headers.indexOf(column);
  expect(index, `Cột ${column}`).to.be.at.least(0);
  const value = Number($row.find('.oxd-table-cell').eq(index).text().trim());
  expect(value, `Giá trị số của ${column}`).to.be.finite;
  return value;
});

ctx.prepareApply = (from, to = from, type = ctx.annualType, comment, reuseForm = false) => {
  if (!reuseForm) cy.leavePage('Apply');
  // Nhập ngày trước loại phép để balance chỉ tải theo kỳ cần kiểm thử.
  ctx.enterDate('From Date', from);
  ctx.enterDate('To Date', to);
  cy.choose('Leave Type', type);
  if (comment !== undefined) cy.field('Comments').find('textarea').clear().type(comment, { delay: 0 });
  cy.contains('h6', /^\s*Apply Leave\s*$/).click();
  return cy.ready();
};
ctx.applyRequest = (from, to = from, options = {}) => {
  ctx.prepareApply(from, to, options.type || ctx.annualType,
    options.comment || `${ctx.data.applicationComment} ${runId}`);
  if (options.duration) cy.choose('Duration', options.duration);
  cy.contains('button', /^\s*Apply\s*$/).click();
  ctx.success('Successfully Saved');
  return cy.ready();
};
ctx.seedRequests = (employee, type, dates, comment) => {
  ctx.loginEmployee(employee);
  cy.leavePage('Apply');
  // Tạo tuần tự trên cùng biểu mẫu UI; chờ từng lần nộp hoàn tất trước khi nhập đơn tiếp theo.
  dates.forEach((date) => {
    const alias = `seedLeaveRequest${++searchSequence}`;
    cy.intercept('POST', /\/api\/v2\/leave\/leave-requests(?:\?|$)/).as(alias);
    ctx.prepareApply(date, date, type, comment, true);
    cy.field('Duration').should('contain.text', 'Full Day');
    cy.contains('button', /^\s*Apply\s*$/).click();
    cy.wait(`@${alias}`, { requestTimeout: 20000 }).its('response.statusCode').should('be.oneOf', [200, 201]);
    ctx.success('Successfully Saved');
    cy.ready();
  });
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
  if (options.includePast) cy.get('.orangehrm-leave-filter .oxd-switch-input').click();
  if (options.employee) cy.employeeUI(typeof options.employee === 'string' ? options.employee : options.employee.name);
  if (options.type) cy.choose('Leave Type', options.type);
  if (options.subUnit) cy.choose('Sub Unit', options.subUnit);
  if (options.statuses) ctx.selectStatuses(options.statuses);
  return searchLeave();
};

// Ô Date có thể kèm "to", Half Day hoặc khoảng giờ; đối chiếu mốc đầu và loại phép riêng.
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
ctx.rowMenu = (row, item) => {
  row.find('.oxd-table-dropdown button').click();
  return cy.contains('.oxd-table-dropdown-item', new RegExp(`^\\s*${Cypress._.escapeRegExp(item)}\\s*$`)).click();
};
ctx.balance = (date, type = ctx.annualType) => {
  ctx.loginEmployee();
  const iso = date.iso || `${date.year}-${date.month}-${date.day}`;
  const alias = `leaveBalance${++searchSequence}`;
  cy.intercept({ method: 'GET', url: '**/api/v2/leave/leave-balance/leave-type/**',
    query: { fromDate: iso, toDate: iso } }).as(alias);
  ctx.prepareApply(date, date, type);
  cy.wait(`@${alias}`, { requestTimeout: 20000 }).its('response.statusCode').should('eq', 200);
  cy.ready();
  return cy.get('.orangehrm-leave-balance-text').should('be.visible').invoke('text')
    .should((text) => {
      expect(text, 'Số dư đã hiển thị trên UI').to.match(/^\s*-?\d+(?:\.\d+)?\s*(?:Day\(s\))?\s*$/);
      // Các fixture còn đủ phép; chờ UI cập nhật thay cho giá trị 0.00 lúc khởi tạo.
      expect(Number.parseFloat(text), 'Fixture còn số dư dương').to.be.greaterThan(0);
    }).then((text) => {
    const value = Number.parseFloat(text.trim());
    expect(value, 'Số dư đọc từ UI Apply').to.be.finite;
    return value;
  });
};

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
    expect(index, 'Kỳ phép chứa ngày hiện tại').to.be.at.least(1);
    expect(index, 'Có kỳ kế tiếp cho dữ liệu tương lai').to.be.lessThan(periods.length - 1);
    ctx.currentPeriod = periods[index];
    ctx.previousPeriod = periods[index - 1];
    ctx.targetPeriod = periods[index + 1];
    ctx.currentYear = ctx.currentPeriod.from.year;
    ctx.targetYear = ctx.targetPeriod.from.year;
  });
  cy.field('Entitlement').find('input').click();
}

// Đọc ngày lễ của các năm trong kỳ thật để chọn đủ tuần làm việc không có ngày lễ.
function prepareCalendar() {
  const holidays = new Set();
  cy.then(() => {
  [...new Set([currentYear, ctx.targetPeriod.from.year, ctx.targetPeriod.to.year])].forEach((year) => {
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
    const dates = buildDates(holidays);
    dates.forEach((date, index) => Object.assign(ctx.dates[index], date));
    const today = new Date();
    const current = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 3, 12);
    if (String(current.getFullYear()) !== currentYear) current.setFullYear(Number(currentYear), 11, 20);
    while ([0, 6].includes(current.getDay()) || holidays.has(dateObject(current).iso)) current.setDate(current.getDate() + 1);
    const past = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 14, 12);
    if (String(past.getFullYear()) !== currentYear) past.setFullYear(Number(currentYear), 0, 5);
    while ([0, 6].includes(past.getDay()) || holidays.has(dateObject(past).iso)) past.setDate(past.getDate() - 1);
    ctx.currentDate = dateObject(current);
    ctx.pastDate = dateObject(past);
    ctx.pastRequestDate = ctx.pastDate;
  });
  });
}

ctx.preparePastEmployee = (employee, type, date) => {
  ctx.loginAdmin();
  cy.then(() => {
    if (!employee.created) {
      createdEmployees.push(employee);
      createEmployee(ctx, employee);
    }
  });
  ctx.addType(type);
  ctx.grant(employee.name, type, ctx.currentYear, 20);
  ctx.loginEmployee(employee);
  ctx.applyRequest(date, date, { type, comment: `Đơn trước khi thôi việc ${runId}` });
  ctx.loginAdmin();
  ctx.filterAdmin({ employee, type, from: date, to: date, statuses: ['Pending Approval'] });
  ctx.requestRow(date, { type }).should('contain.text', employee.name);
  terminateEmployee(ctx, employee, ctx.currentDate);
};

// Ghi lại fixture thật để đối chiếu Excel và dọn đúng dữ liệu nếu lần chạy bị gián đoạn.
function writeRunData(cleanupCompleted = false) {
  return cy.then(() => cy.writeFile(Cypress.env('runDataFile') || 'evidence/executions/22130254/run-data.json', {
    runId,
    recordedAt: new Date().toISOString(),
    cleanupAttempted: Boolean(ctx.cleanupAttempted),
    cleanupCompleted,
    fixtures: [ctx.employee, ctx.employeeB, ctx.employeeNoEnt, ctx.employeePast].filter(Boolean).map((employee) => ({
      id: employee.id, name: employee.name, username: employee.username,
      empNumber: employee.empNumber, createdDuringRun: Boolean(employee.created),
      terminated: Boolean(employee.terminated), subUnit: employee.subUnit,
    })),
    trackedTypes: [...createdTypes],
    shifts: ctx.shiftsCreated,
    periods: { previous: ctx.previousPeriod, current: ctx.currentPeriod, target: ctx.targetPeriod },
    dates: ctx.dates,
    currentDate: ctx.currentDate,
    pastDate: ctx.pastDate,
  }, { log: false }));
}

describe('22130254_CaoTienThanh_ASM7 – Kiểm thử Leave qua giao diện', () => {
  before(() => {
    // Chuẩn bị ESS chính, đối chứng và người chưa có hạn mức; người thôi việc tạo ở nhóm ADMIN.
    cy.fixture('leave').then((data) => {
      ctx.data = data;
      ['employee', 'employeeB', 'employeeNoEnt', 'employeePast'].forEach((key, index) => {
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
      if (onlyCases.length && onlyCases.every((id) => ['TC_LEAVE_CONFIG_002', 'TC_LEAVE_CONFIG_005', 'TC_LEAVE_CONFIG_011'].includes(id))) {
        writeRunData();
        return;
      }
      [ctx.employee, ctx.employeeB, ctx.employeeNoEnt].forEach((employee) => {
        createdEmployees.push(employee);
        createEmployee(ctx, employee);
      });
      cy.field('Date of Birth').find('input').invoke('attr', 'placeholder').then((placeholder) => {
        ctx.datePlaceholder = placeholder;
      });
      assignJob(ctx, ctx.employee);
      assignJob(ctx, ctx.employeeB);
      verify8hShift(ctx, ctx.employee);
      preparePeriods();
      prepareCalendar();
      ctx.addType(ctx.alternateType);
      cy.then(() => {
        ctx.grant(ctx.employee.name, ctx.alternateType, ctx.currentYear, 20);
        ctx.grant(ctx.employee.name, ctx.alternateType, ctx.targetYear, 20);
      });
      writeRunData();
    });
  });

  afterEach(function () {
    // Minh chứng – Chỉ chụp ảnh PASS khi toàn bộ các assertion của case đã hoàn tất.
    if (this.currentTest.state === 'passed') {
      const id = this.currentTest.title.match(/^TC_LEAVE_[A-Z]+_\d{3}/)[0];
      cy.screenshot(id, { capture: 'fullPage', overwrite: true });
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

// Mở danh sách PIM và chỉ quan sát các request do thao tác giao diện phát sinh.
function openEmployeeProfileUI(ctx, employee) {
  const alias = `pimOwnedSearch${++pimSetupNavigation}`;
  cy.intercept('GET', '**/api/v2/pim/employees?*').as(alias);
  cy.contains('.oxd-main-menu-item', /^\s*PIM\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
  cy.ready();
  cy.field('Employee Id').find('input').clear().type(String(employee.id));
  cy.choose('Include', 'Current and Past Employees');
  cy.contains('button', /^\s*Search\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
  cy.ready();
  cy.ownRow(String(employee.id))
    .should('contain.text', employee.firstName || ctx.data.employeeFirstName)
    .and('contain.text', employee.lastName);
  cy.ownRow(String(employee.id)).find('.bi-pencil-fill').click();
  cy.contains('h6', /^\s*Personal Details\s*$/).should('be.visible');
  cy.ready();
  cy.field('Employee Id').find('input').should('have.value', String(employee.id));
  return cy.location('pathname').then((pathname) => {
    const match = pathname.match(/\/empNumber\/(\d+)/);
    expect(match, 'Mã hồ sơ được đọc từ URL của giao diện').not.to.eq(null);
    employee.empNumber = match[1];
  });
}

function openEmployeeJobUI(ctx, employee) {
  openEmployeeProfileUI(ctx, employee);
  const alias = `pimOwnedJob${++pimSetupNavigation}`;
  cy.intercept('GET', '**/api/v2/pim/employees/*/job-details').as(alias);
  cy.contains('.orangehrm-tabs-item', /^\s*Job\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
  cy.contains('h6', /^\s*Job Details\s*$/).should('be.visible');
  return cy.ready();
}

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
    if (ctx.trackEmployee) ctx.trackEmployee(employee);
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

function assignJob(ctx, employee, subUnit) {
  // Arrange – Mở đúng hồ sơ có mã và họ tên của lần chạy này, rồi chuyển đến Job.
  openEmployeeJobUI(ctx, employee);
  cy.field('Sub Unit').find('.oxd-select-text').click();

  // Act – Đọc cấp cây từ class hiển thị và chỉ chọn hai đơn vị lá không chứa nhau.
  return cy.get('.oxd-select-option').then(($options) => {
    const units = [...$options].map((option) => ({
      label: option.innerText.trim(),
      indent: [...option.classList].map((name) => name.match(/^--indent-(\d+)$/))
        .find(Boolean),
    })).filter((unit) => unit.label && unit.label !== '-- Select --');
    expect(units.length, 'Có ít nhất hai đơn vị hiện hữu').to.be.at.least(2);
    const ancestors = [];
    units.forEach((unit, index) => {
      expect(unit.indent, `Cấp cây hiển thị của ${unit.label}`).not.to.eq(undefined);
      unit.level = Number(unit.indent[1]);
      while (ancestors.length && ancestors[ancestors.length - 1].level >= unit.level) ancestors.pop();
      unit.parent = ancestors.length ? ancestors[ancestors.length - 1].index : -1;
      unit.index = index;
      ancestors.push(unit);
    });
    // Trong danh sách preorder, nút không có cấp sâu hơn ngay phía sau là đơn vị lá.
    const leaves = units.filter((unit, index) => index === units.length - 1 || units[index + 1].level <= unit.level);
    expect(leaves.length, 'Có hai đơn vị lá để loại trừ fixture đối chứng').to.be.at.least(2);
    if (!ctx.subUnitA || !ctx.subUnitB) {
      const administration = leaves.find((unit) => unit.label === 'Administration');
      const quality = leaves.find((unit) => unit.label === 'Quality Assurance');
      let pair;
      if (administration && quality) {
        pair = [administration, quality];
      } else {
        const siblings = leaves.reduce((groups, unit) => {
          (groups[unit.parent] ||= []).push(unit);
          return groups;
        }, {});
        pair = Object.values(siblings).find((group) => group.length >= 2)?.slice(-2) || leaves.slice(-2);
      }
      ctx.subUnitA = pair[0].label;
      ctx.subUnitB = pair[1].label;
    }
    expect(ctx.subUnitA).not.to.eq(ctx.subUnitB);
    expect(leaves.map((unit) => unit.label), 'A phải thuộc đơn vị lá').to.include(ctx.subUnitA);
    expect(leaves.map((unit) => unit.label), 'B phải thuộc đơn vị lá').to.include(ctx.subUnitB);
    const selected = subUnit || (employee.id === ctx.employee.id ? ctx.subUnitA : ctx.subUnitB);
    expect(leaves.map((unit) => unit.label), 'Đơn vị đã chọn là nút lá hiện hữu').to.include(selected);
    cy.contains('.oxd-select-option', new RegExp(`^\\s*${Cypress._.escapeRegExp(selected)}\\s*$`)).click();
    // Job Details có thể kèm form Custom Fields; chỉ lưu form chứa Sub Unit.
    cy.field('Sub Unit').closest('form').contains('button', /^\s*Save\s*$/).click();

    // Assert – Giao diện xác nhận cập nhật và giữ đúng Sub Unit sau khi tải lại hồ sơ.
    cy.contains('.oxd-toast', 'Successfully Updated').should('be.visible');
    cy.ready();
    cy.then(() => { employee.subUnit = selected; });
    openEmployeeJobUI(ctx, employee);
    cy.field('Sub Unit').find('.oxd-select-text').should('contain.text', selected);
    return cy.then(() => selected);
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
    if (ctx.trackShift) ctx.trackShift(shiftName);
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

function terminateEmployee(ctx, employee, date) {
  // Arrange – Chỉ mở Job của fixture có đúng mã và tên; giữ lịch sử Leave để kiểm tra Past Employee.
  openEmployeeJobUI(ctx, employee);
  cy.contains('button', /^\s*Terminate Employment\s*$/).click();
  cy.contains('.oxd-dialog-sheet', 'Terminate Employment').should('be.visible');
  const parts = date instanceof Date ? {
    year: String(date.getFullYear()),
    month: String(date.getMonth() + 1).padStart(2, '0'),
    day: String(date.getDate()).padStart(2, '0'),
  } : typeof date === 'string' ? (() => {
    const match = date.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    expect(match, 'Ngày chấm dứt theo ISO hoặc đối tượng ngày').not.to.eq(null);
    return { year: match[1], month: match[2], day: match[3] };
  })() : date;
  let formattedDate;
  cy.field('Termination Date').find('input').invoke('attr', 'placeholder').then((placeholder) => {
    formattedDate = placeholder.replace(/yyyy|mm|dd/gi, (token) => parts[{
      yyyy: 'year', mm: 'month', dd: 'day',
    }[token.toLowerCase()]]);
    cy.field('Termination Date').find('input').clear().type(formattedDate).blur();
  });
  cy.field('Termination Reason').find('.oxd-select-text').click();
  cy.get('.oxd-select-option').then(($options) => {
    const labels = [...$options].map((option) => option.innerText.trim())
      .filter((label) => label && label !== '-- Select --');
    expect(labels.length, 'Có lý do chấm dứt hiện hữu trên giao diện').to.be.at.least(1);
    const reason = ctx.data.terminationReason || labels[0];
    cy.contains('.oxd-select-option', new RegExp(`^\\s*${Cypress._.escapeRegExp(reason)}\\s*$`)).click();
  });
  cy.field('Note').find('textarea').type(`Dữ liệu kiểm thử ASM7 của Cao Tiến Thành – ${ctx.runId}`);

  // Act – Save trên modal Terminate Employment, không xóa nhân viên.
  const alias = `asm7OwnedTermination${++pimSetupNavigation}`;
  cy.intercept('POST', '**/api/v2/pim/employees/*/terminations').as(alias);
  cy.contains('.oxd-dialog-sheet button', /^\s*Save\s*$/).click();
  cy.wait(`@${alias}`).its('response.statusCode').should('be.oneOf', [200, 201]);
  cy.contains('.oxd-toast', 'Successfully Updated').should('be.visible');
  cy.then(() => { employee.terminated = true; });

  // Assert – Hồ sơ hiển thị Past Employee và đúng ngày chấm dứt; dữ liệu Leave vẫn thuộc hồ sơ này.
  cy.contains('button', /^\s*Activate Employment\s*$/).should('be.visible');
  cy.contains('.orangehrm-edit-employee-name', 'Past Employee').should('be.visible');
  return cy.get('.orangehrm-terminate-date').should(($date) => {
    expect($date.text(), 'Ngày chấm dứt hiển thị').to.include(formattedDate);
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
    const situationalType = `ASM7_Sit_${ctx.runId}`;
    const editType = `ASM7_Edit_${ctx.runId}`;
    const updatedType = `ASM7_Updated_${ctx.runId}`;
    const deleteType = `ASM7_Delete_${ctx.runId}`;
    const cancelType = `ASM7_Cancel_${ctx.runId}`;
    const boundaryPrefix = `ASM7_${ctx.runId}_`;
    const name50 = boundaryPrefix + 'A'.repeat(50 - boundaryPrefix.length);
    const name51 = boundaryPrefix + 'B'.repeat(51 - boundaryPrefix.length);
    let previousYear;
    before(() => {
      cy.then(() => { previousYear = ctx.previousPeriod?.from?.year || String(Number(ctx.currentYear) - 1); });
    });

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

    it('TC_LEAVE_CONFIG_003 – Bỏ trống trường Leave Type Name', () => {
      // Arrange – Mở biểu mẫu Add Leave Type mới và giữ Name rỗng.
      openAddType();
      cy.field('Name').find('input').should('have.value', '');

      // Act – Nhấn Save khi chưa nhập tên.
      ctx.save();

      // Assert – Có Required và biểu mẫu vẫn mở, không tạo bản ghi.
      cy.field('Name').contains('Required').should('be.visible');
      cy.contains('h6', /^\s*Add Leave Type\s*$/).should('be.visible');
    });

    it('TC_LEAVE_CONFIG_009 – Hủy thao tác thêm mới Leave Type', () => {
      // Arrange – Dùng biểu mẫu chưa lưu của case 003 và nhập tên riêng.
      ctx.input('Name', cancelType);

      // Act – Nhấn Cancel thay vì Save.
      ctx.cancel();
      cy.ready();

      // Assert – Trở về danh sách và tên đã hủy không xuất hiện.
      cy.contains('h6', /^\s*Leave Types\s*$/).should('be.visible');
      cy.contains('.oxd-table-cell', cancelType).should('not.exist');
    });

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

    it('TC_LEAVE_CONFIG_002 – Thêm mới Leave Type với Situational bằng Yes', () => {
      // Arrange – Mở biểu mẫu và chọn Yes cho loại phép tình huống riêng.
      openAddType();
      ctx.input('Name', situationalType);
      cy.contains('.oxd-radio-wrapper', /^\s*Yes\s*$/).find('.oxd-radio-input').click();
      cy.contains('.oxd-radio-wrapper', /^\s*Yes\s*$/).find('input').should('be.checked');
      ctx.trackType(situationalType);

      // Act – Lưu loại phép và mở lại qua icon Edit trên đúng dòng.
      ctx.save();
      ctx.success('Successfully Saved');
      cy.location('pathname').should('include', '/leaveTypeList');
      cy.ready();
      ctx.openTypeEdit(situationalType);

      // Assert – Tên và giá trị Yes đã được lưu bền vững.
      cy.field('Name').find('input').should('have.value', situationalType);
      cy.contains('.oxd-radio-wrapper', /^\s*Yes\s*$/).find('input').should('be.checked');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_004 – Nhập Leave Type Name chỉ chứa khoảng trắng', () => {
      // Arrange – Nhập đúng ba dấu cách vào Name của biểu mẫu mới.
      openAddType();
      ctx.input('Name', '   ');

      // Act – Thử lưu chuỗi chỉ có khoảng trắng.
      ctx.save();

      // Assert – Required chặn tạo loại phép, biểu mẫu không đóng.
      cy.field('Name').contains('Required').should('be.visible');
      cy.contains('h6', /^\s*Add Leave Type\s*$/).should('be.visible');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_005 – Leave Type Name đúng biên tối đa 50 ký tự', () => {
      // Arrange – Tạo chuỗi tên riêng có đúng 50 ký tự và Situational bằng No.
      expect(name50.length).to.eq(50);
      openAddType();
      ctx.input('Name', name50);
      cy.field('Name').find('input').should('have.value', name50);
      ctx.trackType(name50);

      // Act – Lưu và mở lại chính bản ghi vừa tạo.
      ctx.save();
      ctx.success('Successfully Saved');
      cy.location('pathname').should('include', '/leaveTypeList');
      cy.ready();
      ctx.openTypeEdit(name50);

      // Assert – Name lưu đầy đủ 50 ký tự, không bị cắt khi đọc lại.
      cy.field('Name').find('input').should('have.value', name50)
        .invoke('val').should('have.length', 50);
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_006 – Leave Type Name vượt biên với 51 ký tự', () => {
      // Arrange – Chuẩn bị tên riêng dài 51 ký tự và mở biểu mẫu Add.
      expect(name51.length).to.eq(51);
      openAddType();

      // Act – Nhập toàn bộ chuỗi để kiểm tra ký tự thứ 51.
      ctx.input('Name', name51);

      // Assert – Input chặn ký tự thứ 51 hoặc validation chặn lưu tên dài 51.
      cy.field('Name').find('input').invoke('val').then((value) => {
        if (value.length === 50) {
          expect(value, 'Giá trị thực nhận khi UI giới hạn độ dài').to.eq(name51.slice(0, 50));
          cy.log('UI chỉ nhận 50 ký tự; không lưu chuỗi 51 ký tự.');
        } else {
          expect(value).to.eq(name51);
          ctx.save();
          cy.field('Name').find('.oxd-input-field-error-message')
            .should('be.visible').invoke('text').should('match', /50.*characters|characters.*50/i);
        }
      });
      ctx.cancel();
      cy.contains('.oxd-table-cell', name51).should('not.exist');
    });

    it('TC_LEAVE_CONFIG_007 – Nhập Leave Type Name trùng với tên đã tồn tại', () => {
      // Arrange – Annual được case 001 tạo; nhập lại đúng tên trong biểu mẫu mới.
      openAddType();
      ctx.input('Name', ctx.annualType);

      // Act – Thử lưu loại phép có tên trùng hoàn toàn.
      ctx.save();

      // Assert – Already exists chặn lưu và danh sách chỉ có một dòng tên này.
      cy.field('Name').contains('Already exists').should('be.visible');
      ctx.cancel();
      cy.ready();
      cy.get('.oxd-table-cell').filter((_, cell) => cell.innerText.trim() === ctx.annualType)
        .should('have.length', 1);
    });

    it('TC_LEAVE_CONFIG_010 – Chỉnh sửa tên Leave Type thành tên mới hợp lệ', () => {
      // Arrange – Tạo loại phép chỉnh sửa riêng để giữ Annual cho SELF và ADMIN.
      ctx.addType(editType, 'No');
      ctx.openTypeEdit(editType);
      ctx.input('Name', updatedType);
      ctx.trackType(updatedType);

      // Act – Lưu tên mới rồi mở lại danh sách qua menu.
      ctx.save();
      ctx.success('Successfully Updated');
      cy.location('pathname').should('include', '/leaveTypeList');
      cy.ready();
      ctx.openTypes();

      // Assert – Có tên mới và không còn tên cũ trong danh sách.
      ctx.typeRow(updatedType).should('be.visible');
      cy.contains('.oxd-table-cell', editType).should('not.exist');
    });

    it('TC_LEAVE_CONFIG_011 – Chỉnh sửa Situational từ No sang Yes', () => {
      // Arrange – Mở loại phép riêng đã đổi tên và kiểm tra baseline No.
      cy.then(() => {
        if (!createdTypes.has(updatedType)) ctx.addType(updatedType, 'No');
      });
      ctx.openTypeEdit(updatedType);
      cy.contains('.oxd-radio-wrapper', /^\s*No\s*$/).find('input').should('be.checked');
      cy.contains('.oxd-radio-wrapper', /^\s*Yes\s*$/).find('.oxd-radio-input').click();
      cy.contains('.oxd-radio-wrapper', /^\s*Yes\s*$/).find('input').should('be.checked');

      // Act – Lưu thay đổi rồi mở lại bản ghi qua giao diện.
      ctx.save();
      ctx.success('Successfully Updated');
      cy.location('pathname').should('include', '/leaveTypeList');
      cy.ready();
      ctx.openTypeEdit(updatedType);

      // Assert – Yes vẫn được chọn khi biểu mẫu tải lại.
      cy.contains('.oxd-radio-wrapper', /^\s*Yes\s*$/).find('input').should('be.checked');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_012 – Chỉnh sửa Leave Type nhưng xóa trống Name', () => {
      // Arrange – Mở bản ghi chỉnh sửa riêng và xóa toàn bộ Name.
      ctx.openTypeEdit(updatedType);
      cy.field('Name').find('input').clear();

      // Act – Thử Save với tên rỗng.
      ctx.save();

      // Assert – Required chặn lưu và tên cũ còn nguyên khi mở lại.
      cy.field('Name').contains('Required').should('be.visible');
      ctx.cancel();
      ctx.openTypeEdit(updatedType);
      cy.field('Name').find('input').should('have.value', updatedType);
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_013 – Chỉnh sửa Leave Type nhưng bấm Cancel', () => {
      // Arrange – Nhập tên tạm khác trên bản ghi riêng đang có.
      const temporaryName = `ASM7_Temporary_${ctx.runId}`;
      ctx.openTypeEdit(updatedType);
      ctx.input('Name', temporaryName);

      // Act – Hủy thay đổi bằng Cancel.
      ctx.cancel();
      cy.ready();

      // Assert – Tên trước khi sửa còn nguyên và tên tạm không xuất hiện.
      ctx.typeRow(updatedType).should('be.visible');
      cy.contains('.oxd-table-cell', temporaryName).should('not.exist');
    });

    it('TC_LEAVE_CONFIG_014 – Xóa Leave Type riêng và xác nhận trên modal', () => {
      // Arrange – Tạo loại phép chỉ dùng cho xóa và xác minh đúng dòng sở hữu.
      ctx.addType(deleteType, 'No');
      ctx.typeRow(deleteType).should('be.visible');

      // Act – Nhấn Delete trên dòng fixture và xác nhận Yes, Delete.
      ctx.typeRow(deleteType).find('.bi-trash').click();
      cy.contains('.oxd-dialog-sheet', 'Are you Sure?').should('be.visible');
      cy.contains('.oxd-dialog-sheet button', /^\s*Yes, Delete\s*$/).click();

      // Assert – Có Successfully Deleted và bản ghi biến mất.
      ctx.success('Successfully Deleted');
      cy.ready();
      cy.contains('.oxd-table-cell', deleteType).should('not.exist');
    });

    it('TC_LEAVE_CONFIG_015 – Hủy thao tác xóa Leave Type trên modal', () => {
      // Arrange – Mở danh sách và chọn loại phép chỉnh sửa riêng còn tồn tại.
      ctx.openTypes();
      ctx.typeRow(updatedType).find('.bi-trash').click();
      cy.contains('.oxd-dialog-sheet', 'Are you Sure?').should('be.visible');

      // Act – Nhấn No, Cancel trên hộp xác nhận.
      cy.contains('.oxd-dialog-sheet button', /^\s*No, Cancel\s*$/).click();

      // Assert – Modal đóng và bản ghi vẫn còn nguyên.
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.typeRow(updatedType).should('be.visible');
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

    it('TC_LEAVE_CONFIG_017 – Thêm Leave Entitlement hợp lệ với 7.50 ngày', () => {
      // Arrange – Dùng cùng ESS và Annual ở kỳ trước để giữ baseline 20 ngày của kỳ xin nghỉ.
      ctx.openEntitlements(ctx.employee.name, ctx.annualType, previousYear);
      assertTotal(0);
      prepareEntitlement('7.50', previousYear);

      // Act – Lưu hạn mức thập phân và xác nhận qua modal.
      confirmEntitlement();

      // Assert – Có Successfully Saved; Days và tổng tăng chính xác 7.50.
      ctx.success('Successfully Saved');
      ctx.openEntitlements(ctx.employee.name, ctx.annualType, previousYear);
      assertEntitlements(ctx.annualType, previousYear, 7.5);
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

    it('TC_LEAVE_CONFIG_018 – Thêm Leave Entitlement nhưng bỏ trống Employee Name', () => {
      // Arrange – Để Employee Name rỗng; chọn Annual, kỳ và nhập 10 ngày.
      openAddEntitlements();
      cy.choose('Leave Type', ctx.annualType);
      ctx.selectPeriod(ctx.targetYear);
      ctx.input('Entitlement', '10.00');

      // Act – Thử Save khi chưa chọn nhân viên.
      ctx.save();

      // Assert – Employee Name báo Required, chưa có modal lưu hạn mức.
      cy.field('Employee Name').contains('Required').should('be.visible');
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_019 – Nhập Employee Name không tồn tại khi thêm Entitlement', () => {
      // Arrange – Mở biểu mẫu mới và chuẩn bị tên không tồn tại mang runId.
      openAddEntitlements();
      const missingName = `NonExistentEmployee_${ctx.runId}`;

      // Act – Gõ tên nhân viên không tồn tại vào autocomplete.
      ctx.input('Employee Name', missingName);

      // Assert – No Records Found và không có lựa chọn nhân viên hợp lệ.
      cy.contains('.oxd-autocomplete-option', 'No Records Found').should('be.visible');
      cy.field('Employee Name').find('input').should('have.value', missingName);
      cy.field('Employee Name').find('input').blur();
      cy.choose('Leave Type', ctx.annualType);
      ctx.selectPeriod(ctx.targetYear);
      ctx.input('Entitlement', '10.00');
      ctx.save();
      cy.field('Employee Name').contains('Invalid').should('be.visible');
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_020 – Thêm Leave Entitlement nhưng không chọn Leave Type', () => {
      // Arrange – Chọn ESS và kỳ, nhập 10 ngày; giữ Leave Type ở trạng thái chưa chọn.
      openAddEntitlements();
      cy.employeeUI(ctx.employee.name);
      ctx.selectPeriod(ctx.targetYear);
      ctx.input('Entitlement', '10.00');
      cy.field('Leave Type').find('.oxd-select-text').should('contain.text', '-- Select --');

      // Act – Save khi thiếu loại phép.
      ctx.save();

      // Assert – Leave Type báo Required và không có modal cập nhật.
      cy.field('Leave Type').contains('Required').should('be.visible');
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_021 – Thêm Leave Entitlement nhưng để trống Entitlement', () => {
      // Arrange – Chọn ESS, Annual, kỳ và giữ Entitlement rỗng.
      prepareEntitlement(undefined);
      cy.field('Entitlement').find('input').should('have.value', '');

      // Act – Thử Save với số ngày trống.
      ctx.save();

      // Assert – Entitlement báo Required và không lưu dữ liệu.
      cy.field('Entitlement').contains('Required').should('be.visible');
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_022 – Nhập Entitlement bằng chữ và ký tự đặc biệt', () => {
      // Arrange – Nhập abc@#$ sau khi chọn đủ nhân viên, loại phép và kỳ.
      prepareEntitlement('abc@#$');

      // Act – Thử Save giá trị phi số.
      ctx.save();

      // Assert – Validation số tối đa 2 chữ số thập phân chặn lưu.
      cy.field('Entitlement').find('.oxd-input-field-error-message')
        .should('be.visible').invoke('text').should('match', /Should be a number.*2 decimal places/i);
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_023 – Nhập Entitlement là số âm', () => {
      // Arrange – Chọn đúng fixture và nhập -5.00 ngày.
      prepareEntitlement('-5.00');

      // Act – Thử Save hạn mức âm.
      ctx.save();

      // Assert – Quy tắc số không âm báo lỗi và không mở modal cập nhật.
      cy.field('Entitlement').find('.oxd-input-field-error-message')
        .should('be.visible').invoke('text').should('match', /Should be a number.*2 decimal places/i);
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_024 – Nhập Entitlement có nhiều hơn 2 chữ số thập phân', () => {
      // Arrange – Nhập 10.555 ngày trong biểu mẫu đã chọn đủ fixture.
      prepareEntitlement('10.555');

      // Act – Thử Save số có 3 chữ số sau dấu thập phân.
      ctx.save();

      // Assert – Lỗi yêu cầu tối đa 2 chữ số thập phân chặn lưu dữ liệu.
      cy.field('Entitlement').find('.oxd-input-field-error-message')
        .should('be.visible').invoke('text').should('match', /Should be a number.*2 decimal places/i);
      cy.get('.oxd-dialog-sheet').should('not.exist');
      ctx.cancel();
    });

    it('TC_LEAVE_CONFIG_025 – Hủy thao tác thêm Leave Entitlement khi đang nhập liệu', () => {
      // Arrange – Ghi baseline 20 ngày của A/Annual/kỳ hiện tại và nhập thêm 20.00 ngày.
      ctx.openEntitlements(ctx.employee.name, ctx.annualType, ctx.currentYear);
      assertEntitlements(ctx.annualType, ctx.currentYear, 20);
      prepareEntitlement('20.00', ctx.currentYear);

      // Act – Nhấn Cancel, sau đó tìm lại đúng tổ hợp hạn mức qua UI.
      ctx.cancel();
      ctx.openEntitlements(ctx.employee.name, ctx.annualType, ctx.currentYear);

      // Assert – Tổng vẫn 20 ngày và chỉ có một dòng, không tăng do thao tác đã hủy.
      assertEntitlements(ctx.annualType, ctx.currentYear, 20);
    });
  });
}

function registerSelfCases(ctx) {
  // Đọc ngày khi case chạy để nhận lịch làm việc và ngày nghỉ lễ đã chuẩn bị qua UI.
  const day = (index) => ctx.dates[index];
  const allStatuses = ['Rejected', 'Cancelled', 'Pending Approval', 'Scheduled', 'Taken'];
  let searchNumber = 0;
  const apply = () => cy.contains('button', /^\s*Apply\s*$/).click();
  const search = () => {
    const alias = `selfSearch${++searchNumber}`;
    cy.intercept('GET', /\/api\/v2\/leave\/leave-requests(?:\?|$)/).as(alias);
    cy.contains('button', /^\s*Search\s*$/).click();
    cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
    cy.ready();
  };
  const clearDate = (label) => cy.field(label).find('input')
    .type('{selectall}{backspace}').blur().should('have.value', '');

  // Đối chiếu đúng cột Number of Days, tránh nhầm với số dư hoặc số ngày trong Status.
  function assertDays(date, expected, options = {}) {
    ctx.requestRow(date, options).find('.oxd-table-cell').eq(5)
      .invoke('text').then((value) => {
        expect(Number(value.trim()), 'Số ngày nghỉ trên đúng dòng đơn').to.eq(expected);
      });
  }

  // Nhãn giờ là From/To; khớp hoàn toàn để không chọn nhầm From Date/To Date.
  function timeInput(label) {
    return cy.contains('.oxd-label', new RegExp(`^\\s*${label}\\s*\\*?\\s*$`))
      .closest('.oxd-input-group').find('.oxd-time-input input');
  }

  // Đọc comment đã lưu qua hộp thoại trên UI, không lấy dữ liệu từ API.
  function openComments(date) {
    ctx.requestRow(date).find('.oxd-table-dropdown button').click();
    cy.contains('.oxd-table-dropdown-item', /^\s*Add Comment\s*$/).click();
    cy.contains('.oxd-dialog-sheet', 'Leave Request Comments').should('be.visible');
    cy.ready();
  }

  function noCancel(date) {
    ctx.requestRow(date).should(($row) => {
      const buttons = $row.find('button').filter((_, button) => /^\s*Cancel\s*$/.test(button.innerText));
      expect(buttons.toArray().every((button) => button.disabled),
        'Không có nút Cancel khả dụng trên đơn').to.eq(true);
    });
  }

  // Chuẩn bị trạng thái qua hai tài khoản thật: ESS nộp, Admin xét duyệt bằng nút trên dòng.
  function prepareAdminState(date, action, status) {
    ctx.loginEmployee();
    ctx.applyRequest(date, date, { comment: `ASM7 SELF ${status} ${ctx.runId}` });
    ctx.loginAdmin();
    ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType,
      from: date, to: date, statuses: ['Pending Approval'] });
    ctx.requestRow(date).should('contain.text', ctx.employee.name)
      .and('contain.text', 'Pending Approval');
    ctx.requestRow(date).contains('button', new RegExp(`^\\s*${action}\\s*$`)).click();
    ctx.success('Successfully Updated');
    ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType,
      from: date, to: date, statuses: [status] });
    ctx.requestRow(date).should('contain.text', status);
    ctx.loginEmployee();
  }

  if (onlyCases.length && !onlyCases.some((id) => id.startsWith('TC_LEAVE_SELF_'))) return;
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

    it('TC_LEAVE_SELF_002 – Nộp đơn nghỉ ba ngày làm việc liên tiếp', () => {
      // Arrange – Dùng ba ngày thứ Hai đến thứ Tư đã được loại ngày nghỉ lễ khi chuẩn bị.
      ctx.prepareApply(day(5), day(7), ctx.annualType, `ASM7 ba ngày ${ctx.runId}`);

      // Act – Nộp đơn nhiều ngày và tìm lại khoảng ngày trong My Leave.
      apply();
      ctx.success('Successfully Saved');
      ctx.myLeave({ from: day(5), to: day(7), type: ctx.annualType });

      // Assert – Một đơn Pending Approval bao phủ đủ ba ngày, Number of Days bằng 3.00.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length', 1);
      ctx.requestRow(day(5), { to: day(7) }).should('contain.text', 'Pending Approval');
      assertDays(day(5), 3, { to: day(7) });
    });

    it('TC_LEAVE_SELF_003 – Nộp đơn nghỉ nửa ngày buổi sáng', () => {
      let previousBalance;
      // Arrange – Ghi nhận số dư đúng kỳ và chọn Half Day - Morning trên ngày riêng.
      ctx.balance(day(10)).then((value) => { previousBalance = value; });
      ctx.prepareApply(day(10));
      cy.choose('Duration', 'Half Day - Morning');

      // Act – Nộp đơn nửa ngày và tìm đơn vừa tạo.
      apply();
      ctx.success('Successfully Saved');
      ctx.myLeave({ from: day(10), to: day(10), type: ctx.annualType });

      // Assert – Đơn Pending Approval có Half Day, 0.50 ngày và số dư giảm đúng 0.50.
      ctx.requestRow(day(10)).should('contain.text', 'Half Day').and('contain.text', 'Pending Approval');
      assertDays(day(10), 0.5);
      ctx.balance(day(10)).then((value) => {
        expect(value, 'Số dư sau khi nộp đơn nửa ngày').to.be.closeTo(previousBalance - 0.5, 0.001);
      });
    });

    it('TC_LEAVE_SELF_004 – Không chọn Leave Type khi nộp đơn', () => {
      // Arrange – Mở biểu mẫu mới, chỉ nhập hai ngày hợp lệ và giữ Leave Type chưa chọn.
      cy.leavePage('Apply');
      ctx.enterDate('From Date', day(1));
      ctx.enterDate('To Date', day(1));
      cy.field('Leave Type').should('contain.text', 'Select');

      // Act – Nhấn Apply khi chưa chọn loại phép.
      apply();

      // Assert – Leave Type báo Required và vẫn ở biểu mẫu Apply Leave.
      cy.field('Leave Type').contains('Required').should('be.visible');
      cy.contains('h6', /^\s*Apply Leave\s*$/).should('be.visible');
    });

    it('TC_LEAVE_SELF_005 – Bỏ trống From Date khi nộp đơn', () => {
      // Arrange – Chọn loại phép, chỉ nhập To Date và xác nhận From Date vẫn rỗng.
      cy.leavePage('Apply');
      cy.choose('Leave Type', ctx.annualType);
      ctx.enterDate('To Date', day(1));
      cy.field('From Date').find('input').should('have.value', '');

      // Act – Nhấn Apply với From Date rỗng.
      apply();

      // Assert – From Date báo Required; đơn chưa được gửi.
      cy.field('From Date').contains('Required').should('be.visible');
      cy.contains('h6', /^\s*Apply Leave\s*$/).should('be.visible');
    });

    it('TC_LEAVE_SELF_006 – Bỏ trống To Date khi nộp đơn', () => {
      // Arrange – Nhập From Date rồi xóa To Date được hệ thống tự điền.
      ctx.prepareApply(day(1));
      clearDate('To Date');

      // Act – Nhấn Apply sau khi xác nhận To Date rỗng.
      apply();

      // Assert – To Date báo Required và biểu mẫu không nộp đơn.
      cy.field('To Date').contains('Required').should('be.visible');
      cy.contains('h6', /^\s*Apply Leave\s*$/).should('be.visible');
    });

    it('TC_LEAVE_SELF_007 – Chọn To Date trước From Date', () => {
      // Arrange – Chọn loại phép và nhập From Date sau To Date.
      ctx.prepareApply(day(2), day(1));

      // Act – Nhấn Apply cho khoảng ngày bị đảo ngược.
      apply();

      // Assert – To Date báo lỗi thứ tự ngày, không tạo đơn.
      cy.field('To Date').contains('To date should be after from date').should('be.visible');
      cy.contains('h6', /^\s*Apply Leave\s*$/).should('be.visible');
    });

    it('TC_LEAVE_SELF_008 – Nộp đơn theo giờ bằng Specify Time', () => {
      let previousBalance;
      // Arrange – Ghi số dư; xác nhận UI tự điền ca 09:00–17:00 khi chọn Specify Time.
      ctx.balance(day(11)).then((value) => { previousBalance = value; });
      ctx.prepareApply(day(11));
      cy.choose('Duration', 'Specify Time');
      timeInput('From').should('have.value', '09:00 AM');
      timeInput('To').should('have.value', '05:00 PM');

      // Act – Giữ From 09:00 AM, đổi To thành 11:00 AM và nộp đơn hai giờ.
      timeInput('To').type('{selectall}{backspace}11:00 AM').blur().should('have.value', '11:00 AM');
      cy.contains('h6', /^\s*Apply Leave\s*$/).click();
      cy.get('.orangehrm-leave-duration').should('have.text', '2.00');
      apply();
      ctx.success('Successfully Saved');
      ctx.myLeave({ from: day(11), to: day(11), type: ctx.annualType });

      // Assert – Khoảng 09:00–11:00 có 0.25 ngày trên ca tám giờ; số dư giảm 0.25.
      ctx.requestRow(day(11)).should('contain.text', '09:00').and('contain.text', '11:00')
        .and('contain.text', 'Pending Approval');
      assertDays(day(11), 0.25);
      ctx.balance(day(11)).then((value) => {
        expect(value, 'Số dư sau đơn hai giờ').to.be.closeTo(previousBalance - 0.25, 0.001);
      });
    });

    it('TC_LEAVE_SELF_009 – Nhập From Date là ngày 30 tháng Hai không tồn tại', () => {
      // Arrange – Nhập ngày sai theo đúng placeholder UI, giữ To Date hợp lệ.
      cy.leavePage('Apply');
      cy.choose('Leave Type', ctx.annualType);
      ctx.enterDate('From Date', { year: ctx.targetYear, month: '02', day: '30' });
      ctx.enterDate('To Date', { year: ctx.targetYear, month: '03', day: '02' });

      // Act – Nhấn Apply với ngày 30/02 không có trên lịch.
      apply();

      // Assert – From Date báo ngày không hợp lệ và vẫn ở biểu mẫu nộp đơn.
      cy.field('From Date').find('.oxd-input-field-error-message')
        .should('be.visible').invoke('text').should('match', /valid date|Invalid Date/i);
      cy.contains('h6', /^\s*Apply Leave\s*$/).should('be.visible');
    });

    it('TC_LEAVE_SELF_010 – Nhập To Date là ngày 31 tháng Tư không tồn tại', () => {
      // Arrange – Nhập From Date hợp lệ, To Date là ngày 31/04 theo placeholder UI.
      cy.leavePage('Apply');
      cy.choose('Leave Type', ctx.annualType);
      ctx.enterDate('From Date', { year: ctx.targetYear, month: '04', day: '28' });
      ctx.enterDate('To Date', { year: ctx.targetYear, month: '04', day: '31' });

      // Act – Nhấn Apply với To Date không tồn tại.
      apply();

      // Assert – To Date hiển thị lỗi ngày không hợp lệ, không nộp đơn.
      cy.field('To Date').find('.oxd-input-field-error-message')
        .should('be.visible').invoke('text').should('match', /valid date|Invalid Date/i);
      cy.contains('h6', /^\s*Apply Leave\s*$/).should('be.visible');
    });

    it('TC_LEAVE_SELF_011 – Apply không khả dụng khi chưa có entitlement kỳ hiện tại', () => {
      // Arrange – Đăng nhập ESS riêng chưa được cấp entitlement ở bất kỳ loại phép nào.
      ctx.loginEmployee(ctx.employeeNoEnt);

      // Act – Mở Leave > Apply bằng menu giao diện.
      cy.leavePage('Apply');

      // Assert – Có thông báo không có loại phép có số dư và không có biểu mẫu Apply.
      cy.contains('No Leave Types with Leave Balance').should('be.visible');
      cy.contains('.oxd-input-group', /^\s*Leave Type\s*/).should('not.exist');
      cy.contains('.oxd-input-group', /^\s*From Date\s*/).should('not.exist');
      cy.contains('button', /^\s*Apply\s*$/).should('not.exist');
    });

    it('TC_LEAVE_SELF_012 – Chặn đơn trùng hoàn toàn ngày đã xin nghỉ', () => {
      // Arrange – Trở về ESS chính, nhập đúng ngày của đơn Pending từ SELF_001.
      ctx.loginEmployee();
      ctx.prepareApply(day(0));

      // Act – Thử nộp đơn thứ hai trên cùng ngày rồi tìm lại đơn gốc.
      apply();
      cy.contains('h6', 'Overlapping Leave Request(s) Found').should('be.visible');
      ctx.myLeave({ from: day(0), to: day(0), type: ctx.annualType });

      // Assert – Chỉ còn một đơn ban đầu, chưa phát sinh bản ghi trùng.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length', 1);
      ctx.requestRow(day(0)).should('contain.text', 'Pending Approval');
      assertDays(day(0), 1);
    });

    it('TC_LEAVE_SELF_013 – Chặn đơn giao thoa một ngày với đơn nhiều ngày', () => {
      // Arrange – Khoảng thứ Tư đến thứ Sáu giao thoa ngày cuối của đơn SELF_002.
      ctx.prepareApply(day(7), day(9));

      // Act – Thử nộp khoảng ngày giao thoa và mở lại danh sách đúng khoảng kiểm thử.
      apply();
      cy.contains('h6', 'Overlapping Leave Request(s) Found').should('be.visible');
      cy.contains('.oxd-toast', 'Failed to Submit').should('be.visible');
      ctx.myLeave({ from: day(5), to: day(9), type: ctx.annualType });

      // Assert – Chỉ có đơn ba ngày ban đầu, không lưu thêm đơn giao thoa.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length', 1);
      ctx.requestRow(day(5), { to: day(7) }).should('contain.text', 'Pending Approval');
      assertDays(day(5), 3, { to: day(7) });
    });

    it('TC_LEAVE_SELF_014 – Lưu nguyên vẹn Comments đúng 250 ký tự', () => {
      let comment;
      // Arrange – Tạo chuỗi 250 ký tự có dấu và dấu nhận diện lần chạy, dùng ngày riêng.
      cy.then(() => {
        comment = `Nghỉ phép ASM7 ${ctx.runId} `.padEnd(250, 'á');
        expect(comment.length, 'Độ dài comment tại biên').to.eq(250);
      });
      cy.then(() => ctx.prepareApply(day(12), day(12), ctx.annualType, comment));
      cy.field('Comments').find('textarea').invoke('val').should('have.length', 250);

      // Act – Nộp đơn rồi mở hộp thoại comment của đúng đơn vừa tạo.
      apply();
      ctx.success('Successfully Saved');
      ctx.myLeave({ from: day(12), to: day(12), type: ctx.annualType });
      openComments(day(12));

      // Assert – Nội dung đã lưu khớp toàn bộ 250 ký tự, gồm chữ tiếng Việt có dấu.
      cy.get('.oxd-dialog-sheet .orangehrm-comment-message').invoke('text').then((value) => {
        expect(value.trim(), 'Comments được lưu nguyên vẹn').to.eq(comment);
        expect(value.trim().length, 'Độ dài comment sau khi lưu').to.eq(250);
      });
      cy.contains('.oxd-dialog-sheet button', /^\s*Cancel\s*$/).click();
    });

    it('TC_LEAVE_SELF_015 – Chặn Comments dài 251 ký tự', () => {
      let comment;
      // Arrange – Chuẩn bị một ngày chưa có đơn và chuỗi comment vượt giới hạn một ký tự.
      cy.then(() => { comment = `ASM7 ${ctx.runId} `.padEnd(251, 'á'); });
      cy.then(() => ctx.prepareApply(day(13), day(13), ctx.annualType, comment));

      // Act – Nhấn Apply nếu textarea nhận đủ 251 ký tự; giữ nguyên nếu UI chặn nhập.
      cy.field('Comments').find('textarea').invoke('val').then((value) => {
        if (value.length === 251) {
          apply();
          cy.field('Comments').contains('Should not exceed 250 characters').should('be.visible');
        } else {
          expect(value.length, 'UI chặn ký tự vượt biên').to.eq(250);
          expect(value, 'Phần comment được chấp nhận').to.eq(comment.slice(0, 250));
        }
      });

      // Assert – Không lưu chuỗi 251 ký tự và không có đơn mới tại ngày riêng này.
      ctx.myLeave({ from: day(13), to: day(13), type: ctx.annualType });
      cy.contains('No Records Found').should('be.visible');
      cy.get('.oxd-table-body .oxd-table-row').should('not.exist');
    });

    it('TC_LEAVE_SELF_016 – My Leave mặc định hiển thị đơn của chính nhân viên', () => {
      // Arrange – Tạo một đơn thuộc kỳ hiện tại để kiểm tra được bộ lọc mặc định thật.
      ctx.applyRequest(ctx.currentDate, ctx.currentDate, { comment: `ASM7 kỳ hiện tại ${ctx.runId}` });

      // Act – Mở mới My Leave và giữ nguyên ngày cùng các trạng thái mặc định.
      cy.leavePage('My Leave');

      // Assert – Các cột nghiệp vụ hiện đầy đủ và mọi dòng thuộc ESS đang đăng nhập.
      ['Date', 'Employee Name', 'Leave Type', 'Leave Balance', 'Number of Days', 'Status']
        .forEach((header) => cy.get('.oxd-table-header').should('contain.text', header));
      cy.field('From Date').find('input').invoke('attr', 'placeholder').then((placeholder) => {
        cy.field('From Date').find('input').should('have.value', formatDate(ctx.currentPeriod.from, placeholder));
        cy.field('To Date').find('input').should('have.value', formatDate(ctx.currentPeriod.to, placeholder));
      });
      cy.get('.oxd-table-body .oxd-table-row').should('have.length.at.least', 1).each(($row) => {
        cy.wrap($row).should('contain.text', ctx.employee.name);
      });
      ctx.requestRow(ctx.currentDate).should('contain.text', ctx.annualType);
    });

    it('TC_LEAVE_SELF_017 – Lọc My Leave theo khoảng From Date và To Date', () => {
      // Arrange – Mở My Leave, chọn khoảng riêng chỉ chứa đơn ba ngày từ SELF_002.
      cy.leavePage('My Leave');
      ctx.enterDate('From Date', day(5));
      ctx.enterDate('To Date', day(7));

      // Act – Nhấn Search với khoảng ngày đã nhập.
      search();

      // Assert – Chỉ có đơn nằm hoàn toàn trong khoảng ngày đã chọn.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length', 1);
      ctx.requestRow(day(5), { to: day(7) }).should('contain.text', ctx.employee.name);
      assertDays(day(5), 3, { to: day(7) });
    });

    it('TC_LEAVE_SELF_018 – Tìm khoảng ngày không có đơn trong My Leave', () => {
      // Arrange – Dùng một khoảng đầu năm trước; ESS vừa tạo chưa có đơn trong khoảng đó.
      cy.leavePage('My Leave');
      ctx.enterDate('From Date', { year: String(Number(ctx.currentYear) - 1), month: '01', day: '01' });
      ctx.enterDate('To Date', { year: String(Number(ctx.currentYear) - 1), month: '01', day: '05' });

      // Act – Nhấn Search và chờ giao diện hoàn tất việc tải.
      search();

      // Assert – Có No Records Found, bảng không giữ lại dòng của lần tìm trước.
      cy.contains('No Records Found').should('be.visible');
      cy.get('.oxd-table-body .oxd-table-row').should('not.exist');
    });

    it('TC_LEAVE_SELF_019 – Chỉ hiển thị trạng thái Pending Approval trong My Leave', () => {
      // Arrange – Tạo thêm một đơn rồi Cancel để cùng khoảng có cả Pending và Cancelled.
      ctx.applyRequest(day(14), day(14), { comment: `ASM7 Cancelled để lọc ${ctx.runId}` });
      ctx.myLeave({ from: day(14), to: day(14), type: ctx.annualType });
      ctx.requestRow(day(14)).contains('button', /^\s*Cancel\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.myLeave({ from: day(0), to: day(14), type: ctx.annualType, statuses: allStatuses });
      ctx.requestRow(day(14)).should('contain.text', 'Cancelled');

      // Act – Xóa các trạng thái khác, chỉ chọn Pending Approval rồi Search.
      ctx.selectStatuses(['Pending Approval']);
      search();

      // Assert – Mọi dòng đều Pending Approval, đơn Cancelled không còn trong kết quả.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length.at.least', 1).each(($row) => {
        cy.wrap($row).find('.oxd-table-cell').eq(6).should('contain.text', 'Pending Approval')
          .and('not.contain.text', 'Cancelled').and('not.contain.text', 'Rejected')
          .and('not.contain.text', 'Scheduled').and('not.contain.text', 'Taken');
      });
      cy.get('.oxd-table-body').should('not.contain.text', 'Cancelled');
    });

    it('TC_LEAVE_SELF_020 – Lọc My Leave theo loại phép cụ thể', () => {
      // Arrange – Nộp một đơn ở loại phép đối chứng; cùng khoảng đã có nhiều đơn loại chính.
      ctx.applyRequest(day(17), day(17), { type: ctx.alternateType,
        comment: `ASM7 loại đối chứng ${ctx.runId}` });
      ctx.myLeave({ from: day(0), to: day(17), statuses: allStatuses });
      cy.get('.oxd-table-body').should('contain.text', ctx.annualType).and('contain.text', ctx.alternateType);

      // Act – Chọn Leave Type chính và nhấn Search.
      cy.choose('Leave Type', ctx.annualType);
      search();

      // Assert – Mọi dòng đúng loại phép chính, loại đối chứng bị loại khỏi kết quả.
      cy.get('.oxd-table-body .oxd-table-row').should('have.length.at.least', 1).each(($row) => {
        cy.wrap($row).find('.oxd-table-cell').eq(3).invoke('text').then((value) => {
          expect(value.trim(), 'Loại phép trên từng dòng sau khi lọc').to.eq(ctx.annualType);
        });
      });
      cy.get('.oxd-table-body').should('not.contain.text', ctx.alternateType);
    });

    it('TC_LEAVE_SELF_021 – Reset bộ lọc về mặc định của My Leave', () => {
      let defaultFrom;
      let defaultTo;
      let defaultStatuses;
      // Arrange – Ghi bộ lọc ban đầu, sau đó đổi ngày, loại phép và trạng thái.
      cy.leavePage('My Leave');
      cy.field('From Date').find('input').invoke('val').then((value) => { defaultFrom = value; });
      cy.field('To Date').find('input').invoke('val').then((value) => { defaultTo = value; });
      cy.field('Show Leave with Status').find('.oxd-multiselect-chips-selected').then(($chips) => {
        defaultStatuses = $chips.toArray().map((chip) => chip.innerText.trim()).sort();
      });
      ctx.enterDate('From Date', day(0));
      ctx.enterDate('To Date', day(17));
      cy.choose('Leave Type', ctx.annualType);
      ctx.selectStatuses(['Pending Approval']);
      search();

      // Act – Nhấn Reset để hệ thống tải lại theo bộ lọc mặc định.
      const alias = `selfReset${++searchNumber}`;
      cy.intercept('GET', /\/api\/v2\/leave\/leave-requests(?:\?|$)/).as(alias);
      cy.contains('button', /^\s*Reset\s*$/).click();
      cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
      cy.ready();

      // Assert – Ngày về đầu/cuối kỳ, loại phép chưa chọn và trạng thái đúng như ban đầu.
      cy.field('From Date').find('input').should(($input) => { expect($input.val()).to.eq(defaultFrom); });
      cy.field('To Date').find('input').should(($input) => { expect($input.val()).to.eq(defaultTo); });
      cy.field('Leave Type').should('contain.text', 'Select').and('not.contain.text', ctx.annualType);
      cy.field('Show Leave with Status').find('.oxd-multiselect-chips-selected').should(($chips) => {
        expect($chips.toArray().map((chip) => chip.innerText.trim()).sort()).to.deep.eq(defaultStatuses);
      });
      ctx.requestRow(ctx.currentDate).should('contain.text', ctx.employee.name);
    });

    it('TC_LEAVE_SELF_022 – Hủy đơn Pending Approval và hoàn lại số dư', () => {
      let previousBalance;
      // Arrange – Ghi số dư đúng kỳ và tìm đơn một ngày còn Pending từ SELF_001.
      ctx.balance(day(0)).then((value) => { previousBalance = value; });
      ctx.myLeave({ from: day(0), to: day(0), type: ctx.annualType, statuses: allStatuses });
      ctx.requestRow(day(0)).should('contain.text', 'Pending Approval');
      assertDays(day(0), 1);

      // Act – Nhấn Cancel trên đúng dòng rồi tìm lại sau khi cập nhật.
      ctx.requestRow(day(0)).contains('button', /^\s*Cancel\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.myLeave({ from: day(0), to: day(0), type: ctx.annualType, statuses: ['Cancelled'] });

      // Assert – Đơn chuyển Cancelled và số dư tăng đúng một ngày vừa hủy.
      ctx.requestRow(day(0)).should('contain.text', 'Cancelled');
      ctx.balance(day(0)).then((value) => {
        expect(value, 'Số dư hoàn lại sau Cancel Pending').to.be.closeTo(previousBalance + 1, 0.001);
      });
    });

    it('TC_LEAVE_SELF_023 – Hủy đơn Scheduled và hoàn lại số dư', () => {
      let previousBalance;
      // Arrange – ESS nộp đơn riêng, Admin Approve; ESS ghi số dư khi đơn đã Scheduled.
      prepareAdminState(day(15), 'Approve', 'Scheduled');
      ctx.balance(day(15)).then((value) => { previousBalance = value; });
      ctx.myLeave({ from: day(15), to: day(15), type: ctx.annualType, statuses: ['Scheduled'] });
      ctx.requestRow(day(15)).should('contain.text', 'Scheduled');

      // Act – ESS nhấn Cancel trên đơn đã được duyệt và tải lại với trạng thái Cancelled.
      ctx.requestRow(day(15)).contains('button', /^\s*Cancel\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.myLeave({ from: day(15), to: day(15), type: ctx.annualType, statuses: ['Cancelled'] });

      // Assert – Đơn Scheduled đã thành Cancelled; số dư tăng đúng một ngày.
      ctx.requestRow(day(15)).should('contain.text', 'Cancelled');
      ctx.balance(day(15)).then((value) => {
        expect(value, 'Số dư hoàn lại sau Cancel Scheduled').to.be.closeTo(previousBalance + 1, 0.001);
      });
    });

    it('TC_LEAVE_SELF_024 – Đơn Rejected không có Cancel khả dụng', () => {
      // Arrange – ESS nộp đơn riêng rồi Admin Reject qua nút trên dòng.
      prepareAdminState(day(16), 'Reject', 'Rejected');

      // Act – ESS mở My Leave, lọc đúng ngày và trạng thái Rejected.
      ctx.myLeave({ from: day(16), to: day(16), type: ctx.annualType, statuses: ['Rejected'] });

      // Assert – Đúng đơn đã Rejected; nút Cancel không có hoặc bị vô hiệu hóa.
      ctx.requestRow(day(16)).should('contain.text', 'Rejected');
      noCancel(day(16));
    });

    it('TC_LEAVE_SELF_025 – Đơn Taken trong quá khứ không cho ESS Cancel', () => {
      // Arrange – Nộp đơn tại ngày làm việc trong quá khứ rồi Admin Approve để thành Taken.
      prepareAdminState(ctx.pastDate, 'Approve', 'Taken');

      // Act – ESS tìm lại đơn trong My Leave với khoảng quá khứ và trạng thái Taken.
      ctx.myLeave({ from: ctx.pastDate, to: ctx.pastDate,
        type: ctx.annualType, statuses: ['Taken'] });

      // Assert – Đơn đúng nhân viên đã Taken và ESS không có nút Cancel khả dụng.
      ctx.requestRow(ctx.pastDate).should('contain.text', ctx.employee.name).and('contain.text', 'Taken');
      noCancel(ctx.pastDate);
    });
  });
}

function registerAdminCases(ctx) {
  // Dữ liệu ADMIN dùng các ngày riêng, tách khỏi những đơn của nhóm LEAVE_SELF.
  let coreApprove;
  let coreReject;
  let scheduledFilter;
  let refundDays;
  let cancelScheduled;
  let commentDate;
  let threeDays;
  let bulkDates;
  let controlDate;
  let pageDates;
  const employeeComment = `G01T01 - Ghi chú nhân viên ${ctx.runId}`;
  const adminComment = `Đã xác nhận với trưởng bộ phận G01T01 ${ctx.runId}.`;
  const unsavedComment = `Bình luận chưa lưu ${ctx.runId}`;
  let searchNumber = 0;

  // Chỉ quan sát truy vấn được tạo bởi nút Search, không sửa request hoặc response.
  function searchUI() {
    const alias = `adminSearch${++searchNumber}`;
    cy.intercept('GET', /\/api\/v2\/leave\/employees\/leave-requests(?:\?|$)/).as(alias);
    cy.contains('button', /^\s*Search\s*$/).click();
    cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
    cy.ready();
  }

  function formatDate(date, placeholder) {
    return placeholder.replace(/yyyy|mm|dd/gi, (token) => ({
      yyyy: date.year, mm: date.month, dd: date.day,
    })[token.toLowerCase()]);
  }

  function rows() {
    return cy.get('.oxd-table-body .oxd-table-row');
  }

  function commentsModal() {
    return cy.contains('.oxd-dialog-sheet', 'Leave Request Comments');
  }

  // Cột được xác định theo tiêu đề để đối chiếu số liệu thật trên bảng.
  function cell(date, title, options) {
    return cy.get('.oxd-table-header-cell').then(($headers) => {
      const index = Array.from($headers).findIndex((header) => header.innerText.trim() === title);
      expect(index, `Có cột ${title} trên Leave List`).to.be.at.least(0);
      return ctx.requestRow(date, options).find('.oxd-table-cell').eq(index);
    });
  }

  function readBalance(date, type = ctx.annualType) {
    return ctx.balance(date, type).then((value) => {
      const number = Number(value);
      expect(number, 'Số dư được đọc từ giao diện Apply').to.be.finite;
      return number;
    });
  }

  function checkOwnRows(employee, type, count, statuses) {
    rows().should('have.length', count).each(($row) => {
      cy.wrap($row).should('contain.text', employee.name).and('contain.text', type);
      if (statuses) {
        expect(statuses.some((status) => $row.text().includes(status)), 'Trạng thái thuộc bộ lọc').to.eq(true);
      }
    });
  }

  function selectRequest(date, type = ctx.bulkType) {
    ctx.requestRow(date, { type }).should('contain.text', ctx.employee.name)
      .and('contain.text', type).and('contain.text', 'Pending Approval');
    ctx.requestRow(date, { type }).find('input[type="checkbox"]').should('not.be.checked');
    ctx.requestRow(date, { type }).find('.oxd-checkbox-input').click();
  }

  if (onlyCases.length && !onlyCases.some((id) => id.startsWith('TC_LEAVE_ADMIN_'))) return;
  describe('LEAVE_ADMIN – Danh sách phép và xử lý yêu cầu', () => {
    before(() => {
      // Đọc lịch sau khi các hook CONFIG/SELF đã hoàn tất chuẩn bị ngày làm việc.
      coreApprove = ctx.dates[30];
      coreReject = ctx.dates[31];
      scheduledFilter = ctx.dates[32];
      refundDays = ctx.findWeekdayBlock(33, 2);
      cancelScheduled = ctx.dates[35];
      commentDate = ctx.dates[36];
      threeDays = ctx.findWeekdayBlock(37, 3);
      bulkDates = ctx.dates.slice(40, 45);
      controlDate = ctx.dates[60];
      pageDates = ctx.dates.slice(0, 51);
      const primaryDates = [coreApprove, coreReject, scheduledFilter, ...refundDays, cancelScheduled, commentDate, ...threeDays, ...bulkDates];
      expect(new Set(primaryDates.map((date) => date.iso)).size, 'Ngày fixture ADMIN không trùng nhau').to.eq(primaryDates.length);
      expect(pageDates, 'Đủ 51 ngày riêng cho fixture phân trang').to.have.length(51);

      // Chuẩn bị ba đơn riêng: hai Pending cho Approve/Reject, một Scheduled cho bộ lọc.
      ctx.loginEmployee();
      ctx.applyRequest(coreApprove);
      ctx.applyRequest(coreReject);
      ctx.applyRequest(scheduledFilter);
      ctx.myLeave({ from: coreApprove, to: scheduledFilter, type: ctx.annualType, statuses: ['Pending Approval'] });
      ctx.requestRow(coreApprove).should('contain.text', 'Pending Approval');
      ctx.requestRow(coreReject).should('contain.text', 'Pending Approval');
      ctx.requestRow(scheduledFilter).should('contain.text', 'Pending Approval');

      // Nhân viên đối chứng thuộc Sub Unit B có đơn khác để kiểm tra loại trừ theo đơn vị.
      ctx.loginEmployee(ctx.employeeB);
      ctx.applyRequest(controlDate);
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: scheduledFilter, to: scheduledFilter, statuses: ['Pending Approval'] });
      ctx.requestRow(scheduledFilter).contains('button', /^\s*Approve\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: scheduledFilter, to: scheduledFilter, statuses: ['Scheduled'] });
      ctx.requestRow(scheduledFilter).should('contain.text', 'Scheduled');
    });

    it('TC_LEAVE_ADMIN_001 – Tìm theo tên nhân viên bằng autocomplete', () => {
      // Arrange – Chọn đúng nhân viên và khoảng chỉ chứa hai đơn Pending của ADMIN.
      cy.leavePage('Leave List');
      ctx.enterDate('From Date', coreApprove);
      ctx.enterDate('To Date', coreReject);
      cy.employeeUI(ctx.employee.name);
      cy.field('Show Leave with Status').should('contain.text', 'Pending Approval');

      // Act – Tìm kiếm bằng nhân viên đã được chọn qua gợi ý.
      searchUI();

      // Assert – Có đúng hai đơn Pending và mọi dòng thuộc nhân viên fixture.
      checkOwnRows(ctx.employee, ctx.annualType, 2, ['Pending Approval']);
      ctx.requestRow(coreApprove).should('contain.text', 'Pending Approval');
      ctx.requestRow(coreReject).should('contain.text', 'Pending Approval');
    });

    it('TC_LEAVE_ADMIN_002 – Tên nhân viên không tồn tại', () => {
      // Arrange – Mở bộ lọc mới rồi gõ tên chắc chắn không thuộc fixture nào.
      cy.leavePage('Leave List');
      const unknownName = `Unknown_Employee_G01X_${ctx.runId}`;
      cy.field('Employee Name').find('input').clear().type(unknownName);

      // Act – Quan sát autocomplete và thử tìm khi chưa chọn được nhân viên hợp lệ.
      cy.contains('.oxd-autocomplete-option', 'No Records Found').should('be.visible');
      cy.contains('button', /^\s*Search\s*$/).click();

      // Assert – Báo Invalid và không coi bảng đã tải trước đó là kết quả tìm tên lạ.
      cy.field('Employee Name').contains('Invalid').should('be.visible');
      cy.field('Employee Name').find('input').should('have.value', unknownName);
    });

    it('TC_LEAVE_ADMIN_003 – Kết hợp loại phép và Pending Approval', () => {
      // Arrange – Đặt lại bộ lọc nhân viên, hai ngày và loại phép của các đơn Pending.
      ctx.filterAdmin({ employee: ctx.employee, from: coreApprove, to: coreReject, statuses: ['Pending Approval'] });
      cy.choose('Leave Type', ctx.annualType);
      ctx.selectStatuses(['Pending Approval']);

      // Act – Tìm với các điều kiện kết hợp.
      searchUI();

      // Assert – Hai dòng đều đúng nhân viên, loại phép và Pending Approval.
      checkOwnRows(ctx.employee, ctx.annualType, 2, ['Pending Approval']);
    });

    it('TC_LEAVE_ADMIN_004 – Lọc theo Sub Unit của nhân viên', () => {
      // Arrange – Chứng minh tập đối chứng có đơn của cả hai đơn vị A và B.
      ctx.filterAdmin({ employee: null, type: ctx.annualType, from: coreApprove, to: controlDate, statuses: ['Pending Approval', 'Scheduled'] });
      rows().should('contain.text', ctx.employee.name).and('contain.text', ctx.employeeB.name);
      expect(ctx.subUnitA, 'Sub Unit A đã gán qua PIM').to.be.a('string').and.not.eq(ctx.subUnitB);

      // Act – Chọn Sub Unit A rồi Search trên tập dữ liệu có đối chứng B.
      cy.choose('Sub Unit', ctx.subUnitA);
      searchUI();

      // Assert – Chỉ còn ba đơn fixture của nhân viên A, loại trừ nhân viên B.
      checkOwnRows(ctx.employee, ctx.annualType, 3, ['Pending Approval', 'Scheduled']);
      rows().should('not.contain.text', ctx.employeeB.name);
    });

    it('TC_LEAVE_ADMIN_005 – Lọc khoảng ngày hợp lệ', () => {
      // Arrange – Chọn nhân viên, loại phép và ba trạng thái phù hợp với đơn đã chuẩn bị.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreApprove, to: controlDate, statuses: ['Pending Approval', 'Scheduled'] });
      ctx.enterDate('From Date', coreApprove);
      ctx.enterDate('To Date', scheduledFilter);

      // Act – Tìm trong khoảng ba ngày làm việc chứa đúng ba đơn riêng.
      searchUI();

      // Assert – Tập ngày trả về khớp toàn bộ các ngày fixture trong khoảng.
      checkOwnRows(ctx.employee, ctx.annualType, 3, ['Pending Approval', 'Scheduled']);
      cy.field('From Date').find('input').invoke('attr', 'placeholder').then((placeholder) => {
        const expectedDates = [coreApprove, coreReject, scheduledFilter].map((date) => formatDate(date, placeholder));
        rows().then(($rows) => {
          const actualDates = Array.from($rows, (row) => row.querySelectorAll('.oxd-table-cell')[1].innerText.trim());
          expect(actualDates, 'Ngày của ba đơn nằm trong khoảng lọc').to.have.members(expectedDates);
        });
      });
    });

    it('TC_LEAVE_ADMIN_006 – From Date lớn hơn To Date', () => {
      // Arrange – Mở bộ lọc mới và điền một khoảng ngày đảo ngược.
      cy.leavePage('Leave List');
      ctx.enterDate('From Date', scheduledFilter);
      ctx.enterDate('To Date', coreApprove);

      // Act – Bấm Search với khoảng ngày không hợp lệ.
      cy.contains('button', /^\s*Search\s*$/).click();

      // Assert – To Date báo lỗi thứ tự ngày và form giữ lại dữ liệu sai để sửa.
      cy.field('To Date').contains('To date should be after from date').should('be.visible');
      cy.contains('h5', /^\s*Leave List\s*$/).should('be.visible');
    });

    it('TC_LEAVE_ADMIN_007 – Lọc đồng thời Pending và Scheduled', () => {
      // Arrange – Chuẩn bị bộ lọc riêng trên ba đơn có hai trạng thái khác nhau.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreApprove, to: scheduledFilter, statuses: ['Pending Approval'] });
      ctx.selectStatuses(['Pending Approval', 'Scheduled']);

      // Act – Search với cả hai chip trạng thái.
      searchUI();

      // Assert – Có cả Pending và Scheduled; không có Rejected hoặc Cancelled.
      checkOwnRows(ctx.employee, ctx.annualType, 3, ['Pending Approval', 'Scheduled']);
      ctx.requestRow(coreApprove).should('contain.text', 'Pending Approval');
      ctx.requestRow(coreReject).should('contain.text', 'Pending Approval');
      ctx.requestRow(scheduledFilter).should('contain.text', 'Scheduled');
      rows().should('not.contain.text', 'Rejected').and('not.contain.text', 'Cancelled');
    });

    it('TC_LEAVE_ADMIN_008 – Include Past Employees hiển thị đơn của người thôi việc', () => {
      // Arrange – Tạo đơn rồi chấm dứt nhân viên riêng; chỉ case này cần dữ liệu Past Employee.
      ctx.preparePastEmployee(ctx.employeePast, ctx.pastType, ctx.pastDate);
      const pastDate = ctx.pastRequestDate;
      ctx.filterAdmin({ employee: null, type: ctx.pastType, from: pastDate, to: pastDate, statuses: ['Pending Approval', 'Scheduled', 'Taken'], includePast: false });
      cy.contains('.oxd-table-cell', ctx.employeePast.name).should('not.exist');

      // Act – Bật Include Past Employees và chọn người đã thôi việc từ autocomplete.
      cy.get('.orangehrm-leave-filter .oxd-switch-input').click();
      cy.employeeUI(ctx.employeePast.name);
      searchUI();

      // Assert – Đơn lịch sử của đúng nhân viên đã thôi việc xuất hiện trong kết quả.
      checkOwnRows(ctx.employeePast, ctx.pastType, 1, ['Pending Approval', 'Scheduled', 'Taken']);
      ctx.requestRow(pastDate, { type: ctx.pastType }).should('contain.text', ctx.employeePast.name);
    });

    it('TC_LEAVE_ADMIN_009 – Reset phục hồi bộ lọc mặc định', () => {
      // Arrange – Ghi lại ngày mặc định rồi đặt nhân viên, đơn vị và khoảng tùy biến.
      cy.leavePage('Leave List');
      let defaultFrom;
      let defaultTo;
      cy.field('From Date').find('input').invoke('val').then((value) => { defaultFrom = value; });
      cy.field('To Date').find('input').invoke('val').then((value) => { defaultTo = value; });
      ctx.enterDate('From Date', coreApprove);
      ctx.enterDate('To Date', scheduledFilter);
      cy.employeeUI(ctx.employee.name);
      cy.choose('Sub Unit', ctx.subUnitA);
      cy.choose('Leave Type', ctx.annualType);
      ctx.selectStatuses(['Scheduled']);
      searchUI();

      // Act – Bấm Reset và chờ truy vấn do nút này tạo ra hoàn tất.
      const alias = `adminReset${++searchNumber}`;
      cy.intercept('GET', /\/api\/v2\/leave\/employees\/leave-requests(?:\?|$)/).as(alias);
      cy.contains('button', /^\s*Reset\s*$/).click();
      cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
      cy.ready();

      // Assert – Ngày về đầu/cuối kỳ ban đầu; nhân viên, đơn vị và loại phép được xóa.
      cy.then(() => {
        cy.field('From Date').find('input').should('have.value', defaultFrom);
        cy.field('To Date').find('input').should('have.value', defaultTo);
      });
      cy.field('Employee Name').find('input').should('have.value', '');
      cy.field('Sub Unit').find('.oxd-select-text').should('contain.text', '-- Select --');
      cy.field('Leave Type').find('.oxd-select-text').should('contain.text', '-- Select --');
      cy.field('Show Leave with Status').find('.oxd-multiselect-chips-selected').should('have.length', 1)
        .and('contain.text', 'Pending Approval');
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

    it('TC_LEAVE_ADMIN_012 – Reject một đơn Pending Approval', () => {
      // Arrange – Trở lại Admin, tìm đơn Pending riêng chưa dùng cho nhánh Approve.
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreReject, to: coreReject, statuses: ['Pending Approval'] });
      ctx.requestRow(coreReject).should('contain.text', ctx.employee.name).and('contain.text', 'Pending Approval');

      // Act – Reject đơn đã đối chiếu khóa, sau đó tìm lại Rejected.
      ctx.requestRow(coreReject).contains('button', /^\s*Reject\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreReject, to: coreReject, statuses: ['Rejected'] });

      // Assert – Chỉ đúng đơn này chuyển Rejected và không có nút Approve.
      ctx.requestRow(coreReject).should('contain.text', 'Rejected');
      ctx.requestRow(coreReject).contains('button', /^\s*Approve\s*$/).should('not.exist');
    });

    it('TC_LEAVE_ADMIN_013 – Reject hoàn lại đúng hai ngày vào số dư', () => {
      // Arrange – ESS tạo một đơn riêng hai ngày làm việc và ghi số dư ngay trước Reject.
      ctx.loginEmployee();
      ctx.applyRequest(refundDays[0], refundDays[1]);
      let beforeBalance;
      readBalance(refundDays[0]).then((value) => { beforeBalance = value; });
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: refundDays[0], to: refundDays[1], statuses: ['Pending Approval'] });
      cell(refundDays[0], 'Number of Days').invoke('text').then((value) => { expect(Number(value.trim())).to.eq(2); });

      // Act – Reject đúng đơn hai ngày rồi chuyển lại ESS đọc cùng kỳ phép.
      ctx.requestRow(refundDays[0]).contains('button', /^\s*Reject\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: refundDays[0], to: refundDays[1], statuses: ['Rejected'] });
      ctx.requestRow(refundDays[0]).should('contain.text', 'Rejected');
      ctx.loginEmployee();

      // Assert – Số dư sau Reject tăng đúng 2,00 so với thời điểm trước thao tác.
      readBalance(refundDays[0]).then((afterBalance) => {
        expect(afterBalance, 'Hoàn số ngày của đúng đơn bị từ chối').to.be.closeTo(beforeBalance + 2, 0.001);
      });
    });

    it('TC_LEAVE_ADMIN_014 – Admin Cancel đơn Scheduled và hoàn một ngày', () => {
      // Arrange – Chuẩn bị đơn Scheduled riêng và ghi số dư ESS trước khi Admin hủy.
      ctx.loginEmployee();
      ctx.applyRequest(cancelScheduled);
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: cancelScheduled, to: cancelScheduled, statuses: ['Pending Approval'] });
      ctx.requestRow(cancelScheduled).contains('button', /^\s*Approve\s*$/).click();
      ctx.success('Successfully Updated');
      ctx.loginEmployee();
      let beforeBalance;
      readBalance(cancelScheduled).then((value) => { beforeBalance = value; });
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: cancelScheduled, to: cancelScheduled, statuses: ['Scheduled'] });
      ctx.requestRow(cancelScheduled).should('contain.text', 'Scheduled');

      // Act – Chọn More > Cancel Leave trên đúng đơn đã duyệt.
      ctx.rowMenu(ctx.requestRow(cancelScheduled), 'Cancel Leave');
      ctx.success('Successfully Updated');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: cancelScheduled, to: cancelScheduled, statuses: ['Cancelled'] });

      // Assert – Đơn chuyển Cancelled và ESS nhận lại đúng một ngày trong cùng kỳ.
      ctx.requestRow(cancelScheduled).should('contain.text', 'Cancelled');
      ctx.loginEmployee();
      readBalance(cancelScheduled).then((afterBalance) => {
        expect(afterBalance, 'Hoàn số ngày của đúng đơn Scheduled đã hủy').to.be.closeTo(beforeBalance + 1, 0.001);
      });
    });

    it('TC_LEAVE_ADMIN_015 – Đơn Rejected không thể Approve lại', () => {
      // Arrange – Admin lọc đúng đơn Rejected được tạo bởi nhánh Reject riêng.
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreReject, to: coreReject, statuses: ['Rejected'] });

      // Act – Quan sát cột Actions của dòng Rejected qua giao diện.
      ctx.requestRow(coreReject).should('be.visible').and('contain.text', 'Rejected');

      // Assert – Không xuất hiện nút Approve; trạng thái của dòng vẫn Rejected.
      ctx.requestRow(coreReject).contains('button', /^\s*Approve\s*$/).should('not.exist');
      ctx.requestRow(coreReject).should('contain.text', 'Rejected');
    });

    it('TC_LEAVE_ADMIN_016 – Đơn Cancelled không thể Approve lại', () => {
      // Arrange – Lọc đơn Scheduled đã được Admin Cancel ở case riêng trước đó.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: cancelScheduled, to: cancelScheduled, statuses: ['Cancelled'] });

      // Act – Quan sát Actions của đúng đơn Cancelled.
      ctx.requestRow(cancelScheduled).should('be.visible').and('contain.text', 'Cancelled');

      // Assert – Không có nút Approve trên đơn đã hủy.
      ctx.requestRow(cancelScheduled).contains('button', /^\s*Approve\s*$/).should('not.exist');
    });

    it('TC_LEAVE_ADMIN_017 – Admin xem bình luận do ESS đã lưu', () => {
      // Arrange – ESS tạo đơn riêng có ghi chú nhận diện của lần chạy.
      ctx.loginEmployee();
      ctx.applyRequest(commentDate, commentDate, { type: ctx.annualType, comment: employeeComment });
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: commentDate, to: commentDate, statuses: ['Pending Approval'] });
      ctx.requestRow(commentDate).should('contain.text', ctx.employee.name);

      // Act – Mở chức năng Comments của đúng đơn bằng More > Add Comment.
      ctx.rowMenu(ctx.requestRow(commentDate), 'Add Comment');
      cy.ready();

      // Assert – Modal mở và nội dung đã lưu khớp bình luận nhân viên.
      commentsModal().should('be.visible').and('contain.text', employeeComment);
      commentsModal().contains('button', /^\s*Cancel\s*$/).click();
      cy.ready();
    });

    it('TC_LEAVE_ADMIN_018 – Admin thêm bình luận và đọc lại sau khi lưu', () => {
      // Arrange – Tìm đúng đơn có bình luận ESS, mở modal nhập bình luận Admin.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: commentDate, to: commentDate, statuses: ['Pending Approval'] });
      ctx.rowMenu(ctx.requestRow(commentDate), 'Add Comment');
      cy.ready();
      commentsModal().find('textarea').type(adminComment);

      // Act – Save bình luận rồi mở lại modal trên cùng đơn.
      commentsModal().contains('button', /^\s*Save\s*$/).click();
      ctx.success('Successfully Saved');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: commentDate, to: commentDate, statuses: ['Pending Approval'] });
      ctx.rowMenu(ctx.requestRow(commentDate), 'Add Comment');
      cy.ready();

      // Assert – Có cả comment nhân viên cũ và comment Admin vừa lưu trên đúng đơn.
      commentsModal().should('contain.text', employeeComment).and('contain.text', adminComment);
      commentsModal().contains('button', /^\s*Cancel\s*$/).click();
      cy.ready();
    });

    it('TC_LEAVE_ADMIN_019 – Cancel modal bỏ bình luận chưa lưu', () => {
      // Arrange – Mở Comments của cùng đơn và ghi một bình luận tạm duy nhất.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: commentDate, to: commentDate, statuses: ['Pending Approval'] });
      ctx.rowMenu(ctx.requestRow(commentDate), 'Add Comment');
      cy.ready();
      commentsModal().find('textarea').type(unsavedComment);

      // Act – Cancel modal, sau đó mở lại Comments trên cùng đơn.
      commentsModal().contains('button', /^\s*Cancel\s*$/).click();
      commentsModal().should('not.exist');
      cy.ready();
      ctx.rowMenu(ctx.requestRow(commentDate), 'Add Comment');
      cy.ready();

      // Assert – Text tạm không được lưu; các comment cũ vẫn nguyên vẹn.
      commentsModal().should('not.contain.text', unsavedComment)
        .and('contain.text', employeeComment).and('contain.text', adminComment);
      commentsModal().find('textarea').should('have.value', '');
      commentsModal().contains('button', /^\s*Cancel\s*$/).click();
      cy.ready();
    });

    it('TC_LEAVE_ADMIN_020 – Checkbox header chọn toàn bộ dòng fixture trên trang', () => {
      // Arrange – Tạo loại phép riêng và năm đơn UI, chỉ chọn sau khi đối chiếu mọi dòng.
      ctx.loginAdmin();
      ctx.addType(ctx.bulkType);
      ctx.grant(ctx.employee.name, ctx.bulkType, ctx.currentYear, 20);
      ctx.grant(ctx.employee.name, ctx.bulkType, ctx.targetYear, 20);
      ctx.loginEmployee();
      bulkDates.forEach((date) => { ctx.applyRequest(date, date, { type: ctx.bulkType, comment: `Đơn hàng loạt ${ctx.runId}` }); });
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.bulkType, from: bulkDates[0], to: bulkDates[4], statuses: ['Pending Approval'] });
      checkOwnRows(ctx.employee, ctx.bulkType, 5, ['Pending Approval']);
      bulkDates.forEach((date) => { ctx.requestRow(date, { type: ctx.bulkType }).should('contain.text', ctx.employee.name); });

      // Act – Tích checkbox header của đúng trang có năm đơn fixture riêng.
      cy.get('.oxd-table-header input[type="checkbox"]').should('not.be.checked');
      cy.get('.oxd-table-header .oxd-checkbox-input').click();

      // Assert – Cả năm checkbox dòng được chọn và UI báo năm bản ghi đã chọn.
      rows().find('input[type="checkbox"]').should('have.length', 5).each(($checkbox) => {
        cy.wrap($checkbox).should('be.checked');
      });
      cy.contains('.orangehrm-header-container', /\(5\) Records Selected/).should('be.visible');
    });

    it('TC_LEAVE_ADMIN_021 – Bulk Approve đúng hai đơn và giữ đơn không chọn', () => {
      // Arrange – Lọc riêng năm đơn bulk rồi chỉ tích hai khóa dành cho Approve.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.bulkType, from: bulkDates[0], to: bulkDates[4], statuses: ['Pending Approval'] });
      checkOwnRows(ctx.employee, ctx.bulkType, 5, ['Pending Approval']);
      selectRequest(bulkDates[0]);
      selectRequest(bulkDates[1]);
      rows().find('input[type="checkbox"]').filter(':checked').should('have.length', 2);

      // Act – Approve trên thanh bulk và xác nhận đúng hai đơn trong hộp thoại.
      cy.get('.orangehrm-header-container .actions').contains('button', /^\s*Approve\s*$/).click();
      cy.contains('.oxd-dialog-sheet', 'You are about to Approve 2 Leave Requests').should('be.visible');
      cy.contains('.oxd-dialog-sheet button', /^\s*Yes, Confirm\s*$/).click();
      ctx.success('2 Leave Requests Approved');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.bulkType, from: bulkDates[0], to: bulkDates[4], statuses: ['Pending Approval', 'Scheduled'] });

      // Assert – Chỉ hai đơn đã chọn thành Scheduled, ba đơn còn lại giữ Pending.
      checkOwnRows(ctx.employee, ctx.bulkType, 5, ['Pending Approval', 'Scheduled']);
      ctx.requestRow(bulkDates[0], { type: ctx.bulkType }).should('contain.text', 'Scheduled');
      ctx.requestRow(bulkDates[1], { type: ctx.bulkType }).should('contain.text', 'Scheduled');
      bulkDates.slice(2).forEach((date) => { ctx.requestRow(date, { type: ctx.bulkType }).should('contain.text', 'Pending Approval'); });
    });

    it('TC_LEAVE_ADMIN_022 – Bulk Reject đúng hai đơn và giữ đơn không chọn', () => {
      // Arrange – Tìm ba đơn Pending còn lại và chỉ tích hai khóa dành cho Reject.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.bulkType, from: bulkDates[0], to: bulkDates[4], statuses: ['Pending Approval'] });
      checkOwnRows(ctx.employee, ctx.bulkType, 3, ['Pending Approval']);
      selectRequest(bulkDates[2]);
      selectRequest(bulkDates[3]);
      rows().find('input[type="checkbox"]').filter(':checked').should('have.length', 2);

      // Act – Reject trên thanh bulk và Confirm đúng hai đơn fixture.
      cy.get('.orangehrm-header-container .actions').contains('button', /^\s*Reject\s*$/).click();
      cy.contains('.oxd-dialog-sheet', 'You are about to Reject 2 Leave Requests').should('be.visible');
      cy.contains('.oxd-dialog-sheet button', /^\s*Yes, Confirm\s*$/).click();
      ctx.success('2 Leave Requests Rejected');
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.bulkType, from: bulkDates[0], to: bulkDates[4], statuses: ['Pending Approval', 'Scheduled', 'Rejected'] });

      // Assert – Hai đơn Reject thành Rejected; hai Scheduled và một Pending không đổi.
      checkOwnRows(ctx.employee, ctx.bulkType, 5, ['Pending Approval', 'Scheduled', 'Rejected']);
      ctx.requestRow(bulkDates[2], { type: ctx.bulkType }).should('contain.text', 'Rejected');
      ctx.requestRow(bulkDates[3], { type: ctx.bulkType }).should('contain.text', 'Rejected');
      ctx.requestRow(bulkDates[0], { type: ctx.bulkType }).should('contain.text', 'Scheduled');
      ctx.requestRow(bulkDates[1], { type: ctx.bulkType }).should('contain.text', 'Scheduled');
      ctx.requestRow(bulkDates[4], { type: ctx.bulkType }).should('contain.text', 'Pending Approval');
    });

    it('TC_LEAVE_ADMIN_023 – Phân trang với 51 đơn fixture được tạo qua UI', () => {
      // Arrange – Tạo đủ 51 đơn thật bằng ESS B trên loại phép riêng, không bỏ qua tiền điều kiện.
      ctx.loginAdmin();
      ctx.addType(ctx.pageType);
      ctx.grant(ctx.employeeB.name, ctx.pageType, ctx.currentYear, 20);
      ctx.grant(ctx.employeeB.name, ctx.pageType, ctx.targetYear, 100);
      ctx.seedRequests(ctx.employeeB, ctx.pageType, pageDates, `Đơn phân trang ${ctx.runId}`);
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employeeB, type: ctx.pageType, from: pageDates[0], to: pageDates[50], statuses: ['Pending Approval'] });
      cy.contains('.orangehrm-header-container', /\(51\) Records Found/).should('be.visible');
      checkOwnRows(ctx.employeeB, ctx.pageType, 50, ['Pending Approval']);
      cy.get('.oxd-pagination-page-item--page-selected').should('have.text', '1');
      let pageOneDates;
      let expectedDates;
      cy.field('From Date').find('input').invoke('attr', 'placeholder').then((placeholder) => {
        expectedDates = pageDates.map((date) => formatDate(date, placeholder));
      });
      rows().then(($rows) => {
        pageOneDates = Array.from($rows, (row) => row.querySelectorAll('.oxd-table-cell')[1].innerText.trim());
        expect(new Set(pageOneDates).size, 'Trang một có 50 khóa ngày duy nhất').to.eq(50);
        pageOneDates.forEach((value) => { expect(expectedDates, 'Khóa trang một thuộc tập đã tạo').to.include(value); });
      });

      // Act – Bấm trang 2 và chờ truy vấn do phân trang tạo ra.
      const alias = `adminPage${++searchNumber}`;
      cy.intercept('GET', /\/api\/v2\/leave\/employees\/leave-requests(?:\?|$)/).as(alias);
      cy.contains('.oxd-pagination-page-item--page', /^\s*2\s*$/).scrollIntoView().click();
      cy.wait(`@${alias}`).its('response.statusCode').should('eq', 200);
      cy.ready();

      // Assert – Trang 2 được đánh dấu, có đúng khóa còn lại và không lặp khóa trang 1.
      cy.get('.oxd-pagination-page-item--page-selected').should('have.text', '2');
      checkOwnRows(ctx.employeeB, ctx.pageType, 1, ['Pending Approval']);
      rows().then(($rows) => {
        const pageTwoDates = Array.from($rows, (row) => row.querySelectorAll('.oxd-table-cell')[1].innerText.trim());
        pageTwoDates.forEach((value) => {
          expect(expectedDates, 'Khóa trang hai thuộc tập đã tạo').to.include(value);
          expect(pageOneDates, 'Không lặp khóa giữa hai trang').not.to.include(value);
        });
        expect([...pageOneDates, ...pageTwoDates], 'Hai trang bao phủ đủ 51 đơn đã tạo').to.have.members(expectedDates);
      });
    });

    it('TC_LEAVE_ADMIN_024 – View Leave Details mở đúng đơn và Back về danh sách', () => {
      // Arrange – Lọc đơn Approved một ngày và ghi ngày thực hiển thị trên dòng Admin.
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: coreApprove, to: coreApprove, statuses: ['Scheduled'] });
      ctx.requestRow(coreApprove).should('contain.text', ctx.employee.name).and('contain.text', ctx.annualType).and('contain.text', 'Scheduled');
      let displayedDate;
      cell(coreApprove, 'Date').invoke('text').then((value) => { displayedDate = value.trim(); });

      // Act – Mở More > View Leave Details bằng giao diện.
      ctx.rowMenu(ctx.requestRow(coreApprove), 'View Leave Details');
      cy.contains('h6', /^\s*Leave Request Details\s*$/).should('be.visible');
      cy.ready();

      // Assert – Chi tiết khớp nhân viên, ngày, loại, trạng thái; Back trở lại Leave List.
      cy.field('Employee Name').should('contain.text', ctx.employee.name);
      cy.then(() => { cy.field('Leave requested for').should('contain.text', displayedDate); });
      rows().should('have.length', 1).and('contain.text', ctx.annualType).and('contain.text', 'Scheduled');
      cy.contains('button', /^\s*Back\s*$/).click();
      cy.contains('h5', /^\s*Leave List\s*$/).should('be.visible');
      cy.ready();
    });

    it('TC_LEAVE_ADMIN_025 – Đối chiếu số ngày và số dư trên dòng Admin', () => {
      // Arrange – Ghi balance trước khi ESS tạo đơn riêng gồm ba ngày làm việc liên tiếp.
      ctx.loginEmployee();
      let beforeBalance;
      let afterBalance;
      readBalance(threeDays[0]).then((value) => { beforeBalance = value; });

      // Act – Nộp đơn ba ngày, đọc số dư ESS rồi mở đúng dòng trên Leave List Admin.
      ctx.applyRequest(threeDays[0], threeDays[2]);
      readBalance(threeDays[0]).then((value) => { afterBalance = value; });
      ctx.loginAdmin();
      ctx.filterAdmin({ employee: ctx.employee, type: ctx.annualType, from: threeDays[0], to: threeDays[2], statuses: ['Pending Approval'] });

      // Assert – Number of Days bằng 3 và Leave Balance trùng ESS sau khi trừ đúng ba ngày.
      ctx.requestRow(threeDays[0]).should('contain.text', ctx.employee.name).and('contain.text', ctx.annualType);
      cell(threeDays[0], 'Number of Days').invoke('text').then((value) => {
        expect(Number(value.trim()), 'Số ngày làm việc của đơn vừa lưu').to.eq(3);
      });
      cell(threeDays[0], 'Leave Balance (Days)').invoke('text').then((value) => {
        expect(afterBalance, 'Số dư ESS giảm đúng số ngày của đơn mới').to.be.closeTo(beforeBalance - 3, 0.001);
        expect(Number(value.trim()), 'Balance trên Admin bằng số dư ESS cùng kỳ').to.be.closeTo(afterBalance, 0.001);
      });
    });
  });
}
