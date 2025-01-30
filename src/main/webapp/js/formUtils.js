function loadPage(submenuId) {
       const allSubmenus = document.querySelectorAll('.sidebar');
       const pageContent = document.getElementById('page-content');
       allSubmenus.forEach(function(submenu) {
           submenu.style.display = 'none';
       });
        const allMenuItems = document.querySelectorAll('.menu-bar ul li');
        allMenuItems.forEach(function(menuItem) {
            menuItem.classList.remove('active');
        });
        const clickedMenuItem = Array.from(allMenuItems).find(item => {
                return item.querySelector('a').getAttribute('onclick').includes(submenuId);
        });
        if (clickedMenuItem) {
                clickedMenuItem.classList.add('active');
        }

        const selectedSubmenu = document.getElementById(submenuId);
        if(submenuId!='home') selectedSubmenu.style.display = 'block';
        fetch('/' + submenuId)
            .then(response => response.text())
            .then(html => {
                pageContent.innerHTML = html;
            })
            .catch(error => {
                console.error('Error loading content:', error);
                pageContent.innerHTML = '<p>There was an error loading the content.</p>';
            });
}
function loadContent(apiUrl) {
    const pageContent = document.getElementById('page-content');
    fetch(apiUrl)
        .then(response => response.text())
        .then(html => {
            pageContent.innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading content:', error);
            pageContent.innerHTML = "<p>There was an error loading the page.</p>";
        });
}
function submitForm(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const formId = form.id;
        console.log(formId);
        if(!validForm(formId)){
            return;
        }

        const formData = new FormData(form);
        fetch(form.action, {
            method: 'POST',
            body: formData
        })
        .then(response => response.text())
        .then(html => {
            document.getElementById('page-content').innerHTML = html;
        })
        .catch(error => {
            console.error('Error submitting form:', error);
            document.getElementById('page-content').innerHTML = "<p>There was an error submitting the form. Please try again.</p>";
        });
}
function loadEditPageContent(url) {
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch content');
            }
            return response.text();
        })
        .then(html => {
            document.getElementById('page-content').innerHTML = html;
        })
        .catch(error => {
            console.error('Error submitting form:', error);
            document.getElementById('page-content').innerHTML = "<p>There was an error submitting the form. Please try again.</p>";
        });
}
function deleteEntity(anchor, entityType, apiEndpoint) {
    var entityId = anchor.getAttribute('data-id');
    var reportType = anchor.getAttribute('data-report-type');
    if (confirm(`Are you sure you want to delete this ${entityType}?`)) {
        fetch(`${apiEndpoint}?id=${entityId}`, {
            method: 'DELETE'
        })
        .then(response => {
            return response.text().then(data => {
                if (response.status === 409) {
                    alert(data);
                } else if (response.ok) {
                    if (data === "success") {
                        alert(`${entityType.charAt(0).toUpperCase() + entityType.slice(1)} deleted successfully`);
                        fetchReportContent(reportType);
                    } else {
                        alert(`Error deleting ${entityType}`);
                    }
                } else {
                    alert(`Error: ${data}`);
                }
            });
        })
        .catch(error => {
            alert('Error: ' + error.message);
        });
    }
}

function activateTab(event) {
    const tabs = document.querySelectorAll(".tab-btn");
    const contents = document.querySelectorAll(".tab-content");

    tabs.forEach(t => t.classList.remove("active"));
    contents.forEach(c => c.classList.remove("active"));

    event.target.classList.add("active");
    const targetTab = document.getElementById(event.target.getAttribute("data-tab"));
    if (targetTab) targetTab.classList.add("active");
}

function calculateExpenses() {
        let total = 0;
        document.querySelectorAll(".expense").forEach(input => {
            total += parseFloat(input.value) || 0;
        });
        document.getElementById("totalExpense").value = total;

        let income = parseFloat(document.getElementById("income").value) || 0;
        document.getElementById("remaining").value = income - total;
}
