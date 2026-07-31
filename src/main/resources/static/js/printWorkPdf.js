function showReport(reportType) {
    const pageContent = document.getElementById('page-content') || document;
    const scope = pageContent.querySelector('.content')
        ? pageContent
        : document;
    scope.querySelectorAll('.tab-content').forEach(function (report) {
        report.style.display = 'none';
    });
    const target = document.getElementById(reportType);
    if (target) {
        target.style.display = 'block';
    }
    const tabRoot = scope.querySelector('.content .tabs') || scope;
    tabRoot.querySelectorAll('.tab-button').forEach(function (tab) {
        tab.classList.remove('active');
    });
    const activeTab = tabRoot.querySelector('[onclick*="showReport(\'' + reportType + '\')"]')
        || document.querySelector('[onclick*="showReport(\'' + reportType + '\')"]');
    if (activeTab) {
        activeTab.classList.add('active');
    }
    fetchReportContent(reportType);
}

function prepareUrl(reportType) {
    let year = null;
    let type = reportType;
    if (reportType.includes('_')) {
        const parts = reportType.split('_');
        type = parts[0];
        year = parts[1];
    }
    let baseUrl = '';
    if (type === 'expReport' || type === 'salaryReport') {
        baseUrl = '/api/work/' + type;
    } else if (type === 'ownersReport' || type === 'rentPaymentReport') {
        baseUrl = '/api/rent/' + type;
    } else if (type === 'manageReport') {
        baseUrl = '/api/expenses/' + type;
    } else if (type === 'loansReport' || type === 'loanEmiReport' || type === 'loanBankProjectionReport') {
        baseUrl = '/api/loan/' + type;
    }
    if (type === 'loanEmiReport') {
        const yearEl = document.getElementById('emiScheduleYear');
        const loanEl = document.getElementById('emiScheduleLoan');
        const emiYear = yearEl ? yearEl.value : new Date().getFullYear();
        let url = baseUrl + '?year=' + encodeURIComponent(emiYear);
        if (loanEl && loanEl.value) {
            url += '&loanId=' + encodeURIComponent(loanEl.value);
        }
        return url;
    }
    if (type === 'loanBankProjectionReport') {
        return buildLoanBankProjectionUrl(baseUrl);
    }
    return year ? (baseUrl + '?year=' + year) : baseUrl;
}

function buildLoanBankProjectionUrl(baseUrl) {
    const params = new URLSearchParams();
    const hasLoanGrid = document.getElementById('projectionLoanGrid');
    if (hasLoanGrid) {
        params.set('applied', 'true');
        document.querySelectorAll('.projection-loan-cb:checked').forEach(function (cb) {
            params.append('loanIds', cb.value);
        });
        document.querySelectorAll('.projection-combo-cb:checked').forEach(function (cb) {
            params.append('combo', cb.value);
        });
    }
    const query = params.toString();
    return query ? `${baseUrl}?${query}` : baseUrl;
}

function toggleAllProjectionLoans(selectAll) {
    document.querySelectorAll('.projection-loan-cb').forEach(function (cb) {
        cb.checked = !!selectAll;
    });
}

function reloadLoanBankProjectionReport() {
    const container = document.getElementById('loanBankProjectionReport')
        || (typeof getHubPanel === 'function' ? getHubPanel() : null);
    if (!container) {
        return;
    }
    const url = buildLoanBankProjectionUrl('/api/loan/loanBankProjectionReport');
    fetch(url, {
        credentials: 'same-origin',
        headers: typeof getCsrfHeaders === 'function' ? getCsrfHeaders() : {}
    })
        .then(function (response) { return response.text(); })
        .then(function (html) {
            container.innerHTML = html;
        })
        .catch(function (error) {
            console.error('Error loading EMI projection report:', error);
        });
}

function fetchReportContent(reportType) {
    const url = prepareUrl(reportType);
    let listenerType = reportType;
    let containerId = reportType;
    if (reportType.includes('_')) {
        const parts = reportType.split('_');
        listenerType = parts[0];
        containerId = parts[2] || parts[0];
    }
    const reportContainer = document.getElementById(containerId)
        || (typeof getHubPanel === 'function' ? getHubPanel() : null);
    if (!reportContainer) {
        console.error('Report container not found for', reportType);
        return;
    }
    fetch(url, {
        credentials: 'same-origin',
        headers: typeof getCsrfHeaders === 'function' ? getCsrfHeaders() : {}
    })
        .then(function (response) { return response.text(); })
        .then(function (data) {
            reportContainer.innerHTML = data;
            attachReportListeners(reportContainer, containerId === 'ReportContainer' ? listenerType : (containerId === reportType ? reportType : listenerType));
        })
        .catch(function (error) {
            console.error('Error fetching report:', error);
            reportContainer.innerHTML = '<p>Error loading report. Please try again.</p>';
        });
}

function attachReportListeners(reportContainer, reportType) {
    if (!reportContainer) {
        return;
    }
    if (reportType === 'salaryReport') {
        initSalaryYearPanels(reportContainer);
    }
    const downloadPdfButton = reportContainer.querySelector('#downloadPdf');
    const printReportButton = reportContainer.querySelector('#printReport');
    if (downloadPdfButton && downloadPdfButton.dataset.bound !== '1') {
        downloadPdfButton.dataset.bound = '1';
        downloadPdfButton.addEventListener('click', function () {
            let pdfParam = reportType;
            if (reportType === 'yearSummaryReport' || reportType.indexOf('yearSummary') === 0) {
                if (reportType.indexOf('|') === -1) {
                    const yearEl = document.getElementById('year');
                    pdfParam = 'yearSummary|' + (yearEl ? yearEl.value : '');
                }
            }
            downloadPdf(pdfParam);
        });
    }
    if (printReportButton && printReportButton.dataset.bound !== '1') {
        printReportButton.dataset.bound = '1';
        printReportButton.addEventListener('click', function () {
            const rows = reportContainer.querySelectorAll('table tbody tr');
            const headers = reportContainer.querySelectorAll('table thead tr');
            const reportButtons = reportContainer.querySelectorAll('.report-buttons, .fh-report-actions');
            reportButtons.forEach(function (btn) { btn.style.display = 'none'; });
            if (reportType === 'salaryReport') {
                reportContainer.querySelectorAll('details.fh-year-panel').forEach(function (panel) {
                    panel.open = true;
                });
            }
            var chkLength = 0;
            if (reportType === 'expReport') {
                chkLength = 5;
            } else if (reportType === 'salaryReport' || reportType === 'rentPaymentReport') {
                chkLength = 4;
            } else if (reportType === 'ownersReport') {
                chkLength = 6;
            }
            rows.forEach(function (row) {
                const cells = row.children;
                if (chkLength && cells.length > chkLength) {
                    cells[chkLength].style.display = 'none';
                }
            });
            headers.forEach(function (headerRow) {
                const headerCells = headerRow.children;
                if (chkLength && headerCells.length > chkLength) {
                    headerCells[chkLength].style.display = 'none';
                }
            });

            const printWindow = window.open('', '', 'height=600,width=800');
            printWindow.document.write('<html><head><title>' + reportType + '</title></head><body>');
            printWindow.document.write(reportContainer.innerHTML);
            printWindow.document.write('</body></html>');
            printWindow.document.close();
            printWindow.print();

            reportButtons.forEach(function (btn) { btn.style.display = ''; });
            rows.forEach(function (row) {
                const cells = row.children;
                if (chkLength && cells.length > chkLength) {
                    cells[chkLength].style.display = '';
                }
            });
            headers.forEach(function (headerRow) {
                const headerCells = headerRow.children;
                if (chkLength && headerCells.length > chkLength) {
                    headerCells[chkLength].style.display = '';
                }
            });
        });
    }
}

function reloadLoanEmiReport() {
    const yearEl = document.getElementById('emiScheduleYear');
    const loanEl = document.getElementById('emiScheduleLoan');
    const y = yearEl ? yearEl.value : String(new Date().getFullYear());
    const container = document.getElementById('loanEmiReport')
        || (typeof getHubPanel === 'function' ? getHubPanel() : null);
    if (!container) {
        return;
    }
    let url = '/api/loan/loanEmiReport?year=' + encodeURIComponent(y || new Date().getFullYear());
    if (loanEl && loanEl.value) {
        url += '&loanId=' + encodeURIComponent(loanEl.value);
    }
    fetch(url, {
        credentials: 'same-origin',
        headers: typeof getCsrfHeaders === 'function' ? getCsrfHeaders() : {}
    })
        .then(function (response) { return response.text(); })
        .then(function (html) {
            container.innerHTML = html;
            if (typeof attachReportListeners === 'function') {
                attachReportListeners(container, 'loanEmiReport');
            }
        })
        .catch(function (error) {
            console.error('Error loading loan EMI report:', error);
        });
}

function downloadPdf(param) {
    let parts = param.split("|");
    let reportType = parts[0];
    let year = parts.length > 1 ? parts[1] : "";

    let apiUrl = `/api/pdf/${reportType}Pdf`;
    if (year) {
        apiUrl += `?year=${year}`;
    }

    fetch(apiUrl, {
        method: 'GET',
        headers: {
            'Accept': 'application/pdf',
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.blob();
    })
    .then(blob => {
        const fileName = year ? `${reportType}_${year}.pdf` : `${reportType}.pdf`;
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
    })
    .catch(error => {
        console.error('Error downloading PDF:', error);
    });
}

/** Keep current-year salary panel always expanded; other years stay collapsible. */
function initSalaryYearPanels(reportContainer) {
    if (!reportContainer) {
        return;
    }
    reportContainer.querySelectorAll('details.fh-year-panel--current').forEach(panel => {
        panel.open = true;
        panel.addEventListener('toggle', function () {
            if (!panel.open) {
                panel.open = true;
            }
        });
    });
}
