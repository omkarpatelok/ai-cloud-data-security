/* ================================================================
   API SERVICE LAYER — Data Classification Dashboard
   Centralized fetch calls with Basic Auth, caching, error handling
   ================================================================ */

const API = (() => {
  const BASE_URL = 'http://localhost:8081';

  // ── Credential store (in-memory, persisted to sessionStorage) ──
  let _credentials = null;

  function setCredentials(username, password) {
    _credentials = { username, password };
    const token = btoa(`${username}:${password}`);
    sessionStorage.setItem('auth_token', token);
    sessionStorage.setItem('auth_user', username);
  }

  function getAuthHeader() {
    let token = sessionStorage.getItem('auth_token');
    if (!token && _credentials) {
      token = btoa(`${_credentials.username}:${_credentials.password}`);
    }
    return token ? { 'Authorization': `Basic ${token}` } : {};
  }

  function getStoredUsername() {
    return _credentials?.username || sessionStorage.getItem('auth_user') || null;
  }

  function clearCredentials() {
    _credentials = null;
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('auth_user');
    _cache.clear();
  }

  function isAuthenticated() {
    return !!sessionStorage.getItem('auth_token');
  }


  // ── Simple response cache (TTL-based) ──
  const _cache = new Map();
  const CACHE_TTL = 30_000; // 30 seconds

  function cacheGet(key) {
    const entry = _cache.get(key);
    if (entry && Date.now() - entry.ts < CACHE_TTL) return entry.data;
    _cache.delete(key);
    return null;
  }

  function cacheSet(key, data) {
    _cache.set(key, { data, ts: Date.now() });
  }

  function invalidateCache(prefix) {
    for (const key of _cache.keys()) {
      if (key.startsWith(prefix)) _cache.delete(key);
    }
  }

  /** Clear cached GET responses for scans and security summary (after mutations). */
  function invalidateScanCaches() {
    invalidateCache(`${BASE_URL}/api/scans`);
    invalidateCache(`${BASE_URL}/api/security`);
  }


  // ── Core fetch wrapper ──
  async function request(path, options = {}, useCache = false) {
    const url = `${BASE_URL}${path}`;

    // Check cache for GET requests
    if (useCache && (!options.method || options.method === 'GET')) {
      const cached = cacheGet(url);
      if (cached) return cached;
    }

    const headers = {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
      ...options.headers,
    };

    let response;
    try {
      response = await fetch(url, { ...options, headers });
    } catch (err) {
      throw { status: 0, message: 'Backend not reachable. Make sure the server is running on port 8081.' };
    }

    if (!response.ok) {
      let body = {};
      try {
        const text = await response.text();
        if (text) {
          try {
            body = JSON.parse(text);
          } catch {
            body = { message: text };
          }
        }
      } catch {
        body = { message: response.statusText };
      }
      const serverMsg = body.message || body.error || response.statusText;

      if (response.status === 401) {
        throw { status: 401, message: serverMsg || 'Unauthorized — invalid username or password.' };
      }
      if (response.status === 403) {
        throw { status: 403, message: serverMsg || 'Forbidden — you do not have access to this resource.' };
      }
      // Prefer backend message for 5xx/503 (e.g. AWS credentials, scan errors)
      throw {
        status: response.status,
        message: serverMsg || (response.status >= 500 ? 'Server error — please try again later.' : response.statusText),
      };
    }

    const data = await response.json();

    // Populate cache
    if (useCache && (!options.method || options.method === 'GET')) {
      cacheSet(url, data);
    }

    return data;
  }


  // ── Public API methods ──

  /** GET /api/security/summary → { totalScans, highRiskFiles, mediumRiskFiles, lowRiskFiles } */
  async function getSummary() {
    return request('/api/security/summary', {}, true);
  }

  /** GET /api/scans → array of ScanResult */
  async function getScans() {
    return request('/api/scans', {}, true);
  }

  /** GET /api/security/high-risk → array of ScanResult (CRITICAL + HIGH) */
  async function getHighRiskScans() {
    return request('/api/security/high-risk', {}, true);
  }

  /** POST /api/scans → ScanResult */
  async function triggerScan(resourceName, resourceType) {
    invalidateScanCaches();
    return request('/api/scans', {
      method: 'POST',
      body: JSON.stringify({ resourceName, resourceType }),
    });
  }

  /** POST /api/scans/batch → array of ScanResult */
  async function triggerBatchScan() {
    invalidateScanCaches();
    return request('/api/scans/batch', { method: 'POST' });
  }

  /**
   * GET /api/upload-url?fileName=&contentType=&fileSize=
   * → { uploadUrl, fileKey }
   */
  async function getUploadUrl(fileName, contentType, fileSize) {
    const q = new URLSearchParams();
    q.set('fileName', fileName);
    if (contentType) q.set('contentType', contentType);
    if (fileSize != null) q.set('fileSize', String(fileSize));
    return request(`/api/upload-url?${q.toString()}`, {}, false);
  }

  /**
   * PUT file bytes to S3 using a pre-signed URL (no Basic auth; no JSON).
   * @param {string} uploadUrl
   * @param {File} file
   * @param {string} contentType
   * @param {(pct: number) => void} [onProgress] 0..1
   */
  function uploadFileToS3(uploadUrl, file, contentType, onProgress) {
    const ct = contentType && contentType.trim() ? contentType.trim() : 'application/octet-stream';
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', uploadUrl);
      xhr.setRequestHeader('Content-Type', ct);

      // Inject authorization for the local proxy endpoint to fix HTTP 401
      if (uploadUrl.includes(BASE_URL) || uploadUrl.startsWith('/')) {
        const auth = getAuthHeader();
        if (auth.Authorization) {
          xhr.setRequestHeader('Authorization', auth.Authorization);
        }
      }

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && typeof onProgress === 'function') {
          onProgress(e.loaded / e.total);
        }
      };
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) resolve();
        else {
          reject({ status: xhr.status, message: `Upload failed (HTTP ${xhr.status})` });
        }
      };
      xhr.onerror = () => reject({ status: 0, message: 'Upload failed (network)' });
      xhr.send(file);
    });
  }

  /** GET /api/audit-logs → array of AuditLog (admin only) */
  async function getAuditLogs() {
    return request('/api/audit-logs', {}, true);
  }

  /** GET /api/metadata → array of S3MetadataEntity (admin only) */
  async function getMetadata() {
    return request('/api/metadata', {}, true);
  }

  /**
   * Login flow:
   * 1) Store credentials
   * 2) Call /api/security/summary to validate
   * 3) Determine role by attempting /api/audit-logs (admin-only endpoint)
   */
  async function login(username, password) {
    setCredentials(username, password);

    // Validate credentials
    try {
      await request('/api/security/summary');
    } catch (err) {
      clearCredentials();
      throw err;
    }

    // Determine role by probing admin endpoint
    let role = 'user';
    try {
      await request('/api/audit-logs');
      role = 'admin';
    } catch (_) {
      // 403 → user role, which is fine
    }

    return { name: username, role };
  }


  // ── Expose ──
  return {
    login,
    clearCredentials,
    isAuthenticated,
    getStoredUsername,
    getSummary,
    getScans,
    getHighRiskScans,
    triggerScan,
    triggerBatchScan,
    getUploadUrl,
    uploadFileToS3,
    getAuditLogs,
    getMetadata,
    invalidateCache,
    invalidateScanCaches,
  };
})();
