document.addEventListener("DOMContentLoaded", function () {

    const dashboardElement = document.querySelector('[data-page="dashboard"]');

    if (!dashboardElement) {

        return;

    }

    let pending = null;

    try {

        pending = sessionStorage.getItem('fhPendingLoad');

    } catch (e) {

        pending = null;

    }

    if (pending && typeof pending === 'string' && pending.startsWith('/') && typeof loadContent === 'function') {

        try {

            sessionStorage.removeItem('fhPendingLoad');

        } catch (e) {

            /* ignore */

        }

        loadContent(pending);

        return;

    }

    const pageContent = document.getElementById('page-content');

    if (pageContent) {

        pageContent.innerHTML = '';

    }

    const dashboardContent = document.getElementById('dashboardContent');

    if (dashboardContent) {

        dashboardContent.style.display = '';

    }

    const mainContent = document.getElementById('mainContent');

    if (mainContent) {

        mainContent.style.display = 'none';

    }

    loadDashboardCharts(dashboardElement);

});



window.addEventListener('pageshow', function (event) {

    if (!event.persisted) {

        return;

    }

    const dashboardElement = document.querySelector('[data-page="dashboard"]');

    if (!dashboardElement) {

        return;

    }

    const pageContent = document.getElementById('page-content');

    if (pageContent) {

        pageContent.innerHTML = '';

    }

    const dashboardContent = document.getElementById('dashboardContent');

    if (dashboardContent) {

        dashboardContent.style.display = '';

    }

    const mainContent = document.getElementById('mainContent');

    if (mainContent) {

        mainContent.style.display = 'none';

    }

});



function loadDashboardCharts(dashboardElement) {

    const apiUrl = dashboardElement.getAttribute('data-chart-api') || '/api/home/chart-data';

    fetch(apiUrl, { credentials: 'same-origin' })

        .then(function (response) {

            if (!response.ok) {

                throw new Error('Failed to load dashboard chart data');

            }

            return response.json();

        })

        .then(function (data) {

            updatePendingEmiKpi(data.pendingEmi);

            renderDashboardCharts(data);

        })

        .catch(function () {

            updatePendingEmiKpi(0);

        });

}



function updatePendingEmiKpi(amount) {

    const pendingEl = document.getElementById('kpiPendingEmi');

    if (!pendingEl) {

        return;

    }

    pendingEl.classList.remove('fh-kpi-loading');

    const value = Number(amount);

    pendingEl.textContent = Number.isFinite(value) ? formatDashboardInteger(value) : '0';

}



function formatDashboardInteger(value) {

    return Math.round(value).toLocaleString('en-IN');

}



function renderDashboardCharts(data) {

    const yearlySalaryData = data.yearlySalaryData || {};

    const yearlyExpenseData = data.yearlyExpenseData || {};

    const yearlyRentData = data.yearlyRentData || {};

    const salaryData = data.salaryData || data.monthlySalaryData || {};

    const expenseData = data.expenseData || {};



    const years = Object.keys(yearlySalaryData).slice(-5);

    const salaryValues = Object.values(yearlySalaryData).slice(-5);

    const expenseValues = Object.values(yearlyExpenseData).slice(-5);



    const yearlySalaryCanvas = document.getElementById("yearlySalaryChart");

    if (yearlySalaryCanvas) {

        new Chart(yearlySalaryCanvas.getContext("2d"), {

            type: 'bar',

            data: {

                labels: years,

                datasets: [{

                    label: "Yearly Salary",

                    data: salaryValues,

                    backgroundColor: "#34A853"

                }, {

                    label: "Yearly Expenses",

                    data: expenseValues,

                    backgroundColor: "#EA4335"

                }]

            }

        });

    }



    const rentCanvas = document.getElementById("rentChart");

    if (rentCanvas) {

        const rentYears = Object.keys(yearlyRentData).slice(-5);

        const rentValues = Object.values(yearlyRentData).slice(-5);

        new Chart(rentCanvas.getContext("2d"), {

            type: 'bar',

            data: {

                labels: rentYears,

                datasets: [{

                    label: "Yearly Rent Paid",

                    data: rentValues,

                    backgroundColor: "#D32F2F"

                }]

            }

        });

    }



    const monthlyCanvas = document.getElementById("salaryExpenseChart");

    if (monthlyCanvas) {

        new Chart(monthlyCanvas.getContext("2d"), {

            type: 'line',

            data: {

                labels: Object.keys(salaryData),

                datasets: [

                    {

                        label: "Salary",

                        data: Object.values(salaryData),

                        borderColor: "#34A853",

                        fill: false

                    },

                    {

                        label: "Expenses",

                        data: Object.values(expenseData),

                        borderColor: "#EA4335",

                        fill: false

                    }

                ]

            }

        });

    }

}

