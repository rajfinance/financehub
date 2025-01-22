<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/signup.css">
</head>
<body>
    <%@ include file="/WEB-INF/views/common/header.jsp" %>
    <div class="signup-body">
        <main class="signup">
            <form action="/api/perform_signup" method="post" onsubmit="return validateForm(event);">
                <h2>Signup</h2>

                <!-- Error Message -->
                <c:if test="${not empty error}">
                    <div style="color: red; text-align: center; font-weight: bold; margin-bottom: 10px;">
                        ${error}
                    </div>
                </c:if>

                <!-- Success Message -->
                <c:if test="${not empty success}">
                    <div style="color: green; text-align: center; font-weight: bold; margin-bottom: 10px;">
                        ${success} <a href='/login' style='color: blue;'>Go to Login</a>
                    </div>
                </c:if>

                <!-- Signup Form Fields -->
                <div class="input-group">
                    <label for="username">Username:</label>
                    <input type="text" id="username" name="username" required onchange="validUsername();"/>
                </div>
                <div class="input-group">
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email" required onchange="validEmailId();"/>
                </div>
                <div class="input-group">
                    <label for="password">Password:</label>
                    <input type="password" id="password" name="password" required onchange="validPassword();"/>
                </div>
                <div class="input-group">
                    <label for="confirmPassword">Confirm Password:</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required/>
                </div>
                <button type="submit">Sign Up</button>
            </form>
        </main>
    </div>
</body>
</html>