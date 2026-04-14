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
    loadDashboardCharts();
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

function loadDashboardCharts() {
    const years = Object.keys(yearlySalaryData).slice(-5);
    const salaryValues = Object.values(yearlySalaryData).slice(-5);
    const expenseValues = Object.values(yearlyExpenseData).slice(-5);
        var ctx2 = document.getElementById("yearlySalaryChart").getContext("2d");
        var salaryChart = new Chart(ctx2, {
            type: 'bar',
            data: {
                labels: years,
                datasets: [{
                    label: "Yearly Salary",
                    data: salaryValues,
                    backgroundColor: "#34A853"
                },
                {
                    label: "Yearly Expenses",
                    data: expenseValues,
                    backgroundColor: "#EA4335"
                }]
            }
        });

        var ctx3 = document.getElementById("rentChart").getContext("2d");
        const rentYears = Object.keys(yearlyRentData).slice(-5);
        const rentValues = Object.values(yearlyRentData).slice(-5);
        var rentChart = new Chart(ctx3, {
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

        var ctx4 = document.getElementById("salaryExpenseChart").getContext("2d");
        var salaryChart = new Chart(ctx4, {
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