function getCsrfHeaders() {
    const h = { 'X-Requested-With': 'XMLHttpRequest' };
    if (typeof window.__csrfHeaderName !== 'undefined' && window.__csrfToken) {
        h[window.__csrfHeaderName] = window.__csrfToken;
    }
    return h;
}

function appendCsrfToFormData(formData) {
    if (typeof window.__csrfParameterName !== 'undefined' && window.__csrfToken) {
        if (!formData.get(window.__csrfParameterName)) {
            formData.append(window.__csrfParameterName, window.__csrfToken);
        }
    }
}

function appendCsrfToUrlSearchParams(params) {
    if (typeof window.__csrfParameterName !== 'undefined' && window.__csrfToken) {
        if (!params.has(window.__csrfParameterName)) {
            params.append(window.__csrfParameterName, window.__csrfToken);
        }
    }
}

function formHasFileInput(form) {
    return form.querySelector('input[type="file"]') !== null;
}

function loadContent(apiUrl) {
    const pageContent = document.getElementById('page-content');
    const dashboardContent = document.getElementById('dashboardContent');
    const mainContent = document.getElementById('mainContent');
    if (!pageContent) {
        if (typeof apiUrl === 'string' && apiUrl.startsWith('/')) {
            try {
                sessionStorage.setItem('fhPendingLoad', apiUrl);
            } catch (e) {
                /* storage unavailable */
            }
            window.location.href = '/api/home';
            return;
        }
        console.error('loadContent: #page-content missing and URL not usable for redirect:', apiUrl);
        return;
    }
    if (dashboardContent) {
        dashboardContent.style.display = 'none';
    }
    if (mainContent) {
        mainContent.style.display = 'block';
    }
    fetchHtml(apiUrl)
        .then(function (html) {
            pageContent.innerHTML = html;
            initLoadedPageContent();
        })
        .catch(function (error) {
            console.error('Error loading content:', error);
            pageContent.innerHTML = '<p>There was an error loading the page.</p>';
        });
}

function fetchHtml(url) {
    return fetch(url, {
        credentials: 'same-origin',
        headers: getCsrfHeaders()
    }).then(function (response) {
        if (!response.ok) {
            throw new Error('Request failed: ' + response.status);
        }
        return response.text();
    });
}

function getHubRoot() {
    return document.querySelector('#page-content .fh-section-hub');
}

function getHubPanel() {
    const hub = getHubRoot();
    return hub ? hub.querySelector('#fhHubPanel') : null;
}

function setHubNavActive(button) {
    const hub = button ? button.closest('.fh-section-hub') : getHubRoot();
    if (!hub) {
        return;
    }
    hub.querySelectorAll('.fh-hub-nav-item').forEach(function (item) {
        item.classList.remove('active');
    });
    if (button) {
        button.classList.add('active');
    }
}

function showHubPanelMode(showPagePanel) {
    const panel = getHubPanel();
    const reportCard = document.getElementById('expenseReportCard');
    if (panel) {
        panel.hidden = !showPagePanel;
    }
    if (reportCard) {
        reportCard.hidden = !!showPagePanel;
    }
}

function injectHubHtml(html) {
    const panel = getHubPanel();
    if (!panel) {
        return false;
    }
    showHubPanelMode(true);
    panel.innerHTML = html;
    initLoadedPageContent();
    return true;
}

function loadIntoHubPanel(url, options) {
    const opts = options || {};
    const panel = getHubPanel();
    if (!panel) {
        if (opts.fallbackLoadContent) {
            loadContent(url);
        }
        return;
    }
    showHubPanelMode(true);
    panel.innerHTML = '<p class="fh-hub-loading">Loading…</p>';
    fetchHtml(url)
        .then(function (html) {
            panel.innerHTML = html;
            if (opts.reportType && typeof attachReportListeners === 'function') {
                attachReportListeners(panel, opts.reportType);
            }
            if (!opts.skipInit) {
                initLoadedPageContent();
            }
        })
        .catch(function (error) {
            console.error(opts.errorLabel || 'Error loading hub content:', error);
            panel.innerHTML = '<p>' + (opts.errorMessage || 'There was an error loading the page.') + '</p>';
        });
}

function showHubPage(button) {
    if (!button) {
        return;
    }
    const url = button.getAttribute('data-url');
    if (!url) {
        return;
    }
    setHubNavActive(button);
    loadIntoHubPanel(url, { fallbackLoadContent: true });
}

function showHubReport(button) {
    if (!button) {
        return;
    }
    const reportType = button.getAttribute('data-report');
    if (!reportType || typeof prepareUrl !== 'function') {
        return;
    }
    setHubNavActive(button);
    loadIntoHubPanel(prepareUrl(reportType), {
        reportType: reportType,
        skipInit: true,
        errorLabel: 'Error loading hub report',
        errorMessage: 'Error loading report. Please try again.'
    });
}

function showHubExpenseReport(button) {
    if (!button) {
        return;
    }
    setHubNavActive(button);
    showHubPanelMode(false);
    selectExpenseReport(button);
}

function submitForm(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const formId = form.id;
    if (!validForm(formId)) {
        return;
    }
    const headers = getCsrfHeaders();
    let body;
    if (formHasFileInput(form)) {
        const formData = new FormData(form);
        appendCsrfToFormData(formData);
        body = formData;
    } else {
        const params = new URLSearchParams(new FormData(form));
        appendCsrfToUrlSearchParams(params);
        body = params;
        headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
    }
    fetch(form.action, {
        method: 'POST',
        body: body,
        headers: headers,
        credentials: 'same-origin'
    })
        .then(function (response) { return response.text(); })
        .then(function (html) {
            if (injectHubHtml(html)) {
                return;
            }
            const pageContent = document.getElementById('page-content');
            if (pageContent) {
                pageContent.innerHTML = html;
                initLoadedPageContent();
            }
        })
        .catch(function (error) {
            console.error('Error submitting form:', error);
            const pageContent = document.getElementById('page-content');
            if (pageContent) {
                pageContent.innerHTML = '<p>There was an error submitting the form. Please try again.</p>';
            }
        });
}

function loadEditPageContent(url) {
    if (getHubPanel()) {
        activateHubNavForUrl(url);
        loadIntoHubPanel(url, {
            errorLabel: 'Error loading edit page',
            errorMessage: 'There was an error loading the form. Please try again.'
        });
        return;
    }
    fetchHtml(url)
        .then(function (html) {
            const pageContent = document.getElementById('page-content');
            if (pageContent) {
                pageContent.innerHTML = html;
                initLoadedPageContent();
            }
        })
        .catch(function (error) {
            console.error('Error loading edit page:', error);
            const pageContent = document.getElementById('page-content');
            if (pageContent) {
                pageContent.innerHTML = '<p>There was an error loading the form. Please try again.</p>';
            }
        });
}

function activateHubNavForUrl(url) {
    const hub = getHubRoot();
    if (!hub || !url) {
        return;
    }
    let path = url.split('?')[0];
    if (path === '/api/loan/editLoan') {
        path = '/api/loan/addLoan';
    } else if (path.indexOf('/api/loan/recordEmi') === 0 || path.indexOf('/api/loan/preCloseLoan') === 0) {
        const emiBtn = hub.querySelector('.fh-hub-nav-item[data-report="loanEmiReport"]');
        if (emiBtn) {
            setHubNavActive(emiBtn);
        }
        return;
    }
    const match = Array.from(hub.querySelectorAll('.fh-hub-nav-item[data-url]')).find(function (btn) {
        const dataUrl = btn.getAttribute('data-url') || '';
        return dataUrl.split('?')[0] === path;
    });
    if (match) {
        setHubNavActive(match);
    }
}

function openHubOrLoad(fallbackUrl, navSelector) {
    const hub = getHubRoot();
    if (hub && navSelector) {
        const btn = hub.querySelector(navSelector);
        if (btn) {
            btn.click();
            return;
        }
    }
    loadContent(fallbackUrl);
}

function submitAccountForm(event) {
    if (getHubPanel()) {
        submitForm(event);
        return false;
    }
    return true;
}

function refreshReportAfterDelete(reportType) {
    if (!reportType) {
        return;
    }
    if (reportType.indexOf('ReportContainer') !== -1 || reportType.indexOf('manageReport') === 0) {
        reloadSelectedExpenseReport();
        return;
    }
    const hub = getHubRoot();
    if (hub) {
        const active = hub.querySelector('.fh-hub-nav-item.active[data-report]')
            || hub.querySelector('.fh-hub-nav-item[data-report="' + reportType + '"]');
        if (active) {
            showHubReport(active);
            return;
        }
    }
    if (typeof fetchReportContent === 'function') {
        fetchReportContent(reportType);
    }
}

function deleteEntity(anchor, entityType, apiEndpoint) {
    var entityId = anchor.getAttribute('data-id');
    var reportType = anchor.getAttribute('data-report-type');
    if (!confirm('Are you sure you want to delete this ' + entityType + '?')) {
        return;
    }
    let apiUrl = apiEndpoint + '?id=' + entityId;
    if (entityType.toLowerCase().includes('plan')) {
        apiUrl += '&type=plan';
    } else if (entityType.toLowerCase().includes('actual')) {
        apiUrl += '&type=actual';
    }
    fetch(apiUrl, {
        method: 'DELETE',
        headers: getCsrfHeaders(),
        credentials: 'same-origin'
    })
        .then(function (response) {
            return response.text().then(function (data) {
                if (response.status === 409) {
                    alert(data);
                } else if (response.ok) {
                    if (data === 'success') {
                        alert(entityType.charAt(0).toUpperCase() + entityType.slice(1) + ' deleted successfully');
                        refreshReportAfterDelete(reportType);
                    } else {
                        alert('Error deleting ' + entityType);
                    }
                } else {
                    alert('Error: ' + data);
                }
            });
        })
        .catch(function (error) {
            alert('Error: ' + error.message);
        });
}
function callEditCategory(element) {
        const id = element.getAttribute('data-id') || '';
        const name = element.getAttribute('data-name') || '';
        const icon = element.getAttribute('data-icon') || '';
        const sortOrder = element.getAttribute('data-sort-order') || '';
        const enabled = element.getAttribute('data-enabled') === 'true';

        editCategory(id, name, icon, sortOrder, enabled);
}
let profilePhotoPreviewObjectUrl = null;

function previewProfilePhoto(fileInput) {
    const preview = document.getElementById('profilePhotoPreview');
    if (!preview) {
        return;
    }
    if (profilePhotoPreviewObjectUrl) {
        URL.revokeObjectURL(profilePhotoPreviewObjectUrl);
        profilePhotoPreviewObjectUrl = null;
    }
    if (!fileInput || !fileInput.files || !fileInput.files[0]) {
        const defaultSrc = preview.getAttribute('data-default-src');
        if (defaultSrc) {
            preview.src = defaultSrc;
        }
        return;
    }
    const file = fileInput.files[0];
    const allowed = ['image/jpeg', 'image/jpg', 'image/png'];
    const name = (file.name || '').toLowerCase();
    const typeOk = file.type && allowed.includes(file.type.toLowerCase());
    const extOk = name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png');
    if (!typeOk && !extOk) {
        alert('Please choose a JPG, JPEG, or PNG image.');
        fileInput.value = '';
        return;
    }
    if (file.size > 512 * 1024) {
        alert('Image must be 512 KB or smaller.');
        fileInput.value = '';
        return;
    }
    profilePhotoPreviewObjectUrl = URL.createObjectURL(file);
    preview.src = profilePhotoPreviewObjectUrl;
}

function previewCategoryIcon(fileInput) {
    const preview = document.getElementById("categoryIconPreview");
    if (!preview || !fileInput || !fileInput.files || !fileInput.files[0]) {
        return;
    }
    const file = fileInput.files[0];
    if (file.size > 250 * 1024) {
        alert("Image must be 250 KB or smaller.");
        fileInput.value = "";
        return;
    }
    const url = URL.createObjectURL(file);
    preview.onload = function () {
        URL.revokeObjectURL(url);
        preview.onload = null;
    };
    preview.src = url;
}

function editCategory(id, name, icon, sortOrder, enabled) {
    document.getElementById("categoryId").value = id;
    document.getElementById("categoryName").value = name;
    const hiddenIcon = document.getElementById("iconPath");
    if (hiddenIcon) {
        hiddenIcon.value = icon || "";
    }
    document.getElementById("sortOrder").value = sortOrder;
    document.getElementById("enabled").checked = enabled;

    const fileInput = document.getElementById("iconImage");
    if (fileInput) {
        fileInput.value = "";
    }
    const preview = document.getElementById("categoryIconPreview");
    if (preview) {
        const src = icon && icon.trim() ? icon : "/images/category-placeholder.svg";
        preview.src = src;
    }

    window.scrollTo({ top: 0, behavior: "smooth" });
}
function deleteCategoryEntity(element, entityName, apiUrl) {
    let categoryId = element.getAttribute("data-id");
    if (!confirm(`Are you sure you want to delete this ${entityName}?`)) {
        return;
    }
    const params = new URLSearchParams();
    params.append("id", categoryId);
    if (typeof window.__csrfParameterName !== 'undefined' && window.__csrfToken) {
        params.append(window.__csrfParameterName, window.__csrfToken);
    }
    fetch(apiUrl + "categoryDelete", {
        method: "POST",
        headers: Object.assign({ "Content-Type": "application/x-www-form-urlencoded" }, getCsrfHeaders()),
        body: params.toString(),
        credentials: 'same-origin'
    })
    .then(response => response.text())
    .then(html => {
        alert("Category Deleted Successfully!");
        if (injectHubHtml(html)) {
            return;
        }
        const pageContent = document.getElementById('page-content');
        if (pageContent) {
            pageContent.innerHTML = html;
            initLoadedPageContent();
        }
    })
    .catch(error => console.error("Error deleting category:", error));
}

function calculateExpenses() {
        let total = 0;
        document.querySelectorAll(".expenses").forEach(input => {
            total += parseFloat(input.value) || 0;
        });
        document.getElementById("totalExpense").value = total;
}

function loadReport(yearId, apiUrl, containerId) {
    if (typeof event !== 'undefined' && event && typeof event.preventDefault === 'function') {
        event.preventDefault();
    }
    const yearEl = document.getElementById(yearId);
    const year = yearEl ? yearEl.value : '';
    if (!year) {
        alert("Please select a year.");
        return;
    }
    let lastValue = apiUrl.split("/").pop();

    const url = `${apiUrl}?year=${encodeURIComponent(year)}`;
    const container = document.getElementById(containerId);
    fetch(url, {
        method: 'GET',
        credentials: 'same-origin',
        headers: getCsrfHeaders()
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Network response was not ok " + response.statusText);
        }
        return response.text();
    })
    .then(html => {
        if (!container) {
            alert("Target container not found.");
            return;
        }
        container.innerHTML = html;
        if (containerId === 'ReportContainer') {
            finalizeExpenseReportLoad(container);
        }
        if (typeof attachReportListeners === 'function') {
            let listenerType = lastValue;
            if (lastValue === 'yearSummaryReport') {
                const year = yearEl ? yearEl.value : '';
                listenerType = year ? ('yearSummary|' + year) : 'yearSummary';
            }
            attachReportListeners(containerId === 'ReportContainer'
                ? (document.getElementById('expenseReportCard') || container)
                : container, listenerType);
        }
    })
    .catch(error => {
        console.error("Error fetching report:", error);
        alert("Error fetching report. Please try again.");
    });
}

let selectedExpenseReportUrl = null;

function selectExpenseReport(button) {
    if (!button) {
        return;
    }
    const page = button.closest('.fh-expense-hub, .fh-section-hub');
    if (page) {
        page.querySelectorAll('.fh-hub-nav-item[data-report-url]').forEach(function (tab) {
            tab.classList.remove('active');
        });
    }
    button.classList.add('active');

    selectedExpenseReportUrl = button.getAttribute('data-report-url');
    const title = button.getAttribute('data-report-title') || 'Expense Report';
    const titleEl = document.getElementById('expenseReportTitle');
    const card = document.getElementById('expenseReportCard');
    const yearEl = document.getElementById('year');
    const extra = document.getElementById('expenseReportExtraActions');

    if (titleEl) {
        titleEl.textContent = title;
    }
    showHubPanelMode(false);
    if (card) {
        card.hidden = false;
    }
    if (extra) {
        extra.hidden = true;
        extra.innerHTML = '';
    }
    if (yearEl && !yearEl.value && page) {
        const currentYear = page.getAttribute('data-current-year');
        if (currentYear) {
            yearEl.value = currentYear;
        }
    }
    reloadSelectedExpenseReport();
}

function reloadSelectedExpenseReport() {
    if (!selectedExpenseReportUrl) {
        return;
    }
    loadReport('year', selectedExpenseReportUrl, 'ReportContainer');
}

function finalizeExpenseReportLoad(container) {
    if (!container) {
        return;
    }
    const extra = document.getElementById('expenseReportExtraActions');
    if (!extra) {
        return;
    }
    const actions = container.querySelector('.fh-report-actions, .report-buttons');
    if (actions) {
        extra.innerHTML = '';
        extra.appendChild(actions);
        extra.hidden = false;
    } else {
        extra.hidden = true;
        extra.innerHTML = '';
    }
}

function updateCategoryFileLabel(fileInput) {
    const label = document.getElementById('categoryFileLabel');
    if (!label || !fileInput) {
        return;
    }
    if (fileInput.files && fileInput.files.length > 0) {
        label.textContent = fileInput.files[0].name;
    } else {
        label.textContent = 'No file chosen';
    }
}

function toggleLoanDetails(loanId) {
    const detailRow = document.getElementById(`loan-detail-${loanId}`);
    if (!detailRow) {
        return;
    }
    detailRow.style.display = detailRow.style.display === 'none' || detailRow.style.display === '' ? 'table-row' : 'none';
}

function initAddLoanForm() {
    const emiAmountHidden = document.getElementById('emiAmountHidden');
    const emiAmountInput = document.getElementById('emiAmount');
    const emiDateHidden = document.getElementById('emiDate');
    const emiDateVisible = document.getElementById('emiDateVisible');
    const goldLoanStartDate = document.getElementById('goldLoanStartDate');

    if (emiAmountHidden && emiAmountInput && emiAmountHidden.value && !emiAmountInput.value) {
        emiAmountInput.value = emiAmountHidden.value;
    }
    if (emiDateHidden && emiDateHidden.value) {
        if (emiDateVisible && !emiDateVisible.value) {
            emiDateVisible.value = emiDateHidden.value;
        }
        if (goldLoanStartDate && !goldLoanStartDate.value) {
            goldLoanStartDate.value = emiDateHidden.value;
        }
    }
    const loanType = document.getElementById('loanType');
    const tenureInput = document.getElementById('tenure');
    if (loanType && loanType.value === 'Gold' && tenureInput) {
        tenureInput.value = '12';
    }
    toggleGoldLoanFields();
}

function initLoadedPageContent() {
    if (document.getElementById('addLoanForm')) {
        initAddLoanForm();
    }
    const pageContent = document.getElementById('page-content');
    if (!pageContent) {
        return;
    }
    const hub = getHubRoot();
    const panel = getHubPanel();
    if (hub && panel && panel.dataset.hubReady !== '1') {
        panel.dataset.hubReady = '1';
        const firstNav = hub.querySelector('.fh-hub-nav-item');
        if (firstNav) {
            firstNav.click();
        }
    }
}

function toggleGoldLoanFields() {
    const loanType = document.getElementById('loanType');
    const emiDateGroup = document.getElementById('emiDateGroup');
    const goldLoanStartGroup = document.getElementById('goldLoanStartGroup');
    const emiAmountGroup = document.getElementById('emiAmountGroup');
    const tenureInput = document.getElementById('tenure');
    const emiAmountInput = document.getElementById('emiAmount');
    const emiDateVisible = document.getElementById('emiDateVisible');
    const goldLoanStartDate = document.getElementById('goldLoanStartDate');
    if (!loanType) {
        return;
    }
    const isGold = loanType.value === 'Gold';

    if (emiDateGroup) {
        emiDateGroup.style.display = isGold ? 'none' : '';
    }
    if (goldLoanStartGroup) {
        goldLoanStartGroup.style.display = isGold ? '' : 'none';
    }
    if (emiAmountGroup) {
        emiAmountGroup.style.display = isGold ? 'none' : '';
    }
    if (tenureInput) {
        if (isGold) {
            tenureInput.value = '12';
            tenureInput.readOnly = true;
            tenureInput.classList.add('fh-readonly-field');
        } else {
            tenureInput.readOnly = false;
            tenureInput.classList.remove('fh-readonly-field');
            tenureInput.setAttribute('required', 'required');
        }
    }
    if (emiAmountInput) {
        if (isGold) {
            emiAmountInput.removeAttribute('required');
            emiAmountInput.value = '';
        } else {
            emiAmountInput.setAttribute('required', 'required');
        }
    }
    if (emiDateVisible) {
        if (isGold) {
            emiDateVisible.removeAttribute('required');
        } else {
            emiDateVisible.setAttribute('required', 'required');
        }
    }
    if (goldLoanStartDate && isGold) {
        goldLoanStartDate.setAttribute('required', 'required');
        if (!goldLoanStartDate.value && emiDateVisible && emiDateVisible.value) {
            goldLoanStartDate.value = emiDateVisible.value;
        }
    } else if (goldLoanStartDate) {
        goldLoanStartDate.removeAttribute('required');
        if (emiDateVisible && !emiDateVisible.value && goldLoanStartDate.value) {
            emiDateVisible.value = goldLoanStartDate.value;
        }
    }
    syncAddLoanHiddenFields();
}

function syncAddLoanHiddenFields() {
    const loanType = document.getElementById('loanType');
    const emiDateHidden = document.getElementById('emiDate');
    const emiAmountHidden = document.getElementById('emiAmountHidden');
    const emiAmountInput = document.getElementById('emiAmount');
    const emiDateVisible = document.getElementById('emiDateVisible');
    const goldLoanStartDate = document.getElementById('goldLoanStartDate');
    const isGold = loanType && loanType.value === 'Gold';

    if (emiDateHidden) {
        if (isGold && goldLoanStartDate) {
            emiDateHidden.value = goldLoanStartDate.value || '';
        } else if (emiDateVisible) {
            emiDateHidden.value = emiDateVisible.value || '';
        }
    }
    if (emiAmountHidden) {
        if (isGold) {
            emiAmountHidden.value = '0';
        } else if (emiAmountInput) {
            emiAmountHidden.value = emiAmountInput.value || '';
        }
    }
}

function prefillRecordEmiDate() {
    const loanSelect = document.getElementById('loanIdDisplay') || document.getElementById('loanId');
    const emiNumberInput = document.getElementById('emiNumber');
    const paidOnInput = document.getElementById('paidOn');
    if (!loanSelect || !emiNumberInput || !paidOnInput || paidOnInput.value) {
        return;
    }
    const selectedOption = loanSelect.options[loanSelect.selectedIndex];
    if (!selectedOption) {
        return;
    }
    const firstEmi = selectedOption.getAttribute('data-first-emi');
    const isGoldLoan = selectedOption.getAttribute('data-gold-loan') === 'true';
    const emiNumber = parseInt(emiNumberInput.value, 10);
    if (!firstEmi || Number.isNaN(emiNumber) || emiNumber < 1) {
        return;
    }
    const baseDate = new Date(`${firstEmi}T00:00:00`);
    if (Number.isNaN(baseDate.getTime())) {
        return;
    }
    if (isGoldLoan) {
        baseDate.setMonth(baseDate.getMonth() + (emiNumber * 12));
    } else {
        baseDate.setMonth(baseDate.getMonth() + (emiNumber - 1));
    }
    const y = baseDate.getFullYear();
    const m = String(baseDate.getMonth() + 1).padStart(2, '0');
    const d = String(baseDate.getDate()).padStart(2, '0');
    paidOnInput.value = `${y}-${m}-${d}`;
}

function toggleRecordEmiPreClosure(checkbox) {
    const checked = checkbox && checkbox.checked;
    const section = document.getElementById('preClosureSection');
    const closureType = document.getElementById('preClosureType');
    const preClosureDate = document.getElementById('preClosureDate');
    const preClosureAmount = document.getElementById('preClosureAmount');
    const referenceNumber = document.getElementById('preClosureReferenceNumber');
    if (section) {
        section.style.display = checked ? 'block' : 'none';
    }
    if (preClosureDate) preClosureDate.required = checked;
    if (preClosureAmount) preClosureAmount.required = checked;
    if (referenceNumber) referenceNumber.required = checked;
    if (checked) {
        const paidOnInput = document.getElementById('paidOn');
        if (preClosureDate && !preClosureDate.value && paidOnInput && paidOnInput.value) {
            preClosureDate.value = paidOnInput.value;
        }
    }
    toggleRecordEmiClosureType(closureType);
}

function toggleRecordEmiClosureType(selectEl) {
    const section = document.getElementById('partialClosureSection');
    const preClosureSelected = document.getElementById('preClosureSelected');
    const updatedEmi = document.getElementById('partialUpdatedEmiAmount') || document.getElementById('updatedEmiAmount');
    const updatedTenure = document.getElementById('partialUpdatedTenure') || document.getElementById('updatedTenure');
    const preClosureEnabled = preClosureSelected ? preClosureSelected.checked : true;
    const partial = preClosureEnabled && selectEl && selectEl.value === 'PARTIAL';
    if (section) {
        section.style.display = partial ? 'block' : 'none';
    }
    if (updatedEmi) updatedEmi.required = partial;
    if (updatedTenure) updatedTenure.required = partial;
}

function confirmAndSubmitRecordEmi(event) {
    const preClosureSelected = document.getElementById('preClosureSelected');
    if (preClosureSelected && preClosureSelected.checked) {
        const proceed = window.confirm("Remaining EMIs will be closed/adjusted after pre-closure. Continue?");
        if (!proceed) {
            event.preventDefault();
            return false;
        }
    }
    submitForm(event);
    return false;
}

function persistCategoryOrder(tbody) {
    const ids = [...tbody.querySelectorAll("tr[data-category-id]")]
        .map((r) => parseInt(r.getAttribute("data-category-id"), 10))
        .filter((n) => !Number.isNaN(n));
    if (!ids.length) {
        return;
    }
    const headers = Object.assign(
        { "Content-Type": "application/json" },
        typeof getCsrfHeaders === "function" ? getCsrfHeaders() : {}
    );
    fetch("/api/expenses/categoryReorder", {
        method: "POST",
        headers: headers,
        credentials: "same-origin",
        body: JSON.stringify({ orderedIds: ids }),
    })
        .then((r) => {
            if (!r.ok) {
                throw new Error("Reorder failed");
            }
            if (typeof showHubPage === 'function') {
                const categoriesBtn = document.querySelector(
                    '#page-content .fh-section-hub .fh-hub-nav-item[data-url="/api/expenses/categories"]'
                );
                if (categoriesBtn) {
                    showHubPage(categoriesBtn);
                    return;
                }
            }
            if (typeof loadContent === 'function') {
                loadContent('/api/expenses/categories');
            }
        })
        .catch((err) => console.error(err));
}

(function initExpenseCategoryDrag() {
    let draggedRow = null;
    let orderBeforeDrag = "";

    document.addEventListener("dragstart", function (e) {
        const grip = e.target.closest("#categoryTable .category-drag-grip");
        if (!grip) {
            return;
        }
        const row = grip.closest("tr[data-category-id]");
        const categoryTbody = document.getElementById("categoryTable");
        if (!row || !categoryTbody || !categoryTbody.contains(row)) {
            return;
        }
        draggedRow = row;
        row.classList.add("fh-category-dragging");
        const tbody = row.closest("tbody");
        orderBeforeDrag = [...tbody.querySelectorAll("tr[data-category-id]")]
            .map((r) => r.getAttribute("data-category-id"))
            .join(",");
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", row.getAttribute("data-category-id") || "");
        }
    });

    function onDragOverCategory(e) {
        if (!draggedRow) {
            return;
        }
        const tbody = draggedRow.closest("tbody");
        if (!tbody || !tbody.contains(e.target)) {
            return;
        }
        e.preventDefault();
        if (e.dataTransfer) {
            e.dataTransfer.dropEffect = "move";
        }
        const row = e.target.closest("tr[data-category-id]");
        if (!row || row === draggedRow) {
            return;
        }
        const rect = row.getBoundingClientRect();
        const after = e.clientY > rect.top + rect.height / 2;
        if (after) {
            tbody.insertBefore(draggedRow, row.nextSibling);
        } else {
            tbody.insertBefore(draggedRow, row);
        }
    }

    document.addEventListener("dragover", onDragOverCategory, true);

    document.addEventListener("drop", function (e) {
        if (draggedRow && e.target.closest("#categoryTable")) {
            e.preventDefault();
        }
    });

    document.addEventListener("dragend", function () {
        if (!draggedRow) {
            return;
        }
        draggedRow.classList.remove("fh-category-dragging");
        const tbody = draggedRow.closest("tbody");
        const snapshot = tbody;
        draggedRow = null;
        if (!snapshot || !orderBeforeDrag) {
            orderBeforeDrag = "";
            return;
        }
        const newOrder = [...snapshot.querySelectorAll("tr[data-category-id]")]
            .map((r) => r.getAttribute("data-category-id"))
            .join(",");
        if (newOrder !== orderBeforeDrag) {
            persistCategoryOrder(snapshot);
        }
        orderBeforeDrag = "";
    });
})();