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

    const years = Object.keys(yearlySalaryData || {}).slice(-5);

    const salaryValues = Object.values(yearlySalaryData || {}).slice(-5);

    const expenseValues = Object.values(yearlyExpenseData || {}).slice(-5);



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

        const rentYears = Object.keys(yearlyRentData || {}).slice(-5);

        const rentValues = Object.values(yearlyRentData || {}).slice(-5);

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

                labels: Object.keys(salaryData || {}),

                datasets: [

                    {

                        label: "Salary",

                        data: Object.values(salaryData || {}),

                        borderColor: "#34A853",

                        fill: false

                    },

                    {

                        label: "Expenses",

                        data: Object.values(expenseData || {}),

                        borderColor: "#EA4335",

                        fill: false

                    }

                ]

            }

        });

    }

}


