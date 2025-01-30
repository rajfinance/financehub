<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html>
<head>
    <title>Monthly Income & Expenses</title>
</head>
<body>
<div class="container">
<div class="expenses">
<h2>Monthly Income & Expenses</h2>
    <div class="tab-buttons">
        <button class="tab-btn active" data-tab="income-tab" onclick="activateTab(event)">Income</button>
        <button class="tab-btn" data-tab="expenses-tab" onclick="activateTab(event)">Expenses</button>
    </div>
    <div id="income-tab" class="tab-content active">
        <form action="saveIncome.jsp" method="post">
            <div class="form-group">
                <label>Month:</label>
                <input type="month" name="month" required>
            </div>
            <div class="form-group">
                <label>Monthly Income:</label>
                <input type="number" id="income" name="income" required>
            </div>
            <button type="submit">Save Income</button>
        </form>
    </div>
    <div id="expenses-tab" class="tab-content">
        <form action="saveExpenses.jsp" method="post">
            <div class="form-group">
                <label>Month:</label>
                <input type="month" name="month" required>
            </div>
            <div class="form-group">
                <label>Type:</label>
                <input type="radio" name="expenseType" value="plan" id="plan" checked>
                <label for="plan">Plan</label>
                <input type="radio" name="expenseType" value="actual" id="actual">
                <label for="actual">Actual</label>
            </div>
            <div class="grid-container">
                <c:set var="categories" value="STOCK INVEST,LOAN,CREDIT CARD,AMMA,INSURANCE,RENT,KIRANA,VEGGIES & FRUITS,MEAT,SHOPPING,MEDICAL,ELECTRICITY,HOME NEEDS,BIKE,TRANSPORTATION,SUBSCRIPTION,DINE OUT & MOVIES,OTHERS" />
                <c:forEach var="category" items="${fn:split(categories, ',')}">
                    <div class="grid-item">
                        <label>${category}</label>
                        <input type="number" class="expense" name="${category.toLowerCase().replace(' ', '_')}" oninput="calculateExpenses()">
                    </div>
                </c:forEach>
            </div>
            <div class="form-group">
                <label>Total Expenses:</label>
                <input type="number" id="totalExpense" readonly>
            </div>
            <div class="form-group">
                <label>Remaining Balance:</label>
                <input type="number" id="remaining" readonly>
            </div>
            <button type="submit">Save Expenses</button>
        </form>
    </div>
</div>
</div>
</body>
</html>
