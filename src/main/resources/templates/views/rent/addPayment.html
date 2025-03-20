<%@ page session="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html lang="en">
<head>
    <title><h1>${not empty payment.id ? 'Edit Payment Details' : 'Add Payment Details'}</h1></title>
</head>
<body>
<div class="form-container">
    <h1>${not empty payment.id ? 'Edit Payment Details' : 'Add Payment Details'}</h1>
    <c:if test="${not empty successMessage}">
        <div style="color: green; text-align: center; font-weight: bold; margin-left: 270px;">
            ${successMessage}
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="error">
            <c:forEach var="error" items="${errorMessage}">
                <div style="color: red; text-align: center; font-weight: bold;">
                    ${error}
                </div><br>
            </c:forEach>
        </div>
    </c:if>

    <form id="addRentPayment" action="/api/rent/addPayments" method="post" onsubmit="submitForm(event)">
            <c:if test="${payment.id != null}">
                <input type="hidden" name="paymentId" value="${payment.id}">
            </c:if>

            <div class="input-group">
                <label for="ownerId">Owner ID:<span class="mandatory">*</span>:</label></label>
                <select id="ownerId" name="ownerId" required>
                    <option value="">Select an Owner</option>
                    <c:forEach var="owner" items="${owners}">
                        <option value="${owner.ownerId}" <c:if test="${payment.ownerId == owner.ownerId}">selected</c:if>>${owner.name}</option>
                    </c:forEach>
                </select><br><br>
            </div>

            <div class="input-group">
                <label for="start">Rent Period Start:<span class="mandatory">*</span>:</label></label>
                <input type="date" id="start" name="rentPeriodStart" value="${payment.rentPeriodStart}" required><br><br>
            </div>

            <div class="input-group">
                <label for="end">Rent Period End:<span class="mandatory">*</span>:</label></label>
                <input type="date" id="end" name="rentPeriodEnd" value="${payment.rentPeriodEnd}" required><br><br>
            </div>

            <div class="input-group">
                <label for="paidOn">Paid On:<span class="mandatory">*</span>:</label></label>
                <input type="date" id="paidOn" name="paidOn" value="${payment.paidOn}" required><br><br>
            </div>

            <div class="input-group">
                <label for="amount">Amount:<span class="mandatory">*</span>:</label></label>
                <input type="number" id="amount" name="amount" value="${payment.amount}" required><br><br>
            </div>

            <button type="submit">${not empty payment.id ? 'Update' : 'Submit'}</button>
        </form>
</div>
</body>
</html>
