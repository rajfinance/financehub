document.addEventListener("DOMContentLoaded", function () {
    const dashboardElement = document.querySelector('[data-page="dashboard"]');
    if (dashboardElement) {
        const mainContent = document.getElementById('mainContent');
        if (mainContent) {
            mainContent.style.display = 'none';
        }
        loadDashboardCharts();
    }
});

function loadDashboardCharts() {

//        var ctx1 = document.getElementById("monthlySalaryChart").getContext("2d");
//        var salaryChart = new Chart(ctx1, {
//            type: 'bar',
//            data: {
//                labels: Object.keys(monthlySalaryData),
//                datasets: [{
//                    label: "Monthly Salary",
//                    data: Object.values(monthlySalaryData),
//                    backgroundColor: "#4285F4"
//                }]
//            }
//        });

        var ctx2 = document.getElementById("yearlySalaryChart").getContext("2d");
        var salaryChart = new Chart(ctx2, {
            type: 'bar',
            data: {
                labels: Object.keys(yearlySalaryData),
                datasets: [{
                    label: "Yearly Salary",
                    data: Object.values(yearlySalaryData),
                    backgroundColor: "#34A853"
                },
                {
                    label: "Yearly Expenses",
                    data: Object.values(yearlyExpenseData),
                    backgroundColor: "#EA4335"
                }]
            }
        });

        var ctx3 = document.getElementById("rentChart").getContext("2d");
        var salaryChart = new Chart(ctx3, {
            type: 'bar',
            data: {
                labels: Object.keys(yearlyRentData),
                datasets: [{
                    label: "Yearly Rent Paid",
                    data: Object.values(yearlyRentData),
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

        var backgroundColors = Object.keys(categoryData).map((_, index) =>
            `hsl(${(index * 45) % 360}, 70%, 50%)`
        );
//        var ctx5 = document.getElementById("expenseCategoryChart").getContext("2d");
//        var salaryChart = new Chart(ctx5, {
//            type: 'bar',
//            data: {
//                    labels: Object.keys(categoryData),
//                    datasets: [{
//                        label: "Category-wise Expenses",
//                        data: Object.values(categoryData),
//                        backgroundColor: backgroundColors
//                    }]
//            }
//        });
}