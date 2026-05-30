/* ================================================================
   DATA CLASSIFICATION — App Logic
   Scroll-aware navbar, navigation, auth, data rendering
   Now powered by real REST API via api.js
   ================================================================ */

// ===================== STATE =====================
let currentUser = null;
let currentPage = 'login';
let scanData = [];
let filteredScans = [];
let sortKey = 'id';
let sortDir = 1;
let currentPageNum = 1;
const PAGE_SIZE = 6;

// Debounce timer for search
let _searchDebounce = null;

/** Max upload size (must match backend app.upload.max-file-size-bytes) */
const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;


// ===================== DOM REFS =====================
const navbar       = document.getElementById('navbar');
const navLinks     = document.getElementById('navbar-links');
const navUser      = document.getElementById('navbar-user');
const hamburgerBtn = document.getElementById('hamburger');
const overlay      = document.getElementById('mobile-overlay');
const drawer       = document.getElementById('mobile-drawer');
const drawerClose  = document.getElementById('drawer-close');
const mainContent  = document.getElementById('main-content');


// ===================== SCROLL-BASED NAVBAR =====================
let lastScrollY = 0;
let ticking = false;

function handleScroll() {
  const scrollY = window.pageYOffset || document.documentElement.scrollTop;

  if (scrollY > 40) {
    navbar.classList.add('navbar-scrolled');
  } else {
    navbar.classList.remove('navbar-scrolled');
  }

  lastScrollY = scrollY;
  ticking = false;
}

window.addEventListener('scroll', () => {
  if (!ticking) {
    window.requestAnimationFrame(handleScroll);
    ticking = true;
  }
}, { passive: true });


// ===================== MOBILE DRAWER =====================
function openDrawer() {
  drawer.classList.add('open');
  overlay.classList.add('open');
  hamburgerBtn.classList.add('open');
  document.body.style.overflow = 'hidden';
}

function closeDrawer() {
  drawer.classList.remove('open');
  overlay.classList.remove('open');
  hamburgerBtn.classList.remove('open');
  document.body.style.overflow = '';
}

hamburgerBtn.addEventListener('click', () => {
  if (drawer.classList.contains('open')) closeDrawer();
  else openDrawer();
});

overlay.addEventListener('click', closeDrawer);
drawerClose.addEventListener('click', closeDrawer);


// ===================== AUTH =====================
async function doLogin() {
  const u = document.getElementById('login-user').value.trim();
  const p = document.getElementById('login-pass').value.trim();
  const err = document.getElementById('login-error');
  const btn = document.getElementById('login-submit-btn');
  err.classList.remove('show');

  if (!u || !p) {
    err.textContent = 'Please enter both username and password.';
    err.classList.add('show');
    return;
  }

  // Disable button & show loading
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-sm"></span><span>Authenticating…</span>';

  try {
    currentUser = await API.login(u, p);

    const displayName = u.charAt(0).toUpperCase() + u.slice(1);
    const initials = u.slice(0, 2).toUpperCase();
    const roleText = currentUser.role === 'admin' ? 'ADMINISTRATOR' : 'ANALYST';

    // Update navbar user
    document.getElementById('user-display-name').textContent = displayName;
    document.getElementById('user-avatar').textContent = initials;
    navUser.style.display = 'flex';

    // Update drawer user
    document.getElementById('drawer-avatar').textContent = initials;
    document.getElementById('drawer-user-name').textContent = displayName;
    document.getElementById('drawer-user-role').textContent = roleText;
    document.getElementById('drawer-footer').style.display = 'flex';

    // Show nav links
    navLinks.style.display = 'flex';

    // Admin-only items
    if (currentUser.role === 'admin') {
      document.getElementById('nav-admin').style.display = 'flex';
      document.getElementById('drawer-admin').style.display = 'block';
    }

    navigate('dashboard');
  } catch (apiErr) {
    err.textContent = apiErr.message || 'Login failed. Please try again.';
    err.classList.add('show');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `
      <span>Authenticate</span>
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 8h10M9 4l4 4-4 4"/></svg>
    `;
  }
}

function logout() {
  currentUser = null;
  API.clearCredentials();
  closeDrawer();

  navUser.style.display = 'none';
  navLinks.style.display = 'none';
  document.getElementById('nav-admin').style.display = 'none';
  document.getElementById('drawer-admin').style.display = 'none';
  document.getElementById('drawer-footer').style.display = 'none';

  document.getElementById('login-user').value = '';
  document.getElementById('login-pass').value = '';
  document.getElementById('login-error').classList.remove('show');

  showPage('login');
  currentPage = 'login';
}


// ===================== NAVIGATION =====================
function navigate(page) {
  if (!currentUser && page !== 'login') return;

  // Admin guard
  if (page === 'admin' && currentUser?.role !== 'admin') {
    showToast('error', 'Access Denied', 'Admin panel is restricted to administrators.');
    return;
  }

  // Update desktop nav links
  document.querySelectorAll('.nav-link').forEach(el => {
    el.classList.toggle('active', el.dataset.page === page);
  });

  // Update drawer links
  document.querySelectorAll('.drawer-link').forEach(el => {
    el.classList.toggle('active', el.dataset.page === page);
  });

  showPage(page);
  currentPage = page;

  // Scroll to top smoothly
  window.scrollTo({ top: 0, behavior: 'smooth' });

  // Render page content
  if (page === 'dashboard') renderDashboard();
  if (page === 'scans') renderScanTable();
  if (page === 'admin') renderAdmin();
}

function showPage(name) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const el = document.getElementById('page-' + name);
  if (el) el.classList.add('active');
}


// ===================== LOADING HELPERS =====================
function showTableLoader(containerId, colSpan) {
  const el = document.getElementById(containerId);
  if (el) {
    el.innerHTML = `
      <tr>
        <td colspan="${colSpan}" style="text-align:center;padding:48px 0;">
          <div class="loader-pulse"></div>
          <div style="color:var(--text-muted);font-size:13px;margin-top:12px;">Loading data…</div>
        </td>
      </tr>
    `;
  }
}

function showContainerLoader(containerId) {
  const el = document.getElementById(containerId);
  if (el) {
    el.innerHTML = `
      <div style="text-align:center;padding:48px 0;">
        <div class="loader-pulse"></div>
        <div style="color:var(--text-muted);font-size:13px;margin-top:12px;">Loading…</div>
      </div>
    `;
  }
}

function showErrorInContainer(containerId, message) {
  const el = document.getElementById(containerId);
  if (el) {
    el.innerHTML = `
      <div style="text-align:center;padding:40px 20px;">
        <div style="font-size:28px;margin-bottom:8px;">⚠️</div>
        <div style="color:var(--accent-red);font-size:13px;font-weight:600;">${message}</div>
      </div>
    `;
  }
}

function showTableError(containerId, message, colSpan) {
  const el = document.getElementById(containerId);
  if (el) {
    el.innerHTML = `
      <tr>
        <td colspan="${colSpan}" style="text-align:center;padding:40px 20px;">
          <div style="font-size:28px;margin-bottom:8px;">⚠️</div>
          <div style="color:var(--accent-red);font-size:13px;font-weight:600;">${message}</div>
        </td>
      </tr>
    `;
  }
}


// ===================== DASHBOARD =====================
async function renderDashboard() {
  // ── KPI Cards ──
  const kpiValues = document.querySelectorAll('.kpi-value');
  kpiValues.forEach(v => v.textContent = '…');

  try {
    const summary = await API.getSummary();
    // Map to cards in order: Total, Critical/High, Medium, Low
    if (kpiValues[0]) kpiValues[0].textContent = summary.totalScans ?? 0;
    if (kpiValues[1]) kpiValues[1].textContent = summary.highRiskFiles ?? 0;
    if (kpiValues[2]) kpiValues[2].textContent = summary.mediumRiskFiles ?? 0;
    if (kpiValues[3]) kpiValues[3].textContent = summary.lowRiskFiles ?? 0;

    // Update scan count badge in nav
    const linkBadge = document.querySelector('.link-badge');
    if (linkBadge) linkBadge.textContent = summary.totalScans ?? 0;
  } catch (err) {
    kpiValues.forEach(v => v.textContent = '--');
    showToast('error', 'Dashboard Error', err.message);
  }

  // ── High-risk table ──
  const tbody = document.getElementById('dash-alert-table');
  showTableLoader('dash-alert-table', 6);

  try {
    const highRisk = await API.getHighRiskScans();
    const top5 = (highRisk || []).slice(0, 5);

    if (top5.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center;padding:32px;color:var(--text-muted);">
            No high-risk items found — looking good! 🎉
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = top5.map(s => `
      <tr>
        <td style="font-family:var(--font-mono);font-size:12px;color:var(--text-primary)">${s.resourceName || '—'}</td>
        <td>${s.dataType || '—'}</td>
        <td><span class="badge ${s.riskLevel}">${s.riskLevel}</span></td>
        <td>
          <div class="score-cell">
            <div class="score-bar"><div class="score-fill" style="width:${s.riskScore}%;background:${scoreColor(s.riskScore)}"></div></div>
            <span style="color:${scoreColor(s.riskScore)};font-weight:600;font-family:var(--font-mono);font-size:12px">${s.riskScore}</span>
          </div>
        </td>
        <td><span class="badge-action ${s.policyAction}">${s.policyAction || '—'}</span></td>
        <td class="text-muted" style="font-size:12px">${s.createdAt ? formatTime(s.createdAt) : '—'}</td>
      </tr>
    `).join('');
  } catch (err) {
    showTableError('dash-alert-table', err.message, 6);
  }
}


// ===================== SCAN TABLE =====================
async function renderScanTable() {
  showTableLoader('scan-table-body', 8);

  try {
    const scans = await API.getScans();
    scanData = scans || [];
    filteredScans = [...scanData];
    applyFilters();
  } catch (err) {
    showTableError('scan-table-body', err.message, 8);
  }
}

function filterScans() {
  // Debounce search input
  clearTimeout(_searchDebounce);
  _searchDebounce = setTimeout(() => applyFilters(), 250);
}

function applyFilters() {
  const search = document.getElementById('scan-search').value.toLowerCase();
  const risk   = document.getElementById('scan-filter-risk').value;
  const type   = document.getElementById('scan-filter-type').value;

  filteredScans = scanData.filter(s => {
    const matchSearch = !search || (s.resourceName || '').toLowerCase().includes(search) || (s.dataType || '').toLowerCase().includes(search);
    const matchRisk   = !risk   || s.riskLevel === risk;
    const matchType   = !type   || s.dataType === type;
    return matchSearch && matchRisk && matchType;
  });

  currentPageNum = 1;
  renderTablePage();
}

function sortScans(key) {
  if (sortKey === key) sortDir *= -1;
  else { sortKey = key; sortDir = 1; }

  filteredScans.sort((a, b) => {
    if (a[key] < b[key]) return -sortDir;
    if (a[key] > b[key]) return  sortDir;
    return 0;
  });

  renderTablePage();
}

function renderTablePage() {
  const start = (currentPageNum - 1) * PAGE_SIZE;
  const page  = filteredScans.slice(start, start + PAGE_SIZE);
  const tbody = document.getElementById('scan-table-body');

  if (filteredScans.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align:center;padding:32px;color:var(--text-muted);">
          No scan results found.
        </td>
      </tr>
    `;
    document.getElementById('scan-count-label').textContent = '0 results';
    document.getElementById('pagination').innerHTML = '';
    return;
  }

  tbody.innerHTML = page.map(s => `
    <tr>
      <td class="text-muted" style="font-family:var(--font-mono)">${s.id}</td>
      <td>
        <div class="resource-name">${s.resourceName || '—'}</div>
        <div class="resource-type">${s.resourceType || '—'}</div>
      </td>
      <td>${s.dataType || '—'}</td>
      <td><span class="badge ${s.riskLevel}">${s.riskLevel}</span></td>
      <td>
        <div class="score-cell">
          <div class="score-bar"><div class="score-fill" style="width:${s.riskScore}%;background:${scoreColor(s.riskScore)}"></div></div>
          <span style="font-weight:700;color:${scoreColor(s.riskScore)};font-family:var(--font-mono);font-size:12px">${s.riskScore}</span>
        </div>
      </td>
      <td><span class="badge-action ${s.policyAction}">${s.policyAction || '—'}</span></td>
      <td><span class="public-badge ${s.publicBucket ? 'yes' : 'no'}">${s.publicBucket ? 'PUBLIC' : 'PRIVATE'}</span></td>
      <td class="text-muted" style="font-size:12px">${s.createdAt ? formatTime(s.createdAt) : '—'}</td>
    </tr>
  `).join('');

  // Count label
  const total = filteredScans.length;
  document.getElementById('scan-count-label').textContent =
    `Showing ${Math.min(start + 1, total)}–${Math.min(start + PAGE_SIZE, total)} of ${total} results`;

  // Pagination
  const totalPages = Math.ceil(total / PAGE_SIZE);
  const pag = document.getElementById('pagination');
  pag.innerHTML = '';

  for (let i = 1; i <= totalPages; i++) {
    const btn = document.createElement('button');
    btn.className = 'page-btn' + (i === currentPageNum ? ' active' : '');
    btn.textContent = i;
    btn.onclick = () => { currentPageNum = i; renderTablePage(); };
    pag.appendChild(btn);
  }
}


// ===================== MANUAL SCAN =====================
function appendRecentScanCard(result, resourceLabel, containerId = 'recent-scans') {
  const empty = document.getElementById(containerId + '-empty');
  if (empty) empty.remove();

  const level = result.riskLevel || 'LOW';
  const score = result.riskScore ?? 0;
  const action = result.policyAction || 'ALLOW';
  const name = result.resourceName || resourceLabel;

  const icon = level === 'CRITICAL' ? '🔴' : level === 'HIGH' ? '🟠' : level === 'MEDIUM' ? '🟡' : '🟢';
  const iconClass = (level === 'CRITICAL' || level === 'HIGH') ? 'red-bg' : 'green-bg';
  const rec = document.getElementById(containerId);
  if (!rec) return;
  const div = document.createElement('div');
  div.className = 'recent-result';
  div.innerHTML = `
    <div class="result-icon ${iconClass}">${icon}</div>
    <div>
      <div class="result-name">${name}</div>
      <div class="result-meta">${result.dataType || '—'} — Score ${score} — <span style="color:${scoreColor(score)}">${action}</span></div>
    </div>
    <div class="badge ${level}" style="margin-left:auto">${level}</div>
  `;
  rec.insertBefore(div, rec.firstChild);
}

async function refreshAfterScan() {
  API.invalidateScanCaches();
  if (currentPage === 'dashboard') await renderDashboard();
  if (currentPage === 'scans') await renderScanTable();
}

async function triggerScan() {
  const nameInput = document.getElementById('scan-resource');
  const name = nameInput.value.trim();
  if (!name) {
    showToast('error', 'Missing Resource', 'Please enter a resource name.');
    return;
  }

  const type = document.getElementById('scan-type').value;
  const btn = document.getElementById('run-scan-btn');

  // Disable button
  btn.disabled = true;
  const origText = btn.innerHTML;
  btn.innerHTML = '<span class="spinner-sm"></span> Scanning…';

  try {
    const result = await API.triggerScan(name, type);

    const level = result.riskLevel || 'LOW';
    const score = result.riskScore ?? 0;
    const action = result.policyAction || 'ALLOW';

    showToast('success', 'Scan Complete', `${name} — Risk: ${level} (${score}) — Action: ${action}`);
    nameInput.value = '';

    appendRecentScanCard(result, name);
    await refreshAfterScan();
  } catch (err) {
    showToast('error', 'Scan Failed', err.message || String(err));
  } finally {
    btn.disabled = false;
    btn.innerHTML = origText;
  }
}

// ===================== FILE DRAG & DROP =====================
function handleFileSelect(event) {
  const input = document.getElementById('file-upload');
  const files = event.dataTransfer ? event.dataTransfer.files : event.target.files;
  if (!files || files.length === 0) return;
  input.files = files; // Sync the underlying input element if dropped

  const file = input.files[0];
  if (!file) return;

  document.getElementById('upload-zone').style.display = 'none';
  document.getElementById('selected-file-display').style.display = 'flex';
  document.getElementById('selected-file-name').textContent = file.name;
  document.getElementById('selected-file-size').textContent = formatBytes(file.size);
  document.getElementById('upload-error').classList.remove('show');
}

function clearSelectedFile() {
  const input = document.getElementById('file-upload');
  input.value = '';
  document.getElementById('upload-zone').style.display = 'block';
  document.getElementById('selected-file-display').style.display = 'none';
  document.getElementById('upload-error').classList.remove('show');
}


async function uploadAndScan() {
  const input = document.getElementById('file-upload');
  const file = input.files && input.files[0];
  const errEl = document.getElementById('upload-error');
  const progressRow = document.getElementById('upload-progress-row');
  const progressFill = document.getElementById('upload-progress-fill');
  const progressLabel = document.getElementById('upload-progress-label');
  const btn = document.getElementById('upload-scan-btn');

  errEl.classList.remove('show');
  errEl.textContent = '';

  if (!file || file.size === 0) {
    errEl.textContent = 'Please choose a non-empty file.';
    errEl.classList.add('show');
    return;
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    errEl.textContent = `File is too large (max ${Math.floor(MAX_UPLOAD_BYTES / (1024 * 1024))} MB).`;
    errEl.classList.add('show');
    return;
  }

  const contentType = file.type && file.type.trim() ? file.type.trim() : 'application/octet-stream';

  btn.disabled = true;
  const origBtn = btn.innerHTML;
  btn.innerHTML = '<span class="spinner-sm"></span> Preparing…';
  progressRow.style.display = 'block';
  progressFill.style.width = '0%';
  progressLabel.textContent = 'Uploading… 0%';

  let stage = 'presign';
  try {
    const { uploadUrl, fileKey } = await API.getUploadUrl(file.name, contentType, file.size);

    stage = 'upload';
    btn.innerHTML = '<span class="spinner-sm"></span> Uploading…';
    await API.uploadFileToS3(uploadUrl, file, contentType, (pct) => {
      const p = Math.round(pct * 100);
      progressFill.style.width = `${p}%`;
      progressLabel.textContent = `Uploading… ${p}%`;
    });

    stage = 'scan';
    progressLabel.textContent = 'Starting scan…';
    const result = await API.triggerScan(fileKey, 'S3_OBJECT');

    showToast('success', 'File uploaded & scan started', `${fileKey} — Risk: ${result.riskLevel || '—'}`);
    appendRecentScanCard(result, fileKey, 'recent-uploads');
    clearSelectedFile();
    await refreshAfterScan();
  } catch (err) {
    let msg = err.message || (err.status === 0 ? 'Backend not reachable.' : 'Upload or scan failed.');
    if (msg.length > 600) msg = msg.slice(0, 600) + '…';
    errEl.textContent = err.status === 401 ? 'Unauthorized — sign in again.' : msg;
    errEl.classList.add('show');
    const title = stage === 'scan' ? 'Scan failed' : stage === 'upload' ? 'Upload failed' : 'Request failed';
    showToast('error', title, msg);
  } finally {
    btn.disabled = false;
    btn.innerHTML = origBtn;
    progressRow.style.display = 'none';
    progressFill.style.width = '0%';
  }
}

async function triggerBatch() {
  const btn = document.getElementById('batch-scan-btn');
  btn.disabled = true;
  const origText = btn.innerHTML;
  btn.innerHTML = '<span class="spinner-sm"></span> Scanning Bucket…';

  try {
    const results = await API.triggerBatchScan();
    const count = Array.isArray(results) ? results.length : 0;
    showToast('success', 'Batch Scan Complete', `Scanned ${count} object(s). Check Scan History for results.`);
  } catch (err) {
    showToast('error', 'Batch Scan Failed', err.message);
  } finally {
    btn.disabled = false;
    btn.innerHTML = origText;
  }
}


// ===================== ADMIN =====================
async function renderAdmin() {
  // Audit logs
  showContainerLoader('audit-log-list');

  try {
    const auditLogs = await API.getAuditLogs();

    const list = document.getElementById('audit-log-list');
    if (!auditLogs || auditLogs.length === 0) {
      list.innerHTML = '<div style="text-align:center;padding:32px;color:var(--text-muted);">No audit logs found.</div>';
    } else {
      list.innerHTML = auditLogs.map(a => {
        const colorMap = { AUTO_REMEDIATE: 'var(--accent-light)', BLOCK: 'var(--critical)', QUARANTINE: 'var(--high)' };
        const bgMap    = { AUTO_REMEDIATE: 'rgba(59,130,246,0.15)', BLOCK: 'rgba(239,68,68,0.15)', QUARANTINE: 'rgba(249,115,22,0.12)' };

        return `
          <div class="audit-item">
            <div class="audit-dot" style="background:${colorMap[a.action] || 'var(--low)'}"></div>
            <div>
              <div class="audit-resource">${a.resourceName || '—'}</div>
              <div class="audit-reason">${a.reason || '—'}</div>
              <div class="audit-time">${a.timestamp ? formatTime(a.timestamp) : '—'}</div>
            </div>
            <div class="audit-action-badge" style="background:${bgMap[a.action] || 'rgba(34,197,94,0.1)'};color:${colorMap[a.action] || 'var(--low)'}">${a.action || '—'}</div>
          </div>
        `;
      }).join('');
    }
  } catch (err) {
    showErrorInContainer('audit-log-list', err.message);
  }

  // Metadata table
  showTableLoader('metadata-table', 6);

  try {
    const metadata = await API.getMetadata();

    const meta = document.getElementById('metadata-table');
    if (!metadata || metadata.length === 0) {
      meta.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:32px;color:var(--text-muted);">No metadata found.</td></tr>`;
    } else {
      meta.innerHTML = metadata.map(m => {
        const sizeStr = m.objectSize != null ? formatBytes(m.objectSize) : '—';
        // Derive content type from resourceName extension or default
        const ext = (m.resourceName || '').split('.').pop().toLowerCase();
        const contentTypeMap = {csv:'text/csv', sql:'application/sql', env:'text/plain', json:'application/json', zip:'application/zip', parquet:'application/parquet', tar:'application/x-tar', xlsx:'application/xlsx', log:'text/plain', yaml:'text/yaml', txt:'text/plain'};
        const contentType = contentTypeMap[ext] || 'application/octet-stream';

        return `
          <tr>
            <td class="resource-name">${m.resourceName || m.key || '—'}</td>
            <td>${sizeStr}</td>
            <td class="text-muted">${contentType}</td>
            <td class="text-muted">${m.lastModified || '—'}</td>
            <td><span class="public-badge ${m.publicBucket ? 'yes' : 'no'}">${m.publicBucket ? 'PUBLIC' : 'PRIVATE'}</span></td>
            <td>${m.encryptionEnabled
              ? '<span style="color:var(--low);font-weight:600">✓ YES</span>'
              : '<span style="color:var(--critical);font-weight:600">✗ NO</span>'
            }</td>
          </tr>
        `;
      }).join('');
    }
  } catch (err) {
    showTableError('metadata-table', err.message, 6);
  }
}

function adminTab(name, el) {
  document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.admin-content').forEach(c => c.classList.remove('active'));
  el.classList.add('active');
  document.getElementById('admin-' + name).classList.add('active');
}


// ===================== TOAST =====================
function showToast(type, title, msg) {
  const t = document.getElementById('toast');
  document.getElementById('toast-title').textContent = title;
  document.getElementById('toast-msg').textContent = msg;
  t.className = 'toast ' + type;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 4000);
}


// ===================== HELPERS =====================
function scoreColor(score) {
  if (score >= 75) return 'var(--critical)';
  if (score >= 55) return 'var(--high)';
  if (score >= 35) return 'var(--medium)';
  return 'var(--low)';
}

function formatTime(iso) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' ' +
         d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}


// ===================== CLOCK =====================
function updateClock() {
  const el = document.getElementById('clock');
  if (el) {
    el.textContent = new Date().toLocaleTimeString('en-US', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    });
  }
}
updateClock();
setInterval(updateClock, 1000);


// ===================== INIT =====================
// Hide nav links until logged in
navLinks.style.display = 'none';
