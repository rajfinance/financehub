function showReport(reportType) {
    const allReports = document.querySelectorAll('.tab-content');
    allReports.forEach(report => {
        report.style.display = 'none';
    });

    document.getElementById(reportType).style.display = 'block';
    const allTabs = document.querySelectorAll('.tab-button');
        allTabs.forEach(tab => {
            tab.classList.remove('active');
        });
        document.querySelector('[onclick="showReport(\'' + reportType + '\')"]').classList.add('active');
    fetchReportContent(reportType);
}
function prepareUrl(reportType){
    let year = null;
    if (reportType.includes("_")) {
        let parts = reportType.split("_");
        reportType = parts[0];
        year = parts[1];
    }
    let baseUrl = "";
    if(reportType === "expReport"|| reportType==="salaryReport")
        baseUrl = '/api/work/'+reportType;
    else if(reportType === "ownersReport"|| reportType==="rentPaymentReport")
        baseUrl = '/api/rent/'+reportType;
    else if(reportType === "manageReport")
        baseUrl = '/api/expenses/'+reportType;
    return year ? `${baseUrl}?year=${year}` : baseUrl;
}

function fetchReportContent(reportType) {
    const url = prepareUrl(reportType);
    if (reportType.includes("_")) {
        reportType = reportType.split("_")[2];
    }
    fetch(url)
        .then(response => response.text())
        .then(data => {
            const reportContainer = document.getElementById(reportType);
            reportContainer.innerHTML = data;
           attachReportListeners(reportContainer, reportType);
        })
        .catch(error => {
            console.error('Error fetching report:', error);
            const reportContainer = document.getElementById(reportType);
            reportContainer.innerHTML = '<p>Error loading report. Please try again.</p>';
        });
}

function attachReportListeners(reportContainer, reportType) {
    const downloadPdfButton = reportContainer.querySelector('#downloadPdf');
    const printReportButton = reportContainer.querySelector('#printReport');
   if (downloadPdfButton) {
           downloadPdfButton.addEventListener('click', function () {
                downloadPdf(reportType);
           });
       }

       if (printReportButton) {
           printReportButton.addEventListener('click', function () {
               const rows = reportContainer.querySelectorAll('table tbody tr');
               const headers = reportContainer.querySelectorAll('table thead tr');
               const reportButtons = document.querySelector('.report-buttons');
               if (reportButtons) {
                    reportButtons.style.display = 'none';
               }
               var chkLength = 0;
               if(reportType == 'expReport'){
                chkLength=5;
               }
               if(reportType=='salaryReport' || reportType=='rentPaymentReport'){
                chkLength=4;
               }
               if(reportType=='ownersReport'){
                chkLength=6;
               }
               rows.forEach(row => {
                   const cells = row.children;
                   if (cells.length > chkLength) {
                       cells[chkLength].style.display = 'none';
                   }
               });
               headers.forEach(headerRow => {
                   const headerCells = headerRow.children;
                   if (headerCells.length > chkLength) {
                       headerCells[chkLength].style.display = 'none';
                   }
               });

               const printWindow = window.open('', '', 'height=600,width=800');
               printWindow.document.write('<html><head><title>' + reportType + '</title></head><body>');
               printWindow.document.write(reportContainer.innerHTML);
               printWindow.document.write('</body></html>');
               printWindow.document.close();
               printWindow.print();
                if (reportButtons) {
                    reportButtons.style.display = '';
                }
               rows.forEach(row => {
                   const cells = row.children;
                   if (cells.length > chkLength) {
                       cells[chkLength].style.display = '';
                   }
               });

               headers.forEach(headerRow => {
                   const headerCells = headerRow.children;
                   if (headerCells.length > chkLength) {
                       headerCells[chkLength].style.display = '';
                   }
               });
           });
       }
}
function downloadPdf(param) {
    let parts = param.split("|");
    let reportType = parts[0];
    let year = parts.length > 1 ? parts[1] : "";

    let apiUrl = `/api/pdf/${reportType}Pdf`;
    if (year) {
        apiUrl += `?year=${year}`;
    }

    fetch(apiUrl, {
        method: 'GET',
        headers: {
            'Accept': 'application/pdf',
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.blob();
    })
    .then(blob => {
        const fileName = year ? `${reportType}_${year}.pdf` : `${reportType}.pdf`;
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
    })
    .catch(error => {
        console.error('Error downloading PDF:', error);
    });
}
