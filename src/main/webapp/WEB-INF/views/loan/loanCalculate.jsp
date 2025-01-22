<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="container">
    <div class="form-container">
        <h1>Loan Calculator</h1>
        <form action="/calculate" method="post">
            <div class="form-group">
                <label for="axis">AXIS:</label>
                <input type="number" id="axis" name="axis" required oninput="calculateLoanTotal()">
                <span id="axisResult" class="result"></span> <!-- AXIS Result display -->
            </div>
            <div class="form-group">
                <label for="icici">ICICI:</label>
                <input type="number" id="icici" name="icici" required oninput="calculateLoanTotal()">
            </div>
            <div class="form-group">
                <label for="hdfc">HDFC:</label>
                <input type="number" id="hdfc" name="hdfc" required oninput="calculateLoanTotal()">
                <span id="hdfcResult" class="result"></span> <!-- HDFC Result display -->
            </div>

            <!-- Loan Total (Initially hidden) -->
            <div class="form-group" id="loanTotalContainer">
                <span id="loanTotal" class="loantotal"></span>
            </div>

            <div class="form-group">
                <label for="cc">CC:</label>
                <input type="number" id="cc" name="cc" required>
            </div>
            <div class="form-group">
                <label for="givnamnt">Given Amount:</label>
                <input type="number" id="givnamnt" name="givnamnt" required>
            </div>

            <div class="form-group">
                <button type="submit" onclick="hideLoanTotal()">Calculate</button>
            </div>
        </form>
    </div>
    <div class="result-container">
        <c:if test="${not empty result}">
            <h2>${result}</h2>
        </c:if>
    </div>
</div>
