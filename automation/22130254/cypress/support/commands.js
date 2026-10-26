// Tìm đúng nhóm nhập liệu bằng nhãn đang hiển thị trên giao diện.
Cypress.Commands.add('field', (label) => {
  return cy.contains('.oxd-label', new RegExp(`^\\s*${Cypress._.escapeRegExp(label)}\\s*\\*?\\s*$`))
    .closest('.oxd-input-group');
});

// Chờ giao diện tải xong bằng trạng thái spinner, không dùng thời gian chờ cố định.
Cypress.Commands.add('ready', () => {
  cy.get('.oxd-loading-spinner').should('not.exist');
  cy.get('.oxd-form-loader').should('not.exist');
});

// Đóng modal còn mở sau case lỗi bằng nút X; không bỏ qua lỗi ứng dụng hoặc ép click.
Cypress.Commands.add('closeDialogUI', () => {
  cy.get('body').then(($body) => {
    const close = $body.find('.oxd-dialog-close-button').filter(':visible').first();
    if (close.length) cy.wrap(close).click();
  });
});

// Chọn một mục theo tên trong danh sách thả xuống của OrangeHRM.
Cypress.Commands.add('choose', (label, option) => {
  cy.field(label).find('.oxd-select-text').click();
  cy.contains('.oxd-select-option', new RegExp(`^\\s*${Cypress._.escapeRegExp(option)}\\s*$`)).click();
});

let loginNumber = 0;

// Chờ Vue dựng Dashboard và tải xong widget trước khi chuyển sang module nghiệp vụ.
function dashboardReady(summaryAlias) {
  cy.location('pathname').should('include', '/dashboard');
  cy.get('.orangehrm-dashboard-grid').should('be.visible');
  // My Actions không bật spinner khi tải; chỉ quan sát request do UI tạo ra.
  cy.wait(`@${summaryAlias}`).its('response.statusCode').should('eq', 200);
  cy.get('.orangehrm-dashboard-widget-loader').should('not.exist');
  cy.ready();
}

// Đăng nhập qua biểu mẫu, không sử dụng API hoặc thay đổi cookie trực tiếp.
Cypress.Commands.add('loginUI', (username, password) => {
  const number = ++loginNumber;
  const initialSummary = `loginInitialSummary${number}`;
  const loginSummary = `loginSummary${number}`;
  cy.intercept('GET', '**/api/v2/dashboard/employees/action-summary*').as(initialSummary);
  cy.visit('/web/index.php/auth/login');
  // Nếu phiên còn đăng nhập và trang Login chuyển về Dashboard, đăng xuất bằng UI trước.
  cy.get('body').should(($body) => {
    expect($body.find('input[name="username"], .oxd-userdropdown-tab').length,
      'Biểu mẫu Login hoặc menu tài khoản đã hiển thị').to.be.greaterThan(0);
  }).then(($body) => {
    if ($body.find('.oxd-userdropdown-tab').length) {
      dashboardReady(initialSummary);
      cy.logoutUI();
    }
  });
  cy.get('input[name="username"]').should('be.visible').clear().type(username);
  cy.get('input[name="password"]').clear().type(password, { log: false });
  cy.intercept('GET', '**/api/v2/dashboard/employees/action-summary*').as(loginSummary);
  cy.contains('button', 'Login').click();
  dashboardReady(loginSummary);
});

// Đăng xuất bằng menu tài khoản trên thanh điều hướng.
Cypress.Commands.add('logoutUI', () => {
  cy.get('.oxd-userdropdown-tab').click();
  cy.contains('a', /^\s*Logout\s*$/).click();
  cy.get('input[name="username"]').should('be.visible');
});

// Chuyển tài khoản qua nút Logout và biểu mẫu Login, kể cả khi case trước thất bại.
Cypress.Commands.add('switchUserUI', (username, password) => {
  cy.get('body').then(($body) => {
    if ($body.find('.oxd-userdropdown-tab').length) cy.logoutUI();
  });
  cy.loginUI(username, password);
});

let navigationNumber = 0;

// Mở màn hình bằng menu và chờ request do chính thao tác UI phát sinh hoàn tất.
Cypress.Commands.add('leavePage', (menu, item) => {
  const number = ++navigationNumber;
  let wasInLeave = false;
  function openTab(target, subitem, suffix) {
    const alias = `leaveNavigation${number}${suffix}`;
    const periodAlias = `${alias}Period`;
    const isList = target === 'My Leave' || target === 'Leave List';
    const endpoint = target === 'Leave List' ? 'employees/leave-requests'
      : target === 'My Leave' ? 'leave-requests'
      : target === 'Entitlements' ? 'leave-periods'
      : subitem === 'Holidays' ? 'holidays'
      : target === 'Apply' ? 'leave-types/eligible' : 'leave-types';
    const route = subitem === 'Holidays'
      ? { method: 'GET', url: '**/api/v2/leave/holidays*', query: { limit: '50', offset: '0' } }
      : { method: 'GET', url: `**/api/v2/leave/${endpoint}*` };
    cy.intercept(route).as(alias);
    if (isList) cy.intercept('GET', '**/api/v2/leave/leave-periods*').as(periodAlias);
    cy.contains('.oxd-topbar-body-nav-tab', new RegExp(`^\\s*${Cypress._.escapeRegExp(target)}\\s*$`)).click();
    if (subitem) cy.contains('.oxd-topbar-body-nav-tab-link', new RegExp(`^\\s*${Cypress._.escapeRegExp(subitem)}\\s*$`)).click();
    cy.wait(`@${alias}`, { requestTimeout: 20000 }).its('response.statusCode').should('eq', 200);
    // Kỳ phép khởi tạo ngày độc lập với bảng; chờ cả hai trước khi nhập bộ lọc.
    if (isList) cy.wait(`@${periodAlias}`, { requestTimeout: 20000 }).its('response.statusCode').should('eq', 200);
    cy.ready();
  }

  cy.location('pathname').then((pathname) => {
    wasInLeave = pathname.includes('/leave/');
    if (!wasInLeave) {
      cy.contains('.oxd-main-menu-item', /^\s*Leave\s*$/).click();
      cy.location('pathname').should('include', '/leave/');
      cy.get('.oxd-topbar-body-nav').should('be.visible');
      cy.ready();
    }
  });
  // Đi qua một tab khác để mở biểu mẫu mới, kể cả khi menu trái nhớ trang Entitlements.
  cy.get('.oxd-topbar-body-nav-tab').then(($tabs) => {
    const hasAdminList = $tabs.toArray().some((tab) => tab.innerText.trim() === 'Leave List');
    const reset = menu === 'Leave List' ? 'My Leave'
      : menu === 'My Leave' ? (hasAdminList ? 'Leave List' : 'Apply')
      : hasAdminList ? 'Leave List' : 'My Leave';
    const resetPath = reset === 'Leave List' ? '/viewLeaveList'
      : reset === 'My Leave' ? '/viewMyLeaveList' : '/applyLeave';
    cy.location('pathname').then((pathname) => {
      const sameList = (menu === 'Leave List' && pathname.endsWith('/viewLeaveList'))
        || (menu === 'My Leave' && pathname.endsWith('/viewMyLeaveList'));
      if (wasInLeave && sameList) {
        // Kỳ đã tải xong ở trang đang mở; Reset xóa bộ lọc và tải lại bảng qua UI.
        const alias = `leaveNavigation${number}ResetList`;
        const endpoint = menu === 'Leave List' ? 'employees/leave-requests' : 'leave-requests';
        cy.intercept('GET', `**/api/v2/leave/${endpoint}*`).as(alias);
        cy.contains('button', /^\s*Reset\s*$/).click();
        cy.wait(`@${alias}`, { requestTimeout: 20000 }).its('response.statusCode').should('eq', 200);
        cy.ready();
        return;
      }
      if (!pathname.endsWith(resetPath)) openTab(reset, undefined, 'Reset');
      openTab(menu, item, 'Target');
    });
  });
});

// Chọn nhân viên đã tạo riêng từ gợi ý autocomplete.
Cypress.Commands.add('employeeUI', (name) => {
  cy.field('Employee Name').find('input').clear().type(name);
  cy.contains('.oxd-autocomplete-option', name).should('be.visible').click();
});

// Chỉ lấy dòng có ô khớp hoàn toàn với dữ liệu của lần chạy này.
Cypress.Commands.add('ownRow', (value) => {
  return cy.contains('.oxd-table-cell', new RegExp(`^\\s*${Cypress._.escapeRegExp(value)}\\s*$`)).closest('.oxd-table-row');
});
