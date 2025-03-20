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
<h2>
<c:choose>
    <c:when test="${not empty expenseDetails}">
        Update Monthly Expenses
    </c:when>
    <c:otherwise>
        Monthly Expenses
    </c:otherwise>
</c:choose>
</h2>
<c:if test="${not empty message}">
        <div style="color: green; text-align: center; font-weight: bold;">
            ${message}
        </div>
</c:if>
<c:if test="${not empty error}">
    <div style="color: red; text-align: center; font-weight: bold;">
        ${error}
    </div>
</c:if>
<form id="expensesForm" method="post" action="/api/expenses/save" onsubmit="submitForm(event)">
<input type="hidden" name="expenseId" value="${expenseDetails != null ? expenseDetails.id : ''}" />
            <div class="form-group">
                <label>Month:</label>
                <input type="month" name="month" required
                       value="${expenseDetails.expenseYear}-${expenseDetails.month lt 10 ? '0' : ''}${expenseDetails.month}">
            </div>

            <div class="form-group">
                <label>Type:</label>
                <input type="radio" name="expenseType" value="plan" id="plan"
                       <c:if test="${fn:toLowerCase(expenseDetails.expenseType) == 'plan'}">checked</c:if>>
                <label for="plan">Plan</label>
                <input type="radio" name="expenseType" value="actual" id="actual"
                       <c:if test="${fn:toLowerCase(expenseDetails.expenseType) == 'actual'}">checked</c:if>>
                <label for="actual">Actual</label>
            </div>

            <div class="grid-container">
                <c:forEach var="category" items="${categories}">
                    <div class="grid-item">
                        <label>
                            <img src="${pageContext.request.contextPath}/${category.icon}" alt="${category.name}" width="20" height="20" style="vertical-align: middle; margin-right: 5px;">
                            ${category.name}
                        </label>
                        <input type="number" class="expenses" name="expenses[${category.id}]"
                               oninput="calculateExpenses()"
                               value="${not empty expenseDetails ? (expenseDetails.expenseType == 'plan' ? expenseDetails.plannedExpenses[category.id] : expenseDetails.actualExpenses[category.id]) : 0}" >
                    </div>
                </c:forEach>
            </div>

            <div class="form-group">
                <label>Total Expenses:</label>
                <input type="number" id="totalExpense" readonly value="${not empty totalExpense ? totalExpense : 0}">
            </div>

            <button type="submit">
                <c:choose>
                    <c:when test="${not empty expenseDetails}">
                        Update Expenses
                    </c:when>
                    <c:otherwise>
                        Save Expenses
                    </c:otherwise>
                </c:choose>
            </button>
        </form>
</div>
</div>
</body>
</html>
