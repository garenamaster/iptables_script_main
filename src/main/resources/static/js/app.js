const API = {
    status: () => fetch('/api/v1/dashboard/status').then(r => r.json()),
    logs: () => fetch('/api/v1/dashboard/logs').then(r => r.json()),
    monitoring: () => fetch('/api/v1/dashboard/monitoring').then(r => r.json()),
    securityAlerts: () => fetch('/api/v1/dashboard/security-alerts').then(r => r.json()),
    tempList: () => fetch('/api/v1/temp-access').then(r => r.json()),
    tempGrant: (body) => fetch('/api/v1/temp-access', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    }),
    tempRevoke: (id) => fetch(`/api/v1/temp-access/${id}`, { method: 'DELETE' }),
    lockdown: () => fetch('/api/v1/dashboard/actions/lockdown', { method: 'POST' }),
    reset: () => fetch('/api/v1/dashboard/actions/reset-firewall', { method: 'POST' }),
    baseline: (sshPort) => fetch('/api/v1/dashboard/actions/apply-baseline', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sshPort })
    }),
    openPort: (port) => fetch('/api/v1/dashboard/actions/open-port', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ port })
    }),
    closePort: (port) => fetch('/api/v1/dashboard/actions/close-port', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ port })
    })
};

const BLOCKED_PORTS = new Set([22, 3306]);
let tempRows = [];

function formatRemaining(sec) {
    if (sec <= 0) return '0с';
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    if (h > 0) return `${h}ч ${m}м`;
    if (m > 0) return `${m}м ${s}с`;
    return `${s}с`;
}

function setStatusCard(id, item) {
    const card = document.getElementById(id);
    card.classList.toggle('ok', item.ok);
    card.classList.toggle('fail', !item.ok);
    card.querySelector('.status-icon').textContent = item.ok ? '✓' : '✕';
    card.querySelector('.status-value').textContent = item.label;
    const reasonEl = card.querySelector('.status-reason');
    reasonEl.textContent = item.reason || '';
}

async function refreshStatus() {
    const data = await API.status();
    setStatusCard('status-firewall', data.firewall);
    setStatusCard('status-api', data.api);
}

function renderLogs(entries) {
    const box = document.getElementById('logs-terminal');
    box.innerHTML = '';
    entries.forEach(e => {
        const line = document.createElement('div');
        line.className = 'log-line';
        const lvlClass = e.level === 'БЛОКИР' ? 'lvl-block' : (e.level === 'ПРЕДУПР' ? 'lvl-warn' : 'lvl-info');
        line.innerHTML = `<span class="log-ts">[${e.timestamp}]</span> <span class="${lvlClass}">${e.level}</span> ${escapeHtml(e.message)}`;
        box.appendChild(line);
    });
    box.scrollTop = box.scrollHeight;
}

function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

async function refreshLogs() {
    try {
        const logs = await API.logs();
        renderLogs(logs);
    } catch (e) {
        renderLogs([{ timestamp: '-', level: 'ПРЕДУПР', message: 'Не удалось загрузить журналы: ' + e.message }]);
    }
}

function renderMonitoring(data) {
    const box = document.getElementById('monitor-metrics');
    box.innerHTML = '';
    (data.metrics || []).forEach(m => {
        const item = document.createElement('div');
        item.className = 'metric-item';
        const bar = m.percent > 0
            ? `<div class="metric-bar"><div class="metric-bar-fill" style="width:${m.percent}%"></div></div>`
            : '';
        item.innerHTML = `
            <div class="metric-head">
                <span class="metric-label">${escapeHtml(m.label)}</span>
                <span class="metric-value">${escapeHtml(m.value)}</span>
            </div>
            ${bar}
        `;
        box.appendChild(item);
    });
}

async function refreshMonitoring() {
    try {
        const data = await API.monitoring();
        renderMonitoring(data);
    } catch (e) {
        document.getElementById('monitor-metrics').innerHTML =
            `<p class="placeholder-text">Ошибка загрузки: ${escapeHtml(e.message)}</p>`;
    }
}

function alertLevelClass(level) {
    if (level === 'БЛОКИР') return 'alert-block';
    if (level === 'ПРЕДУПР') return 'alert-warn';
    return 'alert-info';
}

function renderSecurityAlerts(alerts) {
    const box = document.getElementById('security-alerts');
    box.innerHTML = '';
    if (!alerts.length) {
        box.innerHTML = '<p class="placeholder-text">SSH-событий пока нет</p>';
        return;
    }
    alerts.slice().reverse().forEach(a => {
        const item = document.createElement('div');
        item.className = `alert-item ${alertLevelClass(a.level)}`;
        item.innerHTML = `
            <div class="alert-title">${escapeHtml(a.title)}</div>
            <div class="alert-msg">${escapeHtml(a.message)}</div>
            <div class="alert-meta">[${escapeHtml(a.timestamp)}] ${escapeHtml(a.sourceIp || '')}</div>
        `;
        box.appendChild(item);
    });
}

async function refreshSecurityAlerts() {
    try {
        const alerts = await API.securityAlerts();
        renderSecurityAlerts(alerts);
    } catch (e) {
        document.getElementById('security-alerts').innerHTML =
            `<p class="placeholder-text">Ошибка: ${escapeHtml(e.message)}</p>`;
    }
}

function renderTempTable() {
    const tbody = document.getElementById('temp-table-body');
    tbody.innerHTML = '';
    const now = Math.floor(Date.now() / 1000);
    tempRows.forEach(row => {
        const remaining = Math.max(0, row.expiresAt - now);
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${row.ip}</td>
            <td>${row.port}</td>
            <td data-exp="${row.expiresAt}">${formatRemaining(remaining)}</td>
            <td class="badge-active">${remaining > 0 ? 'активен' : 'истёк'}</td>
            <td><button class="btn" data-revoke="${row.id}">Отозвать</button></td>
        `;
        tbody.appendChild(tr);
    });
    tbody.querySelectorAll('[data-revoke]').forEach(btn => {
        btn.addEventListener('click', async () => {
            await API.tempRevoke(btn.dataset.revoke);
            await refreshTemp();
        });
    });
}

async function refreshTemp() {
    tempRows = await API.tempList();
    tempRows = tempRows.map(r => ({
        id: r.id,
        ip: r.ip,
        port: r.port,
        expiresAt: r.expiresAt
    }));
    renderTempTable();
}

function tickTempCountdown() {
    const now = Math.floor(Date.now() / 1000);
    document.querySelectorAll('[data-exp]').forEach(cell => {
        const exp = Number(cell.dataset.exp);
        cell.textContent = formatRemaining(Math.max(0, exp - now));
    });
}

const modal = document.getElementById('modal');
const modalTitle = document.getElementById('modal-title');
const modalMessage = document.getElementById('modal-message');
const modalExtra = document.getElementById('modal-extra');
let modalConfirmHandler = null;

function openModal(title, message, extraHtml, onConfirm) {
    modalTitle.textContent = title;
    modalMessage.textContent = message;
    modalExtra.innerHTML = extraHtml || '';
    modalConfirmHandler = onConfirm;
    modal.classList.remove('hidden');
}

function closeModal() {
    modal.classList.add('hidden');
    modalConfirmHandler = null;
}

document.getElementById('modal-cancel').addEventListener('click', closeModal);
document.getElementById('modal-confirm').addEventListener('click', async () => {
    if (modalConfirmHandler) await modalConfirmHandler();
    closeModal();
});

async function postAction(fn) {
    const res = await fn();
    const body = await res.json();
    if (!res.ok) throw new Error(body.error || 'Ошибка операции');
    alert(body.message || 'Готово');
    await refreshAll();
}

document.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', () => {
        const action = btn.dataset.action;
        if (action === 'lockdown') {
            openModal('Режим блокировки', 'Точно заблокировать сервер? Останутся только SSH, lo и ESTABLISHED.', null, () =>
                postAction(API.lockdown));
        } else if (action === 'reset') {
            openModal('Сброс firewall', 'Точно удалить все правила iptables и вернуть policy ACCEPT?', null, () =>
                postAction(API.reset));
        } else if (action === 'baseline') {
            openModal('Базовая защита', 'Укажите SSH-порт для базовой конфигурации:', '<input type="number" id="ssh-port-input" value="22" min="1" max="65535">', async () => {
                const port = Number(document.getElementById('ssh-port-input').value);
                await postAction(() => API.baseline(port));
            });
        } else if (action === 'open-port') {
            openModal('Открыть порт', 'Введите порт (22 и 3306 запрещены):', '<input type="number" id="port-input" min="1" max="65535">', async () => {
                const port = Number(document.getElementById('port-input').value);
                if (BLOCKED_PORTS.has(port)) throw new Error('Порт запрещён');
                await postAction(() => API.openPort(port));
            });
        } else if (action === 'close-port') {
            openModal('Закрыть порт', 'Введите порт (22 и 3306 запрещены):', '<input type="number" id="port-input" min="1" max="65535">', async () => {
                const port = Number(document.getElementById('port-input').value);
                if (BLOCKED_PORTS.has(port)) throw new Error('Порт запрещён');
                await postAction(() => API.closePort(port));
            });
        }
    });
});

document.getElementById('temp-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const ip = document.getElementById('temp-ip').value.trim();
    const port = Number(document.getElementById('temp-port').value);
    const ttlMinutes = Number(document.getElementById('temp-ttl').value);
    const res = await API.tempGrant({ ip, port, ttlMinutes });
    if (!res.ok) {
        const err = await res.json();
        alert(err.error || 'Ошибка');
        return;
    }
    await refreshTemp();
});

async function refreshAll() {
    await Promise.all([
        refreshStatus(),
        refreshLogs(),
        refreshTemp(),
        refreshMonitoring(),
        refreshSecurityAlerts()
    ]);
}

refreshAll();
setInterval(refreshAll, 30_000);
setInterval(tickTempCountdown, 1000);
