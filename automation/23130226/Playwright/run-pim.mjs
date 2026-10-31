import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
import {chromium} from 'playwright';
import sharp from 'sharp';
const repo=process.env.PIM_REPO||path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..');
const runId=process.env.PIM_RUN_ID||`PIM_${new Date().toISOString().replace(/[:.]/g,'-')}`;
const out=path.join(repo,'evidence/executions/23130226',runId);
await fs.mkdir(path.join(out,'screenshots'),{recursive:true});await fs.mkdir(path.join(out,'assets'),{recursive:true});
const base='https://opensource-demo.orangehrmlive.com/web/index.php';
const actions=[...Array(14).fill('Create'),'CancelCreate',...Array(4).fill('UploadPhoto'),'Search','Search','Search','Filter','Reset','Sort'];
const counters={};const ids=actions.map(a=>`PIM_EMP_${a}_${String(counters[a]=(counters[a]||0)+1).padStart(2,'0')}`);
const report={runId,startedAt:new Date().toISOString(),baseUrl:base,tool:'Playwright',browser:null,fixtures:{},created:[],results:[],cleanup:[],setupErrors:[]};
const browser=await chromium.launch({channel:'msedge',headless:true});report.browser=await browser.version();
const context=await browser.newContext({viewport:{width:1440,height:1000},locale:'en-US'});const page=await context.newPage();
page.setDefaultTimeout(20000);page.setDefaultNavigationTimeout(45000);
const seenRequests=[];page.on('response',r=>{if(/\/api\/v2\/pim\/employees/.test(r.url()))seenRequests.push({time:new Date().toISOString(),status:r.status(),method:r.request().method(),url:r.url().replace(/https:\/\/[^/]+/,'')});});
async function write(){await fs.writeFile(path.join(out,'results.json'),JSON.stringify(report,null,2));}
async function shot(name){const p=`screenshots/${name}.png`;await page.screenshot({path:path.join(out,p),fullPage:true});return p;}
function group(label){return page.locator('.oxd-input-group').filter({has:page.locator('label').filter({hasText:new RegExp(`^${label}$`)})});}
function field(label){return group(label).locator('input');}
const nameField=n=>page.locator(`input[name="${n}Name"]`);
async function ready(){await page.locator('.oxd-form-loader').waitFor({state:'hidden',timeout:30000});}
async function login(){await page.goto(`${base}/auth/login`);await page.locator('[name=username]').fill('Admin');await page.locator('[name=password]').fill('admin123');await page.getByRole('button',{name:'Login',exact:true}).click();await page.waitForURL('**/dashboard/**');}
const renderedId=value=>String(value??'').trim();
assert.equal(renderedId(null),'');assert.equal(renderedId(' 01 '),'01');
async function tableReady(response){const body=await response.json();assert.equal(response.status(),200,'Employees list request must succeed');const expected=body.data.map(r=>renderedId(r.employeeId));await page.waitForFunction(ids=>{const rows=[...document.querySelectorAll('.oxd-table-body .oxd-table-card')];return rows.length===ids.length&&rows.every((row,i)=>row.querySelectorAll('.oxd-table-cell')[1]?.textContent.trim()===ids[i]);},expected,{timeout:20000});return body;}
async function list(){const initial=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?')&&r.request().method()==='GET');await page.goto(`${base}/pim/viewEmployeeList`);await page.getByRole('button',{name:'Search',exact:true}).waitFor();await tableReady(await initial);await ready();}
async function search(id){await list();await field('Employee Id').fill(id);const response=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?')&&new URL(r.url()).searchParams.get('employeeId')===id&&r.request().method()==='GET');await page.getByRole('button',{name:'Search',exact:true}).click();await tableReady(await response);await ready();return page.locator('.oxd-table-body .oxd-table-card');}
async function noRecord(id){const rows=await search(id);assert.equal(await rows.count(),0,`ID ${id} must have no rows`);assert.match(await page.locator('.orangehrm-horizontal-padding').innerText(),/No Records Found/i);}
async function openAdd(){await page.goto(`${base}/pim/addEmployee`);await nameField('first').waitFor();await ready();}
async function fill(data){await nameField('first').fill(data.first??'G01N01');await nameField('middle').fill(data.middle??'Minh');await nameField('last').fill(data.last??'NhutBaseline');await field('Employee Id').fill(data.id);}
async function waitPersonalId(id){await page.waitForFunction(id=>[...document.querySelectorAll('.oxd-input-group')].some(g=>g.querySelector('label')?.textContent==='Employee Id'&&g.querySelector('input')?.value===id),id,{timeout:25000});}
async function saveNew(data,photo){await openAdd();await fill(data);if(photo)await page.locator('input[type=file]').setInputFiles(photo);await page.getByRole('button',{name:'Save',exact:true}).click();await page.waitForURL(/\/pim\/viewPersonalDetails\/empNumber\/\d+/);await nameField('first').waitFor();await waitPersonalId(data.id);await ready();const url=page.url();const key=url.split('/').at(-1);report.created.push({id:data.id,key,url});await page.reload();await nameField('first').waitFor();await waitPersonalId(data.id);await ready();assert.equal(await nameField('first').inputValue(),data.first??'G01N01');assert.equal(await nameField('middle').inputValue(),data.middle??'Minh');assert.equal(await nameField('last').inputValue(),data.last??'NhutBaseline');assert.equal(await field('Employee Id').inputValue(),data.id);return {key,url,id:data.id};}
async function caseRun(index,fn){const id=ids[index-1];const item={oldId:`TC_PIM_EMP_${String(index).padStart(3,'0')}`,id,startedAt:new Date().toISOString(),status:'Untested',actual:'',assertions:[],screenshots:[]};const t=Date.now();const reqStart=seenRequests.length;
 const check=(condition,msg)=>{assert.ok(condition,msg);item.assertions.push(msg);};
 try{await fn(item,check);assert.ok(item.assertions.length>0,'No assertion recorded');item.status='Passed';}
 catch(e){item.status=e.name==='AssertionError'?'Failed':'Blocked';item.actual=`${e.name}: ${e.message}`;item.error={name:e.name,message:e.message,stack:e.stack};}
 try{item.screenshots.push(await shot(id));}catch(e){item.screenshotError=e.message;}
 item.durationMs=Date.now()-t;item.finishedAt=new Date().toISOString();item.network=seenRequests.slice(reqStart);if(!item.actual)item.actual=item.assertions.join(' | ');report.results.push(item);await write();console.log(`${id}: ${item.status} (${item.durationMs} ms) ${item.actual.slice(0,220)}`);
}
async function choose(label,value){await group(label).locator('.oxd-select-text').click();const options=page.getByRole('option');await options.first().waitFor();const labels=(await options.allInnerTexts()).map(t=>t.trim()).filter(t=>t&&t!=='-- Select --');const v=value||labels[0];if(!v)throw Error(`No available ${label}`);await page.getByRole('option',{name:v,exact:true}).click();return v;}
async function makeFixture(id,last,middle=''){await noRecord(id);const f=await saveNew({id,last,middle});report.fixtures[id]=f;await write();return f;}
async function deleteOwned(f){const rows=await search(f.id);if(await rows.count()===0)return 'Already absent';assert.equal(await rows.count(),1);const edit=rows.first().locator('button').filter({has:page.locator('.bi-pencil-fill')});await edit.click();await page.waitForURL(new RegExp(`/empNumber/${f.key}$`));await waitPersonalId(f.id);assert.equal(await field('Employee Id').inputValue(),f.id,'Cleanup must match original ID and numeric employee key');const again=await search(f.id);await again.first().locator('button').filter({has:page.locator('.bi-trash')}).click();await page.getByRole('button',{name:/Yes, Delete/}).click();await page.getByText('Successfully Deleted',{exact:true}).waitFor();await noRecord(f.id);return 'Deleted';}
async function setup(){await login();await write();if(process.env.PIM_CLEANUP_PREVIOUS){for(const file of process.env.PIM_CLEANUP_PREVIOUS.split(';')){const prev=JSON.parse(await fs.readFile(file,'utf8'));for(const f of prev.created){try{report.cleanup.push({fromRun:prev.runId,id:f.id,key:f.key,status:await deleteOwned(f)});}catch(e){report.setupErrors.push({id:f.id,phase:'previous cleanup',error:e.message});}await write();}}}for(const [id,last,middle] of [['G1N01D0001','NhutDuplicate',''],['G1N01P0001','NhutPerson','Minh'],['G1N01F0001','NhutFilterPositive',''],['G1N01F0002','NhutFilterNegative','']]){
 try{await makeFixture(id,last,middle);}catch(e){report.setupErrors.push({id,error:e.message});console.log(`SETUP ${id}: ${e.message.slice(0,160)}`);}await write();
 }
 const f=report.fixtures.G1N01F0001;
 if(f){try{await page.goto(`${base}/pim/viewJobDetails/empNumber/${f.key}`);await page.getByRole('heading',{name:'Job Details',exact:true}).waitFor();await ready();report.catalog={title:await choose('Job Title'),status:await choose('Employment Status')};await page.locator('form').first().getByRole('button',{name:'Save',exact:true}).click();await page.getByText('Successfully Updated',{exact:true}).waitFor();await page.reload();await page.waitForFunction(c=>{const groups=[...document.querySelectorAll('.oxd-input-group')];return [['Job Title',c.title],['Employment Status',c.status]].every(([l,v])=>groups.find(g=>g.querySelector('label')?.textContent===l)?.querySelector('.oxd-select-text-input')?.textContent.trim()===v);},report.catalog);assert.equal((await group('Job Title').locator('.oxd-select-text-input').innerText()).trim(),report.catalog.title);assert.equal((await group('Employment Status').locator('.oxd-select-text-input').innerText()).trim(),report.catalog.status);}catch(e){report.setupErrors.push({id:'F_FILTER_JOB',error:e.message});report.catalogError=e.message;}}
 await write();
}
// Technical upload fixtures, not screenshots or fabricated execution evidence.
const jpg=path.join(out,'assets/photo_valid.jpg'),png=path.join(out,'assets/photo_valid.png'),pdf=path.join(out,'assets/photo_invalid.pdf'),large=path.join(out,'assets/photo_over_1m.png');
await sharp({create:{width:200,height:200,channels:3,background:'#3B82F6'}}).jpeg().toFile(jpg);await sharp({create:{width:200,height:200,channels:3,background:'#DC7A20'}}).png().toFile(png);
// A valid one-page PDF fixture assembled without any user information.
const {PDFDocument}=await import('pdf-lib');const doc=await PDFDocument.create();doc.addPage();await fs.writeFile(pdf,await doc.save());
const raw=Buffer.alloc(580*580*3);let seed=23130226;for(let i=0;i<raw.length;i++){seed=(Math.imul(seed,1664525)+1013904223)>>>0;raw[i]=seed>>>24;}
const image=await sharp(raw,{raw:{width:580,height:580,channels:3}}).png({compressionLevel:0}).toBuffer();
const extra=Buffer.alloc(1048577-image.length-12,65);Buffer.from('Comment\0').copy(extra);const chunkType=Buffer.from('tEXt');const chunk=Buffer.alloc(extra.length+12);chunk.writeUInt32BE(extra.length);chunkType.copy(chunk,4);extra.copy(chunk,8);let crc=0xffffffff;for(const b of Buffer.concat([chunkType,extra])){crc^=b;for(let j=0;j<8;j++)crc=(crc>>>1)^((crc&1)?0xedb88320:0);}chunk.writeUInt32BE((crc^0xffffffff)>>>0,chunk.length-4);await fs.writeFile(large,Buffer.concat([image.subarray(0,-12),chunk,image.subarray(-12)]));report.assets={jpg:(await fs.stat(jpg)).size,png:(await fs.stat(png)).size,pdf:(await fs.stat(pdf)).size,large:(await fs.stat(large)).size};
try{await setup();
for(let n=1;n<=25;n++){if(process.env.PIM_ONLY&&!process.env.PIM_ONLY.split(',').map(Number).includes(n))continue;await caseRun(n,async(item,check)=>{
 const id=`G1N01A${String(n).padStart(4,'0')}`;
 if([1,2,6,8,10,12,16,17].includes(n)){
  await noRecord(id);const data={id,first:'G01N01',middle:n===1?'':'Minh',last:`NhutA${String(n).padStart(3,'0')}`};if(n===6)data.first='A'.repeat(30);if(n===8)data.last='A'.repeat(30);if(n===10)data.middle='A'.repeat(30);
  const f=await saveNew(data,n===16?jpg:n===17?png:undefined);check(true,'Save và reload: First/Middle/Last/Employee Id khớp chính xác input');item.screenshots.push(await shot(`${item.id}-saved`));
  if([16,17].includes(n)){const pic=page.locator('img.employee-image');await pic.waitFor();const src=await pic.getAttribute('src');check(!src.includes('default-photo'),`Ảnh profile được lưu: ${src}`);check(await pic.evaluate(e=>e.complete&&e.naturalWidth>0),'Ảnh profile tải được sau reload');}
  if(n===1){const rows=await search(id);check(await rows.count()===1,'Employee List trả đúng 1 dòng');const cells=await rows.first().locator('.oxd-table-cell').allInnerTexts();check(cells.some(x=>x.trim()===id)&&cells.some(x=>x.trim()===data.first)&&cells.some(x=>x.trim()===data.last),'Đúng ID và tên nhân viên vừa tạo');}item.employeeKey=f.key;return;
 }
 if([3,4,5,7,9,11,13,14].includes(n)){
  if(n===14&&!report.fixtures.G1N01D0001)throw Error('F_DUP not prepared; refusing to use existing unrelated data');
  if(n!==14)await noRecord(id);await openAdd();const data={id:n===14?'G1N01D0001':id,first:'G01N01',middle:'Minh',last:n===14?'NhutDuplicateAttempt':`NhutA${String(n).padStart(3,'0')}`};
  if(n===3)data.first='';if(n===4)data.last='';if(n===5)data.first='   ';if(n===7)data.first='A'.repeat(31);if(n===9)data.last='A'.repeat(31);if(n===11)data.middle='A'.repeat(31);if(n===13)data.id+='X';
  await fill(data);await field('Employee Id').press('Tab');await page.getByRole('button',{name:'Save',exact:true}).click();
  const target=n===4?nameField('last'):n===9?nameField('last'):n===11?nameField('middle'):[13,14].includes(n)?field('Employee Id'):nameField('first');
  const errors=target.locator('xpath=ancestor::div[contains(@class,"oxd-input-group")][1]').locator('.oxd-input-field-error-message');await errors.waitFor();const actual=await errors.innerText();
  const pattern=[3,4,5].includes(n)?/Required/:n===14?/already exists/i:/Should not exceed (30|10) characters/;
  check(pattern.test(actual),`Lỗi đúng trường: ${actual}`);check(page.url().includes('/pim/addEmployee'),'Không chuyển sang hồ sơ mới');item.screenshots.push(await shot(`${item.id}-validation`));
  if(n!==14){await noRecord(n===13?id:data.id);check(true,n===13?'Form chặn ID 11 ký tự; tìm prefix hợp lệ không có hồ sơ mới':'Search xác nhận không có hồ sơ mang ID input sai');}else{const rows=await search(data.id);check(await rows.count()===1,'Chỉ còn một dòng F_DUP sau khi thử ID trùng');check((await rows.first().innerText()).includes('NhutDuplicate')&&!(await rows.first().innerText()).includes('Attempt'),'F_DUP không bị thay bằng tên nhập thử');}return;
 }
 if(n===15){await noRecord(id);await openAdd();await fill({id,first:'G01N01',middle:'',last:'NhutCancel'});await page.getByRole('button',{name:'Cancel',exact:true}).click();await page.waitForURL('**/pim/viewEmployeeList');check(true,'Cancel quay về Employee List');await noRecord(id);check(true,'Không có hồ sơ với ID vừa hủy');return;}
 if(n===18||n===19){await noRecord(id);await openAdd();await fill({id,last:`NhutA${String(n).padStart(3,'0')}`});await page.locator('input[type=file]').setInputFiles(n===18?pdf:large);const err=page.locator('.oxd-input-field-error-message');await err.waitFor();const msg=await err.innerText();check(n===18?/File type not allowed/i.test(msg):/size|1.?MB|1.?048.?576/i.test(msg),`Upload validation: ${msg}`);item.screenshots.push(await shot(`${item.id}-validation`));await page.getByRole('button',{name:'Save',exact:true}).click();check(page.url().includes('/pim/addEmployee'),'Save bị chặn ở Add Employee');check(await err.isVisible(),'Validation vẫn hiển thị sau Save');await noRecord(id);check(true,'Không tạo hồ sơ và không lưu ảnh không hợp lệ');return;}
 if(n===20){if(!report.fixtures.G1N01D0001)throw Error('F_DUP missing');const rows=await search('G1N01D0001');check(await rows.count()===1,'Tìm theo Employee Id trả 1 dòng');const data=await rows.first().locator('.oxd-table-cell').allInnerTexts();check(data.some(x=>x.trim()==='G1N01D0001'),'ID kết quả khớp chính xác');return;}
 if(n===21){if(!report.fixtures.G1N01P0001)throw Error('F_PERSON missing');await list();await field('Employee Name').fill('NhutPerson');const opt=page.getByRole('option').filter({hasText:/G01N01\s+Minh\s+NhutPerson/});await opt.waitFor();await opt.click();const response=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?'));await page.getByRole('button',{name:'Search',exact:true}).click();await tableReady(await response);await ready();const rows=page.locator('.oxd-table-body .oxd-table-card');check(await rows.count()===1,'Autocomplete đã chọn trả 1 dòng');check((await rows.first().innerText()).includes('G1N01P0001'),'Đúng F_PERSON có ID G1N01P0001');return;}
 if(n===22){await noRecord('G1N01NONE1');check(true,'ID không tồn tại hiển thị No Records Found và bảng không có dòng');return;}
 if(n===23){if(!report.catalog||report.catalogError||!report.fixtures.G1N01F0002)throw Error(`F_FILTER not ready: ${report.catalogError||'missing fixture'}`);await list();await choose('Employment Status',report.catalog.status);await choose('Job Title',report.catalog.title);const response=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?'));await page.getByRole('button',{name:'Search',exact:true}).click();const res=await response;const json=await res.json();await ready();let all=[];let pages=0;do{await ready();const rows=await page.locator('.oxd-table-body .oxd-table-card').all();for(const row of rows)all.push(await row.locator('.oxd-table-cell').allInnerTexts());const next=page.locator('.oxd-pagination-page-item--next');if(await next.count()===0||await next.isDisabled())break;const wait=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?'));await next.click();await wait;pages++;if(pages>20)throw Error('Too many result pages; stop safe');}while(true);
 check(all.some(c=>c.some(v=>v.trim()==='G1N01F0001')),'Có positive control F_FILTER');check(!all.some(c=>c.some(v=>v.trim()==='G1N01F0002')),'Không có negative control F_FILTER_NEG');check(all.every(c=>c[4].trim()===report.catalog.title&&c[5].trim()===report.catalog.status),'Mọi dòng khớp đồng thời Job Title và Employment Status');check(all.length===json.meta.total,`Đã kiểm tra toàn bộ ${all.length} dòng kết quả`);return;}
 if(n===24){if(!report.fixtures.G1N01D0001||!report.catalog)throw Error('F_DUP/catalog missing');await list();await field('Employee Id').fill('G1N01D0001');await choose('Employment Status',report.catalog.status);let response=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?'));await page.getByRole('button',{name:'Search',exact:true}).click();await response;await ready();await page.getByRole('button',{name:'Reset',exact:true}).click();check((await field('Employee Id').inputValue())==='','Reset xóa Employee Id');check((await group('Employment Status').locator('.oxd-select-text-input').innerText()).trim()==='-- Select --','Employment Status về mặc định');response=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?'));await page.getByRole('button',{name:'Search',exact:true}).click();const r=await response;await ready();check(!r.url().includes('employeeId=G1N01D0001')&&!/empStatusId=\d/.test(r.url()),'Search mới không giữ bộ lọc ID/status trước đó');return;}
 if(n===25){
  const controls=[{id:'G1N01S0001',first:'G01N01SortAlpha',middle:'',last:'NhutSortAlpha'},{id:'G1N01S0002',first:'G01N01SortZulu',middle:'',last:'NhutSortZulu'}];
  for(const data of controls){await noRecord(data.id);await saveNew(data);await write();}
  await list();const header=page.getByRole('columnheader').filter({hasText:'First (& Middle) Name'});const observed=[];
  for(const dir of ['Ascending','Descending']){
   await header.locator('.oxd-table-header-sort-icon').click();
   let pending=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?')&&new URL(r.url()).searchParams.get('sortOrder')===(dir==='Ascending'?'ASC':'DESC'));
   await header.getByText(dir,{exact:true}).click();
   let response=await pending;const allIds=[];let pages=0;let total;
   do{
    const body=await tableReady(response);await ready();total=body.meta.total;
    const cells=await page.locator('.oxd-table-body .oxd-table-card').all();const names=[];
    for(const row of cells)names.push((await row.locator('.oxd-table-cell').nth(2).innerText()).trim());
    const displayName=value=>value.replace(/\s+/g,' ').trim();
    assert.deepEqual(names.map(displayName),body.data.map(e=>displayName(`${e.firstName}${e.middleName?' '+e.middleName:''}`)),'Tên hiển thị phải khớp thứ tự response sau khi chuẩn hóa khoảng trắng HTML');
    allIds.push(...body.data.map(e=>e.employeeId));
    if(pages===0)item.screenshots.push(await shot(`${item.id}-${dir}`));
    const next=page.locator('.oxd-pagination-page-item').filter({has:page.locator('.bi-chevron-right')});if(await next.count()===0||await next.isDisabled())break;
    if(++pages>20)throw Error('Too many sort result pages; stop safe');
    pending=page.waitForResponse(r=>r.url().includes('/api/v2/pim/employees?'));await next.click();response=await pending;
   }while(true);
   const positions=controls.map(c=>allIds.indexOf(c.id));check(positions.every(i=>i>=0),'Có đủ hai fixture tên khác nhau trong danh sách đã đọc');
   // shortcut: đối chiếu thứ tự bằng hai fixture ASCII; bổ sung oracle khi chốt collation cho tên đa ngôn ngữ.
   check(dir==='Ascending'?positions[0]<positions[1]:positions[0]>positions[1],`Hai fixture Alpha/Zulu đúng chiều ${dir}; từng trang khớp response`);
   observed.push({direction:dir,total,positions,recordsRead:allIds.length});
  }
  check(observed[0].total===observed[1].total,'Tổng bản ghi không đổi giữa hai chiều sắp xếp');item.observed=observed;return;
 }
});}
}catch(e){report.fatal={name:e.name,message:e.message,stack:e.stack};console.error('FATAL',e.message);}
finally{
 // Delete only records whose numeric employee keys were returned by this run's own UI creates.
 for(const f of report.created){try{report.cleanup.push({id:f.id,key:f.key,status:await deleteOwned(f)});}catch(e){report.cleanup.push({id:f.id,key:f.key,status:'Needs cleanup',error:e.message});}}
 report.finishedAt=new Date().toISOString();report.summary=Object.fromEntries(['Passed','Failed','Blocked','Untested'].map(k=>[k,report.results.filter(r=>r.status===k).length]));await write();await browser.close();console.log('SUMMARY',JSON.stringify(report.summary),'OUT',out);
}
