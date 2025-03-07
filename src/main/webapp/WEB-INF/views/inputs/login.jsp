<!DOCTYPE html>
<html lang="en">
<head>
    <script>
        function validateForm() {
            const username = document.getElementById("username").value;
            const password = document.getElementById("password").value;

            if (username === "") {
                alert("Username cannot be empty");
                return false;
            }
            if (password === "") {
                alert("Password cannot be empty");
                return false;
            }
            return true;
        }
    </script>
</head>
<body>
    <%@ include file="/WEB-INF/views/common/header.jsp" %>
    <div class="login-page">
        <main class="login-container">
            <div class="login-form">
                <h1>Login</h1>

                <c:if test="${not empty error}">
                    <div style="color: red; text-align: center; font-weight: bold;">
                        ${error}
                    </div>
                </c:if>

                <form action="/api/perform_login" method="post" onsubmit="return validateForm()">
                    <div class="input-group">
                        <label for="username">Username:</label>
                        <input type="text" id="username" name="username" required>
                    </div>
                    <div class="input-group">
                        <label for="password">Password:</label>
                        <input type="password" id="password" name="password" required>
                    </div>
                    <button type="submit">Login</button>
                </form>
                <div class="signup-link">
                    <p>Don't have an account? <a href="/signup">Sign Up</a></p>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
