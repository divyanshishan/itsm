/**
 * I-ITSM Enterprise Client Application
 * Modular Vanilla JavaScript interfacing with Phase 5 Part 1 REST Controllers.
 */

// Application State
const AppState = {
    currentUser: null,
    services: [],
    categories: [],
    technicians: [],
    publicKbArticles: [],
    selectedTechIncidentCode: null,
    selectedEmailId: null,
    currentAdminView: 'incidents',
    techQueueFilterActive: true,
    notifPollingTimer: null
};

// ============================================================================
// 1. THEME MANAGEMENT (Option V3: Dual Theme System)
// ============================================================================
const THEME_KEY = 'itsm_theme';

function getStoredTheme() {
    return localStorage.getItem(THEME_KEY) || 'dark';
}

function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_KEY, theme);
    updateThemeToggleButtons(theme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    const targetTheme = currentTheme === 'dark' ? 'light' : 'dark';
    applyTheme(targetTheme);
}

function updateThemeToggleButtons(theme) {
    const isDark = theme === 'dark';
    const iconClass = isDark ? 'bi-sun-fill' : 'bi-moon-stars-fill';
    const label = isDark ? 'Light' : 'Dark';

    const headerBtn = document.getElementById('themeToggleBtn');
    if (headerBtn) {
        headerBtn.innerHTML = `<i class="bi ${iconClass} me-1"></i> <span class="theme-label d-none d-sm-inline">${label}</span>`;
        headerBtn.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
    }

    const landingBtn = document.getElementById('landingThemeToggleBtn');
    if (landingBtn) {
        landingBtn.innerHTML = `<i class="bi ${iconClass} me-1"></i> <span class="theme-label">${label}</span>`;
        landingBtn.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
    }
}

// ============================================================================
// 2. INITIALIZATION & AUTHENTICATION
// ============================================================================
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    // Initialize Theme immediately
    const currentTheme = getStoredTheme();
    applyTheme(currentTheme);

    const savedUser = sessionStorage.getItem('itsm_user');
    if (savedUser) {
        try {
            AppState.currentUser = JSON.parse(savedUser);
            renderAuthenticatedShell();
        } catch (e) {
            console.error('Failed to parse saved user session', e);
            sessionStorage.removeItem('itsm_user');
            renderLandingView();
        }
    } else {
        renderLandingView();
    }

    // Attach theme toggle listeners
    document.getElementById('themeToggleBtn')?.addEventListener('click', toggleTheme);
    document.getElementById('landingThemeToggleBtn')?.addEventListener('click', toggleTheme);

    // Landing Page Sign In modal openers
    document.getElementById('landingSignInBtn')?.addEventListener('click', openSignInModal);
    document.getElementById('heroSignInBtn')?.addEventListener('click', openSignInModal);

    // Initialize interactive hairline grid spotlight on landing view
    initLandingGridSpotlight();

    // Attach event listeners
    document.getElementById('loginForm')?.addEventListener('submit', handleLogin);
    document.getElementById('logoutBtn')?.addEventListener('click', handleLogout);
    document.getElementById('empReportForm')?.addEventListener('submit', handleEmployeeReportSubmit);
    document.getElementById('empRefreshBtn')?.addEventListener('click', loadEmployeeIncidents);

    // Auto-classification live preview listeners
    document.getElementById('reportTitle')?.addEventListener('input', updateAutoClassifyPreview);
    document.getElementById('reportDescription')?.addEventListener('input', updateAutoClassifyPreview);
    document.getElementById('reportService')?.addEventListener('change', updateAutoClassifyPreview);
    document.getElementById('empReportForm')?.addEventListener('reset', () => setTimeout(updateAutoClassifyPreview, 50));
    document.getElementById('techRefreshQueueBtn')?.addEventListener('click', () => loadTechnicianQueue());
    document.getElementById('techFilterActiveBtn')?.addEventListener('click', () => toggleTechQueueFilter(true));
    document.getElementById('techFilterAllBtn')?.addEventListener('click', () => toggleTechQueueFilter(false));
    document.getElementById('inspAddNoteBtn')?.addEventListener('click', handleTechAddNote);
    document.getElementById('inspResolveBtn')?.addEventListener('click', handleTechResolve);
    document.getElementById('adminRunSlaBtn')?.addEventListener('click', triggerSlaCheck);
    document.getElementById('applyFiltersBtn')?.addEventListener('click', applyAdminFilters);
    document.getElementById('resetFiltersBtn')?.addEventListener('click', resetAdminFilters);
    document.getElementById('submitFeedbackBtn')?.addEventListener('click', submitFeedback);
    document.getElementById('submitReopenBtn')?.addEventListener('click', submitReopen);

    // Auto-apply inline filters on select change for immediate responsiveness
    document.getElementById('filterStatus')?.addEventListener('change', applyAdminFilters);
    document.getElementById('filterPriority')?.addEventListener('change', applyAdminFilters);
    document.getElementById('filterService')?.addEventListener('change', applyAdminFilters);
    document.getElementById('filterCategory')?.addEventListener('change', applyAdminFilters);
    document.getElementById('filterTechnician')?.addEventListener('change', applyAdminFilters);

    // Live search for audit logs
    document.getElementById('adminAuditSearch')?.addEventListener('input', (e) => {
        filterAuditLogsClient(e.target.value);
    });

    // Star rating picker
    initStarRating();

    // Password visibility toggle
    initPasswordToggle();

    // Synchronize loginIdentifier and loginUserId for universal ID compatibility
    const idInput = document.getElementById('loginIdentifier');
    const userIdInput = document.getElementById('loginUserId');
    if (idInput && userIdInput) {
        idInput.addEventListener('input', () => { userIdInput.value = idInput.value; });
        userIdInput.addEventListener('input', () => { idInput.value = userIdInput.value; });
    }
}

function initPasswordToggle() {
    const toggleBtn = document.getElementById('togglePasswordBtn');
    const pwdInput = document.getElementById('loginPassword');
    const icon = document.getElementById('togglePasswordIcon');
    if (toggleBtn && pwdInput && icon) {
        toggleBtn.addEventListener('click', () => {
            const isPassword = pwdInput.getAttribute('type') === 'password';
            pwdInput.setAttribute('type', isPassword ? 'text' : 'password');
            icon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
            toggleBtn.setAttribute('title', isPassword ? 'Hide Password' : 'Show Password');
        });
    }
}

function renderLandingView() {
    if (AppState.notifPollingTimer) {
        clearInterval(AppState.notifPollingTimer);
        AppState.notifPollingTimer = null;
    }
    document.getElementById('landingView')?.classList.remove('d-none');
    document.getElementById('appShell')?.classList.add('d-none');
    document.getElementById('employeeWorkspace')?.classList.add('d-none');
    document.getElementById('technicianWorkspace')?.classList.add('d-none');
    document.getElementById('adminWorkspace')?.classList.add('d-none');
    loadPublicLandingSummary();
}

function openSignInModal() {
    hideLoginAlert();
    document.getElementById('landingGridSpotlight')?.classList.remove('active');
    const modalEl = document.getElementById('signInModal');
    if (modalEl) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }
}

// ============================================================================
// 1C. INTERACTIVE BACKGROUND HAIRLINE GRID SPOTLIGHT
// ============================================================================
function initLandingGridSpotlight() {
    const spotlight = document.getElementById('landingGridSpotlight');
    const landingView = document.getElementById('landingView');
    if (!spotlight || !landingView) return;

    function handleMouseMove(e) {
        if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            spotlight.classList.remove('active');
            return;
        }

        // Only active while landingView is visible and signInModal is closed
        if (landingView.classList.contains('d-none')) {
            spotlight.classList.remove('active');
            return;
        }
        const signInModal = document.getElementById('signInModal');
        if (signInModal && (signInModal.classList.contains('show') || document.body.classList.contains('modal-open'))) {
            spotlight.classList.remove('active');
            return;
        }

        // Strict blank-space check: if hovering any card, navbar, button, link, input, or modal, deactivate spotlight
        const isNonBlank = e.target.closest(
            '.bento-card, .landing-card, .card, .landing-navbar, nav, header, .navbar, button, a, input, select, textarea, #signInModal, .modal'
        );
        if (isNonBlank) {
            spotlight.classList.remove('active');
            return;
        }

        spotlight.style.setProperty('--gx', `${e.clientX}px`);
        spotlight.style.setProperty('--gy', `${e.clientY}px`);
        spotlight.classList.add('active');
    }

    function handleMouseLeave() {
        spotlight.classList.remove('active');
    }

    landingView.addEventListener('mousemove', handleMouseMove);
    landingView.addEventListener('mouseleave', handleMouseLeave);
    document.addEventListener('mouseleave', handleMouseLeave);
}

// ============================================================================
// 1B. PUBLIC LANDING PORTAL & TELEMETRY
// ============================================================================
async function loadPublicLandingSummary() {
    const healthEl = document.getElementById('bentoServiceHealthList');
    const trendEl = document.getElementById('bentoIncidentTrend');
    const slaEl = document.getElementById('bentoSlaGauges');

    try {
        const res = await fetch('/api/public/summary');
        const json = await res.json();
        if (res.ok && json.success && json.data) {
            renderBentoServiceHealth(json.data.services || []);
            renderBentoTrend(json.data.incidentTrend);
            renderBentoSla(json.data.slaMetrics);
        } else {
            if (healthEl) healthEl.innerHTML = '<div class="text-center text-muted fs-xs py-3">Telemetry currently unavailable.</div>';
            if (trendEl) trendEl.innerHTML = '<div class="text-center text-muted fs-xs py-3">Trend telemetry unavailable.</div>';
            if (slaEl) slaEl.innerHTML = '<div class="text-center text-muted fs-xs py-3">SLA telemetry unavailable.</div>';
        }
    } catch (err) {
        console.error('Failed to load public landing summary', err);
        if (healthEl) healthEl.innerHTML = '<div class="text-center text-muted fs-xs py-3">Offline or server starting up.</div>';
        if (trendEl) trendEl.innerHTML = '<div class="text-center text-muted fs-xs py-3">Offline or server starting up.</div>';
        if (slaEl) slaEl.innerHTML = '<div class="text-center text-muted fs-xs py-3">Offline or server starting up.</div>';
    }
}

function renderBentoServiceHealth(services) {
    const container = document.getElementById('bentoServiceHealthList');
    if (!container) return;

    if (!services || services.length === 0) {
        container.innerHTML = '<div class="text-center text-muted fs-xs py-3">No services active in catalog.</div>';
        return;
    }

    // Dynamic Y-axis scale: maxVal = Math.max(4, ...all active and resolved) rounded up to even integer
    let maxVal = 4;
    services.forEach(s => {
        if ((s.activeCount || 0) > maxVal) maxVal = s.activeCount;
        if ((s.resolvedCount || 0) > maxVal) maxVal = s.resolvedCount;
    });
    if (maxVal % 2 !== 0) {
        maxVal += 1;
    }

    const chartW = 380;
    const chartH = 220;
    const marginLeft = 28;
    const marginRight = 14;
    const marginTop = 20;
    const baselineY = 175;
    const plotHeight = baselineY - marginTop; // 155
    const plotWidth = chartW - marginLeft - marginRight; // 338

    // 3 subtle horizontal gridlines with clean integer Y-axis tick labels (0, mid, maxVal)
    const ticks = [0, maxVal / 2, maxVal];
    const gridLines = ticks.map(t => {
        const y = baselineY - (t / maxVal) * plotHeight;
        const isBaseline = (t === 0);
        return `
            <line x1="${marginLeft}" y1="${y}" x2="${chartW - marginRight}" y2="${y}" 
                  stroke="var(--border-subtle)" stroke-dasharray="${isBaseline ? 'none' : '2 3'}" stroke-width="1" />
            <text x="${marginLeft - 6}" y="${y + 3.5}" text-anchor="end" 
                  font-family="'JetBrains Mono', monospace" font-size="9" font-weight="500" fill="var(--text-muted)">${t}</text>
        `;
    }).join('');

    // Path helper for rounded top corners (rx=3) and flat baseline bottom
    const makeBarPath = (x, y, w, h, r = 3) => {
        if (h <= r) {
            return `M ${x} ${y} h ${w} v ${h} h -${w} Z`;
        }
        return `M ${x} ${y + r} Q ${x} ${y} ${x + r} ${y} L ${x + w - r} ${y} Q ${x + w} ${y} ${x + w} ${y + r} L ${x + w} ${y + h} L ${x} ${y + h} Z`;
    };

    const count = services.length;
    const colWidth = plotWidth / count;
    const barW = 20;
    const gap = 6;

    const serviceColumns = services.map((s, i) => {
        const colCenterX = marginLeft + (i + 0.5) * colWidth;
        const actX = colCenterX - (barW + gap / 2); // colCenterX - 23
        const resX = colCenterX + (gap / 2);        // colCenterX + 3

        const actVal = s.activeCount || 0;
        const resVal = s.resolvedCount || 0;

        // Heights
        const actH = actVal > 0 ? Math.max(4, (actVal / maxVal) * plotHeight) : 2;
        const actY = baselineY - actH;
        const resH = resVal > 0 ? Math.max(4, (resVal / maxVal) * plotHeight) : 2;
        const resY = baselineY - resH;

        // Bars
        const actBarSvg = actVal > 0
            ? `<path d="${makeBarPath(actX, actY, barW, actH, 3)}" fill="#F59E0B" class="service-bar bar-active"/>`
            : `<rect x="${actX}" y="${baselineY - 2}" width="${barW}" height="2" fill="#F59E0B" opacity="0.35"/>`;

        const resBarSvg = resVal > 0
            ? `<path d="${makeBarPath(resX, resY, barW, resH, 3)}" fill="#10B981" class="service-bar bar-resolved"/>`
            : `<rect x="${resX}" y="${baselineY - 2}" width="${barW}" height="2" fill="#10B981" opacity="0.35"/>`;

        // Value labels (5px above bars)
        const actLabelSvg = `<text x="${actX + barW / 2}" y="${actY - 5}" text-anchor="middle" font-family="'JetBrains Mono', monospace" font-size="10" font-weight="600" fill="var(--text-secondary)">${actVal}</text>`;
        const resLabelSvg = `<text x="${resX + barW / 2}" y="${resY - 5}" text-anchor="middle" font-family="'JetBrains Mono', monospace" font-size="10" font-weight="600" fill="var(--text-secondary)">${resVal}</text>`;

        // Health status dot and serviceCode label under baseline
        const stLower = (s.status || 'operational').toLowerCase();
        let dotColor = '#10B981';
        if (stLower === 'degraded') dotColor = '#EF4444';
        else if (stLower === 'warning') dotColor = '#F59E0B';

        const dotSvg = `<circle cx="${colCenterX - 25}" cy="196" r="3.5" fill="${dotColor}" />`;
        const codeSvg = `<text x="${colCenterX - 17}" y="199" font-family="'JetBrains Mono', monospace" font-size="10" font-weight="600" fill="var(--text-primary)">${escapeHtml(s.serviceCode)}</text>`;

        return `
            <g class="service-bar-group" data-idx="${i}">
                <rect class="bar-col-highlight" x="${colCenterX - 36}" y="10" width="72" height="192" rx="6" fill="var(--text-primary)" opacity="0"/>
                ${actBarSvg}
                ${resBarSvg}
                ${actLabelSvg}
                ${resLabelSvg}
                ${dotSvg}
                ${codeSvg}
                <rect class="service-col-hitbox" x="${colCenterX - 36}" y="0" width="72" height="220" fill="transparent" style="cursor: pointer;"/>
                <title>${escapeHtml(s.name)} (${s.serviceCode})\nStatus: ${s.status}\nActive: ${actVal} | Breached: ${s.breachedCount || 0} | Resolved: ${resVal}</title>
            </g>
        `;
    }).join('');

    // Footer summary line: {totalActive} Active • {totalResolved} Resolved • {serviceCount} Services
    const totalActive = services.reduce((acc, s) => acc + (s.activeCount || 0), 0);
    const totalResolved = services.reduce((acc, s) => acc + (s.resolvedCount || 0), 0);
    const serviceCount = services.length;

    const footerHtml = `
        <div class="service-chart-footer">
            <span>${totalActive} Active</span>
            <span class="footer-separator">&bull;</span>
            <span>${totalResolved} Resolved</span>
            <span class="footer-separator">&bull;</span>
            <span>${serviceCount} Services</span>
        </div>
    `;

    container.innerHTML = `
        <div class="position-relative w-100 flex-grow-1 d-flex flex-column justify-content-between">
            <svg viewBox="0 0 380 220" class="service-chart-svg" preserveAspectRatio="xMidYMid meet">
                ${gridLines}
                ${serviceColumns}
            </svg>
            <div id="serviceChartTooltip" class="service-chart-tooltip"></div>
            ${footerHtml}
        </div>
    `;

    // Tooltip event handlers
    const tooltip = container.querySelector('#serviceChartTooltip');
    const groups = container.querySelectorAll('.service-bar-group');

    const positionTooltip = (e) => {
        if (!tooltip) return;
        const rect = container.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const clampedX = Math.max(90, Math.min(rect.width - 90, x));
        tooltip.style.left = `${clampedX}px`;
        tooltip.style.top = `${Math.max(12, y - 12)}px`;
    };

    groups.forEach(g => {
        const idx = parseInt(g.getAttribute('data-idx'), 10);
        const s = services[idx];
        if (!s) return;

        g.addEventListener('mouseenter', (e) => {
            if (!tooltip) return;
            const stLower = (s.status || 'operational').toLowerCase();
            const badgeClass = stLower === 'degraded' ? 'bg-danger-subtle text-danger border-danger-subtle' :
                               (stLower === 'warning' ? 'bg-warning-subtle text-warning border-warning-subtle' :
                                                       'bg-success-subtle text-success border-success-subtle');

            tooltip.innerHTML = `
                <div class="tooltip-header d-flex align-items-center justify-content-between gap-3">
                    <span class="fw-bold text-truncate" style="max-width: 170px;">${escapeHtml(s.name)}</span>
                    <span class="badge border ${badgeClass}" style="font-size: 0.7rem;">${escapeHtml(s.status || 'Operational')}</span>
                </div>
                <div class="tooltip-body mt-1">
                    <span style="color: #F59E0B;"><span class="text-muted">Active:</span> <strong>${s.activeCount || 0}</strong></span>
                    <span class="text-danger"><span class="text-muted">Breached:</span> <strong>${s.breachedCount || 0}</strong></span>
                    <span style="color: #10B981;"><span class="text-muted">Resolved:</span> <strong>${s.resolvedCount || 0}</strong></span>
                </div>
            `;
            tooltip.classList.add('show');
            positionTooltip(e);
        });

        g.addEventListener('mousemove', (e) => {
            positionTooltip(e);
        });

        g.addEventListener('mouseleave', () => {
            if (tooltip) tooltip.classList.remove('show');
        });
    });
}

function renderBentoTrend(trend) {
    const container = document.getElementById('bentoIncidentTrend');
    if (!container) return;

    if (!trend || !trend.buckets || trend.buckets.length === 0) {
        container.innerHTML = '<div class="text-center text-muted fs-xs py-3">No incident volume telemetry available.</div>';
        return;
    }

    const buckets = trend.buckets;
    const count = buckets.length;

    // Scale & bounds matching Card 1
    let maxVal = 4;
    buckets.forEach(b => {
        if ((b.reportedCount || 0) > maxVal) maxVal = b.reportedCount;
        if ((b.resolvedCount || 0) > maxVal) maxVal = b.resolvedCount;
    });
    if (maxVal % 2 !== 0) {
        maxVal += 1;
    }

    const chartW = 380;
    const chartH = 220;
    const marginLeft = 28;
    const marginRight = 14;
    const marginTop = 20;
    const baselineY = 175;
    const plotHeight = baselineY - marginTop; // 155
    const plotWidth = chartW - marginLeft - marginRight; // 338

    // 3 horizontal gridlines matching Card 1
    const ticks = [0, maxVal / 2, maxVal];
    const gridLines = ticks.map(t => {
        const y = baselineY - (t / maxVal) * plotHeight;
        const isBaseline = (t === 0);
        return `
            <line x1="${marginLeft}" y1="${y}" x2="${chartW - marginRight}" y2="${y}" 
                  stroke="var(--border-subtle)" stroke-dasharray="${isBaseline ? 'none' : '2 3'}" stroke-width="1" />
            <text x="${marginLeft - 6}" y="${y + 3.5}" text-anchor="end" 
                  font-family="'JetBrains Mono', monospace" font-size="9" font-weight="500" fill="var(--text-muted)">${t}</text>
        `;
    }).join('');

    const stepX = plotWidth / (count - 1);

    // Compute coordinate points
    const reportedPoints = buckets.map((b, i) => ({
        x: marginLeft + i * stepX,
        y: baselineY - ((b.reportedCount || 0) / maxVal) * plotHeight
    }));

    const resolvedPoints = buckets.map((b, i) => ({
        x: marginLeft + i * stepX,
        y: baselineY - ((b.resolvedCount || 0) / maxVal) * plotHeight
    }));

    // Pure SVG Cubic Bezier Spline Generator
    const getSplinePath = (pts) => {
        if (pts.length === 0) return '';
        if (pts.length === 1) return `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`;
        let d = `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`;
        for (let i = 0; i < pts.length - 1; i++) {
            const p0 = i > 0 ? pts[i - 1] : pts[i];
            const p1 = pts[i];
            const p2 = pts[i + 1];
            const p3 = i < pts.length - 2 ? pts[i + 2] : p2;

            const cp1x = p1.x + (p2.x - p0.x) / 6;
            let cp1y = p1.y + (p2.y - p0.y) / 6;
            const cp2x = p2.x - (p3.x - p1.x) / 6;
            let cp2y = p2.y - (p3.y - p1.y) / 6;

            // Clamp control points to prevent overshoot past baseline or top margin
            cp1y = Math.min(baselineY, Math.max(marginTop - 6, cp1y));
            cp2y = Math.min(baselineY, Math.max(marginTop - 6, cp2y));

            d += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
        }
        return d;
    };

    const repLineD = getSplinePath(reportedPoints);
    const resLineD = getSplinePath(resolvedPoints);
    const lastIdx = count - 1;
    const repAreaD = `${repLineD} L ${reportedPoints[lastIdx].x.toFixed(1)} ${baselineY} L ${reportedPoints[0].x.toFixed(1)} ${baselineY} Z`;
    const resAreaD = `${resLineD} L ${resolvedPoints[lastIdx].x.toFixed(1)} ${baselineY} L ${resolvedPoints[0].x.toFixed(1)} ${baselineY} Z`;

    // X-axis bucket labels
    const xLabels = buckets.map((b, i) => {
        const x = marginLeft + i * stepX;
        return `<text x="${x.toFixed(1)}" y="196" text-anchor="middle" font-family="'JetBrains Mono', monospace" font-size="9" font-weight="500" fill="var(--text-muted)">${escapeHtml(b.label)}</text>`;
    }).join('');

    // Point circles
    const reportedCircles = reportedPoints.map((p, i) => `
        <circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="3.5" fill="#3B82F6" stroke="var(--bg-surface)" stroke-width="2" class="trend-point trend-point-reported" data-idx="${i}" />
    `).join('');

    const resolvedCircles = resolvedPoints.map((p, i) => `
        <circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="3.5" fill="#10B981" stroke="var(--bg-surface)" stroke-width="2" class="trend-point trend-point-resolved" data-idx="${i}" />
    `).join('');

    // Vertical slice hitboxes for smooth hover interactions
    const hitboxes = buckets.map((b, i) => {
        const x = marginLeft + i * stepX;
        const boxX = i === 0 ? (marginLeft - 10) : (x - stepX / 2);
        const boxW = i === 0 || i === count - 1 ? (stepX / 2 + 10) : stepX;
        return `<rect class="trend-col-hitbox" data-idx="${i}" x="${boxX.toFixed(1)}" y="0" width="${boxW.toFixed(1)}" height="${chartH}" fill="transparent" style="cursor: pointer;"/>`;
    }).join('');

    // Single-line footer: {totalReported} Reported • {totalResolved} Resolved • {resolutionRate}% Closed
    const footerHtml = `
        <div class="service-chart-footer">
            <span>${trend.totalReported || 0} Reported</span>
            <span class="footer-separator">&bull;</span>
            <span>${trend.totalResolved || 0} Resolved</span>
            <span class="footer-separator">&bull;</span>
            <span>${trend.resolutionRate || 0}% Closed</span>
        </div>
    `;

    container.innerHTML = `
        <div class="position-relative w-100 flex-grow-1 d-flex flex-column justify-content-between">
            <svg viewBox="0 0 380 220" class="trend-chart-svg" preserveAspectRatio="xMidYMid meet">
                <defs>
                    <linearGradient id="reportedAreaGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stop-color="#3B82F6" stop-opacity="0.28" />
                        <stop offset="100%" stop-color="#3B82F6" stop-opacity="0.01" />
                    </linearGradient>
                    <linearGradient id="resolvedAreaGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stop-color="#10B981" stop-opacity="0.22" />
                        <stop offset="100%" stop-color="#10B981" stop-opacity="0.01" />
                    </linearGradient>
                </defs>
                ${gridLines}
                <path d="${repAreaD}" fill="url(#reportedAreaGrad)" />
                <path d="${resAreaD}" fill="url(#resolvedAreaGrad)" />
                <path d="${repLineD}" fill="none" stroke="#3B82F6" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
                <path d="${resLineD}" fill="none" stroke="#10B981" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
                <line id="trendCrosshair" x1="0" y1="${marginTop}" x2="0" y2="${baselineY}" stroke="var(--border-strong)" stroke-dasharray="2 3" stroke-width="1.5" opacity="0" style="pointer-events: none; transition: opacity 0.12s ease;"/>
                ${reportedCircles}
                ${resolvedCircles}
                ${xLabels}
                ${hitboxes}
            </svg>
            <div id="trendChartTooltip" class="trend-chart-tooltip"></div>
            ${footerHtml}
        </div>
    `;

    // Tooltip & Crosshair event handlers
    const tooltip = container.querySelector('#trendChartTooltip');
    const crosshair = container.querySelector('#trendCrosshair');
    const hitboxesEls = container.querySelectorAll('.trend-col-hitbox');
    const repPts = container.querySelectorAll('.trend-point-reported');
    const resPts = container.querySelectorAll('.trend-point-resolved');

    const positionTooltip = (e) => {
        if (!tooltip) return;
        const rect = container.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const clampedX = Math.max(90, Math.min(rect.width - 90, x));
        tooltip.style.left = `${clampedX}px`;
        tooltip.style.top = `${Math.max(12, y - 12)}px`;
    };

    hitboxesEls.forEach(hb => {
        const idx = parseInt(hb.getAttribute('data-idx'), 10);
        const b = buckets[idx];
        if (!b) return;

        hb.addEventListener('mouseenter', (e) => {
            const x = marginLeft + idx * stepX;
            if (crosshair) {
                crosshair.setAttribute('x1', x.toFixed(1));
                crosshair.setAttribute('x2', x.toFixed(1));
                crosshair.setAttribute('opacity', '1');
            }
            if (repPts[idx]) repPts[idx].classList.add('active');
            if (resPts[idx]) resPts[idx].classList.add('active');

            if (tooltip) {
                tooltip.innerHTML = `
                    <div class="tooltip-header d-flex align-items-center justify-content-between gap-3">
                        <span class="fw-bold">${escapeHtml(b.label)}</span>
                        <span class="text-muted" style="font-size: 0.7rem;">2h Interval</span>
                    </div>
                    <div class="tooltip-body mt-1">
                        <span style="color: #3B82F6;"><span class="text-muted">Reported:</span> <strong>${b.reportedCount || 0}</strong></span>
                        <span style="color: #10B981;"><span class="text-muted">Resolved:</span> <strong>${b.resolvedCount || 0}</strong></span>
                    </div>
                `;
                tooltip.classList.add('show');
                positionTooltip(e);
            }
        });

        hb.addEventListener('mousemove', (e) => {
            positionTooltip(e);
        });

        hb.addEventListener('mouseleave', () => {
            if (crosshair) crosshair.setAttribute('opacity', '0');
            if (repPts[idx]) repPts[idx].classList.remove('active');
            if (resPts[idx]) resPts[idx].classList.remove('active');
            if (tooltip) tooltip.classList.remove('show');
        });
    });
}

function renderBentoCluster(cluster) {
    const container = document.getElementById('bentoClusterDiagram');
    if (!container) return;

    if (!cluster || !cluster.masterCode || cluster.totalLinkedDuplicates === 0) {
        container.innerHTML = `
            <div class="cluster-container text-center py-4">
                <div class="text-muted fs-xs mb-1"><i class="bi bi-shield-check me-1 text-success"></i> No Active Duplicate Clusters</div>
                <div class="text-secondary fs-xs">Incoming tickets are currently distinct with low cross-ticket Jaccard similarity.</div>
            </div>`;
        return;
    }

    const dupPills = (cluster.duplicateCodes || []).map(code =>
        `<span class="badge bg-subtle text-secondary border border-subtle text-mono" style="font-size: 0.82rem;"><i class="bi bi-paperclip me-1"></i>${escapeHtml(code)}</span>`
    ).join(' ');

    container.innerHTML = `
        <div class="cluster-container">
            <div class="cluster-node-master">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle fs-xs">MASTER INCIDENT</span>
                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle fs-xs text-mono">${escapeHtml(cluster.serviceCode || 'N/A')}</span>
                </div>
                <div class="d-flex justify-content-between align-items-center">
                    <span class="text-mono fw-bold text-primary" style="font-size: 0.92rem;">${escapeHtml(cluster.masterCode)}</span>
                    <span class="fs-xs text-muted">Tech: <strong class="text-secondary">${escapeHtml(cluster.assignedTechnicianName || 'Auto-Assigned')}</strong></span>
                </div>
                <div class="text-truncate fs-xs text-secondary mt-1" style="font-size: 0.85rem;" title="${escapeHtml(cluster.masterTitle || '')}">${escapeHtml(cluster.masterTitle || '')}</div>
            </div>
            <div class="cluster-connector">
                <div class="cluster-connector-line"></div>
                <span class="cluster-connector-tag"><i class="bi bi-diagram-2 me-1"></i>${cluster.totalLinkedDuplicates} Linked Duplicate${cluster.totalLinkedDuplicates > 1 ? 's' : ''} &bull; Jaccard &ge; 0.40</span>
                <div class="cluster-connector-line"></div>
            </div>
            <div class="cluster-node-dups">
                ${dupPills}
            </div>
        </div>`;
}

function renderBentoSla(sla) {
    const container = document.getElementById('bentoSlaGauges');
    if (!container) return;

    if (!sla) {
        container.innerHTML = '<div class="text-center text-muted fs-xs py-4">SLA Telemetry Unavailable</div>';
        return;
    }

    const circumference = 113.1;
    const compOffset = Math.max(0, circumference - (circumference * sla.compliancePercent / 100));
    const breachOffset = Math.max(0, circumference - (circumference * sla.breachPercent / 100));

    // Calculate compliant count strictly matching compliancePercent
    const compliantCount = Math.max(0, sla.totalTracked - sla.breachedCount);

    const onTrackPills = (sla.onTrackCodes && sla.onTrackCodes.length > 0)
        ? sla.onTrackCodes.map(c => `<span class="badge bg-success-subtle text-success border border-success-subtle text-mono" style="font-size: 0.82rem;">${escapeHtml(c)}</span>`).join(' ')
        : '<span class="text-muted fs-xs">None active</span>';

    const breachedPills = (sla.breachedCodes && sla.breachedCodes.length > 0)
        ? sla.breachedCodes.map(c => `<span class="badge bg-danger-subtle text-danger border border-danger-subtle text-mono" style="font-size: 0.82rem;">${escapeHtml(c)}</span>`).join(' ')
        : '<span class="text-muted fs-xs">Zero breaches</span>';

    container.innerHTML = `
        <div class="sla-gauge-row mb-3">
            <div class="d-flex align-items-center gap-3">
                <div class="position-relative d-inline-flex align-items-center justify-content-center" style="width: 48px; height: 48px; flex-shrink: 0;">
                    <svg class="ring-gauge-svg" width="48" height="48" viewBox="0 0 44 44">
                        <circle class="ring-gauge-bg" cx="22" cy="22" r="18"></circle>
                        <circle class="ring-gauge-fill-emerald" cx="22" cy="22" r="18" stroke-dasharray="113.1" stroke-dashoffset="${compOffset.toFixed(1)}"></circle>
                    </svg>
                    <span class="position-absolute fw-bold text-mono" style="font-size: 0.72rem;">${sla.compliancePercent}%</span>
                </div>
                <div class="flex-grow-1 overflow-hidden min-w-0">
                    <div class="d-flex flex-column gap-1 mb-2">
                        <span class="fw-semibold fs-xs">On-Track Compliance</span>
                        <span class="fs-xs text-muted text-mono">${compliantCount} / ${sla.totalTracked} compliant (${sla.onTrackCount} on-track, ${sla.atRiskCount} at-risk)</span>
                    </div>
                    <div class="sla-progress-track">
                        <div class="sla-progress-fill-emerald" style="width: ${sla.compliancePercent}%;"></div>
                    </div>
                    <div class="d-flex align-items-center gap-1 mt-2">
                        <span class="fs-xs text-muted me-1">Sample:</span>
                        ${onTrackPills}
                    </div>
                </div>
            </div>
        </div>
        <div class="sla-gauge-row">
            <div class="d-flex align-items-center gap-3">
                <div class="position-relative d-inline-flex align-items-center justify-content-center" style="width: 48px; height: 48px; flex-shrink: 0;">
                    <svg class="ring-gauge-svg" width="48" height="48" viewBox="0 0 44 44">
                        <circle class="ring-gauge-bg" cx="22" cy="22" r="18"></circle>
                        <circle class="ring-gauge-fill-crimson" cx="22" cy="22" r="18" stroke-dasharray="113.1" stroke-dashoffset="${breachOffset.toFixed(1)}"></circle>
                    </svg>
                    <span class="position-absolute fw-bold text-mono" style="font-size: 0.72rem;">${sla.breachPercent}%</span>
                </div>
                <div class="flex-grow-1 overflow-hidden min-w-0">
                    <div class="d-flex flex-column gap-1 mb-2">
                        <span class="fw-semibold fs-xs">Breach & Risk Alerts</span>
                        <span class="fs-xs text-muted text-mono text-nowrap">${sla.breachedCount} / ${sla.totalTracked} breached &bull; ${sla.atRiskCount} at risk</span>
                    </div>
                    <div class="sla-progress-track">
                        <div class="sla-progress-fill-crimson" style="width: ${sla.breachPercent}%;"></div>
                    </div>
                    <div class="d-flex align-items-center gap-1 mt-2">
                        <span class="fs-xs text-muted me-1">Sample:</span>
                        ${breachedPills}
                    </div>
                </div>
            </div>
        </div>`;
}

function fillLoginForm(identifier, password) {
    const userEl = document.getElementById('loginUserId') || document.getElementById('loginIdentifier');
    if (userEl) userEl.value = identifier;
    const idEl = document.getElementById('loginIdentifier');
    if (idEl) idEl.value = identifier;
    const pwdEl = document.getElementById('loginPassword');
    if (pwdEl) pwdEl.value = password;
    hideLoginAlert();
}

async function quickLogin(identifier, password) {
    fillLoginForm(identifier, password);
    await handleLogin();
}

async function handleLogin(e) {
    if (e) e.preventDefault();
    hideLoginAlert();

    const idEl = document.getElementById('loginUserId') || document.getElementById('loginIdentifier');
    const identifier = idEl ? idEl.value.trim() : '';
    const password = document.getElementById('loginPassword').value.trim();

    if (!identifier || !password) {
        showLoginAlert('Please enter both User ID / Email and password.');
        return;
    }

    const submitBtn = document.getElementById('loginSubmitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Signing In...';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userIdCode: identifier, password: password })
        });

        const result = await response.json();

        if (response.ok && result.success && result.data) {
            const signInModalEl = document.getElementById('signInModal');
            if (signInModalEl) {
                const modalInstance = bootstrap.Modal.getInstance(signInModalEl);
                if (modalInstance) {
                    modalInstance.hide();
                }
            }
            AppState.currentUser = result.data;
            sessionStorage.setItem('itsm_user', JSON.stringify(result.data));
            renderAuthenticatedShell();
        } else {
            showLoginAlert(result.message || 'Invalid user ID code or password.');
        }
    } catch (err) {
        console.error('Login error', err);
        showLoginAlert('Unable to connect to I-ITSM server. Please ensure Spring Boot is running.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="bi bi-box-arrow-in-right me-1"></i> Sign In to I-ITSM';
    }
}

async function handleLogout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch (e) {
        // Ignore network errors on logout
    }
    AppState.currentUser = null;
    sessionStorage.removeItem('itsm_user');
    renderLandingView();
}

function showLoginAlert(msg) {
    const alertBox = document.getElementById('loginAlert');
    if (alertBox) {
        alertBox.textContent = msg;
        alertBox.classList.remove('d-none');
    }
}

function hideLoginAlert() {
    const alertBox = document.getElementById('loginAlert');
    if (alertBox) {
        alertBox.classList.add('d-none');
        alertBox.textContent = '';
    }
}

// ============================================================================
// 2. AUTHENTICATED SHELL & ROLE DISPATCHING
// ============================================================================
async function renderAuthenticatedShell() {
    const user = AppState.currentUser;
    if (!user) return;

    const signInModalEl = document.getElementById('signInModal');
    if (signInModalEl) {
        const modalInstance = bootstrap.Modal.getInstance(signInModalEl);
        if (modalInstance) {
            modalInstance.hide();
        }
    }

    document.getElementById('landingView')?.classList.add('d-none');
    document.getElementById('appShell')?.classList.remove('d-none');

    // Header metadata
    document.getElementById('headerUserName').textContent = user.name;
    document.getElementById('headerUserCode').textContent = `(${user.userIdCode})`;

    const roleBadge = document.getElementById('headerRoleBadge');
    roleBadge.textContent = user.role;
    roleBadge.className = 'badge fs-xs ' + (
        user.role === 'EMPLOYEE' ? 'bg-primary' :
        user.role === 'TECHNICIAN' ? 'bg-info text-dark' : 'bg-danger'
    );

    // Load master lookups
    await loadMasterLookups();

    // Start notification polling
    loadNotifications();
    if (!AppState.notifPollingTimer) {
        AppState.notifPollingTimer = setInterval(loadNotifications, 20000);
    }

    // Role-specific workspace rendering
    const empWs = document.getElementById('employeeWorkspace');
    const techWs = document.getElementById('technicianWorkspace');
    const adminWs = document.getElementById('adminWorkspace');

    empWs.classList.add('d-none');
    techWs.classList.add('d-none');
    adminWs.classList.add('d-none');

    if (user.role === 'EMPLOYEE') {
        empWs.classList.remove('d-none');
        initEmployeeWorkspace();
    } else if (user.role === 'TECHNICIAN') {
        techWs.classList.remove('d-none');
        initTechnicianWorkspace();
    } else if (user.role === 'ADMINISTRATOR') {
        adminWs.classList.remove('d-none');
        initAdminWorkspace();
    }
}

async function loadMasterLookups() {
    try {
        const [servicesRes, categoriesRes, techsRes] = await Promise.all([
            fetch('/api/lookup/services').then(r => r.json()),
            fetch('/api/lookup/categories').then(r => r.json()),
            fetch('/api/lookup/technicians').then(r => r.json())
        ]);

        if (servicesRes.success) AppState.services = servicesRes.data;
        if (categoriesRes.success) AppState.categories = categoriesRes.data;
        if (techsRes.success) AppState.technicians = techsRes.data;
    } catch (e) {
        console.error('Failed to load master lookups', e);
    }
}

// ============================================================================
// 3. EMPLOYEE WORKSPACE & AUTO-CLASSIFICATION
// ============================================================================
const HIGH_SCOPE_KEYWORDS = [
    'production', 'outage', 'all users', 'entire', 'customers',
    'company-wide', 'cluster', 'checkout', 'breach', 'data loss'
];

const LOCALIZED_SCOPE_KEYWORDS = [
    'my laptop', 'docking', 'monitor', 'flicker', 'conference room',
    'csv', 'typo', 'cosmetic'
];

const CRITICAL_URGENCY_KEYWORDS = [
    'outage', 'down', 'crash', 'blocked', 'blocking', 'unable to',
    'failed', 'lockout', 'security', '502', 'connection reset', 'timeout'
];

const HIGH_URGENCY_KEYWORDS = [
    'dropping', 'disconnect', 'error', 'eviction', 'deadlock',
    'failing', 'urgent', 'not working'
];

const MEDIUM_URGENCY_KEYWORDS = [
    'slow', 'latency', 'intermittent', 'degraded', 'warning', 'rebuild', 'delay'
];

function clientClassifyImpact(title, description, service) {
    const text = ((title || '') + ' ' + (description || '')).toLowerCase();

    // 1. Initial baseline from service business criticality
    let baseline = 'MEDIUM';
    let isTierOneOrHigh = false;

    if (service) {
        const crit = service.criticality ? String(service.criticality).toUpperCase() : '';
        const code = service.serviceCode ? String(service.serviceCode).toUpperCase() : '';

        if (crit === 'MISSION_CRITICAL' || crit === 'CRITICAL' || code === 'SVC-PAY' || code === 'SVC-AUTH') {
            baseline = 'HIGH';
            isTierOneOrHigh = true;
        } else if (crit === 'HIGH' || code === 'SVC-NET' || code === 'SVC-CLOUD') {
            baseline = 'HIGH';
            isTierOneOrHigh = true;
        } else if (crit === 'MEDIUM') {
            baseline = 'MEDIUM';
        } else {
            baseline = 'LOW';
        }
    }

    // 2. Scan scope modifiers
    const hasHighScope = HIGH_SCOPE_KEYWORDS.some(kw => text.includes(kw));
    const hasLocalizedScope = LOCALIZED_SCOPE_KEYWORDS.some(kw => text.includes(kw));

    if (hasHighScope) {
        return isTierOneOrHigh ? 'CRITICAL' : 'HIGH';
    }

    if (hasLocalizedScope) {
        return (baseline === 'LOW' || text.includes('flicker') || text.includes('cosmetic') ||
                text.includes('typo') || text.includes('my laptop') || text.includes('monitor') ||
                text.includes('docking')) ? 'LOW' : 'MEDIUM';
    }

    return baseline;
}

function clientClassifyUrgency(title, description) {
    const text = ((title || '') + ' ' + (description || '')).toLowerCase();

    if (CRITICAL_URGENCY_KEYWORDS.some(kw => text.includes(kw))) {
        return 'CRITICAL';
    }
    if (HIGH_URGENCY_KEYWORDS.some(kw => text.includes(kw))) {
        return 'HIGH';
    }
    if (MEDIUM_URGENCY_KEYWORDS.some(kw => text.includes(kw))) {
        return 'MEDIUM';
    }
    return 'LOW';
}

function clientEstimatePriority(impact, urgency, service) {
    const weightMap = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4, MISSION_CRITICAL: 4 };
    const impactWeight = weightMap[impact] || 2;
    const urgencyWeight = weightMap[urgency] || 1;
    let critWeight = 2;
    if (service && service.criticality) {
        critWeight = weightMap[String(service.criticality).toUpperCase()] || 2;
    }

    const rawScore = (impactWeight * 0.40) + (urgencyWeight * 0.30) + (critWeight * 0.30);
    const score = Math.round(rawScore * 100) / 100;

    let priority = 'P4_LOW';
    if (score >= 3.4) {
        priority = 'P1_CRITICAL';
    } else if (score >= 2.6) {
        priority = 'P2_HIGH';
    } else if (score >= 1.8) {
        priority = 'P3_MEDIUM';
    }

    return { score, priority };
}

function updateAutoClassifyPreview() {
    const hintEl = document.getElementById('autoClassifyHint');
    if (!hintEl) return;

    const titleInput = document.getElementById('reportTitle');
    const descInput = document.getElementById('reportDescription');
    const serviceSelect = document.getElementById('reportService');

    const title = titleInput ? titleInput.value.trim() : '';
    const desc = descInput ? descInput.value.trim() : '';
    const serviceId = serviceSelect ? serviceSelect.value : '';

    if (!serviceId && !title && !desc) {
        hintEl.innerHTML = '<i class="bi bi-shield-check text-primary me-1"></i> Impact and Urgency are automatically determined from service business criticality and symptom analysis.';
        return;
    }

    const selectedService = AppState.services.find(s => s.id === parseInt(serviceId));
    const impact = clientClassifyImpact(title, desc, selectedService);
    const urgency = clientClassifyUrgency(title, desc);
    const { score, priority } = clientEstimatePriority(impact, urgency, selectedService);

    hintEl.innerHTML = `
        <div class="d-flex align-items-center flex-wrap gap-2 py-1">
            <span class="fw-semibold text-secondary"><i class="bi bi-shield-check text-success me-1"></i> Auto-Classification:</span>
            <span>Impact: <span class="badge bg-light text-dark border font-mono">${impact}</span></span>
            <span>Urgency: <span class="badge bg-light text-dark border font-mono">${urgency}</span></span>
            <span>Estimated Priority: ${getPriorityBadge(priority)} <span class="text-muted font-mono fs-xs">(${score.toFixed(2)})</span></span>
        </div>
    `;
}

function initEmployeeWorkspace() {
    populateReportFormDropdowns();
    updateAutoClassifyPreview();
    loadEmployeeIncidents();
}

function populateReportFormDropdowns() {
    const serviceSelect = document.getElementById('reportService');
    const categorySelect = document.getElementById('reportCategory');

    if (serviceSelect) {
        serviceSelect.innerHTML = '<option value="">Select Affected Service...</option>' +
            AppState.services.map(s => `<option value="${s.id}">${s.serviceName} (${s.serviceCode} - ${s.criticality})</option>`).join('');
    }

    if (categorySelect) {
        categorySelect.innerHTML = '<option value="">Use Service Default Category</option>' +
            AppState.categories.map(c => `<option value="${c.id}">${c.name} — ${c.description || ''}</option>`).join('');
    }
}

async function loadEmployeeIncidents() {
    const tbody = document.getElementById('empIncidentsTbody');
    tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading your incidents...</td></tr>';

    try {
        const res = await fetch(`/api/incidents?reporterId=${AppState.currentUser.id}`);
        const result = await res.json();

        if (result.success && result.data) {
            renderEmployeeIncidentsTable(result.data);
        } else {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted">No incidents found.</td></tr>';
        }
    } catch (e) {
        console.error('Error fetching employee incidents', e);
        tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-danger">Error loading incidents from server.</td></tr>';
    }
}

function renderEmployeeIncidentsTable(incidents) {
    const tbody = document.getElementById('empIncidentsTbody');
    if (!incidents || incidents.length === 0) {
        tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted">You have not submitted any incidents yet. Click "Report New Incident" to submit one.</td></tr>';
        return;
    }

    tbody.innerHTML = incidents.map(inc => {
        const isClosed = inc.status === 'CLOSED';
        const serviceName = inc.affectedService ? inc.affectedService.serviceName : 'N/A';
        const categoryName = inc.category ? inc.category.name : 'N/A';
        const techName = inc.assignedTechnician ? `${inc.assignedTechnician.name} (${inc.assignedTechnician.userIdCode})` : '<span class="text-muted fst-italic">Unassigned</span>';
        const dupBadge = inc.duplicateOfIncidentCode
            ? `<span class="badge-duplicate-cluster">Dup: ${inc.duplicateOfIncidentCode}</span>`
            : '<span class="text-muted fs-xs">-</span>';

        let actionButtons = '';
        if (isClosed) {
            actionButtons = `
                <div class="btn-group btn-group-sm">
                    <button class="btn btn-outline-warning btn-sm py-0 px-2 fs-xs" onclick="openFeedbackModal('${inc.incidentCode}', ${inc.feedbackRating || 5}, '${escapeJs(inc.customerFeedback || '')}')">
                        <i class="bi bi-star me-1"></i> ${inc.feedbackRating ? 'Rated ' + inc.feedbackRating + '/5' : 'Rate & Feedback'}
                    </button>
                    <button class="btn btn-outline-danger btn-sm py-0 px-2 fs-xs" onclick="openReopenModal('${inc.incidentCode}')">
                        <i class="bi bi-arrow-repeat me-1"></i> Reopen
                    </button>
                </div>
            `;
        } else {
            actionButtons = '<span class="badge bg-light text-secondary border fs-xs">In Progress</span>';
        }

        const resolutionSummary = inc.resolutionDetails
            ? `<span title="${escapeHtml(inc.resolutionDetails)}">${truncate(inc.resolutionDetails, 40)}</span>`
            : '<span class="text-muted fs-xs">-</span>';

        return `
            <tr>
                <td class="col-nowrap"><span class="code-pill">${inc.incidentCode}</span></td>
                <td class="incident-title-cell">${escapeHtml(inc.title)}</td>
                <td class="col-nowrap"><span class="service-pill">${inc.affectedService ? inc.affectedService.serviceCode : serviceName}</span></td>
                <td class="col-nowrap"><span class="badge bg-light text-secondary border fs-xs">${categoryName}</span></td>
                <td class="col-nowrap">${getPriorityBadge(inc.priority)}</td>
                <td class="col-nowrap">${getStatusBadge(inc.status)}</td>
                <td class="col-nowrap">${dupBadge}</td>
                <td class="col-nowrap fs-xs">${techName}</td>
                <td class="col-nowrap">${getSlaStatusBadge(inc.slaStatus, inc.slaViolated)}</td>
                <td class="fs-xs">${resolutionSummary}</td>
                <td class="text-end col-nowrap">${actionButtons}</td>
            </tr>
        `;
    }).join('');
}

async function handleEmployeeReportSubmit(e) {
    e.preventDefault();
    const successBox = document.getElementById('empReportSuccessBox');
    const errorBox = document.getElementById('empReportErrorBox');
    const submitBtn = document.getElementById('empSubmitReportBtn');

    successBox.classList.add('d-none');
    errorBox.classList.add('d-none');

    const title = document.getElementById('reportTitle').value.trim();
    const serviceId = document.getElementById('reportService').value;
    const categoryId = document.getElementById('reportCategory').value || null;
    const description = document.getElementById('reportDescription').value.trim();

    if (!title || !serviceId || !description) {
        errorBox.textContent = 'Please fill out all required fields marked with *.';
        errorBox.classList.remove('d-none');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Processing & Auto-Assigning...';

    const selectedService = AppState.services.find(s => s.id === parseInt(serviceId));
    const selectedCategory = categoryId ? AppState.categories.find(c => c.id === parseInt(categoryId)) : null;

    const classifiedImpact = clientClassifyImpact(title, description, selectedService);
    const classifiedUrgency = clientClassifyUrgency(title, description);

    try {
        const payload = {
            title,
            description,
            reporterId: AppState.currentUser.id,
            reporterIdCode: AppState.currentUser.userIdCode,
            serviceId: parseInt(serviceId),
            serviceCode: selectedService ? selectedService.serviceCode : null,
            categoryId: categoryId ? parseInt(categoryId) : null,
            categoryCode: selectedCategory ? selectedCategory.name : null,
            impact: classifiedImpact,
            urgency: classifiedUrgency
        };

        const res = await fetch('/api/incidents/report', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const result = await res.json();

        if (res.ok && result.success && result.data) {
            const inc = result.data;
            const techName = inc.assignedTechnician ? `${inc.assignedTechnician.name} (${inc.assignedTechnician.userIdCode})` : 'Unassigned';
            const isDup = !!inc.duplicateOfIncidentCode;

            let dupMessage = isDup
                ? `<div class="mt-2 p-2 bg-warning bg-opacity-25 rounded border border-warning">
                       <i class="bi bi-diagram-3-fill me-1"></i> <strong>Intelligent Duplicate Cluster Detection:</strong>
                       This ticket was identified as a duplicate of active incident <strong>${inc.duplicateOfIncidentCode}</strong>.
                       Both tickets have been co-assigned to <strong>${techName}</strong> and will be resolved together!
                   </div>`
                : `<div class="mt-2 text-muted">Assigned via SmartCascadingAssignmentStrategy (Skill & Load Priority) to <strong>${techName}</strong>.</div>`;

            successBox.innerHTML = `
                <div class="d-flex align-items-center mb-1">
                    <i class="bi bi-check-circle-fill text-success fs-4 me-2"></i>
                    <div>
                        <strong class="d-block">Incident Reported Successfully! Ticket Code: <span class="font-mono">${inc.incidentCode}</span></strong>
                        <span class="fs-xs text-muted">Auto-Classified Priority: <strong>${getPriorityBadge(inc.priority)}</strong> (Score: ${inc.priorityScore}) | Assigned Technician: <strong>${techName}</strong></span>
                    </div>
                </div>
                ${dupMessage}
                <div class="mt-2">
                    <button class="btn btn-sm btn-outline-success" onclick="switchToMyIncidentsTab()">
                        <i class="bi bi-arrow-left me-1"></i> View in My Submitted Incidents
                    </button>
                </div>
            `;
            successBox.classList.remove('d-none');
            document.getElementById('empReportForm').reset();
            updateAutoClassifyPreview();
            loadEmployeeIncidents();
        } else {
            errorBox.textContent = result.message || 'Failed to submit incident.';
            errorBox.classList.remove('d-none');
        }
    } catch (err) {
        console.error('Error reporting incident', err);
        errorBox.textContent = 'Server communication error while submitting incident.';
        errorBox.classList.remove('d-none');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="bi bi-send me-1"></i> Submit & Auto-Assign';
    }
}

function switchToMyIncidentsTab() {
    const tabBtn = document.getElementById('empTabIncidentsBtn');
    if (tabBtn) {
        bootstrap.Tab.getOrCreateInstance(tabBtn).show();
        loadEmployeeIncidents();
    }
}

// ============================================================================
// 4. TECHNICIAN WORKSPACE (Split-Screen Master-Detail)
// ============================================================================
function initTechnicianWorkspace() {
    loadTechnicianProfile();
    loadTechnicianQueue(AppState.techQueueFilterActive);
}

async function loadTechnicianProfile() {
    try {
        const res = await fetch('/api/lookup/technicians');
        const result = await res.json();
        if (result.success && result.data) {
            const currentTech = result.data.find(t => t.userIdCode === AppState.currentUser.userIdCode);
            if (currentTech) {
                renderTechnicianSummary(currentTech);
            }
        }
    } catch (e) {
        console.error('Failed to load technician profile', e);
    }
}

function renderTechnicianSummary(tech) {
    document.getElementById('techSummaryName').textContent = tech.name;
    document.getElementById('techSummaryCode').textContent = `(${tech.userIdCode})`;

    const skillsContainer = document.getElementById('techSkillsBadges');
    if (tech.skills && tech.skills.length > 0) {
        skillsContainer.innerHTML = Array.from(tech.skills)
            .map(s => `<span class="badge bg-secondary fs-xs">${s}</span>`).join('');
    } else {
        skillsContainer.innerHTML = '<span class="badge bg-light text-muted border fs-xs">General Support</span>';
    }

    document.getElementById('techExpYears').textContent = `${tech.experienceYears || 0} Years Exp`;

    const currentLoad = tech.currentWorkload || 0;
    const maxLoad = tech.maxWorkload || 5;
    const percentage = Math.min(100, Math.round((currentLoad / maxLoad) * 100));

    document.getElementById('techWorkloadRatio').textContent = `${currentLoad} / ${maxLoad}`;
    const bar = document.getElementById('techWorkloadBar');
    bar.style.width = `${percentage}%`;
    bar.className = 'progress-bar ' + (percentage >= 100 ? 'bg-danger' : percentage >= 80 ? 'bg-warning' : 'bg-success');

    const availBadge = document.getElementById('techAvailabilityBadge');
    if (currentLoad >= maxLoad) {
        availBadge.textContent = 'At Capacity';
        availBadge.className = 'badge bg-danger py-2 px-3 fs-xs';
    } else {
        availBadge.textContent = 'Available';
        availBadge.className = 'badge bg-success py-2 px-3 fs-xs';
    }
}

function toggleTechQueueFilter(activeOnly) {
    AppState.techQueueFilterActive = activeOnly;
    document.getElementById('techFilterActiveBtn').classList.toggle('active', activeOnly);
    document.getElementById('techFilterAllBtn').classList.toggle('active', !activeOnly);
    loadTechnicianQueue(activeOnly);
}

async function loadTechnicianQueue(activeOnly = AppState.techQueueFilterActive) {
    const tbody = document.getElementById('techQueueTbody');
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading assigned tickets...</td></tr>';

    try {
        const userCode = AppState.currentUser.userIdCode;
        let url = `/api/incidents?technicianCode=${userCode}`;
        if (activeOnly) {
            url += '&status=IN_PROGRESS';
        }

        const res = await fetch(url);
        const result = await res.json();

        if (result.success && result.data) {
            renderTechnicianQueueTable(result.data);
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No tickets found in your queue.</td></tr>';
        }
    } catch (e) {
        console.error('Failed to load technician queue', e);
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-danger">Error loading queue.</td></tr>';
    }
}

function renderTechnicianQueueTable(incidents) {
    const tbody = document.getElementById('techQueueTbody');
    if (!incidents || incidents.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No assigned tickets in this view.</td></tr>';
        return;
    }

    tbody.innerHTML = incidents.map(inc => {
        const isSelected = AppState.selectedTechIncidentCode === inc.incidentCode;
        const dupBadge = inc.duplicateOfIncidentCode
            ? `<span class="badge-duplicate-cluster">Dup: ${inc.duplicateOfIncidentCode}</span>`
            : '<span class="text-muted fs-xs">-</span>';

        return `
            <tr class="${isSelected ? 'table-active' : ''}" onclick="selectTechnicianIncident('${inc.incidentCode}')">
                <td class="col-nowrap"><span class="code-pill">${inc.incidentCode}</span></td>
                <td class="incident-title-cell">${escapeHtml(inc.title)}</td>
                <td class="col-nowrap"><span class="service-pill">${inc.affectedService ? inc.affectedService.serviceCode : 'N/A'}</span></td>
                <td class="col-nowrap">${getPriorityBadge(inc.priority)}</td>
                <td class="col-nowrap">${getStatusBadge(inc.status)}</td>
                <td class="col-nowrap">${dupBadge}</td>
                <td class="col-nowrap">${getSlaStatusBadge(inc.slaStatus, inc.slaViolated)}</td>
            </tr>
        `;
    }).join('');
}

async function selectTechnicianIncident(incidentCode) {
    AppState.selectedTechIncidentCode = incidentCode;

    // Highlight row
    document.querySelectorAll('#techQueueTbody tr').forEach(tr => {
        if (tr.innerText.includes(incidentCode)) {
            tr.classList.add('table-active');
        } else {
            tr.classList.remove('table-active');
        }
    });

    document.getElementById('techInspectorEmpty').classList.add('d-none');
    const content = document.getElementById('techInspectorContent');
    content.classList.remove('d-none');

    try {
        const res = await fetch(`/api/incidents/${incidentCode}`);
        const result = await res.json();

        if (result.success && result.data) {
            renderTechnicianInspector(result.data);
            loadHistoricalResolutionsForInspector(incidentCode);
        }
    } catch (e) {
        console.error('Failed to load incident details', e);
    }
}

function renderTechnicianInspector(inc) {
    document.getElementById('inspCode').textContent = inc.incidentCode;
    document.getElementById('inspStatusBadge').innerHTML = getStatusBadge(inc.status);
    document.getElementById('inspPriorityBadge').innerHTML = getPriorityBadge(inc.priority);
    document.getElementById('inspSlaBadge').innerHTML = getSlaStatusBadge(inc.slaStatus, inc.slaViolated);

    document.getElementById('inspTitle').textContent = inc.title;
    document.getElementById('inspDescription').textContent = inc.description || 'No description provided.';

    document.getElementById('inspReporter').textContent = inc.reporter ? `${inc.reporter.name} (${inc.reporter.department || 'N/A'})` : 'N/A';
    document.getElementById('inspService').textContent = inc.affectedService ? `${inc.affectedService.serviceName} [${inc.affectedService.criticality}]` : 'N/A';
    document.getElementById('inspCategory').textContent = inc.category ? inc.category.name : 'N/A';
    document.getElementById('inspResolutionDeadline').textContent = inc.resolutionDeadline ? formatDateTime(inc.resolutionDeadline) : 'N/A';

    // Duplicate cluster warning
    const dupBox = document.getElementById('inspDuplicateWarning');
    if (inc.duplicateOfIncidentCode) {
        document.getElementById('inspMasterCode').textContent = inc.duplicateOfIncidentCode;
        dupBox.classList.remove('d-none');
    } else {
        dupBox.classList.add('d-none');
    }

    // Notes stream
    const notesStream = document.getElementById('inspNotesStream');
    if (inc.investigationNotes && inc.investigationNotes.trim()) {
        notesStream.textContent = inc.investigationNotes;
    } else {
        notesStream.textContent = 'No investigation notes recorded yet.';
    }

    // Resolution box pre-fill if closed
    const resInput = document.getElementById('inspResolutionDetails');
    const resolveBtn = document.getElementById('inspResolveBtn');

    if (inc.status === 'CLOSED') {
        resInput.value = inc.resolutionDetails || '';
        resInput.disabled = true;
        resolveBtn.disabled = true;
        resolveBtn.innerHTML = '<i class="bi bi-check-all me-1"></i> Incident Already Resolved & Closed';
    } else {
        resInput.value = '';
        resInput.disabled = false;
        resolveBtn.disabled = false;
        resolveBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Resolve & Close Incident (Cascade Duplicates)';
    }
}

async function handleTechAddNote() {
    const code = AppState.selectedTechIncidentCode;
    const input = document.getElementById('inspNoteInput');
    const noteText = input.value.trim();

    if (!code || !noteText) return;

    try {
        const res = await fetch(`/api/incidents/${code}/notes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                actorUserCode: AppState.currentUser.userIdCode,
                note: noteText
            })
        });

        const result = await res.json();
        if (result.success && result.data) {
            input.value = '';
            document.getElementById('inspNotesStream').textContent = result.data.investigationNotes;
            showToast('Investigation note appended.');
        } else {
            showToast(result.message || 'Failed to append note', 'danger');
        }
    } catch (e) {
        console.error('Failed to append note', e);
        showToast('Error appending note', 'danger');
    }
}

async function loadHistoricalResolutionsForInspector(incidentCode) {
    const tbody = document.getElementById('inspHistoricalTbody');
    tbody.innerHTML = '<tr><td colspan="3" class="text-center py-2 text-muted fs-xs"><span class="spinner-border spinner-border-sm me-1"></span>Grounded historical search...</td></tr>';

    try {
        const res = await fetch(`/api/intelligence/incidents/${incidentCode}/suggested-resolutions`);
        const result = await res.json();

        if (result.success && result.data) {
            renderHistoricalResolutionsList(result.data);
        } else {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center py-2 text-muted fs-xs">No historical resolutions available.</td></tr>';
        }
    } catch (e) {
        console.error('Failed to load historical resolutions', e);
        tbody.innerHTML = '<tr><td colspan="3" class="text-center py-2 text-muted fs-xs">Unable to load recommendations.</td></tr>';
    }
}

function renderHistoricalResolutionsList(suggestions) {
    const tbody = document.getElementById('inspHistoricalTbody');
    const countBadge = document.getElementById('inspHistoricalCount');
    countBadge.textContent = `${suggestions.length} available`;

    if (!suggestions || suggestions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center py-2 text-muted fs-xs">No matching historical resolutions in repository.</td></tr>';
        return;
    }

    tbody.innerHTML = suggestions.map(dto => {
        const score = Math.round(dto.similarityScore);
        const scoreBadge = score >= 80 ? 'bg-success' : score >= 60 ? 'bg-primary' : 'bg-secondary';
        const escapedRes = escapeJs(dto.resolutionDetails || '');

        return `
            <tr>
                <td style="width: 80px;">
                    <span class="badge ${scoreBadge} fs-xs">${score}% Match</span>
                    <span class="font-mono fs-xs text-muted d-block">${dto.historicalIncidentCode}</span>
                </td>
                <td class="fs-xs">
                    <strong class="d-block text-dark">${escapeHtml(dto.title)}</strong>
                    <span class="text-muted">${escapeHtml(truncate(dto.resolutionDetails, 60))}</span>
                </td>
                <td class="text-end" style="width: 140px;">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-secondary py-0 px-1 fs-xs" title="Copy solution text into resolution textarea" onclick="copyHistoricalResolution('${escapedRes}')">
                            <i class="bi bi-clipboard me-1"></i> Copy
                        </button>
                        <button class="btn btn-outline-success py-0 px-1 fs-xs" title="Apply this exact historical solution and close immediately" onclick="applyHistoricalResolution('${dto.historicalIncidentCode}')">
                            <i class="bi bi-lightning-fill"></i> Apply & Close
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function copyHistoricalResolution(text) {
    const textarea = document.getElementById('inspResolutionDetails');
    textarea.value = text;
    textarea.focus();
    showToast('Historical resolution text copied to resolution box.');
}

async function applyHistoricalResolution(historicalCode) {
    const code = AppState.selectedTechIncidentCode;
    if (!code || !confirm(`Apply historical resolution from ${historicalCode} and immediately close incident ${code}?`)) return;

    try {
        const res = await fetch(`/api/intelligence/incidents/${code}/apply-historical-resolution/${historicalCode}?actorUserCode=${AppState.currentUser.userIdCode}`, {
            method: 'POST'
        });

        const result = await res.json();
        if (result.success && result.data) {
            showToast(`Incident ${code} successfully resolved and closed via historical solution!`);
            loadTechnicianProfile();
            loadTechnicianQueue();
            selectTechnicianIncident(code);
        } else {
            showToast(result.message || 'Failed to apply historical resolution', 'danger');
        }
    } catch (e) {
        console.error('Error applying historical resolution', e);
        showToast('Error applying historical resolution', 'danger');
    }
}

async function handleTechResolve() {
    const code = AppState.selectedTechIncidentCode;
    const details = document.getElementById('inspResolutionDetails').value.trim();

    if (!code) return;
    if (!details) {
        showToast('Please document resolution details before closing.', 'warning');
        return;
    }

    try {
        const res = await fetch(`/api/incidents/${code}/resolve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                actorUserCode: AppState.currentUser.userIdCode,
                resolutionDetails: details
            })
        });

        const result = await res.json();
        if (result.success && result.data) {
            showToast(`Incident ${code} resolved and closed! Any linked duplicate cluster tickets closed.`);
            loadTechnicianProfile();
            loadTechnicianQueue();
            selectTechnicianIncident(code);
        } else {
            showToast(result.message || 'Failed to resolve incident', 'danger');
        }
    } catch (e) {
        console.error('Error resolving incident', e);
        showToast('Error resolving incident', 'danger');
    }
}

// ============================================================================
// 5. ADMINISTRATOR WORKSPACE
// ============================================================================
function initAdminWorkspace() {
    populateAdminFilterDropdowns();
    switchAdminView('incidents');
}

function populateAdminFilterDropdowns() {
    const serviceFilter = document.getElementById('filterService');
    const categoryFilter = document.getElementById('filterCategory');
    const techFilter = document.getElementById('filterTechnician');

    if (serviceFilter) {
        serviceFilter.innerHTML = '<option value="">Service: All</option>' +
            AppState.services.map(s => `<option value="${s.id}">${s.serviceName} (${s.serviceCode})</option>`).join('');
    }

    if (categoryFilter) {
        categoryFilter.innerHTML = '<option value="">Category: All</option>' +
            AppState.categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    }

    if (techFilter) {
        techFilter.innerHTML = '<option value="">Technician: All</option>' +
            AppState.technicians.map(t => `<option value="${t.id}">${t.name} (${t.userIdCode})</option>`).join('');
    }
}

function switchAdminView(viewName, event) {
    if (event) event.preventDefault();
    AppState.currentAdminView = viewName;

    // Sidebar active state
    document.querySelectorAll('.admin-nav-item').forEach(el => el.classList.remove('active'));
    const activeNav = document.getElementById(`adminNav${capitalize(viewName)}`);
    if (activeNav) activeNav.classList.add('active');

    // Toggle views
    document.getElementById('adminViewIncidents').classList.add('d-none');
    document.getElementById('adminViewSla').classList.add('d-none');
    document.getElementById('adminViewTechs').classList.add('d-none');
    document.getElementById('adminViewAudit').classList.add('d-none');
    document.getElementById('adminViewEmails').classList.add('d-none');

    const targetView = document.getElementById(`adminView${capitalize(viewName)}`);
    if (targetView) targetView.classList.remove('d-none');

    // Load data for view
    if (viewName === 'incidents') loadAdminIncidents();
    else if (viewName === 'sla') loadAdminSlaView();
    else if (viewName === 'techs') loadAdminTechsView();
    else if (viewName === 'audit') loadAdminAuditView();
    else if (viewName === 'emails') loadAdminEmailsView();
}

async function loadAdminIncidents() {
    const tbody = document.getElementById('adminIncidentsTbody');
    tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading enterprise incidents...</td></tr>';

    const status = document.getElementById('filterStatus').value;
    const priority = document.getElementById('filterPriority').value;
    const serviceId = document.getElementById('filterService').value;
    const categoryId = document.getElementById('filterCategory').value;
    const technicianId = document.getElementById('filterTechnician').value;

    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (priority) params.append('priority', priority);
    if (serviceId) params.append('serviceId', serviceId);
    if (categoryId) params.append('categoryId', categoryId);
    if (technicianId) params.append('technicianId', technicianId);

    try {
        const res = await fetch(`/api/incidents?${params.toString()}`);
        const result = await res.json();

        if (result.success && result.data) {
            updateAdminKpiMetrics(result.data);
            renderAdminIncidentsTable(result.data);
        } else {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted">No matching incidents.</td></tr>';
        }
    } catch (e) {
        console.error('Error loading admin incidents', e);
        tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-danger">Error loading incidents.</td></tr>';
    }
}

function updateAdminKpiMetrics(incidents) {
    document.getElementById('kpiTotal').textContent = incidents.length;
    document.getElementById('kpiOpen').textContent = incidents.filter(i => i.status === 'OPEN').length;
    document.getElementById('kpiInProgress').textContent = incidents.filter(i => i.status === 'IN_PROGRESS').length;
    document.getElementById('kpiClosed').textContent = incidents.filter(i => i.status === 'CLOSED').length;
    document.getElementById('kpiSlaViolated').textContent = incidents.filter(i => i.slaViolated).length;
    document.getElementById('kpiDuplicates').textContent = incidents.filter(i => i.duplicateOfIncidentCode).length;
}

function renderAdminIncidentsTable(incidents) {
    const tbody = document.getElementById('adminIncidentsTbody');
    if (!incidents || incidents.length === 0) {
        tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted">No incidents match the selected filter criteria.</td></tr>';
        return;
    }

    tbody.innerHTML = incidents.map(inc => {
        const reporterName = inc.reporter ? inc.reporter.name : 'N/A';
        const serviceCode = inc.affectedService ? inc.affectedService.serviceCode : 'N/A';
        const techName = inc.assignedTechnician ? `${inc.assignedTechnician.name} (${inc.assignedTechnician.userIdCode})` : '<span class="text-muted">Unassigned</span>';
        const dupBadge = inc.duplicateOfIncidentCode
            ? `<span class="badge-duplicate-cluster">Dup: ${inc.duplicateOfIncidentCode}</span>`
            : '<span class="text-muted fs-xs">-</span>';

        return `
            <tr>
                <td class="col-nowrap"><span class="code-pill">${inc.incidentCode}</span></td>
                <td class="incident-title-cell">${escapeHtml(inc.title)}</td>
                <td class="col-nowrap">${reporterName}</td>
                <td class="col-nowrap"><span class="service-pill">${serviceCode}</span></td>
                <td class="col-nowrap">${getPriorityBadge(inc.priority)}</td>
                <td class="col-nowrap">${getStatusBadge(inc.status)}</td>
                <td class="col-nowrap">${dupBadge}</td>
                <td class="col-nowrap fs-xs">${techName}</td>
                <td class="col-nowrap">${getSlaStatusBadge(inc.slaStatus, inc.slaViolated)}</td>
                <td class="col-nowrap fs-xs text-muted">${formatDateTime(inc.createdAt)}</td>
                <td class="text-end col-nowrap">
                    <button class="btn btn-outline-secondary btn-sm py-0 px-2 fs-xs" onclick="openAuditModal('${inc.incidentCode}')">
                        <i class="bi bi-clock-history me-1"></i> Audit
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function applyAdminFilters() {
    loadAdminIncidents();
}

function resetAdminFilters() {
    document.getElementById('filterStatus').value = '';
    document.getElementById('filterPriority').value = '';
    document.getElementById('filterService').value = '';
    document.getElementById('filterCategory').value = '';
    document.getElementById('filterTechnician').value = '';
    loadAdminIncidents();
}

async function loadAdminSlaView() {
    const tbody = document.getElementById('adminSlaTbody');
    tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading SLA monitor data...</td></tr>';

    try {
        const res = await fetch('/api/incidents');
        const result = await res.json();

        if (result.success && result.data) {
            renderAdminSlaTable(result.data);
        }
    } catch (e) {
        console.error('Error loading SLA data', e);
    }
}

function renderAdminSlaTable(incidents) {
    const tbody = document.getElementById('adminSlaTbody');
    if (!incidents || incidents.length === 0) {
        tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted">No incidents tracked.</td></tr>';
        return;
    }

    tbody.innerHTML = incidents.map(inc => {
        const violatedBadge = inc.slaViolated
            ? '<span class="badge-sla-violated">Breached</span>'
            : '<span class="badge-sla-on-track">On Track</span>';

        return `
            <tr>
                <td class="col-nowrap"><span class="code-pill">${inc.incidentCode}</span></td>
                <td class="incident-title-cell">${escapeHtml(inc.title)}</td>
                <td class="col-nowrap"><span class="service-pill">${inc.affectedService ? inc.affectedService.serviceCode : 'N/A'}</span></td>
                <td class="col-nowrap fs-xs">${inc.assignedTechnician ? inc.assignedTechnician.userIdCode : 'Unassigned'}</td>
                <td class="col-nowrap">${getStatusBadge(inc.status)}</td>
                <td class="col-nowrap fs-xs text-muted">${formatDateTime(inc.createdAt)}</td>
                <td class="col-nowrap fs-xs font-mono">${formatDateTime(inc.responseDeadline)}</td>
                <td class="col-nowrap fs-xs font-mono">${formatDateTime(inc.resolutionDeadline)}</td>
                <td class="col-nowrap fs-xs text-muted">${inc.acknowledgedAt ? formatDateTime(inc.acknowledgedAt) : 'Pending'}</td>
                <td class="col-nowrap">${violatedBadge}</td>
                <td class="col-nowrap">${getSlaStatusBadge(inc.slaStatus, inc.slaViolated)}</td>
            </tr>
        `;
    }).join('');
}

async function triggerSlaCheck() {
    try {
        const res = await fetch('/api/intelligence/sla/check-now', { method: 'POST' });
        const result = await res.json();

        if (result.success) {
            const count = result.data ? result.data.length : 0;
            showToast(`Continuous SLA check completed. ${count} active incidents evaluated.`);
            if (AppState.currentAdminView === 'sla') loadAdminSlaView();
            else if (AppState.currentAdminView === 'incidents') loadAdminIncidents();
        } else {
            showToast('SLA evaluation trigger failed', 'danger');
        }
    } catch (e) {
        console.error('SLA check error', e);
        showToast('Error triggering SLA check', 'danger');
    }
}

async function loadAdminTechsView() {
    const tbody = document.getElementById('adminTechsTbody');
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading technicians...</td></tr>';

    try {
        const res = await fetch('/api/lookup/technicians');
        const result = await res.json();

        if (result.success && result.data) {
            AppState.technicians = result.data;
            tbody.innerHTML = result.data.map(tech => {
                const currentLoad = tech.currentWorkload || 0;
                const maxLoad = tech.maxWorkload || 5;
                const percentage = Math.min(100, Math.round((currentLoad / maxLoad) * 100));
                const barColor = percentage >= 100 ? 'bg-danger' : percentage >= 80 ? 'bg-warning' : 'bg-success';

                const skillsBadges = (tech.skills && tech.skills.length > 0)
                    ? Array.from(tech.skills).map(s => `<span class="badge bg-secondary fs-xs me-1">${s}</span>`).join('')
                    : '<span class="text-muted fs-xs">-</span>';

                const availBadge = (currentLoad >= maxLoad)
                    ? '<span class="badge bg-danger fs-xs">At Capacity</span>'
                    : '<span class="badge bg-success fs-xs">Available</span>';

                return `
                    <tr>
                        <td class="col-nowrap"><span class="code-pill">${tech.userIdCode}</span></td>
                        <td><strong>${tech.name}</strong></td>
                        <td class="text-muted fs-xs">${tech.email}</td>
                        <td>${skillsBadges}</td>
                        <td class="fs-xs">${tech.experienceYears || 0} Years</td>
                        <td style="width: 220px;">
                            <div class="d-flex justify-content-between align-items-center fs-xs mb-1">
                                <span class="fw-semibold font-mono">${currentLoad} / ${maxLoad} active</span>
                                <span class="text-muted">${percentage}%</span>
                            </div>
                            <div class="progress" style="height: 6px;">
                                <div class="progress-bar ${barColor}" style="width: ${percentage}%"></div>
                            </div>
                        </td>
                        <td>${availBadge}</td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('Error loading technicians', e);
    }
}

let cachedAuditLogs = [];
async function loadAdminAuditView() {
    const tbody = document.getElementById('adminAuditTbody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading system audit trail...</td></tr>';

    try {
        const res = await fetch('/api/lookup/audit-logs');
        const result = await res.json();

        if (result.success && result.data) {
            cachedAuditLogs = result.data;
            renderAuditTable(cachedAuditLogs);
        }
    } catch (e) {
        console.error('Error loading audit logs', e);
    }
}

function renderAuditTable(logs) {
    const tbody = document.getElementById('adminAuditTbody');
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No audit trail records found.</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(l => {
        let actionBadgeClass = 'bg-secondary';
        if (l.action.includes('VIOLATED')) actionBadgeClass = 'bg-danger';
        else if (l.action.includes('WARNING')) actionBadgeClass = 'bg-warning text-dark';
        else if (l.action.includes('ASSIGNED')) actionBadgeClass = 'bg-primary';
        else if (l.action.includes('CLOSED')) actionBadgeClass = 'bg-success';
        else if (l.action.includes('DUPLICATE')) actionBadgeClass = 'bg-info text-dark';

        const stateTransition = (l.previousState || l.newState)
            ? `<span class="badge bg-light text-dark border fs-xs">${l.previousState || 'None'}</span> <i class="bi bi-arrow-right"></i> <span class="badge bg-light text-dark border fs-xs">${l.newState || 'None'}</span>`
            : '<span class="text-muted fs-xs">-</span>';

        return `
            <tr>
                <td class="fs-xs font-mono text-muted col-nowrap">${formatDateTime(l.timestamp)}</td>
                <td class="col-nowrap">${l.incidentCode ? `<span class="code-pill">${l.incidentCode}</span>` : '<span class="text-muted font-mono fs-xs">SYSTEM</span>'}</td>
                <td><span class="badge ${actionBadgeClass} fs-xs">${l.action}</span></td>
                <td>${stateTransition}</td>
                <td><span class="badge bg-dark fs-xs font-mono">${l.performedByUserCode || 'SYSTEM'}</span></td>
                <td class="fs-xs">${escapeHtml(l.description)}</td>
            </tr>
        `;
    }).join('');
}

function filterAuditLogsClient(term) {
    if (!term || !term.trim()) {
        renderAuditTable(cachedAuditLogs);
        return;
    }
    const q = term.trim().toLowerCase();
    const filtered = cachedAuditLogs.filter(l =>
        (l.incidentCode && l.incidentCode.toLowerCase().includes(q)) ||
        (l.action && l.action.toLowerCase().includes(q)) ||
        (l.performedByUserCode && l.performedByUserCode.toLowerCase().includes(q)) ||
        (l.description && l.description.toLowerCase().includes(q))
    );
    renderAuditTable(filtered);
}

let cachedSimulatedEmails = [];
async function loadAdminEmailsView() {
    const tbody = document.getElementById('adminEmailsTbody');
    tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading simulated emails...</td></tr>';

    try {
        const res = await fetch('/api/notifications/emails');
        const result = await res.json();

        if (result.success && result.data) {
            cachedSimulatedEmails = result.data;
            if (cachedSimulatedEmails.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">No simulated emails have been dispatched yet.</td></tr>';
                document.getElementById('adminEmailPreview').textContent = 'No simulated emails sent yet.';
                return;
            }

            tbody.innerHTML = cachedSimulatedEmails.map(mail => `
                <tr onclick="selectAdminEmail(${mail.id})">
                    <td class="fs-xs font-mono text-muted col-nowrap">${formatDateTime(mail.createdAt)}</td>
                    <td class="col-nowrap">${mail.incidentCode ? `<span class="code-pill">${mail.incidentCode}</span>` : '<span class="text-muted font-mono fs-xs">SYSTEM</span>'}</td>
                    <td><span class="badge bg-light text-dark border fs-xs">${mail.eventType}</span></td>
                    <td class="fs-xs">${mail.recipientEmail}</td>
                </tr>
            `).join('');

            // Automatically select first email
            selectAdminEmail(cachedSimulatedEmails[0].id);
        }
    } catch (e) {
        console.error('Error loading emails', e);
    }
}

function selectAdminEmail(id) {
    const mail = cachedSimulatedEmails.find(m => m.id === id);
    if (!mail) return;

    const viewer = document.getElementById('adminEmailPreview');
    viewer.textContent = `----------------------------- [SIMULATED EMAIL] -----------------------------
From    : noreply@i-itsm.org
To      : ${mail.recipientEmail} (${mail.recipientUserCode})
Subject : ${mail.subject}
Event   : ${mail.eventType}
Timestamp: ${formatDateTime(mail.createdAt)}
Body    :
${mail.message}
-----------------------------------------------------------------------------`;
}

// ============================================================================
// 6. NOTIFICATIONS OFFCANVAS (5D-1 Mechanism 1: DASHBOARD)
// ============================================================================
async function loadNotifications() {
    if (!AppState.currentUser) return;
    const userCode = AppState.currentUser.userIdCode;

    try {
        const res = await fetch(`/api/notifications/user/${userCode}`);
        const result = await res.json();

        if (result.success && result.data) {
            renderNotificationsDrawer(result.data);
        }
    } catch (e) {
        console.error('Failed to load notifications', e);
    }
}

function renderNotificationsDrawer(notifications) {
    const container = document.getElementById('notificationsListContainer');
    const badge = document.getElementById('unreadNotifCount');

    const unread = notifications.filter(n => !n.readStatus);
    if (unread.length > 0) {
        badge.textContent = unread.length;
        badge.classList.remove('d-none');
    } else {
        badge.classList.add('d-none');
    }

    if (!notifications || notifications.length === 0) {
        container.innerHTML = '<div class="text-center py-5 text-muted"><i class="bi bi-bell-slash fs-1 d-block mb-2"></i>No notifications received yet.</div>';
        return;
    }

    container.innerHTML = notifications.map(n => {
        let eventBadge = 'bg-primary';
        if (n.eventType.includes('VIOLATED')) eventBadge = 'bg-danger';
        else if (n.eventType.includes('APPROACHING')) eventBadge = 'bg-warning text-dark';
        else if (n.eventType.includes('CLOSED')) eventBadge = 'bg-success';

        return `
            <div class="notification-card p-3 mb-2 ${!n.readStatus ? 'unread' : ''}">
                <div class="d-flex justify-content-between align-items-start mb-1">
                    <span class="badge ${eventBadge} fs-xs">${n.eventType}</span>
                    <small class="text-muted fs-xs">${formatDateTime(n.createdAt)}</small>
                </div>
                <div class="fw-bold fs-sm text-dark mb-1">${escapeHtml(n.subject)}</div>
                <p class="fs-xs text-muted mb-2">${escapeHtml(n.message)}</p>
                <div class="d-flex justify-content-between align-items-center">
                    <span class="font-mono fs-xs fw-bold text-secondary">Ticket: ${n.incidentCode || 'SYSTEM'}</span>
                    ${!n.readStatus ? `<button class="btn btn-sm btn-link p-0 fs-xs text-decoration-none" onclick="markNotificationRead(${n.id})">Mark as Read</button>` : '<span class="text-muted fs-xs"><i class="bi bi-check2"></i> Read</span>'}
                </div>
            </div>
        `;
    }).join('');
}

async function markNotificationRead(id) {
    try {
        const res = await fetch(`/api/notifications/${id}/read`, { method: 'POST' });
        const result = await res.json();
        if (result.success) {
            loadNotifications();
        }
    } catch (e) {
        console.error('Error marking notification read', e);
    }
}

// ============================================================================
// 7. MODALS & LIFECYCLE ACTIONS
// ============================================================================
function initStarRating() {
    const stars = document.querySelectorAll('#starRatingContainer i');
    stars.forEach(star => {
        star.addEventListener('click', (e) => {
            const val = parseInt(e.target.getAttribute('data-val'));
            setStarRating(val);
        });
    });
}

function setStarRating(val) {
    document.getElementById('feedbackRatingValue').value = val;
    const stars = document.querySelectorAll('#starRatingContainer i');
    stars.forEach(star => {
        const starVal = parseInt(star.getAttribute('data-val'));
        if (starVal <= val) {
            star.className = 'bi bi-star-fill';
        } else {
            star.className = 'bi bi-star';
        }
    });

    const labels = {
        1: '1 / 5 - Poor',
        2: '2 / 5 - Fair',
        3: '3 / 5 - Good',
        4: '4 / 5 - Very Good',
        5: '5 / 5 - Excellent'
    };
    document.getElementById('feedbackRatingLabel').textContent = labels[val] || `${val} / 5`;
}

let activeFeedbackIncidentCode = null;
function openFeedbackModal(code, rating = 5, comments = '') {
    activeFeedbackIncidentCode = code;
    document.getElementById('feedbackIncidentCode').textContent = code;
    document.getElementById('feedbackComments').value = comments;
    setStarRating(rating || 5);
    const modal = new bootstrap.Modal(document.getElementById('feedbackModal'));
    modal.show();
}

async function submitFeedback() {
    const code = activeFeedbackIncidentCode;
    const rating = parseInt(document.getElementById('feedbackRatingValue').value);
    const comments = document.getElementById('feedbackComments').value.trim();

    if (!code) return;

    try {
        const res = await fetch(`/api/incidents/${code}/feedback`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                reporterUserCode: AppState.currentUser.userIdCode,
                rating,
                comments
            })
        });

        const result = await res.json();
        if (result.success) {
            showToast('Customer satisfaction rating recorded. Thank you!');
            bootstrap.Modal.getInstance(document.getElementById('feedbackModal'))?.hide();
            loadEmployeeIncidents();
        } else {
            showToast(result.message || 'Failed to submit rating', 'danger');
        }
    } catch (e) {
        console.error('Error submitting feedback', e);
        showToast('Error submitting feedback', 'danger');
    }
}

let activeReopenIncidentCode = null;
function openReopenModal(code) {
    activeReopenIncidentCode = code;
    document.getElementById('reopenIncidentCode').textContent = code;
    document.getElementById('reopenReason').value = '';
    const modal = new bootstrap.Modal(document.getElementById('reopenModal'));
    modal.show();
}

async function submitReopen() {
    const code = activeReopenIncidentCode;
    const reason = document.getElementById('reopenReason').value.trim();

    if (!code || !reason) {
        showToast('Please provide a reason to reopen.', 'warning');
        return;
    }

    try {
        const res = await fetch(`/api/incidents/${code}/reopen`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                actorUserCode: AppState.currentUser.userIdCode,
                note: reason
            })
        });

        const result = await res.json();
        if (result.success) {
            showToast(`Incident ${code} reopened back to IN_PROGRESS.`);
            bootstrap.Modal.getInstance(document.getElementById('reopenModal'))?.hide();
            loadEmployeeIncidents();
        } else {
            showToast(result.message || 'Failed to reopen incident', 'danger');
        }
    } catch (e) {
        console.error('Error reopening incident', e);
        showToast('Error reopening incident', 'danger');
    }
}

async function openAuditModal(incidentCode) {
    document.getElementById('auditModalIncidentCode').textContent = incidentCode;
    const tbody = document.getElementById('auditModalTbody');
    tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted"><span class="spinner-border spinner-border-sm me-1"></span>Loading audit entries...</td></tr>';

    const modal = new bootstrap.Modal(document.getElementById('auditHistoryModal'));
    modal.show();

    try {
        const res = await fetch(`/api/lookup/audit-logs?incidentCode=${incidentCode}`);
        const result = await res.json();

        if (result.success && result.data && result.data.length > 0) {
            tbody.innerHTML = result.data.map(l => `
                <tr>
                    <td class="font-mono fs-xs text-muted" style="white-space: nowrap;">${formatDateTime(l.timestamp)}</td>
                    <td><span class="badge bg-secondary fs-xs">${l.action}</span></td>
                    <td class="fs-xs">${(l.previousState || l.newState) ? `${l.previousState || '-'} &rarr; ${l.newState || '-'}` : '-'}</td>
                    <td><span class="badge bg-dark fs-xs font-mono">${l.performedByUserCode}</span></td>
                    <td class="fs-xs">${escapeHtml(l.description)}</td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">No audit entries found for this incident.</td></tr>';
        }
    } catch (e) {
        console.error('Failed to load audit history', e);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-danger">Error loading audit entries.</td></tr>';
    }
}

// ============================================================================
// 8. HELPERS & FORMATTERS
// ============================================================================
function getPriorityBadge(priority) {
    if (!priority) return '<span class="badge bg-secondary fs-xs">N/A</span>';
    if (priority.includes('P1')) return '<span class="badge-priority-p1">P1 - Critical</span>';
    if (priority.includes('P2')) return '<span class="badge-priority-p2">P2 - High</span>';
    if (priority.includes('P3')) return '<span class="badge-priority-p3">P3 - Medium</span>';
    return '<span class="badge-priority-p4">P4 - Low</span>';
}

function getStatusBadge(status) {
    if (!status) return '<span class="badge bg-secondary fs-xs">N/A</span>';
    if (status === 'OPEN') return '<span class="badge-status-open">Open</span>';
    if (status === 'IN_PROGRESS') return '<span class="badge-status-in-progress">In Progress</span>';
    if (status === 'CLOSED') return '<span class="badge-status-closed">Closed</span>';
    return `<span class="badge-status-open">${status}</span>`;
}

function getSlaStatusBadge(slaStatus, isViolated) {
    if (isViolated || slaStatus === 'RESPONSE_VIOLATED' || slaStatus === 'RESOLUTION_VIOLATED') {
        const text = slaStatus === 'RESPONSE_VIOLATED' ? 'Resp Breach' : slaStatus === 'RESOLUTION_VIOLATED' ? 'Resol Breach' : 'Breached';
        return `<span class="badge-sla-violated">${text}</span>`;
    }
    if (slaStatus === 'AT_RISK') {
        return '<span class="badge-sla-at-risk">At Risk</span>';
    }
    return '<span class="badge-sla-on-track">On Track</span>';
}

function formatDateTime(isoString) {
    if (!isoString) return 'N/A';
    try {
        const d = new Date(isoString);
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    } catch (e) {
        return isoString;
    }
}

function truncate(str, len = 30) {
    if (!str) return '';
    return str.length > len ? str.substring(0, len) + '...' : str;
}

function capitalize(str) {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1);
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeJs(str) {
    if (!str) return '';
    return str
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"')
        .replace(/\n/g, '\\n')
        .replace(/\r/g, '');
}

function showToast(msg, bg = 'primary') {
    const toastEl = document.getElementById('appToast');
    if (!toastEl) return;
    toastEl.className = `toast align-items-center text-white bg-${bg} border-0`;
    document.getElementById('toastMessage').textContent = msg;
    const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
    toast.show();
}
