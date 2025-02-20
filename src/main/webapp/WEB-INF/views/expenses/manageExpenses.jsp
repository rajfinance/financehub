<%@ page session="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html>
<head>
    <title>Manage Expenses</title>
</head>
<body>
<div class="container">
<div class="expenses">
<h2>Manage Expenses</h2>
<form action="/expenses/report" method="get">
        <label for="year">Select Year:</label>
        <select name="year" id="year" required>
            <option value="">--Select Year--</option>
            <c:forEach var="yr" items="${years}">
                <option value="${yr}">${yr}</option>
            </c:forEach>
        </select>
        <button type="submit">Show Report</button>
</form>
</div>
</div>
</body>
</html>
