<%@ page session="true" %>
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
<h2>Monthly Expenses</h2>
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
               <c:forEach var="category" items="${categories}">
                    <div class="grid-item">
                        <label>
                            <img src="${pageContext.request.contextPath}/${category.icon}" alt="${category.name}" width="20" height="20" style="vertical-align: middle; margin-right: 5px;">
                                ${category.name}
                        </label>
                        <input type="number" class="expense" name="${fn:toLowerCase(fn:replace(category.name, ' ', '_'))}" oninput="calculateExpenses()">
                    </div>
                </c:forEach>
            </div>
            <div class="form-group">
                <label>Total Expenses:</label>
                <input type="number" id="totalExpense" readonly>
            </div>
            <button type="submit">Save Expenses</button>
        </form>
        </div>
</div>
</body>
</html>
