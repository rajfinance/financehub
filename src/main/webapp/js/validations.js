function isValidDate(dateString) {
        const dateParts = dateString.split('-');

        const year = parseInt(dateParts[0]);
        const month = parseInt(dateParts[1],10);
        const day = parseInt(dateParts[2],10);
        if (year < 2000) return false;

        if (month < 1 || month > 12) return false;

        const monthDays = [31, (isLeapYear(year) ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        return day > 0 && day <= monthDays[month - 1];
}

function isLeapYear(year) {
        return (year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0);
}
function validUsername(){
        const username = document.getElementById('username').value;
        const usernameRegex = /^[a-zA-Z0-9_]+$/;
        if (!usernameRegex.test(username)) {
            alert('Username can only contain alphanumeric characters and underscores (_).');
            document.getElementById('username').style.borderColor = 'red';
            return false;
        }
        document.getElementById('username').style.borderColor = '';
    }
function validEmailId(){
        const email = document.getElementById('email').value;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                alert('Please enter a valid email address.');
                document.getElementById('email').style.borderColor = 'red';
                return false;
            }
        document.getElementById('email').style.borderColor = '';
    }
    function validPassword(){
        const password = document.getElementById('password').value;
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$#!*?&])[A-Za-z\d@#$!*?&]{8,15}$/;
        if (password.length < 8 && password.length < 15) {
            alert('Password must be at least 8 characters long and maximum 15 characters long');
            document.getElementById('password').value = "";
            document.getElementById('password').style.borderColor = 'red';
            return false;
        }
        if (!passwordRegex.test(password)) {
            alert('Password must contain:\n- At least one lowercase letter (a-z)\n- At least one uppercase letter (A-Z)\n- At least one numeric digit (0-9)\n- At least one special character in(@,$,!,*,?,#,&)');
            document.getElementById('password').style.borderColor = 'red';
            document.getElementById('password').value = "";
            return false;
        }
        document.getElementById('password').style.borderColor = '';
    }
    function validateForm(event) {
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        // Check if passwords match
        if (password !== confirmPassword) {
            alert('Passwords do not match.');
            event.preventDefault(); // Prevent form submission
            return false;
        }
        return true; // Allow form submission
    }

function validForm(formId){
    if(formId == "addExperience"){
        return validateExperienceForm();
    }
    else if(formId == "addSalaryForm"){
        return validateSalaryForm();
    }
    else if(formId=="addOwner"){
        return validateRentForm();
    }
    else if(formId=="addRentPayment"){
        return validateRentPaymentForm();
    }
}
function validateExperienceForm() {
        const companyName = document.getElementById("companyName").value.trim();
        const clientName = document.getElementById("clientName").value.trim();
        const projectName = document.getElementById("projectName").value.trim();
        const fromDate = document.getElementById("fromDate").value;
        const toDate = document.getElementById("toDate").value;
        const currentlyEmployed = document.getElementById("currentlyEmployed").checked;
        const alphanumericRegex = /^[a-zA-Z0-9\s.]+$/;
        if (!companyName || !clientName || !projectName || !fromDate) {
            alert("Please fill in all mandatory fields marked with *.");
            return false;
        }
        if (!alphanumericRegex.test(companyName)) {
            alert("Company Name can only contain alphanumeric characters and spaces.");
            return false;
        }
        if (!alphanumericRegex.test(clientName)) {
            alert("Client Name can only contain alphanumeric characters and spaces.");
            return false;
        }
        if (!alphanumericRegex.test(projectName)) {
            alert("Project Name can only contain alphanumeric characters and spaces.");
            return false;
        }
        if (!isValidDate(fromDate)) {
            alert("Experience From (Date) must be a valid date (mm/dd/yyyy) from 2000 onwards.");
            return false;
        }
        if (!currentlyEmployed) {
                    if (!isValidDate(toDate)) {
                        alert("Experience To (Date) must be a valid date (mm/dd/yyyy) from 2000 onwards.");
                        return false;
                    }

                    if (new Date(toDate) < new Date(fromDate)) {
                        alert("Experience To (Date) must be after Experience From (Date).");
                        return false;
                    }
        }

        return true;
        }

function toggleToDate() {
        const checkbox = document.getElementById("currentlyEmployed");
        const toDateField = document.getElementById("toDate");

        if (checkbox.checked) {
            toDateField.value = "";
            toDateField.disabled = true;
        } else {
            toDateField.disabled = false;
        }
}
function validateSalaryForm() {
        const salaryAmount = document.getElementById('salaryAmount').value;
        const dateInput = document.getElementById("dateCredited").value;
        if (!isValidDate(dateInput)) {
            alert("Date Credited must be a valid date (mm/dd/yyyy) from 2000 onwards.");
            return false;
        }
        if (!salaryAmount || salaryAmount <= 0) {
            alert('Please enter a valid salary amount greater than 0.');
            return false;
        }
        return true;
}
function validateRentForm() {
                let isValid = true;
                let errorMessages = [];

                const name = document.getElementById("name");
                const phone = document.getElementById("phone");
                const address = document.getElementById("address");
                const advanceMonths = document.getElementById("advanceMonths");
                const advance = document.getElementById("advance");
                const date = document.getElementById("date");
                const today = new Date().toISOString().split("T")[0];
                if (new Date(date.value) > new Date(today)) {
                    errorMessages.push("Advance date cannot be in the future.");
                    isValid = false;
                }

                if (!name.value.trim()) {
                    errorMessages.push("Owner Name is required.");
                    isValid = false;
                }

                if (!/^[6-9]\d{9}$/.test(phone.value.trim())) {
                    errorMessages.push("Enter a valid 10-digit phone number.");
                    isValid = false;
                }

                if (!address.value.trim()) {
                    errorMessages.push("Address is required.");
                    isValid = false;
                }
                if (!advanceMonths.value || parseInt(advanceMonths.value) <= 0) {
                    errorMessages.push("Advance Months must be a positive number.");
                    isValid = false;
                }
                if (!advance.value || parseInt(advance.value) <= 0) {
                    errorMessages.push("Advance amount must be a positive number.");
                    isValid = false;
                }
                if (!date.value) {
                    errorMessages.push("Advance date is required.");
                    isValid = false;
                }

                if (!isValid) {
                    alert(errorMessages.join("\n"));
                    return false;
                } else {
                    return true;
                }
}
function validateRentPaymentForm() {
                const start = document.getElementById("start");
                const end = document.getElementById("end");
                const paidOn = document.getElementById("paidOn");

                const startDate = new Date(start.value);
                const endDate = new Date(end.value);
                const paidOnDate = new Date(paidOn.value);

                if (startDate >= endDate) {
                    alert("Rent Period Start Date must be before Rent Period End Date.");
                    return false;
                }

                if (paidOnDate <= endDate) {
                    alert("Paid On Date must be after Rent Period End Date.");
                    return false;
                }

                return true;
}
